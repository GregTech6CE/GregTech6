/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.Set;

/** One immutable process rule. An empty form set means the rule is not form-specific. */
public record MaterialProcessRule<T>(Set<FormKey> forms, T value) {
    public MaterialProcessRule {
        forms = Set.copyOf(forms);
        java.util.Objects.requireNonNull(value, "value");
    }

    public static <T> MaterialProcessRule<T> anyForm(T value) {
        return new MaterialProcessRule<>(Set.of(), value);
    }
}
