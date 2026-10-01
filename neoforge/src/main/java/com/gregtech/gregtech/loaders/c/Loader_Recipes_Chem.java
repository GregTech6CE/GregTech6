package com.gregtech.gregtech.loaders.c;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialStackItemHelper;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.loaders.IGTLoader;
import com.gregtech.gregtech.registry.GTFluids;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;

/**
 * Core industrial chemistry chains, ported from GT6 {@code Loader_Recipes_Chem}.
 *
 * <p>GT6 amounts are atom-count units: 1 atom = 1 U = 1000 mB of material gas/liquid
 * (water: 3 atoms = 3000 mB). Covered here: salt water, chloralkali electrolysis,
 * hydrochloric + sulfuric acid chains, ammonia. The remaining ~350 chem recipes
 * (organics, dyes, polymers) are tracked as follow-up work.</p>
 */
public class Loader_Recipes_Chem implements IGTLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    private int added, skipped;

    @Override
    public void run() {
        // ── Salt water ───────────────────────────────────────────────────────
        // NaCl dust + 3 H2O → 4 salt water
        mixerItemFluid(dust("NaCl", 1), water(3000), gasOrLiquid("SaltWater", 4), 16, 16);

        // ── Chloralkali: salt electrolysis ───────────────────────────────────
        // 2 NaCl + H2O-rich brine → Cl + H + O + NaOH   (GT6: dust + 3 water)
        electrolyzeSalt("NaCl", "NaOH");
        electrolyzeSalt("KCl", "KOH");
        electrolyzeSalt("LiCl", "LiOH");

        // ── Water electrolysis: 3 H2O → 2 H + 1 O (per 3000 mB) ─────────────
        FluidStack h2 = gasOrLiquid("H", 2);
        FluidStack o = gasOrLiquid("O", 1);
        if (h2 != null && o != null) {
            MachineRecipeMaps.Electrolyzer.addRecipe0(true, 64, 96, water(3000), h2, o);
            added++;
        } else skipped++;

        // ── Hydrochloric acid: 3 H2O + 2 Cl → 4 HCl + 1 O ───────────────────
        mixerFluids(new FluidStack[]{water(3000), gasOrLiquid("Cl", 2)},
                gasOrLiquid("HCl", 4), gasOrLiquid("O", 1), 16, 64);

        // ── Sulfur chain: S + 2 O → 3 SO2 (Roaster); 3 SO2 + 1 O → 4 SO3 (Burner
        // Mixer — GT6 had no direct roasting step, SO3 came from side routes);
        // 3 H2O + 4 SO3 → 7 H2SO4 (Mixer, GT6 ratio)
        roastingItemGas(dust("S", 1), gasOrLiquid("O", 2), gasOrLiquid("SO2", 3), 16, 96);
        burnMixerFluids(gasOrLiquid("SO2", 3), gasOrLiquid("O", 1), gasOrLiquid("SO3", 4), 16, 96);
        mixerFluids(new FluidStack[]{water(3000), gasOrLiquid("SO3", 4)},
                gasOrLiquid("H2SO4", 7), null, 16, 64);

        // ── Ammonia: 1 N + 3 H → 4 NH3 units (N2 + 3 H2 → 2 NH3) ────────────
        mixerFluids(new FluidStack[]{gasOrLiquid("N", 2), gasOrLiquid("H", 6)},
                gasOrLiquid("NH3", 8), null, 64, 320);

        // ── Oil refining (Distillation Tower) — GT6 ratios per 25 mB crude ──
        oilDistillation("Oil_ExtraHeavy", 25, 256, 5000, new String[]{"Fuel", "Diesel", "Kerosine", "Petrol"}, new int[]{35, 25, 25, 20});
        oilDistillation("Oil_Heavy", 25, 196, 4000, new String[]{"Fuel", "Diesel", "Kerosine", "Petrol"}, new int[]{30, 20, 20, 15});
        oilDistillation("Oil_Medium", 25, 128, 3000, new String[]{"Fuel", "Diesel", "Kerosine", "Petrol"}, new int[]{25, 15, 15, 15});
        oilDistillation("Oil_Normal", 25, 128, 3000, new String[]{"Fuel", "Diesel", "Kerosine", "Petrol"}, new int[]{25, 15, 15, 15});
        oilDistillation("Oil_Light", 25, 64, 2000, new String[]{"Fuel", "Diesel", "Kerosine", "Petrol"}, new int[]{15, 10, 10, 10});
        oilDistillation("Oil_Soulsand", 25, 64, 1000, new String[]{"Fuel", "Diesel", "Kerosine", "Petrol"}, new int[]{10, 5, 5, 5});

        // ── Biomass distillation: 80 mB → bio ethanol + glycerol + methane ──
        biomassDistillation("Biomass");
        biomassDistillation("BiomassIC2");

        // ── Cryogenic air separation: 200 mB air → N/O/CO2 fractions ────────
        FluidStack air = make("Air", 200);
        FluidStack nitrogen = gasOrLiquid("N", 0.937);
        FluidStack oxygen = gasOrLiquid("O", 0.328);
        FluidStack carbonDioxide = gasOrLiquid("CO2", 0.06);
        if (air != null && nitrogen != null && oxygen != null && carbonDioxide != null) {
            MachineRecipeMaps.CryoDistillationTower.addRecipe0(true, 64, 64, new long[]{9000},
                    new FluidStack[]{air}, new FluidStack[]{nitrogen, oxygen, carbonDioxide});
            added++;
        } else skipped++;

        LOGGER.info("[gregtech] Chemistry recipes: {} added, {} skipped (missing fluids/materials)", added, skipped);

        // The bulk-transpiled GT6 tables (tools/transpile_gt6_chem.py) are no longer loaded here:
        // they run as the LAST phase-C step so that every hand-written recipe — which was reviewed
        // against the original one by one — wins the map's collision check. See GregTech#commonSetup.
    }

    /** GT6 oil refining: crude → fuel/diesel/kerosine/petrol with per-output chances. */
    private void oilDistillation(String crude, int mb, long duration, long chance, String[] outputs, int[] amounts) {
        FluidStack in = make(crude, mb);
        if (in == null) { skipped++; return; }
        java.util.List<FluidStack> outs = new java.util.ArrayList<>();
        for (int i = 0; i < outputs.length; i++) {
            FluidStack out = make(outputs[i], amounts[i]);
            if (out == null) { skipped++; return; }
            outs.add(out);
        }
        MachineRecipeMaps.DistillationTower.addRecipe0(false, 64, duration, new long[]{chance, chance, chance},
                new FluidStack[]{in}, outs.toArray(new FluidStack[0]));
        added++;
    }

    private void biomassDistillation(String biomass) {
        FluidStack in = make(biomass, 80);
        FluidStack ethanol = make("BioEthanol", 20);
        if (ethanol == null) ethanol = make("Ethanol", 20);
        FluidStack glycerol = gasOrLiquid("Glycerol", 0.13);
        FluidStack methane = make("Methane", 4);
        if (in == null || ethanol == null || methane == null) { skipped++; return; }
        java.util.List<FluidStack> outs = new java.util.ArrayList<>();
        outs.add(ethanol);
        if (glycerol != null) outs.add(glycerol);
        outs.add(methane);
        MachineRecipeMaps.DistillationTower.addRecipe0(false, 64, 16, new long[]{500, 500, 500},
                new FluidStack[]{in}, outs.toArray(new FluidStack[0]));
        added++;
    }

    /** FluidStack from an FL registry key, or null when not registered. */
    private static FluidStack make(String key, int mb) {
        if (RegisteredFluids.get(key) == null) return null;
        net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.material.Fluid,? extends net.minecraft.world.level.material.Fluid> fluid = GTFluids.still(key);
        if (fluid == null || !fluid.isBound()) return null;
        return new FluidStack(fluid.get(), mb);
    }

    /** dust salt + 3 water → Cl/H/O gases + hydroxide dust (GT6 chloralkali). */
    private void electrolyzeSalt(String salt, String hydroxide) {
        ItemStack saltDust = dust(salt, 1);
        ItemStack hydroxideDust = dust(hydroxide, 2);
        FluidStack cl = gasOrLiquid("Cl", 0.5);
        FluidStack h = gasOrLiquid("H", 1.5);
        FluidStack o = gasOrLiquid("O", 0.5);
        if (saltDust.isEmpty() || cl == null || h == null || o == null) { skipped++; return; }
        if (hydroxideDust.isEmpty()) hydroxideDust = ItemStack.EMPTY;
        MachineRecipeMaps.Electrolyzer.addRecipe1(true, 16, 5120, saltDust,
                new FluidStack[]{water(3000)}, new FluidStack[]{cl, h, o},
                hydroxideDust.isEmpty() ? RecipeMap.ZL_IS : new ItemStack[]{hydroxideDust});
        added++;
    }

    private void mixerItemFluid(ItemStack input, FluidStack fluidIn, FluidStack fluidOut, long eut, long duration) {
        if (input.isEmpty() || fluidIn == null || fluidOut == null) { skipped++; return; }
        MachineRecipeMaps.Mixer.addRecipe1(true, eut, duration, input, fluidIn, fluidOut, RecipeMap.ZL_IS);
        added++;
    }

    private void mixerFluids(FluidStack[] inputs, FluidStack output, FluidStack output2, long eut, long duration) {
        if (output == null) { skipped++; return; }
        for (FluidStack in : inputs) if (in == null) { skipped++; return; }
        if (output2 != null) {
            MachineRecipeMaps.Mixer.addRecipe0(true, eut, duration, inputs, output, output2);
        } else {
            MachineRecipeMaps.Mixer.addRecipe0(true, eut, duration, inputs, new FluidStack[]{output});
        }
        added++;
    }

    private void roastingItemGas(ItemStack input, FluidStack gasIn, FluidStack gasOut, long eut, long duration) {
        if (input.isEmpty() || gasIn == null || gasOut == null) { skipped++; return; }
        MachineRecipeMaps.Roasting.addRecipe1(true, eut, duration, input, gasIn, gasOut, RecipeMap.ZL_IS);
        added++;
    }

    /** Burner Mixer takes up to 6 fluid inputs — used for gas-phase oxidation steps. */
    private void burnMixerFluids(FluidStack in1, FluidStack in2, FluidStack out, long eut, long duration) {
        if (in1 == null || in2 == null || out == null) { skipped++; return; }
        MachineRecipeMaps.BurnMixer.addRecipe0(true, eut, duration, new FluidStack[]{in1, in2}, new FluidStack[]{out});
        added++;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static ItemStack dust(String materialName, int count) {
        GTMaterial material = GTMaterialRegistry.get(materialName);
        if (material == null || !material.resolve().isValid()) return ItemStack.EMPTY;
        return MaterialStackItemHelper.mat(MaterialPrefix.dust, material.resolve(), count);
    }

    private static FluidStack water(int mb) {
        return new FluidStack(Fluids.WATER, mb);
    }

    /** Material fluid (gas/liquid/named), amount in atom units (1 U = 1000 mB). */
    private static FluidStack gasOrLiquid(String materialName, double units) {
        GTMaterial material = GTMaterialRegistry.get(materialName);
        String name = material != null && material.resolve().isValid() ? material.resolve().getName() : materialName;
        for (String key : new String[]{"GenGas_" + name, "GenLiquid_" + name, name}) {
            if (RegisteredFluids.get(key) == null) continue;
            DeferredHolder<Fluid,? extends Fluid> fluid = GTFluids.still(key);
            if (fluid == null || !fluid.isBound()) continue;
            int mb = (int) Math.round(units * 1000);
            if (mb <= 0) return null;
            return new FluidStack(fluid.get(), mb);
        }
        return null;
    }
}
