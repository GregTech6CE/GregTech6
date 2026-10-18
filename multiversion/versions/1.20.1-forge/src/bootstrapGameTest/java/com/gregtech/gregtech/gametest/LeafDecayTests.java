package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.wood.WoodLeavesBlock;
import com.gregtech.gregtech.block.wood.WoodSpecies;
import com.gregtech.gregtech.registry.GTWoods;
import com.gregtech.gregtech.worldgen.GTTreeShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Regression tests for the reported bug "naturally grown trees lose their leaves over time".
 *
 * <p>GT6's leaves are kept as long as the tree's own log is in range
 * ({@code BlockBaseLeaves.updateTick2}); vanilla {@code LeavesBlock} instead decays any leaf whose
 * {@code distance} is 7, and because the port's logs were not vanilla logs every worldgen leaf sat at
 * distance 7 and vanished. The port now implements GT6's check and tags its logs/leaves, so both
 * paths keep a standing tree intact.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class LeafDecayTests {
    private static final int BASE_X = 36000;
    private static final int BASE_Z = 36000;
    private static final int BASE_Y = 210;

    private static BlockPos base(int index) {
        return new BlockPos(BASE_X + index * 48, BASE_Y, BASE_Z);
    }

    private static void clear(ServerLevel level, BlockPos center, int radius, int height) {
        level.getBlockState(center);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= height; dy++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.getBlockState(cursor).isAir()) level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    /** Leaves of a standing tree survive many random ticks (GT6's log check keeps them). */
    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void standingTreeKeepsItsLeaves(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        int index = 0;
        for (WoodSpecies species : WoodSpecies.values()) {
            BlockPos pos = base(index++);
            clear(level, pos, 8, 22);
            level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            if (!GTTreeShapes.grow(level, pos, species, RandomSource.create(4242L + index))) {
                problems.add(species + ": the tree did not grow");
                continue;
            }
            // Every leaf of the tree, ticked like the game would (random ticks hit a few leaves per tick).
            List<BlockPos> leaves = new ArrayList<>();
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int dx = -8; dx <= 8; dx++) {
                for (int dy = -1; dy <= 20; dy++) {
                    for (int dz = -8; dz <= 8; dz++) {
                        cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                        if (level.getBlockState(cursor).is(GTWoods.leaves(species))) {
                            leaves.add(cursor.immutable());
                        }
                    }
                }
            }
            if (leaves.isEmpty()) {
                problems.add(species + ": the tree grew without leaves");
                continue;
            }
            RandomSource random = RandomSource.create(99L);
            for (int round = 0; round < 200; round++) {
                for (BlockPos leaf : leaves) {
                    BlockState state = level.getBlockState(leaf);
                    if (!state.is(GTWoods.leaves(species))) continue;
                    state.randomTick(level, leaf, random);
                }
            }
            int remaining = 0;
            for (BlockPos leaf : leaves) {
                if (level.getBlockState(leaf).is(GTWoods.leaves(species))) remaining++;
            }
            if (remaining != leaves.size()) {
                problems.add(species + ": " + (leaves.size() - remaining) + " of " + leaves.size()
                        + " leaves disappeared while the tree stands");
            }
        }
        helper.assertTrue(problems.isEmpty(), "standing trees (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }

    /** Chop the trunk and the leaves do decay — GT6 drops a sapling (or nothing) and removes them. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void choppedTreeLosesItsLeaves(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = base(20);
        clear(level, pos, 8, 22);
        level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        helper.assertTrue(GTTreeShapes.grow(level, pos, WoodSpecies.RUBBER, RandomSource.create(7L)),
                "the rubber tree grew");
        WoodLeavesBlock leaves = GTWoods.leaves(WoodSpecies.RUBBER);
        List<BlockPos> leafPositions = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = 0; dy <= 12; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (level.getBlockState(cursor).is(leaves)) leafPositions.add(cursor.immutable());
                }
            }
        }
        helper.assertTrue(!leafPositions.isEmpty(), "the tree has leaves to decay");
        // Remove the whole trunk first (the player chopped the tree).
        for (int dy = 0; dy <= 12; dy++) {
            if (level.getBlockState(pos.above(dy)).is(GTWoods.log(WoodSpecies.RUBBER))) {
                level.setBlock(pos.above(dy), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        RandomSource random = RandomSource.create(1234L);
        for (int round = 0; round < 50; round++) {
            for (BlockPos leaf : leafPositions) {
                BlockState state = level.getBlockState(leaf);
                if (!state.is(leaves)) continue;
                state.randomTick(level, leaf, random);
            }
        }
        int left = 0;
        for (BlockPos leaf : leafPositions) {
            if (level.getBlockState(leaf).is(leaves)) left++;
        }
        helper.assertTrue(left == 0, "GT6 decays the leaves of a chopped tree, " + left + " leaves stayed");
        helper.succeed();
    }

    /** GT6's "will not decay" flag (vanilla's persistent) and the scan ranges are GT6's own. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void persistentLeavesAndRangesMatchGt6(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = base(30);
        clear(level, pos, 3, 6);
        level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        // A leaf that would decay (no log anywhere near) but is persistent: GT6 meta < 8.
        level.setBlock(pos, GTWoods.leaves(WoodSpecies.MAPLE).defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true), 3);
        BlockState state = level.getBlockState(pos);
        RandomSource random = RandomSource.create(5L);
        for (int i = 0; i < 500; i++) state.randomTick(level, pos, random);
        helper.assertTrue(level.getBlockState(pos).is(GTWoods.leaves(WoodSpecies.MAPLE)),
                "persistent leaves never decay (GT6 meta < 8)");

        // GT6's per-species ranges (BlockTreeLeavesAB / BlockTreeLeavesCD).
        helper.assertTrue(WoodLeavesBlock.rangeSide(WoodSpecies.RUBBER) == 2, "rubber side range 2");
        helper.assertTrue(WoodLeavesBlock.rangeSide(WoodSpecies.WILLOW) == 4, "willow side range 4");
        helper.assertTrue(WoodLeavesBlock.rangeSide(WoodSpecies.COCONUT) == 4, "coconut side range 4");
        helper.assertTrue(WoodLeavesBlock.rangeSide(WoodSpecies.MAPLE) == 3, "maple side range 3");
        helper.assertTrue(WoodLeavesBlock.rangeSide(WoodSpecies.BLUE_SPRUCE) == 6, "blue spruce side range 6");
        helper.assertTrue(WoodLeavesBlock.rangeYNeg(WoodSpecies.BLUE_MAHOE) == 4, "blue mahoe down range 4");
        helper.assertTrue(WoodLeavesBlock.rangeYNeg(WoodSpecies.CINNAMON) == 3, "cinnamon down range 3");
        helper.assertTrue(WoodLeavesBlock.rangeYNeg(WoodSpecies.COCONUT) == 1, "coconut down range 1");
        helper.assertTrue(WoodLeavesBlock.rangeYNeg(WoodSpecies.RUBBER) == 2, "rubber down range 2");
        // The log check itself: a log five blocks away is outside the rubber range, one next to it is not.
        BlockPos far = pos.above(3);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
        helper.assertFalse(WoodLeavesBlock.hasLogNearby(level, far, WoodSpecies.RUBBER),
                "no log near the far leaf");
        level.setBlock(far.below(), GTWoods.log(WoodSpecies.RUBBER).defaultBlockState(), 3);
        helper.assertTrue(WoodLeavesBlock.hasLogNearby(level, far, WoodSpecies.RUBBER),
                "the rubber log keeps the leaf alive");
        helper.succeed();
    }

    /** The port's logs and leaves are in vanilla's tags, which is what vanilla's distance logic needs. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void woodIsInVanillaTags(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.BLOCK)
                .getTag(BlockTags.LOGS).isPresent(), "minecraft:logs is loaded");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.BLOCK)
                .getTag(BlockTags.LEAVES).isPresent(), "minecraft:leaves is loaded");
        for (WoodSpecies species : WoodSpecies.values()) {
            if (!GTWoods.log(species).defaultBlockState().is(BlockTags.LOGS)) {
                problems.add("log_" + species.id() + " is not in #minecraft:logs");
            }
            if (!GTWoods.leaves(species).defaultBlockState().is(BlockTags.LEAVES)) {
                problems.add("leaves_" + species.id() + " is not in #minecraft:leaves");
            }
        }
        helper.assertTrue(problems.isEmpty(), "wood tags (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }
}
