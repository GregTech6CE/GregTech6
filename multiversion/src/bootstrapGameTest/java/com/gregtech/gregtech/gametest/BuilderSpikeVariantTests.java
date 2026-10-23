package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.misc.SpikeBlock;
import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Native pick-block stock identity must not be confused with placement-facing state. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class BuilderSpikeVariantTests {
    private static SpikeBlock spike(GameTestHelper h) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "spike_steel"));
        h.assertTrue(block instanceof SpikeBlock, "registered steel spike");
        return (SpikeBlock) block;
    }

    /** Invoke the existing native identity producer; never construct arbitrary state tags in this test. */
    private static ItemStack stock(GameTestHelper h, BlockState state, int count) {
        var block = (SpikeBlock) state.getBlock();
        var pos = h.absolutePos(new BlockPos(4, 20, 4));
//? if >=1.21.1 {
        /*var result = block.getCloneItemStack(state,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false), h.getLevel(), pos, null);
*///?} else {
        var result = block.getCloneItemStack(h.getLevel(), pos, state);
//?}
        result.setCount(count);
        return result;
    }

    @GameTest(template = "test_empty")
    public static void spikeMaterialsSkipMismatchedStock(GameTestHelper h) {
        var block = spike(h);
        for (boolean secondary : new boolean[]{false, true}) {
            var clicked = block.defaultBlockState().setValue(SpikeBlock.SECONDARY, secondary);
            exercise(h, clicked, stock(h, clicked.setValue(SpikeBlock.SECONDARY, !secondary), 4),
                    stock(h, clicked, 3), Direction.UP);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void spikeModesAndDefaultPreservePlacementFacing(GameTestHelper h) {
        var block = spike(h);
        for (var mode : SpikeBlock.Mode.values()) {
            var clicked = block.defaultBlockState().setValue(SpikeBlock.MODE, mode).setValue(SpikeBlock.FACING, Direction.NORTH);
            var other = clicked.setValue(SpikeBlock.MODE, mode == SpikeBlock.Mode.OMNI ? SpikeBlock.Mode.WALL : SpikeBlock.Mode.OMNI);
            for (var face : Direction.values())
                exercise(h, clicked, stock(h, other, 4), stock(h, clicked, 3), face);
        }
        // Bare items place primary WALL, not the block's default OMNI form.
        var wall = block.defaultBlockState().setValue(SpikeBlock.MODE, SpikeBlock.Mode.WALL).setValue(SpikeBlock.FACING, Direction.NORTH);
        exercise(h, wall, stock(h, block.defaultBlockState(), 4), new ItemStack(block, 3), Direction.EAST);
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void spikeNoMatchingIdentityPreservesStockAndWear(GameTestHelper h) {
        var block = spike(h);
        var clicked = block.defaultBlockState();
        exercise(h, clicked, stock(h, clicked.setValue(SpikeBlock.SECONDARY, true), 4), ItemStack.EMPTY, Direction.UP);
        exercise(h, clicked, stock(h, clicked.setValue(SpikeBlock.MODE, SpikeBlock.Mode.FALLING), 4), ItemStack.EMPTY, Direction.UP);
        exercise(h, clicked, new ItemStack(block, 4), ItemStack.EMPTY, Direction.UP);
        h.succeed();
    }

    private static void exercise(GameTestHelper h, BlockState clicked, ItemStack wrong, ItemStack right, Direction face) {
//? if >=1.21.1 {
        /*var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "[gt-wand-spike]"));
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        var pos = h.absolutePos(new BlockPos(4, 20, 4));
        var target = pos.relative(face);
        player.setPos(pos.getX() - 3, pos.getY(), pos.getZ() - 3);
        var wand = GTToolHelper.write(GTToolItems.empty(GTToolType.BUILDER_WAND),
                GTMaterialRegistry.get("Steel"), GTMaterialRegistry.get("Wood"));
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        player.getInventory().setItem(35, wrong.copy());
        player.getInventory().setItem(34, right.copy());
        var beforeSupport = h.getLevel().getBlockState(pos);
        var beforeTarget = h.getLevel().getBlockState(target);
        try {
            h.getLevel().setBlockAndUpdate(pos, clicked);
            h.getLevel().setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
            player.gameMode.useItemOn(player, h.getLevel(), wand, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos).add(face.getStepX() * .5, face.getStepY() * .5, face.getStepZ() * .5), face, pos, false));
            h.assertTrue(h.getLevel().getBlockState(pos).equals(clicked), "support identity untouched");
            var expectedState = clicked.setValue(SpikeBlock.FACING, face.getOpposite());
            h.assertTrue(right.isEmpty() ? h.getLevel().isEmptyBlock(target)
                    : h.getLevel().getBlockState(target).equals(expectedState), "material/mode preserved, placement facing follows clicked face " + face);
            h.assertTrue(sameStack(player.getInventory().getItem(35), wrong), "wrong higher-priority stock untouched");
            var expectedStock = right.copy();
            if (!expectedStock.isEmpty()) expectedStock.shrink(1);
            h.assertTrue(sameStack(player.getInventory().getItem(34), expectedStock), "matching stock charged once or absent preserved");
            h.assertTrue(player.getMainHandItem() == wand && wand.getCount() == 1
                    && wand.getDamageValue() == (right.isEmpty() ? 0 : 1), "exact wand identity/wear");
        } finally {
            h.getLevel().setBlockAndUpdate(target, beforeTarget);
            h.getLevel().setBlockAndUpdate(pos, beforeSupport);
        }
    }

    private static boolean sameStack(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return a.isEmpty() && b.isEmpty();
//? if >=1.21.1 {
        /*return a.getCount() == b.getCount() && ItemStack.isSameItemSameComponents(a, b);
*///?} else {
        return a.getCount() == b.getCount() && ItemStack.isSameItemSameTags(a, b);
//?}
    }
}
