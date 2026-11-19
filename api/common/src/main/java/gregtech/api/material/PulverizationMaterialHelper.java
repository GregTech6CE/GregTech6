/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.*;

import java.util.Optional;

/** Ordinary material recycling and pulverization target queries. */
public final class PulverizationMaterialHelper {
    private PulverizationMaterialHelper() {}

    public static Optional<MaterialTarget> target(MaterialKey material) {
        return GregTechMaterials.processes()
                .findSingle(material, MaterialProcessKeys.PULVERIZING)
                .map(MaterialProcessRule::value)
                .filter(target -> target.amount() > 0);
    }

}
