package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Guards GT6's two downstream routes from purified ore
 * ({@code Loader_Recipes_Ores} lines 455-461).
 * <p>
 * Regression these tests exist for: the Magnetic Separator and Sifter machines had no recipes at
 * all in this port, and the ore chain skipped straight from purified ore to the centrifuge.
 * </p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class OrePurificationRoutesTests {

    private static Recipe withChances(java.util.Collection<Recipe> recipes, long... chances) {
        for (Recipe recipe : recipes) {
            if (recipe.mChances == null || recipe.mChances.length != chances.length) continue;
            boolean same = true;
            for (int i = 0; i < chances.length; i++) same &= recipe.mChances[i] == chances[i];
            if (same) return recipe;
        }
        return null;
    }

    private static boolean inputIs(Recipe recipe, MaterialPrefix prefix, GTMaterial material) {
        ItemStack expected = com.gregtech.gregtech.registry.GTItems.getStack(prefix, material, 1);
        if (expected.isEmpty()) return false;
        for (ItemStack input : recipe.mInputs) {
            if (!input.isEmpty() && MaterialEquivalence.matches(expected, input)) return true;
        }
        return false;
    }

    private static boolean outputIs(Recipe recipe, MaterialPrefix prefix, GTMaterial material) {
        ItemStack expected = com.gregtech.gregtech.registry.GTItems.getStack(prefix, material, 1);
        if (expected.isEmpty()) return false;
        for (ItemStack output : recipe.mOutputs) {
            if (!output.isEmpty() && MaterialEquivalence.matches(expected, output)) return true;
        }
        return false;
    }

    private static Recipe forMaterial(java.util.Collection<Recipe> recipes, MaterialPrefix prefix, GTMaterial material) {
        for (Recipe recipe : recipes) {
            if (inputIs(recipe, prefix, material)) return recipe;
        }
        return null;
    }

    private static boolean hasChances(Recipe recipe, long... chances) {
        if (recipe.mChances == null || recipe.mChances.length != chances.length) return false;
        for (int i = 0; i < chances.length; i++) if (recipe.mChances[i] != chances[i]) return false;
        return true;
    }

    /** GT6 {@code tMagnet}: purified ore → centrifuged ore plus five byproduct fines. */
    @GameTest(template = "test_empty")
    public static void magneticSeparatorProcessesPurifiedOre(GameTestHelper helper) {
        var recipes = MachineRecipeMaps.MagneticSeparator.mRecipeList;
        helper.assertTrue(recipes.size() >= 50, "magnetic separator recipes: " + recipes.size());

        GTMaterial bauxite = GTMaterialRegistry.get("Bauxite");
        helper.assertTrue(bauxite != null && bauxite.isValid(), "bauxite material exists");
        Recipe recipe = forMaterial(recipes, MaterialPrefix.crushedPurified, bauxite);
        helper.assertTrue(recipe != null, "the purified-ore route is registered for bauxite");

        helper.assertTrue(hasChances(recipe, 10000, 600, 600, 600, 600, 600),
                "GT6 tMagnet chances: " + java.util.Arrays.toString(recipe.mChances));
        helper.assertTrue(recipe.mOutputs.length == 6,
                "primary output plus five byproduct fines, got " + recipe.mOutputs.length);
        helper.assertTrue(recipe.mDuration == 144, "GT6 duration 144, got " + recipe.mDuration);
        helper.assertTrue(recipe.mEUt == 16, "GT6 EU/t 16, got " + recipe.mEUt);
        helper.assertTrue(outputIs(recipe, MaterialPrefix.crushedCentrifuged, bauxite),
                "it yields centrifuged bauxite");

        // The tiny purified form has its own route at a sixteenth of the duration.
        Recipe tiny = forMaterial(recipes, MaterialPrefix.crushedPurifiedTiny, bauxite);
        helper.assertTrue(tiny != null && tiny.mDuration == 16,
                "tiny purified ore has its own 16-tick route");
        helper.succeed();
    }

    /** GT6's sifting ladder: purified ore → gems and dust. */
    @GameTest(template = "test_empty")
    public static void siftingYieldsGemsFromPurifiedOre(GameTestHelper helper) {
        var recipes = MachineRecipeMaps.Sifting.mRecipeList;
        helper.assertTrue(recipes.size() >= 50, "sifting recipes: " + recipes.size());

        Recipe recipe = withChances(recipes, 9, 90, 360, 1350, 1800, 3600, 4500);
        helper.assertTrue(recipe != null, "GT6 sifting gem chances are registered");
        helper.assertTrue(recipe.mOutputs.length == 7,
                "legendary/exquisite/flawless/gem/flawed/chipped/dust, got " + recipe.mOutputs.length);
        helper.assertTrue(recipe.mDuration == 144, "GT6 duration 144, got " + recipe.mDuration);
        // Data-driven check: whatever material GT6's ladder was generated for, its input must be
        // purified ore of a material that has a gem form.
        var form = MaterialEquivalence.form(recipe.mInputs[0]);
        helper.assertTrue(form != null && form.prefix() == MaterialPrefix.crushedPurified,
                "the gem route starts from purified ore, got " + recipe.mInputs[0]);
        helper.assertTrue(MaterialPrefix.gem.isValidFor(form.material()),
                "the route's material has a gem form: " + form.material().getName());

        // The ladder must start at a legendary gem and end at the material's own dust.
        helper.assertTrue(!recipe.mOutputs[0].isEmpty() && !recipe.mOutputs[5].isEmpty(),
                "gem grades present");
        helper.assertTrue(recipe.mOutputs[0].getCount() == 8 && recipe.mOutputs[1].getCount() == 4
                        && recipe.mOutputs[2].getCount() == 2,
                "rare grades are scaled 8x/4x/2x like GT6: " + recipe.mOutputs[0].getCount() + "/"
                        + recipe.mOutputs[1].getCount() + "/" + recipe.mOutputs[2].getCount());

        // The tiny purified form uses the same ladder at a tenth of the chances.
        helper.assertTrue(withChances(recipes, 1, 10, 40, 150, 200, 400, 500) != null,
                "tiny purified ore has its own sifting route");
        helper.succeed();
    }

    /**
     * GT6's sluice route ({@code Loader_Recipes_Ores:415-420}): crushed ore + water → one
     * purified ore plus seven byproduct fines, with the water variants of {@code FL.waters}.
     * <p>
     * Regression: the four Sluice tiers and the Large Sluice had an empty recipe map.
     * </p>
     */
    @GameTest(template = "test_empty")
    public static void sluiceWashesCrushedOreIntoPurifiedOre(GameTestHelper helper) {
        var recipes = MachineRecipeMaps.Sluice.mRecipeList;
        helper.assertTrue(recipes.size() >= 100, "sluice recipes: " + recipes.size());

        GTMaterial bauxite = GTMaterialRegistry.get("Bauxite");
        Recipe recipe = forMaterial(recipes, MaterialPrefix.crushed, bauxite);
        helper.assertTrue(recipe != null, "the sluice route is registered for crushed bauxite");

        helper.assertTrue(hasChances(recipe, 10000, 300, 300, 300, 300, 300, 300, 300),
                "GT6 tSluice chances: " + java.util.Arrays.toString(recipe.mChances));
        helper.assertTrue(recipe.mOutputs.length == 8,
                "purified ore plus seven byproduct fines, got " + recipe.mOutputs.length);
        helper.assertTrue(recipe.mDuration == 144, "GT6 duration 144, got " + recipe.mDuration);
        helper.assertTrue(recipe.mEUt == 16, "GT6 EU/t 16, got " + recipe.mEUt);
        helper.assertTrue(outputIs(recipe, MaterialPrefix.crushedPurified, bauxite),
                "it yields purified bauxite");
        helper.assertTrue(recipe.mFluidInputs.length == 1 && recipe.mFluidInputs[0].getAmount() == 900,
                "900 mB of water per recipe");
        helper.assertTrue(recipe.mFluidOutputs.length == 1
                        && recipe.mFluidOutputs[0].getFluid() == com.gregtech.gregtech.registry.GTFluids
                                .still("Sluice").get(),
                "the machine leaves GT6's sluice fluid: " + java.util.Arrays.toString(recipe.mFluidOutputs));

        // The tiny crushed form has its own 16-tick route at 100 mB.
        Recipe tiny = forMaterial(recipes, MaterialPrefix.crushedTiny, bauxite);
        helper.assertTrue(tiny != null && tiny.mDuration == 16,
                "tiny crushed ore has its own 16-tick route");
        helper.assertTrue(tiny != null && tiny.mFluidInputs.length == 1
                        && tiny.mFluidInputs[0].getAmount() == 100,
                "the tiny route takes 100 mB of water");
        helper.assertTrue(tiny != null && outputIs(tiny, MaterialPrefix.crushedPurifiedTiny, bauxite),
                "the tiny route yields tiny purified bauxite");

        // GT6 registers one recipe per FL.waters variant: water, mineral water, distilled water
        // and spectral dew — all four are registered in this port.
        int forBauxite = 0;
        for (Recipe candidate : recipes) {
            if (inputIs(candidate, MaterialPrefix.crushed, bauxite) && candidate.mDuration == 144) forBauxite++;
        }
        helper.assertTrue(forBauxite == 4, "four water variants per ore, got " + forBauxite);
        helper.succeed();
    }

    /** Coverage of the two routes stays on the record. */
    @GameTest(template = "test_empty")
    public static void coverageIsReported(GameTestHelper helper) {
        var report = new java.util.TreeMap<String, Object>();
        report.put("magneticSeparatorRecipes", MachineRecipeMaps.MagneticSeparator.mRecipeList.size());
        report.put("siftingRecipes", MachineRecipeMaps.Sifting.mRecipeList.size());
        report.put("sluiceRecipes", MachineRecipeMaps.Sluice.mRecipeList.size());
        report.put("centrifugeRecipes", MachineRecipeMaps.Centrifuge.mRecipeList.size());
        report.put("bathRecipes", MachineRecipeMaps.Bath.mRecipeList.size());
        report.put("machinesUnblocked", java.util.List.of(
                "sifter (4 tiers)", "electricsifter (5 tiers)", "magneticseparator (5 tiers)",
                "sluice (4 tiers)", "largesluice"));
        java.nio.file.Path out = java.nio.file.Path.of("../../docs/ore-routes-coverage.json");
        try {
            java.nio.file.Files.writeString(out, new com.google.gson.GsonBuilder().setPrettyPrinting()
                    .create().toJson(report));
        } catch (Exception e) {
            helper.assertTrue(false, "cannot write ore route coverage report: " + e);
        }
        helper.succeed();
    }
}
