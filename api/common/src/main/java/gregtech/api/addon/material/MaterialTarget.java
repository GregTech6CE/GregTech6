/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** A material target and amount in GregTech material units. */
public record MaterialTarget(MaterialKey material, long amount) {
    public MaterialTarget {
        java.util.Objects.requireNonNull(material, "material");
        if (amount < 0) throw new IllegalArgumentException("Negative material target amount");
    }
}
