/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** A resolved canonical material name, preserving existing case and save identities. */
public record MaterialKey(String name) {
    public MaterialKey {
        if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("Invalid material key: " + name);
    }
}
