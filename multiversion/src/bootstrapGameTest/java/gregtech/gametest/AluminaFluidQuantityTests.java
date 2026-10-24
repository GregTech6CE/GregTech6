package gregtech.gametest;

import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.recipe.Recipe;
import gregtech.internal.recipe.RecipeInputs;
import gregtech.data.MachineRecipeMaps;
import gregtech.data.MaterialPrefix;
import gregtech.registry.GTFluids;
import gregtech.registry.GTItems;
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

/** Actual startup coefficients, separate from processing-policy and physical-property contracts. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class AluminaFluidQuantityTests {
    private static ItemStack dust(GameTestHelper h, String name, int count) {
        var result = GTItems.getStack(MaterialPrefix.dust, GTMaterialRegistry.get(name).resolve(), count);
        h.assertTrue(!result.isEmpty() && result.getCount() == count, "actual dust: " + name);
        return result;
    }

    private static FluidStack fluid(GameTestHelper h, String key, int amount) {
        var result = GTFluids.stack(key, amount);
        h.assertTrue(!result.isEmpty() && result.getAmount() == amount, "actual fluid: " + key);
        return result;
    }

    private static int quantity(FluidStack[] stacks, FluidStack target) {
        return Arrays.stream(stacks).filter(s -> s.getFluid() == target.getFluid()).mapToInt(FluidStack::getAmount).sum();
    }

    private static String fluids(FluidStack[] stacks) {
        return Arrays.stream(stacks).map(s -> BuiltInRegistries.FLUID.getKey(s.getFluid()) + "=" + s.getAmount()).toList().toString();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void specializedAluminaUsesOriginalFluidCoefficients(GameTestHelper h) {
        var coal = dust(h, "Coal", 3);
        var alumina = dust(h, "Alumina", 10);
        var aluminium = dust(h, "Aluminium", 4);
        // Keep existing registered identities. Source unit correction must not rename saved fluids.
        var cryolite = fluid(h, "GenLiquid_Cryolite", 2);
        var aluminiumFluoride = fluid(h, "GenLiquid_AluminiumFluoride", 4);
        var carbonDioxide = fluid(h, "GenGas_CarbonDioxide", 9000);
        var fluorine = fluid(h, "GenGas_Fluorine", 29);
        var candidates = MachineRecipeMaps.Electrolyzer.mRecipeList.stream()
                .filter(r -> r.mEnabled && !r.mFakeRecipe && r.mInputs.length == 2 && r.mFluidInputs.length == 2)
                .filter(r -> Arrays.stream(r.mInputs).anyMatch(s -> s.is(coal.getItem()) && s.getCount() == 3))
                .filter(r -> Arrays.stream(r.mInputs).anyMatch(s -> s.is(alumina.getItem()) && s.getCount() == 10))
                .filter(r -> quantity(r.mFluidInputs, cryolite) > 0 && quantity(r.mFluidInputs, aluminiumFluoride) > 0)
                .toList();
        h.assertTrue(candidates.size() == 1, "processing-policy positive: one actual specialized Coal3 + Alumina10 recipe");
        Recipe recipe = candidates.get(0);
        var actualFluids = Arrays.stream(recipe.mFluidInputs).map(FluidStack::copy).toList();
        var items = List.of(coal, alumina);
        h.assertTrue(MachineRecipeMaps.Electrolyzer.findRecipe(items, actualFluids, false, 6, 6,
                h.getLevel(), null, ItemStack.EMPTY) == recipe, "actual full-input machine lookup selects specialized recipe");
        var actualRemaining = RecipeInputs.consume(recipe, items, actualFluids, 1);
        h.assertTrue(actualRemaining != null && actualRemaining.items().stream().allMatch(ItemStack::isEmpty)
                && actualRemaining.fluids().stream().allMatch(FluidStack::isEmpty), "control consumes its actual listed quantities");
        h.assertTrue(recipe.mEUt == 16 && recipe.mDuration == 2048 && recipe.mOutputs.length == 1
                && recipe.mOutputs[0].is(aluminium.getItem()) && recipe.mOutputs[0].getCount() == 4,
                "unrelated item output and work parameters remain original");
        h.assertTrue(quantity(recipe.mFluidOutputs, carbonDioxide) == 9000 && recipe.mFluidOutputs.length == 2,
                "unrelated carbon dioxide output and fluid arity remain original");

        // Capture every observed coefficient before the final failing assertion.
        int actualCryolite = quantity(recipe.mFluidInputs, cryolite);
        int actualAluminiumFluoride = quantity(recipe.mFluidInputs, aluminiumFluoride);
        int actualFluorine = quantity(recipe.mFluidOutputs, fluorine);
        var sourceFluids = List.of(cryolite, aluminiumFluoride);
        Recipe sourceLookup = MachineRecipeMaps.Electrolyzer.findRecipe(items, sourceFluids, false, 6, 6,
                h.getLevel(), null, ItemStack.EMPTY);
        var sourceRemaining = RecipeInputs.consume(recipe, items, sourceFluids, 1);
        boolean sourceConsumesExactly = sourceRemaining != null
                && sourceRemaining.items().stream().allMatch(ItemStack::isEmpty)
                && sourceRemaining.fluids().stream().allMatch(FluidStack::isEmpty);
        h.assertTrue(coal.getCount() == 3 && alumina.getCount() == 10 && cryolite.getAmount() == 2
                && aluminiumFluoride.getAmount() == 4, "lookups and consumption plans preserve caller-owned inputs");
        for (int i = 0; i < actualFluids.size(); i++) h.assertTrue(
                actualFluids.get(i).getAmount() == recipe.mFluidInputs[i].getAmount(), "control plans preserve fluid " + i);
        h.assertTrue(actualCryolite == 2 && actualAluminiumFluoride == 4 && actualFluorine == 29
                && sourceLookup == recipe && sourceConsumesExactly,
                "original U72/U36 liquid round-up and 7*U240 gas round-down: inputs=" + fluids(recipe.mFluidInputs)
                        + "; outputs=" + fluids(recipe.mFluidOutputs) + "; sourceLookup=" + (sourceLookup == recipe)
                        + "; sourceConsumesExactly=" + sourceConsumesExactly);
        h.succeed();
    }
}
