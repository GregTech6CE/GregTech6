package com.gregtech.gregtech.api.addon;

/**
 * SPDX-License-Identifier: LGPL-3.0-or-later.
 * One addon structure casing. An empty {@code texture} uses the GregTech metal wall,
 * tinted with the material colour. A texture is a path in the addon's own namespace.
 */
public record StructurePartDeclaration(String name, String material, String texture, float hardness) {
    public StructurePartDeclaration {
        if (name == null || !name.matches("[a-z][a-z0-9_]{0,63}"))
            throw new IllegalArgumentException(
                    "Structure part name must be a lower-case registry path: " + name);
        if (material == null || material.isBlank())
            throw new IllegalArgumentException("Structure part '" + name + "' needs a material");
        texture = texture == null ? "" : texture;
        if (!texture.isEmpty() && !texture.matches("[a-z0-9]+(?:[._-][a-z0-9]+)*(?:/[a-z0-9]+(?:[._-][a-z0-9]+)*)*"))
            throw new IllegalArgumentException(
                    "Structure part '" + name + "' texture must be a path in the addon namespace: " + texture);
        if (Float.isNaN(hardness) || hardness <= 0.0F)
            throw new IllegalArgumentException("Structure part '" + name + "' hardness must be positive");
    }
}
