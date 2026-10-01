package com.gregtech.gregtech.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Set;

/**
 * Port of GT6's {@code WorldgenTurf} ({@code Loader_Worldgen:582}, registered as
 * {@code "swamp.turf"}): peat bogs in swamps.
 *
 * <p>GT6 rolls one bog per chunk with {@code nextInt(32) > 0} (about 1 in 32 chunks), requires a
 * swamp biome in the chunk, and then carves the same 48x48 {@link GTPitShape} mask as the clay pits
 * — anchored 16 blocks before the chunk — between {@code waterLevel + 1} and
 * {@code waterLevel - 12}. Per column it turns dirt into {@code BlocksGT.Diggables} meta 2 (Turf,
 * the port's {@code gregtech:turf}), at most two blocks deep, and only continues through rock once
 * it is inside the bog; dirt under wood or gourds is left alone.
 */
public class GTTurfFeature extends Feature<NoneFeatureConfiguration> {
    /** GT6 {@code aRandom.nextInt(32) > 0}. */
    public static final int DIVIDER = TerrainWorldgenRules.TURF_DIVIDER;
    /** GT6's vertical window: {@code waterLevel + 1} down to {@code waterLevel - 12}. */
    public static final int ABOVE_SEA = TerrainWorldgenRules.TURF_ABOVE_SEA;
    public static final int BELOW_SEA = TerrainWorldgenRules.TURF_BELOW_SEA;
    /** GT6 stops a column after two turf blocks ({@code tGenerated < 2}). */
    public static final int MAX_DEPTH = TerrainWorldgenRules.TURF_MAX_DEPTH;
    /** GT6 {@code BlocksGT.Diggables} meta 2 = "Turf" (material Peat). */
    public static final String TURF = "gregtech:turf";

    public GTTurfFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        if (random.nextInt(DIVIDER) > 0) return false;
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        Set<ResourceLocation> biomes = GTWorldgenBiomes.chunkBiomes(level, minX, minZ);
        if (!GTWorldgenBiomes.anyOf(biomes, GTWorldgenBiomes.SWAMP)) return false;

        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(TURF));
        if (block == null || block == Blocks.AIR) return false;
        BlockState fill = block.defaultBlockState();

        int anchorX = minX - 16;
        int anchorZ = minZ - 16;
        int upper = level.getSeaLevel() + ABOVE_SEA;
        int lower = level.getSeaLevel() - BELOW_SEA;
        boolean placed = false;
        for (int i = 0; i < GTPitShape.SIZE; i++) {
            for (int j = 0; j < GTPitShape.SIZE; j++) {
                if (!GTPitShape.at(i, j)) continue;
                placed |= carveColumn(level, anchorX + i, anchorZ + j, upper, lower, fill);
            }
        }
        return placed;
    }

    /**
     * GT6's per-column loop of {@code WorldgenTurf.generate}, public so a GameTest can drive a
     * prepared column instead of hunting for a generated bog (the feature itself is 1 in 32).
     *
     * <p>GT6's window ends strictly at {@code waterLevel - 12} ({@code tY > tLowerBound}).
     *
     * @return true when at least one block was replaced
     */
    public static boolean carveColumn(WorldGenLevel level, int x, int z, int upper, int lower, BlockState fill) {
        BlockState previous = level.getBlockState(new BlockPos(x, upper + 1, z));
        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = upper, generated = 0; y > lower && generated < MAX_DEPTH; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            BlockState last = previous;
            previous = state;
            if (state.is(fill.getBlock())) {
                generated++;
                continue;
            }
            if (!state.isSolidRender(level, cursor)) {
                if (generated > 0) break;
                continue;
            }
            if (isVanillaDirt(state)) {
                // GT6 leaves the dirt under wood or gourds alone.
                if (generated <= 0 && isWoodOrGourd(last)) continue;
            } else if (generated > 0) {
                if (!GTPitFeature.isRock(state)) break;
            } else {
                continue;
            }
            level.setBlock(cursor, fill, 2);
            placed = true;
            generated++;
        }
        return placed;
    }

    /** GT6 tests {@code tBlock == Blocks.dirt} — vanilla dirt and its coarse variant, nothing else. */
    private static boolean isVanillaDirt(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT);
    }

    /** 1.7.10 {@code Material.wood} / {@code Material.gourd}. */
    private static boolean isWoodOrGourd(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS)
                || state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN);
    }

    /** Registry key of the configured feature (data file {@code worldgen/configured_feature/gt_turf.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath("gregtech", "gt_turf"));
}
