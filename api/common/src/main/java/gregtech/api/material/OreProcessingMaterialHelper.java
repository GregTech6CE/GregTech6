/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.*;

import java.util.List;

/** Ore byproduct and multiplier queries. */
public final class OreProcessingMaterialHelper {
    private OreProcessingMaterialHelper() {}

    public static OreProcessingRule rule(MaterialKey material) {
        return GregTechMaterials.processes()
                .findSingle(material, MaterialProcessKeys.ORE_PROCESSING)
                .map(MaterialProcessRule::value)
                .orElseGet(() -> new OreProcessingRule(List.of(), 1, 1));
    }

}
