package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.worldgen.GTCellNoise;
import com.gregtech.gregtech.worldgen.GTNetherDepositFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * GT6's nether quartz ({@code WorldgenNetherQuartz}, {@code Loader_Worldgen:600}).
 *
 * <p>GT6 samples its value noise at two fixed heights per column — {@code 40 + noise(x, 0, z, 200)}
 * and {@code 40 + noise(x, 64, z, 200)} — and turns the netherrack it finds there into nether quartz
 * rock ore. The port used to gate a full-height noise sweep instead, which produced scattered quartz
 * instead of GT6's two thin layers; the tests pin GT6's formula.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class NetherQuartzTests {
    private static final int BASE_X = 44000;
    private static final int BASE_Z = 44000;

    /** GT6's constants: base height 40, noise at Y = 0 and 64, 200 options. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void quartzUsesGt6NoiseLayers(GameTestHelper helper) {
        helper.assertTrue(GTNetherDepositFeature.QUARTZ_BASE_Y == 40, "GT6 starts the layers at y = 40");
        helper.assertTrue(GTNetherDepositFeature.QUARTZ_OPTIONS == 200, "GT6 uses rng(200) noise");
        helper.assertTrue(GTNetherDepositFeature.QUARTZ_NOISE_Y.length == 2
                        && GTNetherDepositFeature.QUARTZ_NOISE_Y[0] == 0.0F
                        && GTNetherDepositFeature.QUARTZ_NOISE_Y[1] == 64.0F,
                "GT6 samples the noise at y = 0 and y = 64");
        helper.succeed();
    }

    /** The layers really land where GT6's noise says, and only inside netherrack. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void quartzReplacesNetherrackAtTheNoiseHeights(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldGenLevel gen = (WorldGenLevel) level;
        var quartz = ForgeRegistries.BLOCKS.getValue(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", "block_ore_netherquartz"));
        helper.assertTrue(quartz != null && quartz != Blocks.AIR,
                "the port registers block_ore_netherquartz for GT6's RockOres meta 8");
        var noise = new GTCellNoise(level.getSeed());
        // Prepare one chunk-sized column set: netherrack at both noise heights, stone elsewhere.
        int minX = BASE_X;
        int minZ = BASE_Z;
        level.getBlockState(new BlockPos(minX, 64, minZ));
        int hits = 0;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                int x = minX + i, z = minZ + j;
                for (float noiseY : GTNetherDepositFeature.QUARTZ_NOISE_Y) {
                    int y = GTNetherDepositFeature.QUARTZ_BASE_Y
                            + noise.get(x, noiseY, z, GTNetherDepositFeature.QUARTZ_OPTIONS);
                    if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) continue;
                    level.setBlock(new BlockPos(x, y, z), Blocks.NETHERRACK.defaultBlockState(), 2);
                    hits++;
                }
            }
        }
        helper.assertTrue(hits > 0, "the test placed its netherrack columns");
        // The pass is public so the test can drive GT6's formula on a prepared chunk.
        var seam = new GTNetherDepositFeature();
        seam.placeSeams(gen, noise, new BlockPos(minX, 64, minZ));
        int found = 0;
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                int x = minX + i, z = minZ + j;
                for (float noiseY : GTNetherDepositFeature.QUARTZ_NOISE_Y) {
                    int y = GTNetherDepositFeature.QUARTZ_BASE_Y
                            + noise.get(x, noiseY, z, GTNetherDepositFeature.QUARTZ_OPTIONS);
                    if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) continue;
                    if (level.getBlockState(new BlockPos(x, y, z)).is(quartz)) found++;
                }
            }
        }
        helper.assertTrue(found > 0, "GT6's noise heights received nether quartz, found " + found + "/" + hits);
        helper.succeed();
    }
}
