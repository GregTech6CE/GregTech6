/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.Set;

/**
 * One process rule with an immutable form set. The process-specific value remains caller-owned and
 * is not deep-frozen. An empty form set means the rule is not form-specific.
 */
public record MaterialProcessRule<T>(Set<FormKey> forms, T value) {
    public MaterialProcessRule {
        forms = Set.copyOf(forms);
        java.util.Objects.requireNonNull(value, "value");
    }

    public static <T> MaterialProcessRule<T> anyForm(T value) {
        return new MaterialProcessRule<>(Set.of(), value);
    }
}
