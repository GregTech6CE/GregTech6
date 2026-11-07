/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.FuelRule;
import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.addon.material.MaterialTarget;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Fuel value and residue queries. */
public final class FuelMaterialHelper {
    private FuelMaterialHelper() {}

    public static Optional<FuelRule> rule(GTMaterial material) {
        if (material == null || !material.isValid()) return Optional.empty();
        long burnTime = legacyBurnTime(material);
        GTMaterial residue = legacyResidue(material);
        long residueAmount = legacyResidueAmount(material);
        Optional<MaterialTarget> output = residue != null && residue.isValid() && residueAmount > 0
                ? Optional.of(
                        new MaterialTarget(new MaterialKey(residue.resolve().getName()), residueAmount))
                : Optional.empty();
        return burnTime > 0 || output.isPresent() ? Optional.of(new FuelRule(burnTime, output)) : Optional.empty();
    }

    public static Optional<FuelRule> rule(MaterialKey material) {
        var rules = GregTechMaterials.processes().find(material, MaterialProcessKeys.FUEL);
        return rules.isEmpty() ? Optional.empty() : Optional.of(rules.get(0).value());
    }

    static long legacyBurnTime(GTMaterial material) {
        return material.rawFurnaceBurnTime();
    }

    static @Nullable GTMaterial legacyResidue(GTMaterial material) {
        return material.rawTargetBurningMaterial();
    }

    static long legacyResidueAmount(GTMaterial material) {
        return material.rawTargetBurningAmount();
    }
}
