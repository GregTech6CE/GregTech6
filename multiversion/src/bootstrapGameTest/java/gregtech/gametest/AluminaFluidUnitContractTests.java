package gregtech.gametest;

import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.recipe.Recipe;
import gregtech.internal.recipe.RecipeInputs;
import gregtech.content.recipe.generated.GeneratedRecipeResolver;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Scaled startup recipes and additive token compatibility; no recipe-map mutation. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class AluminaFluidUnitContractTests {
    private record Scale(MaterialPrefix coalForm, int coalCount, int aluminaCount, int aluminiumCount,
                         int cryolite, int aluminiumFluoride, int carbonDioxide, int fluorine, int duration) {}
    private record Coefficients(String form, int cryolite, int aluminiumFluoride, int fluorine,
                                boolean exactConsumption, boolean correct) {}
    private record TokenCase(String token, String field, String nativeId, int amount) {}

    private static ItemStack item(GameTestHelper h, MaterialPrefix form, String name, int count) {
        var result = GTItems.getStack(form, GTMaterialRegistry.get(name).resolve(), count);
        h.assertTrue(!result.isEmpty() && result.getCount() == count, "actual item: " + form.getName() + ":" + name);
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

    private static String describe(FluidStack stack) {
        return stack == null ? "null" : BuiltInRegistries.FLUID.getKey(stack.getFluid()) + "=" + stack.getAmount();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void scaledAluminaRoutesUseExactSourceUnits(GameTestHelper h) {
        // blockDust has the existing port fallback to nine ordinary dusts. Do not change its identity.
        var scales = List.of(
                new Scale(MaterialPrefix.dust, 9, 30, 12, 6, 12, 27000, 87, 6144),
                new Scale(MaterialPrefix.dustTiny, 27, 10, 4, 2, 4, 9000, 29, 2048),
                new Scale(MaterialPrefix.dustSmall, 6, 5, 2, 1, 2, 4500, 14, 1024));
        List<Coefficients> observations = new ArrayList<>();
        for (Scale scale : scales) {
            var coal = item(h, scale.coalForm(), "Coal", scale.coalCount());
            var alumina = item(h, MaterialPrefix.dust, "Alumina", scale.aluminaCount());
            var aluminium = item(h, MaterialPrefix.dust, "Aluminium", scale.aluminiumCount());
            var cryolite = fluid(h, "GenLiquid_Cryolite", scale.cryolite());
            var aluminiumFluoride = fluid(h, "GenLiquid_AluminiumFluoride", scale.aluminiumFluoride());
            var carbonDioxide = fluid(h, "GenGas_CarbonDioxide", scale.carbonDioxide());
            var fluorine = fluid(h, "GenGas_Fluorine", scale.fluorine());
            var candidates = MachineRecipeMaps.Electrolyzer.mRecipeList.stream()
                    .filter(r -> r.mEnabled && !r.mFakeRecipe && r.mInputs.length == 2 && r.mFluidInputs.length == 2)
                    .filter(r -> Arrays.stream(r.mInputs).anyMatch(s -> s.is(coal.getItem()) && s.getCount() == coal.getCount()))
                    .filter(r -> Arrays.stream(r.mInputs).anyMatch(s -> s.is(alumina.getItem()) && s.getCount() == alumina.getCount()))
                    .filter(r -> quantity(r.mFluidInputs, cryolite) > 0 && quantity(r.mFluidInputs, aluminiumFluoride) > 0)
                    .toList();
            h.assertTrue(candidates.size() == 1, "one actual registered scaled Alumina row: " + scale);
            Recipe recipe = candidates.get(0);
            var items = List.of(coal, alumina);
            var actualFluids = Arrays.stream(recipe.mFluidInputs).map(FluidStack::copy).toList();
            var actualRemaining = RecipeInputs.consume(recipe, items, actualFluids, 1);
            h.assertTrue(actualRemaining != null && actualRemaining.items().stream().allMatch(ItemStack::isEmpty)
                    && actualRemaining.fluids().stream().allMatch(FluidStack::isEmpty),
                    "scaled row consumes its listed inputs: " + scale);
            h.assertTrue(recipe.mEUt == 16 && recipe.mDuration == scale.duration() && recipe.mOutputs.length == 1
                    && recipe.mOutputs[0].is(aluminium.getItem()) && recipe.mOutputs[0].getCount() == scale.aluminiumCount()
                    && recipe.mFluidOutputs.length == 2 && quantity(recipe.mFluidOutputs, carbonDioxide) == scale.carbonDioxide(),
                    "scaled item, CO2, and work controls: " + scale);

            int actualCryolite = quantity(recipe.mFluidInputs, cryolite);
            int actualAluminiumFluoride = quantity(recipe.mFluidInputs, aluminiumFluoride);
            int actualFluorine = quantity(recipe.mFluidOutputs, fluorine);
            // Larger stocks can legitimately select the ordinary smaller batch; direct consumption
            // checks this registered scaled row without imposing recipe-map priority.
            var sourceRemaining = RecipeInputs.consume(recipe, items, List.of(cryolite, aluminiumFluoride), 1);
            boolean exactConsumption = sourceRemaining != null
                    && sourceRemaining.items().stream().allMatch(ItemStack::isEmpty)
                    && sourceRemaining.fluids().stream().allMatch(FluidStack::isEmpty);
            h.assertTrue(coal.getCount() == scale.coalCount() && alumina.getCount() == scale.aluminaCount()
                    && cryolite.getAmount() == scale.cryolite() && aluminiumFluoride.getAmount() == scale.aluminiumFluoride(),
                    "scaled consumption plans preserve caller-owned inputs");
            for (int i = 0; i < actualFluids.size(); i++) h.assertTrue(
                    actualFluids.get(i).getAmount() == recipe.mFluidInputs[i].getAmount(), "scaled control preserves fluid " + i);
            observations.add(new Coefficients(scale.coalForm().getName() + ":" + scale.coalCount(),
                    actualCryolite, actualAluminiumFluoride, actualFluorine, exactConsumption,
                    actualCryolite == scale.cryolite() && actualAluminiumFluoride == scale.aluminiumFluoride()
                            && actualFluorine == scale.fluorine() && exactConsumption));
        }
        h.assertTrue(observations.stream().allMatch(Coefficients::correct),
                "original scaled liquid round-up and gas round-down: " + observations);
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void sourceUnitTokensKeepNativeIdentityAndLegacySemantics(GameTestHelper h) {
        var cases = List.of(
                new TokenCase("mu:Na3AlF6:liquid:9009000:up", "GenLiquid_Cryolite", "gt6ce:cryolite", 2),
                new TokenCase("mu:Cryolite:liquid:9009000:up", "GenLiquid_Cryolite", "gt6ce:cryolite", 2),
                new TokenCase("mu:AlF3:liquid:54054000:up", "GenLiquid_AluminiumFluoride", "gt6ce:aluminiumfluoride", 12),
                new TokenCase("mu:CO2:gas:17513496000:down", "GenGas_CarbonDioxide", "gt6ce:carbondioxide", 27000),
                new TokenCase("mu:F:gas:56756700:down", "GenGas_Fluorine", "gt6ce:fluorine", 87),
                new TokenCase("mu:Na3AlF6:liquid:1:up", "GenLiquid_Cryolite", "gt6ce:cryolite", 1),
                new TokenCase("mu:Na3AlF6:liquid:9009000:down", "GenLiquid_Cryolite", "gt6ce:cryolite", 2),
                // These are existing token contracts, not a claim that every old producer has original units.
                new TokenCase("m:Na3AlF6:14", "GenLiquid_Cryolite", "gt6ce:cryolite", 14),
                new TokenCase("m:AlF3:28", "GenLiquid_AluminiumFluoride", "gt6ce:aluminiumfluoride", 28),
                new TokenCase("m:F:28", "GenGas_Fluorine", "gt6ce:fluorine", 28),
                new TokenCase("f:CarbonDioxide:123", "GenGas_CarbonDioxide", "gt6ce:carbondioxide", 123),
                new TokenCase("fr:cryolite:456", "GenLiquid_Cryolite", "gt6ce:cryolite", 456));
        List<String> observations = new ArrayList<>();
        boolean allMatch = true;
        for (TokenCase test : cases) {
            var expected = fluid(h, test.field(), test.amount());
            var actual = GeneratedRecipeResolver.resolveFluidSpec(test.token());
            boolean matches = actual != null && !actual.isEmpty() && actual.getFluid() == expected.getFluid()
                    && actual.getAmount() == test.amount()
                    && test.nativeId().equals(BuiltInRegistries.FLUID.getKey(actual.getFluid()).toString());
            observations.add(test.token() + " -> " + describe(actual) + "; correct=" + matches);
            allMatch &= matches;
        }
        var water = GeneratedRecipeResolver.resolveFluidSpec("w:321");
        boolean waterMatches = water != null && water.getFluid() == Fluids.WATER && water.getAmount() == 321;
        observations.add("w:321 -> " + describe(water) + "; correct=" + waterMatches);
        allMatch &= waterMatches;
        // Reject unsupported phase/binding, nonintegral or out-of-range source amounts, malformed
        // tokens, and zero native volume. Do not coerce a tiny rounded-down amount into a catalyst.
        var invalid = List.of("mu:Na3AlF6:gas:9009000:up", "mu:Na3AlF6:fluid:9009000:up",
                "mu:Salt:liquid:648648000:up", "mu:Alumina:liquid:648648000:up",
                "mu:MissingAluminaUnitMaterial:liquid:648648000:up", "mu:Na3AlF6:liquid:0:up",
                "mu:Na3AlF6:liquid:1:down", "mu:Na3AlF6:liquid:-1:up", "mu:Na3AlF6:liquid:1.0:up",
                "mu:Na3AlF6:liquid:9223372036854775808:up", "mu:CO2:gas:9223372036854775807:up",
                "mu:Na3AlF6:liquid:9009000:UP", "mu:Na3AlF6:liquid:9009000:nearest",
                "mu:Na3AlF6:liquid:9009000", "mu:Na3AlF6:liquid::up", "mu:Na3AlF6:liquid:9009000:up:");
        for (String token : invalid) {
            var actual = GeneratedRecipeResolver.resolveFluidSpec(token);
            observations.add(token + " -> " + describe(actual) + "; rejected=" + (actual == null));
            allMatch &= actual == null;
        }
        h.assertTrue(allMatch, "additive source-unit identity, arithmetic, rejection and legacy compatibility: " + observations);
        h.succeed();
    }
}
