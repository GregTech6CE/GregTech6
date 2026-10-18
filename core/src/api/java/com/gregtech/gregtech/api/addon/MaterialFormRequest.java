package com.gregtech.gregtech.api.addon;

import java.util.Objects;

/** One extra material form. The prefix must already exist; addons cannot declare new prefixes. */
public record MaterialFormRequest(String materialName, String prefixName) {
    public MaterialFormRequest {
        materialName = Objects.requireNonNull(materialName, "material name").trim();
        prefixName = Objects.requireNonNull(prefixName, "prefix name").trim();
        if (materialName.isEmpty() || prefixName.isEmpty())
            throw new IllegalArgumentException("A material form needs a material name and a prefix name");
    }
}
