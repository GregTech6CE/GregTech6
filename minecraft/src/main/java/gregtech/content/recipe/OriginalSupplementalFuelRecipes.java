/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* Copyright (c) 2026 GregTech-6 Team */
package gregtech.content.recipe;

import gregtech.engine.recipe.RecipeMap;
import gregtech.content.data.FuelRecipeMaps;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.registry.GTFluids;
import com.mojang.logging.LogUtils;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import net.minecraft.world.level.material.Fluid;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Resolves original FL.list/category declarations against the actual prepared fluid catalog. */
final class OriginalSupplementalFuelRecipes {
    private OriginalSupplementalFuelRecipes() {}

    static void register() {
        var entries = new TreeMap<>(RegisteredFluids.all());
        Map<RecipeMap, Set<Fluid>> seen = new IdentityHashMap<>();
        for (var map : java.util.List.of(FuelRecipeMaps.Burn, FuelRecipeMaps.Gas)) {
            Set<Fluid> fluids = Collections.newSetFromMap(new IdentityHashMap<>());
            for (var recipe : map.mRecipeList)
                for (var input : recipe.mFluidInputs)
                    if (input != null && !input.isEmpty()) fluids.add(input.getFluid());
            seen.put(map, fluids);
        }
        int added = 0;
        int skipped = 0;
        int rejected = 0;
        for (var row : OriginalSupplementalFuelRows.ROWS) {
            var map = switch (row.kind()) {
                case "burn" -> FuelRecipeMaps.Burn;
                case "gas" -> FuelRecipeMaps.Gas;
                default -> throw new IllegalArgumentException("Unknown original fuel map " + row.kind());
            };
            FluidStack output = GTFluids.stack(row.output(), row.outputAmount());
            FluidStack output2 = row.output2() == null ? FluidStack.EMPTY
                    : GTFluids.stack(row.output2(), row.output2Amount());
            if (output == null || output.isEmpty() || output2 == null
                    || row.output2() != null && output2.isEmpty()) {
                skipped++;
                continue;
            }
            boolean resolved = false;
            for (var entry : entries.entrySet()) {
                var definition = entry.getValue();
                if (!(row.category() != 0 ? definition.hasFlag(row.category())
                        : row.registryNames().contains(definition.registryName()))) continue;
                FluidStack input = GTFluids.stack(entry.getKey(), row.amount());
                if (input == null || input.isEmpty()) continue;
                resolved = true;
                if (seen.get(map).contains(input.getFluid())) continue;
                var recipe = output2.isEmpty()
                        ? map.addRecipe0(true, -row.eut(), row.duration(), input, output, RecipeMap.ZL_IS)
                        : map.addRecipe0(true, -row.eut(), row.duration(), input, output, output2);
                if (recipe == null) rejected++;
                else {
                    seen.get(map).add(input.getFluid());
                    added++;
                }
            }
            if (!resolved) skipped++;
        }
        LogUtils.getLogger().info("[gregtech] Supplemental Burn/Gas fuels: {} added, {} skipped (missing fluids), {} rejected",
                added, skipped, rejected);
    }
}
