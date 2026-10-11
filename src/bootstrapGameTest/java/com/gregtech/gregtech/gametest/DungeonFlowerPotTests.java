package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.plant.BedrockFlowers;
import com.gregtech.gregtech.registry.GTDungeonBlocks;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomLibraryNormal;
import com.gregtech.gregtech.worldgen.dungeon.GTDungeonData;
import com.gregtech.gregtech.block.stone.StoneType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;

/** GT6 DungeonData.pot and the normal library's eight flower/ZPM positions. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class DungeonFlowerPotTests {
    private DungeonFlowerPotTests() {}

    private static GTDungeonData data(GameTestHelper helper, BlockPos origin, RandomSource random) {
        return new GTDungeonData(helper.getLevel(), origin.getX(), origin.getY(), origin.getZ(),
                StoneType.LIMESTONE, StoneType.SLATE, 3, new byte[5][5], 2, 2, 0,
                new long[GTDungeonFeature.KEY_COUNT], new ItemStack[GTDungeonFeature.KEY_COUNT],
                new boolean[GTDungeonFeature.KEY_COUNT], new HashSet<>(), new HashSet<>(), random);
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void gt6FlowerPotKeepsVanillaAndIndicatorChoicesAndRandomOrder(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(48, 70, 48));
        helper.getLevel().setBlock(origin.below(), Blocks.STONE.defaultBlockState(), 3);
        Block[] vanilla = {
                Blocks.POTTED_CACTUS, Blocks.POTTED_BROWN_MUSHROOM, Blocks.POTTED_RED_MUSHROOM,
                Blocks.POTTED_DANDELION, Blocks.POTTED_POPPY, Blocks.POTTED_BLUE_ORCHID,
                Blocks.POTTED_ALLIUM, Blocks.POTTED_AZURE_BLUET, Blocks.POTTED_RED_TULIP,
                Blocks.POTTED_ORANGE_TULIP, Blocks.POTTED_WHITE_TULIP, Blocks.POTTED_PINK_TULIP,
                Blocks.POTTED_OXEYE_DAISY};
        boolean sawVanilla = false, sawA = false, sawB = false;
        for (int seed = 0; seed < 96; seed++) {
            RandomSource expectedRandom = RandomSource.create(seed);
            int vanillaIndex = expectedRandom.nextInt(13);
            Block expected;
            if (expectedRandom.nextBoolean()) {
                expected = vanilla[vanillaIndex];
                sawVanilla = true;
            } else if (expectedRandom.nextBoolean()) {
                expected = GTDungeonBlocks.pottedFlower(BedrockFlowers.ALL.get(expectedRandom.nextInt(9)).id());
                sawA = true;
            } else {
                expected = GTDungeonBlocks.pottedFlower(BedrockFlowers.ALL.get(9 + expectedRandom.nextInt(8)).id());
                sawB = true;
            }
            RandomSource actualRandom = RandomSource.create(seed);
            // setBlock returns false for a repeated state, even though the flower choice is valid.
            helper.getLevel().setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(data(helper, origin, actualRandom).pot(0, 0, 0), "GT6 pot placed for seed " + seed);
            helper.assertTrue(helper.getLevel().getBlockState(origin).is(expected),
                    "GT6 vanilla/A/B flower selection for seed " + seed);
            helper.assertTrue(actualRandom.nextInt(100000) == expectedRandom.nextInt(100000),
                    "flower selection consumed GT6's draws for seed " + seed);
        }
        helper.assertTrue(sawVanilla && sawA && sawB, "all three original flower families occurred");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void indicatorPotsShowAndDropTheirOriginalGt6Flowers(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        var player = helper.makeMockPlayer();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        for (BedrockFlowers.Flower flower : BedrockFlowers.ALL) {
            Block flowerBlock = ForgeRegistries.BLOCKS.getValue(
                    com.gregtech.gregtech.GregTech.id(flower.id()));
            Block pot = GTDungeonBlocks.pottedFlower(flower.id());
            helper.assertTrue(flowerBlock != null && pot instanceof FlowerPotBlock
                            && ((FlowerPotBlock) pot).getContent() == flowerBlock,
                    flower.id() + " has a real flower-pot block with its own plant");
            var drops = pot.getDrops(pot.defaultBlockState(), new LootParams.Builder(helper.getLevel())
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY));
            helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(Blocks.FLOWER_POT.asItem()))
                            && drops.stream().anyMatch(stack -> stack.is(flowerBlock.asItem())),
                    flower.id() + " drops the pot and the same GT flower");
            helper.getLevel().setBlock(pos, Blocks.FLOWER_POT.defaultBlockState(), 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(flowerBlock));
            helper.getLevel().getBlockState(pos).use(helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(helper.getLevel().getBlockState(pos).is(pot),
                    "player can plant " + flower.id() + " in the ordinary flower pot");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void normalLibraryFillsEveryDecorativeFlowerPosition(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(48, 70, 48));
        helper.assertTrue(new GTDungeonChunkRoomLibraryNormal().generate(
                data(helper, origin, RandomSource.create(1099))), "GT6 normal library builds");
        int[][] pots = {{14, 2, 6}, {14, 2, 9}, {1, 2, 6}, {1, 2, 9},
                {6, 2, 14}, {9, 2, 14}, {6, 2, 1}, {9, 2, 1}};
        int flowerCount = 0;
        for (int[] xyz : pots) {
            Block block = helper.getLevel().getBlockState(origin.offset(xyz[0], xyz[1], xyz[2])).getBlock();
            if (block instanceof FlowerPotBlock pot) {
                flowerCount++;
                helper.assertTrue(pot.getContent() != Blocks.AIR, "library flower pot is not empty");
            } else {
                helper.assertTrue(block == com.gregtech.gregtech.registry.GTLasers.ZPM.get(),
                        "library pot position is a GT flower or its one permitted ZPM");
            }
        }
        helper.assertTrue(flowerCount >= 7, "GT6 normal library has at most one ZPM");
        helper.succeed();
    }
}
