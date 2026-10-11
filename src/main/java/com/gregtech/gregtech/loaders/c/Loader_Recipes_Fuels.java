package com.gregtech.gregtech.loaders.c;

import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.FuelRecipeMaps;
import com.gregtech.gregtech.loaders.IGTLoader;
import com.gregtech.gregtech.registry.GTFluids;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fluids.FluidStack;
import org.slf4j.Logger;

/**
 * Fuel tables, ported from GT6 {@code Loader_Fuels}.
 *
 * <p>Convention (matching GT6's fuel maps): {@code mEUt} is negative (energy
 * produced per tick), {@code mDuration} is burn time in ticks for the listed input
 * amount, so total energy = {@code |EUt| * duration} per charge. Exhaust fluids
 * (CO2 / water / pahoehoe lava) are recipe outputs. Missing fluids skip silently.</p>
 */
public class Loader_Recipes_Fuels implements IGTLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    private int added, skipped, rejected;

    @Override
    public void run() {
        for(var row:com.gregtech.gregtech.content.recipe.OriginalFuelRecipeRows.ROWS)switch(row.kind()){
            case "engine"->engine(row.eut(),row.duration(),row.input());
            case "burn"->burn(row.eut(),row.duration(),row.input());
            case "gas"->gas(row.eut(),row.duration(),row.input(),row.amount(),rowFluid(row.output(),row.outputAmount()),rowFluid(row.output2(),row.output2Amount()));
            case "hot"->hot(row);
            default->throw new IllegalStateException("Unknown original fuel kind "+row.kind());
        }

        LOGGER.info("[gregtech] Fuel tables: {} added, {} skipped (missing fluids), {} rejected", added, skipped, rejected);
    }

    private static FluidStack rowFluid(String key,int amount){return key==null?null:key.equals("water")?water(amount):make(key,amount);}

    private void engine(long eut, long duration, String key) {
        FluidStack in = make(key, 1);
        if (in == null) { skipped++; return; }
        FluidStack exhaust = co2(1);
        if (exhaust != null) {
            record(FuelRecipeMaps.Engine.addRecipe0(true, -eut, duration, in, exhaust, RecipeMap.ZL_IS));
        } else {
            record(FuelRecipeMaps.Engine.addRecipe0(true, -eut, duration, in, RecipeMap.ZL_FS));
        }
    }

    private void gas(long eut, long duration, String key, int amount, FluidStack out1, FluidStack out2) {
        FluidStack in = make(key, amount);
        if (in == null) { skipped++; return; }
        if (out1 != null && out2 != null) {
            record(FuelRecipeMaps.Gas.addRecipe0(true, -eut, duration, in, out1, out2));
        } else if (out1 != null) {
            record(FuelRecipeMaps.Gas.addRecipe0(true, -eut, duration, in, out1, RecipeMap.ZL_IS));
        } else {
            record(FuelRecipeMaps.Gas.addRecipe0(true, -eut, duration, in, RecipeMap.ZL_FS));
        }
    }

    private void burn(long eut, long duration, String key) {
        FluidStack in = make(key, 1);
        if (in == null) { skipped++; return; }
        FluidStack exhaust = co2(1);
        if (exhaust != null) {
            record(FuelRecipeMaps.Burn.addRecipe0(true, -eut, duration, in, exhaust, RecipeMap.ZL_IS));
        } else {
            record(FuelRecipeMaps.Burn.addRecipe0(true, -eut, duration, in, RecipeMap.ZL_FS));
        }
    }

    private void hot(com.gregtech.gregtech.content.recipe.OriginalFuelRecipeRows.Row row) {
        FluidStack in = make(row.input(), row.amount());
        FluidStack out = rowFluid(row.output(), row.outputAmount());
        // A declared exhaust is part of the recipe: never turn a missing output into voiding fuel.
        if (in == null || row.output() != null && out == null) { skipped++; return; }
        if (out != null) {
            record(FuelRecipeMaps.Hot.addRecipe0(true, -row.eut(), row.duration(), in, out, RecipeMap.ZL_IS));
        } else {
            record(FuelRecipeMaps.Hot.addRecipe0(true, -row.eut(), row.duration(), in, RecipeMap.ZL_FS));
        }
    }

    private void record(com.gregtech.gregtech.api.recipe.Recipe recipe) {
        if (recipe == null) rejected++;
        else added++;
    }

    private static FluidStack water(int mb) {
        return new FluidStack(net.minecraft.world.level.material.Fluids.WATER, mb);
    }

    private static FluidStack co2(int mb) {
        return make("CarbonDioxide", mb);
    }

    /** FluidStack from an FL registry key, or null when not registered. */
    private static FluidStack make(String key, int mb) {
        // Original FL.Lava/Water reuse vanilla fluids and intentionally have no GT still holder.
        var fluid=GTFluids.stack(key,mb);
        return fluid==null||fluid.isEmpty()?null:fluid;
    }
}
