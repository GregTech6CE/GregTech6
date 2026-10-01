package com.gregtech.gregtech.loaders.c;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.loaders.IGTLoader;
import com.gregtech.gregtech.registry.GTFluids;
import com.mojang.logging.LogUtils;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Molten-metal alloying recipes for the Mixer, ported from GT6
 * {@code gregtech.loaders.c.Loader_Recipes_Alloys}.
 *
 * <p>Inputs/outputs are molten fluids ({@code GenMolten_<Material>}); amounts are in
 * material units (1 unit = 144 mB). Recipes whose materials/fluids are not registered
 * are skipped silently — GT6 had the same behavior for absent cross-mod metals.</p>
 */
public class Loader_Recipes_Alloys implements IGTLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int UNIT_MB = 144;

    private int added, skipped;

    @Override
    public void run() {
        // Annealing / purification smelting (fluid -> fluid)
        smelt("Copper", "AnnealedCopper");
        smelt("Iron", "WroughtIron");

        for (String copper : new String[]{"Copper", "AnnealedCopper"}) {
            mix(out("Constantan", 2), copper, 1, "Nickel", 1);
            mix(out("ArsenicCopper", 4), copper, 3, "Arsenic", 1);
            mix(out("Bronze", 4), copper, 3, "Tin", 1);
            mix(out("Brass", 4), copper, 3, "Zinc", 1);
            mix(out("AluminiumBrass", 4), copper, 1, "Aluminium", 3);
            mix(out("RedAlloy", 1), copper, 1, "Redstone", 4);
            mix(out("SterlingSilver", 5), copper, 1, "Silver", 4);
            mix(out("PurpleAlloy", 2), copper, 1, "Silver", 1, "Redstone", 4, "Nikolite", 4);
            mix(out("Signalum", 4), copper, 3, "Silver", 1, "Redstone", 10);
            mix(out("Signalum", 8), copper, 1, "Silver", 2, "RedAlloy", 5);
            mix(out("BlackBronze", 5), copper, 3, "Silver", 1, "Gold", 1);
            mix(out("Hepatizon", 8), copper, 3, "Tin", 1, "Gold", 4);
            mix(out("BlackBronze", 5), copper, 3, "Electrum", 2);
            mix(out("RoseGold", 5), copper, 1, "Gold", 4);
            mix(out("ArsenicBronze", 5), copper, 3, "Tin", 1, "Arsenic", 1);
            mix(out("BismuthBronze", 5), copper, 3, "Zinc", 1, "Bismuth", 1);
        }
        for (String iron : new String[]{"Iron", "WroughtIron", "PigIron", "MeteoricIron"}) {
            mix(out("Invar", 3), iron, 2, "Nickel", 1);
            mix(out("Kanthal", 3), iron, 1, "Aluminium", 1, "Chromium", 1);
            mix(out("TinAlloy", 2), iron, 1, "Tin", 1);
            mix(out("Angmallen", 2), iron, 1, "Gold", 1);
            mix(out("StainlessSteel", 9), iron, 6, "Nickel", 1, "Chromium", 1, "Manganese", 1);
            mix(out("StainlessSteel", 9), iron, 4, "Invar", 3, "Chromium", 1, "Manganese", 1);
            mix(out("StainlessSteel", 36), iron, 24, "Nichrome", 5, "Chromium", 3, "Manganese", 4);
            mix(out("ElectrotineAlloy", 1), iron, 1, "Nikolite", 8);
        }
        for (String steel : new String[]{"Steel", "MeteoricSteel", "HSLA", "Knightmetal"}) {
            mix(out("TungstenSteel", 2), steel, 1, "Tungsten", 1);
        }
        mix(out("Lumium", 4), "Tin", 3, "Silver", 1, "Glowstone", 4);
        mix(out("BlueAlloy", 1), "Nikolite", 4, "Silver", 1);
        mix(out("PurpleAlloy", 1), "RedAlloy", 1, "BlueAlloy", 1);
        mix(out("RedstoneAlloy", 1), "Silicon", 1, "Redstone", 1);
        mix(out("NikolineAlloy", 1), "Silicon", 1, "Nikolite", 1);
        mix(out("Nichrome", 5), "Chromium", 1, "Nickel", 4);
        mix(out("Netherite", 1), "Gold", 4, "AncientDebris", 4);
        mix(out("TitaniumGold", 4), "Gold", 1, "Titanium", 3);
        mix(out("Hepatizon", 2), "Gold", 1, "Bronze", 1);
        mix(out("Electrum", 2), "Gold", 1, "Silver", 1);
        mix(out("GoldInductive", 2), "Gold", 1, "Redstone", 1);
        mix(out("SolderingAlloy", 10), "Tin", 9, "Antimony", 1);
        mix(out("BatteryAlloy", 5), "Lead", 4, "Antimony", 1);
        mix(out("Celenegil", 2), "Orichalcum", 1, "Platinum", 1);
        mix(out("Manyullyn", 2), "Ardite", 1, "Cobalt", 1);
        mix(out("Magnalium", 3), "Magnesium", 1, "Aluminium", 2);
        mix(out("CobaltBrass", 9), "Brass", 7, "Aluminium", 1, "Cobalt", 1);
        mix(out("BismuthBronze", 5), "Brass", 4, "Bismuth", 1);
        mix(out("ArsenicBronze", 5), "Bronze", 4, "Arsenic", 1);
        mix(out("ArsenicBronze", 5), "ArsenicCopper", 4, "Tin", 1);
        mix(out("Ultimet", 9), "Cobalt", 5, "Chromium", 2, "Nickel", 1, "Molybdenum", 1);
        mix(out("Ultimet", 36), "Cobalt", 20, "Chromium", 7, "Nichrome", 5, "Molybdenum", 4);
        mix(out("Osmiridium", 2), "Osmium", 1, "Iridium", 1);
        mix(out("HSSG", 9), "TungstenSteel", 5, "Chromium", 1, "Molybdenum", 2, "Vanadium", 1);
        mix(out("HSSE", 9), "HSSG", 6, "Cobalt", 1, "Manganese", 1, "Silicon", 1);
        mix(out("HSSS", 9), "HSSG", 6, "Osmiridium", 2, "Iridium", 1);
        mix(out("HSSS", 9), "HSSG", 6, "Osmium", 1, "Iridium", 2);

        crucibleAlloyingDisplay();

        LOGGER.info("[gregtech] Alloy recipes: {} added, {} skipped (missing fluids)", added, skipped);
    }

    /**
     * Informational recipes for the Combination Smelting (crucible alloying) JEI tab:
     * one fake recipe per ALLOY material with composition data, shown with unit items.
     * The actual conversion happens inside {@code SmeltingCrucibleBlockEntity}.
     */
    private void crucibleAlloyingDisplay() {
        int shown = 0;
        for (var reaction : com.gregtech.gregtech.api.machine.crucible.CrucibleReactions.allRecipes()) {
            List<net.minecraft.world.item.ItemStack> inputs = new ArrayList<>();
            boolean complete = reaction.yield() > 0 && reaction.yield() <= 64;
            for (var part : reaction.parts()) {
                if (part.ratio() > 64) { complete = false; break; }
                var stack = com.gregtech.gregtech.api.material.MaterialStackItemHelper.mat(
                        com.gregtech.gregtech.data.MaterialPrefix.unit, part.material(), (int)part.ratio());
                if (stack.isEmpty()) { complete = false; break; }
                inputs.add(stack);
            }
            if (!complete || inputs.isEmpty()) continue;
            var output = com.gregtech.gregtech.api.material.MaterialStackItemHelper.mat(
                    com.gregtech.gregtech.data.MaterialPrefix.unit, reaction.output(), (int)reaction.yield());
            if (output.isEmpty()) continue;
            MachineRecipeMaps.CrucibleAlloying.addFakeRecipe(false, inputs.toArray(RecipeMap.ZL_IS),
                    new net.minecraft.world.item.ItemStack[]{output}, null, null, null, null, 0, 0, reaction.output().getMeltingPoint());
            shown++;
        }
        for (var item : new net.minecraft.world.item.Item[]{net.minecraft.world.item.Items.COAL, net.minecraft.world.item.Items.CHARCOAL}) {
            var carbon = com.gregtech.gregtech.api.material.MaterialStackItemHelper.mat(
                    com.gregtech.gregtech.data.MaterialPrefix.unit,
                    com.gregtech.gregtech.content.material.Materials.Carbon, 1);
            MachineRecipeMaps.CrucibleAlloying.addFakeRecipe(false,
                    new net.minecraft.world.item.ItemStack[]{new net.minecraft.world.item.ItemStack(item, 2)},
                    new net.minecraft.world.item.ItemStack[]{carbon}, null, null, null, null, 0, 0, 1700);
        }
        LOGGER.info("[gregtech] Crucible alloying display recipes: {}", shown);
    }

    /** Output spec: material name + unit amount. */
    private record Out(String name, int units) {}

    private static Out out(String name, int units) {
        return new Out(name, units);
    }

    /** Smelter: 1 unit of molten {@code from} → 1 unit of molten {@code to}. */
    private void smelt(String from, String to) {
        FluidStack in = molten(from, 1);
        FluidStack result = molten(to, 1);
        if (in == null || result == null) { skipped++; return; }
        MachineRecipeMaps.Smelter.addRecipe0(true, 16, 16, in, result, RecipeMap.ZL_IS);
        added++;
    }

    /** Mixer recipe from (materialName, units) pairs → output spec. */
    private void mix(Out output, Object... pairs) {
        FluidStack result = molten(output.name(), output.units());
        if (result == null) { skipped++; return; }
        List<FluidStack> inputs = new ArrayList<>();
        int totalUnits = 0;
        for (int i = 0; i < pairs.length; i += 2) {
            String name = (String) pairs[i];
            int units = (Integer) pairs[i + 1];
            FluidStack fluid = molten(name, units);
            if (fluid == null) { skipped++; return; }
            inputs.add(fluid);
            totalUnits += units;
        }
        long duration = Math.max(output.units(), totalUnits) * 16L;
        MachineRecipeMaps.Mixer.addRecipe0(true, 16, duration, inputs.toArray(new FluidStack[0]), result, RecipeMap.ZL_IS);
        added++;
    }

    /** Molten fluid stack for a material name, or null when unavailable. */
    private static FluidStack molten(String materialName, int units) {
        GTMaterial material = GTMaterialRegistry.get(materialName);
        if (material == null || !material.resolve().isValid()) return null;
        String key = "GenMolten_" + material.resolve().getName();
        if (RegisteredFluids.get(key) == null) return null;
        DeferredHolder<Fluid,? extends Fluid> fluid = GTFluids.still(key);
        if (fluid == null || !fluid.isBound()) return null;
        return new FluidStack(fluid.get(), units * UNIT_MB);
    }
}
