package gregtech.content.worldgen;

import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.block.stone.StoneType;
import gregtech.content.block.stone.StoneVariant;
import gregtech.content.registry.GTBlocks;
import net.minecraft.core.BlockPos;
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

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Port of GT6 {@code WorldgenStoneLayers}: regional stone strata selected per block by
 * cellular noise ({@link GTCellNoise}, ~110-block horizontal regions, ~13-block
 * vertical strata) over the full {@link GTStoneLayersGen#LAYERS} table.
 *
 * <p>A 7-entry sliding window of layer samples (y-3..y+3) drives ore placement exactly
 * like GT6: deep inside one layer (scan[5]==scan[1]) the layer's own ores roll —
 * normal ore in the core (scan[6]==scan[0]), small ore near the fringe; where two
 * different layers meet, the contact-ore table (GT6 doublelayers) applies. This is
 * what confines each ore to its regional stone layer instead of blanketing the world
 * at a fixed height.</p>
 *
 * <p>The surface-rock state follows GT6's {@code tLastRock}/{@code tLastOre}: every column
 * tracks the topmost replaced layer and the last indicator ore, and the 1/128 rock roll
 * happens at the first air or replaceable block above the opaque ground. The rock carries
 * the layer material, or the ore's crushing target when the 50% ore roll wins.</p>
 */
public class GTStoneLayerFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_GEN_Y = GTWorldgenScale.WORLD_FLOOR + 1;

    private static final Set<Block> REPLACEABLE_STONE = Set.of(
            Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE,
            Blocks.DEEPSLATE, Blocks.TUFF,
            Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.COPPER_ORE,
            Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.LAPIS_ORE, Blocks.REDSTONE_ORE,
            Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.DEEPSLATE_COPPER_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE_REDSTONE_ORE
    );
    private static final Set<Block> REPLACEABLE_COBBLE = Set.of(Blocks.COBBLESTONE);
    private static final Set<Block> REPLACEABLE_MOSSY = Set.of(Blocks.MOSSY_COBBLESTONE);

    /** Resolved layer: stone state (null = keep host) + ores with remapped Y ranges. */
    private record Layer(@Nullable BlockState stone, @Nullable BlockState cobble, @Nullable BlockState mossy,
                         String material, boolean noDeep, List<Ore> ores) {}

    private record Ore(GTMaterial material, boolean indicators, int chanceDiv, int minY, int maxY,
                       StoneLayerOreRules.Rule rule, @Nullable net.minecraft.resources.ResourceLocation explicitBlock) {}

    private static volatile Layer[] LAYER_CACHE;
    private static volatile Layer DEEPSLATE_LAYER;
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
        Layer deepslate = deepslateLayer();
        Map<String, Map<String, List<Ore>>> contacts = contacts();
        GTCellNoise noise = NOISE_BY_SEED.computeIfAbsent(level.getSeed(), GTCellNoise::new);
        int listSize = layers.length;

        int chunkMinX = origin.getX() & ~15;
        int chunkMinZ = origin.getZ() & ~15;
        int maxGenY = maxGenY(level, chunkMinX, chunkMinZ);
        boolean placedAny = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; dx++) {
            int x = chunkMinX + dx;
            for (int dz = 0; dz < 16; dz++) {
                int z = chunkMinZ + dz;
                SurfaceRockRules.ColumnState rockState = new SurfaceRockRules.ColumnState();
                StoneLayerScan<Layer> scan = new StoneLayerScan<>(
                        (sx, sy, sz) -> layers[noise.get(sx, sy, sz, listSize)],
                        Layer::noDeep, deepslate, GTWorldgenScale.remapY(24), x, MIN_GEN_Y, z);

                for (int y = MIN_GEN_Y; y < maxGenY; y++) {
                    cursor.set(x, y, z);
                    BlockState host = level.getBlockState(cursor);
                    Block hostBlock = host.getBlock();
                    Layer layer = scan.current();

                    if (hostBlock == Blocks.BEDROCK) {
                        rockState.bedrock();
                    } else if (host.isAir()) {
                        maybeRock(level, cursor, rockState, random);
                        rockState.reset();
                    } else if (REPLACEABLE_STONE.contains(hostBlock)) {
                        // The ore background must be the layer's stone (what the host is about
                        // to become), not the pre-replacement host — otherwise ores show the
                        // vanilla rock (e.g. deepslate) inside a GT stone layer.
                        BlockState background = layer.stone() != null ? layer.stone() : host;
                        Ore placed = null;
                        boolean orePlaced = false;
                        if (scan.isInterior()) {
                            // Interior of one layer: the layer's own ores.
                            boolean core = scan.isCore();
                            for (int oreIndex = 0; oreIndex < layer.ores().size(); oreIndex++) {
                                Ore ore = layer.ores().get(oreIndex);
                                if (y < ore.minY() || y > ore.maxY()) continue;
                                if (!StoneLayerOrePlacement.enabled(ore.explicitBlock())) continue;
                                if (!ore.rule().biomes().isEmpty() && !StoneLayerOrePlacement.matches(ore.rule(), level.getBiome(cursor))) continue;
                                if (random.nextInt(ore.chanceDiv()) != 0) continue;
                                if (placeOre(level, cursor, background, ore, !core)) {
                                    orePlaced = true;
                                    placed = ore;
                                    placedAny = true;
                                    break;
                                }
                            }
                        } else {
                            // Layer boundary: contact ores (GT6 doublelayers).
                            var below = contacts.get(scan.above().material());
                            List<Ore> contactOres = below == null ? null : below.get(scan.below().material());
                            if (contactOres != null) {
                                for (int oreIndex = 0; oreIndex < contactOres.size(); oreIndex++) {
                                    Ore ore = contactOres.get(oreIndex);
                                    if (y < ore.minY() || y > ore.maxY()) continue;
                                    if (!StoneLayerOrePlacement.enabled(ore.explicitBlock())) continue;
                                    if (!ore.rule().biomes().isEmpty() && !StoneLayerOrePlacement.matches(ore.rule(), level.getBiome(cursor))) continue;
                                    if (random.nextInt(ore.chanceDiv()) != 0) continue;
                                    if (placeOre(level, cursor, background, ore, random.nextBoolean())) {
                                        orePlaced = true;
                                        placed = ore;
                                        placedAny = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (orePlaced) {
                            rockState.ore(placed.material().getName(), placed.indicators());
                        } else {
                            rockState.replaced(layer.material());
                            if (layer.stone() != null && !host.is(layer.stone().getBlock())) {
                                level.setBlock(cursor, layer.stone(), 2);
                                placedAny = true;
                            }
                        }
                    } else if (REPLACEABLE_COBBLE.contains(hostBlock)) {
                        rockState.replaced(layer.material());
                        if (layer.cobble() != null) {
                            level.setBlock(cursor, layer.cobble(), 2);
                            placedAny = true;
                        }
                    } else if (REPLACEABLE_MOSSY.contains(hostBlock)) {
                        rockState.replaced(layer.material());
                        if (layer.mossy() != null) {
                            level.setBlock(cursor, layer.mossy(), 2);
                            placedAny = true;
                        }
                    } else if (hostBlock instanceof GTStoneBlock stone && stone.variant().meta() < 3) {
                        // GT6 BlockStones: metadata 0-2 (stone/cobble/mossy) keeps the rock flag.
                        rockState.opaque(true);
                    } else if (replaceable(host)) {
                        maybeRock(level, cursor, rockState, random);
                        rockState.reset();
                    } else if (host.isSolidRender(level, cursor)) {
                        rockState.opaque(suitableGround(host));
                    } else {
                        rockState.reset();
                    }

                    // Existing vanilla structure half-blocks follow the same local host material.
                    // Do not recolor existing GT slabs or other decorative slab families.
                    if (hostBlock == Blocks.COBBLESTONE_SLAB || hostBlock == Blocks.MOSSY_COBBLESTONE_SLAB) {
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

    /** GT6's {@code tCanPlaceRocks && aRandom.nextInt(128) == 0} branch. */
    private static void maybeRock(WorldGenLevel level, BlockPos pos, SurfaceRockRules.ColumnState state,
            RandomSource random) {
        if (!gregtech.GregTechConfig.Worldgen.SURFACE_ROCKS.enabled()) return;
        if (!state.canPlaceRocks()) return;
        if (!SurfaceRockRules.roll(random::nextInt)) return;
        String material = SurfaceRockRules.rockMaterial(state, random::nextBoolean, GTStoneLayerFeature::targetCrushing);
        GTRockPlacement.place(level, pos, material, null, false);
    }

    /** GT6 {@code tLastOre.mTargetCrushing.mMaterial}; most ores resolve to themselves. */
    private static String targetCrushing(String name) {
        GTMaterial material = GTMaterialRegistry.get(name);
        if (material == null || !material.resolve().isValid()) return name;
        return material.resolve().getTargetCrushingMaterial().getName();
    }

    /** GT6's {@code material.clay / sand / grass / ground} opaque-ground test. */
    private static boolean suitableGround(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.GRAVEL);
    }

    /** GT6 {@code WD.easyRep}: air, plants, snow, fire and leaves may be replaced by a rock. */
    private static boolean replaceable(BlockState state) {
        return state.isAir()
                || state.is(BlockTags.REPLACEABLE)
                || state.is(Blocks.SNOW)
                || state.is(BlockTags.LEAVES);
    }

    /** GT6's {@code tMaxHeight = aChunk.getTopFilledSegment()+15}, in modern section terms. */
    private static int maxGenY(WorldGenLevel level, int chunkMinX, int chunkMinZ) {
        ChunkAccess chunk = level.getChunk(chunkMinX >> 4, chunkMinZ >> 4);
        int highest = chunk.getHighestFilledSectionIndex();
        return highest < 0 ? level.getMinBuildHeight() : level.getMinBuildHeight() + (highest + 1) * 16;
    }

    private static boolean placeOre(WorldGenLevel level, BlockPos pos, BlockState background, Ore ore, boolean small) {
        BlockState explicit = StoneLayerOrePlacement.explicitState(ore.explicitBlock());
        return explicit != null ? level.setBlock(pos, explicit, 2)
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
                .setValue(gregtech.content.block.stone.GTStoneSlabBlock.FACING,
                        half == net.minecraft.world.level.block.state.properties.SlabType.TOP
                                ? net.minecraft.core.Direction.UP : net.minecraft.core.Direction.DOWN)
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
                layers[i] = resolveLayer(defs.get(i));
            }
            LAYER_CACHE = layers;
            return layers;
        }
    }

    private static Layer deepslateLayer() {
        Layer cached = DEEPSLATE_LAYER;
        if (cached != null) return cached;
        synchronized (GTStoneLayerFeature.class) {
            if (DEEPSLATE_LAYER == null) DEEPSLATE_LAYER = resolveLayer(GTStoneLayersGen.DEEPSLATE);
            return DEEPSLATE_LAYER;
        }
    }

    private static Layer resolveLayer(GTStoneLayersGen.LayerDef def) {
        BlockState stone = null, cobble = null, mossy = null;
        if (def.stoneType() != null) {
            StoneType type = StoneType.valueOf(def.stoneType());
            stone = GTBlocks.getStoneState(type, StoneVariant.STONE);
            cobble = GTBlocks.getStoneState(type, StoneVariant.COBBLE);
            mossy = GTBlocks.getStoneState(type, StoneVariant.COBBLE_MOSSY);
        } else if (def.blockId() != null) {
            // Whole-block ore seams (GT6 BlockRockOres -> iconset blocks).
//? if >=1.21.1 {
            /*Block seam = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", def.blockId()));
*///?} else {
            Block seam = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(gregtech.GregTech.NAMESPACE, def.blockId()));
//?}
            if (seam != null && seam != Blocks.AIR) {
                stone = seam.defaultBlockState();
            }
        }
        return new Layer(stone, cobble, mossy, def.material(), def.noDeep(), resolveOres(def.ores()));
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
            var block = rule.blockId().isEmpty() ? null : net.minecraft.resources.ResourceLocation.parse(rule.blockId());
            out.add(new Ore(material.resolve(), def.indicators(), Math.max(1, def.chanceDiv()),
                    GTWorldgenScale.stoneLayerOreMinY(def), GTWorldgenScale.remapY(def.maxY()), rule, block));
        }
        return List.copyOf(out);
    }
}
