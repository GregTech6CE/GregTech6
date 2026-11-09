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
                    "BAWLS",
                    new DrinkNutritionRows.Row(
                            "bawls",
                            2,
                            0.5f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("Potion.jump.id", 1200, 1, 90), new DrinkNutritionRows.Effect("PotionsGT.ID_STICKY", 300, 0, 90)))),
            Map.entry(
                    "HolyWater",
                    new DrinkNutritionRows.Row(
                            "holywater",
                            0,
                            0.0f,
                            0,
                            true,
                            false,
                            "drink",
                            true,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("Potion.poison.id", 100, 1, 100), new DrinkNutritionRows.Effect("PotionsGT.ID_INSANITY", 600, 1, 100)))),
            Map.entry(
                    "Purple_Drink",
                    new DrinkNutritionRows.Row(
                            "purpledrink",
                            8,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.moveSlowdown.id", 1200, 2, 90)))),
            Map.entry(
                    "Rotten_Drink",
                    new DrinkNutritionRows.Row(
                            "rottendrink",
                            0,
                            0.0f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.hunger.id", 100, 1, 100)))),
            Map.entry(
                    "Sap_Maple",
                    new DrinkNutritionRows.Row(
                            "maplesap",
                            3,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("Potion.hunger.id", 300, 1, 50), new DrinkNutritionRows.Effect("PotionsGT.ID_STICKY", 300, 0, 30)))),
            Map.entry(
                    "Sap_Rainbow",
                    new DrinkNutritionRows.Row(
                            "rainbowsap",
                            5,
                            0.5f,
                            0,
                            false,
                            false,
                            "drink",
                            true,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("Potion.regeneration.id", 60, 4, 100), new DrinkNutritionRows.Effect("PotionsGT.ID_STICKY", 300, 0, 30), new DrinkNutritionRows.Effect("Potion.digSpeed.id", 900, 2, 100)))),
            Map.entry(
                    "Juice_Coconut",
                    new DrinkNutritionRows.Row(
                            "coconutmilk",
                            6,
                            0.8f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.damageBoost.id", 200, 1, 25)))),
            Map.entry(
                    "Juice_Currant",
                    new DrinkNutritionRows.Row(
                            "currantjuice",
                            3,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.damageBoost.id", 200, 1, 25)))),
            Map.entry(
                    "Juice_Fig",
                    new DrinkNutritionRows.Row(
                            "figjuice",
                            2,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.hunger.id", 100, 1, 20)))),
            Map.entry(
                    "Juice_Kiwi",
                    new DrinkNutritionRows.Row(
                            "kiwijuice",
                            4,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("Potion.digSpeed.id", 1200, 0, 60), new DrinkNutritionRows.Effect("PotionsGT.ID_CONDUCTIVE", 300, 0, 70)))),
            Map.entry(
                    "Juice_Mango",
                    new DrinkNutritionRows.Row(
                            "mangojuice",
                            3,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.hunger.id", 100, 1, 20)))),
            Map.entry(
                    "Juice_Papaya",
                    new DrinkNutritionRows.Row(
                            "papayajuice",
                            3,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.hunger.id", 100, 1, 20)))),
            Map.entry(
                    "Juice_Persimmon",
                    new DrinkNutritionRows.Row(
                            "persimmonjuice",
                            3,
                            0.8f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("Potion.digSpeed.id", 1200, 0, 60), new DrinkNutritionRows.Effect("PotionsGT.ID_CONDUCTIVE", 300, 0, 70)))),
            Map.entry(
                    "Juice_Starfruit",
                    new DrinkNutritionRows.Row(
                            "starfruitjuice",
                            3,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.hunger.id", 100, 1, 20)))),
            Map.entry(
                    "Juice_Grape_Green",
                    new DrinkNutritionRows.Row(
                            "grapejuice",
                            4,
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
                    "Juice_Grape_Purple",
                    new DrinkNutritionRows.Row(
                            "grc.grapewine0",
                            4,
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
                    "Juice_Potato",
                    new DrinkNutritionRows.Row(
                            "potatojuice",
                            3,
                            0.3f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of())),
            Map.entry(
                    "Juice",
                    new DrinkNutritionRows.Row(
                            "juice",
                            3,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.hunger.id", 100, 1, 20)))),
            Map.entry(
                    "Mash_Hops",
                    new DrinkNutritionRows.Row(
                            "hopsmash",
                            1,
                            0.1f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of())),
            Map.entry(
                    "MilkSoy",
                    new DrinkNutritionRows.Row(
                            "soymilk",
                            0,
                            0.0f,
                            0,
                            true,
                            false,
                            "drink",
                            true,
                            false,
                            false,
                            List.of())),
            Map.entry(
                    "Sauce_Chili",
                    new DrinkNutritionRows.Row(
                            "chillysauce",
                            2,
                            0.1f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("PotionsGT.ID_FLAMMABLE", 300, 0, 70), new DrinkNutritionRows.Effect("Potion.confusion.id", 1000, 0, 10), new DrinkNutritionRows.Effect("Potion.fireResistance.id", 1000, 0, 60), new DrinkNutritionRows.Effect("PotionsGT.ID_DEHYDRATION", 400, 0, 100)))),
            Map.entry(
                    "Sauce_BBQ",
                    new DrinkNutritionRows.Row(
                            "bbqsauce",
                            4,
                            0.3f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("Potion.heal.id", 1, 0, 10), new DrinkNutritionRows.Effect("Potion.moveSpeed.id", 200, 0, 20)))),
            Map.entry(
                    "Syrup_Maple",
                    new DrinkNutritionRows.Row(
                            "maplesyrup",
                            4,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("PotionsGT.ID_STICKY", 300, 0, 70)))),
            Map.entry(
                    "Cream_Coconut",
                    new DrinkNutritionRows.Row(
                            "coconutcream",
                            2,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of())),
            Map.entry(
                    "Smoothie_Coconut",
                    new DrinkNutritionRows.Row(
                            "coconutsmoothie",
                            4,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.damageBoost.id", 200, 1, 25)))),
            Map.entry(
                    "Pina_Colada",
                    new DrinkNutritionRows.Row(
                            "pina.colada",
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
                    "Smoothie_Currant",
                    new DrinkNutritionRows.Row(
                            "currantsmoothie",
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
                    "Smoothie_Fig",
                    new DrinkNutritionRows.Row(
                            "figsmoothie",
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
                    "Smoothie_Kiwi",
                    new DrinkNutritionRows.Row(
                            "kiwismoothie",
                            3,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("Potion.digSpeed.id", 1200, 0, 60), new DrinkNutritionRows.Effect("PotionsGT.ID_CONDUCTIVE", 300, 0, 70)))),
            Map.entry(
                    "Smoothie_Mango",
                    new DrinkNutritionRows.Row(
                            "mangosmoothie",
                            2,
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
                    "Smoothie_Papaya",
                    new DrinkNutritionRows.Row(
                            "papayasmoothie",
                            2,
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
                    "Smoothie_Persimmon",
                    new DrinkNutritionRows.Row(
                            "persimmonsmoothie",
                            2,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("Potion.digSpeed.id", 1200, 0, 60), new DrinkNutritionRows.Effect("PotionsGT.ID_CONDUCTIVE", 300, 0, 70)))),
            Map.entry(
                    "Smoothie_Starfruit",
                    new DrinkNutritionRows.Row(
                            "starfruitsmoothie",
                            2,
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
                    "Smoothie_Grape_Green",
                    new DrinkNutritionRows.Row(
                            "grapesmoothie",
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
                    "Smoothie_Grape_Purple",
                    new DrinkNutritionRows.Row(
                            "purplegrapesmoothie",
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
                    "Smoothie_Fruit",
                    new DrinkNutritionRows.Row(
                            "fruitsmoothie",
                            2,
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
                    "Wine_Grape_Green",
                    new DrinkNutritionRows.Row(
                            "wine",
                            2,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.heal.id", 1, 0, 60)))),
            Map.entry(
                    "Wine_Grape_Purple",
                    new DrinkNutritionRows.Row(
                            "ricardosanchez",
                            2,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.heal.id", 1, 0, 60)))),
            Map.entry(
                    "Vinegar_Grape",
                    new DrinkNutritionRows.Row(
                            "vinegar",
                            2,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.heal.id", 1, 1, 70)))),
            Map.entry(
                    "Med_Laxative",
                    new DrinkNutritionRows.Row(
                            "medicine.laxative",
                            0,
                            0.0f,
                            0,
                            true,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.hunger.id", 300, 10, 100)))),
            Map.entry(
                    "Med_Heal",
                    new DrinkNutritionRows.Row(
                            "medicine.heal",
                            0,
                            0.0f,
                            0,
                            true,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.regeneration.id", 120, 4, 100)))),
            Map.entry(
                    "Vodka",
                    new DrinkNutritionRows.Row(
                            "binnie.vodka",
                            2,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.damageBoost.id", 500, 1, 60)))),
            Map.entry(
                    "Leninade",
                    new DrinkNutritionRows.Row(
                            "potion.leninade",
                            2,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.damageBoost.id", 500, 2, 90)))),
            Map.entry(
                    "Beer_Dark",
                    new DrinkNutritionRows.Row(
                            "darkbeer",
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
                    "Beer_Dragonblood",
                    new DrinkNutritionRows.Row(
                            "potion.dragonblood",
                            4,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("Potion.damageBoost.id", 300, 2, 90), new DrinkNutritionRows.Effect("PotionsGT.ID_INSANITY", 600, 0, 100)))),
            Map.entry(
                    "Sauce_Hot",
                    new DrinkNutritionRows.Row(
                            "potion.hotsauce",
                            2,
                            0.1f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("PotionsGT.ID_FLAMMABLE", 300, 0, 70), new DrinkNutritionRows.Effect("Potion.confusion.id", 2000, 0, 30), new DrinkNutritionRows.Effect("Potion.fireResistance.id", 2000, 0, 70), new DrinkNutritionRows.Effect("PotionsGT.ID_DEHYDRATION", 400, 1, 100)))),
            Map.entry(
                    "Sauce_Diabolo",
                    new DrinkNutritionRows.Row(
                            "potion.diabolosauce",
                            2,
                            0.1f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("PotionsGT.ID_FLAMMABLE", 300, 1, 70), new DrinkNutritionRows.Effect("Potion.confusion.id", 3000, 1, 50), new DrinkNutritionRows.Effect("Potion.fireResistance.id", 3000, 0, 80), new DrinkNutritionRows.Effect("PotionsGT.ID_DEHYDRATION", 400, 2, 100), new DrinkNutritionRows.Effect("PotionsGT.ID_INSANITY", 600, 0, 100)))),
            Map.entry(
                    "Sauce_Diablo",
                    new DrinkNutritionRows.Row(
                            "potion.diablosauce",
                            2,
                            0.1f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("PotionsGT.ID_FLAMMABLE", 300, 1, 70), new DrinkNutritionRows.Effect("Potion.confusion.id", 4000, 1, 70), new DrinkNutritionRows.Effect("Potion.fireResistance.id", 4000, 0, 90), new DrinkNutritionRows.Effect("PotionsGT.ID_DEHYDRATION", 400, 3, 100), new DrinkNutritionRows.Effect("PotionsGT.ID_INSANITY", 600, 1, 100)))),
            Map.entry(
                    "Sauce_Cow_Level",
                    new DrinkNutritionRows.Row(
                            "potion.diablosauce.strong",
                            2,
                            0.1f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("PotionsGT.ID_FLAMMABLE", 300, 2, 90), new DrinkNutritionRows.Effect("Potion.confusion.id", 9999, 2, 999), new DrinkNutritionRows.Effect("Potion.fireResistance.id", 9999, 9, 999), new DrinkNutritionRows.Effect("PotionsGT.ID_DEHYDRATION", 400, 4, 100), new DrinkNutritionRows.Effect("PotionsGT.ID_INSANITY", 600, 2, 100)))),
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
                            List.of( new DrinkNutritionRows.Effect("Potion.digSpeed.id", 1200, 0, 60), new DrinkNutritionRows.Effect("PotionsGT.ID_CONDUCTIVE", 300, 0, 70)))),
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
                            List.of( new DrinkNutritionRows.Effect("Potion.digSpeed.id", 1200, 0, 60), new DrinkNutritionRows.Effect("PotionsGT.ID_CONDUCTIVE", 300, 0, 70)))),
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
                    "goldencarrotjuice",
                    new DrinkNutritionRows.Row(
                            "goldencarrotjuice",
                            4,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.nightVision.id", 1200, 0, 90)))),
            Map.entry(
                    "potion.goldenapplejuice",
                    new DrinkNutritionRows.Row(
                            "potion.goldenapplejuice",
                            4,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("Potion.field_76444_x.id", 2400, 0, 100), new DrinkNutritionRows.Effect("Potion.regeneration.id", 100, 1, 100)))),
            Map.entry(
                    "potion.goldencider",
                    new DrinkNutritionRows.Row(
                            "potion.goldencider",
                            4,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.field_76444_x.id", 2400, 1, 95)))),
            Map.entry(
                    "potion.idunsapplejuice",
                    new DrinkNutritionRows.Row(
                            "potion.idunsapplejuice",
                            4,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("Potion.regeneration.id", 600, 4, 100), new DrinkNutritionRows.Effect("Potion.field_76444_x.id", 2400, 0, 100), new DrinkNutritionRows.Effect("Potion.resistance.id", 6000, 0, 100), new DrinkNutritionRows.Effect("Potion.fireResistance.id", 6000, 0, 100)))),
            Map.entry(
                    "potion.notchesbrew",
                    new DrinkNutritionRows.Row(
                            "potion.notchesbrew",
                            4,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("Potion.regeneration.id", 700, 4, 95), new DrinkNutritionRows.Effect("Potion.field_76444_x.id", 3000, 1, 95), new DrinkNutritionRows.Effect("Potion.resistance.id", 7000, 1, 95), new DrinkNutritionRows.Effect("Potion.fireResistance.id", 7000, 0, 95)))),
            Map.entry(
                    "Smoothie_Ananas",
                    new DrinkNutritionRows.Row(
                            "pineapplesmoothie",
                            2,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.digSpeed.id", 1200, 0, 60)))),
            Map.entry(
                    "Smoothie_Banana",
                    new DrinkNutritionRows.Row(
                            "bananasmoothie",
                            4,
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
                    "Smoothie_Gooseberry",
                    new DrinkNutritionRows.Row(
                            "gooseberrysmoothie",
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
                    "Smoothie_Lemon",
                    new DrinkNutritionRows.Row(
                            "lemonsmoothie",
                            2,
                            0.4f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of( new DrinkNutritionRows.Effect("Potion.digSpeed.id", 1200, 0, 60), new DrinkNutritionRows.Effect("PotionsGT.ID_CONDUCTIVE", 300, 0, 70)))),
            Map.entry(
                    "Smoothie_Pomegranate",
                    new DrinkNutritionRows.Row(
                            "pomegranatesmoothie",
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
                    "Smoothie_Strawberry",
                    new DrinkNutritionRows.Row(
                            "strawberrysmoothie",
                            4,
                            0.2f,
                            0,
                            false,
                            false,
                            "drink",
                            false,
                            false,
                            false,
                            List.of(new DrinkNutritionRows.Effect("Potion.hunger.id", 100, 1, 10))))
    );

    private BottleDrinkFallback() {}

    static DrinkNutritionRows.Row row(String fluidKey) {
        return fluidKey == null ? null : ROWS.get(fluidKey);
    }

    static void drink(Player player, DrinkNutritionRows.Row row) {
        player.getFoodData().eat(row.foodLevel(), row.saturation());
        if (row.extinguish()) player.clearFire();
        if (row.milk()) {
//? if >=1.21.1 {
            /*player.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);
*///?} else {
            player.curePotionEffects(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MILK_BUCKET));
//?}
        }
        for (var effect : row.effects()) {
            var type = effectType(effect.sourceId());
            if (type == null || player.getRandom().nextInt(100) >= effect.chance()) continue;
//? if >=1.21.1 {
            /*if (type.value().isInstantenous()) type.value().applyInstantenousEffect(player, player, player, effect.amplifier(), 1.0);
            else player.addEffect(new MobEffectInstance(type, effect.duration(), effect.amplifier(), false, !row.invisibleParticles()));
*///?} else {
            if (type.isInstantenous()) type.applyInstantenousEffect(player, player, player, effect.amplifier(), 1.0);
            else player.addEffect(new MobEffectInstance(type, effect.duration(), effect.amplifier(), false, !row.invisibleParticles()));
//?}
        }
        if (row.explosive()) {
            player.level().explode(player, player.getX(), player.getY(), player.getZ(), 4, true,
                    net.minecraft.world.level.Level.ExplosionInteraction.TNT);
            player.hurt(player.damageSources().explosion(player, player), Float.MAX_VALUE);
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
            case "Potion.confusion.id" -> MobEffects.CONFUSION;
            case "Potion.damageBoost.id" -> MobEffects.DAMAGE_BOOST;
            case "Potion.digSpeed.id" -> MobEffects.DIG_SPEED;
            case "Potion.field_76444_x.id" -> MobEffects.ABSORPTION;
            case "Potion.fireResistance.id" -> MobEffects.FIRE_RESISTANCE;
            case "Potion.heal.id" -> MobEffects.HEAL;
            case "Potion.hunger.id" -> MobEffects.HUNGER;
            case "Potion.jump.id" -> MobEffects.JUMP;
            case "Potion.moveSlowdown.id" -> MobEffects.MOVEMENT_SLOWDOWN;
            case "Potion.moveSpeed.id" -> MobEffects.MOVEMENT_SPEED;
            case "Potion.nightVision.id" -> MobEffects.NIGHT_VISION;
            case "Potion.poison.id" -> MobEffects.POISON;
            case "Potion.regeneration.id" -> MobEffects.REGENERATION;
            case "Potion.resistance.id" -> MobEffects.DAMAGE_RESISTANCE;
            default -> null;
        };
    }
*///?} else {
    private static net.minecraft.world.effect.MobEffect effectType(String source) {
        return switch (source) {
            case "Potion.confusion.id" -> MobEffects.CONFUSION;
            case "Potion.damageBoost.id" -> MobEffects.DAMAGE_BOOST;
            case "Potion.digSpeed.id" -> MobEffects.DIG_SPEED;
            case "Potion.field_76444_x.id" -> MobEffects.ABSORPTION;
            case "Potion.fireResistance.id" -> MobEffects.FIRE_RESISTANCE;
            case "Potion.heal.id" -> MobEffects.HEAL;
            case "Potion.hunger.id" -> MobEffects.HUNGER;
            case "Potion.jump.id" -> MobEffects.JUMP;
            case "Potion.moveSlowdown.id" -> MobEffects.MOVEMENT_SLOWDOWN;
            case "Potion.moveSpeed.id" -> MobEffects.MOVEMENT_SPEED;
            case "Potion.nightVision.id" -> MobEffects.NIGHT_VISION;
            case "Potion.poison.id" -> MobEffects.POISON;
            case "Potion.regeneration.id" -> MobEffects.REGENERATION;
            case "Potion.resistance.id" -> MobEffects.DAMAGE_RESISTANCE;
            default -> null;
        };
    }
//?}
}
