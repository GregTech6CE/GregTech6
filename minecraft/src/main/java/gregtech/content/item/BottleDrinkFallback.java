package gregtech.content.item;

import gregtech.content.food.DrinkNutritionRows;
import java.util.List;
import java.util.Map;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * GT6 bottle rows whose fluids are no longer registered. The bottle item stays, and this table keeps its
 * original drink values so removing the fluid does not turn the item into an inert shell.
 *
 * <p>The table is keyed by {@link BottleItem#fluidKey()} (GT6's {@code FL} expression), not by the drink
 * row's registry field; the two differ for Sake, Dressing and every Smoothie.
 */
final class BottleDrinkFallback {
    private static final Map<String, DrinkNutritionRows.Row> ROWS = Map.ofEntries(
            Map.entry(
                    "Sake",
                    new DrinkNutritionRows.Row(
                            "potion.sake",
                            4,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.damageBoost.id", 300, 1, 60)))),
            Map.entry(
                    "Dressing",
                    new DrinkNutritionRows.Row(
                            "potion.dressing",
                            1,
                            0.5f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("PotionsGT.ID_SLIPPERY", 300, 1, 70)))),
            Map.entry(
                    "Smoothie_Grape_White",
                    new DrinkNutritionRows.Row(
                            "whitegrapesmoothie",
                            4,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.hunger.id", 200, 1, 60)))),
            Map.entry(
                    "Smoothie_Grape_Red",
                    new DrinkNutritionRows.Row(
                            "redgrapesmoothie",
                            4,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.hunger.id", 200, 1, 60)))),
            Map.entry(
                    "Smoothie_Cherry",
                    new DrinkNutritionRows.Row(
                            "cherrysmoothie",
                            4,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.hunger.id", 100, 1, 10)))),
            Map.entry(
                    "Smoothie_Plum",
                    new DrinkNutritionRows.Row(
                            "plumsmoothie",
                            1,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.hunger.id", 100, 1, 20)))),
            Map.entry(
                    "Smoothie_Peach",
                    new DrinkNutritionRows.Row(
                            "peachsmoothie",
                            1,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.hunger.id", 100, 1, 20)))),
            Map.entry(
                    "Smoothie_Pear",
                    new DrinkNutritionRows.Row(
                            "pearsmoothie",
                            4,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.hunger.id", 200, 0, 20)))),
            Map.entry(
                    "Smoothie_Apricot",
                    new DrinkNutritionRows.Row(
                            "apricotsmoothie",
                            2,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.digSpeed.id", 1200, 0, 60)))),
            Map.entry(
                    "Smoothie_Elderberry",
                    new DrinkNutritionRows.Row(
                            "elderberrysmoothie",
                            2,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.damageBoost.id", 200, 1, 25)))),
            Map.entry(
                    "Smoothie_Grapefruit",
                    new DrinkNutritionRows.Row(
                            "grapefruitsmoothie",
                            2,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.hunger.id", 200, 1, 60)))),
            Map.entry(
                    "Smoothie_Lime",
                    new DrinkNutritionRows.Row(
                            "limesmoothie",
                            2,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(
                                    new DrinkNutritionRows.Effect("Potion.digSpeed.id", 1200, 0, 60),
                                    new DrinkNutritionRows.Effect("PotionsGT.ID_CONDUCTIVE", 300, 0, 70)))),
            Map.entry(
                    "Smoothie_Orange",
                    new DrinkNutritionRows.Row(
                            "orangesmoothie",
                            2,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(
                                    new DrinkNutritionRows.Effect("Potion.digSpeed.id", 1200, 0, 60),
                                    new DrinkNutritionRows.Effect("PotionsGT.ID_CONDUCTIVE", 300, 0, 70)))));

    private BottleDrinkFallback() {}

    static DrinkNutritionRows.Row row(String fluidKey) {
        return fluidKey == null ? null : ROWS.get(fluidKey);
    }

    static void drink(Player player, DrinkNutritionRows.Row row) {
        player.getFoodData().eat(row.foodLevel(), row.saturation());
        for (var effect : row.effects()) {
            var type = effectType(effect.sourceId());
            if (type != null && player.getRandom().nextInt(100) < effect.chance()) {
                player.addEffect(new MobEffectInstance(
                        type, effect.duration(), effect.amplifier(), false, !row.invisibleParticles()));
            }
        }
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.GENERIC_DRINK,
                SoundSource.PLAYERS,
                0.5F,
                0.4F / (player.level().random.nextFloat() * 0.4F + 0.8F));
    }

//? if >=1.21.1 {
    /*private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effectType(String source) {
        return switch (source) {
            case "Potion.hunger.id" -> MobEffects.HUNGER;
            case "Potion.digSpeed.id" -> MobEffects.DIG_SPEED;
            case "Potion.damageBoost.id" -> MobEffects.DAMAGE_BOOST;
            default -> null;
        };
    }
*///?} else {
    private static net.minecraft.world.effect.MobEffect effectType(String source) {
        return switch (source) {
            case "Potion.hunger.id" -> MobEffects.HUNGER;
            case "Potion.digSpeed.id" -> MobEffects.DIG_SPEED;
            case "Potion.damageBoost.id" -> MobEffects.DAMAGE_BOOST;
            default -> null;
        };
    }
//?}
}
