package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.food.FoodStatEvents;
import com.gregtech.gregtech.content.food.GTFoodStats;
import com.gregtech.gregtech.content.food.PlayerFoodStats;
import com.gregtech.gregtech.content.nuclear.PlayerRadiation;
import com.gregtech.gregtech.damage.GTDamageTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * GT6's food statistics: the {@code FoodsGT} table, the {@code EntityFoodTracker} thresholds, the decay
 * pass and the {@code PlayerUseItemEvent.Finish} entry point.
 *
 * <p>All the numbers asserted here are GT6's own ({@code EntityFoodTracker.java:84-185} for the thresholds,
 * {@code CS.FoodsGT} for the table), including the two config defaults GT6 ships with
 * ({@code DeathByOverdosingCertainFoods} and {@code NutritionSystem}, both true).
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class FoodStatsTests {
    private static final int BASE_X = 68000;
    private static final int BASE_Z = 68000;
    private static final int BASE_Y = 100;

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void foodTableMatchesGt6(GameTestHelper helper) {
        // GT6's table, extracted from MultiItemFood + Loader_Recipes_Food/Crops.
        record Expected(ItemStack stack, int alcohol, int caffeine, int dehydration, int sugar, int fat) {}
        List<Expected> expected = List.of(
                new Expected(new ItemStack(Items.ROTTEN_FLESH), 10, 0, 0, 0, 8),
                new Expected(new ItemStack(Items.MUSHROOM_STEW), 0, 10, 0, 5, 0),
                new Expected(new ItemStack(Items.COOKIE), 0, 0, 0, 10, 0),
                new Expected(new ItemStack(Items.COCOA_BEANS), 0, 0, 4, 4, 0),
                new Expected(new ItemStack(Items.APPLE), 0, 0, 0, 8, 0),
                new Expected(new ItemStack(Items.CARROT), 0, 0, 0, 8, 0),
                new Expected(new ItemStack(Items.POTATO), 0, 0, 0, 4, 0),
                new Expected(new ItemStack(Items.COOKED_BEEF), 0, 0, 0, 0, 16),
                new Expected(new ItemStack(Items.BEEF), 0, 0, 0, 0, 16),
                new Expected(new ItemStack(Items.COD), 0, 0, 0, 0, 12),
                new Expected(new ItemStack(Items.COOKED_COD), 0, 0, 0, 0, 12),
                new Expected(new ItemStack(Items.CHICKEN), 0, 0, 0, 0, 12),
                new Expected(new ItemStack(Items.MUTTON), 0, 0, 0, 0, 16),
                new Expected(new ItemStack(Items.PORKCHOP), 0, 0, 0, 0, 16),
                new Expected(new ItemStack(Items.RABBIT), 0, 0, 0, 0, 12));
        for (Expected row : expected) {
            int[] stats = GTFoodStats.stats(row.stack());
            helper.assertTrue(stats != null, row.stack() + " is in the GT food table");
            if (stats == null) continue;
            helper.assertTrue(stats[GTFoodStats.ALCOHOL] == row.alcohol()
                            && stats[GTFoodStats.CAFFEINE] == row.caffeine()
                            && stats[GTFoodStats.DEHYDRATION] == row.dehydration()
                            && stats[GTFoodStats.SUGAR] == row.sugar()
                            && stats[GTFoodStats.FAT] == row.fat(),
                    row.stack() + " has GT6's numbers, got "
                            + java.util.Arrays.toString(stats));
        }
        helper.assertTrue(GTFoodStats.registered() == 201,
                "the generated table has 20 vanilla rows plus 181 of GT6's own food items, got "
                        + GTFoodStats.registered());
        helper.assertTrue(GTFoodStats.stats(new ItemStack(Items.BREAD)) == null,
                "an item GT6 never registered has no statistics");
        helper.assertTrue(GTFoodStats.stats(ItemStack.EMPTY) == null, "an empty stack has none either");
        helper.assertTrue(GTFoodStats.skipped().size() >= 40,
                "the GT6 food with no 1.20.1 counterpart is recorded, got " + GTFoodStats.skipped().size());
        helper.assertTrue(GTFoodStats.skipped().stream().anyMatch(s -> s.startsWith("foodCheese")),
                "cheese is recorded as skipped rather than invented");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void eatingFeedsTheStatistics(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.ALCOHOL) == 0, "a fresh tracker is empty");

        // GT6 GT_API_Proxy:995-1010 -> FoodsGT.get(item) -> changeAlcohol/... (event priority LOWEST).
        finishUsing(player, new ItemStack(Items.ROTTEN_FLESH));
        helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.ALCOHOL) == 10
                        && PlayerFoodStats.get(player, GTFoodStats.FAT) == 8,
                "rotten flesh gives alcohol 10 and fat 8, got alcohol "
                        + PlayerFoodStats.get(player, GTFoodStats.ALCOHOL) + " fat "
                        + PlayerFoodStats.get(player, GTFoodStats.FAT));

        finishUsing(player, new ItemStack(Items.MUSHROOM_STEW));
        helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.CAFFEINE) == 10
                        && PlayerFoodStats.get(player, GTFoodStats.SUGAR) == 5,
                "mushroom stew gives caffeine 10 and sugar 5");

        finishUsing(player, new ItemStack(Items.COOKIE));
        helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.SUGAR) == 15,
                "sugar accumulates (5 + 10), got " + PlayerFoodStats.get(player, GTFoodStats.SUGAR));

        int before = PlayerFoodStats.get(player, GTFoodStats.SUGAR);
        finishUsing(player, new ItemStack(Items.BREAD));
        helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.SUGAR) == before,
                "eating something outside the table changes nothing");

        // GT6 clamps into a signed 7-bit value.
        for (int i = 0; i < 30; i++) {
            finishUsing(player, new ItemStack(Items.COOKIE));
        }
        helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.SUGAR) == 127,
                "the statistics clamp at 127, got " + PlayerFoodStats.get(player, GTFoodStats.SUGAR));
        helper.succeed();
    }

    private static void finishUsing(Player player, ItemStack stack) {
        // Forge 1.20.1's Finish carries the resulting stack as a fourth argument.
        FoodStatEvents.onItemUseFinish(new LivingEntityUseItemEvent.Finish(player, stack, 32, stack.copy()));
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void thresholdsApplyGt6sEffects(GameTestHelper helper) {
        // Alcohol: strength from 25, confusion from 50, both stepping up at 75 and 100.
        Player at25 = helper.makeMockSurvivalPlayer();
        PlayerFoodStats.change(at25, 25, GTFoodStats.ALCOHOL);
        PlayerFoodStats.applyEffects(at25);
        helper.assertTrue(hasEffect(at25, MobEffects.DAMAGE_BOOST, 0, 300),
                "alcohol 25 gives strength I for 300 ticks");
        helper.assertTrue(at25.getEffect(MobEffects.CONFUSION) == null, "and no nausea yet");

        Player at50 = helper.makeMockSurvivalPlayer();
        PlayerFoodStats.change(at50, 50, GTFoodStats.ALCOHOL);
        PlayerFoodStats.applyEffects(at50);
        helper.assertTrue(hasEffect(at50, MobEffects.CONFUSION, 0, 1200)
                        && hasEffect(at50, MobEffects.DAMAGE_BOOST, 1, 300),
                "alcohol 50 gives nausea I for 1200 and strength II for 300");

        Player at75 = helper.makeMockSurvivalPlayer();
        PlayerFoodStats.change(at75, 75, GTFoodStats.ALCOHOL);
        PlayerFoodStats.applyEffects(at75);
        helper.assertTrue(hasEffect(at75, MobEffects.CONFUSION, 1, 1200)
                        && hasEffect(at75, MobEffects.DAMAGE_BOOST, 2, 300),
                "alcohol 75 steps both up");

        Player at100 = helper.makeMockSurvivalPlayer();
        PlayerFoodStats.change(at100, 100, GTFoodStats.ALCOHOL);
        float before = at100.getHealth();
        at100.invulnerableTime = 0;
        PlayerFoodStats.applyEffects(at100);
        helper.assertTrue(hasEffect(at100, MobEffects.CONFUSION, 2, 1200)
                        && hasEffect(at100, MobEffects.DAMAGE_BOOST, 3, 300),
                "alcohol 100 gives the level 3 effects");
        helper.assertTrue(before - at100.getHealth() == 2.0F,
                "alcohol 100 deals GT's 2 overdose damage, health went " + before + " -> " + at100.getHealth());
        helper.assertTrue(at100.getLastDamageSource() != null
                        && at100.getLastDamageSource().is(GTDamageTypes.ALCOHOL),
                "and it is GT's own alcohol damage type, got " + at100.getLastDamageSource());

        // Caffeine: haste from 25, weakness from 50.
        Player caffeine = helper.makeMockSurvivalPlayer();
        PlayerFoodStats.change(caffeine, 100, GTFoodStats.CAFFEINE);
        caffeine.invulnerableTime = 0;
        PlayerFoodStats.applyEffects(caffeine);
        helper.assertTrue(hasEffect(caffeine, MobEffects.DIG_SPEED, 3, 300)
                        && hasEffect(caffeine, MobEffects.WEAKNESS, 2, 1200),
                "caffeine 100 gives haste IV and weakness III");
        helper.assertTrue(caffeine.getLastDamageSource() != null
                        && caffeine.getLastDamageSource().is(GTDamageTypes.CAFFEINE),
                "caffeine overdose uses GT's caffeine damage");

        // Sugar: speed and jump from 25, mining fatigue from 50.
        Player sugar = helper.makeMockSurvivalPlayer();
        PlayerFoodStats.change(sugar, 100, GTFoodStats.SUGAR);
        sugar.invulnerableTime = 0;
        PlayerFoodStats.applyEffects(sugar);
        helper.assertTrue(hasEffect(sugar, MobEffects.MOVEMENT_SPEED, 3, 300)
                        && hasEffect(sugar, MobEffects.JUMP, 3, 300)
                        && hasEffect(sugar, MobEffects.DIG_SLOWDOWN, 2, 1200),
                "sugar 100 gives speed IV, jump boost IV and mining fatigue III");
        helper.assertTrue(sugar.getLastDamageSource() != null
                        && sugar.getLastDamageSource().is(GTDamageTypes.SUGAR), "sugar overdose damage type");

        // Fat: resistance from 25, slowness from 50 (nutrition system).
        Player fat = helper.makeMockSurvivalPlayer();
        PlayerFoodStats.change(fat, 100, GTFoodStats.FAT);
        fat.invulnerableTime = 0;
        PlayerFoodStats.applyEffects(fat);
        helper.assertTrue(hasEffect(fat, MobEffects.DAMAGE_RESISTANCE, 3, 300)
                        && hasEffect(fat, MobEffects.MOVEMENT_SLOWDOWN, 2, 1200),
                "fat 100 gives resistance IV and slowness III");
        helper.assertTrue(fat.getLastDamageSource() != null
                        && fat.getLastDamageSource().is(GTDamageTypes.FAT), "fat overdose damage type");

        // Dehydration: hunger only, four levels.
        Player dehydration = helper.makeMockSurvivalPlayer();
        PlayerFoodStats.change(dehydration, 100, GTFoodStats.DEHYDRATION);
        dehydration.invulnerableTime = 0;
        PlayerFoodStats.applyEffects(dehydration);
        helper.assertTrue(hasEffect(dehydration, MobEffects.HUNGER, 3, 1200),
                "dehydration 100 gives hunger IV");
        helper.assertTrue(dehydration.getLastDamageSource() != null
                        && dehydration.getLastDamageSource().is(GTDamageTypes.DEHYDRATION),
                "dehydration overdose damage type");

        // Below the first threshold nothing happens at all.
        Player calm = helper.makeMockSurvivalPlayer();
        PlayerFoodStats.change(calm, 24, GTFoodStats.ALCOHOL);
        PlayerFoodStats.change(calm, 24, GTFoodStats.SUGAR);
        PlayerFoodStats.applyEffects(calm);
        helper.assertTrue(calm.getActiveEffects().isEmpty() && calm.getHealth() == calm.getMaxHealth(),
                "below 25 the tracker is silent, effects " + calm.getActiveEffects());
        helper.succeed();
    }

    private static boolean hasEffect(Player player, net.minecraft.world.effect.MobEffect effect,
                                     int amplifier, int duration) {
        var instance = player.getEffect(effect);
        return instance != null && instance.getAmplifier() == amplifier && instance.getDuration() == duration;
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void statisticsDecayButRadiationDoesNot(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        PlayerFoodStats.change(player, 5, GTFoodStats.ALCOHOL);
        PlayerFoodStats.change(player, 3, GTFoodStats.SUGAR);
        PlayerFoodStats.change(player, 2, GTFoodStats.FAT);
        PlayerRadiation.change(player, 60);

        PlayerFoodStats.decay(player);
        helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.ALCOHOL) == 4
                        && PlayerFoodStats.get(player, GTFoodStats.SUGAR) == 2
                        && PlayerFoodStats.get(player, GTFoodStats.FAT) == 1,
                "one decay pass takes one point off each statistic");
        helper.assertTrue(PlayerRadiation.dose(player) == 60,
                "radiation never decays (GT6 EntityFoodTracker:193), got " + PlayerRadiation.dose(player));

        for (int i = 0; i < 6; i++) {
            PlayerFoodStats.decay(player);
        }
        helper.assertTrue(PlayerFoodStats.get(player, GTFoodStats.ALCOHOL) == 0
                        && PlayerFoodStats.get(player, GTFoodStats.SUGAR) == 0
                        && PlayerFoodStats.get(player, GTFoodStats.FAT) == 0,
                "decay stops at zero");
        helper.assertTrue(PlayerRadiation.dose(player) == 60, "radiation is still untouched");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void statisticsUseGt6sNbtShape(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        PlayerFoodStats.change(player, 40, GTFoodStats.ALCOHOL);
        PlayerFoodStats.change(player, 7, GTFoodStats.CAFFEINE);
        PlayerFoodStats.change(player, 3, GTFoodStats.DEHYDRATION);
        PlayerFoodStats.change(player, 9, GTFoodStats.SUGAR);
        PlayerFoodStats.change(player, 2, GTFoodStats.FAT);

        CompoundTag saved = new CompoundTag();
        player.saveWithoutId(saved);
        CompoundTag food = saved.getCompound("ForgeData").getCompound("gt.props.food");
        helper.assertTrue(!food.isEmpty(), "the statistics live in gt.props.food like GT6's tracker");
        helper.assertTrue(food.getByte("a") == 40 && food.getByte("c") == 7 && food.getByte("d") == 3
                        && food.getByte("s") == 9 && food.getByte("f") == 2,
                "with GT6's byte keys a/c/d/s/f, got " + food);

        Player restored = helper.makeMockSurvivalPlayer();
        restored.load(saved);
        helper.assertTrue(PlayerFoodStats.get(restored, GTFoodStats.ALCOHOL) == 40
                        && PlayerFoodStats.get(restored, GTFoodStats.SUGAR) == 9,
                "the statistics survive a save/load round trip");

        // Dying clears them (GT6's tracker is not copied on respawn); a dimension change keeps them.
        // Note Forge's argument order: PlayerEvent.Clone(newPlayer, oldPlayer, wasDeath) - the first
        // argument is the NEW player (it becomes getEntity()), which is the reverse of the field names.
        Player kept = helper.makeMockSurvivalPlayer();
        PlayerFoodStats.change(kept, 30, GTFoodStats.ALCOHOL);
        FoodStatEvents.onClone(new PlayerEvent.Clone(kept, player, false));
        helper.assertTrue(PlayerFoodStats.get(kept, GTFoodStats.ALCOHOL) == 30,
                "a non-death clone keeps the statistics");
        Player died = helper.makeMockSurvivalPlayer();
        PlayerFoodStats.change(died, 30, GTFoodStats.ALCOHOL);
        FoodStatEvents.onClone(new PlayerEvent.Clone(died, player, true));
        helper.assertTrue(PlayerFoodStats.get(died, GTFoodStats.ALCOHOL) == 0,
                "dying clears the statistics");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void configDefaultsMatchGt6(GameTestHelper helper) {
        // GT_API.java:511-512 ships both of these as true.
        helper.assertTrue(PlayerFoodStats.OVERDOSE_DEATH, "DeathByOverdosingCertainFoods defaults to true");
        helper.assertTrue(PlayerFoodStats.NUTRITION_SYSTEM, "NutritionSystem defaults to true");
        // Every generated row really is a registered item with at least one non-zero statistic.
        for (GTFoodStats.Row row : com.gregtech.gregtech.data.generated.GTFoodStatsGen.ROWS) {
            helper.assertTrue(BuiltInRegistries.ITEM.getKey(row.item()) != null,
                    row.item() + " is registered");
            helper.assertTrue(row.alcohol() != 0 || row.caffeine() != 0 || row.dehydration() != 0
                            || row.sugar() != 0 || row.fat() != 0 || row.radiation() != 0,
                    row.item() + " carries at least one statistic");
        }
        helper.succeed();
    }
}
