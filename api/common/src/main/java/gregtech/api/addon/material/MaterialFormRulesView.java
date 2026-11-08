/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.Set;

/** Immutable definition-time form rules. A rule does not prove that native content was registered. */
public interface MaterialFormRulesView {
    Set<FormKey> forms();

    Set<FormKey> forms(MaterialKey material);

    boolean allows(MaterialKey material, FormKey form);

    default Set<MaterialFormRule> rules() {
        return forms().stream()
                .map(form -> new MaterialFormRule(form, MaterialFormKind.ITEM, 0, false, "compatibility view"))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    default Set<MaterialFormRule> rules(MaterialKey material) {
        return forms(material).stream()
                .map(form -> new MaterialFormRule(form, MaterialFormKind.ITEM, 0, false, "compatibility view"))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
