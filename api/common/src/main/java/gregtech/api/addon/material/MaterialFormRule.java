/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** One allowed definition-time realization of a material form. */
public record MaterialFormRule(
        FormKey form, MaterialFormKind kind, long materialUnits, boolean requested, String source) {
    public MaterialFormRule {
        java.util.Objects.requireNonNull(form, "form");
        java.util.Objects.requireNonNull(kind, "kind");
        if (materialUnits < 0) throw new IllegalArgumentException("Negative material units for " + form);
        if (source == null || source.isBlank()) throw new IllegalArgumentException("Form rule source is missing");
    }
}
