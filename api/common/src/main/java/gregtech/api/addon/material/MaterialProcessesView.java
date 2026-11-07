/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.List;
import java.util.Set;

/** Immutable queries over material process rules. Process membership does not invent recipes. */
public interface MaterialProcessesView {
    <T> List<MaterialProcessRule<T>> find(MaterialKey material, MaterialProcessKey<T> process);

    default <T> List<MaterialProcessRule<T>> find(
            MaterialKey material, FormKey inputForm, MaterialProcessKey<T> process) {
        return find(material, process).stream()
                .filter(rule -> rule.forms().isEmpty() || rule.forms().contains(inputForm))
                .toList();
    }

    default boolean supports(MaterialKey material, MaterialProcessKey<?> process) {
        return !find(material, process).isEmpty();
    }

    Set<MaterialProcessKey<?>> keys(MaterialKey material);
}
