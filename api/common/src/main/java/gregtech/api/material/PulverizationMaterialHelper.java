/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.*;

import java.util.Optional;

/** Ordinary material recycling and pulverization target queries. */
public final class PulverizationMaterialHelper {
    private PulverizationMaterialHelper() {}

    @Deprecated(since = "1.7.0", forRemoval = false)
    public static Optional<MaterialTarget> target(GTMaterial material) {
        if (material == null || !material.isValid()) return Optional.empty();
        return MaterialInfoTool.currentProcess(
                        new MaterialKey(material.resolve().getName()), MaterialProcessKeys.PULVERIZING)
                .map(MaterialProcessRule::value)
                .filter(target -> target.amount() > 0);
    }

    public static Optional<MaterialTarget> target(MaterialKey material) {
        return GregTechMaterials.processes()
                .findSingle(material, MaterialProcessKeys.PULVERIZING)
                .map(MaterialProcessRule::value)
                .filter(target -> target.amount() > 0);
    }

}
