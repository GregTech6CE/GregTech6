package gregtech.client;


import gregtech.api.material.MaterialTextureSet;
import gregtech.engine.material.MaterialIconDefinitions;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.platform.Mc;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Block material icon paths under {@code textures/block/material_icons/{set}/{prefix}.png}. */
public final class BlockMaterialIcons {
    private BlockMaterialIcons() {}

    @Nullable
    public static MaterialTextureSet resolveTextureOwner(MaterialTextureSet set, String fileName) {
        return MaterialIconDefinitions.resolveBlockTextureOwner(set, fileName, MaterialIcons::resourceExists);
    }

    /** Sprite location for this set, following the inheritance chain to the set that ships it. */
    public static ResourceLocation resolveTexture(MaterialTextureSet set, String fileName) {
        MaterialTextureSet owner = resolveTextureOwner(set, fileName);
        return Mc.gregtechId(MaterialIconDefinitions.blockIconPath(owner == null ? set : owner, fileName));
    }

    /** Detail sprite for this set, or {@code null} when no set in the chain ships one. */
    @Nullable
    public static ResourceLocation resolveOverlayTexture(MaterialTextureSet set, String fileName) {
        String overlayFile = fileName + "_overlay";
        MaterialTextureSet owner = resolveTextureOwner(set, overlayFile);
        return owner == null ? null : Mc.gregtechId(MaterialIconDefinitions.blockIconPath(owner, overlayFile));
    }

    public static ResourceLocation addonModel(BlockMaterialPrefix prefix) {
        return Mc.gregtechId("block/material/addon/" + prefix.getTextureFileName());
    }

    public static ResourceLocation sharedModelLocation(MaterialTextureSet set, BlockMaterialPrefix prefix) {
        return Mc.gregtechId("block/material/" + set.folder() + "/" + prefix.getTextureFileName());
    }

    /**
     * Shared block model, or {@code null} when no set in the inheritance chain ships the base
     * sprite. Sets with a detail sprite get a two-layer composite so the detail art draws over the
     * tinted base; the rest are a plain {@code cube_all}.
     */
    @Nullable
    public static String sharedModelJson(
            MaterialTextureSet set, BlockMaterialPrefix prefix, Predicate<String> resourceExists) {
        String file = prefix.getTextureFileName();
        MaterialTextureSet baseOwner = MaterialIconDefinitions.resolveBlockTextureOwner(set, file, resourceExists);
        if (baseOwner == null) return null;
        String base = "gt6ce:" + MaterialIconDefinitions.blockIconPath(baseOwner, file);
        MaterialTextureSet overlayOwner =
                MaterialIconDefinitions.resolveBlockTextureOwner(set, file + "_overlay", resourceExists);
        if (overlayOwner == null) {
            return """
                    {
                      "parent": "minecraft:block/cube_all",
                      "textures": {
                        "all": "%s"
                      }
                    }
                    """.formatted(base);
        }
        String overlay = "gt6ce:" + MaterialIconDefinitions.blockIconPath(overlayOwner, file + "_overlay");
        return compositeCubeJson(base, overlay);
    }

    /** Solid tinted base layer plus a cutout detail layer over the same cube. */
    private static String compositeCubeJson(String base, String overlay) {
//? if >=1.21.1 {
        /*return """
                {
                  "loader": "neoforge:composite",
                  "parent": "minecraft:block/block",
                  "textures": { "particle": "%s" },
                  "children": {
                    "layer0": {
                      "parent": "minecraft:block/cube_all",
                      "textures": { "all": "%s" },
                      "render_type": "minecraft:solid"
                    },
                    "layer1": {
                      "parent": "minecraft:block/cube_all",
                      "textures": { "all": "%s" },
                      "render_type": "minecraft:cutout"
                    }
                  }
                }
                """.formatted(base, base, overlay);
*///?} else {
        return """
                {
                  "loader": "forge:composite",
                  "parent": "minecraft:block/block",
                  "textures": { "particle": "%s" },
                  "children": {
                    "layer0": {
                      "parent": "minecraft:block/cube_all",
                      "textures": { "all": "%s" },
                      "render_type": "minecraft:solid"
                    },
                    "layer1": {
                      "parent": "minecraft:block/cube_all",
                      "textures": { "all": "%s" },
                      "render_type": "minecraft:cutout"
                    }
                  }
                }
                """.formatted(base, base, overlay);
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
