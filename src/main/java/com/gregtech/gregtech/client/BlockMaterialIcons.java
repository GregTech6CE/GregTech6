package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.block.stone.GTStoneBlock;
import net.minecraft.resources.ResourceLocation;

/** Block material icon paths under {@code textures/block/material_icons/{set}/{prefix}.png}. */
public final class BlockMaterialIcons {
    private BlockMaterialIcons() {}

    public static ResourceLocation baseTexture(MaterialTextureSet set, BlockMaterialPrefix prefix) {
        return texture(set, prefix, false);
    }

    public static ResourceLocation overlayTexture(MaterialTextureSet set, BlockMaterialPrefix prefix) {
        return texture(set, prefix, true);
    }

    public static ResourceLocation texture(MaterialTextureSet set, BlockMaterialPrefix prefix, boolean overlay) {
        String file = prefix.getTextureFileName() + (overlay ? "_overlay" : "");
        return GregTech.id("block/material_icons/" + set.folder() + "/" + file);
    }

    public static ResourceLocation sharedModelLocation(MaterialTextureSet set, BlockMaterialPrefix prefix) {
        return GregTech.id("block/material/" + set.folder() + "/" + prefix.getTextureFileName());
    }

    public static ResourceLocation crateBottomTexture() {
        return GregTech.id("block/iconsets/crate");
    }

    public static String sharedModelJson(MaterialTextureSet set, BlockMaterialPrefix prefix) {
        ResourceLocation layer0 = baseTexture(set, prefix);
        ResourceLocation layer1 = overlayTexture(set, prefix);
        String particle = "minecraft:block/stone";
        if (prefix.isCrate()) {
            ResourceLocation hull = crateBottomTexture();
            return """
                    {
                      "loader": "forge:composite",
                      "children": {
                        "layer0": {
                          "parent": "minecraft:block/cube_all",
                          "textures": { "all": "%s", "particle": "%s" },
                          "render_type": "minecraft:solid"
                        },
                        "layer1": {
                          "parent": "minecraft:block/cube_all",
                          "textures": { "all": "%s" },
                          "render_type": "minecraft:solid"
                        },
                        "layer2": {
                          "parent": "minecraft:block/cube_all",
                          "textures": { "all": "%s" },
                          "render_type": "minecraft:cutout"
                        }
                      }
                    }
                    """.formatted(hull, particle, layer0, layer1);
        }
        return """
                {
                  "loader": "forge:composite",
                  "children": {
                    "layer0": {
                      "parent": "minecraft:block/cube_all",
                      "textures": { "all": "%s", "particle": "%s" },
                      "render_type": "minecraft:solid"
                    },
                    "layer1": {
                      "parent": "minecraft:block/cube_all",
                      "textures": { "all": "%s" },
                      "render_type": "minecraft:cutout"
                    }
                  }
                }
                """.formatted(layer0, particle, layer1);
    }

    public static ResourceLocation stoneTexture(GTStoneBlockPaths paths) {
        return GregTech.id("block/stones/" + paths.folder() + "/" + paths.variantTexture());
    }

    public record GTStoneBlockPaths(String folder, String variantTexture) {}

    public static GTStoneBlockPaths stonePaths(GTStoneBlock block) {
        return new GTStoneBlockPaths(block.stoneType().textureFolder(), block.variant().textureName());
    }
}
