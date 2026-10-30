/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import gregtech.api.addon.AddonNames;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * One explicit crucible alloy row. {@code yield} and each {@code ratio} are coefficients, not GregTech units:
 * 3 copper and 1 tin producing 4 bronze is yield 4 with ratios 3 and 1.
 */
public record CrucibleReactionDeclaration(String output, long yield, List<Part> parts) {
    public CrucibleReactionDeclaration {
        if (!AddonNames.matches(AddonNames.MATERIAL, output))
            throw new IllegalArgumentException("Use a material name: " + output);
        if (yield <= 0) throw new IllegalArgumentException("Crucible yield must be positive: " + output);
        if (parts == null) throw new IllegalArgumentException("Crucible reaction inputs are missing: " + output);
        if (parts.isEmpty()) throw new IllegalArgumentException("Crucible reaction needs inputs: " + output);
        var copied = new ArrayList<Part>(parts.size());
        var seen = new HashSet<String>();
        for (Part part : parts) {
            if (part == null) throw new IllegalArgumentException("Crucible reaction has a null input: " + output);
            if (!seen.add(part.material()))
                throw new IllegalArgumentException("Repeats input '" + part.material() + "' in " + output);
            copied.add(part);
        }
        parts = List.copyOf(copied);
    }

    /** One input. {@code ratio} is the coefficient consumed per reaction count. */
    public record Part(String material, long ratio) {
        public Part {
            if (!AddonNames.matches(AddonNames.MATERIAL, material))
                throw new IllegalArgumentException("Use a material name: " + material);
            if (ratio <= 0) throw new IllegalArgumentException("Crucible ratio must be positive: " + material);
        }
    }
}
