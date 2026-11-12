/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.*;

import java.util.Optional;

/** Smelting queries for callers that must not depend on the legacy GTMaterial field layout. */
public final class SmeltingMaterialHelper {
    private SmeltingMaterialHelper() {}

    @Deprecated(since = "1.7.0", forRemoval = false)
    public static Optional<MaterialTarget> target(GTMaterial material) {
        if (material == null || !material.isValid()) return Optional.empty();
        return MaterialInfoTool.currentProcess(
                        new MaterialKey(material.resolve().getName()), MaterialProcessKeys.SMELTING)
                .map(MaterialProcessRule::value)
                .filter(target -> target.amount() > 0);
    }

    public static Optional<MaterialTarget> target(MaterialKey material) {
        return GregTechMaterials.processes()
                .findSingle(material, MaterialProcessKeys.SMELTING)
                .map(MaterialProcessRule::value)
                .filter(target -> target.amount() > 0);
    }

    @Deprecated(since = "1.7.0", forRemoval = false)
    public static boolean isMaterialSmeltable(GTMaterial material) {
        return target(material).isPresent();
    }

    public static boolean isMaterialSmeltable(MaterialKey material) {
        return target(material).isPresent();
    }

}
