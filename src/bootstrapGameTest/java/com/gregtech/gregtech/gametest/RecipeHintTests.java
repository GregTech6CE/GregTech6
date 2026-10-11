package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.content.recipe.GTMainRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Guards GT6's "Did you know...?" recipe viewer page ({@code RM.DidYouKnow},
 * {@code Loader_Recipes_Hints}).
 *
 * <p>GT6 shows its hint pages on their own recipe viewer tab: display-only rows whose ingredients
 * carry a label ("Wait until it melts into Mercury"). The port already had the map (GT6's internal
 * key {@code gt.recipe.other}, alias {@code Other}) but no hint rows; this batch ports five of them
 * through the spec based row table of {@link GTMainRecipes}, including the ingredient labels.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class RecipeHintTests {

    /** The page exists with GT6's key, name and slot layout. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void hintPageMatchesGt6(GameTestHelper helper) {
        RecipeMap map = MachineRecipeMaps.DidYouKnow;
        helper.assertTrue(map == MachineRecipeMaps.Other, "GT6 aliases Other to DidYouKnow");
        helper.assertTrue(map.mNameInternal.equals("gt.recipe.other"),
                "GT6's internal key, got " + map.mNameInternal);
        helper.assertTrue(map.mNameLocal.equals("Did you know...?"),
                "GT6's page name, got " + map.mNameLocal);
        helper.assertTrue(RecipeMap.RECIPE_MAP_LIST.contains(map), "the page is registered for the viewer");
        helper.assertTrue(map.mInputItemsCount == 6 && map.mOutputItemsCount == 6
                        && map.mInputFluidCount == 3 && map.mOutputFluidCount == 3,
                "GT6 declares 6 item and 3 fluid slots per side");
        helper.succeed();
    }

    /** The ported hint rows are display-only, labelled and resolvable. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void hintRowsAreDisplayOnlyAndLabelled(GameTestHelper helper) {
        GTMainRecipes.register();
        RecipeMap map = MachineRecipeMaps.DidYouKnow;
        List<String> problems = new ArrayList<>();
        int rows = 0, labelled = 0;
        for (Recipe recipe : map.mRecipeList) {
            rows++;
            if (!recipe.mFakeRecipe) problems.add("hint row " + rows + " is not marked as a fake recipe");
            boolean hasLabel = false;
            for (ItemStack stack : recipe.mInputs) {
                if (stack == null || stack.isEmpty()) {
                    problems.add("hint row " + rows + " has an empty ingredient");
                    continue;
                }
                if (stack.hasCustomHoverName()) hasLabel = true;
            }
            for (ItemStack stack : recipe.mOutputs) {
                if (stack != null && !stack.isEmpty() && stack.hasCustomHoverName()) hasLabel = true;
            }
            if (!hasLabel) problems.add("hint row " + rows + " carries no GT6 label");
            labelled++;
        }
        helper.assertTrue(rows >= 3, "at least three GT6 hint rows are ported, got " + rows);
        helper.assertTrue(labelled == rows, "every hint row is labelled, " + labelled + "/" + rows);
        // The ported rows must not have been dropped for missing content: the cinnabar/steel rows are
        // the classic GT6 hints and only use content the port has.
        long skips = GTMainRecipes.skipped().stream().filter(s -> s.contains("DidYouKnow")).count();
        helper.assertTrue(skips == 0, "hint rows skipped for missing content: "
                + GTMainRecipes.skipped().stream().filter(s -> s.contains("DidYouKnow")).toList());
        helper.assertTrue(problems.isEmpty(), "hint rows (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }

    /** GT6's hint rows describe real chains of the port: cinnabar to mercury and iron to steel. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void hintsDescribePortChains(GameTestHelper helper) {
        GTMainRecipes.register();
        List<String> labels = new ArrayList<>();
        for (Recipe recipe : MachineRecipeMaps.DidYouKnow.mRecipeList) {
            for (ItemStack stack : recipe.mInputs) {
                if (stack != null && !stack.isEmpty() && stack.hasCustomHoverName()) {
                    labels.add(stack.getHoverName().getString());
                }
            }
            for (ItemStack stack : recipe.mOutputs) {
                if (stack != null && !stack.isEmpty() && stack.hasCustomHoverName()) {
                    labels.add(stack.getHoverName().getString());
                }
            }
        }
        String joined = String.join(" | ", labels);
        helper.assertTrue(joined.contains("Cinnabar"), "the cinnabar hint is ported: " + joined);
        helper.assertTrue(joined.contains("Mercury"), "the mercury result is ported");
        helper.assertTrue(joined.contains("Steel"), "the steel hint is ported");
        helper.assertTrue(joined.contains("Bathing Pot") || joined.contains("Faucet"),
                "the galvanized steel hint is ported");
        helper.succeed();
    }
}
