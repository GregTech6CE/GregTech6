/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.addon.material.MaterialInfoTool;
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
        return MaterialInfoTool.currentProcess(
                        new MaterialKey(material.resolve().getName()), MaterialProcessKeys.ORE_PROCESSING)
                .map(MaterialProcessRule::value)
                .orElseGet(() -> new OreProcessingRule(List.of(), 1, 1));
    }

    public static OreProcessingRule rule(MaterialKey material) {
        return GregTechMaterials.processes()
                .findSingle(material, MaterialProcessKeys.ORE_PROCESSING)
                .map(MaterialProcessRule::value)
                .orElseGet(() -> new OreProcessingRule(List.of(), 1, 1));
    }

    static List<GTMaterial> legacyByproducts(GTMaterial material) {
        return rule(material).byproducts().stream()
                .flatMap(key -> MaterialInfoTool.findMaterial(key).stream())
                .map(GTMaterial::resolve)
                .toList();
    }

    static int legacyOreMultiplier(GTMaterial material) {
        return rule(material).oreMultiplier();
    }

    static int legacyProcessingMultiplier(GTMaterial material) {
        return rule(material).processingMultiplier();
    }
}
