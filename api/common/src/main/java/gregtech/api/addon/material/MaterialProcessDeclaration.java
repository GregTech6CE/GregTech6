/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.Set;

/** An addon-owned process rule attached to an existing or addon-defined real material. */
public record MaterialProcessDeclaration<T>(
        String materialName, MaterialProcessKey<T> process, Set<FormKey> forms, T value) {
    public MaterialProcessDeclaration {
        if (materialName == null || materialName.isBlank())
            throw new IllegalArgumentException("Missing process material");
        java.util.Objects.requireNonNull(process, "process");
        forms = Set.copyOf(forms);
        java.util.Objects.requireNonNull(value, "value");
        if (!process.type().isInstance(value))
            throw new IllegalArgumentException("Process value type mismatch for " + process.id());
    }
}
