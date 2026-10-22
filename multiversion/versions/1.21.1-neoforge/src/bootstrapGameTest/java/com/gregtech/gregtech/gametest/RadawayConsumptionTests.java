package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.food.GTFoodStats;
import com.gregtech.gregtech.content.food.PlayerFoodStats;
import com.gregtech.gregtech.content.nuclear.PlayerRadiation;
import com.gregtech.gregtech.item.RadawayItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Uses actual player item-use ticks: no direct food-event call or synthetic Finish event. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class RadawayConsumptionTests {
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void lastRadawayPillAppliesOneDose(GameTestHelper helper) {
        awayFromSymptomTick(helper, () -> {
            Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            PlayerRadiation.change(player, 120);
            player.getFoodData().setFoodLevel(20);
            consume(helper, player, radaway(1));
            helper.assertTrue(PlayerRadiation.dose(player) == 70,
                    "one pill must reduce 120 to 70, never double-apply to 20");
            helper.assertTrue(player.getMainHandItem().isEmpty(), "last survival pill is consumed");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void stackedRadawayAppliesOncePerPill(GameTestHelper helper) {
        awayFromSymptomTick(helper, () -> {
            Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            PlayerRadiation.change(player, 120);
            consume(helper, player, radaway(2));
            helper.assertTrue(PlayerRadiation.dose(player) == 70, "first pill applies one dose");
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "one pill remains");
            consume(helper, player, player.getMainHandItem());
            helper.assertTrue(PlayerRadiation.dose(player) == 20, "second separate pill applies its own dose");
            helper.assertTrue(player.getMainHandItem().isEmpty(), "second pill is consumed");
            consume(helper, player, radaway(1));
            helper.assertTrue(PlayerRadiation.dose(player) == 0, "low dose clamps to zero");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void ordinaryFoodStillAppliesStatisticsOnce(GameTestHelper helper) {
        awayFromSymptomTick(helper, () -> {
            Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            player.getFoodData().setFoodLevel(10);
            consume(helper, player, new ItemStack(Items.COOKIE));
            helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.SUGAR) == 10,
                    "ordinary cookie still applies exactly one sugar statistic through Finish");
            helper.assertTrue(PlayerRadiation.dose(player) == 0, "ordinary cookie adds no radiation");
            helper.succeed();
        });
    }

    private static void awayFromSymptomTick(GameTestHelper helper, Runnable action) {
        // Isolate consumption from the independent 50/100-tick symptom/decay handlers.
        if (helper.getLevel().getGameTime() % 50 == 0) helper.runAfterDelay(1, action);
        else action.run();
    }

    private static ItemStack radaway(int count) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "radaway"));
        if (!(item instanceof RadawayItem)) throw new AssertionError("registered Radaway item required");
        return new ItemStack(item, count);
    }

    private static void consume(GameTestHelper helper, Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.isUsingItem(), "native Item.use starts consumption");
        // The helper's mock player is not inserted in the world, so drive its real tick ourselves.
        // LivingEntity completes consumption and the loader emits its actual Finish event.
        for (int tick = 0; tick < 100 && player.isUsingItem(); tick++) player.tick();
        helper.assertTrue(!player.isUsingItem(), "native item-use lifecycle finishes within 100 ticks");
    }
}
