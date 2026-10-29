/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;




import gregtech.api.addon.*;

import gregtech.api.material.GTValues;

import java.util.Objects;

/**
 * What an addon material becomes once a crucible reaches its melting point.
 * {@code amount} is GregTech units produced for each {@link GTValues#U} of input, the same unit as a
 * composition part. Pass {@link GTValues#U} for one to one. A fraction such as {@link GTValues#U4} is allowed.
 * Declaring a target also marks the material as melting, so the crucible smelting page lists it.
 */
public record SmeltingTargetDeclaration(String material, String target, long amount) {
    public SmeltingTargetDeclaration {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(target, "target");
        if (!AddonNames.matches(AddonNames.MATERIAL, material))
            throw new IllegalArgumentException("Use a material name: " + material);
        if (!AddonNames.matches(AddonNames.MATERIAL, target))
            throw new IllegalArgumentException("Use a material name: " + target);
        if (amount <= 0) throw new IllegalArgumentException("Smelting amount must be positive: " + material);
    }
}
