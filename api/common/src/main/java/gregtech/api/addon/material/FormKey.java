/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** A resolved existing prefix name. Construction alone does not prove that a form exists. */
public record FormKey(String name) {
    public FormKey {
        if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("Invalid form key: " + name);
    }
}
