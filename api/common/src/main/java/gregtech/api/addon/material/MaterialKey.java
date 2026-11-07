/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import gregtech.api.addon.AddonNames;

/** A resolved canonical material name, preserving existing case and save identities. */
public record MaterialKey(String name) {
    public MaterialKey {
        if (!AddonNames.matches(AddonNames.MATERIAL, name))
            throw new IllegalArgumentException("Invalid material key: " + name);
    }
}
