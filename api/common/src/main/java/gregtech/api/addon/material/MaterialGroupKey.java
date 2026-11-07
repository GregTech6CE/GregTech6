/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** Substitution-group key, deliberately distinct from classification and real material keys. */
public record MaterialGroupKey(DataKey id) {
    public MaterialGroupKey { java.util.Objects.requireNonNull(id, "id"); }
}
