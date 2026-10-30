/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import gregtech.api.addon.material.GregTechMaterials;

/**
 * A read-only snapshot of the molten material a crucible controller exposes.
 * The material is a name; resolve it with {@link GregTechMaterials#find(String)}.
 * {@code amount} is in GregTech units and {@code temperature} is in Kelvin.
 * Nothing is removed by reading this, and no block entity is kept.
 * A negative amount or temperature is stored as zero.
 */
public record CrucibleContent(String material, long amount, long temperature) {
    public static final CrucibleContent EMPTY = new CrucibleContent("", 0L, 0L);

    public CrucibleContent {
        if (material == null)
            throw new IllegalArgumentException("Crucible content needs a material name; pass an empty string when empty");
        amount = Math.max(0L, amount);
        temperature = Math.max(0L, temperature);
    }

    /** True when a material name and a positive amount are present. */
    public boolean present() { return !material.isEmpty() && amount > 0L; }
}
