/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.*;

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

}
