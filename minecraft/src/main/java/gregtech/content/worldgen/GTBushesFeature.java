package gregtech.content.worldgen;

import gregtech.content.block.plant.BushBlock;
import gregtech.content.plant.GTBerryBushes;
import gregtech.content.registry.GTBushes;
import java.util.Set;
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

/**
 * Port of GT6's {@code WorldgenBushes} ({@code Loader_Worldgen:633}).
 *
 * <p>The current feature attempts one column per chunk with a {@code 1/4} roll, checks its biome
 * allowlist and surface, then places a ripe core, optional horizontal cores and attached branches.
 *
 * <p>{@code GTCellNoise} retains the original cell algorithm and offset table. This caller uses
 * floating-point half-coordinates, while GT6 used integer division; identical world coordinates
 * therefore do not guarantee the same selection. Biome and surface policies are audited separately.
 */
public class GTBushesFeature extends Feature<NoneFeatureConfiguration> {
    /** GT6 {@code new WorldgenBushes("plant.bush", T, 1, 4, ...)}. */
    public static final int AMOUNT = 1;

    public static final int PROBABILITY = 4;
    /** GT6's noise Y for the bush type lookup. */
    public static final float NOISE_Y = 300.0F;

    private static final Set<ResourceLocation> PLAINS =
            Set.of(id("plains"), id("sunflower_plains"), id("meadow"), id("cherry_grove"));
    private static final Set<ResourceLocation> WOODS = Set.of(
            id("forest"),
            id("flower_forest"),
            id("birch_forest"),
            id("old_growth_birch_forest"),
            id("dark_forest"),
            id("windswept_forest"),
            id("taiga"),
            id("old_growth_pine_taiga"),
            id("old_growth_spruce_taiga"));
    private static final Set<ResourceLocation> FROZEN = Set.of(
            id("snowy_plains"),
            id("snowy_taiga"),
            id("snowy_beach"),
            id("ice_spikes"),
            id("snowy_slopes"),
            id("frozen_peaks"),
            id("frozen_river"),
            id("frozen_ocean"),
            id("deep_frozen_ocean"));

    public GTBushesFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!gregtech.GregTechConfig.Worldgen.VEGETATION.enabled() || !gregtech.GregTechConfig.Rule.BUSHES.enabled())
            return false;
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        boolean placed = false;
        for (int i = 0; i < gregtech.GregTechConfig.Number.BUSH_AMOUNT.value(); i++) {
            if (random.nextInt(gregtech.GregTechConfig.Number.BUSH_PROBABILITY.value()) != 0) continue;
            int x = minX + random.nextInt(16);
            int z = minZ + random.nextInt(16);
            placed |= placeBush(level, x, z, random);
        }
        return placed;
    }

    /** Current candidate-column biome allowlist, distinct from GT6's chunk-name gate. */
    public static boolean bushBiome(ResourceLocation biome) {
        if (biome == null || FROZEN.contains(biome)) return false;
        return PLAINS.contains(biome) || WOODS.contains(biome);
    }

    /** Current lookup uses floating-point half-coordinates and the registered worldgen list. */
    public static int berryIndex(WorldGenLevel level, int x, int z) {
        var noise = new GTCellNoise(level.getSeed());
        return Math.floorMod(
                noise.get(x / 2.0F, NOISE_Y, z / 2.0F, GTBerryBushes.worldgenSize()), GTBerryBushes.worldgenSize());
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
        BlockState state = GTBushes.byBerry(berry).defaultBlockState().setValue(BushBlock.STAGE, 3);
        if (!level.setBlock(pos, state, 2)) return false;
        return true;
    }

    /** GT6 WorldgenBushes.placeBushSides: four horizontal faces and the top of a ripe core. */
    public static void placeBranches(WorldGenLevel level, BlockPos core, String berry) {
        for (var direction : net.minecraft.core.Direction.values()) {
            if (direction == net.minecraft.core.Direction.DOWN) continue;
            BlockPos pos = core.relative(direction);
            if (!level.getBlockState(pos).canBeReplaced()) continue;
            var state = GTBushes.byBerry(berry)
                    .defaultBlockState()
                    .setValue(BushBlock.STAGE, 3)
                    .setValue(BushBlock.SUPPORT, direction.getOpposite().get3DDataValue());
            level.setBlock(pos, state, 2);
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
            ResourceKey.create(
                    Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("gt6ce", "gt_bushes"));
}
