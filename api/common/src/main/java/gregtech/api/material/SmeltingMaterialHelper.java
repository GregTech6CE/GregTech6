/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialTarget;
import java.util.Optional;

/** Smelting queries for callers that must not depend on the legacy GTMaterial field layout. */
public final class SmeltingMaterialHelper {
    private SmeltingMaterialHelper() {}

    public static Optional<MaterialTarget> target(GTMaterial material) {
        if (material == null || !material.isValid()) return Optional.empty();
        GTMaterial target = legacyTarget(material);
        long amount = legacyAmount(material);
        if (!target.isValid() || amount <= 0) return Optional.empty();
        return Optional.of(new MaterialTarget(new MaterialKey(target.getName()), amount));
    }

    public static boolean isMaterialSmeltable(GTMaterial material) {
        return target(material).isPresent();
    }

    static GTMaterial legacyTarget(GTMaterial material) {
        return material.rawTargetSmeltingMaterial().resolve();
    }

    static long legacyAmount(GTMaterial material) {
        return material.rawTargetSmeltingAmount();
    }
}
