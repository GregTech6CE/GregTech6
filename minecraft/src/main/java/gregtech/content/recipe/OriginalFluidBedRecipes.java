/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* Copyright (c) 2026 GregTech-6 Team */
package gregtech.content.recipe;

import com.mojang.logging.LogUtils;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.api.addon.recipe.MaterialUnitIngredient.Rounding;
import gregtech.content.data.FuelRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTFluids;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialStackItemHelper;
import gregtech.engine.material.MaterialUnification;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.MaterialUnitRecipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.util.OM;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Native item/fluid boundary for the original fluidized-bed fuel charges. */
final class OriginalFluidBedRecipes {
    private OriginalFluidBedRecipes() {}

    static void register() {
        var calcite = GTFluids.stack("GenMolten_Calcite", 1);
        if (calcite == null || calcite.isEmpty()) {
            LogUtils.getLogger().warn("[gregtech] Fluidized-bed fuels unavailable: molten calcite is not registered");
            return;
        }
        int added = 0;
        int skipped = 0;
        int rejected = 0;
        for (var fuel : OriginalFluidBedFuelRows.FUELS) {
            GTMaterial material = GTMaterialRegistry.get(fuel.materialId());
            for (var form : OriginalFluidBedFuelRows.FORMS) {
                ItemStack input = input(material, form.prefix());
                if (input.isEmpty()) {
                    skipped++;
                    continue;
                }
                long ashUnits = OriginalFluidBedFuelRows.ashUnits(fuel, form);
                ItemStack ash = ashUnits < GTValues.U72
                        ? ItemStack.EMPTY
                        : OM.dust(GTMaterialRegistry.get(fuel.ashMaterialId()), ashUnits);
                // A representable declared ash output must not disappear if its form is unavailable.
                if (ashUnits >= GTValues.U72 && ash.isEmpty()) {
                    skipped++;
                    continue;
                }
                var calciteInput = calcite.copy();
                calciteInput.setAmount(form.calcite());
                var template = RecipeMap.make(
                        true,
                        new ItemStack[] {input},
                        new ItemStack[] {ash},
                        null,
                        null,
                        new FluidStack[] {calciteInput},
                        null,
                        OriginalFluidBedFuelRows.heatUnits(fuel, form),
                        -1,
                        0);
                if (template == null) {
                    rejected++;
                    continue;
                }
                long required = Math.multiplyExact(form.materialAmount(), input.getCount());
                var rule = new MaterialUnitRecipe(template, 0, material, required, Rounding.WHOLE_OUTPUTS);
                rule.source(input, form.materialAmount());
                rule.publish(FuelRecipeMaps.FluidBed);
                added++;
            }
        }
        LogUtils.getLogger()
                .info(
                        "[gregtech] Fluidized-bed fuels: {} added, {} skipped (unavailable forms), {} rejected",
                        added,
                        skipped,
                        rejected);
    }

    private static ItemStack input(GTMaterial material, String prefix) {
        if (!material.isValid()) return ItemStack.EMPTY;
        return switch (prefix) {
            case "blockDust" -> {
                ItemStack stack = GTBlocks.getStack(BlockMaterialPrefix.blockDust, material);
                yield stack.isEmpty()
                        ? MaterialUnification.vanillaFormStack("blockDust", material)
                        : stack;
            }
            case "dust" -> MaterialStackItemHelper.mat(MaterialPrefix.dust, material, 1);
            case "dustSmall" -> MaterialStackItemHelper.mat(MaterialPrefix.dustSmall, material, 1);
            case "dustTiny" -> MaterialStackItemHelper.mat(MaterialPrefix.dustTiny, material, 1);
            case "dustDiv72" -> MaterialStackItemHelper.mat(MaterialPrefix.dustDiv72, material, 1);
            default -> throw new IllegalArgumentException("Unknown original fuel form " + prefix);
        };
    }
}
