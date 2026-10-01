package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.content.material.generated.OreMaterials;
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

import java.util.Random;

/**
 * Port of GT6's {@code WorldgenColtan} ({@code Loader_Worldgen:779}) — the "coltan contention"
 * region.
 *
 * <p>GT6 derives one centre point per world from the world seed
 * ({@code new Random(seed + 5).nextGaussian() * 1500} twice) and then treats that spot as a coltan
 * field: within 480 blocks of it every chunk scatters small Coltan/Columbite/Tantalite ores between
 * y = 20 and y = 40, and within 64 blocks a <em>second</em> scatter adds full blocks on top. The
 * chunk carrying the centre also generates a coltan bedrock vein first — that is what the rest of
 * the field points at (the port routes it through {@link GTBedrockOreFeature}, documented
 * deviation).
 */
public class GTColtanFeature extends Feature<NoneFeatureConfiguration> {
    /** GT6 {@code new WorldgenColtan("ore.special.coltan", T, 20, 40, 32, 480, ...)}. */
    public static final int MIN_Y = 20;
    public static final int MAX_Y = 40;
    public static final int AMOUNT = 32;
    public static final int RANGE = 480;
    /** GT6's {@code 64*64} — closer than this the ores also come as full blocks. */
    public static final int LARGE_RANGE = 64;
    /** GT6's {@code switch (aRandom.nextInt(5))}: 3/5 coltan, 1/5 columbite, 1/5 tantalite. */
    public static final int TABLE_SIZE = 5;

    public GTColtanFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    /** GT6's centre point: {@code new Random(seed + 5).nextGaussian() * 1500} for x and z. */
    public static BlockPos centre(long seed) {
        Random random = new Random(seed + 5);
        int x = (int) (random.nextGaussian() * 1500);
        int z = (int) (random.nextGaussian() * 1500);
        return new BlockPos(x, 0, z);
    }

    /** GT6's material table for one scatter roll. */
    public static GTMaterial materialFor(int roll) {
        return switch (roll) {
            case 0 -> OreMaterials.Columbite;
            case 1 -> OreMaterials.Tantalite;
            default -> OreMaterials.Coltan;
        };
    }

    /** GT6's count: {@code max(1, amount/2 + rand(1 + amount)/2)}. */
    public static int count(RandomSource random, int amount) {
        return Math.max(1, amount / 2 + random.nextInt(1 + amount) / 2);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        BlockPos centre = centre(level.getSeed());
        if (isCentreChunk(centre, minX, minZ)) placeCentreVein(level, context.random(), minX, minZ);
        return placeField(level, context.random(), minX, minZ, centre);
    }

    /** GT6's squared distance from a chunk's origin corner to the field centre. */
    public static int distanceSquared(BlockPos centre, int minX, int minZ) {
        return (centre.getX() - minX) * (centre.getX() - minX) + (centre.getZ() - minZ) * (centre.getZ() - minZ);
    }

    /** True when GT6 would treat this chunk as part of the coltan field. */
    public static boolean inRange(BlockPos centre, int minX, int minZ) {
        return distanceSquared(centre, minX, minZ) <= RANGE * RANGE;
    }

    /**
     * GT6's per-chunk scatter, split out of {@link #place} so tests can drive it with an explicit
     * centre (GT6 derives the centre from the world seed, which a GameTest cannot choose).
     *
     * <p>GT6 runs <b>two</b> loops: the small-ore scatter covers the whole 480-block field, and a
     * second full-ore scatter is added — not substituted — inside 64 blocks of the centre.
     *
     * @return true when at least one ore was placed
     */
    public boolean placeField(WorldGenLevel level, RandomSource random, int minX, int minZ, BlockPos centre) {
        int distance = distanceSquared(centre, minX, minZ);
        if (distance > RANGE * RANGE) return false;
        boolean placed = scatter(level, random, minX, minZ, true);
        // Second loop — full blocks near the centre, on top of the small ores above.
        if (distance <= LARGE_RANGE * LARGE_RANGE) placed |= scatter(level, random, minX, minZ, false);
        return placed;
    }

    /** One of GT6's two scatter loops: {@code amount} rolls of the 5-way material table. */
    private boolean scatter(WorldGenLevel level, RandomSource random, int minX, int minZ, boolean small) {
        int minY = GTWorldgenScale.remapY(level, MIN_Y);
        int maxY = Mth.clamp(GTWorldgenScale.remapY(level, MAX_Y), minY + 1, level.getMaxBuildHeight() - 1);
        int count = count(random, AMOUNT);
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            int x = minX + random.nextInt(16);
            int z = minZ + random.nextInt(16);
            int y = minY + random.nextInt(Math.max(1, maxY - minY));
            cursor.set(x, y, z);
            if (level.isOutsideBuildHeight(cursor.getY())) continue;
            GTMaterial material = materialFor(random.nextInt(TABLE_SIZE));
            placed |= GTOreBlockResolver.placeOre(level, cursor, material, small);
        }
        return placed;
    }

    /** GT6: {@code (tX>>4) == (aMinX>>4) && (tZ>>4) == (aMinZ>>4)} — the chunk the centre sits in. */
    public static boolean isCentreChunk(BlockPos centre, int minX, int minZ) {
        return (centre.getX() >> 4) == (minX >> 4) && (centre.getZ() >> 4) == (minZ >> 4);
    }

    /**
     * GT6 asks {@code WorldgenOresBedrock.generateVein(MT.OREMATS.Coltan, …)} for the centre chunk
     * before scattering anything — that vein is what the rest of the field points at. §56 gave the
     * port GT6's real vein shape, so the centre chunk gets exactly what GT6 generates there.
     */
    private void placeCentreVein(WorldGenLevel level, RandomSource random, int minX, int minZ) {
        GTBedrockOreFeature.placeVein(level, random, minX, minZ, OreMaterials.Coltan);
    }

    /** Lowest Y this field can use in {@code level} (GT6's 20, remapped for the modern depth). */
    public static int minY(WorldGenLevel level) {
        return GTWorldgenScale.remapY(level, MIN_Y);
    }

    /** Registry key of the configured feature (data file {@code worldgen/configured_feature/gt_coltan.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath("gregtech", "gt_coltan"));
}
