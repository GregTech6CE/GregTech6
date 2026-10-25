/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* Copyright (c) 2026 GregTech-6 Team */
package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialStackItemHelper;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.data.FuelRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.util.OM;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;

/** Native item/fluid boundary for the original fluidized-bed fuel charges. */
final class OriginalFluidBedRecipes {
    private OriginalFluidBedRecipes() {}

    static void register() {
        int added = 0;
        int skipped = 0;
        int rejected = 0;
        for (var fuel : OriginalFluidBedFuelRows.FUELS) {
            GTMaterial material = GTMaterialRegistry.get(fuel.materialId());
            for (var form : OriginalFluidBedFuelRows.FORMS) {
                ItemStack input = input(material, form.prefix());
                var calcite = GTFluids.stack("Calcite", form.calcite());
                if (input.isEmpty() || calcite == null || calcite.isEmpty()) {
                    skipped++;
                    continue;
                }
                long ashUnits = OriginalFluidBedFuelRows.ashUnits(fuel, form);
                ItemStack ash = ashUnits < GTValues.U72 ? ItemStack.EMPTY
                        : OM.dust(GTMaterialRegistry.get(fuel.ashMaterialId()), ashUnits);
                // A representable declared ash output must not disappear if its form is unavailable.
                if (ashUnits >= GTValues.U72 && ash.isEmpty()) {
                    skipped++;
                    continue;
                }
                var recipe = FuelRecipeMaps.FluidBed.addRecipe1(true, -1,
                        OriginalFluidBedFuelRows.heatUnits(fuel, form), input, calcite,
                        null, ash);
                if (recipe == null) rejected++;
                else added++;
            }
        }
        LogUtils.getLogger().info("[gregtech] Fluidized-bed fuels: {} added, {} skipped (unavailable forms), {} rejected",
                added, skipped, rejected);
    }

    private static ItemStack input(GTMaterial material, String prefix) {
        if (!material.isValid()) return ItemStack.EMPTY;
        return switch (prefix) {
            case "blockDust" -> GTBlocks.getStack(BlockMaterialPrefix.blockDust, material);
            case "dust" -> MaterialStackItemHelper.mat(MaterialPrefix.dust, material, 1);
            case "dustSmall" -> MaterialStackItemHelper.mat(MaterialPrefix.dustSmall, material, 1);
            case "dustTiny" -> MaterialStackItemHelper.mat(MaterialPrefix.dustTiny, material, 1);
            case "dustDiv72" -> MaterialStackItemHelper.mat(MaterialPrefix.dustDiv72, material, 1);
            default -> throw new IllegalArgumentException("Unknown original fuel form " + prefix);
        };
    }
}
