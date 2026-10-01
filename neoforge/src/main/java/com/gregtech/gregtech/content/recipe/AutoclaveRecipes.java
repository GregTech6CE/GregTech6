package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.content.recipe.AutoclaveRecipeRows.Row;


import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.data.generated.MaterialWorkability;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GT6's Autoclave handler block: {@code Loader_Recipes_Handlers:60-77}.
 * <p>
 * Twelve rows grow crystals from dust: {@code dustSmall 1/2/4/8/16/32} and {@code dust 1/1/1/2/4/8} turn
 * into the gem ladder ({@code gemChipped} → {@code gemLegendary}), consuming steam and returning
 * distilled water. The machine runs on steam rather than EU ({@code aEUt = 0}) and the rows are gated
 * on {@code TD.Processing.CRYSTALLISABLE} — the port imports that flag through
 * {@code tools/extract_gt6_workability.py} (43 materials). Every row carries GT6's circuit selector
 * ({@code ST.tag(0..5)}), which the port expresses as {@link GTTechnological#selectorTag(int)}.
 */
public final class AutoclaveRecipes {
    /** One registered crystallisation route, for tests and reports. */
    public record Entry(String route, Recipe recipe) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final List<String> SKIPPED = List.of(
            "GT6's COATED.NOT / EXPLODES_IN_NONVANILLA_CRAFTING_GRID.NOT conditions have no port"
                    + " counterpart; ANTIMATTER.NOT and CRYSTALLISABLE are applied");

    /** GT6 row: input prefix, input count, steam mB, distilled water mB, ticks, circuit, output prefix, count. */


    private static final List<Row> ROWS = AutoclaveRecipeRows.ROWS;

    private static boolean registered;

    private AutoclaveRecipes() {}

    public static List<Entry> entries() { return Collections.unmodifiableList(ENTRIES); }

    public static List<String> skipped() { return SKIPPED; }

    public static int register() {
        if (registered) throw new IllegalStateException("Autoclave recipes registered twice");
        registered = true;
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (material.has(MaterialProperty.ANTIMATTER)) continue;
            if (!MaterialWorkability.isCrystallisable(material)) continue;
            for (Row row : ROWS) crystallise(material, row);
        }
        com.mojang.logging.LogUtils.getLogger().info("Registered {} GT6 autoclave crystallisation recipes ({} notes: {})",
                ENTRIES.size(), SKIPPED.size(), SKIPPED);
        return ENTRIES.size();
    }

    private static void crystallise(GTMaterial material, Row row) {
        ItemStack input = GTItems.getStack(row.input(), material, row.inCount());
        ItemStack output = GTItems.getStack(row.output(), material, row.outCount());
        FluidStack steam = GTFluids.stack("Steam", row.steam());
        FluidStack distilledWater = byRegistryName("ic2distilledwater", row.distW());
        if (input.isEmpty() || output.isEmpty() || steam == null || distilledWater == null) return;
        ItemStack tag = new ItemStack(GTTechnological.selectorTag(row.circuit()));
        // GT6 runs these on steam (aEUt = 0); the circuit selector is a required input.
        Recipe recipe = MachineRecipeMaps.Autoclave.addRecipe2(false, 0, row.ticks(), input, tag,
                steam, distilledWater, output);
        if (recipe != null) {
            ENTRIES.add(new Entry(row.input().getName() + " x" + row.inCount() + " -> "
                    + row.output().getName() + " x" + row.outCount(), recipe));
        }
    }

    /** Fluid by GT6 registry name (GT6 {@code FL.DistW} is registered as {@code ic2distilledwater}). */
    private static FluidStack byRegistryName(String registryName, int mb) {
        for (var entry : RegisteredFluids.all().entrySet()) {
            if (entry.getValue().registryName().equalsIgnoreCase(registryName)) {
                return GTFluids.stack(entry.getKey(), mb);
            }
        }
        return null;
    }
}
