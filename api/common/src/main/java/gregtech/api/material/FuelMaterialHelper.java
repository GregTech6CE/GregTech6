/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.AddonNames;
import gregtech.api.addon.material.*;

import java.util.Optional;

/** Fuel value and residue queries. */
public final class FuelMaterialHelper {
    private FuelMaterialHelper() {}

    @Deprecated(since = "1.7.0", forRemoval = false)
    public static Optional<FuelRule> rule(GTMaterial material) {
        if (material == null || !material.isValid()) return Optional.empty();
        String name = material.resolve().getName();
        if (!AddonNames.matches(AddonNames.MATERIAL, name)) return Optional.empty();
        return MaterialInfoTool.currentProcess(
                new MaterialKey(name), MaterialProcessKeys.FUEL)
                .map(MaterialProcessRule::value);
    }

    public static Optional<FuelRule> rule(MaterialKey material) {
        return GregTechMaterials.processes()
                .findSingle(material, MaterialProcessKeys.FUEL)
                .map(MaterialProcessRule::value);
    }

}
