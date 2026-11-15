package gregtech.client;


import gregtech.api.material.MaterialTextureSet;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.platform.Mc;
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
        return Mc.gregtechId("block/material_icons/" + set.folder() + "/" + file);
    }

    public static ResourceLocation addonModel(BlockMaterialPrefix prefix) {
        return Mc.gregtechId("block/material/addon/" + prefix.getTextureFileName());
    }

    public static ResourceLocation sharedModelLocation(MaterialTextureSet set, BlockMaterialPrefix prefix) {
        return Mc.gregtechId("block/material/" + set.folder() + "/" + prefix.getTextureFileName());
    }

    /**
     * Shared block model. Sets without an overlay sprite get a plain {@code cube_all} model; the
     * remaining ones keep the two-layer composite so the detail art draws over the tinted base.
     */
    public static String sharedModelJson(MaterialTextureSet set, BlockMaterialPrefix prefix, boolean overlay) {
        ResourceLocation layer0 = baseTexture(set, prefix);
        if (!overlay) {
            return """
                    {
                      "parent": "minecraft:block/cube_all",
                      "textures": {
                        "all": "%s"
                      }
                    }
                    """.formatted(layer0);
        }
        ResourceLocation layer1 = overlayTexture(set, prefix);
        String particle = "minecraft:block/stone";
//? if >=1.21.1 {
        /*return """
                {
                  "loader": "neoforge:composite",
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
*///?} else {
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
//?}
    }

    public static ResourceLocation stoneTexture(GTStoneBlockPaths paths) {
        return Mc.gregtechId("block/stones/" + paths.folder() + "/" + paths.variantTexture());
    }

    public record GTStoneBlockPaths(String folder, String variantTexture) {}

    public static GTStoneBlockPaths stonePaths(GTStoneBlock block) {
        return new GTStoneBlockPaths(block.stoneType().textureFolder(), block.variant().textureName());
    }
}
