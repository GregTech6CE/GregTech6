/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import gregtech.api.addon.AddonNames;

/** A resolved existing prefix name. Construction alone does not prove that a form exists. */
public record FormKey(String name) {
    public FormKey {
        if (!AddonNames.matches(AddonNames.MATERIAL, name))
            throw new IllegalArgumentException("Invalid form key: " + name);
    }
}
