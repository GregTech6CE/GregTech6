/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.Set;

/** A descriptive material tag and the same-kind tags whose members it includes.
 * Membership never grants form generation, substitution or processing behavior. */
public record MaterialTagDeclaration(DataKey tag, Set<DataKey> includes) {
    public MaterialTagDeclaration {
        java.util.Objects.requireNonNull(tag, "tag");
        includes = Set.copyOf(includes);
    }
}
