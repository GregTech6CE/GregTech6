package com.gregtech.gregtech.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Port of GT6's surface flora worldgen: the forest floor pieces of {@code Loader_Worldgen}
 * that are not ores or trees — fallen logs ({@code WorldgenLogDry/Rotten/Mossy/Frozen},
 * {@code :603-606}), twigs on the ground ({@code WorldgenSticks}, {@code :630}) and glowtus on
 * jungle/swamp water ({@code WorldgenGlowtus}, {@code :632}).
 *
 * <p>All of them are GT6 {@code WorldgenOnSurface} generators, so this feature uses the same
 * algorithm as {@link GTTreesFeature}: a number of target columns per chunk, a {@code 1/probability}
 * roll each, a ray cast from the sky down to the first solid (or liquid) block, then GT6's
 * placement rules ({@code plantableGreens} for twigs and logs, water for glowtus).
 *
 * <p>The fallen logs keep GT6's three shapes — a vertical pile that starts one block below the
 * surface, an X-axis run and a Z-axis run, each 3-5 blocks long — and the mossy variant also
 * plants GT6's red/brown mushrooms on top of the run.
 */
public class GTSurfaceFloraFeature extends Feature<NoneFeatureConfiguration> {

    public GTSurfaceFloraFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;

        boolean placed = placeTwigs(level, minX, minZ, random);
        for (GTSurfaceFlora.Log log : GTSurfaceFlora.LOGS) {
            placed |= placeLog(level, minX, minZ, log, random);
        }
        placed |= placeGlowtus(level, minX, minZ, random);
        return placed;
    }

    // ── GT6 WorldgenSticks ────────────────────────────────────────────────────────────────────

    /** GT6 {@code WorldgenSticks}: 2 target columns, ×1-3 by biome, half of them get twigs. */
    public static boolean placeTwigs(WorldGenLevel level, int minX, int minZ, RandomSource random) {
        boolean placed = false;
        for (int i = 0; i < GTSurfaceFlora.TWIG_AMOUNT; i++) {
            if (random.nextInt(GTSurfaceFlora.TWIG_PROBABILITY) != 0) continue;
            int x = minX + random.nextInt(16);
            int z = minZ + random.nextInt(16);
            int multiplier = GTSurfaceFlora.twigMultiplier(biomeId(level, x, z));
            // GT6 scales the column count by the biome multiplier before rolling per column.
            for (int j = 0; j < multiplier - 1; j++) {
                if (random.nextInt(GTSurfaceFlora.TWIG_PROBABILITY) == 0) {
                    placed |= placeTwig(level, minX + random.nextInt(16), minZ + random.nextInt(16));
                }
            }
            if (multiplier == 0) continue;
            placed |= placeTwig(level, x, z);
        }
        return placed;
    }

    private static boolean placeTwig(WorldGenLevel level, int x, int z) {
        BlockPos ground = surface(level, x, z);
        if (ground == null) return false;
        if (!plantableGreens(level.getBlockState(ground))) return false;
        BlockPos pos = ground.above();
        if (!level.getBlockState(pos).isAir()) return false;
        return level.setBlock(pos, GTSurfaceFlora.twigs().defaultBlockState(), 2);
    }

    // ── GT6 WorldgenLogDry / Rotten / Mossy / Frozen ──────────────────────────────────────────

    /** One log per entry per chunk with GT6's {@code 1/probability} roll. */
    public static boolean placeLog(WorldGenLevel level, int minX, int minZ, GTSurfaceFlora.Log log, RandomSource random) {
        if (random.nextInt(log.probability()) != 0) return false;
        int x = minX + random.nextInt(16);
        int z = minZ + random.nextInt(16);
        if (!log.biomes().contains(String.valueOf(biomeId(level, x, z)))) return false;
        BlockPos ground = surface(level, x, z);
        if (ground == null) return false;
        BlockState state = level.getBlockState(ground);
        boolean allowed = plantableGreens(state)
                || (log.sandAllowed() && state.is(BlockTags.SAND))
                || (!log.sandAllowed() && state.is(Blocks.SNOW_BLOCK));
        if (!allowed) return false;
        if (!level.getBlockState(ground.above()).isAir()) return false;
        Block block = GTSurfaceFlora.logBlock(log);
        if (block == null || block == Blocks.AIR) return false;
        return placeShape(level, ground, block, random.nextInt(3), log.mushrooms(), random);
    }

    /**
     * GT6's three fallen-log shapes: 0 = vertical pile, 1 = X-axis run, 2 = Z-axis run.
     * Public so the GameTest can drive the real shape code instead of re-implementing it.
     */
    public static boolean placeShape(WorldGenLevel level, BlockPos ground, Block block, int shape,
                                     boolean mushrooms, RandomSource random) {
        return switch (shape) {
            case 0 -> verticalLog(level, ground, block, random);
            case 1 -> horizontalLog(level, ground, block, Direction.Axis.X, mushrooms, random);
            default -> horizontalLog(level, ground, block, Direction.Axis.Z, mushrooms, random);
        };
    }

    /** GT6 shape 0: 3-5 logs standing on/through the surface (the lowest one may be buried). */
    private static boolean verticalLog(WorldGenLevel level, BlockPos ground, Block block, RandomSource random) {
        BlockState log = block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        if (random.nextBoolean()) set(level, ground.below(), log);
        set(level, ground, log);
        set(level, ground.above(), log);
        set(level, ground.above(2), log);
        if (random.nextBoolean()) set(level, ground.above(3), log);
        return true;
    }

    /** GT6 shapes 1/2: a 3-5 block log run lying on the ground along one axis. */
    private static boolean horizontalLog(WorldGenLevel level, BlockPos ground, Block block,
                                        Direction.Axis axis, boolean mushrooms, RandomSource random) {
        BlockState log = block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
        BlockPos base = ground.above();
        if (random.nextBoolean()) set(level, along(base, axis, -2), log);
        for (int i = -1; i <= 1; i++) set(level, along(base, axis, i), log);
        if (random.nextBoolean()) set(level, along(base, axis, 2), log);
        if (mushrooms) {
            for (int i = -1; i <= 1; i++) {
                if (!random.nextBoolean()) continue;
                BlockPos pos = along(base, axis, i).above();
                if (!level.getBlockState(pos).isAir()) continue;
                level.setBlock(pos, random.nextBoolean() ? Blocks.RED_MUSHROOM.defaultBlockState()
                        : Blocks.BROWN_MUSHROOM.defaultBlockState(), 2);
            }
        }
        return true;
    }

    private static BlockPos along(BlockPos base, Direction.Axis axis, int amount) {
        return switch (axis) {
            case X -> base.offset(amount, 0, 0);
            case Y -> base.offset(0, amount, 0);
            case Z -> base.offset(0, 0, amount);
        };
    }

    // ── GT6 WorldgenGlowtus ───────────────────────────────────────────────────────────────────

    /** GT6 {@code WorldgenGlowtus}: 16 target columns on jungle/swamp water, half of them glowtus. */
    public static boolean placeGlowtus(WorldGenLevel level, int minX, int minZ, RandomSource random) {
        boolean placed = false;
        for (int i = 0; i < GTSurfaceFlora.GLOWTUS_AMOUNT; i++) {
            if (random.nextInt(GTSurfaceFlora.GLOWTUS_PROBABILITY) != 0) continue;
            int x = minX + random.nextInt(16);
            int z = minZ + random.nextInt(16);
            if (!GTSurfaceFlora.glowtusBiome(biomeId(level, x, z))) continue;
            BlockPos water = waterSurface(level, x, z);
            if (water == null) continue;
            BlockPos pos = water.above();
            if (!level.getBlockState(pos).isAir()) continue;
            Block glowtus = GTSurfaceFlora.glowtus(
                    GTSurfaceFlora.GLOWTUS_COLOURS.get(random.nextInt(GTSurfaceFlora.GLOWTUS_COLOURS.size())));
            if (glowtus == null || glowtus == Blocks.AIR) continue;
            placed |= level.setBlock(pos, glowtus.defaultBlockState(), 2);
        }
        return placed;
    }

    // ── shared GT6 helpers ────────────────────────────────────────────────────────────────────

    /** GT6's sky ray cast: the first solid block of the column, or null when it is not placeable ground. */
    public static BlockPos surface(WorldGenLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int y = top; y > level.getMinBuildHeight() + 1; y--) {
            BlockPos pos = new BlockPos(x, y - 1, z);
            BlockState state = level.getBlockState(pos);
            if (state.liquid()) return null; // GT6 ignores liquids for the solid-surface generators
            if (!state.isFaceSturdy(level, pos, Direction.UP) && !state.is(BlockTags.LEAVES)
                    && !state.is(BlockTags.LOGS)) {
                continue;
            }
            return pos;
        }
        return null;
    }

    /** GT6's water ray: the topmost water source block of the column (glowtus floats on it). */
    public static BlockPos waterSurface(WorldGenLevel level, int x, int z) {
        // MOTION_BLOCKING_NO_LEAVES already counts fluids, so this starts right above the water.
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int y = top; y > level.getMinBuildHeight(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            var fluid = state.getFluidState();
            if (!fluid.isEmpty() && fluid.is(FluidTags.WATER)) return fluid.isSource() ? pos : null;
            // GT6's ray ignores everything that is not opaque (flowers, glowtus, air) and keeps going.
            if (state.isAir() || !state.isSolidRender(level, pos)) continue;
            return null;
        }
        return null;
    }

    /** GT6 {@code BlocksGT.plantableGreens}: grass/dirt-like ground the log and twig generators accept. */
    public static boolean plantableGreens(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MOSS_BLOCK);
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (level.isOutsideBuildHeight(pos)) return;
        level.setBlock(pos, state, 2);
    }

    private static ResourceLocation biomeId(WorldGenLevel level, int x, int z) {
        Holder<Biome> biome = level.getBiome(new BlockPos(x, level.getSeaLevel(), z));
        return biome.unwrapKey().map(ResourceKey::location).orElse(null);
    }

    /** Registry key of the configured feature (data file {@code worldgen/configured_feature/gt_surface_flora.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("gregtech", "gt_surface_flora"));
}
