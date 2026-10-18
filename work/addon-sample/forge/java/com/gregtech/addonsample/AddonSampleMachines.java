package com.gregtech.addonsample;

import com.gregtech.gregtech.api.addon.AddonMachineContributor;
import com.gregtech.gregtech.api.addon.MachineDeclaration;
import com.gregtech.gregtech.api.addon.MachineLayout;
import com.gregtech.gregtech.api.addon.RecipeMapDeclaration;
import com.gregtech.gregtech.api.addon.StructureRole;

import java.util.ArrayList;
import java.util.List;

/** One single-block EU press and one tank-wall multiblock, each with its own recipe map. */
public final class AddonSampleMachines implements AddonMachineContributor {
    @Override public String addonId() { return "addon_sample"; }

    @Override public List<RecipeMapDeclaration> recipeMaps() {
        return List.of(
                map("addon_sample.recipe.press", "Sample Press", "press"),
                map("addon_sample.recipe.large_press", "Large Sample Press", "large_press"));
    }

    @Override public List<MachineDeclaration> machines() {
        return List.of(
                MachineDeclaration.builder("sample_press", "Steel")
                        .machineType("samplepress").energy("EU", 32).tier(1)
                        .recipeMap("addon_sample.recipe.press").basic().build(),
                MachineDeclaration.builder("large_sample_press", "Steel")
                        .machineType("largesamplepress").energy("EU", 512).tier(1)
                        .recipeMap("addon_sample.recipe.large_press").largeRecipe()
                        .structureKeys(List.of("addon_sample.structure.press.1")).build());
    }

    @Override public List<MachineLayout> layouts() {
        var cells = new ArrayList<MachineLayout.Cell>();
        for (int up = 0; up < 2; up++) {
            for (int right = -1; right <= 1; right++) {
                for (int back = 0; back <= 2; back++) {
                    if (right == 0 && up == 0 && back == 0) continue;
                    var role = back == 1 && right == 0
                            ? StructureRole.ENERGY_INPUT : StructureRole.ITEM_FLUID_IO;
                    cells.add(new MachineLayout.Cell(right, up, back, "tank_wall", role));
                }
            }
        }
        return List.of(new MachineLayout("largesamplepress", cells));
    }

    private static RecipeMapDeclaration map(String name, String title, String gui) {
        return new RecipeMapDeclaration(name, title, "addon_sample:textures/gui/machines/" + gui,
                1, 1, 1, 0, 0, 0, 1, true, false, false, false);
    }
}
