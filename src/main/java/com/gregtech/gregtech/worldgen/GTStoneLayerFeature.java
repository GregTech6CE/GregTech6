package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.block.stone.StoneType;
import com.gregtech.gregtech.block.stone.StoneVariant;
import com.gregtech.gregtech.registry.GTBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
 */
public class GTStoneLayerFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_GEN_Y = GTWorldgenScale.WORLD_FLOOR + 1;
    private static final int MAX_GEN_Y = 112;

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

    /** Below this, noDeep layers (coal seams etc.) keep the host — GT6 used y<24. */
    private static final int NO_DEEP_Y = GTWorldgenScale.remapY(24);

    private record Ore(GTMaterial material, int chanceDiv, int minY, int maxY) {}

    private record ContactKey(String above, String below) {}

    private static volatile Layer[] LAYER_CACHE;
    private static volatile Map<ContactKey, List<Ore>> CONTACT_CACHE;
    private static final Map<Long, GTCellNoise> NOISE_BY_SEED = new ConcurrentHashMap<>();

    public GTStoneLayerFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        Layer[] layers = layers();
        Map<ContactKey, List<Ore>> contacts = contacts();
        GTCellNoise noise = NOISE_BY_SEED.computeIfAbsent(level.getSeed(), GTCellNoise::new);
        int listSize = layers.length;

        int chunkMinX = origin.getX() & ~15;
        int chunkMinZ = origin.getZ() & ~15;
        boolean placedAny = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Layer[] scan = new Layer[7];

        for (int dx = 0; dx < 16; dx++) {
            int x = chunkMinX + dx;
            for (int dz = 0; dz < 16; dz++) {
                int z = chunkMinZ + dz;

                // scan[k] = layer at y + (k - 3); start so that scan[3] is MIN_GEN_Y.
                for (int k = 0; k < 7; k++) {
                    scan[k] = layers[noise.get(x, MIN_GEN_Y + k - 3, z, listSize)];
                }

                for (int y = MIN_GEN_Y; y < MAX_GEN_Y; y++) {
                    cursor.set(x, y, z);
                    BlockState host = level.getBlockState(cursor);
                    Block hostBlock = host.getBlock();
                    Layer layer = scan[3];

                    // Flammable / soft seams keep the host at depth (GT6 NoDeep).
                    if (layer.noDeep() && y < NO_DEEP_Y) {
                        System.arraycopy(scan, 1, scan, 0, 6);
                        scan[6] = layers[noise.get(x, y + 4, z, listSize)];
                        continue;
                    }

                    if (REPLACEABLE_STONE.contains(hostBlock)) {
                        // The ore background must be the layer's stone (what the host is about
                        // to become), not the pre-replacement host — otherwise ores show the
                        // vanilla rock (e.g. deepslate) inside a GT stone layer.
                        BlockState background = layer.stone() != null ? layer.stone() : host;
                        boolean orePlaced = false;
                        if (scan[5] == scan[1]) {
                            // Interior of one layer: the layer's own ores.
                            boolean core = scan[6] == scan[0];
                            for (Ore ore : layer.ores()) {
                                if (y < ore.minY() || y > ore.maxY()) continue;
                                if (random.nextInt(ore.chanceDiv()) != 0) continue;
                                if (GTOreBlockResolver.placeOre(level, cursor, background, ore.material(), !core)) {
                                    orePlaced = true;
                                    placedAny = true;
                                }
                                break;
                            }
                        } else {
                            // Layer boundary: contact ores (GT6 doublelayers).
                            List<Ore> contactOres = contacts.get(new ContactKey(scan[5].material(), scan[1].material()));
                            if (contactOres != null) {
                                for (Ore ore : contactOres) {
                                    if (y < ore.minY() || y > ore.maxY()) continue;
                                    if (random.nextInt(ore.chanceDiv()) != 0) continue;
                                    if (GTOreBlockResolver.placeOre(level, cursor, background, ore.material(), random.nextBoolean())) {
                                        orePlaced = true;
                                        placedAny = true;
                                    }
                                    break;
                                }
                            }
                        }
                        if (!orePlaced && layer.stone() != null && !host.is(layer.stone().getBlock())) {
                            level.setBlock(cursor, layer.stone(), 2);
                            placedAny = true;
                        }
                    } else if (REPLACEABLE_COBBLE.contains(hostBlock)) {
                        if (layer.cobble() != null) {
                            level.setBlock(cursor, layer.cobble(), 2);
                            placedAny = true;
                        }
                    } else if (REPLACEABLE_MOSSY.contains(hostBlock)) {
                        if (layer.mossy() != null) {
                            level.setBlock(cursor, layer.mossy(), 2);
                            placedAny = true;
                        }
                    }

                    // Slide the window up by one block.
                    System.arraycopy(scan, 1, scan, 0, 6);
                    scan[6] = layers[noise.get(x, y + 4, z, listSize)];
                }

                // GT6 ground pebbles: ~1/128 columns get a rock of the local surface layer.
                if (random.nextInt(128) == 0) {
                    GTMaterial rockMaterial = GTMaterialRegistry.get(scan[3].material());
                    if (rockMaterial != null && rockMaterial.resolve().isValid()) {
                        GTRockPlacement.placeRock(level, x, z, rockMaterial.resolve());
                    }
                }
            }
        }
        return placedAny;
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
                    Block seam = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                            new net.minecraft.resources.ResourceLocation(com.gregtech.gregtech.GregTech.MODID, def.blockId()));
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

    private static Map<ContactKey, List<Ore>> contacts() {
        Map<ContactKey, List<Ore>> cached = CONTACT_CACHE;
        if (cached != null) return cached;
        synchronized (GTStoneLayerFeature.class) {
            if (CONTACT_CACHE != null) return CONTACT_CACHE;
            Map<ContactKey, List<Ore>> map = new HashMap<>();
            for (GTStoneLayersGen.ContactDef def : GTStoneLayersGen.CONTACTS) {
                List<Ore> ores = resolveOres(def.ores());
                if (ores.isEmpty()) continue;
                map.merge(new ContactKey(def.materialA(), def.materialB()), ores, GTStoneLayerFeature::concat);
                if (def.bothSides()) {
                    map.merge(new ContactKey(def.materialB(), def.materialA()), ores, GTStoneLayerFeature::concat);
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
            out.add(new Ore(material.resolve(), Math.max(1, def.chanceDiv()),
                    GTWorldgenScale.remapY(def.minY()), GTWorldgenScale.remapY(def.maxY())));
        }
        return List.copyOf(out);
    }
}
