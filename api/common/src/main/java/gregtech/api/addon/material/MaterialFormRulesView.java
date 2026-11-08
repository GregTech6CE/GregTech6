/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.Set;

/**
 * Immutable definition-time form rules. A rule does not prove that native content was registered.
 *
 * <p>The untyped {@link #forms()} and {@link #allows} queries are the legacy item-form view.
 * {@link #rules()} includes item, block and fluid rules.</p>
 */
public interface MaterialFormRulesView {
    /** Legacy item-form identities. */
    Set<FormKey> forms();

    /** Legacy item-form identities for one material. */
    Set<FormKey> forms(MaterialKey material);

    /** Answers the legacy item-form view; use {@link #rules(MaterialKey)} for all form kinds. */
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
