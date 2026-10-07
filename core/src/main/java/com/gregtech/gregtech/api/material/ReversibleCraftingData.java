/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from CR.shaped, OreDictItemData and OreDictManager.setItemData_. */
package com.gregtech.gregtech.api.material;

import com.gregtech.gregtech.data.MaterialGroups;
import com.gregtech.gregtech.data.generated.GT6Materials;
import java.util.*;

/** CR.REV: sum known consumed inputs, then divide each component by the output count. */
public final class ReversibleCraftingData {
    private ReversibleCraftingData() {}

    public static ItemComposition perItem(Collection<MaterialComponent> inputs, int outputCount, String source) {
        if (outputCount < 1) throw new IllegalArgumentException("Empty crafting output");
        var totals = new LinkedHashMap<GTMaterial, Long>();
        for (var input : inputs) {
            if (input == null || input.amount() <= 0) continue; // Unknown ingredients/tools add no material.
            GTMaterial target = reversingOutput(input.material());
            if (target.isValid()) totals.merge(target, input.amount(), Math::addExact);
        }
        var components = totals.entrySet().stream()
                .sorted(Map.Entry.<GTMaterial, Long>comparingByValue().reversed())
                .filter(e -> e.getValue() / outputCount > 0)
                .map(e -> MaterialComponent.of(e.getKey(), e.getValue() / outputCount)).toList();
        return new ItemComposition(null, components, source, true);
    }

    /** Original ANY reversing groups have concrete output targets; reuse the existing identities. */
    private static GTMaterial reversingOutput(GTMaterial material) {
        if (material == MaterialGroups.Fe || material == MaterialGroups.Iron || material == MaterialGroups.MagicIron) return GT6Materials.Elements.Fe;
        if (material == MaterialGroups.Steel) return GT6Materials.Compounds.Steel;
        if (material == MaterialGroups.Cu) return GT6Materials.Elements.Cu;
        if (material == MaterialGroups.W) return GT6Materials.Elements.W;
        if (material == MaterialGroups.Wood) return GT6Materials.Woods.Wood;
        if (material == MaterialGroups.Stone) return GTMaterialRegistry.get("Stone");
        return material.resolve();
    }
}
