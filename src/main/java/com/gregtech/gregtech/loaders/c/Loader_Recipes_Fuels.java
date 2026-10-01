package com.gregtech.gregtech.loaders.c;

import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.data.FuelRecipeMaps;
import com.gregtech.gregtech.loaders.IGTLoader;
import com.gregtech.gregtech.registry.GTFluids;
import com.mojang.logging.LogUtils;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;
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

    private int added, skipped;

    @Override
    public void run() {
        // ── Engine fuels (combustion engines) ───────────────────────────────
        engine(128, 12, "Nitrofuel");
        engine(64, 12, "Fuel");          // high-octane
        engine(64, 8, "Diesel");
        engine(64, 7, "Petrol");
        engine(64, 5, "Rocket_Fuel");
        engine(32, 10, "Fire_Water");
        engine(16, 9, "Ethanol");
        engine(16, 9, "Methanol");
        engine(16, 6, "Hootch");

        // ── Gas turbine fuels ────────────────────────────────────────────────
        gas(16, 2, "Hydrogen", 2, water(3), null);
        gas(64, 30, "Methane", 5, water(6), co2(3));
        gas(64, 30, "NaturalGas", 5, water(6), co2(3));
        gas(64, 56, "Butane", 7, water(7), co2(6));
        gas(64, 40, "Propane", 5, water(5), co2(4));
        gas(64, 5, "Ethylene", 1, water(1), co2(1));
        gas(64, 4, "Propylene", 1, water(1), co2(1));

        // ── Burnable fuels (boilers / fluidized bed) ─────────────────────────
        burn(16, 48, "Oil_ExtraHeavy");
        burn(16, 36, "Oil_Heavy");
        burn(16, 24, "Oil_Medium");
        burn(16, 24, "Oil_Normal");
        burn(16, 18, "Oil_Light");
        burn(16, 18, "Oil_Soulsand");
        burn(16, 2, "Oil_Creosote");
        burn(16, 2, "Biomass");
        burn(16, 2, "BiomassIC2");
        burn(16, 4, "Oil_Nut");
        burn(16, 4, "Oil_Olive");
        burn(16, 2, "Oil_Lin");
        burn(16, 2, "Oil_Hemp");
        burn(16, 2, "Oil_Sunflower");
        burn(16, 2, "Oil_Seed");
        burn(16, 4, "Oil_Fish");
        burn(16, 8, "Oil_Whale");

        // ── Hot fluids (heat exchangers / thermal generators) ────────────────
        hot(16, 1250, "Lava", make("Lava_Pahoehoe", 1));
        hot(16, 20000, "Lava_Volcanic", make("Lava_Pahoehoe", 1));
        hot(2, 1, "Hot_Water", water(1));
        hot(16, 6, "Blaze", null);

        LOGGER.info("[gregtech] Fuel tables: {} added, {} skipped (missing fluids)", added, skipped);
    }

    private void engine(long eut, long duration, String key) {
        FluidStack in = make(key, 1);
        if (in == null) { skipped++; return; }
        FluidStack exhaust = co2(1);
        if (exhaust != null) {
            FuelRecipeMaps.Engine.addRecipe0(true, -eut, duration, in, exhaust, RecipeMap.ZL_IS);
        } else {
            FuelRecipeMaps.Engine.addRecipe0(true, -eut, duration, in, RecipeMap.ZL_FS);
        }
        added++;
    }

    private void gas(long eut, long duration, String key, int amount, FluidStack out1, FluidStack out2) {
        FluidStack in = make(key, amount);
        if (in == null) { skipped++; return; }
        if (out1 != null && out2 != null) {
            FuelRecipeMaps.Gas.addRecipe0(true, -eut, duration, in, out1, out2);
        } else if (out1 != null) {
            FuelRecipeMaps.Gas.addRecipe0(true, -eut, duration, in, out1, RecipeMap.ZL_IS);
        } else {
            FuelRecipeMaps.Gas.addRecipe0(true, -eut, duration, in, RecipeMap.ZL_FS);
        }
        added++;
    }

    private void burn(long eut, long duration, String key) {
        FluidStack in = make(key, 1);
        if (in == null) { skipped++; return; }
        FluidStack exhaust = co2(1);
        if (exhaust != null) {
            FuelRecipeMaps.Burn.addRecipe0(true, -eut, duration, in, exhaust, RecipeMap.ZL_IS);
        } else {
            FuelRecipeMaps.Burn.addRecipe0(true, -eut, duration, in, RecipeMap.ZL_FS);
        }
        added++;
    }

    private void hot(long eut, long duration, String key, FluidStack out) {
        FluidStack in = make(key, 1);
        if (in == null) { skipped++; return; }
        if (out != null) {
            FuelRecipeMaps.Hot.addRecipe0(true, -eut, duration, in, out, RecipeMap.ZL_IS);
        } else {
            FuelRecipeMaps.Hot.addRecipe0(true, -eut, duration, in, RecipeMap.ZL_FS);
        }
        added++;
    }

    private static FluidStack water(int mb) {
        return new FluidStack(net.minecraft.world.level.material.Fluids.WATER, mb);
    }

    private static FluidStack co2(int mb) {
        return make("CarbonDioxide", mb);
    }

    /** FluidStack from an FL registry key, or null when not registered. */
    private static FluidStack make(String key, int mb) {
        if (RegisteredFluids.get(key) == null) return null;
        RegistryObject<Fluid> fluid = GTFluids.still(key);
        if (fluid == null || !fluid.isPresent()) return null;
        return new FluidStack(fluid.get(), mb);
    }
}
