/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.Set;

/** Immutable definition-time item-form rules. A rule does not prove that an item was registered. */
public interface MaterialFormRulesView {
    Set<FormKey> forms();

    Set<FormKey> forms(MaterialKey material);

    boolean allows(MaterialKey material, FormKey form);
}
