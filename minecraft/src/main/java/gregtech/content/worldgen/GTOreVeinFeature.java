package gregtech.content.worldgen;

import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.worldgen.GTOreVeins.OreVein;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Port of GT6 {@code WorldgenOresLarge} + the vein-grid selection from {@code GT6WorldGenerator}.
 *
 * <p>Vein origins lie on a 3x3 chunk grid (chunks where {@code floorMod(chunkX,3)==1 &&
 * floorMod(chunkZ,3)==1}). For every origin a single vein type is selected from
 * {@link GTOreVeins#activeOverworldVeins()} by weighted random, deterministically seeded from the world
 * seed and the origin chunk coordinates, so all chunks the (up to 80-block wide) vein touches
 * agree on the vein type and its base Y level.</p>
 *
 * <p>The blob shape is GT6's: a 3-block-tall bottom ore layer (y0-1..y0+1), a 3-block-tall top ore
 * layer (y0+3..y0+5), a "between" ore in the middle (y0+2..y0+3) and a "spread" ore over the whole
 * height (y0-1..y0+5), with the placement probability rising toward the vein center
 * ({@code rand(max(1, distToEdge / density)) == 0}).</p>
 *
 * <p>Like GT6, each vein origin also places 1-3 surface indicator rocks in the current chunk
 * before the vein body: two thirds carry a vein material (top weighted twice), one third is a
 * plain rock, and the scan from {@code yBase+25} down to {@code yBase-10} only finds a surface
 * contact when the vein is shallow enough.</p>
 *
 * <p>This feature is placed once per chunk with no placement modifiers; it scans the 5x5 chunk
 * neighbourhood for grid origins and writes only the part of each vein that intersects the
 * current chunk (exactly like GT6, which clipped to the generating chunk's bounds).</p>
 */
public class GTOreVeinFeature extends Feature<NoneFeatureConfiguration> {

    public GTOreVeinFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!gregtech.GregTechConfig.Worldgen.GEOLOGY.enabled()) return false;
        WorldGenLevel level = context.level();

        // Per-dimension vein tables: built-ins where they exist, then addon veins for this dimension.
        String dimension = level.getLevel().dimension().location().toString();
        java.util.List<OreVein> veins = GTOreVeins.activeVeins(dimension);
        int totalWeight = OreVeinDeclaration.END.equals(dimension)
                ? GTOreVeins.endSelectionWeight(veins)
                : veins.stream().mapToInt(OreVein::weight).sum();
        if (veins.isEmpty() || totalWeight <= 0) return false;

        RandomSource chunkRandom = context.random();
        ChunkPos chunkPos = new ChunkPos(context.origin());

        boolean placedAny = false;
        // GT6 scanned chunk offsets -2..+2 in both axes (tX/tZ from -32 to +32 step 16).
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int originChunkX = chunkPos.x + dx;
                int originChunkZ = chunkPos.z + dz;
                // GT6: ((chunkX + 402653184) % 3 == 1) — 402653184 is a multiple of 3, so this is floorMod.
                if (Math.floorMod(originChunkX, 3) != 1 || Math.floorMod(originChunkZ, 3) != 1) continue;

                Random originRandom = originRandom(level, originChunkX, originChunkZ);
                OreVein vein = selectVein(originRandom, veins, totalWeight);
                if (vein == null) continue;

                placedAny |= generateVein(level, chunkRandom, chunkPos, originChunkX, originChunkZ, vein);
            }
        }
        return placedAny;
    }

    /** Empty End rolls deliberately produce no vein. */
    private static OreVein selectVein(Random originRandom, java.util.List<OreVein> veins, int totalWeight) {
        return MineralWorldgenRules.selectVein(veins, originRandom.nextInt(totalWeight));
    }

    private boolean generateVein(
            WorldGenLevel level,
            RandomSource chunkRandom,
            ChunkPos chunkPos,
            int originChunkX,
            int originChunkZ,
            OreVein vein) {
        // GT6 uses a fresh WD.random(origin) for the base Y, so the first nextInt is independent
        // of the weighted vein selection.
        int minY = GTWorldgenScale.remapY(level, vein.minY());
        int maxY = GTWorldgenScale.remapY(level, vein.maxY());
        Random baseYRandom = originRandom(level, originChunkX, originChunkZ);
        int yBase = minY + baseYRandom.nextInt(Math.max(1, maxY - minY - 5));

        // GT6 places the indicator rocks before the vein body.
        placeIndicatorRocks(level, chunkRandom, chunkPos, yBase, vein);

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
            }
        }
        return placedAny;
    }

    /**
     * GT6 {@code WorldgenOresLarge}'s indicator rocks: 1-3 candidates in the current chunk,
     * scanning from {@code yBase+25} down to {@code yBase-10} for an opaque grass/ground/sand/rock
     * contact with a replaceable block above it. Two thirds carry a vein material
     * ({@code select(top, top, bottom, between, spread)}); one third is a plain rock.
     */
    private static void placeIndicatorRocks(
            WorldGenLevel level, RandomSource random, ChunkPos chunkPos, int yBase, OreVein vein) {
        int count = LargeVeinIndicatorRules.count(random::nextInt);
        int chunkMinX = chunkPos.getMinBlockX();
        int chunkMinZ = chunkPos.getMinBlockZ();
        int startY = Math.min(level.getMaxBuildHeight() - 1, LargeVeinIndicatorRules.scanStart(yBase));
        int stopY = Math.max(level.getMinBuildHeight() + 1, yBase - 10);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int i = 0; i < count; i++) {
            int x = chunkMinX + random.nextInt(16);
            int z = chunkMinZ + random.nextInt(16);
            for (int y = startY; y >= stopY; y--) {
                cursor.set(x, y, z);
                BlockState contact = level.getBlockState(cursor);
                if (contact.liquid()) break;
                if (!contact.isSolidRender(level, cursor)) continue;
                if (!indicatorGround(contact)) break;
                BlockPos above = cursor.above();
                if (replaceable(level.getBlockState(above))) {
                    if (LargeVeinIndicatorRules.hasMaterial(random::nextInt)) {
                        GTMaterial material = LargeVeinIndicatorRules.material(
                                random::nextInt, vein.top(), vein.bottom(), vein.between(), vein.spread());
                        GTRockPlacement.place(level, above, material == null ? null : material.getName(), null, false);
                    } else {
                        GTRockPlacement.place(level, above, null, null, false);
                    }
                }
                break;
            }
        }
    }

    /** GT6's {@code Material.grass / ground / sand / rock} contact test. */
    private static boolean indicatorGround(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.GRAVEL)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.STONE_ORE_REPLACEABLES)
                || state.getBlock() instanceof GTStoneBlock;
    }

    /** GT6 {@code WD.easyRep}: air, plants, snow, fire and leaves may be replaced by a rock. */
    private static boolean replaceable(BlockState state) {
        return state.isAir() || state.is(BlockTags.REPLACEABLE) || state.is(Blocks.SNOW) || state.is(BlockTags.LEAVES);
    }

    /** GT6 density check: more likely to hit near the vein center, scaled by the density divisor. */
    private static boolean densityHit(
            RandomSource random, int cX, int eX, int tX, int cZ, int eZ, int tZ, int density) {
        return random.nextInt(Math.max(1, Math.max(Math.abs(cZ - tZ), Math.abs(eZ - tZ)) / density)) == 0
                || random.nextInt(Math.max(1, Math.max(Math.abs(cX - tX), Math.abs(eX - tX)) / density)) == 0;
    }

    private static boolean setOre(WorldGenLevel level, BlockPos pos, gregtech.api.material.GTMaterial material) {
        if (level.isOutsideBuildHeight(pos.getY())) return false;
        return GTOreBlockResolver.placeOre(level, pos, material, false);
    }

    /** GT6 {@code WD.random(World, origin)}: dimension-xored seed, chunk coordinates. */
    private static Random originRandom(WorldGenLevel level, long chunkX, long chunkZ) {
        return Gt6Random.forChunk(level.getSeed(), dimensionId(level.getLevel().dimension()), chunkX, chunkZ);
    }

    /** GT6's numeric dimension id; custom dimensions fall back to the overworld id. */
    private static int dimensionId(ResourceKey<Level> dimension) {
        if (dimension == Level.OVERWORLD) return 0;
        if (dimension == Level.NETHER) return -1;
        if (dimension == Level.END) return 1;
        return 0;
    }

    /**
     * Legacy CE grid random, still used by {@link GTBedrockOreFeature} for its deposit selection.
     * The vein selection uses {@link Gt6Random} instead.
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
