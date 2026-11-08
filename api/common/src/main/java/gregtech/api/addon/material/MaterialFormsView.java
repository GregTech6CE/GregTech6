/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.Set;

/** Immutable native item, block and fluid bindings published after content registration. */
public interface MaterialFormsView {
    Set<MaterialFormBinding> find(MaterialKey material);

    default Set<MaterialFormBinding> find(MaterialKey material, FormKey form) {
        java.util.Objects.requireNonNull(form, "form");
        return find(material).stream()
                .filter(binding -> binding.form().equals(form))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    default boolean present(MaterialKey material, FormKey form) {
        return !find(material, form).isEmpty();
    }
}
