/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import gregtech.api.addon.AddonNames;

/**
 * Removes the crucible smelting target of a material, including a built-in one.
 * Applied after every addon's {@link SmeltingTargetDeclaration}s. The material keeps its
 * identity in the crucible instead of turning into another material once it melts.
 */
public record SmeltingTargetRemoval(String material) {
    public SmeltingTargetRemoval {
        if (!AddonNames.matches(AddonNames.MATERIAL, material))
            throw new IllegalArgumentException("Use a material name: " + material);
    }
}
