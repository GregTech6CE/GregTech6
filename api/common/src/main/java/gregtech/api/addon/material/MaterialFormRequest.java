/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import gregtech.api.addon.AddonNames;

/** One extra material form of a prefix GregTech already generates. A new prefix is a {@link PrefixDeclaration}. */
public record MaterialFormRequest(String materialName, String prefixName) {
    public MaterialFormRequest {
        if (materialName == null || prefixName == null || materialName.isEmpty() || prefixName.isEmpty())
            throw new IllegalArgumentException("A material form needs a material name and a prefix name");
        if (!AddonNames.matches(AddonNames.MATERIAL, materialName))
            throw new IllegalArgumentException("Use a material name: " + materialName);
        if (!AddonNames.matches(AddonNames.PREFIX_NAME, prefixName))
            throw new IllegalArgumentException("Invalid prefix name: " + prefixName);
    }
}
