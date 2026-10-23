package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.OreBlock;
import com.gregtech.gregtech.block.OreHostStone;
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

/** Registered item-use tests: ore identity is not placement orientation. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class BuilderOreVariantTests {
    private static OreBlock ore(GameTestHelper h, String id) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", id));
        h.assertTrue(block instanceof OreBlock, "registered ore fixture " + id);
        return (OreBlock) block;
    }

    @GameTest(template = "test_empty")
    public static void oreHostsSkipMismatchedStock(GameTestHelper h) {
        for (String id : new String[]{"ore_iron", "ore_small_iron"}) {
            var ore = ore(h, id);
            var clicked = ore.stateFor(OreHostStone.DEEPSLATE);
            exercise(h, clicked, OreBlock.withStone(new ItemStack(ore, 4), ore.stateFor(OreHostStone.GRANITE)),
                    OreBlock.withStone(new ItemStack(ore, 3), clicked));
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void oreBrokenFormSkipsMismatchedStock(GameTestHelper h) {
        var ore = ore(h, "ore_iron");
        var intactState = ore.stateFor(OreHostStone.DEEPSLATE);
        var intact = OreBlock.withStone(new ItemStack(ore, 3), intactState);
        // Real bedrock-drill product factory, not hand-written BlockStateTag/components.
        var broken = OreBlock.brokenStack(GTMaterialRegistry.get("Iron"), OreHostStone.DEEPSLATE);
        h.assertTrue(!broken.isEmpty() && OreBlock.isBrokenStack(broken), "registered broken ore factory");
        exercise(h, intactState, broken.copyWithCount(4), intact);
        exercise(h, ore.brokenStateFor(OreHostStone.DEEPSLATE), intact.copyWithCount(4), broken.copyWithCount(3));
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void oreNoMatchingVariantPreservesInventory(GameTestHelper h) {
        var ore = ore(h, "ore_iron");
        var clicked = ore.stateFor(OreHostStone.DEEPSLATE);
        exercise(h, clicked, OreBlock.withStone(new ItemStack(ore, 4), ore.stateFor(OreHostStone.GRANITE)), ItemStack.EMPTY);
        exercise(h, clicked, OreBlock.brokenStack(GTMaterialRegistry.get("Iron"), OreHostStone.DEEPSLATE), ItemStack.EMPTY);
        h.succeed();
    }

    private static void exercise(GameTestHelper h, BlockState clicked, ItemStack wrong, ItemStack right) {
//? if >=1.21.1 {
        /*var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "[gt-wand-ore]"));
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        var pos = h.absolutePos(new BlockPos(4, 20, 4));
        player.setPos(pos.getX() - 3, pos.getY(), pos.getZ() - 3);
        var wand = GTToolHelper.write(GTToolItems.empty(GTToolType.BUILDER_WAND),
                GTMaterialRegistry.get("Steel"), GTMaterialRegistry.get("Wood"));
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        player.getInventory().setItem(35, wrong.copy());
        player.getInventory().setItem(34, right.copy());
        var beforeSupport = h.getLevel().getBlockState(pos);
        var beforeTarget = h.getLevel().getBlockState(pos.above());
        var beforeBase = h.getLevel().getBlockState(pos.below());
        try {
            h.getLevel().setBlockAndUpdate(pos.below(), Blocks.BEDROCK.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos, clicked);
            h.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            player.gameMode.useItemOn(player, h.getLevel(), wand, InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5), Direction.UP, pos, false));
            h.assertTrue(h.getLevel().getBlockState(pos).equals(clicked), "support identity unchanged");
            h.assertTrue(right.isEmpty() ? h.getLevel().isEmptyBlock(pos.above())
                    : h.getLevel().getBlockState(pos.above()).equals(clicked), "only exact ore identity placed");
            h.assertTrue(sameStack(player.getInventory().getItem(35), wrong), "higher-priority wrong stock untouched");
            var expected = right.copy();
            if (!expected.isEmpty()) expected.shrink(1);
            h.assertTrue(sameStack(player.getInventory().getItem(34), expected), "matching stock consumed exactly once or absent preserved");
            h.assertTrue(player.getMainHandItem() == wand && wand.getCount() == 1
                    && wand.getDamageValue() == (right.isEmpty() ? 0 : 1), "wand identity and exact wear");
        } finally {
            h.getLevel().setBlockAndUpdate(pos.above(), beforeTarget);
            h.getLevel().setBlockAndUpdate(pos, beforeSupport);
            h.getLevel().setBlockAndUpdate(pos.below(), beforeBase);
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
