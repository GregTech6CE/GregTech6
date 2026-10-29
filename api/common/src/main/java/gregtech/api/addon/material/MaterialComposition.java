/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;




import gregtech.api.addon.*;

import gregtech.api.material.GTValues;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Composition of one addon material, named at link time.
 * {@code amount} on each part is GregTech units, the same unit as {@link PrefixDeclaration#amount()}:
 * a positive multiple of {@link GTValues#U}, so one ingot is {@code GTValues.U} and 3U is {@code 3 * GTValues.U}.
 * {@code divider} is stored as the composition divider. When {@code crucible} is true,
 * GregTech also adds one crucible reaction whose ratios are {@code amount / U} and whose yield is {@code divider}.
 * Composition and crucible participation alone do not permit automatic machine decomposition;
 * opt in with {@link MaterialDeclaration.Builder#centrifuge()} or
 * {@link MaterialDeclaration.Builder#electrolyzer()}.
 */
public record MaterialComposition(String material, long divider, List<Part> parts, boolean crucible) {
    public MaterialComposition {
        Objects.requireNonNull(material, "material");
        if (!AddonNames.matches(AddonNames.MATERIAL, material))
            throw new IllegalArgumentException("Use a material name: " + material);
        if (divider <= 0) throw new IllegalArgumentException("Composition divider must be positive: " + material);
        Objects.requireNonNull(parts, "parts");
        if (parts.isEmpty()) throw new IllegalArgumentException("Composition needs components: " + material);
        var copied = new ArrayList<Part>(parts.size());
        var seen = new HashSet<String>();
        for (Part part : parts) {
            Part checked = Objects.requireNonNull(part, "part");
            if (!seen.add(checked.material()))
                throw new IllegalArgumentException("Repeats component '" + checked.material() + "' in " + material);
            copied.add(checked);
        }
        parts = List.copyOf(copied);
    }

    /** One component. {@code amount} is GregTech units, a positive multiple of {@link GTValues#U}. */
    public record Part(String material, long amount) {
        public Part {
            Objects.requireNonNull(material, "material");
            if (!AddonNames.matches(AddonNames.MATERIAL, material))
                throw new IllegalArgumentException("Use a material name: " + material);
            if (amount <= 0 || amount % GTValues.U != 0)
                throw new IllegalArgumentException(
                        "Composition amount must be a positive multiple of U (one ingot is GTValues.U): " + material);
        }
    }
}
