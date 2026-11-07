/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.addon.material.MaterialProcessRule;
import gregtech.api.addon.material.MaterialTarget;
import java.util.Optional;

/** Ordinary material recycling and pulverization target queries. */
public final class PulverizationMaterialHelper {
    private PulverizationMaterialHelper() {}

    @Deprecated(since = "1.7.0", forRemoval = false)
    public static Optional<MaterialTarget> target(GTMaterial material) {
        if (material == null || !material.isValid()) return Optional.empty();
        GTMaterial target = legacyTarget(material);
        long amount = legacyAmount(material);
        if (!target.isValid() || amount <= 0) return Optional.empty();
        return Optional.of(new MaterialTarget(new MaterialKey(target.getName()), amount));
    }

    public static Optional<MaterialTarget> target(MaterialKey material) {
        return GregTechMaterials.processes()
                .findSingle(material, MaterialProcessKeys.PULVERIZING)
                .map(MaterialProcessRule::value);
    }

    static GTMaterial legacyTarget(GTMaterial material) {
        return material.rawTargetPulverMaterial().resolve();
    }

    static long legacyAmount(GTMaterial material) {
        return material.rawTargetPulverAmount();
    }
}
