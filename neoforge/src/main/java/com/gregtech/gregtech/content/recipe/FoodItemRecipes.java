package com.gregtech.gregtech.content.recipe;


import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.content.food.GTFoodItems;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6's food recipes for the port's own food items ({@code gregtech/items/MultiItemFood.java} and the
 * {@code Loader_Recipes_Food} listener bodies).
 *
 * <p>Only rows that need nothing the port does not already have are transcribed; everything else is
 * recorded in {@link #skipped()} with its GT6 line, never approximated. The four families here are the
 * ones the original puts <em>between</em> its food items, which is what a food line is made of:</p>
 * <ol>
 *   <li><b>Smelting</b> — the raw → cooked chain ({@code RM.add_smelting}): hams, bacon, ribs, dogmeat,
 *       mutton, horse, doughs into their baked forms, the four pizzas, all three cookie doughs, buns,
 *       bread, baguette and toast. GT6 passes {@code (removeOthers, smoker, blast)}; the port's
 *       {@code MachineRecipeMaps.add_smelting} has the same five-argument shape.</li>
 *   <li><b>Slicer</b> — {@code RM.Slicer.addRecipe2(..., IL.Shape_Slicer_*, ...)}: a slicer blade turns
 *       dough into cookie shapes, a loaf into slices, cheese into cheese slices and a boiled egg into
 *       egg slices.</li>
 *   <li><b>Mixer</b> — the dough flavourings, the scrambled egg, and every ice cream that GT6 flavours
 *       with a fruit juice or honey.</li>
 *   <li><b>Boxinator / Unboxinator</b> — the packaged food (bags of chips, fries in paper) and GT6's
 *       three pills.</li>
 * </ol>
 *
 * <p>Recorded rather than done, with reasons: every crafting-grid row (GT6's {@code CR.shaped}/
 * {@code CR.shapeless} rows live in this port's datapack JSON, which this batch may not write), every
 * row whose fluid volume GT6 writes in <em>material units</em> ({@code U}, {@code U4}, {@code L} - the
 * unit-to-mB conversion belongs to the material system, not the food line), the
 * {@code RM.replicateOrganic} / {@code RM.generify} / {@code RM.food_can} generic mechanics, and the
 * crops {@code Loader_Recipes_Crops} drives (already ported in {@link CropProcessingRecipes}).
 */
public final class FoodItemRecipes {
    private static final List<String> SKIPPED = new ArrayList<>();
    private static int registered;

    private FoodItemRecipes() {}

    /** Rows of this class that could not be expressed, with the reason and the GT6 line. */
    public static List<String> skipped() {
        return List.copyOf(SKIPPED);
    }

    /** How many food recipes the last {@link #register()} call added. */
    public static int registered() {
        return registered;
    }

    /** Registers the food recipes and returns how many rows were added. */
    public static int register() {
        SKIPPED.clear();
        registered = 0;
        smelting();
        slicing();
        mixing();
        packaging();
        registered += CannedFoodRecipes.register();
        com.mojang.logging.LogUtils.getLogger().info("Registered {} GT6 food item recipes ({} rows recorded as not expressible: {})",
                registered, SKIPPED.size(), SKIPPED);
        return registered;
    }

    // ── 1. Smelting (GT6 MultiItemFood.java RM.add_smelting rows) ─────────────────────────────────

    /**
     * {@code input -> output} pairs with the GT6 line they come from. GT6 passes
     * {@code (aRemoveOthers, aSmoker, aBlast)} = {@code (F, T, F)} for every one of these: they are
     * smoker recipes, and they never remove another recipe for the same input.
     */
    private static final String[][] SMELTING = {
            {"potato_on_a_stick", "roasted_potato_on_a_stick", "348"},
            {"potato_chips_raw", "potato_chips", "367"},
            {"raw_ham", "cooked_ham", "531"},
            {"raw_ham_slice", "cooked_ham_slice", "536"},
            {"raw_bacon", "grilled_bacon", "543"},
            {"raw_ribs", "grilled_ribs", "549"},
            {"raw_rib_eye_steak", "grilled_rib_eye_steak", "555"},
            {"dogmeat", "grilled_dogmeat", "560"},
            {"mutton", "grilled_mutton", "565"},
            {"horse_meat", "grilled_horse_meat", "570"},
            {"donkey_meat", "grilled_donkey_meat", "578"},
            {"cookie_shaped_dough", "minecraft:cookie", "599"},
            {"cookie_shaped_raisin_dough", "raisin_cookie", "607"},
            {"cookie_shaped_chocolate_raisin_dough", "cookie", "615"},
            {"cookie_shaped_abyssal_dough", "minecraft:cookie", "626"},
            {"raw_cake_bottom", "cake_bottom", "634"},
            {"raw_pizza_margherita", "pizza_margherita", "653"},
            {"raw_mince_meat_pizza", "mince_meat_pizza", "659"},
            {"raw_veggie_pizza", "veggie_pizza", "665"},
            {"raw_pizza_hawaii", "pizza_hawaii", "671"},
            {"dough_bun", "bun", "679"},
            {"dough_bread", "minecraft:bread", "718"},
            {"dough_baguette", "baguette", "748"},
            {"dough_toast_loaf", "loaf_of_toast", "778"},
            {"toast", "toasted_toast", "779"},
    };

    private static void smelting() {
        for (String[] row : SMELTING) {
            ItemStack input = food(row[0], 1);
            ItemStack output = food(row[1], 1);
            if (input.isEmpty() || output.isEmpty()) {
                continue;
            }
            // GT6 MultiItemFood.java:<line>; (removeOthers, smoker, blast) = (F, T, F).
            if (MachineRecipeMaps.add_smelting(input, output, false, true, false)) {
                registered++;
            }
        }
        // GT6 MultiItemFood.java:574 calls add_smelting(Food_DogMeat_Raw, Food_DogMeat_Cooked) a second
        // time instead of the Mule pair of :572-573, so GT6 ships *no* smelting row for Mule Meat. The
        // port reproduces the original's behaviour and records the copy-paste bug instead of fixing it
        // silently: a "corrected" row would make the port differ from the game it is porting.
        SKIPPED.add("mule_meat -> grilled_mule_meat (GT6 MultiItemFood.java:574 repeats the Dogmeat pair of "
                + ":560, so the Mule pair declared at :572-573 never gets a smelting row; reproduced as-is)");
    }

    // ── 2. Slicer (GT6 RM.Slicer.addRecipe2 rows) ────────────────────────────────────────────────

    /** {@code input, slicer blade id, output id, output count, GT6 source}. */
    private static final String[][] SLICER = {
            {"chocolate_dough", "slicer_shape_flat", "cookie_shaped_dough", "4", "MultiItemFood.java:602"},
            {"sugary_raisin_dough", "slicer_shape_flat", "cookie_shaped_raisin_dough", "4", "MultiItemFood.java:610"},
            {"sugary_chocolate_raisin_dough", "slicer_shape_flat", "cookie_shaped_chocolate_raisin_dough", "4", "MultiItemFood.java:618"},
            {"abyssal_dough", "slicer_shape_flat", "cookie_shaped_abyssal_dough", "4", "MultiItemFood.java:629"},
            {"bun", "slicer_shape_split", "sliced_bun", "2", "MultiItemFood.java:688"},
            // GT6's IL.Food_Bread is ST.make(Items.bread, 1, 0) - a vanilla alias (MultiItemFood.java:715),
            // so this row has no GT item and uses the "minecraft:" input form below.
            {"minecraft:bread", "slicer_shape_split", "sliced_bread", "2", "MultiItemFood.java:727"},
            {"baguette", "slicer_shape_split", "sliced_baguette", "2", "MultiItemFood.java:757"},
            {"loaf_of_toast", "slicer_shape_flat", "toast", "8", "MultiItemFood.java:785"},
            {"cheese", "slicer_shape_flat", "cheese_slice", "4", "Loader_Recipes_Food.java:130"},
            {"boiled_egg", "slicer_shape_flat", "sliced_egg", "4", "Loader_Recipes_Food.java:297"},
    };

    private static void slicing() {
        for (String[] row : SLICER) {
            // A "namespace:path" input is a vanilla-aliased GT6 row (IL.Food_Bread is Items.bread), so it
            // resolves through the item registry instead of the GT food table.
            ItemStack input = row[0].contains(":") ? item(row[0], 1) : food(row[0], 1);
            ItemStack blade = food(row[1], 1);
            ItemStack output = food(row[2], Integer.parseInt(row[3]));
            if (input.isEmpty() || blade.isEmpty() || output.isEmpty()) {
                SKIPPED.add(row[0] + " + " + row[1] + " -> " + row[2] + " (" + row[4]
                        + "): an item of this row is not in the port");
                continue;
            }
            // GT6 RM.Slicer.addRecipe2(T, 16, 16, input, blade, output).
            if (MachineRecipeMaps.Slicer.addRecipe2(false, 16, 16, input, blade, output) != null) {
                registered++;
            }
        }
    }

    // ── 3. Mixer (dough flavourings, scrambled egg, ice cream flavours) ──────────────────────────

    /** {@code input, second ingredient id, output id, output count, GT6 source}. */
    private static final String[][] MIXER_ITEMS = {
            {"chocolate_dough", "gregtech:gem_chipped_sugar", "cookie_shaped_dough", "4", "Loader_Recipes_Food.java:140"},
            {"abyssal_dough", "gregtech:dust_chocolate", "cookie_shaped_abyssal_dough", "4", "Loader_Recipes_Food.java:142"},
    };

    /** {@code input, fluid field, mB, output id, GT6 source}; {@code U} is not a mB volume, see below. */
    private static final String[][] MIXER_FLUIDS = {
            {"ice_cream", "Juice_Lemon", "50", "lemon_ice_cream", "MultiItemFood.java:852"},
            {"ice_cream", "Juice_Banana", "50", "banana_ice_cream", "MultiItemFood.java:853"},
            {"ice_cream", "Juice_Grape_Red", "50", "grape_ice_cream", "MultiItemFood.java:854"},
            {"ice_cream", "Juice_Grape_White", "50", "grape_ice_cream", "MultiItemFood.java:855"},
            {"ice_cream", "Juice_Grape_Green", "50", "grape_ice_cream", "MultiItemFood.java:856"},
            {"ice_cream", "Juice_Grape_Purple", "50", "grape_ice_cream", "MultiItemFood.java:857"},
            {"ice_cream", "Juice_Apple", "50", "apple_ice_cream", "MultiItemFood.java:858"},
            {"ice_cream", "Juice_Ananas", "50", "ananas_ice_cream", "MultiItemFood.java:860"},
            {"ice_cream", "Juice_Cherry", "50", "cherry_ice_cream", "MultiItemFood.java:861"},
            {"ice_cream", "Juice_Cranberry", "50", "cranberry_ice_cream", "MultiItemFood.java:862"},
            {"ice_cream", "Juice_Strawberry", "50", "strawberry_ice_cream", "MultiItemFood.java:863"},
            {"ice_cream", "Juice_Kiwi", "50", "kiwi_ice_cream", "MultiItemFood.java:864"},
            {"ice_cream", "Juice_Melon", "50", "melon_ice_cream", "MultiItemFood.java:865"},
            {"ice_cream", "Juice_Currant", "50", "currant_ice_cream", "MultiItemFood.java:866"},
            {"ice_cream", "Juice_Raspberry", "50", "raspberry_ice_cream", "MultiItemFood.java:867"},
            {"ice_cream", "Juice_Blackberry", "50", "blackberry_ice_cream", "MultiItemFood.java:868"},
            {"ice_cream", "Juice_Blueberry", "50", "blueberry_ice_cream", "MultiItemFood.java:869"},
            {"ice_cream", "Juice_Gooseberry", "50", "gooseberry_ice_cream", "MultiItemFood.java:870"},
            {"ice_cream", "RoyalJelly", "5", "honey_ice_cream", "MultiItemFood.java:871"},
            {"ice_cream", "Honey", "50", "honey_ice_cream", "MultiItemFood.java:872"},
            {"ice_cream", "Syrup_Maple", "50", "maple_ice_cream", "MultiItemFood.java:876"},
            {"ice_cream", "Cream_Nutella", "50", "nutella_ice_cream", "MultiItemFood.java:877"},
            {"ice_cream", "Nutbutter_Peanut", "50", "peanut_butter_ice_cream", "MultiItemFood.java:878"},
            {"ice_cream", "Sap_Rainbow", "50", "rainbow_ice_cream", "MultiItemFood.java:879"},
    };

    private static void mixing() {
        // Loader_Recipes_Food.java:137-150 — the dough flavourings.
        for (String[] row : MIXER_ITEMS) {
            ItemStack input = food(row[0], 1);
            ItemStack ingredient = item(row[1], 1);
            ItemStack output = food(row[2], Integer.parseInt(row[3]));
            if (input.isEmpty() || ingredient.isEmpty() || output.isEmpty()) {
                continue;
            }
            if (MachineRecipeMaps.Mixer.addRecipe2(false, 16, 16, input, ingredient, output) != null) {
                registered++;
            }
        }
        // Loader_Recipes_Food.java:141,143 — dough plus a full dust; the same dust stack the GT6 line
        // names with OM.dust(MT.Sugar) / OM.dust(MT.Chocolate) (one dust, no unit arithmetic).
        mixerDust("dough", "Sugar", "sugary_dough", 2, "Loader_Recipes_Food.java:141");
        mixerDust("dough", "Cocoa", "chocolate_dough", 1, "Loader_Recipes_Food.java:142");
        mixerDust("dough", "Chocolate", "chocolate_dough", 2, "Loader_Recipes_Food.java:143");
        // Loader_Recipes_Food.java:255,259 — sugared dough plus raisins.
        mixerPair("sugary_dough", "green_raisins", "sugary_raisin_dough", 1, "Loader_Recipes_Food.java:255");
        mixerPair("sugary_dough", "chocolate_raisins", "sugary_chocolate_raisin_dough", 1, "Loader_Recipes_Food.java:259");
        // MultiItemFood.java:506 — the two egg halves mix back into a scrambled egg.
        mixerPair("egg_white", "egg_yolk", "scrambled_egg", 1, "MultiItemFood.java:506");
        // MultiItemFood.java:509-511 — egg yolk plus a cooking oil and a vinegar makes 250 mB mayonnaise.
        for (String oil : fluidFields(com.gregtech.gregtech.data.RegisteredFluids.FluidFlags.COOKING_OIL)) {
            for (String vinegar : fluidFields(com.gregtech.gregtech.data.RegisteredFluids.FluidFlags.VINEGAR)) {
                mayonnaise(oil, vinegar, "MultiItemFood.java:509");
            }
        }
        // MultiItemFood.java:510-511 use lemon / lime juice directly instead of a vinegar.
        mayonnaise("Oil_Seed", "Juice_Lemon", "MultiItemFood.java:510");
        mayonnaise("Oil_Seed", "Juice_Lime", "MultiItemFood.java:511");
        // MultiItemFood.java:852-879 — GT6's flavoured ice creams.
        for (String[] row : MIXER_FLUIDS) {
            ItemStack input = food(row[0], 1);
            ItemStack output = food(row[3], 1);
            FluidStack fluid = fluid(row[1], Integer.parseInt(row[2]));
            if (input.isEmpty() || output.isEmpty() || fluid.isEmpty()) {
                continue;
            }
            if (MachineRecipeMaps.Mixer.addRecipe1(false, 16, 16, input, fluid, FluidStack.EMPTY, output) != null) {
                registered++;
            }
        }
        // MultiItemFood.java:875,881-900 — the ice creams GT6 flavours with a material fluid or dust.
        SKIPPED.add("ice cream + MT.Chocolate.liquid(U4) -> chocolate ice cream (MultiItemFood.java:875): "
                + "GT6 writes the volume in material units (U4 = U/4), which is the material system's "
                + "conversion, not the food line's");
        SKIPPED.add("ice cream + OM.dust(MT.Chocolate/Vanilla/Coffee/Mint/Pistachio/MeatCooked, U4|U) -> "
                + "stracciatella/vanilla/mocha/mint/pistachio/bear ice cream (MultiItemFood.java:881-892): "
                + "the same material-unit amounts, in dust form");
        SKIPPED.add("ice cream mixes (Neapolitan, Spumoni, Superman) (MultiItemFood.java:897-900): "
                + "RM.Mixer.addRecipeX with three inputs; the rows themselves are expressible but the "
                + "port's Mixer map holds 6 item inputs, so they belong to a mixer batch that can test "
                + "the 3-input shape");
        SKIPPED.add("mayonnaise from MT.X.liquid(U/100) oils (MultiItemFood.java:509): the oil volume is "
                + "in material units");
    }

    private static void mixerDust(String inputId, String material, String outputId, int count, String source) {
        ItemStack input = food(inputId, 1);
        MaterialPrefix dust = PrefixRegistry.byName("dust");
        GTMaterial mat = GTMaterialRegistry.get(material);
        ItemStack ingredient = dust == null || mat == null ? ItemStack.EMPTY : GTItems.getStack(dust, mat, 1);
        ItemStack output = food(outputId, count);
        if (input.isEmpty() || ingredient.isEmpty() || output.isEmpty()) {
            SKIPPED.add("dough + dust(" + material + ") -> " + outputId + " (" + source + "): the port has no "
                    + (mat == null ? material + " material" : "dust_" + material.toLowerCase() + " item"));
            return;
        }
        if (MachineRecipeMaps.Mixer.addRecipe2(false, 16, 16, input, ingredient, output) != null) {
            registered++;
        }
    }

    private static void mixerPair(String a, String b, String out, int count, String source) {
        ItemStack input1 = food(a, 1);
        ItemStack input2 = food(b, 1);
        ItemStack output = food(out, count);
        if (input1.isEmpty() || input2.isEmpty() || output.isEmpty()) {
            return;
        }
        if (MachineRecipeMaps.Mixer.addRecipe2(false, 16, 16, input1, input2, output) != null) {
            registered++;
        }
    }

    private static void mayonnaise(String oilField, String vinegarField, String source) {
        ItemStack yolk = food("egg_yolk", 1);
        FluidStack oil = fluid(oilField, 100);
        FluidStack vinegar = fluid(vinegarField, 100);
        FluidStack mayo = fluid("Mayo", 250);
        ItemStack tag = selector();
        if (yolk.isEmpty() || tag.isEmpty() || oil.isEmpty() || vinegar.isEmpty() || mayo.isEmpty()) {
            SKIPPED.add("egg yolk + " + oilField + " + " + vinegarField + " -> 250 mB Mayo (" + source
                    + "): a fluid or the circuit-selector item input of this row is not in the port");
            return;
        }
        if (MachineRecipeMaps.Mixer.addRecipeX(false, 16, 16, new ItemStack[] {tag, yolk},
                new FluidStack[] {oil, vinegar}, new FluidStack[] {mayo}, ItemStack.EMPTY) != null) {
            registered++;
        }
    }

    // ── 4. Boxinator / Unboxinator ───────────────────────────────────────────────────────────────

    private static void packaging() {
        // MultiItemFood.java:360,370,377 — the packaged food is boxed, the packaging comes back off.
        box("fries", "gregtech:plate_double_paper", "fries_2", "MultiItemFood.java:360");
        box("potato_chips", "gregtech:foil_aluminium", "bag_of_potato_chips", "MultiItemFood.java:370");
        box("chili_chips", "gregtech:foil_aluminium", "bag_of_chili_chips", "MultiItemFood.java:377");
        unbox("fries_2", "fries", "gregtech:scrap_gt_paper", 16, "MultiItemFood.java:361");
        unbox("bag_of_potato_chips", "potato_chips", "gregtech:scrap_gt_aluminium", 2, "MultiItemFood.java:371");
        unbox("bag_of_chili_chips", "chili_chips", "gregtech:scrap_gt_aluminium", 2, "MultiItemFood.java:378");
        // MultiItemFood.java:917-919 — the three pills are boxed from a dust and the empty pill.
        pill("Iodine", "radaway", "MultiItemFood.java:917");
        pill("Mint", "peppermint", "MultiItemFood.java:918");
        ItemStack mushroom = new ItemStack(Items.BROWN_MUSHROOM);
        ItemStack empty = food("empty_wax_pill", 1);
        ItemStack antidote = food("antidote", 1);
        if (!empty.isEmpty() && !antidote.isEmpty()
                && MachineRecipeMaps.Boxinator.addRecipe2(false, 16, 16, mushroom, empty, antidote) != null) {
            registered++;
        }
    }

    private static void box(String inputId, String packageId, String outputId, String source) {
        ItemStack input = food(inputId, 1);
        ItemStack packaging = item(packageId, 1);
        ItemStack output = food(outputId, 1);
        if (input.isEmpty() || packaging.isEmpty() || output.isEmpty()) {
            SKIPPED.add(inputId + " + " + packageId + " -> " + outputId + " (" + source
                    + "): an item of this row is not in the port");
            return;
        }
        if (MachineRecipeMaps.Boxinator.addRecipe2(false, 16, 16, input, packaging, output) != null) {
            registered++;
        }
    }

    private static void unbox(String inputId, String outputId, String scrapId, int scrap, String source) {
        ItemStack input = food(inputId, 1);
        ItemStack output = food(outputId, 1);
        ItemStack residue = item(scrapId, scrap);
        if (input.isEmpty() || output.isEmpty() || residue.isEmpty()) {
            SKIPPED.add(inputId + " -> " + outputId + " + " + scrapId + " (" + source
                    + "): an item of this row is not in the port");
            return;
        }
        if (MachineRecipeMaps.Unboxinator.addRecipe1(false, 16, 16, input, output, residue) != null) {
            registered++;
        }
    }

    private static void pill(String material, String outputId, String source) {
        MaterialPrefix dust = PrefixRegistry.byName("dust");
        GTMaterial mat = GTMaterialRegistry.get(material);
        ItemStack ingredient = dust == null || mat == null ? ItemStack.EMPTY : GTItems.getStack(dust, mat, 1);
        ItemStack empty = food("empty_wax_pill", 1);
        ItemStack output = food(outputId, 1);
        if (ingredient.isEmpty() || empty.isEmpty() || output.isEmpty()) {
            SKIPPED.add("dust(" + material + ") + empty_wax_pill -> " + outputId + " (" + source
                    + "): the port has no such dust or pill item");
            return;
        }
        if (MachineRecipeMaps.Boxinator.addRecipe2(false, 16, 16, ingredient, empty, output) != null) {
            registered++;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────────────────────

    /** A GT6 food item of the generated table (never a guessed id). */
    private static ItemStack food(String id, int count) {
        if (id.indexOf(':') >= 0) {
            return item(id, count);
        }
        Item item = GTFoodItems.itemOrNull(id);
        if (item == null || item==net.minecraft.world.item.Items.AIR) {
            SKIPPED.add("gregtech:" + id + ": the port registers no such GT6 food item");
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }

    /** A port item by full id, for the rows that name a material-prefix or vanilla item. */
    private static ItemStack item(String id, int count) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (item == null || item==net.minecraft.world.item.Items.AIR) {
            SKIPPED.add(id + ": the port registers no such item");
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }

    private static FluidStack fluid(String field, int amount) {
        FluidStack stack = com.gregtech.gregtech.registry.GTFluids.stack(field, amount);
        return stack == null ? FluidStack.EMPTY : stack;
    }

    /**
     * The port's fluid fields carrying a GT6 fluid flag ({@code FluidsGT.COOKING_OIL} /
     * {@code VINEGAR}), sorted so the group loops are deterministic (the recipe map keeps the first
     * recipe for a given input, and the fluid registry is a hash map).
     */
    private static List<String> fluidFields(long flag) {
        java.util.TreeSet<String> fields = new java.util.TreeSet<>();
        for (var entry : com.gregtech.gregtech.data.RegisteredFluids.all().entrySet()) {
            if (entry.getValue().hasFlag(flag)) {
                fields.add(entry.getKey());
            }
        }
        return new ArrayList<>(fields);
    }

    /** GT6 registers these fluid-only rows with {@code ST.tag(0)} as their item input. */
    private static ItemStack selector() {
        Item tag = com.gregtech.gregtech.registry.GTTechnological.selectorTag(0);
        return tag == null ? ItemStack.EMPTY : new ItemStack(tag);
    }
}
