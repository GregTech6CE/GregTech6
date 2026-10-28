package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialStackItemHelper;
import gregtech.engine.recipe.RecipeMap;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.bootstrap.InitializationStep;
import gregtech.content.registry.GTFluids;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;
//?}
import org.slf4j.Logger;

/**
 * Explicit chemistry chains resolved against the registered materials and fluids.
 *
 * <p>The local gas/liquid helper uses 1000 mB per material unit; callers such as
 * water handling also supply explicit mB amounts. This loader covers salt water,
 * chloralkali, acid/ammonia chains and oil/biomass distillation. Generated tables
 * are loaded separately; these rows do not prove complete GT6 recipe coverage.</p>
 */
public class ChemicalRecipeLoader implements InitializationStep {
    private static final Logger LOGGER = LogUtils.getLogger();

    private int added, skipped;

    @Override
    public void run() {
        // ── Salt water ───────────────────────────────────────────────────────
        // NaCl dust + 3 H2O → 4 salt water
        mixerItemFluid(dust("NaCl", 1), water(3000), gasOrLiquid("SaltWater", 4), 16, 16);

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

        // The original six-product air separation rows are registered by GTGeneratedChem.
        // Do not shadow them with a same-input three-product approximation.

        LOGGER.info("[gregtech] Chemistry recipes: {} added, {} skipped (missing fluids/materials)", added, skipped);

        // The bulk-transpiled GT6 tables (tools/transpile_gt6_chem.py) are no longer loaded here:
        // they run as the LAST phase-C step so that every hand-written recipe — which was reviewed
        // against the original one by one — wins the map's collision check. See GregTech#commonSetup.
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
//? if >=1.21.1 {
            /*DeferredHolder<Fluid, ? extends Fluid> fluid = GTFluids.still(key);
            if (fluid == null || !fluid.isBound()) continue;
*///?} else {
            RegistryObject<Fluid> fluid = GTFluids.still(key);
            if (fluid == null || !fluid.isPresent()) continue;
//?}
            int mb = (int) Math.round(units * 1000);
            if (mb <= 0) return null;
            return new FluidStack(fluid.get(), mb);
        }
        return null;
    }
}
