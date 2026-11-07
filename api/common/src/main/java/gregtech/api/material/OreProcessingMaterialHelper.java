/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.addon.material.MaterialProcessRule;
import gregtech.api.addon.material.OreProcessingRule;
import java.util.List;

/** Ore byproduct and multiplier queries. */
public final class OreProcessingMaterialHelper {
    private OreProcessingMaterialHelper() {}

    @Deprecated(since = "1.7.0", forRemoval = false)
    public static OreProcessingRule rule(GTMaterial material) {
        if (material == null || !material.isValid()) return new OreProcessingRule(List.of(), 1, 1);
        List<MaterialKey> byproducts = legacyByproducts(material).stream()
                .map(value -> new MaterialKey(value.resolve().getName()))
                .toList();
        int oreMultiplier = legacyOreMultiplier(material);
        int processingMultiplier = legacyProcessingMultiplier(material);
        return new OreProcessingRule(byproducts, Math.max(1, oreMultiplier), Math.max(1, processingMultiplier));
    }

    public static OreProcessingRule rule(MaterialKey material) {
        return GregTechMaterials.processes()
                .findSingle(material, MaterialProcessKeys.ORE_PROCESSING)
                .map(MaterialProcessRule::value)
                .orElseGet(() -> new OreProcessingRule(List.of(), 1, 1));
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
