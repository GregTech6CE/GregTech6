/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.FuelRule;
import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.addon.material.MaterialInfoTool;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.addon.material.MaterialProcessRule;
import gregtech.api.addon.material.MaterialTarget;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

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

    static long legacyBurnTime(GTMaterial material) {
        return rule(material).map(FuelRule::burnTime).orElse(0L);
    }

    static @Nullable GTMaterial legacyResidue(GTMaterial material) {
        return rule(material)
                .flatMap(FuelRule::residue)
                .flatMap(target -> MaterialInfoTool.findMaterial(target.material()))
                .map(GTMaterial::resolve)
                .orElse(null);
    }

    static long legacyResidueAmount(GTMaterial material) {
        return rule(material).flatMap(FuelRule::residue).map(MaterialTarget::amount).orElse(0L);
    }
}
