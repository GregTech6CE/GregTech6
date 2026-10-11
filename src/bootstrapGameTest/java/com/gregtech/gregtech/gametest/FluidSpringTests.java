package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.blockentity.FluidSpringBlockEntity;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import com.gregtech.gregtech.worldgen.GTFeatures;
import com.gregtech.gregtech.worldgen.GTFluidSpringsFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Guards GT6's fluid springs ({@code WorldgenFluidSpring}, {@code Loader_Worldgen:782-797}): the
 * spring kinds and their rarities, the crater, the spring tile entity and the fluid blocks the port
 * had to add first (no GT6 fluid could exist in the world before this batch).
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class FluidSpringTests {
    private static final int BASE_X = 40000;
    private static final int BASE_Z = 40000;
    private static final int BASE_Y = 210;

    private static BlockPos base(int index) {
        return new BlockPos(BASE_X + index * 16, BASE_Y, BASE_Z);
    }

    /** GT6's seven overworld springs with their rarities, indicators and amounts. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void springsMatchGt6Table(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        helper.assertTrue(GTFluidSpringsFeature.SPRINGS.size() == 7,
                "Loader_Worldgen:782-788 registers 7 overworld springs, got "
                        + GTFluidSpringsFeature.SPRINGS.size());
        int[] probabilities = {400, 400, 400, 400, 200, 100, 200};
        int[] indicators = {2, 2, 2, 2, 1, 3, 1};
        int[] amounts = {6000, 6000, 6000, 6000, 3000, 500, 1000};
        for (int i = 0; i < probabilities.length; i++) {
            var spring = GTFluidSpringsFeature.SPRINGS.get(i);
            if (spring.probability() != probabilities[i]) {
                problems.add(spring.name() + ": GT6 uses 1/" + probabilities[i] + ", got 1/" + spring.probability());
            }
            if (spring.indicatorType() != indicators[i]) {
                problems.add(spring.name() + ": GT6's mIndicatorType is " + indicators[i]);
            }
            if (spring.amount() != amounts[i]) {
                problems.add(spring.name() + ": GT6 stores " + amounts[i] + " mB, got " + spring.amount());
            }
            var fluid = ForgeRegistries.FLUIDS.getValue(net.minecraft.resources.ResourceLocation.parse(spring.fluidId()));
            if (fluid == null) problems.add(spring.name() + ": fluid " + spring.fluidId() + " is not registered");
            var block = ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.parse(spring.blockId()));
            if (block == null || block == Blocks.AIR) {
                problems.add(spring.name() + ": world block " + spring.blockId() + " is missing");
            } else if (!(block instanceof LiquidBlock)) {
                problems.add(spring.name() + ": " + spring.blockId() + " is not a LiquidBlock");
            }
        }
        helper.assertTrue(GTFluidSpringsFeature.CRATER_LAYERS == 7, "GT6 carves 7 crater layers");
        helper.assertTrue(GTFluidSpringsFeature.CRATER_SPRING_CHANCE == 16, "one spring per 16 crater columns");
        helper.assertTrue(GTFeatures.FLUID_SPRINGS.getId().getPath().equals("gt_fluid_springs"), "feature id");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .containsKey(GTFluidSpringsFeature.CONFIGURED),
                "data/gregtech/worldgen/configured_feature/gt_fluid_springs.json is loaded");
        helper.assertTrue(problems.isEmpty(), "spring table (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }

    /** The port's new fluid blocks really place a source block of their fluid. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void fluidBlocksPlaceTheirFluid(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        int index = 0;
        for (var spring : GTFluidSpringsFeature.SPRINGS) {
            BlockPos pos = base(index++);
            level.getBlockState(pos);
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            if (!GTFluidSpringsFeature.placeSpring((WorldGenLevel) level, pos, spring)) {
                problems.add(spring.name() + ": the spring was not placed");
                continue;
            }
            var springBlock = com.gregtech.gregtech.registry.GTFluidSprings.byFluid(spring.fluidId());
            if (!level.getBlockState(pos).is(springBlock)) {
                problems.add(spring.name() + ": fluid_spring block missing");
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof FluidSpringBlockEntity be)) {
                problems.add(spring.name() + ": no spring block entity");
                continue;
            }
            FluidStack stored = be.springFluid();
            if (stored.isEmpty() || stored.getAmount() != spring.amount()) {
                problems.add(spring.name() + ": stored fluid " + stored);
            }
            // A free spot above: the spring emits its own fluid block (GT6 pushes the fluid up).
            if (!be.emit()) {
                problems.add(spring.name() + ": the spring emitted nothing");
                continue;
            }
            BlockState above = level.getBlockState(pos.above());
            if (above.getFluidState().isEmpty()) {
                problems.add(spring.name() + ": no fluid above the spring (" + above + ")");
            }
        }
        helper.assertTrue(problems.isEmpty(), "spring fluids (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }

    /** GT6's crater: a shrinking square of the spring's block with filler above, springs on bedrock. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void craterFollowsGt6(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldGenLevel gen = (WorldGenLevel) level;
        int minX = BASE_X + 200;
        int minZ = BASE_Z + 200;
        level.getBlockState(new BlockPos(minX, level.getMinBuildHeight(), minZ));
        // A bedrock floor with stone above, like the chunk bottom GT6 carves into.
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                level.setBlock(new BlockPos(x, level.getMinBuildHeight(), z), Blocks.BEDROCK.defaultBlockState(), 2);
                for (int i = 1; i <= 8; i++) {
                    level.setBlock(new BlockPos(x, level.getMinBuildHeight() + i, z),
                            Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
        var spring = GTFluidSpringsFeature.SPRINGS.get(6); // lava
        helper.assertTrue(GTFluidSpringsFeature.carve(gen, minX, minZ, spring, RandomSource.create(11L)),
                "the crater is carved");
        // Layer 1 covers the 16x16 area inset by one block (GT6's `aMinX+i .. aMaxX-i` squares),
        // and the square keeps shrinking towards the bottom.
        helper.assertTrue(level.getBlockState(new BlockPos(minX + 1, level.getMinBuildHeight() + 1, minZ + 1))
                        .is(Blocks.LAVA),
                "layer 1 of the crater is the spring's block");
        helper.assertFalse(level.getBlockState(new BlockPos(minX, level.getMinBuildHeight() + 1, minZ))
                        .is(Blocks.LAVA),
                "the crater insets by one block per layer (GT6's shrinking squares)");
        helper.assertTrue(level.getBlockState(new BlockPos(minX + 7, level.getMinBuildHeight() + 6, minZ + 7))
                        .is(Blocks.LAVA),
                "the crater reaches GT6's deepest fluid layer (the i = 6 square) at the centre");
        helper.assertFalse(level.getBlockState(new BlockPos(minX + 15, level.getMinBuildHeight() + 7, minZ + 15))
                        .is(Blocks.LAVA),
                "the crater shrinks towards the bottom (GT6's inset squares)");
        helper.assertTrue(GTFluidSpringsFeature.hasBedrock(gen, minX + 8, minZ + 8),
                "the bedrock check sees the floor");
        helper.succeed();
    }

    /** The three indicator types use GT6's yellow, brown and plain grass. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void indicatorGrassFollowsGt6(GameTestHelper helper) {
        var yellow = GTFluidSpringsFeature.indicatorBlock(1);
        var brown = GTFluidSpringsFeature.indicatorBlock(2);
        var plain = GTFluidSpringsFeature.indicatorBlock(3);
        helper.assertTrue(yellow != null && yellow != Blocks.AIR, "indicator 1 (gas/lava) is yellow grass");
        helper.assertTrue(brown != null && brown != Blocks.AIR, "indicator 2 (oil) is brown grass");
        helper.assertTrue(plain != null && plain != Blocks.AIR, "indicator 3 (geothermal water) is plain grass");
        helper.assertTrue(yellow != brown && brown != plain, "the three indicators differ");
        helper.assertTrue(GTBlockEntities.FLUID_SPRING.get()
                        .isValid(GTDecorBlocks.FLUID_SPRING.get().defaultBlockState()),
                "the fluid_spring block entity type covers the block");
        helper.succeed();
    }
}
