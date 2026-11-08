/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** Immutable intrinsic material facts. Substitution groups are queried separately from real materials. */
public interface MaterialView {
    MaterialKey key();
    MaterialInfo info();
    <T> java.util.Optional<T> property(PropertyKey<T> key);
    java.util.Set<DataKey> flags();
}
