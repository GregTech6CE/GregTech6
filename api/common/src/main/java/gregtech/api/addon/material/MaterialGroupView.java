/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** Immutable group membership; membership alone does not authorize substitution in any process. */
public record MaterialGroupView(MaterialGroupKey key, String displayName, java.util.Set<MaterialKey> members) {
    public MaterialGroupView {
        java.util.Objects.requireNonNull(key);
        java.util.Objects.requireNonNull(displayName);
        members = java.util.Set.copyOf(members);
    }
}
