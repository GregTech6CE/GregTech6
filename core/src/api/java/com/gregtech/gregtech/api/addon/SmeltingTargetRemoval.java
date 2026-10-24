/* SPDX-License-Identifier: LGPL-3.0-or-later */

package com.gregtech.gregtech.api.addon;

import java.util.Objects;

/**
 * Removes the crucible smelting target of a material, including a built-in one.
 * Applied after every addon's {@link SmeltingTargetDeclaration}s. The material keeps its
 * identity in the crucible instead of turning into another material once it melts.
 */
public record SmeltingTargetRemoval(String material) {
    public SmeltingTargetRemoval {
        Objects.requireNonNull(material, "material");
        if (!material.matches("[A-Za-z][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("Use a material name: " + material);
    }
}
