package gregtech.content.food;

import gregtech.content.data.FluidCatalog;
import gregtech.content.data.generated.GTBottlesGen;
import gregtech.content.data.generated.GTDrinksGen;
import gregtech.content.data.generated.GTFoodItemsGen;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Guards the RFC #363 removal: five statistics are gone, radiation and the bottle items stay. */
class NutritionRemovalTest {
    private static List<String> components(Class<? extends Record> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
    }

    @Test
    void foodStatisticRowsKeepOnlyMedicalRadiationRemoval() {
        assertEquals(2, FoodStatisticRows.ROWS.size());
        assertEquals(List.of("itemId", "radiation"), components(FoodStatisticRows.Row.class));
        Map<String, Integer> values = FoodStatisticRows.ROWS.stream()
                .collect(Collectors.toMap(FoodStatisticRows.Row::itemId, FoodStatisticRows.Row::radiation));
        assertEquals(Map.of("gt6ce:radaway", -50, "gt6ce:cure_all", -999), values);
        assertTrue(GTDrinksGen.ROWS.stream().allMatch(row -> row.radiation() == 0), "drinks add no radiation");
    }

    @Test
    void sharedRowsNoLongerCarryTheFiveStatistics() {
        assertEquals(
                List.of("id", "display", "meta", "food", "container", "containerCount", "hasFoodStat", "line"),
                components(FoodNutritionRows.Entry.class));
        assertEquals(
                List.of(
                        "field",
                        "foodLevel",
                        "saturation",
                        "radiation",
                        "alwaysEdible",
                        "invisibleParticles",
                        "useAction",
                        "milk",
                        "extinguish",
                        "explosive",
                        "effects"),
                components(DrinkNutritionRows.Row.class));
        assertEquals(
                List.of("item", "food", "saturation", "radiation", "milk", "extinguish", "explosive"),
                components(SandwichBottleFoods.Food.class));
    }

    @Test
    void tableSizesArePreserved() {
        assertEquals(251, GTFoodItemsGen.ROWS.size());
        assertEquals(57, CannedFoodCatalog.ROWS.size());
        assertEquals(131, GTDrinksGen.ROWS.size());
        assertEquals(29, SandwichBottleFoods.ROWS.size());
        assertEquals(137, GTBottlesGen.ENTRIES.length / 3);
    }

    @Test
    void closedLoopFluidsAreGoneButTheirBottleItemsStay() {
        FluidCatalog.bootstrap();
        Set<String> removedFields = Set.of(
                "Sake",
                "Dressing",
                "Smoothie_Apricot",
                "Smoothie_Cherry",
                "Smoothie_Elderberry",
                "Smoothie_Grapefruit",
                "Smoothie_Lime",
                "Smoothie_Orange",
                "Smoothie_Peach",
                "Smoothie_Pear",
                "Smoothie_Plum",
                "Smoothie_Grape_Red",
                "Smoothie_Grape_White",
                "Tea_Ice",
                "BAWLS",
                "Holywater",
                "Purple_Drink",
                "Rotten_Drink",
                "Med_Heal",
                "Juice_Coconut",
                "Juice_Currant",
                "Juice_Fig",
                "Juice_Kiwi",
                "Juice_Mango",
                "Juice_Papaya",
                "Juice_Persimmon",
                "Juice_Starfruit",
                "Juice_Hellderberry",
                "Juice_AppleGrC",
                "Juice_Grape_Green",
                "Juice_Grape_Purple",
                "Juice_Potato",
                "Juice",
                "Mash_Hops",
                "MilkSoy",
                "Sauce_Chili",
                "Sauce_BBQ",
                "Cream_Coconut",
                "Smoothie_Coconut",
                "Pina_Colada",
                "Smoothie_Currant",
                "Smoothie_Fig",
                "Smoothie_Kiwi",
                "Smoothie_Mango",
                "Smoothie_Papaya",
                "Smoothie_Persimmon",
                "Smoothie_Starfruit",
                "Smoothie_Grape_Green",
                "Smoothie_Grape_Purple",
                "Smoothie_Fruit",
                "Wine_Grape_Green",
                "Wine_Grape_Purple",
                "Vinegar_Grape",
                "Med_Laxative",
                "Vodka",
                "Leninade",
                "Beer_Dark",
                "Beer_Dragonblood",
                "Sauce_Hot",
                "Sauce_Diabolo",
                "Sauce_Diablo",
                "Sauce_Cow_Level",
                "Sap",
                "Smoothie_Ananas",
                "Smoothie_Banana",
                "Smoothie_Gooseberry",
                "Smoothie_Lemon",
                "Smoothie_Pomegranate",
                "Smoothie_Strawberry");
        for (String field : removedFields) {
            assertNull(FluidCatalog.get(field), field);
        }
        Set<String> removedDrinkFields = Set.of(
                "potion.sake",
                "potion.dressing",
                "apricotsmoothie",
                "cherrysmoothie",
                "elderberrysmoothie",
                "grapefruitsmoothie",
                "limesmoothie",
                "orangesmoothie",
                "peachsmoothie",
                "pearsmoothie",
                "plumsmoothie",
                "redgrapesmoothie",
                "whitegrapesmoothie",
                "icetea",
                "bawls",
                "holywater",
                "purpledrink",
                "rottendrink",
                "medicine.heal",
                "coconutmilk",
                "currantjuice",
                "figjuice",
                "kiwijuice",
                "mangojuice",
                "papayajuice",
                "persimmonjuice",
                "starfruitjuice",
                "hellderberryjuice",
                "grc.applecider0",
                "grapejuice",
                "grc.grapewine0",
                "potatojuice",
                "juice",
                "hopsmash",
                "soymilk",
                "chillysauce",
                "bbqsauce",
                "coconutcream",
                "coconutsmoothie",
                "pina.colada",
                "currantsmoothie",
                "figsmoothie",
                "kiwismoothie",
                "mangosmoothie",
                "papayasmoothie",
                "persimmonsmoothie",
                "starfruitsmoothie",
                "grapesmoothie",
                "purplegrapesmoothie",
                "fruitsmoothie",
                "wine",
                "ricardosanchez",
                "vinegar",
                "medicine.laxative",
                "binnie.vodka",
                "potion.leninade",
                "darkbeer",
                "potion.dragonblood",
                "potion.hotsauce",
                "potion.diabolosauce",
                "potion.diablosauce",
                "potion.diablosauce.strong",
                "pineapplesmoothie",
                "bananasmoothie",
                "gooseberrysmoothie",
                "lemonsmoothie",
                "pomegranatesmoothie",
                "strawberrysmoothie");
        assertFalse(GTDrinksGen.ROWS.stream().anyMatch(row -> removedDrinkFields.contains(row.field())));

        assertNotNull(FluidCatalog.get("Ambrosia"), "Ambrosia is restored as a source product");
        assertTrue(
                GTDrinksGen.ROWS.stream().anyMatch(row -> row.field().equals("potion.ambrosia")),
                "Ambrosia keeps its drink row");

        assertEquals("Sake", GTBottles.fluidKeyOf("sake"));
        assertEquals("Dressing", GTBottles.fluidKeyOf("dressing"));
        assertEquals("Smoothie_Apricot", GTBottles.fluidKeyOf("apricot_smoothie"));
        assertEquals(250, GTBottles.amountOf("sake"));
        assertTrue(GTBottles.fluidKeyOf("apricot_smoothie") != null);
    }
}
