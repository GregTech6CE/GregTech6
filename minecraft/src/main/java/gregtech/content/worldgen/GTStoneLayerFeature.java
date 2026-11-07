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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

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
 */
public class GTStoneLayerFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_GEN_Y = GTWorldgenScale.WORLD_FLOOR + 1;
    private static final int MAX_GEN_Y = GTWorldgenScale.BAND_TOP;

    private static final Set<Block> REPLACEABLE_STONE = Set.of(
            Blocks.STONE,
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
        boolean placedAny = false;
        boolean surfaceRocks = gregtech.GregTechConfig.Worldgen.SURFACE_ROCKS.enabled();
        int minY = Math.max(MIN_GEN_Y, level.getMinBuildHeight());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Layer[] scan = new Layer[7];

        for (int dx = 0; dx < 16; dx++) {
            int x = chunkMinX + dx;
            for (int dz = 0; dz < 16; dz++) {
                int z = chunkMinZ + dz;

                // scan[k] = layer at y + (k - 3).
                for (int k = 0; k < 7; k++) {
                    scan[k] = layers[noise.get(x, minY + k - 3, z, listSize)];
                }

                boolean canPlaceRocks = false;
                String lastRock = "Deepslate";
                GTMaterial lastOre = null;
                // Continue observing existing terrain above the generation band, without replacing
                // it. An indicator belongs to an exposed surface, not the layer sampled at the cap.
                int topY = Math.min(
                        level.getMaxBuildHeight() - 1,
                        Math.max(MAX_GEN_Y, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z)));
                for (int y = minY; y <= topY; y++) {
                    cursor.set(x, y, z);
                    BlockState host = level.getBlockState(cursor);
                    Block hostBlock = host.getBlock();
                    Layer layer = scan[3];
                    boolean generateLayer = y < MAX_GEN_Y && !(layer.noDeep() && y < NO_DEEP_Y);

                    if (GTRocksFeature.replaceable(host)
                            || !host.getFluidState().isEmpty()) {
                        if (surfaceRocks
                                && canPlaceRocks
                                && host.getFluidState().isEmpty()
                                && random.nextInt(128) == 0) {
                            String material = random.nextBoolean() && lastOre != null
                                    ? lastOre.getTargetCrushingMaterial().getName()
                                    : lastRock;
                            BlockPos below = cursor.below();
                            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP))
                                placedAny |= GTRockPlacement.place(level, cursor, material, null, false);
                        }
                        // Air, plants and water break the solid column. An ore below a cave must
                        // not be advertised by rocks above an unrelated roof.
                        lastOre = null;
                        canPlaceRocks = false;
                    } else if (host.is(Blocks.BEDROCK)) {
                        canPlaceRocks = true;
                    } else if (REPLACEABLE_STONE.contains(hostBlock)) {
                        canPlaceRocks = true;
                        // The ore background must be the layer's stone (what the host is about
                        // to become), not the pre-replacement host — otherwise ores show the
                        // vanilla rock (e.g. deepslate) inside a GT stone layer.
                        BlockState background = generateLayer && layer.stone() != null ? layer.stone() : host;
                        boolean orePlaced = false;
                        if (generateLayer && scan[5] == scan[1]) {
                            // Interior of one layer: the layer's own ores.
                            boolean core = scan[6] == scan[0];
                            for (int oreIndex = 0; oreIndex < layer.ores().size(); oreIndex++) {
                                Ore ore = layer.ores().get(oreIndex);
                                if (y < ore.minY() || y > ore.maxY()) continue;
                                if (!StoneLayerOrePlacement.enabled(ore.explicitBlock())) continue;
                                if (!ore.rule().biomes().isEmpty()
                                        && !StoneLayerOrePlacement.matches(ore.rule(), level.getBiome(cursor)))
                                    continue;
                                if (random.nextInt(ore.chanceDiv()) != 0) continue;
                                if (placeOre(level, cursor, background, ore, !core)) {
                                    if (ore.indicators()) lastOre = ore.material();
                                    orePlaced = true;
                                    placedAny = true;
                                    break;
                                }
                            }
                        } else if (generateLayer) {
                            // Layer boundary: contact ores (GT6 doublelayers).
                            var below = contacts.get(scan[5].material());
                            List<Ore> contactOres = below == null ? null : below.get(scan[1].material());
                            if (contactOres != null) {
                                for (int oreIndex = 0; oreIndex < contactOres.size(); oreIndex++) {
                                    Ore ore = contactOres.get(oreIndex);
                                    if (y < ore.minY() || y > ore.maxY()) continue;
                                    if (!StoneLayerOrePlacement.enabled(ore.explicitBlock())) continue;
                                    if (!ore.rule().biomes().isEmpty()
                                            && !StoneLayerOrePlacement.matches(ore.rule(), level.getBiome(cursor)))
                                        continue;
                                    if (random.nextInt(ore.chanceDiv()) != 0) continue;
                                    if (placeOre(level, cursor, background, ore, random.nextBoolean())) {
                                        if (ore.indicators()) lastOre = ore.material();
                                        orePlaced = true;
                                        placedAny = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (!orePlaced) {
                            lastRock = generateLayer
                                            && layer.stone() != null
                                            && !(layer.stone().getBlock() instanceof GTStoneBlock)
                                    ? layer.material()
                                    : rockMaterial(background);
                            if (generateLayer
                                    && layer.stone() != null
                                    && !host.is(layer.stone().getBlock())) {
                                level.setBlock(cursor, layer.stone(), 2);
                                placedAny = true;
                            }
                        }
                    } else if (REPLACEABLE_COBBLE.contains(hostBlock)) {
                        canPlaceRocks = true;
                        lastRock = generateLayer && layer.cobble() != null ? rockMaterial(layer.cobble()) : "Stone";
                        if (generateLayer && layer.cobble() != null) {
                            level.setBlock(cursor, layer.cobble(), 2);
                            placedAny = true;
                        }
                    } else if (REPLACEABLE_MOSSY.contains(hostBlock)) {
                        canPlaceRocks = true;
                        lastRock = generateLayer && layer.mossy() != null ? rockMaterial(layer.mossy()) : "Stone";
                        if (generateLayer && layer.mossy() != null) {
                            level.setBlock(cursor, layer.mossy(), 2);
                            placedAny = true;
                        }
                    } else if (hostBlock instanceof GTStoneBlock stone) {
                        canPlaceRocks = stone.variant() == StoneVariant.STONE
                                || stone.variant() == StoneVariant.COBBLE
                                || stone.variant() == StoneVariant.COBBLE_MOSSY;
                        if (canPlaceRocks)
                            lastRock = stone.stoneType().material().getName();
                    } else if (host.isSolidRender(level, cursor)) {
                        canPlaceRocks =
                                GTRocksFeature.rockGround(host) || host.is(Blocks.CLAY) || host.is(Blocks.GRAVEL);
                    } else {
                        lastOre = null;
                        canPlaceRocks = false;
                    }

                    // Existing vanilla structure half-blocks follow the same local host material.
                    // Do not recolor existing GT slabs or other decorative slab families.
                    if (generateLayer
                            && (hostBlock == Blocks.COBBLESTONE_SLAB || hostBlock == Blocks.MOSSY_COBBLESTONE_SLAB)) {
                        BlockState full = hostBlock == Blocks.COBBLESTONE_SLAB ? layer.cobble() : layer.mossy();
                        BlockState replacement = structureSlabState(host, full);
                        if (replacement != null) placedAny |= level.setBlock(cursor, replacement, 2);
                    }

                    // Slide the window up by one block.
                    if (y < MAX_GEN_Y) {
                        System.arraycopy(scan, 1, scan, 0, 6);
                        scan[6] = layers[noise.get(x, y + 4, z, listSize)];
                    }
                }
            }
        }
        return placedAny;
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
