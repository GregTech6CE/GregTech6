/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later. */
package gregtech.internal.fluid;

import gregtech.api.recipe.*;
import gregtech.internal.recipe.*;
import gregtech.content.multiblock.HeatExchangerRules;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** One native fluid/recipe boundary for both sizes of original heat exchanger. */
public final class HotFluidFuelCycle {

    private HotFluidFuelCycle() {
    }

    public record Result(long heat, Recipe recipe, boolean active) {
    }

    public static Result charge(HeatExchangerRules.Settings settings, FluidTankGT hot, FluidTankGT cold,
            long heat, long history, Recipe lastRecipe) {
        var idleResult = new Result(heat, lastRecipe, false);
        var map = RecipeMap.RECIPE_MAPS.get(settings.fuelMap());
        if (hot.isEmpty() || map == null) return idleResult;
        Recipe fuelRecipe = map.mRecipeList.contains(lastRecipe) && matches(lastRecipe, hot) ? lastRecipe : null;
        if (fuelRecipe == null)
            for (var candidate : map.mRecipeList)
                if (matches(candidate, hot)) {
                    fuelRecipe = candidate;
                    break;
                }
        if (fuelRecipe == null) {
            // A configurable map may contain presence-only rows. Reject without purging their fluid.
            for (var candidate : map.mRecipeList)
                if (matchesFluid(candidate, hot) && candidate.isPresenceFluidInput(0)) return idleResult;
            hot.setEmpty();
            return idleResult;
        }
        var output = fuelRecipe.mFluidOutputs.length == 0 ? FluidStack.EMPTY : fuelRecipe.mFluidOutputs[0];
//? if >=1.21.1 {
        /*if (!output.isEmpty() && !cold.isEmpty() && !FluidStack.isSameFluidSameComponents(cold.getFluidLong(), output)) return idleResult;
*///?} else {
        if (!output.isEmpty() && !cold.isEmpty() && !cold.getFluidLong().isFluidEqual(output)) return idleResult;
//?}
        var plan = HeatExchangerRules.charge(hot.getAmount(), fuelRecipe.mFluidInputs[0].getAmount(), cold.getAmount(),
                output.getAmount(), heat, settings.bufferTarget(), fuelRecipe.mEUt, fuelRecipe.mDuration, settings.efficiency());
        if (plan == null) {
            if (hot.getAmount() < fuelRecipe.mFluidInputs[0].getAmount() && history == 0) hot.setEmpty();
            return idleResult;
        }
        hot.remove(plan.inputUsed());
        if (plan.outputMade() > 0) cold.setFluid(output, cold.getAmount() + plan.outputMade());
        return new Result(heat + plan.energyAdded(), fuelRecipe, true);
    }

    private static boolean matches(Recipe recipe, FluidTankGT hot) {
        return matchesFluid(recipe, hot) && !recipe.isPresenceFluidInput(0);
    }

    private static boolean matchesFluid(Recipe recipe, FluidTankGT hot) {
        return recipe != null && recipe.mEnabled && !recipe.mFakeRecipe && recipe.mInputs.length == 0
                && recipe.mFluidInputs.length == 1 && !recipe.mFluidInputs[0].isEmpty()
//? if >=1.21.1 {
                /*&& FluidStack.isSameFluidSameComponents(hot.getFluidLong(), recipe.mFluidInputs[0]);
*///?} else {
                && hot.getFluidLong().isFluidEqual(recipe.mFluidInputs[0]);
//?}
    }
}
