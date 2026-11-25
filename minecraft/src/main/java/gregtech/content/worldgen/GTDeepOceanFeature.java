package gregtech.content.worldgen;

import gregtech.content.block.OreHostStone;
import gregtech.content.block.stone.StoneVariant;
import gregtech.content.registry.GTBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Port of GT6's {@code WorldgenDeepOcean} ({@code Loader_Worldgen}, registered as
 * {@code "ocean.prismacorals"}).
 *
 * <p>Only in the deep ocean. GT6 picks one random column of the chunk at {@code 3 + rand(9)},
 * {@code 30 + rand(9)} and requires water there; a 16-way noise sample at
 * {@code (minX + 8, 32, minZ + 8)} then decides what grows: values 12 and 13 build a
 * <b>dark prismarine</b> pylon, 14 and 15 a <b>light prismarine</b> one, everything else leaves the
 * ocean alone.
 *
 * <p>The pylon is a double-ended cone around the water column: four tiers of square rings mirrored
 * above and below {@code j}, one block wide at the top (radius 0 for {@code l = 8..10}), then
 * radius 1 ({@code l = 5..7}), radius 2 ({@code l = 2..4}) and radius 3 ({@code l = 0..1}). Each
 * position at or above the centre has a 1-in-8 chance of containing prismarine-backed ore —
 * garnierite for the dark prismarine, pyrolusite for the light one.
 */
public class GTDeepOceanFeature extends Feature<NoneFeatureConfiguration> {
    /** GT6's noise bound: {@code new NoiseGenerator(world).get(minX + 8, 32, minZ + 8, 16)}. */
    public static final int NOISE_OPTIONS = 16;

    public static final int NOISE_XZ = 8;
    public static final float NOISE_Y = 32.0F;

    public GTDeepOceanFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!gregtech.GregTechConfig.Worldgen.GEOLOGY.enabled()) return false;
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        if (level.getLevel().dimension() != Level.OVERWORLD) return false;
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        // GT6 requires the deep ocean specifically (BiomeGenBase.deepOcean).
        if (!GTWorldgenBiomes.anyOf(
                GTWorldgenBiomes.chunkBiomes(level, minX, minZ), java.util.Set.of(Biomes.DEEP_OCEAN.location()))) {
            return false;
        }

        int i = 3 + random.nextInt(9);
        int j = SpecialOreDeposits.OCEAN_CENTER_MIN
                + random.nextInt(SpecialOreDeposits.OCEAN_CENTER_MAX - SpecialOreDeposits.OCEAN_CENTER_MIN + 1);
        int k = 3 + random.nextInt(9);
        BlockPos column = new BlockPos(minX + i, j, minZ + k);
        if (!isWater(level.getBlockState(column))) return false;

        GTCellNoise noise = new GTCellNoise(level.getSeed());
        int pick = noise.get(minX + NOISE_XZ, NOISE_Y, minZ + NOISE_XZ, NOISE_OPTIONS);
        for (var pylon : SpecialOreDeposits.OCEAN_PYLONS) {
            if (pick >= pylon.firstNoise() && pick <= pylon.lastNoise()) {
                return buildPylon(level, column, pylon, random);
            }
        }
        return false;
    }

    private static boolean buildPylon(
            WorldGenLevel level, BlockPos column, SpecialOreDeposits.Pylon pylon, RandomSource random) {
        BlockState stone = GTBlocks.getStoneState(pylon.stone(), StoneVariant.STONE);
        if (stone == null) return false;
        var placement = GTOreBlockResolver.resolve(OreHostStone.of(pylon.stone()), pylon.material(), false);
        BlockState ore = placement == null ? stone : placement.state();
        boolean placed = false;
        for (var tier : MineralWorldgenRules.PYLON_TIERS) {
            for (int y = tier.fromL(); y <= tier.toL(); y++) {
                for (int x = -tier.radius(); x <= tier.radius(); x++) {
                    for (int z = -tier.radius(); z <= tier.radius(); z++) {
                        BlockState upper = random.nextInt(SpecialOreDeposits.OCEAN_ORE_DIVISOR) == 0 ? ore : stone;
                        placed |= setPylonBlock(level, column.offset(x, y, z), upper);
                        if (y > 0) placed |= setPylonBlock(level, column.offset(x, -y, z), stone);
                    }
                }
            }
        }
        return placed;
    }

    private static boolean setPylonBlock(WorldGenLevel level, BlockPos pos, BlockState state) {
        return !level.isOutsideBuildHeight(pos) && level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    /** GT6's {@code WD.anywater}: the port uses vanilla water only. */
    public static boolean isWater(BlockState state) {
        return state.is(Blocks.WATER);
    }

    /** Registry key of the configured feature (data file {@code worldgen/configured_feature/gt_deep_ocean.json}). */
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> CONFIGURED =
            ResourceKey.create(
                    Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("gt6ce", "gt_deep_ocean"));
}
