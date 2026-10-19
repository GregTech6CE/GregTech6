/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later. */
package com.gregtech.gregtech.api.fluid;

import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.content.multiblock.HeatExchangerRules;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** One native fluid/recipe boundary for both sizes of original heat exchanger. */
public final class HotFluidFuelCycle {
    private HotFluidFuelCycle() {}
    public record Result(long heat, Recipe recipe, boolean active) {}
    public static Result charge(HeatExchangerRules.Settings settings, FluidTankGT hot, FluidTankGT cold,
                                long heat, long history, Recipe last) {
        var idle = new Result(heat, last, false);
        var map = RecipeMap.RECIPE_MAPS.get(settings.fuelMap());
        if (hot.isEmpty() || map == null) return idle;
        Recipe recipe = map.mRecipeList.contains(last) && matches(last, hot) ? last : null;
        if (recipe == null) for (var candidate : map.mRecipeList) if (matches(candidate, hot)) { recipe = candidate; break; }
        if (recipe == null) { hot.setEmpty(); return idle; }
        var output = recipe.mFluidOutputs.length == 0 ? FluidStack.EMPTY : recipe.mFluidOutputs[0];
//? if >=1.21.1 {
        /*if (!output.isEmpty() && !cold.isEmpty() && !FluidStack.isSameFluidSameComponents(cold.getFluidLong(), output)) return idle;
*///?} else {
        if (!output.isEmpty() && !cold.isEmpty() && !cold.getFluidLong().isFluidEqual(output)) return idle;
//?}
        var plan = HeatExchangerRules.charge(hot.getAmount(), recipe.mFluidInputs[0].getAmount(), cold.getAmount(),
                output.getAmount(), heat, settings.bufferTarget(), recipe.mEUt, recipe.mDuration, settings.efficiency());
        if (plan == null) {
            if (hot.getAmount() < recipe.mFluidInputs[0].getAmount() && history == 0) hot.setEmpty();
            return idle;
        }
        hot.remove(plan.inputUsed());
        if (plan.outputMade() > 0) cold.setFluid(output, cold.getAmount() + plan.outputMade());
        return new Result(heat + plan.energyAdded(), recipe, true);
    }
    private static boolean matches(Recipe recipe, FluidTankGT hot) {
        return recipe != null && recipe.mEnabled && !recipe.mFakeRecipe && recipe.mInputs.length == 0
                && recipe.mFluidInputs.length == 1 && !recipe.mFluidInputs[0].isEmpty()
//? if >=1.21.1 {
                /*&& FluidStack.isSameFluidSameComponents(hot.getFluidLong(), recipe.mFluidInputs[0]);
*///?} else {
                && hot.getFluidLong().isFluidEqual(recipe.mFluidInputs[0]);
//?}
    }
}
