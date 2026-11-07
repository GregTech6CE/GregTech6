/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.OreProcessingRule;
import java.util.List;

/** Ore byproduct and multiplier queries. */
public final class OreProcessingMaterialHelper {
    private OreProcessingMaterialHelper() {}

    public static OreProcessingRule rule(GTMaterial material) {
        if (material == null || !material.isValid()) return new OreProcessingRule(List.of(), 1, 1);
        List<MaterialKey> byproducts =
                legacyByproducts(material).stream().map(value -> new MaterialKey(value.resolve().getName())).toList();
        int oreMultiplier = legacyOreMultiplier(material);
        int processingMultiplier = legacyProcessingMultiplier(material);
        return new OreProcessingRule(byproducts, Math.max(1, oreMultiplier), Math.max(1, processingMultiplier));
    }

    static List<GTMaterial> legacyByproducts(GTMaterial material) {
        return material.rawByProducts().stream().map(GTMaterial::resolve).toList();
    }

    static int legacyOreMultiplier(GTMaterial material) {
        return material.rawOreMultiplier();
    }

    static int legacyProcessingMultiplier(GTMaterial material) {
        return material.rawOreProcessingMultiplier();
    }
}
