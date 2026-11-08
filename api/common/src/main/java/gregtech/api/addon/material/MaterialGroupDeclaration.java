/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.Set;

/** A substitution group composed of real materials and other substitution groups.
 * Membership is descriptive; each consuming recipe still decides whether a process permits substitution. */
public record MaterialGroupDeclaration(
        MaterialGroupKey key,
        String displayName,
        Set<MaterialKey> materials,
        Set<MaterialGroupKey> includedGroups) {
    public MaterialGroupDeclaration {
        java.util.Objects.requireNonNull(key, "key");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("Missing material group name");
        materials = Set.copyOf(materials);
        includedGroups = Set.copyOf(includedGroups);
    }
}
