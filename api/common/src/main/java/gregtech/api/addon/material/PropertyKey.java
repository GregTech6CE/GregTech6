/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** Typed property identity. A catalog rejects the same ID registered with different value types. */
public record PropertyKey<T>(DataKey id, Class<T> type) {
    public PropertyKey {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(type, "type");
    }
}
