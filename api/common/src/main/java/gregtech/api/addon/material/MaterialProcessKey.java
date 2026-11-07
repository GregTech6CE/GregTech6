/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** Typed identity of a material process or transformation rule. */
public record MaterialProcessKey<T>(DataKey id, Class<T> type) {
    public MaterialProcessKey {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(type, "type");
    }
}
