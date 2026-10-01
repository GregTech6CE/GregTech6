package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.loaders.c.Loader_Recipes_Parts;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Guards GT6's generic extruder table ({@code Loader_Recipes_Handlers:740-800}).
 * <p>
 * Regression these tests exist for: the port only registered the ingot row of the hot mold family, so
 * tool heads, blocks, non-ingot inputs and every {@code low_heat_extruder_shape_*} mold were missing —
 * players could not shape a pickaxe head or a block of metal, and JEI showed no low-heat alternative.
 * </p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class ExtruderMatrixTests {

    /** The extruder table now holds tens of thousands of recipes, so it is indexed once per test. */
    private static java.util.Map<String, List<Recipe>> index() {
        java.util.Map<String, List<Recipe>> index = new java.util.HashMap<>();
        for (Recipe recipe : MachineRecipeMaps.Extruder.mRecipeList) {
            for (ItemStack input : recipe.mInputs) {
                if (input == null || input.isEmpty()) continue;
                index.computeIfAbsent(key(input), ignored -> new ArrayList<>()).add(recipe);
            }
        }
        return index;
    }

    private static String key(ItemStack stack) {
        return net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem())
                + "@" + stack.getDamageValue();
    }

    private static Recipe route(java.util.Map<String, List<Recipe>> index, ItemStack input, String moldId,
                                ItemStack output) {
        List<Recipe> candidates = index.get(key(input));
        if (candidates == null) return null;
        for (Recipe recipe : candidates) {
            if (!contains(recipe.mInputs, input)) continue;
            if (moldId == null ? !recipe.mInputs[1].isEmpty() : !contains(recipe.mInputs, mold(moldId))) continue;
            if (contains(recipe.mOutputs, output)) return recipe;
        }
        return null;
    }

    private static ItemStack mold(String moldId) {
        net.minecraft.world.item.Item item = GTTechnological.get(moldId);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static boolean contains(ItemStack[] stacks, ItemStack expected) {
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty() || expected.isEmpty()) continue;
            if (ItemStack.isSameItemSameTags(stack, expected) || MaterialEquivalence.matches(expected, stack)) {
                return true;
            }
        }
        return false;
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void toolHeadsAndBlocksCanBeExtruded(GameTestHelper helper) {
        GTMaterial steel = GTMaterialRegistry.get("Steel");
        helper.assertTrue(steel != null && steel.isValid(), "steel material exists");
        ItemStack ingot = GTItems.getStack(MaterialPrefix.ingot, steel, 1);
        var index = index();

        // GT6 Loader_Recipes_Handlers:756-768 — tool heads, gears, blocks come from the same table.
        for (var pair : List.of(
                new String[]{"pickaxehead", "toolHeadRawPickaxe"},
                new String[]{"axehead", "toolHeadRawAxe"},
                new String[]{"shovelhead", "toolHeadRawShovel"},
                new String[]{"swordblade", "toolHeadRawSword"},
                new String[]{"hammerhead", "toolHeadHammer"},
                new String[]{"filehead", "toolHeadFile"})) {
            MaterialPrefix prefix = com.gregtech.gregtech.api.prefix.PrefixRegistry.byName(pair[1]);
            ItemStack output = GTItems.getStack(prefix, steel, 1);
            helper.assertTrue(!output.isEmpty(), "port has the " + pair[1] + " form");
            helper.assertTrue(route(index, ingot, "extruder_shape_" + pair[0], output) != null,
                    "hot mold extrudes " + pair[1] + " from a steel ingot");
            helper.assertTrue(route(index, ingot, "low_heat_extruder_shape_" + pair[0], output) != null,
                    "low-heat mold extrudes " + pair[1] + " from a steel ingot");
        }

        // blockSolid is 9U, so GT6 scales the input to nine ingots (Loader_Recipes_Handlers:812).
        ItemStack block = com.gregtech.gregtech.registry.GTBlocks.getStack(
                com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.blockSolid, steel);
        helper.assertTrue(!block.isEmpty(), "steel has a solid block form");
        Recipe blockRoute = route(index, ingot, "extruder_shape_block", block);
        helper.assertTrue(blockRoute != null, "hot mold extrudes a solid steel block");
        helper.assertTrue(blockRoute.mInputs[0].getCount() == 9,
                "nine steel ingots (9U) feed one 9U block, got " + blockRoute.mInputs[0].getCount());
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void nonIngotInputsFeedTheSameTable(GameTestHelper helper) {
        GTMaterial copper = GTMaterialRegistry.get("Copper");
        ItemStack plate = GTItems.getStack(MaterialPrefix.plate, copper, 1);
        ItemStack rod = GTItems.getStack(MaterialPrefix.stick, copper, 1);
        helper.assertTrue(!plate.isEmpty() && !rod.isEmpty(), "copper plate and rod exist");
        var index = index();
        // GT6 iterates EXTRUDER_FODDER / INGOT_BASED / GEM_BASED prefixes as INPUT, not just ingots.
        helper.assertTrue(route(index, plate, "extruder_shape_rod", rod) != null, "a plate extrudes into rods");
        ItemStack gem = GTItems.getStack(MaterialPrefix.gem, GTMaterialRegistry.get("Diamond"), 1);
        ItemStack tiny = GTItems.getStack(MaterialPrefix.plateTiny, GTMaterialRegistry.get("Diamond"), 1);
        if (!gem.isEmpty() && !tiny.isEmpty()) {
            helper.assertTrue(route(index, gem, "extruder_shape_tinyplate", tiny) != null,
                    "a gem extrudes into tiny plates");
        }
        helper.succeed();
    }

    /** The matrix must actually grow the table, and the GT6 outputs the port lacks stay recorded. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void matrixCoverageIsReported(GameTestHelper helper) {
        int recipes = MachineRecipeMaps.Extruder.mRecipeList.size();
        List<String> skipped = Loader_Recipes_Parts.EXTRUDER_SKIPPED;
        helper.assertTrue(recipes > 3000, "extruder recipes after the matrix: " + recipes);
        helper.assertTrue(skipped.size() >= 4, "GT6 extruder outputs recorded as skipped: " + skipped);
        List<String> report = new ArrayList<>(skipped);
        helper.assertTrue(report.stream().anyMatch(s -> s.contains("casingSmall")),
                "casingSmall is the documented missing prefix: " + report);
        var json = new java.util.TreeMap<String, Object>();
        json.put("extruderRecipes", recipes);
        json.put("skippedGt6Outputs", skipped);
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/extruder-matrix-coverage.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (java.io.IOException e) {
            helper.fail("cannot write docs/extruder-matrix-coverage.json: " + e);
            return;
        }
        helper.succeed();
    }
}
