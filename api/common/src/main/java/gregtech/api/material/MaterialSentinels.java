/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import org.jetbrains.annotations.ApiStatus;

/**
 * Host-side sentinels. The instances are frozen, so every mutator throws
 * {@link IllegalStateException}; addons must not configure them.
 * <p>
 * Internal: not part of the addon contract. Sentinel lookup must not initialize the entire
 * built-in material alias table.
 */
@ApiStatus.Internal
public final class MaterialSentinels {
    private MaterialSentinels() {}

    public static final GTMaterial INVALID = new GTMaterial(-1, "NULL", "Null", 0xFF00FF)
            .put(MaterialProperty.HIDDEN)
            .freeze();

    public static final GTMaterial EMPTY = new GTMaterial(0, "Empty", "Empty", 0xFFFFFF)
            .setAtomicProperties(AtomicProperties.ZERO)
            .put(MaterialProperty.HIDDEN)
            .freeze();
}
