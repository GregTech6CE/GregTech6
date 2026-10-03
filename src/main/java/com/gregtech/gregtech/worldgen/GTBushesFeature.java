package com.gregtech.gregtech.worldgen;

import com.gregtech.gregtech.block.plant.BushBlock;
import com.gregtech.gregtech.blockentity.BushBlockEntity;
import com.gregtech.gregtech.content.plant.GTBerryBushes;
import com.gregtech.gregtech.registry.GTBushes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Set;

/**
 * Port of GT6's {@code WorldgenBushes} ({@code Loader_Worldgen:633}).
 *
 * <p>GT6 plants one bush per chunk with a {@code 1/4} roll in plains and woods (never in frozen
 * biomes), picks the berry type with its value noise at {@code (x/2, 300, z/2)} modulo the number of
 * berry types, and then spreads the same bush to up to four horizontal neighbours (and attached
 * branches on each core).
 *
 * <p>The port keeps GT6's noise: {@code gregtech.worldgen.GTCellNoise} is a port of GT6's
 * {@code NoiseGenerator} including its 256-cell offset table, so the same coordinates give the same
 * berry type.
 */
public class GTBushesFeature extends Feature<NoneFeatureConfiguration> {
    /** GT6 {@code new WorldgenBushes("plant.bush", T, 1, 4, ...)}. */
    public static final int AMOUNT = 1;
    public static final int PROBABILITY = 4;
    /** GT6's noise Y for the bush type lookup. */
    public static final float NOISE_Y = 300.0F;

    private static final Set<ResourceLocation> PLAINS = Set.of(
            id("plains"), id("sunflower_plains"), id("meadow"), id("cherry_grove"));
    private static final Set<ResourceLocation> WOODS = Set.of(
            id("forest"), id("flower_forest"), id("birch_forest"), id("old_growth_birch_forest"),
            id("dark_forest"), id("windswept_forest"), id("taiga"), id("old_growth_pine_taiga"),
            id("old_growth_spruce_taiga"));
    private static final Set<ResourceLocation> FROZEN = Set.of(
            id("snowy_plains"), id("snowy_taiga"), id("snowy_beach"), id("ice_spikes"),
            id("snowy_slopes"), id("frozen_peaks"), id("frozen_river"), id("frozen_ocean"),
            id("deep_frozen_ocean"));

    public GTBushesFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        boolean placed = false;
        for (int i = 0; i < AMOUNT; i++) {
            if (random.nextInt(PROBABILITY) != 0) continue;
            int x = minX + random.nextInt(16);
            int z = minZ + random.nextInt(16);
            placed |= placeBush(level, x, z, random);
        }
        return placed;
    }

    /** GT6's biome gate: plains and woods, never frozen. */
    public static boolean bushBiome(ResourceLocation biome) {
        if (biome == null || FROZEN.contains(biome)) return false;
        return PLAINS.contains(biome) || WOODS.contains(biome);
    }

    /** GT6's berry type selection: the world noise at {@code (x/2, 300, z/2)} into the berry list. */
    public static int berryIndex(WorldGenLevel level, int x, int z) {
        var noise = new GTCellNoise(level.getSeed());
        return Math.floorMod(noise.get(x / 2.0F, NOISE_Y, z / 2.0F, GTBerryBushes.worldgenSize()), GTBerryBushes.worldgenSize());
    }

    static boolean placeBush(WorldGenLevel level, int x, int z, RandomSource random) {
        if (!bushBiome(biomeId(level, x, z))) return false;
        BlockPos ground = GTSurfaceFloraFeature.surface(level, x, z);
        if (ground == null || !BushBlock.isPlantableGround(level.getBlockState(ground))) return false;
        String berry = GTBerryBushes.worldgenByIndex(berryIndex(level, x, z)).id();
        if (!placeOne(level, ground.above(), berry)) return false;
        // GT6 spreads the same berry to up to four horizontal neighbours.
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            if (!random.nextBoolean()) continue;
            BlockPos side = ground.relative(direction);
            if (!BushBlock.isPlantableGround(level.getBlockState(side))) continue;
            if (placeOne(level, side.above(), berry)) placeBranches(level, side.above(), berry);
        }
        placeBranches(level, ground.above(), berry);
        return true;
    }

    private static boolean placeOne(WorldGenLevel level, BlockPos pos, String berry) {
        if (!level.getBlockState(pos).canBeReplaced()) return false;
        if (level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK))
            level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 2);
        BlockState state = GTBushes.BUSH.get().defaultBlockState().setValue(BushBlock.STAGE, 3);
        if (!level.setBlock(pos, state, 2)) return false;
        if (level.getBlockEntity(pos) instanceof BushBlockEntity bush) bush.setBerry(berry);
        return true;
    }

    /** GT6 WorldgenBushes.placeBushSides: four horizontal faces and the top of a ripe core. */
    public static void placeBranches(WorldGenLevel level, BlockPos core, String berry) {
        for (var direction : net.minecraft.core.Direction.values()) {
            if (direction == net.minecraft.core.Direction.DOWN) continue;
            BlockPos pos = core.relative(direction);
            if (!level.getBlockState(pos).canBeReplaced()) continue;
            var state = GTBushes.BUSH.get().defaultBlockState().setValue(BushBlock.STAGE, 3)
                    .setValue(BushBlock.SUPPORT, direction.getOpposite().get3DDataValue());
            if (level.setBlock(pos, state, 2) && level.getBlockEntity(pos) instanceof BushBlockEntity bush)
                bush.setBerry(berry);
        }
    }

    private static ResourceLocation biomeId(WorldGenLevel level, int x, int z) {
        Holder<Biome> biome = level.getBiome(new BlockPos(x, level.getSeaLevel(), z));
        return biome.unwrapKey().map(ResourceKey::location).orElse(null);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    /** Registry key of the configured feature (data file {@code worldgen/configured_feature/gt_bushes.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("gregtech", "gt_bushes"));
}
