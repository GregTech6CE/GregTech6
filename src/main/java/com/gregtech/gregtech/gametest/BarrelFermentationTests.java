package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.content.recipe.FermenterFoodRecipes;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTTanks;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.TreeSet;

/**
 * Guards GT6's sealed-barrel fermentation ({@code TileEntityBase08Barrel#onTick2:188-207}) and the
 * food fermentation chain it consumes ({@code Loader_Recipes_Food:605-649}).
 * <p>
 * Regressions these tests exist for: the soft-hammer "sealed" state of every barrel did nothing at
 * all, and the Fermenter only knew the two chemical {@code Biomass → Methane} recipes — so neither
 * the machine nor the barrel had a working food chain.
 * </p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class BarrelFermentationTests {

    /** Places a metal drum (GT6: sealable, not restricted to "simple" fluids like wood barrels). */
    private static TankBlockEntity drum(GameTestHelper helper) {
        var block = GTTanks.DRUM_STEEL.get();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        TankBlockEntity be = (TankBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(be != null, "the drum has a block entity");
        return be;
    }

    private static void fill(TankBlockEntity be, String fluid, int mb) {
        FluidStack stack = GTFluids.stack(fluid, mb);
        be.getFluidTank().fill(stack, IFluidHandler.FluidAction.EXECUTE);
    }

    private static void tick(GameTestHelper helper, TankBlockEntity be, int ticks) {
        for (int i = 0; i < ticks; i++) {
            TankBlockEntity.serverTick(helper.getLevel(), be.getBlockPos(), be.getBlockState(), be);
        }
    }

    /** A sealed drum turns its juice into cider over exactly the duration GT6 computes. */
    @GameTest(template = "test_empty")
    public static void sealedBarrelFermentsItsContents(GameTestHelper helper) {
        TankBlockEntity be = drum(helper);
        fill(be, "Juice_Apple", 50);
        be.toggleSoftHammerState(); // soft hammer → sealed
        helper.assertTrue(be.getSoftHammerState(), "the drum is sealed");

        Recipe recipe = TankBlockEntity.findFermentationRecipe(be.getFluidTank().getFluid());
        helper.assertTrue(recipe != null, "juice ferments into cider");
        helper.assertTrue(recipe.mInputs.length == 1
                        && recipe.mInputs[0].is(GTTechnological.selectorTag(0)),
                "GT6 barrel supplies only the 0-number selector circuit, not a flower");
        // GT6: ceil(|EU/t * duration| * max(1, tank) / max(1, recipeInput)) = 16*64*50/50 = 1024.
        long expected = TankBlockEntity.sealedDuration(recipe, 50);
        helper.assertTrue(expected == 1024, "GT6 sealed duration for 50 L is 1024, got " + expected);

        tick(helper, be, (int) expected - 1);
        helper.assertTrue(be.getFluidTank().getFluid().getFluid() == GTFluids.stack("Juice_Apple", 1).getFluid(),
                "still juice one tick before completion");
        helper.assertTrue(be.getSealedTime() == expected - 1,
                "progress is counted, got " + be.getSealedTime());

        // GT6 counts up to mMaxSealedTime and converts on the following tick.
        tick(helper, be, 1);
        helper.assertTrue(be.getFluidTank().getFluid().getFluid() == GTFluids.stack("Juice_Apple", 1).getFluid()
                        && be.getSealedTime() == expected,
                "the seal is full after " + expected + " ticks but not yet converted");

        tick(helper, be, 1);
        FluidStack result = be.getFluidTank().getFluid();
        helper.assertTrue(result.getFluid() == GTFluids.stack("Cider_Apple", 1).getFluid(),
                "the whole drum is cider after the last tick, got " + result.getFluid());
        helper.assertTrue(result.getAmount() == 25,
                "GT6 scales the output by the amount ratio (50 → 25), got " + result.getAmount());
        helper.assertTrue(be.getSealedTime() == 0 && be.getMaxSealedTime() == 0,
                "the seal progress resets after a finished fermentation");
        helper.succeed();
    }

    /** GT6 matches on the fluid only and scales by the amount, so a partial drum still ferments. */
    @GameTest(template = "test_empty")
    public static void fermentationScalesWithTheFillLevel(GameTestHelper helper) {
        TankBlockEntity be = drum(helper);
        fill(be, "Juice_Apple", 10);
        be.toggleSoftHammerState();

        Recipe recipe = TankBlockEntity.findFermentationRecipe(be.getFluidTank().getFluid());
        long expected = TankBlockEntity.sealedDuration(recipe, 10);
        helper.assertTrue(expected == 205, "ceil(1024 * 10 / 50) = 205, got " + expected);

        tick(helper, be, (int) expected + 1);
        FluidStack result = be.getFluidTank().getFluid();
        helper.assertTrue(result.getFluid() == GTFluids.stack("Cider_Apple", 1).getFluid()
                        && result.getAmount() == 5,
                "10 L of juice yields 5 L of cider, got " + result.getAmount() + " of " + result.getFluid());
        helper.succeed();
    }

    /** An unsealed drum never ferments — the soft hammer is the switch, as in GT6. */
    @GameTest(template = "test_empty")
    public static void unsealedBarrelDoesNotFerment(GameTestHelper helper) {
        TankBlockEntity be = drum(helper);
        fill(be, "Juice_Apple", 50);
        tick(helper, be, 1200);
        FluidStack result = be.getFluidTank().getFluid();
        helper.assertTrue(result.getFluid() == GTFluids.stack("Juice_Apple", 1).getFluid()
                        && result.getAmount() == 50,
                "juice stays juice while unsealed, got " + result);
        helper.assertTrue(be.getSealedTime() == 0, "no progress while unsealed");
        helper.succeed();
    }

    /** Barrels cannot hold pressure: GT6 rejects gaseous fermentation, e.g. Biomass → Methane. */
    @GameTest(template = "test_empty")
    public static void gaseousFermentationIsRejected(GameTestHelper helper) {
        FluidStack biomass = GTFluids.stack("Biomass", 40);
        helper.assertTrue(biomass != null, "biomass is registered");
        helper.assertTrue(TankBlockEntity.findFermentationRecipe(biomass) == null,
                "the gaseous Biomass → Methane recipe is not usable by a barrel");

        // …but liquids are, and the same fluid in a drum ferments when sealed.
        TankBlockEntity be = drum(helper);
        fill(be, "Biomass", 40);
        be.toggleSoftHammerState();
        tick(helper, be, 200);
        helper.assertTrue(be.getFluidTank().getFluid().getFluid() == biomass.getFluid(),
                "sealed barrel leaves gaseous output alone");
        helper.succeed();
    }

    /** GT6's food fermentation chain is registered, and no fluid of it is missing. */
    @GameTest(template = "test_empty")
    public static void foodFermentationChainIsRegistered(GameTestHelper helper) {
        TreeSet<String> problems = new TreeSet<>();
        String[][] expected = {
                {"Milk", "Milk_Spoiled"},
                {"Honeydew", "ShortMead"},
                {"Juice_Apple", "Cider_Apple"},
                {"Cider_Apple", "Vinegar_Apple"},
                {"Juice_Grape_Red", "Wine_Grape_Red"},
                {"Wine_Grape_Red", "Vinegar_Grape"},
                {"Mash_Rice", "Sake"},
                {"Mash_Wheat", "Whiskey_Scotch"},
                {"Mash_WheatHops", "Beer"},
                {"Mash_Hops", "Beer_Dark"},
                {"Juice_Reed", "Rum_White"},
                {"Wine_Apricot", "Wine_Fortified"},
                // A fruit juice without a dedicated recipe of its own: the fruit-juice group of
                // GT6's chain turns it into Wine_Fruit. (Juices that do have a dedicated recipe —
                // apple → cider, grape → wine — keep that one; the map's collision check drops the
                // group variant, exactly as the original's single-input lookup prefers the first.)
                {"Juice_Melon", "Wine_Fruit"},
        };
        for (String[] pair : expected) {
            if (find(pair[0], pair[1]) == null) {
                problems.add(pair[0] + " → " + pair[1]);
            }
        }
        helper.assertTrue(problems.isEmpty(), "missing fermentation recipes: " + problems);
        helper.assertTrue(FermenterFoodRecipes.missingFluids().isEmpty(),
                "fluids missing for the food chain: " + FermenterFoodRecipes.missingFluids());
        // 70 from GT6's food chain plus the two transpiled chemical Biomass → Methane recipes.
        helper.assertTrue(MachineRecipeMaps.Fermenter.mRecipeList.size() >= 70,
                "fermenter recipes: " + MachineRecipeMaps.Fermenter.mRecipeList.size());
        helper.succeed();
    }

    private static Recipe find(String inField, String outField) {
        FluidStack in = GTFluids.stack(inField, 1);
        FluidStack out = GTFluids.stack(outField, 1);
        if (in == null || out == null) return null;
        for (Recipe recipe : MachineRecipeMaps.Fermenter.mRecipeList) {
            if (!recipe.mEnabled || recipe.mFakeRecipe) continue;
            if (recipe.mFluidInputs.length == 0 || recipe.mFluidOutputs.length == 0) continue;
            if (recipe.mFluidInputs[0].getFluid() == in.getFluid()
                    && recipe.mFluidOutputs[0].getFluid() == out.getFluid()) return recipe;
        }
        return null;
    }

    /** The GT6 rates of the chain: 16 EU/t, 50 L in, and 64/128 ticks. */
    @GameTest(template = "test_empty")
    public static void foodChainUsesTheOriginalRates(GameTestHelper helper) {
        Recipe cider = find("Juice_Apple", "Cider_Apple");
        helper.assertTrue(cider != null, "juice → cider exists");
        helper.assertTrue(cider.mEUt == 16, "16 EU/t, got " + cider.mEUt);
        helper.assertTrue(cider.mDuration == 64, "64 ticks, got " + cider.mDuration);
        helper.assertTrue(cider.mFluidInputs[0].getAmount() == 50,
                "50 L in, got " + cider.mFluidInputs[0].getAmount());
        helper.assertTrue(cider.mFluidOutputs[0].getAmount() == 25,
                "25 L out, got " + cider.mFluidOutputs[0].getAmount());

        Recipe vinegar = find("Cider_Apple", "Vinegar_Apple");
        helper.assertTrue(vinegar != null, "cider → vinegar exists");
        helper.assertTrue(vinegar.mDuration == 128 && vinegar.mFluidOutputs[0].getAmount() == 10,
                "the souring step is 128 ticks and yields 10 L");
        helper.succeed();
    }
}
