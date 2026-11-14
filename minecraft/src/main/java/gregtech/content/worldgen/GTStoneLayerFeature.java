package gregtech.content.worldgen;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.block.stone.StoneType;
import gregtech.content.block.stone.StoneVariant;
import gregtech.content.registry.GTBlocks;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.platform.Mc;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Port of GT6 {@code WorldgenStoneLayers}: regional stone strata selected per block by
 * cellular noise ({@link GTCellNoise}, ~110-block horizontal regions, ~13-block
 * vertical strata) over the full {@link GTStoneLayersGen#LAYERS} table.
 *
 * <p>A 7-entry sliding window ({@link StoneLayerScan}, y-3..y+3) drives ore placement exactly
 * like GT6: deep inside one layer (scan[5]==scan[1]) the layer's own ores roll —
 * normal ore in the core (scan[6]==scan[0]), small ore near the fringe; where two
 * different layers meet, the contact-ore table (GT6 doublelayers) applies. Per-column
 * surface-rock tracking uses {@link SurfaceRockRules}.</p>
 */
public class GTStoneLayerFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_GEN_Y = GTWorldgenScale.WORLD_FLOOR + 1;

    private static final Set<Block> REPLACEABLE_STONE = Set.of(
            Blocks.STONE,
            Blocks.INFESTED_STONE,
            Blocks.GRANITE,
            Blocks.DIORITE,
            Blocks.ANDESITE,
            Blocks.DEEPSLATE,
            Blocks.TUFF,
            Blocks.COAL_ORE,
            Blocks.IRON_ORE,
            Blocks.GOLD_ORE,
            Blocks.COPPER_ORE,
            Blocks.DIAMOND_ORE,
            Blocks.EMERALD_ORE,
            Blocks.LAPIS_ORE,
            Blocks.REDSTONE_ORE,
            Blocks.DEEPSLATE_COAL_ORE,
            Blocks.DEEPSLATE_IRON_ORE,
            Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DEEPSLATE_REDSTONE_ORE);
    private static final Set<Block> REPLACEABLE_COBBLE = Set.of(Blocks.COBBLESTONE);
    private static final Set<Block> REPLACEABLE_MOSSY = Set.of(Blocks.MOSSY_COBBLESTONE);

    /** Resolved layer: stone state (null = keep host) + ores with remapped Y ranges. */
    private record Layer(
            @Nullable BlockState stone,
            @Nullable BlockState cobble,
            @Nullable BlockState mossy,
            String material,
            boolean noDeep,
            List<Ore> ores) {}

    /** Below this, noDeep layers (coal seams etc.) keep the host — GT6 used y<24. */
    private static final int NO_DEEP_Y = GTWorldgenScale.remapY(24);

    private record Ore(
            GTMaterial material,
            boolean indicators,
            int chanceDiv,
            int minY,
            int maxY,
            StoneLayerOreRules.Rule rule,
            @Nullable net.minecraft.resources.ResourceLocation explicitBlock) {}

    private static volatile Layer[] LAYER_CACHE;
    private static volatile Map<String, Map<String, List<Ore>>> CONTACT_CACHE;
    private static final Map<Long, GTCellNoise> NOISE_BY_SEED = new ConcurrentHashMap<>();

    public GTStoneLayerFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!gregtech.GregTechConfig.Worldgen.GEOLOGY.enabled()) return false;
        if (!GTWorldgenDefaults.STONE_LAYERS) return false;
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        Layer[] layers = layers();
        Map<String, Map<String, List<Ore>>> contacts = contacts();
        GTCellNoise noise = NOISE_BY_SEED.computeIfAbsent(level.getSeed(), GTCellNoise::new);
        int listSize = layers.length;

        int chunkMinX = origin.getX() & ~15;
        int chunkMinZ = origin.getZ() & ~15;
        int minY = Math.max(MIN_GEN_Y, level.getMinBuildHeight());
        int maxY = maxScanY(level, chunkMinX, chunkMinZ);
        boolean placedAny = false;
        boolean surfaceRocks = gregtech.GregTechConfig.Worldgen.SURFACE_ROCKS.enabled();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        // Mirrors GT6 tLastReplaced so continuous layers of other mods' stones keep generating.
        Block lastReplaced = Blocks.STONE;

        for (int dx = 0; dx < 16; dx++) {
            int x = chunkMinX + dx;
            for (int dz = 0; dz < 16; dz++) {
                int z = chunkMinZ + dz;
                SurfaceRockRules.ColumnState rockState = new SurfaceRockRules.ColumnState();
                StoneLayerScan<Layer> scan =
                        new StoneLayerScan<>((sx, sy, sz) -> layers[noise.get(sx, sy, sz, listSize)], x, minY, z);

                for (int y = minY; y <= maxY; y++) {
                    cursor.set(x, y, z);
                    BlockState host = level.getBlockState(cursor);
                    Block hostBlock = host.getBlock();
                    Layer layer = scan.current();
                    boolean generateLayer = !(layer.noDeep() && y < NO_DEEP_Y);

                    if (GTRocksFeature.replaceable(host)
                            || !host.getFluidState().isEmpty()) {
                        if (surfaceRocks && host.getFluidState().isEmpty()) {
                            placedAny |= maybePlaceSurfaceRock(level, cursor, rockState, random);
                        }
                        // Air, plants and water break the solid column so buried ores never advertise above.
                        rockState.reset();
                    } else if (host.is(Blocks.BEDROCK)) {
                        rockState.bedrock();
                    } else if (hostBlock instanceof GTStoneBlock stone) {
                        // GT6 BlockStones (stone/cobble/mossy): keep placement eligibility without
                        // overwriting tLastRock so structures or pre-placed GT stones preserve strata hints.
                        boolean natural = stone.variant() == StoneVariant.STONE
                                || stone.variant() == StoneVariant.COBBLE
                                || stone.variant() == StoneVariant.COBBLE_MOSSY;
                        if (natural) {
                            rockState.opaque(true);
                        } else {
                            rockState.reset();
                        }
                    } else if (isReplaceableStone(host, hostBlock, lastReplaced)) {
                        lastReplaced = hostBlock;
                        // The ore background must be the layer's stone (what the host is about
                        // to become), not the pre-replacement host — otherwise ores show the
                        // vanilla rock (e.g. deepslate) inside a GT stone layer.
                        BlockState background = generateLayer && layer.stone() != null ? layer.stone() : host;
                        Ore placedOre = null;
                        if (generateLayer && scan.isInterior()) {
                            boolean core = scan.isCore();
                            placedOre = tryPlaceOre(level, cursor, background, layer.ores(), y, !core, false, random);
                        } else if (generateLayer) {
                            var below = contacts.get(scan.above().material());
                            List<Ore> contactOres = below == null
                                    ? null
                                    : below.get(scan.below().material());
                            if (contactOres != null) {
                                placedOre = tryPlaceOre(level, cursor, background, contactOres, y, false, true, random);
                            }
                        }
                        if (placedOre != null) {
                            rockState.opaque(true);
                            rockState.ore(placedOre.material().getName(), placedOre.indicators());
                            placedAny = true;
                        } else {
                            String rockMat = generateLayer
                                            && layer.stone() != null
                                            && !(layer.stone().getBlock() instanceof GTStoneBlock)
                                    ? layer.material()
                                    : rockMaterial(background);
                            rockState.replaced(rockMat);
                            if (generateLayer
                                    && layer.stone() != null
                                    && !host.is(layer.stone().getBlock())) {
                                level.setBlock(cursor, layer.stone(), 2);
                                placedAny = true;
                            }
                        }
                    } else if (REPLACEABLE_COBBLE.contains(hostBlock)) {
                        rockState.replaced(
                                generateLayer && layer.cobble() != null ? rockMaterial(layer.cobble()) : "Stone");
                        if (generateLayer && layer.cobble() != null) {
                            level.setBlock(cursor, layer.cobble(), 2);
                            placedAny = true;
                        }
                    } else if (REPLACEABLE_MOSSY.contains(hostBlock)) {
                        rockState.replaced(
                                generateLayer && layer.mossy() != null ? rockMaterial(layer.mossy()) : "Stone");
                        if (generateLayer && layer.mossy() != null) {
                            level.setBlock(cursor, layer.mossy(), 2);
                            placedAny = true;
                        }
                    } else if (host.isSolidRender(level, cursor)) {
                        rockState.opaque(suitableRockGround(host));
                    } else {
                        rockState.reset();
                    }

                    // Existing vanilla structure half-blocks follow the same local host material.
                    // Do not recolor existing GT slabs or other decorative slab families.
                    if (generateLayer
                            && (hostBlock == Blocks.COBBLESTONE_SLAB || hostBlock == Blocks.MOSSY_COBBLESTONE_SLAB)) {
                        BlockState full = hostBlock == Blocks.COBBLESTONE_SLAB ? layer.cobble() : layer.mossy();
                        BlockState replacement = structureSlabState(host, full);
                        if (replacement != null) placedAny |= level.setBlock(cursor, replacement, 2);
                    }

                    scan.advance();
                }
            }
        }
        return placedAny;
    }

    @Nullable
    private static Ore tryPlaceOre(
            WorldGenLevel level,
            BlockPos pos,
            BlockState background,
            List<Ore> ores,
            int y,
            boolean small,
            boolean randomSmall,
            RandomSource random) {
        for (int oreIndex = 0; oreIndex < ores.size(); oreIndex++) {
            Ore ore = ores.get(oreIndex);
            if (y < ore.minY() || y > ore.maxY()) continue;
            if (!StoneLayerOrePlacement.enabled(ore.explicitBlock())) continue;
            if (!ore.rule().biomes().isEmpty() && !StoneLayerOrePlacement.matches(ore.rule(), level.getBiome(pos)))
                continue;
            if (random.nextInt(ore.chanceDiv()) != 0) continue;
            boolean useSmall = randomSmall ? random.nextBoolean() : small;
            if (placeOre(level, pos, background, ore, useSmall)) {
                return ore;
            }
        }
        return null;
    }

    private static boolean maybePlaceSurfaceRock(
            WorldGenLevel level, BlockPos pos, SurfaceRockRules.ColumnState rockState, RandomSource random) {
        if (!rockState.canPlaceRocks() || !SurfaceRockRules.roll(random::nextInt)) return false;
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return false;
        String material =
                SurfaceRockRules.rockMaterial(rockState, random::nextBoolean, GTStoneLayerFeature::targetCrushing);
        return GTRockPlacement.place(level, pos, material, null, false);
    }

    private static boolean isReplaceableStone(BlockState host, Block hostBlock, Block lastReplaced) {
        return hostBlock == lastReplaced
                || REPLACEABLE_STONE.contains(hostBlock)
                || host.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    private static boolean suitableRockGround(BlockState state) {
        return GTRocksFeature.rockGround(state) || state.is(Blocks.CLAY) || state.is(Blocks.GRAVEL);
    }

    private static int maxScanY(WorldGenLevel level, int chunkMinX, int chunkMinZ) {
        ChunkAccess chunk = level.getChunk(chunkMinX >> 4, chunkMinZ >> 4);
        return StoneLayerScan.maxScanY(
                level.getMinBuildHeight(), level.getMaxBuildHeight(), chunk.getHighestFilledSectionIndex());
    }

    private static String targetCrushing(String name) {
        GTMaterial material = GTMaterialRegistry.get(name);
        if (material == null || !material.resolve().isValid()) return name;
        return material.resolve().getTargetCrushingMaterial().getName();
    }

    private static String rockMaterial(BlockState state) {
        if (state.getBlock() instanceof GTStoneBlock stone)
            return stone.stoneType().material().getName();
        var host = GTOreBlockResolver.stoneOf(state);
        return host == null || host.smallOreDustMaterial() == null ? "Stone" : host.smallOreDustMaterial();
    }

    private static boolean placeOre(WorldGenLevel level, BlockPos pos, BlockState background, Ore ore, boolean small) {
        BlockState explicit = StoneLayerOrePlacement.explicitState(ore.explicitBlock());
        return explicit != null
                ? level.setBlock(pos, explicit, 2)
                : GTOreBlockResolver.placeOre(level, pos, background, ore.material(), small);
    }

    /** Only called for the two vanilla cobble slab hosts, during stone-layer generation. */
    private static BlockState structureSlabState(BlockState host, @Nullable BlockState full) {
        if (full == null || !(full.getBlock() instanceof gregtech.content.block.stone.GTStoneBlock stone)) return null;
        var half = host.getValue(net.minecraft.world.level.block.SlabBlock.TYPE);
        boolean wet = host.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED);
        if (half == net.minecraft.world.level.block.state.properties.SlabType.DOUBLE) {
            // A noncanonical externally-created wet double cannot be represented by a full GT block.
            return wet ? null : full;
        }
        Block slab = GTBlocks.getStoneSlab(stone.stoneType(), stone.variant());
        if (!(slab instanceof gregtech.content.block.stone.GTStoneSlabBlock)) return null;
        return slab.defaultBlockState()
                .setValue(
                        gregtech.content.block.stone.GTStoneSlabBlock.FACING,
                        half == net.minecraft.world.level.block.state.properties.SlabType.TOP
                                ? net.minecraft.core.Direction.UP
                                : net.minecraft.core.Direction.DOWN)
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, wet);
    }

    // ── data resolution (lazy, after registries are loaded) ─────────────────

    private static Layer[] layers() {
        Layer[] cached = LAYER_CACHE;
        if (cached != null) return cached;
        synchronized (GTStoneLayerFeature.class) {
            if (LAYER_CACHE != null) return LAYER_CACHE;
            List<GTStoneLayersGen.LayerDef> defs = GTStoneLayersGen.LAYERS;
            Layer[] layers = new Layer[defs.size()];
            for (int i = 0; i < defs.size(); i++) {
                GTStoneLayersGen.LayerDef def = defs.get(i);
                BlockState stone = null, cobble = null, mossy = null;
                if (def.stoneType() != null) {
                    StoneType type = StoneType.valueOf(def.stoneType());
                    stone = GTBlocks.getStoneState(type, StoneVariant.STONE);
                    cobble = GTBlocks.getStoneState(type, StoneVariant.COBBLE);
                    mossy = GTBlocks.getStoneState(type, StoneVariant.COBBLE_MOSSY);
                } else if (def.blockId() != null) {
                    // Whole-block ore seams (GT6 BlockRockOres -> iconset blocks).
                    Block seam = Mc.block(Mc.gregtechId(def.blockId()));
                    if (seam != null && seam != Blocks.AIR) {
                        stone = seam.defaultBlockState();
                    }
                }
                layers[i] = new Layer(stone, cobble, mossy, def.material(), def.noDeep(), resolveOres(def.ores()));
            }
            LAYER_CACHE = layers;
            return layers;
        }
    }

    private static Map<String, Map<String, List<Ore>>> contacts() {
        Map<String, Map<String, List<Ore>>> cached = CONTACT_CACHE;
        if (cached != null) return cached;
        synchronized (GTStoneLayerFeature.class) {
            if (CONTACT_CACHE != null) return CONTACT_CACHE;
            Map<String, Map<String, List<Ore>>> map = new HashMap<>();
            for (GTStoneLayersGen.ContactDef def : GTStoneLayersGen.CONTACTS) {
                List<Ore> ores = resolveOres(def.ores());
                if (ores.isEmpty()) continue;
                map.computeIfAbsent(def.materialA(), ignored -> new HashMap<>())
                        .merge(def.materialB(), ores, GTStoneLayerFeature::concat);
                if (def.bothSides()) {
                    map.computeIfAbsent(def.materialB(), ignored -> new HashMap<>())
                            .merge(def.materialA(), ores, GTStoneLayerFeature::concat);
                }
            }
            CONTACT_CACHE = map;
            return map;
        }
    }

    private static List<Ore> concat(List<Ore> a, List<Ore> b) {
        java.util.ArrayList<Ore> out = new java.util.ArrayList<>(a);
        out.addAll(b);
        return out;
    }

    private static List<Ore> resolveOres(List<GTStoneLayersGen.OreDef> defs) {
        java.util.ArrayList<Ore> out = new java.util.ArrayList<>();
        for (GTStoneLayersGen.OreDef def : defs) {
            GTMaterial material = GTMaterialRegistry.get(def.material());
            if (material == null || !material.resolve().isValid()) continue;
            var rule = StoneLayerOreRules.rule(def);
            var block =
                    rule.blockId().isEmpty() ? null : net.minecraft.resources.ResourceLocation.parse(rule.blockId());
            out.add(new Ore(
                    material.resolve(),
                    def.indicators(),
                    Math.max(1, def.chanceDiv()),
                    GTWorldgenScale.stoneLayerOreMinY(def),
                    GTWorldgenScale.remapY(def.maxY()),
                    rule,
                    block));
        }
        return List.copyOf(out);
    }
}
