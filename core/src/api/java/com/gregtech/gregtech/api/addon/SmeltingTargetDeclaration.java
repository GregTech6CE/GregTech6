package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.material.GTValues;

import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * What an addon material becomes once a crucible reaches its melting point.
 * {@code amount} is the output for each {@link GTValues#U} of input. {@code U} is one to one.
 * Declaring a target also marks the material as melting, so the crucible smelting page lists it.
 */
public record SmeltingTargetDeclaration(String material, String target, long amount) {
    public SmeltingTargetDeclaration {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(target, "target");
        if (!material.matches("[A-Za-z][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("Use a material name: " + material);
        if (!target.matches("[A-Za-z][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("Use a material name: " + target);
        if (amount <= 0) throw new IllegalArgumentException("Smelting amount must be positive: " + material);
    }
}
