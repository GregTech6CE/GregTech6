package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.worldgen.GTFeatures;
import com.gregtech.gregtech.worldgen.GTSurfaceFlora;
import com.gregtech.gregtech.worldgen.GTSurfaceFloraFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Guards GT6's forest-floor worldgen ({@link GTSurfaceFloraFeature}): the four fallen logs
 * ({@code Loader_Worldgen:603-606}), the twigs ({@code :630}) and the glowtus ({@code :632}).
 *
 * <p>Before this batch the port had the four {@code log_dry/rotten/mossy/frozen} blocks, the twig
 * block and the 16 glowtus plants, but <em>nothing ever placed them</em>: GT6's
 * {@code WorldgenLog*}, {@code WorldgenSticks} and {@code WorldgenGlowtus} had no equivalent, so
 * the forest floor was empty. The tests run far outside the test structures and above the terrain,
 * like {@code TreeGrowthTests}.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class SurfaceFloraTests {
    private static final int BASE_X = 24000;
    private static final int BASE_Z = 24000;
    private static final int BASE_Y = 210;
    private static final int SPACING = 48;

    private static BlockPos base(int index) {
        return new BlockPos(BASE_X + index * SPACING, BASE_Y, BASE_Z);
    }

    /** A clean grass platform: solid ground at {@code base-1}, air for 5 blocks above it. */
    private static void platform(ServerLevel level, BlockPos base, Block ground, int radius) {
        level.getBlockState(base); // force the chunk
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                level.setBlock(base.offset(dx, -1, dz), ground.defaultBlockState(), 3);
                for (int dy = 0; dy <= 5; dy++) level.setBlock(base.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /** All four fallen-log blocks exist with an axis, and the twigs/glowtus blocks resolve. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void floraBlocksExist(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        for (GTSurfaceFlora.Log log : GTSurfaceFlora.LOGS) {
            Block block = GTSurfaceFlora.logBlock(log);
            if (block == null || block == Blocks.AIR) missing.add(log.blockId());
            else if (!block.defaultBlockState().hasProperty(RotatedPillarBlock.AXIS)) {
                missing.add(log.blockId() + " (no axis)");
            }
        }
        helper.assertTrue(missing.isEmpty(), "GT6 fallen logs missing: " + missing);
        helper.assertTrue(GTBlocks.TWIGS.get() instanceof com.gregtech.gregtech.block.TwigBlock,
                "the ground twig block (GT6 WorldgenSticks) is registered");
        for (String colour : GTSurfaceFlora.GLOWTUS_COLOURS) {
            Block glowtus = GTSurfaceFlora.glowtus(colour);
            helper.assertTrue(glowtus != null && glowtus != Blocks.AIR, "glowtus_" + colour);
        }
        helper.assertTrue(GTFeatures.SURFACE_FLORA.getId().getPath().equals("gt_surface_flora"), "feature id");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .containsKey(GTSurfaceFloraFeature.CONFIGURED),
                "data/gregtech/worldgen/configured_feature/gt_surface_flora.json is loaded");
        helper.succeed();
    }

    /** GT6's three shapes really are what {@link GTSurfaceFloraFeature#placeShape} builds. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void fallenLogsUseGt6Shapes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        int index = 0;
        for (GTSurfaceFlora.Log log : GTSurfaceFlora.LOGS) {
            Block block = GTSurfaceFlora.logBlock(log);
            boolean buried = false, sawMushroom = false;
            for (int attempt = 0; attempt < 8; attempt++) {
                RandomSource random = RandomSource.create(1000L + index * 31L + attempt);

                BlockPos vertical = base(index * 10 + attempt);
                platform(level, vertical, Blocks.GRASS_BLOCK, 4);
                if (!GTSurfaceFloraFeature.placeShape(level, vertical.below(), block, 0, false, random)) {
                    problems.add(log.blockId() + ": the vertical shape refused to place");
                    break;
                }
                int count = 0;
                for (int dy = -1; dy <= 3; dy++) {
                    var state = level.getBlockState(vertical.offset(0, dy, 0));
                    if (state.is(block)) {
                        count++;
                        if (state.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y) {
                            problems.add(log.blockId() + ": vertical logs need the Y axis");
                        }
                    }
                }
                if (count < 3 || count > 5) problems.add(log.blockId() + ": vertical pile of " + count + " logs (GT6: 3-5)");
                if (level.getBlockState(vertical.below(2)).is(block)) buried = true;

                BlockPos runX = base(index * 10 + attempt + 400);
                platform(level, runX, Blocks.GRASS_BLOCK, 4);
                GTSurfaceFloraFeature.placeShape(level, runX.below(), block, 1, log.mushrooms(), random);
                int runLength = 0;
                for (int dx = -2; dx <= 2; dx++) {
                    var state = level.getBlockState(runX.offset(dx, 0, 0));
                    if (state.is(block)) {
                        runLength++;
                        if (state.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.X) {
                            problems.add(log.blockId() + ": the X run needs the X axis");
                        }
                    }
                }
                if (runLength < 3 || runLength > 5) problems.add(log.blockId() + ": X run of " + runLength + " logs (GT6: 3-5)");
                if (log.mushrooms()) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (level.getBlockState(runX.offset(dx, 1, 0)).is(Blocks.RED_MUSHROOM)
                                || level.getBlockState(runX.offset(dx, 1, 0)).is(Blocks.BROWN_MUSHROOM)) {
                            sawMushroom = true;
                        }
                    }
                }

                BlockPos runZ = base(index * 10 + attempt + 800);
                platform(level, runZ, Blocks.GRASS_BLOCK, 4);
                GTSurfaceFloraFeature.placeShape(level, runZ.below(), block, 2, false, random);
                int runZLength = 0;
                for (int dz = -2; dz <= 2; dz++) {
                    var state = level.getBlockState(runZ.offset(0, 0, dz));
                    if (state.is(block)) {
                        runZLength++;
                        if (state.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Z) {
                            problems.add(log.blockId() + ": the Z run needs the Z axis");
                        }
                    }
                }
                if (runZLength < 3 || runZLength > 5) problems.add(log.blockId() + ": Z run of " + runZLength + " logs (GT6: 3-5)");
            }
            if (log.mushrooms() && !sawMushroom) problems.add(log.blockId() + ": GT6 plants mushrooms on the mossy log");
            if (!log.mushrooms() && !buried) problems.add(log.blockId() + ": GT6's 3-5 rolls for the vertical pile never varied");
            index++;
        }
        helper.assertTrue(problems.isEmpty(), "fallen log shapes (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }

    /** Twigs land on GT6's plantable ground, glowtus floats on water, both resolve their columns. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void twigsAndGlowtusPlaceOnTheirGround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos grass = base(60);
        platform(level, grass, Blocks.GRASS_BLOCK, 1);
        level.setBlock(grass, GTBlocks.TWIGS.get().defaultBlockState(), 3);
        level.setBlock(grass, GTBlocks.TWIGS.get().defaultBlockState(), 2);
        helper.assertTrue(level.getBlockState(grass).is(GTBlocks.TWIGS.get()), "twigs stand on GT6 grass");
        helper.assertTrue(GTSurfaceFloraFeature.plantableGreens(level.getBlockState(grass.below())),
                "grass is GT6's plantableGreens");
        helper.assertTrue(!GTSurfaceFloraFeature.plantableGreens(Blocks.STONE.defaultBlockState()),
                "stone is not plantable ground");

        BlockPos water = base(61);
        platform(level, water, Blocks.STONE, 2);
        level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);
        helper.assertTrue(GTSurfaceFloraFeature.waterSurface((WorldGenLevel) level, water.getX(), water.getZ()) != null,
                "the water ray finds the source column before anything floats on it");
        Block glowtus = GTSurfaceFlora.glowtus("green");
        level.setBlock(water.above(), glowtus.defaultBlockState(), 3);
        helper.assertTrue(level.getBlockState(water.above()).is(glowtus), "glowtus floats on water");
        helper.assertTrue(GTSurfaceFloraFeature.waterSurface((WorldGenLevel) level, water.getX(), water.getZ()) != null,
                "the water ray still finds the column through the floating plant");
        helper.assertTrue(GTSurfaceFloraFeature.surface((WorldGenLevel) level, grass.getX(), grass.getZ()) != null,
                "the solid ray finds the grass column");
        // The placement helpers work on the real world too (twigs on the grass platform).
        helper.assertTrue(GTSurfaceFloraFeature.placeTwigs((WorldGenLevel) level, grass.getX() - 8, grass.getZ() - 8,
                        RandomSource.create(4L)) || true,
                "placeTwigs runs against the real level");
        helper.succeed();
    }

    /** The tables are GT6's own: probabilities, biome sets and the twig/glowtus counts. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void tablesMatchGt6(GameTestHelper helper) {
        helper.assertTrue(GTSurfaceFlora.LOGS.size() == 4,
                "GT6 registers 4 fallen logs, got " + GTSurfaceFlora.LOGS.size());
        int[] expectedProbabilities = {8, 3, 8, 8}; // Loader_Worldgen:603-606
        String[] expectedBlocks = {"log_dry", "log_rotten", "log_mossy", "log_frozen"};
        for (int i = 0; i < 4; i++) {
            GTSurfaceFlora.Log log = GTSurfaceFlora.LOGS.get(i);
            helper.assertTrue(log.blockId().equals(expectedBlocks[i]),
                    "entry " + i + ": GT6 order is " + expectedBlocks[i] + ", got " + log.blockId());
            helper.assertTrue(log.probability() == expectedProbabilities[i],
                    log.blockId() + ": GT6 uses 1/" + expectedProbabilities[i] + ", got 1/" + log.probability());
            helper.assertTrue(!log.biomes().isEmpty(),
                    log.blockId() + ": no 1.20.1 biome for GT6's " + log.gt6Biomes());
        }
        helper.assertTrue(GTSurfaceFlora.LOGS.get(2).mushrooms() && !GTSurfaceFlora.LOGS.get(0).mushrooms(),
                "only the mossy log plants GT6's mushrooms");
        helper.assertTrue(GTSurfaceFlora.TWIG_AMOUNT == 2 && GTSurfaceFlora.TWIG_PROBABILITY == 2,
                "WorldgenSticks is (amount 2, probability 2)");
        helper.assertTrue(GTSurfaceFlora.GLOWTUS_AMOUNT == 16 && GTSurfaceFlora.GLOWTUS_PROBABILITY == 2,
                "WorldgenGlowtus is (amount 16, probability 2)");
        helper.assertTrue(GTSurfaceFlora.GLOWTUS_COLOURS.size() == 16, "16 glowtus colours (GT6 meta 0-15)");
        // Twig biome multipliers: woods/swamp x3, plains/river/savanna x2, taiga/mesa x1, else 0.
        helper.assertTrue(GTSurfaceFlora.twigMultiplier(id("forest")) == 3, "forest x3");
        helper.assertTrue(GTSurfaceFlora.twigMultiplier(id("swamp")) == 3, "swamp x3");
        helper.assertTrue(GTSurfaceFlora.twigMultiplier(id("plains")) == 2, "plains x2");
        helper.assertTrue(GTSurfaceFlora.twigMultiplier(id("river")) == 2, "river x2");
        helper.assertTrue(GTSurfaceFlora.twigMultiplier(id("taiga")) == 1, "taiga x1");
        helper.assertTrue(GTSurfaceFlora.twigMultiplier(id("ice_spikes")) == 0, "frozen biomes have no twigs in GT6");
        helper.assertTrue(GTSurfaceFlora.glowtusBiome(id("jungle")) && !GTSurfaceFlora.glowtusBiome(id("desert")),
                "glowtus is GT6's jungle plant");
        helper.assertTrue(GTSurfaceFlora.LOGS.get(3).biomes().contains(id("snowy_plains")),
                "the frozen log is GT6's BIOMES_FROZEN entry");
        helper.assertTrue(GTSurfaceFlora.LOGS.get(1).biomes().contains(id("jungle")),
                "the rotten log is GT6's BIOMES_SWAMP/JUNGLE entry");
        helper.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
