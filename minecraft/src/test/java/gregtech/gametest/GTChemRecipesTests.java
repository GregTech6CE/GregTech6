package gregtech.gametest;

import static gregtech.engine.recipe.chem.FluidPhase.GAS;
import static gregtech.engine.recipe.chem.FluidPhase.LIQUID;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.generated.ChemRecipeResolver;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.recipe.chem.ChemOutcome;
import gregtech.engine.recipe.chem.MaterialFluidBinding;
import java.util.List;
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

/** The source chemistry family's exact amounts, forms and live lookup. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class GTChemRecipesTests {
    private static final String BATCH = "recipe_map_registration";

    private GTChemRecipesTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void cryoliteTwentyUnitsResolveTo2880(GameTestHelper helper) {
        ItemStack sodiumHydroxide = stack(MaterialPrefix.dust, "NaOH", 18);
        ItemStack alumina = stack(MaterialPrefix.dust, "Al2O3", 5);
        FluidStack hydrogenFluoride = materialFluid("HydrogenFluoride", GAS, 24000);
        helper.assertFalse(hydrogenFluoride.isEmpty(), "HF fluid");
        Recipe row = find(MachineRecipeMaps.Mixer, List.of(sodiumHydroxide, alumina), List.of(hydrogenFluoride));
        helper.assertTrue(row != null, "cryolite mixer row is queryable");
        helper.assertTrue(hasFluidOutput(row, materialFluid("Cryolite", LIQUID, 2880)),
                "cryolite output is 2880 mB, not 20000");
        helper.assertFalse(hasFluidOutput(row, materialFluid("Cryolite", LIQUID, 20000)),
                "the old 20000 mB output is gone");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void blazeKeepsExactNinthOfAUnit(GameTestHelper helper) {
        ItemStack tiny = new ItemStack(net.minecraft.world.item.Items.BLAZE_POWDER);
        FluidStack hydrogen = materialFluid("Hydrogen", GAS, 2000);
        helper.assertFalse(hydrogen.isEmpty(), "hydrogen fluid");
        Recipe row = find(MachineRecipeMaps.HeatMixer, List.of(tiny), List.of(hydrogen));
        helper.assertTrue(row != null, "blaze heat-mixer row is queryable");
        helper.assertTrue(hasItemInput(row, tiny, 1), "blaze is one blaze powder (1/9 U)");
        helper.assertTrue(hasFluidOutput(row, materialFluid("HydrosulfuricAcid", GAS, 3000)),
                "H2S output stays 3000 mB; row " + describe(row));
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void aluminiumFluorideUsesOneUnitRule(GameTestHelper helper) {
        ItemStack alumina = stack(MaterialPrefix.dust, "Al2O3", 5);
        FluidStack hexafluorosilicic = materialFluid("HexafluorosilicicAcid", LIQUID, 9000);
        helper.assertFalse(hexafluorosilicic.isEmpty(), "H2SiF6 fluid");
        Recipe heatMixer = find(MachineRecipeMaps.HeatMixer, List.of(alumina), List.of(hexafluorosilicic));
        helper.assertTrue(heatMixer != null, "AlF3 production row is queryable");
        helper.assertTrue(hasFluidOutput(heatMixer, materialFluid("AluminiumFluoride", LIQUID, 1152)),
                "8 U AlF3 is 1152 mB");

        GTMaterial coal = material("Coal");
        helper.assertTrue(coal != null, "coal material");
        ItemStack coalBlock = GTBlocks.getStack(BlockMaterialPrefix.blockDust, coal);
        ItemStack alumina30 = stack(MaterialPrefix.dust, "Al2O3", 30);
        FluidStack cryolite = materialFluid("Cryolite", LIQUID, 6);
        FluidStack aluminiumFluoride = materialFluid("AluminiumFluoride", LIQUID, 12);
        helper.assertFalse(coalBlock.isEmpty(), "coal blockDust");
        helper.assertFalse(cryolite.isEmpty(), "cryolite fluid");
        helper.assertFalse(aluminiumFluoride.isEmpty(), "AlF3 fluid");
        Recipe electrolyzer = find(MachineRecipeMaps.Electrolyzer, List.of(coalBlock, alumina30),
                List.of(cryolite, aluminiumFluoride));
        helper.assertTrue(electrolyzer != null, "aluminium electrolysis row is queryable; requested "
                + requested(coalBlock, alumina30, cryolite, aluminiumFluoride) + "; candidates "
                + candidates(MachineRecipeMaps.Electrolyzer, coalBlock, alumina30));
        helper.assertTrue(hasFluidInput(electrolyzer, materialFluid("Cryolite", LIQUID, 6)),
                "electrolysis consumes 1/24 U cryolite = 6 mB");
        helper.assertTrue(hasFluidInput(electrolyzer, materialFluid("AluminiumFluoride", LIQUID, 12)),
                "electrolysis consumes 1/12 U AlF3 = 12 mB with the same 144 mB/U rule");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void chemistryLedgerPublishesSourceRows(GameTestHelper helper) {
        helper.assertTrue(ChemRecipeResolver.count(ChemOutcome.PUBLISHED)
                        + ChemRecipeResolver.count(ChemOutcome.SUBSTITUTED) > 250,
                "the chemistry family publishes its source rows");
        helper.assertTrue(ChemRecipeResolver.count(ChemOutcome.VALIDATION_FAILED) == 0,
                "no chemistry row failed validation");
        boolean cryolite = false, blaze = false, alf3 = false;
        for (var result : ChemRecipeResolver.results()) {
            if (result.outcome() != ChemOutcome.PUBLISHED && result.outcome() != ChemOutcome.SUBSTITUTED) continue;
            if (result.source().endsWith(":103") || result.source().endsWith(":104")
                    || result.source().endsWith(":105") || result.source().endsWith(":106")) cryolite = true;
            if (result.source().endsWith(":127")) blaze = true;
            if (result.source().endsWith(":101") || result.source().endsWith(":102")) alf3 = true;
        }
        helper.assertTrue(cryolite, "cryolite source rows published");
        helper.assertTrue(blaze, "blaze source row published");
        helper.assertTrue(alf3, "AlF3 source rows published");
        helper.succeed();
    }

    private static Recipe find(RecipeMap map, List<ItemStack> items, List<FluidStack> fluids) {
        return map.findRecipe(items, fluids, false, map.mInputItemsCount, map.mOutputItemsCount);
    }

    private static String requested(ItemStack first, ItemStack second, FluidStack third, FluidStack fourth) {
        return describeStacks(first, second) + " fluids=" + describeFluids(third, fourth);
    }

    private static String describeStacks(ItemStack... stacks) {
        StringBuilder out = new StringBuilder("items=");
        for (ItemStack stack : stacks)
            out.append(stack.getCount()).append("x").append(stack.getItem())
                    .append(stack.getItem() instanceof net.minecraft.world.item.BlockItem ? "(block)" : "").append(' ');
        return out.toString();
    }

    private static String describeFluids(FluidStack... stacks) {
        StringBuilder out = new StringBuilder();
        for (FluidStack stack : stacks)
            out.append(stack.getAmount()).append("mB/").append(stack.getFluid()).append(' ');
        return out.toString();
    }

    private static String candidates(RecipeMap map, ItemStack first, ItemStack second) {
        StringBuilder out = new StringBuilder();
        for (Recipe row : map.mRecipeList) {
            if (row.mInputs.length < 2) continue;
            if (!row.mInputs[0].is(first.getItem()) && !row.mInputs[1].is(second.getItem())) continue;
            if (out.length() > 0) out.append(" | ");
            out.append(describe(row));
        }
        return out.toString();
    }

    private static String describe(Recipe row) {
        StringBuilder out = new StringBuilder("in=");
        for (ItemStack stack : row.mInputs) if (!stack.isEmpty())
            out.append(stack.getCount()).append("x").append(stack.getItem()).append(' ');
        out.append("fluids=");
        for (FluidStack stack : row.mFluidInputs) if (!stack.isEmpty())
            out.append(stack.getAmount()).append("mB/").append(stack.getFluid()).append(' ');
        out.append("out=");
        for (FluidStack stack : row.mFluidOutputs) if (!stack.isEmpty())
            out.append(stack.getAmount()).append("mB/").append(stack.getFluid()).append(' ');
        out.append("items=");
        for (ItemStack stack : row.mOutputs) if (!stack.isEmpty())
            out.append(stack.getCount()).append("x").append(stack.getItem()).append(' ');
        return out.toString();
    }

    private static boolean hasItemInput(Recipe recipe, ItemStack expected, int count) {
        for (ItemStack input : recipe.mInputs)
            if (input.is(expected.getItem()) && input.getCount() == count) return true;
        return false;
    }

    private static boolean hasFluidInput(Recipe recipe, FluidStack expected) {
        for (FluidStack input : recipe.mFluidInputs)
            if (!input.isEmpty() && input.isFluidEqual(expected) && input.getAmount() == expected.getAmount()) return true;
        return false;
    }

    private static boolean hasFluidOutput(Recipe recipe, FluidStack expected) {
        for (FluidStack output : recipe.mFluidOutputs)
            if (!output.isEmpty() && output.isFluidEqual(expected) && output.getAmount() == expected.getAmount()) return true;
        return false;
    }

    private static ItemStack stack(MaterialPrefix prefix, String material, int count) {
        GTMaterial resolved = material(material);
        return resolved == null ? ItemStack.EMPTY : GTItems.getStack(prefix, resolved, count);
    }

    private static GTMaterial material(String name) {
        GTMaterial material = GTMaterialRegistry.get(name);
        return material == null || !material.resolve().isValid() ? null : material.resolve();
    }

    private static FluidStack materialFluid(String material, gregtech.engine.recipe.chem.FluidPhase phase, int amount) {
        var binding = MaterialFluidBinding.find(material, phase).orElse(null);
        if (binding == null) return FluidStack.EMPTY;
        FluidStack stack = GTFluids.stack(binding.field(), amount);
        return stack == null ? FluidStack.EMPTY : stack;
    }
}
