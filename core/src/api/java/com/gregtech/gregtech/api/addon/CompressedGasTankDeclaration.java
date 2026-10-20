package com.gregtech.gregtech.api.addon;

/**
 * SPDX-License-Identifier: LGPL-3.0-or-later.
 * One compressed gas drum. Capacity is the stored millibuckets, not a multiplier.
 * The drum is always gas proof. Acid, plasma and magic are the pressure rating.
 * The temperature ceiling is the material melting point, resolved when machines install.
 */
public record CompressedGasTankDeclaration(
        String id,
        String material,
        int capacity,
        boolean acidProof,
        boolean plasmaProof,
        boolean magicProof
) {
    public static final int MAX_CAPACITY = 1_024_000_000;

    public CompressedGasTankDeclaration {
        if (id == null || !id.matches("[a-z][a-z0-9_]{0,63}"))
            throw new IllegalArgumentException(
                    "Compressed gas tank name must be a lower-case registry path: " + id);
        if (material == null || material.isBlank())
            throw new IllegalArgumentException("Compressed gas tank '" + id + "' needs a material");
        if (capacity < 1 || capacity > MAX_CAPACITY)
            throw new IllegalArgumentException("Compressed gas tank '" + id
                    + "' capacity must be 1.." + MAX_CAPACITY);
    }

    /** Compressed drums store gas. This proof is not optional. */
    public boolean gasProof() { return true; }
}
