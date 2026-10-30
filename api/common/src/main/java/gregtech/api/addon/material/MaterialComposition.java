/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import gregtech.api.addon.AddonNames;
import gregtech.api.material.GTValues;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Composition of one addon material, named at link time.
 * {@code amount} on each part is GregTech units, the same unit as {@link PrefixDeclaration#amount()}:
 * a positive multiple of {@link GTValues#U72}, so one ingot is {@code GTValues.U} and 3U is {@code 3 * GTValues.U}.
 * {@code divider} is stored as the composition divider. When {@code crucible} is true,
 * GregTech also adds one crucible reaction whose ratios are {@code amount / U} and whose yield is {@code divider};
 * such parts must therefore be whole multiples of {@link GTValues#U}.
 * Composition and crucible participation alone do not permit automatic machine decomposition;
 * opt in with {@link MaterialDeclaration.Builder#centrifuge()} or
 * {@link MaterialDeclaration.Builder#electrolyzer()}.
 */
public record MaterialComposition(String material, long divider, List<Part> parts, boolean crucible) {
    public MaterialComposition {
        if (!AddonNames.matches(AddonNames.MATERIAL, material))
            throw new IllegalArgumentException("Use a material name: " + material);
        if (divider <= 0) throw new IllegalArgumentException("Composition divider must be positive: " + material);
        if (parts == null) throw new IllegalArgumentException("Composition parts are missing: " + material);
        if (parts.isEmpty()) throw new IllegalArgumentException("Composition needs components: " + material);
        var copied = new ArrayList<Part>(parts.size());
        var seen = new HashSet<String>();
        for (Part part : parts) {
            if (part == null) throw new IllegalArgumentException("Composition has a null part: " + material);
            if (!seen.add(part.material()))
                throw new IllegalArgumentException("Repeats component '" + part.material() + "' in " + material);
            copied.add(part);
        }
        parts = List.copyOf(copied);
        if (crucible) {
            for (Part part : parts) {
                if (part.amount() % GTValues.U != 0)
                    throw new IllegalArgumentException(
                            "Crucible composition amounts must be whole multiples of U (ratios are integers): "
                                    + material);
            }
        }
    }

    /** One component. {@code amount} is GregTech units, a positive multiple of {@link GTValues#U72}. */
    public record Part(String material, long amount) {
        public Part {
            if (!AddonNames.matches(AddonNames.MATERIAL, material))
                throw new IllegalArgumentException("Use a material name: " + material);
            if (amount <= 0 || amount % GTValues.U72 != 0)
                throw new IllegalArgumentException("Composition amount must be a positive multiple of U72"
                        + " (one ingot is GTValues.U; a tiny dust is GTValues.U9): " + material);
        }
    }
}
