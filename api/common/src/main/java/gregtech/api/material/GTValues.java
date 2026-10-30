/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

/**
 * GregTech material units ({@code CS.U} in GT6).
 * Prefix amounts, composition parts, smelting targets and {@code GregTechMaterials.Component}
 * all use this unit. One ingot is {@link #U}. Prefix, composition and smelting-target amounts
 * are positive multiples of {@link #U72}, the finest fraction the unit defines.
 */
public final class GTValues {
    /** One ingot or one dust. */
    public static final long U = 648_648_000L;
    public static final long U2 = U / 2;
    public static final long U3 = U / 3;
    public static final long U4 = U / 4;
    public static final long U9 = U / 9;
    public static final long U72 = U / 72;

    private GTValues() {}
}
