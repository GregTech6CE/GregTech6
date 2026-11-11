/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.*;

import java.util.Optional;

/** Fuel value and residue queries. */
public final class FuelMaterialHelper {
    private FuelMaterialHelper() {}

    @Deprecated(since = "1.7.0", forRemoval = false)
    public static Optional<FuelRule> rule(GTMaterial material) {
        if (material == null || !material.isValid()) return Optional.empty();
        return MaterialInfoTool.currentProcess(
                new MaterialKey(material.resolve().getName()), MaterialProcessKeys.FUEL)
                .map(MaterialProcessRule::value);
    }

    public static Optional<FuelRule> rule(MaterialKey material) {
        return GregTechMaterials.processes()
                .findSingle(material, MaterialProcessKeys.FUEL)
                .map(MaterialProcessRule::value);
    }

}
