package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.worldgen.GTOreVeins.OreVein;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

/**
 * Port of GT6 {@code WorldgenOresLarge} + the vein-grid selection from {@code GT6WorldGenerator}.
 *
 * <p>Vein origins lie on a 3x3 chunk grid (chunks where {@code floorMod(chunkX,3)==1 &&
 * floorMod(chunkZ,3)==1}). For every origin a single vein type is selected from
 * {@link GTOreVeins#OVERWORLD_VEINS} by weighted random, deterministically seeded from the world
 * seed and the origin chunk coordinates, so all chunks the (up to 80-block wide) vein touches
 * agree on the vein type and its base Y level.
 *
 * <p>The blob shape is GT6's: a 3-block-tall bottom ore layer (y0-1..y0+1), a 3-block-tall top ore
 * layer (y0+3..y0+5), a "between" ore in the middle (y0+2..y0+3) and a "spread" ore over the whole
 * height (y0-1..y0+5), with the placement probability rising toward the vein center
 * ({@code rand(max(1, distToEdge / density)) == 0}).
 *
 * <p>This feature is placed once per chunk with no placement modifiers; it scans the 5x5 chunk
 * neighbourhood for grid origins and writes only the part of each vein that intersects the
 * current chunk (exactly like GT6, which clipped to the generating chunk's bounds).
 */
public class GTOreVeinFeature extends Feature<NoneFeatureConfiguration> {

    public GTOreVeinFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();

        // Per-dimension vein tables (GT6 ORE_OVERWORLD / ORE_END; the Nether had no large veins).
        java.util.List<OreVein> veins;
        int totalWeight;
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim = level.getLevel().dimension();
        if (dim == net.minecraft.world.level.Level.END) {
            veins = GTOreVeins.END_VEINS;
            totalWeight = GTOreVeins.TOTAL_END_VEIN_WEIGHT;
        } else if (dim == net.minecraft.world.level.Level.NETHER) {
            return false;
        } else {
            veins = GTOreVeins.OVERWORLD_VEINS;
            totalWeight = GTOreVeins.TOTAL_VEIN_WEIGHT;
        }
        if (veins.isEmpty() || totalWeight <= 0) return false;

        RandomSource chunkRandom = context.random();
        ChunkPos chunkPos = new ChunkPos(context.origin());
        long worldSeed = level.getSeed();

        boolean placedAny = false;
        // GT6 scanned chunk offsets -2..+2 in both axes (tX/tZ from -32 to +32 step 16).
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int originChunkX = chunkPos.x + dx;
                int originChunkZ = chunkPos.z + dz;
                // GT6: ((chunkX + 402653184) % 3 == 1) — 402653184 is a multiple of 3, so this is floorMod.
                if (Math.floorMod(originChunkX, 3) != 1 || Math.floorMod(originChunkZ, 3) != 1) continue;

                Random originRandom = gridRandom(worldSeed, originChunkX, originChunkZ);
                OreVein vein = selectVein(originRandom, veins, totalWeight);
                if (vein == null) continue;

                placedAny |= generateVein(level, chunkRandom, chunkPos, originChunkX, originChunkZ, originRandom, vein);
            }
        }
        return placedAny;
    }

    /** GT6 weighted selection: {@code w = rand(total); for each vein: w -= weight; if (w <= 0) pick}. */
    private static OreVein selectVein(Random originRandom, java.util.List<OreVein> veins, int totalWeight) {
        int w = originRandom.nextInt(totalWeight);
        for (OreVein vein : veins) {
            w -= vein.weight();
            if (w <= 0) return vein;
        }
        return null;
    }

    private boolean generateVein(WorldGenLevel level, RandomSource chunkRandom, ChunkPos chunkPos,
                                 int originChunkX, int originChunkZ, Random originRandom, OreVein vein) {
        // Deterministic per-origin base Y (GT6: mMinY + rand(mMaxY - mMinY - 5)),
        // stretched into the extended [-64,112] band in the Overworld only.
        int minY = GTWorldgenScale.remapY(level, vein.minY());
        int maxY = GTWorldgenScale.remapY(level, vein.maxY());
        int yBase = minY + originRandom.nextInt(Math.max(1, maxY - minY - 5));

        int originBlockX = originChunkX << 4;
        int originBlockZ = originChunkZ << 4;
        int chunkMinX = chunkPos.getMinBlockX(), chunkMaxX = chunkPos.getMaxBlockX();
        int chunkMinZ = chunkPos.getMinBlockZ(), chunkMaxZ = chunkPos.getMaxBlockZ();
        int size = vein.size();
        int density = vein.density();

        boolean placedAny = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // GT6 loop structure: vein extends from origin-rand(size) to origin+16+rand(size); the
        // extents are re-rolled with the per-chunk random (ragged edges, exactly like GT6).
        int cX = originBlockX - chunkRandom.nextInt(size);
        int eX = originBlockX + 16 + chunkRandom.nextInt(size);
        for (int tX = Math.max(chunkMinX, cX); tX <= Math.min(chunkMaxX, eX); tX++) {
            int cZ = originBlockZ - chunkRandom.nextInt(size);
            int eZ = originBlockZ + 16 + chunkRandom.nextInt(size);
            for (int tZ = Math.max(chunkMinZ, cZ); tZ <= Math.min(chunkMaxZ, eZ); tZ++) {
                // Bottom ore layer: yBase-1 .. yBase+1
                for (int y = yBase - 1; y < yBase + 2; y++) {
                    if (densityHit(chunkRandom, cX, eX, tX, cZ, eZ, tZ, density)) {
                        placedAny |= setOre(level, cursor.set(tX, y, tZ), vein.bottom());
                    }
                }
                // Top ore layer: yBase+3 .. yBase+5
                for (int y = yBase + 3; y < yBase + 6; y++) {
                    if (densityHit(chunkRandom, cX, eX, tX, cZ, eZ, tZ, density)) {
                        placedAny |= setOre(level, cursor.set(tX, y, tZ), vein.top());
                    }
                }
                // Between layer: yBase+2 .. yBase+3
                if (densityHit(chunkRandom, cX, eX, tX, cZ, eZ, tZ, density)) {
                    placedAny |= setOre(level, cursor.set(tX, yBase + 2 + chunkRandom.nextInt(2), tZ), vein.between());
                }
                // Spread ore over the full height: yBase-1 .. yBase+5
                if (densityHit(chunkRandom, cX, eX, tX, cZ, eZ, tZ, density)) {
                    placedAny |= setOre(level, cursor.set(tX, yBase - 1 + chunkRandom.nextInt(7), tZ), vein.spread());
                }
                // Surface indicator pebble (GT6 rocks above veins, ~1/128 per vein column).
                if (placedAny && chunkRandom.nextInt(128) == 0) {
                    GTMaterial rockMaterial = switch (chunkRandom.nextInt(4)) {
                        case 0 -> vein.top();
                        case 1 -> vein.bottom();
                        case 2 -> vein.between();
                        default -> vein.spread();
                    };
                    GTRockPlacement.placeRock(level, tX, tZ, rockMaterial);
                }
            }
        }
        return placedAny;
    }

    /** GT6 density check: more likely to hit near the vein center, scaled by the density divisor. */
    private static boolean densityHit(RandomSource random, int cX, int eX, int tX, int cZ, int eZ, int tZ, int density) {
        return random.nextInt(Math.max(1, Math.max(Math.abs(cZ - tZ), Math.abs(eZ - tZ)) / density)) == 0
            || random.nextInt(Math.max(1, Math.max(Math.abs(cX - tX), Math.abs(eX - tX)) / density)) == 0;
    }

    private static boolean setOre(WorldGenLevel level, BlockPos pos, com.gregtech.gregtech.api.material.GTMaterial material) {
        if (level.isOutsideBuildHeight(pos.getY())) return false;
        return GTOreBlockResolver.placeOre(level, pos, material, false);
    }

    /**
     * Deterministic per-grid-origin random, following GT6 {@code WD.random(world, chunkX, chunkZ)}:
     * a first random warmed up from the seed provides multipliers for the chunk coordinates,
     * the combination seeds the actual random.
     */
    static Random gridRandom(long worldSeed, long chunkX, long chunkZ) {
        Random random = new Random(worldSeed);
        for (int i = 0; i < 50; i++) random.nextInt(0x00ffffff);
        long mulX = random.nextLong() | 1L;
        long mulZ = random.nextLong() | 1L;
        random = new Random(worldSeed ^ (mulX * chunkX + mulZ * chunkZ));
        for (int i = 0; i < 10; i++) random.nextInt(0x00ffffff);
        return random;
    }
}
