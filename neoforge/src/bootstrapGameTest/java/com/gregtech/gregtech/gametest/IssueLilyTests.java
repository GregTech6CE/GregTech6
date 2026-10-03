package com.gregtech.gregtech.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_issues")
@PrefixGameTestTemplate(false)
public final class IssueLilyTests {
    @GameTest(template="test_empty", timeoutTicks=40)
    public static void allLiliesUseWaterSurfacePlacementAndVanillaCollision(GameTestHelper h) {
        var level = h.getLevel();
        var water = h.absolutePos(new BlockPos(2, 2, 2));
        var pos = water.above();
        int checked = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof WaterlilyBlock) || !BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("gregtech")) continue;
            h.assertTrue(block.asItem() instanceof PlaceOnWaterBlockItem, "water surface item: " + block);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
            var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            player.setPos(water.getX() + 0.5, water.getY() + 2, water.getZ() + 0.5);
            player.setXRot(90); player.setYRot(0);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(block, 2));
            var result = block.asItem().use(level, player, InteractionHand.MAIN_HAND);
            h.assertTrue(result.getResult().consumesAction() && level.getBlockState(pos).is(block), "native water ray places lily: " + block);
            h.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "placement consumes one lily");
            var actual = block.defaultBlockState().getCollisionShape(level, pos, CollisionContext.empty());
            var vanilla = Blocks.LILY_PAD.defaultBlockState().getCollisionShape(level, pos, CollisionContext.empty());
            h.assertTrue(!actual.isEmpty() && !Shapes.joinIsNotEmpty(actual, vanilla, BooleanOp.NOT_SAME), "vanilla thin collision: " + block);
            h.assertTrue(block.defaultBlockState().getLightEmission() == (BuiltInRegistries.BLOCK.getKey(block).getPath().startsWith("glowtus_") ? 15 : 0), "preserved light level");
            checked++;
        }
        h.assertTrue(checked == 17, "all sixteen glowtus and hexalily checked: " + checked);
        h.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=40)
    public static void lilyRejectsFlowingWaterAndBreaksOnBoat(GameTestHelper h) {
        var level = h.getLevel();
        var water = h.absolutePos(new BlockPos(2, 2, 2));
        var pos = water.above();
        var block = BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", "glowtus_white"));
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(water.getX() + 0.5, water.getY() + 2, water.getZ() + 0.5);
        player.setXRot(90); player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(block));
        block.asItem().use(level, player, InteractionHand.MAIN_HAND);
        h.assertTrue(level.getBlockState(pos).isAir() && player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "flowing water not targeted");
        level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        var boat = net.minecraft.world.entity.EntityType.BOAT.create(level);
        boat.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        block.defaultBlockState().entityInside(level, pos, boat);
        h.assertTrue(level.getBlockState(pos).isAir(), "native boat contact destroys lily");
        h.succeed();
    }
}
