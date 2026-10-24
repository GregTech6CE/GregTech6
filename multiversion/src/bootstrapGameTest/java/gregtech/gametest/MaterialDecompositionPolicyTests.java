package gregtech.gametest;

import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.recipe.Recipe;
import gregtech.internal.recipe.RecipeInputs;
import gregtech.internal.recipe.RecipeMap;
import gregtech.data.MachineRecipeMaps;
import gregtech.data.MaterialPrefix;
import gregtech.registry.GTItems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Actual startup maps, not a synthetic loader invocation. Phase/work-parameter restoration is separate. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class MaterialDecompositionPolicyTests {
    private static ItemStack dust(GameTestHelper h, String material, int count) {
        var resolved = GTMaterialRegistry.get(material).resolve();
        h.assertTrue(resolved.isValid(), "actual material exists: " + material);
        var stack = GTItems.getStack(MaterialPrefix.dust, resolved, count);
        h.assertTrue(!stack.isEmpty() && stack.getCount() == count, "actual native dust exists: " + material);
        return stack;
    }

    private static Recipe lookup(GameTestHelper h, RecipeMap map, List<ItemStack> items, List<FluidStack> fluids) {
        return map.findRecipe(items, fluids, false, 6, 6, h.getLevel(), null, ItemStack.EMPTY);
    }

    private static ItemStack selectorZero(GameTestHelper h) {
        ItemStack selector = gregtech.content.recipe.generated.GeneratedRecipeResolver.resolveSpec("tag:0");
        h.assertTrue(selector != null && !selector.isEmpty() && selector.getCount() == 1, "actual selector zero exists");
        return selector;
    }

    private static boolean item(Recipe recipe, ItemStack input) {
        return Arrays.stream(recipe.mInputs).anyMatch(s -> !s.isEmpty() && s.is(input.getItem()));
    }

    private static String fluidName(FluidStack fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid.getFluid()).getPath().replace("_", "");
    }

    private static String describe(Recipe recipe) {
        if (recipe == null) return "none";
        return "inputs=" + Arrays.toString(recipe.mInputs) + ", fluidInputs=" + Arrays.toString(recipe.mFluidInputs)
                + ", outputs=" + Arrays.toString(recipe.mOutputs) + ", fluidOutputs=" + Arrays.toString(recipe.mFluidOutputs);
    }

    // Diagnostic only: resolve the exact existing generated row, never register it or change a map.
    private static String nativeItems(ItemStack[] values) {
        return Arrays.stream(values).map(s -> s == null ? "null" : s.isEmpty() ? "empty"
                : BuiltInRegistries.ITEM.getKey(s.getItem()) + "=" + s.getCount()).toList().toString();
    }

    private static String nativeFluids(FluidStack[] values) {
        return Arrays.stream(values).map(s -> s == null ? "null" : s.isEmpty() ? "empty"
                : BuiltInRegistries.FLUID.getKey(s.getFluid()) + "=" + s.getAmount()).toList().toString();
    }

    private static String nativeRecipe(Recipe recipe) {
        if (recipe == null) return "none";
        return "items=" + nativeItems(recipe.mInputs) + "; fluids=" + nativeFluids(recipe.mFluidInputs)
                + "; outputs=" + nativeItems(recipe.mOutputs) + "; fluidOutputs=" + nativeFluids(recipe.mFluidOutputs)
                + "; EUt=" + recipe.mEUt + "; ticks=" + recipe.mDuration;
    }

    private static String specializedDiagnostic(ItemStack coal, ItemStack alumina, ItemStack aluminium) {
        var rows = MachineRecipeMaps.Electrolyzer.mRecipeList.stream().filter(r -> r.mEnabled && !r.mFakeRecipe).toList();
        var coalRows = rows.stream().filter(r -> item(r, coal)).toList();
        var bothRows = coalRows.stream().filter(r -> item(r, alumina)).toList();
        var arityRows = bothRows.stream().filter(r -> r.mInputs.length == 2 && r.mFluidInputs.length == 2).toList();
        var fluidRows = arityRows.stream().filter(r -> Arrays.stream(r.mFluidInputs).anyMatch(
                f -> !f.isEmpty() && fluidName(f).endsWith("cryolite")) && Arrays.stream(r.mFluidInputs).anyMatch(
                f -> !f.isEmpty() && fluidName(f).endsWith("aluminiumfluoride"))).toList();
        var outputRows = fluidRows.stream().filter(r -> Arrays.stream(r.mOutputs).anyMatch(
                s -> !s.isEmpty() && s.is(aluminium.getItem()))).toList();
        long consumableRows = outputRows.stream().filter(r -> RecipeInputs.consume(r,
                List.of(coal, alumina), Arrays.asList(r.mFluidInputs), 1) != null).count();
        var related = rows.stream().filter(r -> item(r, coal) || item(r, alumina))
                .map(MaterialDecompositionPolicyTests::nativeRecipe).toList();
        var inputs = new ItemStack[]{
                gregtech.content.recipe.generated.GeneratedRecipeResolver.resolveSpec("i:dust:Coal:3"),
                gregtech.content.recipe.generated.GeneratedRecipeResolver.resolveSpec("i:dust:Al2O3:10")};
        var outputs = new ItemStack[]{
                gregtech.content.recipe.generated.GeneratedRecipeResolver.resolveSpec("omd:Al:4000")};
        var fluidInputs = new FluidStack[]{
                gregtech.content.recipe.generated.GeneratedRecipeResolver.resolveFluidSpec("m:Na3AlF6:14"),
                gregtech.content.recipe.generated.GeneratedRecipeResolver.resolveFluidSpec("m:AlF3:28")};
        var fluidOutputs = new FluidStack[]{
                gregtech.content.recipe.generated.GeneratedRecipeResolver.resolveFluidSpec("m:CO2:9000"),
                gregtech.content.recipe.generated.GeneratedRecipeResolver.resolveFluidSpec("m:F:28")};
        boolean resolved = Arrays.stream(inputs).allMatch(s -> s != null && !s.isEmpty())
                && Arrays.stream(outputs).allMatch(s -> s != null && !s.isEmpty())
                && Arrays.stream(fluidInputs).allMatch(s -> s != null && !s.isEmpty())
                && Arrays.stream(fluidOutputs).allMatch(s -> s != null && !s.isEmpty());
        Recipe candidate = resolved ? new Recipe(inputs, outputs, null, null, fluidInputs, fluidOutputs, 2048, 16, 0) : null;
        Recipe collision = candidate == null ? null : MachineRecipeMaps.Electrolyzer.findCollision(candidate);
        return "fixture=" + nativeItems(new ItemStack[]{coal, alumina, aluminium})
                + "; enabled=" + rows.size() + "; coalRows=" + coalRows.size() + "; coalAndAluminaRows=" + bothRows.size()
                + "; correctArityRows=" + arityRows.size() + "; fluidIdentityRows=" + fluidRows.size()
                + "; aluminiumOutputRows=" + outputRows.size() + "; consumableRows=" + consumableRows
                + "; resolvedInputs=" + nativeItems(inputs) + "; resolvedOutputs=" + nativeItems(outputs)
                + "; resolvedFluidInputs=" + nativeFluids(fluidInputs) + "; resolvedFluidOutputs=" + nativeFluids(fluidOutputs)
                + "; nativeCollision={" + nativeRecipe(collision) + "}; acceptedRelated=" + related;
    }

    /** Preserve a real specialized recipe without pretending its current fluid units match the original. */
    private static Recipe specializedAlumina(GameTestHelper h) {
        ItemStack coal = dust(h, "Coal", 3), alumina = dust(h, "Alumina", 10), aluminium = dust(h, "Aluminium", 1);
        var candidates = MachineRecipeMaps.Electrolyzer.mRecipeList.stream()
                .filter(r -> r.mEnabled && !r.mFakeRecipe && r.mInputs.length == 2 && item(r, coal) && item(r, alumina))
                .filter(r -> r.mFluidInputs.length == 2
                        && Arrays.stream(r.mFluidInputs).anyMatch(f -> !f.isEmpty() && fluidName(f).endsWith("cryolite"))
                        && Arrays.stream(r.mFluidInputs).anyMatch(f -> !f.isEmpty() && fluidName(f).endsWith("aluminiumfluoride")))
                .filter(r -> Arrays.stream(r.mOutputs).anyMatch(s -> !s.isEmpty() && s.is(aluminium.getItem())))
                .filter(r -> RecipeInputs.consume(r, List.of(coal, alumina), Arrays.asList(r.mFluidInputs), 1) != null)
                .toList();
        h.assertTrue(!candidates.isEmpty(), "actual coal3 + Alumina10 + cryolite + AlF3 specialized route is retained; "
                + specializedDiagnostic(coal, alumina, aluminium));
        Recipe recipe = candidates.get(0);
        var fluids = Arrays.stream(recipe.mFluidInputs).map(FluidStack::copy).toList();
        h.assertTrue(lookup(h, MachineRecipeMaps.Electrolyzer, List.of(coal, alumina), fluids) == recipe,
                "actual machine lookup selects the specialized reaction, not a generic shadow recipe");
        var remaining = RecipeInputs.consume(recipe, List.of(coal, alumina), fluids, 1);
        h.assertTrue(remaining != null && remaining.items().stream().allMatch(ItemStack::isEmpty)
                && remaining.fluids().stream().allMatch(FluidStack::isEmpty),
                "specialized input quantities are all consumed, not presence-only or retained catalysts: " + describe(recipe));
        h.assertTrue(RecipeInputs.consume(recipe, List.of(alumina), fluids, 1) == null,
                "specialized reaction requires its carbon input");
        h.assertTrue(RecipeInputs.consume(recipe, List.of(coal, alumina), List.of(), 1) == null,
                "specialized reaction requires its fluids");
        for (int absent = 0; absent < fluids.size(); absent++) {
            var missing = new ArrayList<>(fluids);
            missing.set(absent, FluidStack.EMPTY);
            h.assertTrue(RecipeInputs.consume(recipe, List.of(coal, alumina), missing, 1) == null,
                    "specialized reaction requires each separate fluid " + absent);
        }
        // Consumption plans must not modify the native recipe, or the supplied control stacks.
        h.assertTrue(coal.getCount() == 3 && alumina.getCount() == 10, "recipe planning preserves caller-owned control inputs");
        for (int i = 0; i < fluids.size(); i++) h.assertTrue(fluids.get(i).getAmount() == recipe.mFluidInputs[i].getAmount(),
                "recipe planning preserves supplied fluid quantity " + i);
        return recipe;
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void unflaggedAluminaAndSaltDoNotAutoDecompose(GameTestHelper h) {
        var findings = new ArrayList<String>();
        ItemStack selector = selectorZero(h);
        for (String material : List.of("Alumina", "Salt")) {
            ItemStack input = dust(h, material, 64);
            for (boolean selected : List.of(false, true)) {
                var inputs = selected ? List.of(input, selector) : List.of(input);
                Recipe electrolysis = lookup(h, MachineRecipeMaps.Electrolyzer, inputs, List.of());
                Recipe centrifuging = lookup(h, MachineRecipeMaps.Centrifuge, inputs, List.of());
                if (electrolysis != null) findings.add(material + " selector=" + selected + " Electrolyzer " + nativeRecipe(electrolysis));
                if (centrifuging != null) findings.add(material + " selector=" + selected + " Centrifuge " + nativeRecipe(centrifuging));
            }
            h.assertTrue(input.getCount() == 64 && selector.getCount() == 1, "lookup never consumes the diagnostic inputs");
        }
        h.assertTrue(findings.isEmpty(), "original dcmp-only materials have no standalone machine route: " + findings);
        h.succeed();
    }

    private static boolean expectedOutput(GameTestHelper h, Recipe recipe, String material) {
        if (recipe == null) return false;
        ItemStack expected = dust(h, material, 1);
        return Arrays.stream(recipe.mOutputs).anyMatch(s -> !s.isEmpty() && s.is(expected.getItem()));
    }

    private static int outputCount(GameTestHelper h, Recipe recipe, String material) {
        if (recipe == null) return 0;
        ItemStack expected = dust(h, material, 1);
        return Arrays.stream(recipe.mOutputs).filter(s -> !s.isEmpty() && s.is(expected.getItem()))
                .mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void decompositionMapsFollowExplicitMaterialProcessing(GameTestHelper h) {
        // Explicit elec factories remain valid. Cobalt hexahydrate's water output is covered by
        // decompositionUsesFluidInputsAmbientPhasesAndOriginalWork below.
        ItemStack selector = selectorZero(h);
        Recipe silverIodide = lookup(h, MachineRecipeMaps.Electrolyzer, List.of(dust(h, "AgI", 64), selector), List.of());
        Recipe vanadiumOxide = lookup(h, MachineRecipeMaps.Electrolyzer, List.of(dust(h, "V2O5", 64), selector), List.of());
        h.assertTrue(expectedOutput(h, silverIodide, "Silver") && expectedOutput(h, vanadiumOxide, "Vanadium"),
                "existing explicitly permitted electrolyzer positive controls remain registered");

        var bronze = dust(h, "Bronze", 64);
        var lazurite = dust(h, "Lazurite", 64);
        // GT6 feeds Bronze as 576 mB molten metal (divider 4U, 144 mB/U) and suppresses the dust row.
        FluidStack bronzeFluid = gregtech.content.recipe.generated.GeneratedRecipeResolver
                .materialFluid("Bronze", 4000);
        h.assertTrue(bronzeFluid != null && bronzeFluid.getAmount() == 576, "Bronze molten input exists");
        Recipe bronzeCentrifuge = lookup(h, MachineRecipeMaps.Centrifuge, List.of(), List.of(bronzeFluid));
        Recipe bronzeDustCentrifuge = lookup(h, MachineRecipeMaps.Centrifuge, List.of(bronze), List.of());
        Recipe bronzeElectrolyzer = lookup(h, MachineRecipeMaps.Electrolyzer, List.of(bronze, selector), List.of());
        Recipe bronzeBareElectrolyzer = lookup(h, MachineRecipeMaps.Electrolyzer, List.of(bronze), List.of());
        Recipe lazuriteElectrolyzer = lookup(h, MachineRecipeMaps.Electrolyzer, List.of(lazurite, selector), List.of());
        Recipe lazuriteBareElectrolyzer = lookup(h, MachineRecipeMaps.Electrolyzer, List.of(lazurite), List.of());
        Recipe lazuriteCentrifuge = lookup(h, MachineRecipeMaps.Centrifuge, List.of(lazurite), List.of());
        boolean bronzeCorrect = expectedOutput(h, bronzeCentrifuge, "Copper")
                && expectedOutput(h, bronzeCentrifuge, "Tin") && bronzeDustCentrifuge == null
                && bronzeElectrolyzer == null && bronzeBareElectrolyzer == null;
        boolean lazuriteCorrect = expectedOutput(h, lazuriteElectrolyzer, "Alumina")
                && expectedOutput(h, lazuriteElectrolyzer, "SiliconDioxide") && lazuriteCentrifuge == null
                && lazuriteBareElectrolyzer == null;
        boolean lazuriteBatch = lazuriteElectrolyzer != null && lazuriteElectrolyzer.mInputs.length == 2
                && Arrays.stream(lazuriteElectrolyzer.mInputs).anyMatch(s -> s.is(lazurite.getItem()) && s.getCount() == 14)
                && Arrays.stream(lazuriteElectrolyzer.mInputs).anyMatch(s -> s.is(selector.getItem()) && s.getCount() == 1)
                && lazuriteElectrolyzer.mOutputs.length == 4 && lazuriteElectrolyzer.mFluidOutputs.length == 0
                && outputCount(h, lazuriteElectrolyzer, "Alumina") == 3
                && outputCount(h, lazuriteElectrolyzer, "SiliconDioxide") == 3
                && outputCount(h, lazuriteElectrolyzer, "Calcium") == 4
                && outputCount(h, lazuriteElectrolyzer, "Sodium") == 4;
        h.assertTrue(bronze.getCount() == 64 && lazurite.getCount() == 64 && selector.getCount() == 1,
                "all route lookups preserve caller inputs");
        h.assertTrue(bronzeCorrect && lazuriteCorrect && lazuriteBatch,
                "processing flags, not elemental-component inference: Bronze Centrifuge=" + describe(bronzeCentrifuge)
                        + "; Bronze selected Electrolyzer=" + nativeRecipe(bronzeElectrolyzer)
                        + "; Bronze bare Electrolyzer=" + nativeRecipe(bronzeBareElectrolyzer)
                        + "; Lazurite selected Electrolyzer=" + nativeRecipe(lazuriteElectrolyzer)
                        + "; Lazurite bare Electrolyzer=" + nativeRecipe(lazuriteBareElectrolyzer)
                        + "; Lazurite Centrifuge=" + describe(lazuriteCentrifuge));
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void specializedAluminaStillConsumesCarbonAndBothFluids(GameTestHelper h) {
        specializedAlumina(h);
        h.succeed();
    }
    private static boolean hasFluidOutput(Recipe recipe, String nameSuffix, int amount) {
        return recipe != null && Arrays.stream(recipe.mFluidOutputs).anyMatch(
                f -> !f.isEmpty() && fluidName(f).endsWith(nameSuffix) && f.getAmount() == amount);
    }

    private static Recipe recipeWithFluid(RecipeMap map, String fluidSuffix, int amount) {
        return map.mRecipeList.stream()
                .filter(r -> r.mEnabled && !r.mFakeRecipe)
                .filter(r -> Arrays.stream(r.mFluidInputs).anyMatch(
                        f -> !f.isEmpty() && fluidName(f).contains(fluidSuffix) && f.getAmount() == amount))
                .findFirst().orElse(null);
    }

    private static String saltwaterDiagnostic() {
        return MachineRecipeMaps.Electrolyzer.mRecipeList.stream()
                .filter(r -> r.mEnabled && !r.mFakeRecipe)
                .filter(r -> Arrays.stream(r.mFluidInputs).anyMatch(
                        f -> !f.isEmpty() && fluidName(f).contains("saltwater")))
                .map(MaterialDecompositionPolicyTests::nativeRecipe).toList().toString();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void decompositionUsesFluidInputsAmbientPhasesAndOriginalWork(GameTestHelper h) {
        ItemStack selector = selectorZero(h);

        // Liquid input: Saltwater divider 4 -> 4000 mB in, ambient water 3000 mB and salt dust out.
        FluidStack saltwater = gregtech.content.recipe.generated.GeneratedRecipeResolver
                .materialFluid("Saltwater", 4000);
        h.assertTrue(saltwater != null && saltwater.getAmount() == 4000, "Saltwater liquid input exists");
        // The map stores recipes in a HashSet, so lookup order is not registration order; locate
        // the generic 4000 mB row directly instead of relying on which saltwater row wins.
        Recipe saltwaterRecipe = recipeWithFluid(MachineRecipeMaps.Electrolyzer, "saltwater", 4000);
        h.assertTrue(saltwaterRecipe != null && saltwaterRecipe.mEUt == 56 && saltwaterRecipe.mDuration == 1168,
                "Saltwater fluid electrolysis keeps original work: supplied=" + nativeFluids(new FluidStack[]{saltwater})
                        + "; selected=" + nativeRecipe(saltwaterRecipe) + "; rows=" + saltwaterDiagnostic());
        h.assertTrue(hasFluidOutput(saltwaterRecipe, "water", 3000) && expectedOutput(h, saltwaterRecipe, "Salt"),
                "Saltwater outputs ambient water fluid and salt dust: " + nativeRecipe(saltwaterRecipe));

        // Gas input: Nitrogen Monoxide divider 2 -> 2000 mB in, both components are ambient gases.
        FluidStack nitrogenMonoxide = gregtech.content.recipe.generated.GeneratedRecipeResolver
                .materialFluid("NitrogenMonoxide", 2000);
        h.assertTrue(nitrogenMonoxide != null && nitrogenMonoxide.getAmount() == 2000,
                "Nitrogen Monoxide gas input exists");
        Recipe noRecipe = lookup(h, MachineRecipeMaps.Electrolyzer, List.of(selector), List.of(nitrogenMonoxide));
        h.assertTrue(noRecipe != null && noRecipe.mFluidInputs.length == 1 && noRecipe.mFluidInputs[0].getAmount() == 80
                        && hasFluidOutput(noRecipe, "nitrogen", 40) && hasFluidOutput(noRecipe, "oxygen", 40),
                "NO gas electrolysis is normalized and emits ambient gases: " + nativeRecipe(noRecipe));

        // Vanilla water output: Cobalt hexahydrate divider 7 -> cobalt dust and 6000 mB water.
        ItemStack cobaltHydrate = dust(h, "CobaltHexahydrate", 7);
        Recipe cobaltRecipe = lookup(h, MachineRecipeMaps.Centrifuge, List.of(cobaltHydrate), List.of());
        h.assertTrue(cobaltRecipe != null && expectedOutput(h, cobaltRecipe, "Cobalt")
                        && hasFluidOutput(cobaltRecipe, "water", 6000),
                "vanilla water is a resolved ambient output: " + nativeRecipe(cobaltRecipe));

        // Work parameters: original EU = max(16, total*14/U), duration = ceil(total*292/U).
        FluidStack bronzeFluid = gregtech.content.recipe.generated.GeneratedRecipeResolver
                .materialFluid("Bronze", 4000);
        Recipe bronze = lookup(h, MachineRecipeMaps.Centrifuge, List.of(), List.of(bronzeFluid));
        h.assertTrue(bronze != null && bronze.mEUt == 56 && bronze.mDuration == 1168,
                "Bronze keeps original EU and duration: " + nativeRecipe(bronze));
        Recipe lazurite = lookup(h, MachineRecipeMaps.Electrolyzer, List.of(dust(h, "Lazurite", 64), selector), List.of());
        h.assertTrue(lazurite != null && lazurite.mEUt == 392 && lazurite.mDuration == 4088,
                "Lazurite keeps original EU and normalized duration: " + nativeRecipe(lazurite));
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void genericElectrolysisRequiresAndRetainsSelectorZero(GameTestHelper h) {
        ItemStack coal = dust(h, "Coal", 1);
        ItemStack carbon = dust(h, "Carbon", 1);
        ItemStack selector = gregtech.content.recipe.generated.GeneratedRecipeResolver.resolveSpec("tag:0");
        ItemStack wrongSelector = gregtech.content.recipe.generated.GeneratedRecipeResolver.resolveSpec("tag:1");
        h.assertTrue(selector != null && !selector.isEmpty() && selector.getCount() == 1
                && wrongSelector != null && !wrongSelector.isEmpty() && wrongSelector.getCount() == 1
                && selector.getItem() != wrongSelector.getItem(), "actual distinct selector zero and one items exist");
        Recipe bare = lookup(h, MachineRecipeMaps.Electrolyzer, List.of(coal), List.of());
        Recipe selected = lookup(h, MachineRecipeMaps.Electrolyzer, List.of(coal, selector), List.of());
        Recipe wrong = lookup(h, MachineRecipeMaps.Electrolyzer, List.of(coal, wrongSelector), List.of());
        String collision = specializedDiagnostic(dust(h, "Coal", 3), dust(h, "Alumina", 10), dust(h, "Aluminium", 1));
        h.assertTrue(selected != null && Arrays.stream(selected.mOutputs).anyMatch(
                s -> s.is(carbon.getItem()) && s.getCount() == 1), "permitted Coal1 to Carbon1 positive route exists");
        var remaining = RecipeInputs.consume(selected, List.of(coal, selector), List.of(), 1);
        h.assertTrue(remaining != null && remaining.items().size() == 2 && remaining.items().get(0).isEmpty()
                && remaining.items().get(1).is(selector.getItem()) && remaining.items().get(1).getCount() == 1
                && remaining.fluids().isEmpty(), "positive decomposition consumes coal and retains selector exactly");
        h.assertTrue(coal.getCount() == 1 && selector.getCount() == 1 && wrongSelector.getCount() == 1,
                "all lookups and consumption plans preserve caller-owned control stacks");
        boolean selectedHasGate = Arrays.stream(selected.mInputs).anyMatch(s -> s.is(selector.getItem()));
        h.assertTrue(bare == null && wrong == null && selectedHasGate,
                "original generic Electrolyzer selector zero gate: bare={" + nativeRecipe(bare)
                        + "}; selected={" + nativeRecipe(selected) + "}; wrong={" + nativeRecipe(wrong)
                        + "}; specialized collision diagnostic=" + collision);
        h.succeed();
    }

}
