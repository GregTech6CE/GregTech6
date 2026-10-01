package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.content.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class SurvivalProcessingTests {
    @GameTest(template="test_empty")
    public static void publishedSurvivalRecipesConsumeInputsAndRetainTools(GameTestHelper h) {
        h.assertTrue(TextileFinishingRecipes.INSTANCE.entries().size() == 331, "331 textile recipes");
        h.assertTrue(BiologicalMaterialRecipes.INSTANCE.entries().size() == 21, "21 biological recipes");
        h.assertTrue(SurvivalUtilityRecipes.INSTANCE.entries().size() == 78, "78 utility recipes");
        for (var batch : List.of(TextileFinishingRecipes.INSTANCE, BiologicalMaterialRecipes.INSTANCE, SurvivalUtilityRecipes.INSTANCE))
            for (var entry : batch.entries()) {
                var r = entry.recipe();
                h.assertTrue(entry.map().mRecipeList.contains(r) && !r.mFakeRecipe && !r.mHidden, "Live machine/JEI recipe: " + entry.id());
                var items = Arrays.stream(r.mInputs).map(ItemStack::copy).toList();
                var fluids = Arrays.stream(r.mFluidInputs).map(FluidStack::copy).toList();
                var result = RecipeInputs.consume(r, items, fluids, 1);
                h.assertTrue(result != null, "Executable: " + entry.id());
                for (int i = 0; i < items.size(); i++) {
                    h.assertTrue(r.isCatalystInput(i) ? result.items().get(i).getCount() == items.get(i).getCount() : result.items().get(i).isEmpty(), "Retained selector/blade or consumed ingredient: " + entry.id());
                    var missing = new ArrayList<>(items);
                    missing.set(i, ItemStack.EMPTY);
                    h.assertTrue(RecipeInputs.consume(r, missing, fluids, 1) == null, "Every ingredient required: " + entry.id());
                }
                h.assertTrue(result.fluids().stream().allMatch(FluidStack::isEmpty), "Exact fluids consumed: " + entry.id());
                if (!fluids.isEmpty()) {
                    var shortFluids = new ArrayList<>(fluids);
                    var shortStack = fluids.get(0).copy(); shortStack.shrink(1); shortFluids.set(0, shortStack);
                    h.assertTrue(RecipeInputs.consume(r, items, shortFluids, 1) == null, "One mB short is rejected: " + entry.id());
                }
            }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void dyeBathsKeepRoundingAndBleachingQuantities(GameTestHelper h) {
        var batch = TextileFinishingRecipes.INSTANCE;
        h.assertTrue(row(batch, "Dye_Flower_Red/carpet").mFluidInputs[0].getAmount() == 5, "144/32 rounded upward");
        h.assertTrue(row(batch, "Dye_Chemical_Blue/pane").mFluidInputs[0].getAmount() == 4, "144*3/128 rounded upward");
        h.assertTrue(row(batch, "bleach/red_wool").mFluidInputs[0].getAmount() == 50, "Chlorine is 1000 mB per unit, not molten 144");
        h.assertTrue(row(batch, "paper/DistW").mFluidInputs[0].getAmount() == 100 && row(batch, "paper/SpDew").mFluidInputs[0].getAmount() == 125, "Only distilled water receives original discount");
        var red = row(batch, "Dye_Chemical_Red/wool");
        h.assertTrue(red.mOutputs[0].is(Items.RED_WOOL) && red.mEUt == 0, "Passive red dye bath");
        var found = MachineRecipeMaps.Bath.findRecipe(Arrays.asList(red.mInputs), Arrays.asList(red.mFluidInputs), false, 6, 6);
        h.assertTrue(found == red, "Dye bath reachable through real machine lookup");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void slimeProvidesLatexForExistingRubberChain(GameTestHelper h) {
        var slime = row(BiologicalMaterialRecipes.INSTANCE, "slimeball/squeeze");
        var latex = row(BiologicalMaterialRecipes.INSTANCE, "Slime_Green/latex_glue");
        h.assertTrue(slime.mInputs[0].is(Items.SLIME_BALL) && slime.mFluidOutputs[0].getAmount() == 250, "Vanilla survival input supplies one centrifuge batch");
        h.assertTrue(RecipeInputs.consume(latex, List.of(), List.of(slime.mFluidOutputs[0].copy()), 1) != null, "Squeezer directly feeds centrifuge");
        h.assertTrue(latex.mFluidOutputs[0].getAmount() == 72 && latex.mFluidOutputs[1].getAmount() == 250, "Original latex/glue yields");
        var coagulator = MachineRecipeMaps.Coagulator.findRecipe(List.of(), List.of(latex.mFluidOutputs[0].copy()), false, 6, 6);
        h.assertTrue(coagulator != null && coagulator.mFluidInputs[0].getAmount() == 16 && !coagulator.mOutputs[0].isEmpty(), "Existing rubber nugget route now has a survival upstream");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void loomSelectorsDisambiguateAndAreReusable(GameTestHelper h) {
        var wool = row(TextileFinishingRecipes.INSTANCE, "loom/wool");
        var web = row(TextileFinishingRecipes.INSTANCE, "loom/cobweb");
        for (var r : List.of(wool, web)) {
            var found = MachineRecipeMaps.Loom.findRecipe(Arrays.asList(r.mInputs), List.of(), false, 6, 1);
            h.assertTrue(found == r, "Correct selector finds intended loom output");
            var result = RecipeInputs.consume(r, Arrays.asList(r.mInputs), List.of(), 1);
            h.assertTrue(result != null && result.items().get(0).getCount() == 1 && result.items().get(1).isEmpty(), "Selector retained, four strings consumed");
        }
        h.assertTrue(wool.mOutputs[0].is(Items.WHITE_WOOL) && web.mOutputs[0].is(Items.COBWEB), "Distinct outputs");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void survivalDropsAndRestoredMaterialFormsAreUsable(GameTestHelper h) {
        var fermented = row(SurvivalUtilityRecipes.INSTANCE, "mix/fermented_eye");
        h.assertTrue(RecipeInputs.consume(fermented, List.of(new ItemStack(Items.SUGAR), new ItemStack(Items.SPIDER_EYE), new ItemStack(Items.BROWN_MUSHROOM)), List.of(), 1) != null, "Vanilla sugar crafts fermented eye without a duplicate material item");
        var magma = row(BiologicalMaterialRecipes.INSTANCE, "Slime_Green/magma");
        h.assertTrue(magma.mInputs[0].is(Items.BLAZE_POWDER), "Vanilla blaze powder enters slime chain");
        var gold = row(SurvivalUtilityRecipes.INSTANCE, "mix/glistering_melon");
        h.assertTrue(gold.mInputs[1].is(Items.GOLD_NUGGET), "Vanilla nuggets accepted");
        var sugar = com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Sugar");
        h.assertTrue(!com.gregtech.gregtech.registry.GTItems.getStack(MaterialPrefix.gemChipped, sugar).isEmpty(), "Forced sugar crystal form restored");
        h.assertTrue(com.gregtech.gregtech.registry.GTItems.getStack(MaterialPrefix.gem, sugar).isEmpty(), "Sugar does not gain unrelated gem forms");
        var wood = com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood");
        for (var prefix : List.of(MaterialPrefix.dust, MaterialPrefix.dustSmall, MaterialPrefix.dustTiny))
            h.assertTrue(!com.gregtech.gregtech.registry.GTItems.getStack(prefix, wood).isEmpty(), "Original G_WOOD dust forms registered");
        h.succeed();
    }
    private static Recipe row(OriginalRecipeBatch batch, String id) {
        return batch.entries().stream().filter(e -> e.id().equals(id)).findFirst().orElseThrow().recipe();
    }
}
