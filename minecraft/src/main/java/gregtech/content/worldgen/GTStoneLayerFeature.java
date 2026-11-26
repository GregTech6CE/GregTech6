package gregtech.content.worldgen;

import gregtech.GregTechConfig;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.OreBlock;
import gregtech.content.block.OreHostStone;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.block.stone.GTStoneSlabBlock;
import gregtech.content.block.stone.StoneType;
import gregtech.content.block.stone.StoneVariant;
import gregtech.content.registry.GTBlocks;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.platform.Mc;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.jetbrains.annotations.Nullable;

/** Applies the shared {@link StoneLayerField} to the natural hosts in one chunk. */
public final class GTStoneLayerFeature extends Feature<NoneFeatureConfiguration> {
    private static final Set<Block> VANILLA_STONE_ORES = Set.of(
            Blocks.COAL_ORE,
            Blocks.IRON_ORE,
            Blocks.GOLD_ORE,
            Blocks.COPPER_ORE,
            Blocks.DIAMOND_ORE,
            Blocks.EMERALD_ORE,
            Blocks.LAPIS_ORE,
            Blocks.REDSTONE_ORE);
    private static final Set<Block> VANILLA_DEEPSLATE_ORES = Set.of(
            Blocks.DEEPSLATE_COAL_ORE,
            Blocks.DEEPSLATE_IRON_ORE,
            Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DEEPSLATE_REDSTONE_ORE);

    private record Layer(
            @Nullable BlockState stone,
            @Nullable BlockState cobble,
            @Nullable BlockState mossy,
            String surfaceMaterial) {
        private Layer {
            if (stone != null && stone.getBlock() instanceof GTStoneBlock gtStone)
                surfaceMaterial = gtStone.stoneMaterial().getName();
        }
    }

    private enum Host {
        NATURAL,
        REPLACEABLE,
        OTHER
    }

    private static final class Catalog {
        private static final List<Layer> LAYERS = StoneLayerField.layers().stream()
                .map(GTStoneLayerFeature::resolveLayer)
                .toList();
    }

    static @Nullable BlockState layerStone(int index) {
        return Catalog.LAYERS.get(index).stone();
    }

    static String layerMaterial(int index) {
        return Catalog.LAYERS.get(index).surfaceMaterial();
    }

    static boolean naturalLayerHost(BlockState state) {
        return classify(state) == Host.NATURAL;
    }

    public GTStoneLayerFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GregTechConfig.Worldgen.GEOLOGY.enabled()) return false;
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        var origin = context.origin();
        var chunk = level.getChunk(origin.getX() >> 4, origin.getZ() >> 4);
        int minY = level.getMinBuildHeight();
        int maxY = StoneLayerField.maxScanY(minY, level.getMaxBuildHeight(), chunk.getHighestFilledSectionIndex());
        if (maxY < minY) return false;
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        var samples = new StoneLayerField(seed).sample(minX, minZ, 16, 16, minY, maxY);
        var contacts = new StoneLayerField.Contacts();
        var cursor = new BlockPos.MutableBlockPos();
        var support = new BlockPos.MutableBlockPos();
        var orePlacement = new OrePlacement(level, cursor);
        var hosts = new IdentityHashMap<BlockState, Host>();
        boolean surfaceRocks = GregTechConfig.Worldgen.SURFACE_ROCKS.enabled();
        int gemProbability = GregTechConfig.Number.BOUNDARY_GEM_PROBABILITY.value();
        boolean changed = false;

        for (int dx = 0; dx < 16; dx++) {
            int x = minX + dx;
            for (int dz = 0; dz < 16; dz++) {
                int z = minZ + dz;
                int topY = Math.min(maxY, chunk.getHeight(Heightmap.Types.WORLD_SURFACE, dx, dz) + 1);
                if (topY < minY) continue;
                var surface = new SurfaceRockRules.ColumnState();
                surface.gap();

                for (int y = minY; y <= topY; y++) {
                    cursor.set(x, y, z);
                    BlockState host = chunk.getBlockState(cursor);
                    Block block = host.getBlock();
                    Host kind = hosts.computeIfAbsent(host, GTStoneLayerFeature::classify);
                    int rock = samples.layerIndex(x, y, z);
                    Layer layer = Catalog.LAYERS.get(rock);

                    if (kind == Host.NATURAL) {
                        long positionSeed = WorldgenHash.at(seed, x, y, z);
                        BlockState background = layer.stone() == null ? host : layer.stone();
                        samples.contacts(x, y, z, contacts);
                        orePlacement.background = background;
                        var ore = StratumOreRules.place(rock, y, contacts, positionSeed, orePlacement);
                        if (ore != null) {
                            surface.replaced(layer.stone() == null ? rockMaterial(host) : layer.surfaceMaterial());
                            surface.ore(ore.material().getName(), ore.indicators());
                            changed = true;
                        } else {
                            boolean replaced = layer.stone() != null
                                    && !host.is(layer.stone().getBlock())
                                    && GTOreBlockResolver.setInChunk(level, chunk, cursor, layer.stone());
                            boolean gem =
                                    tryBoundaryGem(level, cursor, background, positionSeed, gemProbability, contacts);
                            surface.replaced(
                                    layer.stone() != null && (replaced || block == background.getBlock())
                                            ? layer.surfaceMaterial()
                                            : rockMaterial(host));
                            changed |= replaced || gem;
                        }
                    } else if (block == Blocks.COBBLESTONE || block == Blocks.MOSSY_COBBLESTONE) {
                        BlockState next = block == Blocks.COBBLESTONE ? layer.cobble() : layer.mossy();
                        boolean replaced = next != null && GTOreBlockResolver.setInChunk(level, chunk, cursor, next);
                        surface.replaced(replaced ? rockMaterial(next) : "Stone");
                        changed |= replaced;
                    } else if (block == Blocks.COBBLESTONE_SLAB || block == Blocks.MOSSY_COBBLESTONE_SLAB) {
                        BlockState next = block == Blocks.COBBLESTONE_SLAB ? layer.cobble() : layer.mossy();
                        BlockState replacement = structureSlabState(host, next);
                        if (replacement != null)
                            changed |= GTOreBlockResolver.setInChunk(level, chunk, cursor, replacement);
                        surface.gap();
                    } else if (kind == Host.REPLACEABLE) {
                        support.set(x, y - 1, z);
                        long positionSeed = WorldgenHash.at(seed, x, y, z);
                        if (y > minY
                                && surface.canPlaceRocks()
                                && chunk.getBlockState(support).isFaceSturdy(level, support, Direction.UP)
                                && SurfaceRockRules.roll(positionSeed)) {
                            var pebble = SurfaceRockRules.pebble(
                                    surface, positionSeed, GTStoneLayerFeature::crushingMaterial);
                            if (pebble.shouldPlace(surfaceRocks))
                                changed |= GTRockPlacement.placeStratum(level, cursor, pebble);
                        }
                        surface.gap();
                    } else if (block instanceof GTStoneBlock stone && naturalVariant(stone.variant())) {
                        surface.replaced(stone.stoneMaterial().getName());
                    } else if (block instanceof OreBlock && host.hasProperty(OreBlock.STONE)) {
                        String material = host.getValue(OreBlock.STONE).smallOreDustMaterial();
                        if (material != null) surface.replaced(material);
                        else surface.gap();
                    } else {
                        OreHostStone vanillaOreHost = vanillaOreHost(block);
                        if (vanillaOreHost != null) surface.replaced(vanillaOreHost.smallOreDustMaterial());
                        else if (host.getFluidState().isEmpty()
                                && (GTRocksFeature.rockGround(host) || block == Blocks.CLAY || block == Blocks.GRAVEL))
                            surface.opaque(true);
                        else surface.gap();
                    }
                }
            }
        }
        return changed;
    }

    private static Host classify(BlockState state) {
        if (!state.hasBlockEntity()) {
            if (state.is(Blocks.STONE) || state.is(Blocks.INFESTED_STONE) || state.is(Blocks.INFESTED_DEEPSLATE))
                return Host.NATURAL;
            boolean stone = state.is(BlockTags.BASE_STONE_OVERWORLD)
                    || state.is(BlockTags.STONE_ORE_REPLACEABLES)
                    || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
            if (stone && vanillaOreHost(state.getBlock()) == null) return Host.NATURAL;
        }
        return GTRocksFeature.replaceable(state) ? Host.REPLACEABLE : Host.OTHER;
    }

    private static @Nullable OreHostStone vanillaOreHost(Block block) {
        if (VANILLA_STONE_ORES.contains(block)) return OreHostStone.STONE;
        if (VANILLA_DEEPSLATE_ORES.contains(block)) return OreHostStone.DEEPSLATE;
        return null;
    }

    private static boolean naturalVariant(StoneVariant variant) {
        return variant == StoneVariant.STONE || variant == StoneVariant.COBBLE || variant == StoneVariant.COBBLE_MOSSY;
    }

    private static String crushingMaterial(String name) {
        var material = GTMaterialRegistry.get(name);
        return material.isValid() ? material.getTargetCrushingMaterial().getName() : name;
    }

    private static boolean tryBoundaryGem(
            WorldGenLevel level,
            BlockPos pos,
            BlockState host,
            long positionSeed,
            int probability,
            StoneLayerField.Contacts contacts) {
        GTMaterial gem = BoundaryGemOres.choose(positionSeed, probability);
        if (gem == null || contacts.nearest() != 1) return false;
        OreHostStone stone = GTOreBlockResolver.stoneOf(host);
        return stone != null && GTOreBlockResolver.placeOre(level, pos, stone, gem, true);
    }

    private static final class OrePlacement implements StratumOreRules.Placer {
        private final WorldGenLevel level;
        private final BlockPos pos;
        private BlockState background;

        private OrePlacement(WorldGenLevel level, BlockPos pos) {
            this.level = level;
            this.pos = pos;
        }

        @Override
        public boolean place(StratumOreRules.Ore ore, boolean small) {
            if (ore.blockId().isEmpty())
                return GTOreBlockResolver.placeOre(level, pos, background, ore.material(), small);
            BlockState state = StoneLayerOrePlacement.enabledState(ResourceLocation.parse(ore.blockId()));
            return state != null && level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    private static String rockMaterial(BlockState state) {
        if (state.getBlock() instanceof GTStoneBlock stone)
            return stone.stoneMaterial().getName();
        OreHostStone stone = GTOreBlockResolver.stoneOf(state);
        return stone == null || stone.smallOreDustMaterial() == null ? "Stone" : stone.smallOreDustMaterial();
    }

    /** Preserves the half and waterlogged state of vanilla cobble slabs in generated structures. */
    @Nullable
    private static BlockState structureSlabState(BlockState host, @Nullable BlockState full) {
        if (full == null || !(full.getBlock() instanceof GTStoneBlock stone)) return null;
        SlabType half = host.getValue(SlabBlock.TYPE);
        boolean wet = host.getValue(BlockStateProperties.WATERLOGGED);
        if (half == SlabType.DOUBLE) return wet ? null : full;
        Block slab = GTBlocks.getStoneSlab(stone.stoneType(), stone.variant());
        if (!(slab instanceof GTStoneSlabBlock)) return null;
        return slab.defaultBlockState()
                .setValue(GTStoneSlabBlock.FACING, half == SlabType.TOP ? Direction.UP : Direction.DOWN)
                .setValue(BlockStateProperties.WATERLOGGED, wet);
    }

    private static Layer resolveLayer(GTStoneLayersGen.LayerDef def) {
        if (def.stoneType() != null) {
            StoneType type = StoneType.valueOf(def.stoneType());
            return new Layer(
                    GTBlocks.getStoneState(type, StoneVariant.STONE),
                    GTBlocks.getStoneState(type, StoneVariant.COBBLE),
                    GTBlocks.getStoneState(type, StoneVariant.COBBLE_MOSSY),
                    def.material());
        }
        BlockState stone = null;
        if (def.blockId() != null) {
            Block block = Mc.block(
                    ResourceLocation.parse(def.blockId().contains(":") ? def.blockId() : "gt6ce:" + def.blockId()));
            if (block != null && block != Blocks.AIR) stone = block.defaultBlockState();
        }
        return new Layer(stone, null, null, def.material());
    }
}
