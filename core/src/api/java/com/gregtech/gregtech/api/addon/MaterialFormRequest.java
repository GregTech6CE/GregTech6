/* SPDX-License-Identifier: LGPL-3.0-or-later */

package com.gregtech.gregtech.api.addon;

import java.util.Objects;

/** One extra material form of a prefix GregTech already generates. A new prefix is a {@link PrefixDeclaration}. */
public record MaterialFormRequest(String materialName, String prefixName) {
    public MaterialFormRequest {
        materialName = Objects.requireNonNull(materialName, "material name").trim();
        prefixName = Objects.requireNonNull(prefixName, "prefix name").trim();
        if (materialName.isEmpty() || prefixName.isEmpty())
            throw new IllegalArgumentException("A material form needs a material name and a prefix name");
    }
}
