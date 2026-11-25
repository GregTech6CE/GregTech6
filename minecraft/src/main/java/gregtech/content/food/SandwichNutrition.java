package gregtech.content.food;

import gregtech.content.item.BottleItem;
import gregtech.content.nuclear.PlayerRadiation;
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
//? if >=1.21.1 {
        /*return properties == null ? 1 : Math.max(1, properties.nutrition());
*///?} else {
        return properties == null ? 1 : Math.max(1, properties.getNutrition());
//?}
    }
    public static float saturation(ItemStack stack) {
        var bottle = bottleFood(stack);
        if (bottle != null) return bottle.saturation();
        var entry = GTFoodItems.entryFor(stack.getItem());
        if (entry != null && entry.hasFoodStat()) return entry.food().saturation();
        var drink = drink(stack);
        if (drink != null) return drink.saturation();
        var properties = stack.getFoodProperties(null);
//? if >=1.21.1 {
        /*return properties == null ? 0 : (properties.nutrition() > 0 ? properties.saturation() / (2f * properties.nutrition()) : 0f);
*///?} else {
        return properties == null ? 0 : properties.getSaturationModifier();
//?}
    }
    /** No hunger addition or empty containers here: those are already handled by the sandwich. */
    public static void apply(ItemStack stack, Player player) {
        String item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var bottle = bottleFood(stack);
        var can = item.startsWith("gt6ce:") ? CannedFoodCatalog.forId(item.substring("gt6ce:".length())) : null;
        if (can != null && can.rebreathe() > 0) player.setAirSupply(player.getAirSupply() + can.rebreathe());
        if (item.equals("gt6ce:cure_all") || bottle != null && bottle.extinguish()) player.clearFire();
        if (item.equals("gt6ce:cure_all") || bottle != null && bottle.milk()) {
            gregtech.engine.effect.MobEffectCompat.cureWithMilk(player);
        }
        for (var row : SandwichIngredientEffects.forItem(item)) {
            if (player.getRandom().nextInt(100) >= row.chance()) continue;
            // UT.Entities.applyPotion rejects negative durations even for removal tuples.
            if (row.duration() <= 0) continue;
            var id = net.minecraft.resources.ResourceLocation.tryParse(row.effect());
            // IE callbacks are optional, just as the original skips unregistered potion IDs.
            if (id == null) continue;
            if (row.amplifier() < 0) gregtech.engine.effect.MobEffectCompat.remove(player, id);
            else gregtech.engine.effect.MobEffectCompat.apply(
                    player, id, row.duration(), row.amplifier(), row.ambient(), true);
        }
        if (bottle != null && bottle.explosive()) {
            player.level().explode(player, player.getX(), player.getY(), player.getZ(), 4, true, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
            player.hurt(player.damageSources().explosion(player, player), Float.MAX_VALUE);
        }
        var entry = GTFoodItems.entryFor(stack.getItem());
        if (bottle != null) {
            applyRadiation(player, bottle.radiation());
        } else if (entry != null && entry.hasFoodStat()) {
            Integer radiation = GTFoodStats.radiation(stack);
            if (radiation != null) applyRadiation(player, radiation);
        } else {
            var drink = drink(stack);
            if (drink != null) applyRadiation(player, drink.radiation());
        }
    }
    private static void applyRadiation(Player player, int radiation) {
        if (radiation != 0) PlayerRadiation.change(player, radiation);
    }
}
