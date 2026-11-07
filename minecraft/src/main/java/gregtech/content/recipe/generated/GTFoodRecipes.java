package gregtech.content.recipe.generated;

import static gregtech.content.recipe.generated.GeneratedRows.*;
import static gregtech.engine.recipe.chem.Rounding.CEIL;
import static gregtech.engine.recipe.chem.Rounding.FLOOR;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MaterialGroups;
import gregtech.engine.recipe.chem.ChemRecipe;
import java.util.ArrayList;
import java.util.List;

/**
 * The portable {@code Loader_Recipes_Food} machine rows.
 *
 * <p>The source iterates meat forms, sugar forms and smoothie flavours; each loop is expanded so
 * every form and flavour keeps its own row. Non-material food rows that name other mods' items
 * are recorded as exclusions.</p>
 */
public final class GTFoodRecipes {
    private static final String SOURCE = "gt6@3703e403/Loader_Recipes_Food:row/";

    private record Form(String prefix, int count, int ticks) {}

    private static final Form[] MEAT_FORMS = {
        new Form("dustTiny", 1, 16), new Form("dustSmall", 1, 36), new Form("dust", 1, 144),
        new Form("nugget", 1, 16), new Form("chunkGt", 1, 36), new Form("billet", 1, 96),
        new Form("ingot", 1, 144),
    };

    private static final Form[] SUGAR_FORMS = {
        new Form("gemChipped", 4, 48), new Form("dust", 1, 48),
        new Form("dustSmall", 4, 48), new Form("dustTiny", 9, 48),
    };

    /** The source iterates water, mineral water and distilled water; spectral dew is retired. */
    private static final String[] WATERS = {"Water", "MnWtr", "DistW"};

    private static final String[] SMOOTHIE_JUICES = {"Juice", "Juice_Ananas", "Juice_Apple", "Juice_AppleGrC",
        "Juice_Banana", "Juice_Blackberry", "Juice_Blueberry", "Juice_Cranberry", "Juice_Currant",
        "Juice_Gooseberry", "Juice_Grape_Green", "Juice_Grape_Purple", "Juice_Kiwi", "Juice_Lemon",
        "Juice_Melon", "Juice_Persimmon", "Juice_Raspberry", "Juice_Strawberry", "Juice_Mango",
        "Juice_Starfruit", "Juice_Fig", "Juice_Pomegranate", "Juice_Papaya", "Juice_Coconut"};

    private GTFoodRecipes() {}

    public static void register() {
        List<ChemRecipe> rows = new ArrayList<>();
        meatRows(rows);
        waterRows(rows);
        flourDoughRows(rows);
        flourGrainsRows(rows);
        compressorRows(rows);
        wineRows(rows);
        sugarRows(rows);
        chiliRows(rows);
        smoothieRows(rows);
        sourceRows(rows);
        ChemRecipeResolver.exclude(SOURCE + "optional",
                "other mods' crops, fish and prepared foods are absent; those rows are not approximated");
        ChemRecipeResolver.exclude(SOURCE + "640-648",
                "the orange, elderberry and plum wines and the fortified/fruit loops need fluids "
                        + "this port does not register");
        publish("food recipes", rows);
    }

    private static void meatRows(List<ChemRecipe> rows) {
        for (String[] meat : new String[][] {
                {"MeatRaw", "MeatRotten"}, {"MeatCooked", "MeatRotten"},
                {"FishRaw", "FishRotten"}, {"FishCooked", "FishRotten"}}) {
            for (Form form : MEAT_FORMS)
                row(SOURCE + "1-28/" + meat[0] + "/" + form.prefix, "Fermenter", 16, form.ticks)
                        .item(form(form.prefix, meat[0], form.count))
                        .out(form(form.prefix, meat[1], form.count)).add(rows);
        }
    }

    /** Loader_Recipes_Food:581-592: the coffee, milk, honey, honeydew and tea rows per water. */
    private static void waterRows(List<ChemRecipe> rows) {
        for (String water : WATERS) {
            String source = SOURCE + "581/" + water;
            row(source, "Distillery", 16, 48).item(dust("Coffee", U(1)))
                    .fluid(waterInput(water, 750)).out(registryFluid("potion.darkcoffee", 750)).add(rows);
            row(source, "Distillery", 16, 16).item(dust("Coffee", U(1, 3)))
                    .fluid(waterInput(water, 250)).out(registryFluid("potion.darkcoffee", 250)).add(rows);
            milk(rows, source, water, "Milk", "Milk");
            milk(rows, source, water, "Honey", "Honey");
            milk(rows, source, water, "Honeydew", "Honeydew");
            row(source, "Mixer", 16, 16).item(dust("Tea", U(1, 9)))
                    .fluid(waterInput(water, 750)).out(nativeFluid("Tea", 750)).add(rows);
            row(source, "Mixer", 16, 36).item(dust("Tea", U(1, 4)))
                    .fluid(waterInput(water, 1687)).out(nativeFluid("Tea", 1687)).add(rows);
            row(source, "Mixer", 16, 144).item(dust("Tea", U(1)))
                    .fluid(waterInput(water, 6750)).out(nativeFluid("Tea", 6750)).add(rows);
        }
    }

    private static void milk(List<ChemRecipe> rows, String source, String water,
            String material, String fluidKey) {
        row(source, "Mixer", 16, 16).item(dust(material, U(1)))
                .fluid(waterInput(water, 500)).out(nativeFluid(fluidKey, 1000)).add(rows);
    }

    /** Loader_Recipes_Food:585: the flour group's dough rows. */
    private static void flourDoughRows(List<ChemRecipe> rows) {
        for (GTMaterial flour : group(MaterialGroups.Flour)) {
            String name = flour.getName();
            if (!hasForm("dust", name)) continue;
            for (String water : WATERS)
                row(SOURCE + "585/" + name + "/" + water, "Mixer", 16, 16)
                        .item(dust(name, U(1))).fluid(waterInput(water, 250))
                        .out(tech("food_dough", 2)).add(rows);
        }
    }

    /** Loader_Recipes_Food:719-724: flour grain turns raw and rotten meat into the ingot form. */
    private static void flourGrainsRows(List<ChemRecipe> rows) {
        for (GTMaterial grain : group(MaterialGroups.FlourGrains)) {
            String name = grain.getName();
            if (!hasForm("dust", name)) continue;
            for (String meat : new String[] {"MeatRaw", "FishRaw", "MeatRotten", "FishRotten"}) {
                if (!hasForm("ingot", meat)) continue;
                row(SOURCE + "719/" + name + "/" + meat, "Mixer", 16, 16)
                        .item(dust(name, U(1))).item(dust(meat, U(1)))
                        .out(ingot(meat, U(1))).add(rows);
            }
        }
    }

    private static gregtech.engine.recipe.chem.ChemFluid waterInput(String water, int millibuckets) {
        return water.equals("Water") ? water(waterUnits(millibuckets), FLOOR) : nativeFluid(water, millibuckets);
    }

    private static boolean hasForm(String prefixName, String materialName) {
        var material = gregtech.engine.material.GTMaterialRegistry.get(materialName);
        if (material == null || !material.resolve().isValid()) return false;
        var entry = gregtech.content.data.MaterialPrefixes.all().get(prefixName);
        if (entry == null) return false;
        var prefix = entry.getDelegate();
        if (prefix != null)
            return !gregtech.content.registry.GTItems.getStack(prefix, material.resolve(), 1).isEmpty();
        var block = entry.getBlockDelegate();
        return block != null
                && !gregtech.content.registry.GTBlocks.getStack(block, material.resolve()).isEmpty();
    }


    /** Loader_Recipes_Food:56-60: compressed meat forms, without the IC2-only extra rows. */
    private static void compressorRows(List<ChemRecipe> rows) {
        for (String material : new String[] {"MeatRotten", "MeatRaw", "MeatCooked",
                "FishRotten", "FishRaw", "FishCooked", "SoylentGreen", "Tofu"}) {
            compressed(rows, material, "dustTiny", "nugget");
            compressed(rows, material, "dustSmall", "chunkGt");
            compressed(rows, material, "dust", "ingot");
        }
    }

    private static void compressed(List<ChemRecipe> rows, String material,
            String inputPrefix, String outputPrefix) {
        if (!hasForm(inputPrefix, material) || !hasForm(outputPrefix, material)) return;
        row(SOURCE + "56/" + material + "/" + inputPrefix, "Compressor", 16, 16)
                .item(form(inputPrefix, material, 1)).out(form(outputPrefix, material, 1)).add(rows);
    }


    /** Loader_Recipes_Food:640-645: wines whose juice and fallback fluid both exist here. */
    private static void wineRows(List<ChemRecipe> rows) {
        wine(rows, 16, "Juice_Kiwi", "Wine_Lemon");
        wine(rows, 16, "Juice_Cranberry", "Wine_Cranberry");
        wine(rows, 16, "Juice_Tomato", "Wine_Tomato");
    }

    private static void wine(List<ChemRecipe> rows, long ticks, String juice, String wine) {
        var stack = gregtech.content.registry.GTFluids.stack(juice, 1);
        if (stack == null || stack.isEmpty()) return;
        row(SOURCE + "640/" + juice, "Fermenter", 16, ticks)
                .kept(selector(0)).fluid(nativeFluid(juice, 50))
                .out(nativeFluid(wine, 25)).add(rows);
    }

    private static void sugarRows(List<ChemRecipe> rows) {
        for (Form sugar : SUGAR_FORMS) {
            sweeten(rows, sugar, "potion.coffee", "potion.cafeaulait");
            sweeten(rows, sugar, "potion.cafeaulait", "potion.laitaucafe");
            sweeten(rows, sugar, "potion.darkcoffee", "potion.darkcafeaulait");
            sweeten(rows, sugar, "potion.darkchocolatemilk", "chocolatemilk");
            sweetenNative(rows, sugar, "Juice_Lemon", "Lemonade");
            sweetenNative(rows, sugar, "Juice_Tomato", "Ketchup");
            sweetenNative(rows, sugar, "Tea", "Tea_Sweet");
        }
    }

    private static void sweeten(List<ChemRecipe> rows, Form sugar, String from, String to) {
        row(SOURCE + "135-162/" + from, "Mixer", 16, 48)
                .item(form(sugar.prefix, "Sugar", sugar.count))
                .fluid(registryFluid(from, 750)).out(registryFluid(to, 750)).add(rows);
    }

    private static void sweetenNative(List<ChemRecipe> rows, Form sugar, String from, String to) {
        row(SOURCE + "135-162/" + from, "Mixer", 16, 48)
                .item(form(sugar.prefix, "Sugar", sugar.count))
                .fluid(nativeFluid(from, 750)).out(nativeFluid(to, 750)).add(rows);
    }

    private static void chiliRows(List<ChemRecipe> rows) {
        for (Form chili : new Form[] {new Form("dust", 1, 16), new Form("dustSmall", 4, 16),
                new Form("dustTiny", 9, 16)}) {
            row(SOURCE + "163-168/chips/" + chili.prefix, "Mixer", 16, 16)
                    .item(form(chili.prefix, "Chili", chili.count)).item(tech("food_potatochips", 1))
                    .out(tech("food_chilichips", 1)).add(rows);
            row(SOURCE + "163-168/sauce/" + chili.prefix, "Mixer", 16, 48)
                    .item(form(chili.prefix, "Chili", chili.count))
                    .fluid(nativeFluid("Sauce_Chili", 750))
                    .out(registryFluid("potion.hotsauce", 750)).add(rows);
        }
    }

    private static void smoothieRows(List<ChemRecipe> rows) {
        for (String juice : SMOOTHIE_JUICES) {
            // The plain juice and the green apple cultivar share their source smoothie names.
            String output = juice.equals("Juice") ? "Smoothie_Fruit"
                    : juice.equals("Juice_AppleGrC") ? "Smoothie_Apple"
                    : "Smoothie" + juice.substring("Juice".length());
            row(SOURCE + "172-195/" + juice, "Mixer", 16, 16)
                    .fluid(nativeFluid("Ice", 125)).fluid(nativeFluid(juice, 125))
                    .out(nativeFluid(output, 250)).add(rows);
        }
    }

    private static void sourceRows(List<ChemRecipe> rows) {
        row(SOURCE, "Mixer", 16, 16, 29).fluid(nativeFluid("RoyalJelly", 100)).fluid(nativeFluid("Honeydew", 200)).out(nativeFluid("Ambrosia", 400)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 30).item(dust("Milk", thousandths(1000))).fluid(nativeFluid("Soda", 500)).out(nativeFluid("Milk_Spoiled", 1000)).add(rows);
        row(SOURCE, "Distillery", 16, 16, 31).kept(selector(0)).fluid(nativeFluid("RoyalJelly", 10)).out(dust("Sugar", thousandths(1000))).out(nativeFluid("DistW", 1)).add(rows);
        row(SOURCE, "Distillery", 16, 16, 32).kept(selector(0)).fluid(nativeFluid("Honey", 100)).out(dust("Sugar", thousandths(500))).out(nativeFluid("DistW", 10)).add(rows);
        row(SOURCE, "Distillery", 16, 16, 33).kept(selector(0)).fluid(nativeFluid("Honeydew", 100)).out(dust("Sugar", thousandths(1000))).out(nativeFluid("DistW", 10)).add(rows);
        row(SOURCE, "Distillery", 16, 80, 34).kept(selector(0)).fluid(nativeFluid("Juice_Reed", 100)).out(dust("Sugar", thousandths(1000))).out(nativeFluid("DistW", 50)).add(rows);
        row(SOURCE, "Distillery", 16, 80, 35).kept(selector(0)).fluid(nativeFluid("Juice_Beet", 100)).out(dust("Sugar", thousandths(1000))).out(nativeFluid("DistW", 50)).add(rows);
        row(SOURCE, "Distillery", 16, 80, 36).kept(selector(0)).fluid(nativeFluid("Juice_Cactus", 100)).out(nativeFluid("DistW", 50)).add(rows);
        row(SOURCE, "Distillery", 16, 80, 37).kept(selector(0)).fluid(nativeFluid("Sap_Maple", 125)).out(nativeFluid("Syrup_Maple", 50)).out(nativeFluid("DistW", 50)).add(rows);
        row(SOURCE, "Distillery", 16, 16, 38).kept(selector(0)).fluid(nativeFluid("Juice_Lemon", 4)).out(nativeFluid("Alcopops", 2)).add(rows);
        row(SOURCE, "Distillery", 16, 16, 39).kept(selector(0)).fluid(nativeFluid("Juice_Potato", 4)).out(nativeFluid("Vodka", 2)).add(rows);
        row(SOURCE, "Distillery", 16, 16, 40).kept(selector(0)).fluid(nativeFluid("Rum_White", 2)).out(nativeFluid("Rum_Dark", 1)).add(rows);
        row(SOURCE, "Distillery", 16, 16, 41).kept(selector(0)).fluid(nativeFluid("ShortMead", 2)).out(nativeFluid("Mead", 1)).add(rows);
        row(SOURCE, "Distillery", 16, 16, 42).kept(selector(0)).fluid(nativeFluid("Vodka", 2)).out(nativeFluid("Vodka", 1)).add(rows);
        row(SOURCE, "Distillery", 16, 16, 43).kept(selector(0)).fluid(nativeFluid("Rum_Dark", 2)).out(nativeFluid("Rum_Dark", 1)).add(rows);
        row(SOURCE, "Distillery", 16, 80, 44).kept(selector(0)).fluid(nativeFluid("Sap", 125)).out(dust("Sugar", thousandths(1000))).out(nativeFluid("DistW", 50)).add(rows);
        row(SOURCE, "Distillery", 16, 128, 45).item(dust("Chili", thousandths(1000))).fluid(nativeFluid("Sauce_Hot", 750)).out(nativeFluid("Sauce_Diabolo", 750)).add(rows);
        row(SOURCE, "Distillery", 64, 128, 46).item(dust("Chili", thousandths(1000))).fluid(nativeFluid("Sauce_Diabolo", 750)).out(nativeFluid("Sauce_Diablo", 750)).add(rows);
        row(SOURCE, "Distillery", 64, 128, 47).item(dust("Gunpowder", thousandths(1000))).fluid(nativeFluid("Sauce_Diablo", 250)).out(nativeFluid("Sauce_Cow_Level", 250)).add(rows);
        row(SOURCE, "Distillery", 64, 128, 48).item(dust("Gunpowder", thousandths(1000))).fluid(nativeFluid("Lemonade", 250)).out(nativeFluid("Grenade_Juice", 250)).add(rows);
        row(SOURCE, "Injector", 16, 16, 58).item(dust("Mg", thousandths(1000))).fluid(water(waterUnits(1000), FLOOR)).out(nativeFluid("MnWtr", 1000)).add(rows);
        row(SOURCE, "Injector", 16, 16, 59).item(dust("Ca", thousandths(1000))).fluid(water(waterUnits(1000), FLOOR)).out(nativeFluid("MnWtr", 1000)).add(rows);
        row(SOURCE, "Injector", 16, 16, 60).fluid(water(waterUnits(250), FLOOR)).fluid(gas("CO2", thousandths(250), CEIL)).out(nativeFluid("Soda", 250)).add(rows);
        row(SOURCE, "Injector", 16, 16, 61).fluid(nativeFluid("MnWtr", 250)).fluid(gas("CO2", thousandths(250), CEIL)).out(nativeFluid("Mineralsoda", 250)).add(rows);
        row(SOURCE, "Fermenter", 16, 64, 62).kept(selector(0)).fluid(nativeFluid("Milk", 50)).out(nativeFluid("Milk_Spoiled", 50)).add(rows);
        row(SOURCE, "Fermenter", 16, 64, 63).kept(selector(0)).fluid(nativeFluid("Honeydew", 50)).out(nativeFluid("ShortMead", 50)).add(rows);
        row(SOURCE, "Fermenter", 16, 64, 64).kept(selector(0)).fluid(nativeFluid("Juice_Ananas", 50)).out(nativeFluid("Cider_Ananas", 25)).add(rows);
        row(SOURCE, "Fermenter", 16, 64, 65).kept(selector(0)).fluid(nativeFluid("Juice_Apple", 50)).out(nativeFluid("Cider_Apple", 25)).add(rows);
        row(SOURCE, "Fermenter", 16, 64, 66).kept(selector(0)).fluid(nativeFluid("Juice_AppleGrC", 50)).out(nativeFluid("Cider_Apple", 25)).add(rows);
        row(SOURCE, "Fermenter", 16, 128, 67).kept(selector(0)).fluid(nativeFluid("Cider_Apple", 50)).out(nativeFluid("Vinegar_Apple", 10)).add(rows);
        row(SOURCE, "Fermenter", 16, 64, 68).kept(selector(0)).fluid(nativeFluid("Mash_Hops", 50)).out(nativeFluid("Beer_Dark", 25)).add(rows);
        row(SOURCE, "Fermenter", 16, 128, 69).kept(selector(0)).fluid(nativeFluid("Beer_Dark", 50)).out(nativeFluid("Beer_Dragonblood", 10)).add(rows);
        row(SOURCE, "Fermenter", 16, 64, 70).kept(selector(0)).fluid(nativeFluid("Juice_Reed", 50)).out(nativeFluid("Rum_White", 25)).add(rows);
        row(SOURCE, "Fermenter", 16, 128, 71).kept(selector(0)).fluid(nativeFluid("Rum_White", 50)).out(nativeFluid("Vinegar_Cane", 10)).add(rows);
        row(SOURCE, "Fermenter", 16, 128, 72).kept(selector(0)).fluid(nativeFluid("Rum_Dark", 50)).out(nativeFluid("Vinegar_Cane", 10)).add(rows);
        row(SOURCE, "Fermenter", 16, 128, 73).kept(selector(0)).fluid(nativeFluid("Wine_Grape_Green", 50)).out(nativeFluid("Vinegar_Grape", 10)).add(rows);
        row(SOURCE, "Fermenter", 16, 128, 74).kept(selector(0)).fluid(nativeFluid("Wine_Grape_Purple", 50)).out(nativeFluid("Vinegar_Grape", 10)).add(rows);
        row(SOURCE, "Fermenter", 16, 64, 75).kept(selector(0)).fluid(nativeFluid("Juice_Grape_Green", 50)).out(nativeFluid("Wine_Grape_Green", 25)).add(rows);
        row(SOURCE, "Fermenter", 16, 64, 76).kept(selector(0)).fluid(nativeFluid("Juice_Grape_Purple", 50)).out(nativeFluid("Wine_Grape_Purple", 25)).add(rows);
        row(SOURCE, "Fermenter", 16, 64, 77).kept(selector(0)).fluid(nativeFluid("Juice_Lemon", 50)).out(nativeFluid("Wine_Lemon", 25)).add(rows);
        row(SOURCE, "Drying", 16, 200, 78).fluid(nativeFluid("Sap", 250)).out(dust("Sugar", thousandths(1000))).out(nativeFluid("DistW", 100)).add(rows);
        row(SOURCE, "Drying", 16, 200, 79).fluid(nativeFluid("Sap_Maple", 250)).out(dust("Sugar", thousandths(1000))).out(nativeFluid("DistW", 100)).add(rows);
        row(SOURCE, "Drying", 16, 100, 80).fluid(nativeFluid("Juice_Reed", 200)).out(dust("Sugar", thousandths(1000))).out(nativeFluid("DistW", 50)).add(rows);
        row(SOURCE, "Drying", 16, 100, 81).fluid(nativeFluid("Juice_Cactus", 200)).out(nativeFluid("DistW", 50)).add(rows);
        row(SOURCE, "Centrifuge", 16, 16, 82).fluid(nativeFluid("Milk", 50)).out(nativeFluid("Cream", 50)).add(rows);
        row(SOURCE, "Centrifuge", 16, 16, 83).fluid(nativeFluid("MilkSoy", 50)).out(nativeFluid("Cream", 50)).add(rows);
        row(SOURCE, "Centrifuge", 16, 16, 84).fluid(nativeFluid("Juice_Coconut", 50)).out(nativeFluid("Cream_Coconut", 50)).add(rows);
        row(SOURCE, "Centrifuge", 16, 64, 85).fluid(nativeFluid("Cream", 250)).out(tech("food_butter", 1)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 86).kept(form("stick", "WoodTreated", 1)).fluid(nativeFluid("MilkSoy", 50)).out(nativeFluid("Cream", 50)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 87).kept(form("stick", "WoodTreated", 1)).fluid(nativeFluid("Milk", 50)).out(nativeFluid("Cream", 50)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 88).kept(form("stick", "WoodTreated", 1)).fluid(nativeFluid("Juice_Coconut", 50)).out(nativeFluid("Cream_Coconut", 50)).add(rows);
        row(SOURCE, "Mixer", 16, 64, 89).kept(form("stick", "WoodTreated", 1)).fluid(nativeFluid("Cream", 250)).out(tech("food_butter", 1)).add(rows);
        row(SOURCE, "Coagulator", 0, 256, 90).fluid(nativeFluid("Slime_Green", 250)).out(vanilla("slime_ball", 1)).add(rows);
        row(SOURCE, "Coagulator", 0, 1024, 91).fluid(nativeFluid("Milk", 1000)).out(tech("food_cheese", 1)).add(rows);
        row(SOURCE, "Melter", 16, 1, 92).fluid(nativeFluid("Oil_Sunflower", 1)).out(nativeFluid("Oil_Frying", 1)).add(rows);
        row(SOURCE, "Melter", 16, 1, 93).fluid(nativeFluid("Oil_Olive", 1)).out(nativeFluid("Oil_Frying", 1)).add(rows);
        row(SOURCE, "Melter", 16, 1, 94).fluid(nativeFluid("Oil_Nut", 1)).out(nativeFluid("Oil_Frying", 1)).add(rows);
        row(SOURCE, "Melter", 16, 1, 95).fluid(nativeFluid("Oil_Hemp", 1)).out(nativeFluid("Oil_Frying", 1)).add(rows);
        row(SOURCE, "Melter", 16, 1, 96).fluid(nativeFluid("Oil_Lin", 1)).out(nativeFluid("Oil_Frying", 1)).add(rows);
        row(SOURCE, "Melter", 16, 1, 97).fluid(nativeFluid("Oil_Seed", 1)).out(nativeFluid("Oil_Frying", 1)).add(rows);
        row(SOURCE, "Melter", 16, 1, 98).fluid(nativeFluid("Oil_Fish", 1)).out(nativeFluid("Oil_Frying", 1)).add(rows);
        row(SOURCE, "Melter", 16, 1, 99).fluid(nativeFluid("Ice", 1)).out(water(waterUnits(1), FLOOR)).add(rows);
        row(SOURCE, "Smelter", 16, 1, 100).fluid(nativeFluid("Oil_Sunflower", 1)).out(nativeFluid("Oil_Frying", 1)).add(rows);
        row(SOURCE, "Smelter", 16, 1, 101).fluid(nativeFluid("Oil_Olive", 1)).out(nativeFluid("Oil_Frying", 1)).add(rows);
        row(SOURCE, "Smelter", 16, 1, 102).fluid(nativeFluid("Oil_Nut", 1)).out(nativeFluid("Oil_Frying", 1)).add(rows);
        row(SOURCE, "Smelter", 16, 1, 103).fluid(nativeFluid("Oil_Hemp", 1)).out(nativeFluid("Oil_Frying", 1)).add(rows);
        row(SOURCE, "Smelter", 16, 1, 104).fluid(nativeFluid("Oil_Lin", 1)).out(nativeFluid("Oil_Frying", 1)).add(rows);
        row(SOURCE, "Smelter", 16, 1, 105).fluid(nativeFluid("Oil_Seed", 1)).out(nativeFluid("Oil_Frying", 1)).add(rows);
        row(SOURCE, "Smelter", 16, 1, 106).fluid(nativeFluid("Oil_Fish", 1)).out(nativeFluid("Oil_Frying", 1)).add(rows);
        row(SOURCE, "Smelter", 16, 1, 107).fluid(nativeFluid("Ice", 1)).out(water(waterUnits(1), FLOOR)).add(rows);
        row(SOURCE, "Mixer", 16, 144, 108).item(form("dust", "NaCl", 1)).item(tech("food_butter", 9)).out(tech("food_butter_salted", 9)).add(rows);
        row(SOURCE, "Mixer", 16, 32, 109).item(form("dustSmall", "NaCl", 1)).item(tech("food_butter", 2)).out(tech("food_butter_salted", 2)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 110).item(form("dustTiny", "NaCl", 1)).item(tech("food_butter", 1)).out(tech("food_butter_salted", 1)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 111).item(dust("Sugar", thousandths(1000))).item(dust("Cocoa", thousandths(1000))).out(dust("Chocolate", thousandths(2000))).add(rows);
        row(SOURCE, "Mixer", 16, 16, 112).item(form("gemChipped", "Sugar", 4)).item(dust("Cocoa", thousandths(1000))).out(dust("Chocolate", thousandths(2000))).add(rows);
        row(SOURCE, "Mixer", 16, 16, 113).item(dust("Sugar", thousandths(250))).item(dust("Cocoa", thousandths(250))).out(dust("Chocolate", thousandths(500))).add(rows);
        row(SOURCE, "Mixer", 16, 16, 114).item(form("gemChipped", "Sugar", 1)).item(dust("Cocoa", thousandths(250))).out(dust("Chocolate", thousandths(500))).add(rows);
        row(SOURCE, "Mixer", 16, 16, 115).item(dust("Sugar", U(1, 9))).item(dust("Cocoa", U(1, 9))).out(dust("Chocolate", U(2, 9))).add(rows);
        row(SOURCE, "Mixer", 16, 16, 116).fluid(nativeFluid("Lemonade", 125)).fluid(nativeFluid("Vodka", 125)).out(nativeFluid("Leninade", 250)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 117).fluid(nativeFluid("Juice_Ananas", 150)).fluid(nativeFluid("Cream_Coconut", 50)).fluid(nativeFluid("Rum_White", 50)).out(nativeFluid("Pina_Colada", 250)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 118).fluid(registryFluid("potion.darkcoffee", 125)).fluid(nativeFluid("Milk", 125)).out(registryFluid("potion.coffee", 250)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 119).fluid(registryFluid("potion.darkcoffee", 125)).fluid(nativeFluid("MilkSoy", 125)).out(registryFluid("potion.coffee", 250)).add(rows);
        row(SOURCE, "Mixer", 16, 48, 120).item(form("dust", "Milk", 1)).fluid(registryFluid("potion.darkcoffee", 750)).out(registryFluid("potion.coffee", 750)).add(rows);
        row(SOURCE, "Mixer", 16, 48, 121).item(form("dustSmall", "Milk", 4)).fluid(registryFluid("potion.darkcoffee", 750)).out(registryFluid("potion.coffee", 750)).add(rows);
        row(SOURCE, "Mixer", 16, 48, 122).item(form("dustTiny", "Milk", 9)).fluid(registryFluid("potion.darkcoffee", 750)).out(registryFluid("potion.coffee", 750)).add(rows);
        row(SOURCE, "Mixer", 16, 64, 123).item(form("dust", "Hazelnut", 1)).fluid(nativeFluid("Cream_Chocolate", 250)).out(nativeFluid("Cream_Nutella", 250)).add(rows);
        row(SOURCE, "Mixer", 16, 64, 124).item(form("dustSmall", "Hazelnut", 4)).fluid(nativeFluid("Cream_Chocolate", 250)).out(nativeFluid("Cream_Nutella", 250)).add(rows);
        row(SOURCE, "Mixer", 16, 64, 125).item(form("dustTiny", "Hazelnut", 9)).fluid(nativeFluid("Cream_Chocolate", 250)).out(nativeFluid("Cream_Nutella", 250)).add(rows);
        row(SOURCE, "Mixer", 16, 48, 126).item(form("dust", "Chocolate", 1)).fluid(nativeFluid("Milk", 750)).out(registryFluid("potion.darkchocolatemilk", 750)).add(rows);
        row(SOURCE, "Mixer", 16, 48, 127).item(form("dust", "Chocolate", 1)).fluid(nativeFluid("MilkSoy", 750)).out(registryFluid("potion.darkchocolatemilk", 750)).add(rows);
        row(SOURCE, "Mixer", 16, 64, 128).item(form("dust", "Chocolate", 1)).fluid(nativeFluid("Cream", 250)).out(nativeFluid("Cream_Chocolate", 250)).add(rows);
        row(SOURCE, "Mixer", 16, 48, 129).item(form("dustSmall", "Chocolate", 4)).fluid(nativeFluid("Milk", 750)).out(registryFluid("potion.darkchocolatemilk", 750)).add(rows);
        row(SOURCE, "Mixer", 16, 48, 130).item(form("dustSmall", "Chocolate", 4)).fluid(nativeFluid("MilkSoy", 750)).out(registryFluid("potion.darkchocolatemilk", 750)).add(rows);
        row(SOURCE, "Mixer", 16, 64, 131).item(form("dustSmall", "Chocolate", 4)).fluid(nativeFluid("Cream", 250)).out(nativeFluid("Cream_Chocolate", 250)).add(rows);
        row(SOURCE, "Mixer", 16, 48, 132).item(form("dustTiny", "Chocolate", 9)).fluid(nativeFluid("Milk", 750)).out(registryFluid("potion.darkchocolatemilk", 750)).add(rows);
        row(SOURCE, "Mixer", 16, 48, 133).item(form("dustTiny", "Chocolate", 9)).fluid(nativeFluid("MilkSoy", 750)).out(registryFluid("potion.darkchocolatemilk", 750)).add(rows);
        row(SOURCE, "Mixer", 16, 64, 134).item(form("dustTiny", "Chocolate", 9)).fluid(nativeFluid("Cream", 250)).out(nativeFluid("Cream_Chocolate", 250)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 169).fluid(nativeFluid("Juice_Grape_Green", 125)).fluid(nativeFluid("Juice_Apple", 125)).out(nativeFluid("Med_Laxative", 250)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 170).fluid(nativeFluid("Juice_Grape_Purple", 125)).fluid(nativeFluid("Juice_Apple", 125)).out(nativeFluid("Med_Laxative", 250)).add(rows);
        row(SOURCE, "Mixer", 16, 16, 171).fluid(nativeFluid("Sap_Rainbow", 125)).fluid(nativeFluid("Ambrosia", 125)).out(nativeFluid("Med_Heal", 250)).add(rows);    }
}
