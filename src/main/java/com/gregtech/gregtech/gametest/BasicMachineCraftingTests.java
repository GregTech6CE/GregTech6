package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.data.BasicMachineCraftingRecipes;
import com.gregtech.gregtech.data.BasicMachineRecipePack;
import com.gregtech.gregtech.data.MachineRecipeIngredients;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Guards the single-block machine crafting recipes against the original GT6
 * "Basic Machines" registrations (see {@link BasicMachineCraftingRecipes}).
 * <p>
 * The regression these tests exist for: the port used to emit three generic patterns
 * chosen only by energy type (HU/RU/EU), so every machine's crafting grid differed from
 * GregTech 6. These tests assert the original pattern and that no machine is uncraftable.
 * </p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class BasicMachineCraftingTests {

    private record MachineRecipe(String id, ShapedRecipe recipe) {}

    /** All crafting recipes produced by the machine recipe pack, keyed by result item. */
    private static List<MachineRecipe> machineRecipes(GameTestHelper helper) {
        List<MachineRecipe> result = new ArrayList<>();
        for (var recipe : helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!(recipe instanceof ShapedRecipe shaped)) continue;
            String path = recipe.getId().getPath();
            if (!path.startsWith("machines/basic/")) continue;
            result.add(new MachineRecipe(recipe.getId().toString(), shaped));
        }
        return result;
    }

    /** Every machine must be craftable with the ORIGINAL GregTech 6 pattern. */
    @GameTest(template = "test_empty")
    public static void everyMachineUsesItsOriginalPattern(GameTestHelper helper) {
        var byResult = new IdentityHashMap<net.minecraft.world.item.Item, MachineRecipe>();
        for (MachineRecipe machine : machineRecipes(helper)) {
            var stack = machine.recipe().getResultItem(helper.getLevel().registryAccess());
            byResult.put(stack.getItem(), machine);
        }

        TreeSet<String> problems = new TreeSet<>();
        int checked = 0;
        for (var entry : MachineRegistry.basicMachines()) {
            if (!entry.isPresent()) continue;
            var spec = entry.get().basicSpec();
            if (BasicMachineRecipePack.MULTIBLOCK_CONTROLLERS.contains(spec.machineName())) continue;

            MachineRecipe machine = byResult.get(entry.get().asItem());
            if (machine == null) {
                problems.add("no crafting recipe: " + spec.id());
                continue;
            }
            var table = BasicMachineCraftingRecipes.find(spec.machineName(), spec.tier());
            if (table == null) {
                problems.add("no original recipe row: " + spec.machineName() + " tier " + spec.tier());
                continue;
            }

            String[] rows = table.rows();
            if (machine.recipe().getHeight() != rows.length) {
                problems.add(spec.id() + ": pattern rows " + machine.recipe().getHeight()
                        + " != original " + rows.length);
                continue;
            }
            if (machine.recipe().getWidth() != rows[0].length()) {
                problems.add(spec.id() + ": pattern width " + machine.recipe().getWidth()
                        + " != original " + rows[0].length());
                continue;
            }
            for (int y = 0; y < rows.length; y++) {
                for (int x = 0; x < rows[y].length(); x++) {
                    Ingredient ingredient = machine.recipe().getIngredients().get(x + y * rows[y].length());
                    boolean expectedFilled = rows[y].charAt(x) != ' ';
                    if (ingredient.isEmpty() == expectedFilled) {
                        problems.add(spec.id() + ": cell " + x + "," + y + " filled=" + !ingredient.isEmpty()
                                + " expected=" + expectedFilled + " (original row \"" + rows[y] + "\")");
                    }
                }
            }
            checked++;
        }

        helper.assertTrue(checked > 200, "machine variants checked: " + checked);
        helper.assertTrue(problems.isEmpty(),
                "machines not matching the original GT6 crafting pattern (" + problems.size() + "): " + problems);
        helper.succeed();
    }

    /** No machine recipe may contain an ingredient that matches nothing. */
    @GameTest(template = "test_empty")
    public static void everyIngredientResolves(GameTestHelper helper) {
        List<String> empty = new ArrayList<>();
        int ingredients = 0;
        for (MachineRecipe machine : machineRecipes(helper)) {
            var list = machine.recipe().getIngredients();
            for (int i = 0; i < list.size(); i++) {
                Ingredient ingredient = list.get(i);
                if (ingredient.isEmpty()) continue;
                ingredients++;
                if (ingredient.getItems().length == 0) {
                    empty.add(machine.id() + "#" + i);
                }
            }
        }
        helper.assertTrue(ingredients > 500, "ingredients checked: " + ingredients);
        helper.assertTrue(empty.isEmpty(), "ingredients resolving to nothing: " + empty);
        helper.succeed();
    }

    /** The table must cover exactly the machines the pack is expected to produce recipes for. */
    @GameTest(template = "test_empty")
    public static void tableCoversEverySingleBlockMachine(GameTestHelper helper) {
        TreeSet<String> registered = new TreeSet<>();
        for (var entry : MachineRegistry.basicMachines()) {
            if (!entry.isPresent()) continue;
            var spec = entry.get().basicSpec();
            if (BasicMachineRecipePack.MULTIBLOCK_CONTROLLERS.contains(spec.machineName())) continue;
            registered.add(spec.machineName());
        }
        TreeSet<String> missing = new TreeSet<>(registered);
        missing.removeAll(BasicMachineCraftingRecipes.machines());
        helper.assertTrue(missing.isEmpty(), "machine types without an original crafting recipe: " + missing);
        helper.assertTrue(registered.size() > 50, "single-block machine types: " + registered.size());
        helper.succeed();
    }

    /** Ingredient translation may substitute, but must stay visible instead of silently breaking a recipe. */
    @GameTest(template = "test_empty")
    public static void substitutionsAreReported(GameTestHelper helper) {
        Map<String, String> substitutions = MachineRecipeIngredients.substitutions();
        java.nio.file.Path out = java.nio.file.Path.of("../../docs/machine-crafting-substitutions.json");
        try {
            java.nio.file.Files.writeString(out, new com.google.gson.GsonBuilder().setPrettyPrinting()
                    .create().toJson(substitutions));
        } catch (Exception e) {
            helper.assertTrue(false, "cannot write substitution report: " + e);
        }
        // Every original GT6 ingredient must resolve to its exact 1.20.1 counterpart, except the
        // documented GT6 "ANY family" keys (OP.wireGtNN.dat(ANY.X) accepts any material of that
        // group, which the port expresses as "any wire of that size"). A new entry here means a
        // currently unmet ingredient and must be reviewed before extending this allow-list.
        // The Lightning Processor is single-tier in this port, so only GT6's 1x iron wire key is used.
        // The Lightning Processor used to need GT6's "any 1x wire" key ("wire 1 of Iron") and was the
        // only substitution in the table. It is multiblock-only in this port now (its controller owns
        // the machine), so no single-block recipe substitutes anything — an empty map is the expected
        // state and any new entry means an unmet ingredient again.
        Map<String, String> allowed = Map.of();
        helper.assertTrue(substitutions.equals(allowed),
                "machine crafting ingredient substitutions changed (see "
                        + "docs/machine-crafting-substitutions.json): " + substitutions);
        helper.succeed();
    }

    /** Sanity: the original table itself must stay populated and unique. */
    @GameTest(template = "test_empty")
    public static void originalTableIsWellFormed(GameTestHelper helper) {        int previous = 0;
        for (var entry : BasicMachineCraftingRecipes.ENTRIES) {
            helper.assertTrue(!entry.keys().isEmpty(), "recipe has keys: " + entry.machine());
            String[] rows = entry.rows();
            helper.assertTrue(rows.length >= 1 && rows.length <= 3, "pattern row count: " + entry.machine());
            int width = rows[0].length();
            for (String row : rows) {
                helper.assertTrue(row.length() == width, "rectangular pattern: " + entry.machine());
                for (char symbol : row.toCharArray()) {
                    helper.assertTrue(symbol == ' ' || entry.keys().containsKey(symbol),
                            "pattern symbol '" + symbol + "' has a key: " + entry.machine());
                }
            }
            for (char symbol : entry.keys().keySet()) {
                boolean used = false;
                for (String row : rows) used |= row.indexOf(symbol) >= 0;
                helper.assertTrue(used, "key '" + symbol + "' appears in the pattern: " + entry.machine());
            }
        }
        helper.assertTrue(BasicMachineCraftingRecipes.ENTRIES.size() > 200, "table entries");
        helper.succeed();
    }
}
