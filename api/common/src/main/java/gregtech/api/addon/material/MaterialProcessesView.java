/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.List;
import java.util.Set;

/** Immutable queries over material process rules. Process membership does not invent recipes. */
public interface MaterialProcessesView {
    <T> List<MaterialProcessRule<T>> find(MaterialKey material, MaterialProcessKey<T> process);

    /**
     * Resolves the single rule declared by {@code process}. Multi-valued processes and missing
     * rules return empty; registration rejects zero or multiple rules for a single-valued process.
     */
    default <T> java.util.Optional<MaterialProcessRule<T>> findSingle(
            MaterialKey material, MaterialProcessKey<T> process) {
        if (process.cardinality() != MaterialProcessCardinality.SINGLE) return java.util.Optional.empty();
        var rules = find(material, process);
        return rules.size() == 1 ? java.util.Optional.of(rules.get(0)) : java.util.Optional.empty();
    }

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
