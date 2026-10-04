package com.gregtech.gregtech.content.food;

import com.gregtech.gregtech.item.BottleItem;
import com.gregtech.gregtech.content.nuclear.PlayerRadiation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** ST.food/ST.saturation and FoodStat.onEaten(false): each occupied layer is one serving. */
public final class SandwichNutrition {
    private SandwichNutrition() {}
    private static GTDrinks.Row drink(ItemStack stack) {
        return stack.getItem() instanceof BottleItem bottle && bottle.fluid() != null
                ? GTDrinks.rowFor(bottle.fluid()) : null;
    }
    private static SandwichBottleFoods.Food bottleFood(ItemStack stack) {
        return SandwichBottleFoods.forItem(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
    public static int food(ItemStack stack) {
        var bottle = bottleFood(stack);
        if (bottle != null) return Math.max(1, bottle.food());
        var entry = GTFoodItems.entryFor(stack.getItem());
        if (entry != null && entry.hasFoodStat()) return Math.max(1, entry.food().level());
        var drink = drink(stack);
        if (drink != null) return Math.max(1, drink.foodLevel());
        var properties = stack.getFoodProperties(null);
        return properties == null ? 1 : Math.max(1, properties.nutrition());
    }
    public static float saturation(ItemStack stack) {
        var bottle = bottleFood(stack);
        if (bottle != null) return bottle.saturation();
        var entry = GTFoodItems.entryFor(stack.getItem());
        if (entry != null && entry.hasFoodStat()) return entry.food().saturation();
        var drink = drink(stack);
        if (drink != null) return drink.saturation();
        var properties = stack.getFoodProperties(null);
        return properties == null ? 0 : (properties.nutrition() > 0 ? properties.saturation() / (2f * properties.nutrition()) : 0f);
    }
    /** No hunger addition or empty containers here: those are already handled by the sandwich. */
    public static void apply(ItemStack stack, Player player) {
        String item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var bottle = bottleFood(stack);
        if (item.equals("gregtech:cure_all") || bottle != null && bottle.extinguish()) player.clearFire();
        if (item.equals("gregtech:cure_all") || bottle != null && bottle.milk()) {
            player.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
        }
        for (var row : SandwichIngredientEffects.forItem(item)) {
            if (player.getRandom().nextInt(100) >= row.chance()) continue;
            // UT.Entities.applyPotion:3057 rejects negative durations even for removal tuples.
            if (row.duration() <= 0) continue;
            var id = net.minecraft.resources.ResourceLocation.parse(row.effect());
            var effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
            // IE callbacks are optional, just as the original skips unregistered potion IDs.
            if (effect == null) continue;
            if (row.amplifier() < 0) player.removeEffect(effect);
            else player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect,
                    row.duration(), row.amplifier(), row.ambient(), true));
        }
        if (bottle != null && bottle.explosive()) {
            player.level().explode(player, player.getX(), player.getY(), player.getZ(), 4, true, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
            player.hurt(player.damageSources().explosion(player, player), Float.MAX_VALUE);
        }
        var entry = GTFoodItems.entryFor(stack.getItem());
        if (bottle != null) {
            applyStats(player, bottle.alcohol(), bottle.caffeine(), bottle.dehydration(), bottle.sugar(), bottle.fat(), bottle.radiation());
        } else if (entry != null && entry.hasFoodStat()) {
            var values = entry.stats();
            applyStats(player, values.alcohol(), values.caffeine(), values.dehydration(), values.sugar(), values.fat(), values.radiation());
        } else {
            var drink = drink(stack);
            if (drink != null) applyStats(player, drink.alcohol(), drink.caffeine(), drink.dehydration(), drink.sugar(), drink.fat(), drink.radiation());
        }
    }
    private static void applyStats(Player player, int alcohol, int caffeine, int dehydration, int sugar, int fat, int radiation) {
        int[] values = {alcohol, caffeine, dehydration, sugar, fat};
        for (int stat = 0; stat < values.length; stat++) if (values[stat] != 0) PlayerFoodStats.change(player, values[stat], stat);
        if (radiation != 0) PlayerRadiation.change(player, radiation);
    }
}
