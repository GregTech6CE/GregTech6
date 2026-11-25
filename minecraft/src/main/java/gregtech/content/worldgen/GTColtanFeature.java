package gregtech.content.worldgen;

import static gregtech.content.worldgen.SpecialOreDeposits.COLTAN_END_LEGACY_Y;
import static gregtech.content.worldgen.SpecialOreDeposits.COLTAN_FIRST_LEGACY_Y;
import static gregtech.content.worldgen.SpecialOreDeposits.COLTAN_FULL_ORE_RADIUS;
import static gregtech.content.worldgen.SpecialOreDeposits.COLTAN_RADIUS;

import gregtech.content.material.generated.OreMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Port of GT6's {@code WorldgenColtan} ({@code Loader_Worldgen}) — the "coltan contention"
 * region.
 *
 * <p>GT6 derives one centre point per world from the world seed
 * ({@code new Random(seed + 5).nextGaussian() * 1500} twice) and then treats that spot as a coltan
 * field: within 480 blocks of it every chunk scatters small Coltan/Columbite/Tantalite ores between
 * y = 20 and y = 40, and within 64 blocks a <em>second</em> scatter adds full blocks on top. The
 * chunk carrying the centre also generates a coltan bedrock vein before the ordinary bedrock
 * attempts, through {@link GTBedrockOreFeature}.
 */
public class GTColtanFeature extends Feature<NoneFeatureConfiguration> {
    public GTColtanFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    /** GT6's centre point: {@code new Random(seed + 5).nextGaussian() * 1500} for x and z. */
    public static BlockPos centre(long seed) {
        var point = MineralWorldgenRules.coltanCentre(seed);
        return new BlockPos(point.x(), 0, point.z());
    }

    /** GT6's count: {@code max(1, amount/2 + rand(1 + amount)/2)}. */
    public static int count(RandomSource random, int amount) {
        return MineralWorldgenRules.scatterAttempts(amount, random::nextInt);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!gregtech.GregTechConfig.Worldgen.GEOLOGY.enabled()) return false;
        WorldGenLevel level = context.level();
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        BlockPos centre = centre(level.getSeed());
        boolean centrePlaced = isCentreChunk(centre, minX, minZ)
                && GTBedrockOreFeature.placeVein(level, context.random(), minX, minZ, OreMaterials.Coltan);
        return placeField(level, context.random(), minX, minZ, centre) || centrePlaced;
    }

    /** GT6's squared distance from a chunk's origin corner to the field centre. */
    public static long distanceSquared(BlockPos centre, int minX, int minZ) {
        return MineralWorldgenRules.distanceSquared(centre.getX(), centre.getZ(), minX, minZ);
    }

    /** True when GT6 would treat this chunk as part of the coltan field. */
    public static boolean inRange(BlockPos centre, int minX, int minZ) {
        return distanceSquared(centre, minX, minZ) <= COLTAN_RADIUS * COLTAN_RADIUS;
    }

    /**
     * GT6's per-chunk scatter, split out of {@link #place} so it can be driven with an explicit
     * centre (GT6 derives the centre from the world seed).
     *
     * <p>GT6 runs <b>two</b> loops: the small-ore scatter covers the whole 480-block field, and a
     * second full-ore scatter is added — not substituted — inside 64 blocks of the centre.
     *
     * @return true when at least one ore was placed
     */
    public boolean placeField(WorldGenLevel level, RandomSource random, int minX, int minZ, BlockPos centre) {
        long distance = distanceSquared(centre, minX, minZ);
        if (distance > COLTAN_RADIUS * COLTAN_RADIUS) return false;
        boolean placed = scatter(level, random, minX, minZ, true);
        // Second loop — full blocks near the centre, on top of the small ores above.
        if (distance <= COLTAN_FULL_ORE_RADIUS * COLTAN_FULL_ORE_RADIUS)
            placed |= scatter(level, random, minX, minZ, false);
        return placed;
    }

    /** One of GT6's two scatter loops: {@code amount} rolls of the 5-way material table. */
    private boolean scatter(WorldGenLevel level, RandomSource random, int minX, int minZ, boolean small) {
        int minY = minY(level);
        int maxY =
                Mth.clamp(GTWorldgenScale.remapY(level, COLTAN_END_LEGACY_Y), minY + 1, level.getMaxBuildHeight() - 1);
        int count = count(random, SpecialOreDeposits.COLTAN_AMOUNT);
        var materials = SpecialOreDeposits.COLTAN_ROLLS;
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            int x = minX + random.nextInt(16);
            int z = minZ + random.nextInt(16);
            int y = minY + random.nextInt(Math.max(1, maxY - minY));
            cursor.set(x, y, z);
            if (level.isOutsideBuildHeight(cursor.getY())) continue;
            var material = materials.get(random.nextInt(materials.size()));
            placed |= GTOreBlockResolver.placeOre(level, cursor, material, small);
        }
        return placed;
    }

    /** GT6: {@code (tX>>4) == (aMinX>>4) && (tZ>>4) == (aMinZ>>4)} — the chunk the centre sits in. */
    public static boolean isCentreChunk(BlockPos centre, int minX, int minZ) {
        return (centre.getX() >> 4) == (minX >> 4) && (centre.getZ() >> 4) == (minZ >> 4);
    }

    /** Lowest Y this field can use in {@code level} (GT6's 20, remapped for the modern depth). */
    public static int minY(WorldGenLevel level) {
        return GTWorldgenScale.remapY(level, COLTAN_FIRST_LEGACY_Y);
    }

    /** Registry key of the configured feature (data file {@code worldgen/configured_feature/gt_coltan.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(
                    Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("gt6ce", "gt_coltan"));
}
