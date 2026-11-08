/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** One native registered representation of a material form. */
public record MaterialFormBinding(
        MaterialKey material, FormKey form, MaterialFormKind kind, String id, long materialUnits) {
    public MaterialFormBinding {
        java.util.Objects.requireNonNull(material, "material");
        java.util.Objects.requireNonNull(form, "form");
        java.util.Objects.requireNonNull(kind, "kind");
        if (id == null || !id.contains(":")) throw new IllegalArgumentException("Form binding needs a namespaced id");
        if (materialUnits < 0) throw new IllegalArgumentException("Negative material units for " + form);
    }
}
