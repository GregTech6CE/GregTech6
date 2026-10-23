package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.BlockEvent;
//?}

/** Regression for SolidBucketItem replacing the real wand hand while placing a synthetic source. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class BuilderContainerTransactionTests {
    public static final class PlacementListener {
        final ServerPlayer player;
        boolean cancel;
        int calls;
        PlacementListener(ServerPlayer player) { this.player = player; }
        @SubscribeEvent public void placed(BlockEvent.EntityPlaceEvent event) {
            if (event.getEntity() != player) return;
            calls++;
            if (cancel) event.setCanceled(true);
        }
    }

    @GameTest(template = "test_empty")
    public static void powderBucketSurvivalMainHand(GameTestHelper h) { exercise(h, false, InteractionHand.MAIN_HAND); }
    @GameTest(template = "test_empty")
    public static void powderBucketSurvivalOffhand(GameTestHelper h) { exercise(h, false, InteractionHand.OFF_HAND); }
    @GameTest(template = "test_empty")
    public static void powderBucketCreativeMainHand(GameTestHelper h) { exercise(h, true, InteractionHand.MAIN_HAND); }
    @GameTest(template = "test_empty")
    public static void powderBucketCreativeOffhand(GameTestHelper h) { exercise(h, true, InteractionHand.OFF_HAND); }

    private static void exercise(GameTestHelper h, boolean creative, InteractionHand hand) {
//? if >=1.21.1 {
        /*var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "[gt-wand-bucket]"));
        player.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        var pos = h.absolutePos(new BlockPos(4, 20, 4));
        player.setPos(pos.getX() - 3, pos.getY(), pos.getZ() - 3);
        var tool = GTToolHelper.write(GTToolItems.empty(GTToolType.BUILDER_WAND),
                GTMaterialRegistry.get("Steel"), GTMaterialRegistry.get("Wood"));
        var otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        var sentinel = new ItemStack(Items.BLAZE_ROD, 3);
        // Full inventory: the committed empty bucket must use its source's now-empty slot.
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        player.setItemInHand(hand, tool);
        player.setItemInHand(otherHand, sentinel);
        player.getInventory().setItem(35, new ItemStack(Items.POWDER_SNOW_BUCKET));
        var supportBefore = h.getLevel().getBlockState(pos);
        var targetBefore = h.getLevel().getBlockState(pos.above());
        var listener = new PlacementListener(player);
//? if >=1.21.1 {
        /*net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(listener);
*///?} else {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(listener);
//?}
        try {
            h.getLevel().setBlockAndUpdate(pos, Blocks.POWDER_SNOW.defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            for (boolean cancel : new boolean[]{true, false}) {
                listener.cancel = cancel;
                int beforeCalls = listener.calls;
                player.gameMode.useItemOn(player, h.getLevel(), tool, hand,
                        new BlockHitResult(new Vec3(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5), Direction.UP, pos, false));
                h.assertTrue(listener.calls == beforeCalls + 1, "one native event for bucket placement");
                h.assertTrue(player.getItemInHand(hand) == tool && tool.getCount() == 1, "same wand remains in original hand");
                h.assertTrue(player.getItemInHand(otherHand) == sentinel && sentinel.getCount() == 3, "other hand untouched");
                h.assertTrue(tool.getDamageValue() == (cancel || creative ? 0 : 1), "wear only on accepted survival placement");
                h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.POWDER_SNOW), "support unchanged");
                h.assertTrue(cancel ? h.getLevel().isEmptyBlock(pos.above())
                        : h.getLevel().getBlockState(pos.above()).is(Blocks.POWDER_SNOW), "world follows cancellation result");
                var expected = cancel || creative ? Items.POWDER_SNOW_BUCKET : Items.BUCKET;
                var slot = player.getInventory().getItem(35);
                h.assertTrue(slot.is(expected) && slot.getCount() == 1, "exact source/container in original source slot");
                int powder = 0, empty = 0;
                for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
                    var stack = player.getInventory().getItem(index);
                    if (stack.is(Items.POWDER_SNOW_BUCKET)) powder += stack.getCount();
                    if (stack.is(Items.BUCKET)) empty += stack.getCount();
                }
                h.assertTrue(powder == (cancel || creative ? 1 : 0) && empty == (cancel || creative ? 0 : 1),
                        "no duplicate or missing source/container across inventory and hands");
                h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(2)).isEmpty(), "no loose extras with full inventory");
            }
        } finally {
//? if >=1.21.1 {
            /*net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
*///?} else {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(listener);
//?}
            h.getLevel().setBlockAndUpdate(pos.above(), targetBefore);
            h.getLevel().setBlockAndUpdate(pos, supportBefore);
        }
        h.succeed();
    }
}
