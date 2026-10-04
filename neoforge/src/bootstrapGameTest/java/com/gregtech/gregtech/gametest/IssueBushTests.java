package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.plant.BushBlock;
import com.gregtech.gregtech.blockentity.BushBlockEntity;
import com.gregtech.gregtech.registry.GTBushes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_issues")
@PrefixGameTestTemplate(false)
public final class IssueBushTests {
    private static BushBlockEntity core(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, GTBushes.BUSH.get().defaultBlockState());
        var bush = (BushBlockEntity)h.getLevel().getBlockEntity(pos);
        bush.setBerry("blueberry");
        return (BushBlockEntity)h.getLevel().getBlockEntity(pos);
    }

    @GameTest(template="test_empty", timeoutTicks=40)
    public static void cottonAcceptsStringHarvestsAndRetainsRipeCycleRemainder(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var bush = core(h, pos);
        bush.setBerry("");
        bush=(BushBlockEntity)h.getLevel().getBlockEntity(pos);
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.STRING, 4));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var result = GTBushes.BUSH.get().interact(bush.getBlockState(), h.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        bush=(BushBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(result.consumesAction() && bush.berryId().equals("minecraft:string"), "original default cotton adopts string");
        h.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 4, "setting output does not consume its specimen");
        h.getLevel().setBlockAndUpdate(pos, bush.getBlockState().setValue(BushBlock.STAGE, 3));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        GTBushes.BUSH.get().interact(bush.getBlockState(), h.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        int harvested = player.getInventory().countItem(net.minecraft.world.item.Items.STRING);
        h.assertTrue(harvested >= 1 && harvested <= 2 && bush.stage() == 0, "cotton hands out 1-2 string and resets");
        h.assertTrue(!bush.saveWithoutMetadata(h.getLevel().registryAccess()).contains("berry"), "cotton identity is the registered block, not BE data");
        var remainder = com.gregtech.gregtech.content.plant.BushGrowthRules.advance(255, 2, 2);
        h.assertTrue(remainder.stage() == 3 && remainder.counter() == 1, "rain growth after maturation remains in byte counter");
        h.assertTrue(com.gregtech.gregtech.content.plant.GTBerryBushes.worldgenSize() == 9, "cotton plus eight berries in generation pool");
        h.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=40)
    public static void bushCoreAndSixAttachmentsUseOriginalBoundsAndItemPlacement(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        core(h, pos);
        var root = h.getLevel().getBlockState(pos);
        h.assertTrue(root.getShape(h.getLevel(), pos).bounds().getSize() == 1, "root selects a full cube");
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for (var face : Direction.values()) {
            var adjacent = pos.relative(face);
            h.getLevel().setBlockAndUpdate(adjacent, Blocks.AIR.defaultBlockState());
            var stack = new ItemStack(GTBushes.BUSH.get(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5)), face, pos, false);
            var result = ((net.minecraft.world.item.BlockItem)stack.getItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
            var state = h.getLevel().getBlockState(adjacent);
            h.assertTrue(result.consumesAction() && state.is(GTBushes.byBerry("blueberry")) && state.getValue(BushBlock.SUPPORT) == face.getOpposite().get3DDataValue(), "item attaches to root face " + face);
            var bush = (BushBlockEntity)h.getLevel().getBlockEntity(adjacent);
            h.assertTrue(bush.berryId().equals("blueberry"), "branch inherits berry");
            var outline = state.getShape(h.getLevel(), adjacent).bounds();
            var collision = state.getCollisionShape(h.getLevel(), adjacent).bounds();
            h.assertTrue(outline.max(face.getAxis()) - outline.min(face.getAxis()) == 0.25 && collision.max(face.getAxis()) - collision.min(face.getAxis()) == 0.125, "original branch outline and collision " + face);
            h.getLevel().removeBlock(adjacent, false);
        }
        h.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=40)
    public static void branchCopiesRootTypeGrowthSpeedAndPopsWithoutSupport(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var root = core(h, pos);
        var adjacent = pos.east();
        var state = GTBushes.BUSH.get().defaultBlockState().setValue(BushBlock.SUPPORT, Direction.WEST.get3DDataValue());
        h.getLevel().setBlockAndUpdate(adjacent, state);
        var branch = (BushBlockEntity)h.getLevel().getBlockEntity(adjacent);
        branch.refreshSupport();
        branch=(BushBlockEntity)h.getLevel().getBlockEntity(adjacent);
        h.assertTrue(branch.berryId().equals("blueberry") && branch.speed() == 1, "root supplies berry and speed without dirt below branch");
        root.setBerry("cranberry"); branch.refreshSupport();
        branch=(BushBlockEntity)h.getLevel().getBlockEntity(adjacent);
        h.assertTrue(branch.berryId().equals("cranberry"), "branch follows later core type change");
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        h.assertTrue(branch.speed() == 0 && branch.grow() == 0, "invalid root soil stops attached growth");
        h.getLevel().removeBlock(pos, false);
        branch.refreshSupport();
        h.assertTrue(h.getLevel().getBlockState(adjacent).isAir(), "branch drops after its support is removed");
        h.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=40)
    public static void worldgenBranchesStartRipeAndSnowIsRemoved(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var root = core(h, pos);
        for (var face : Direction.values()) if(face != Direction.DOWN) h.getLevel().setBlockAndUpdate(pos.relative(face), Blocks.AIR.defaultBlockState());
        com.gregtech.gregtech.worldgen.GTBushesFeature.placeBranches(h.getLevel(), pos, "blueberry");
        for (var face : Direction.values()) {
            if(face == Direction.DOWN)continue;
            var state = h.getLevel().getBlockState(pos.relative(face));
            h.assertTrue(state.is(GTBushes.byBerry("blueberry")) && state.getValue(BushBlock.STAGE) == 3 && state.getValue(BushBlock.SUPPORT) == face.getOpposite().get3DDataValue(), "ripe generated branch " + face);
        }
        h.getLevel().setBlockAndUpdate(pos.above(), Blocks.SNOW.defaultBlockState());
        root.refreshSupport();
        h.assertTrue(h.getLevel().getBlockState(pos.above()).isAir(), "original snow removal");
        h.succeed();
    }
}
