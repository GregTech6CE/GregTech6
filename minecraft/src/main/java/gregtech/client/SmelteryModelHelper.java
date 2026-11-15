package gregtech.client;


import gregtech.api.material.MaterialTextureSet;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.platform.Mc;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Shared composite-model template and geometry for all smeltery hull blocks.
 * <p>
 * Each icon class (CrucibleBowlIcons, MoldBasinIcons, etc.) delegates here for the JSON
 * template and just supplies the model path prefix and element geometry.
 */
public final class SmelteryModelHelper {
    private SmelteryModelHelper() {}

    /** Full block bounds: [0,0,0]→[16,16,16] */
    public static final String FULL_CUBE = """
            {"from":[0,0,0],"to":[16,16,16],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","tintindex":0},"south":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","tintindex":0},"east":{"texture":"#wall","tintindex":0}}}
            """;

    /** Solid face templates with tint. */
    public static final String FACE_TINTED = """
            "down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","tintindex":0},"south":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","tintindex":0},"east":{"texture":"#wall","tintindex":0}
            """;

    // === Hull model template ===

    /**
     * Build the hull model. Without {@code overlay} this is a plain block model holding the tinted
     * faces directly; with it, the faces sit under a {@code forge:composite} cutout detail pass.
     */
    public static String hullModelJson(
            MaterialTextureSet set, String elementsTinted, @Nullable String elementsOverlay) {
        String icon = BlockMaterialPrefix.blockSolid.getTextureFileName();
        ResourceLocation base = BlockMaterialIcons.resolveTexture(set, icon);
        ResourceLocation overlay = BlockMaterialIcons.resolveOverlayTexture(set, icon);
        if (elementsOverlay == null || overlay == null) {
            return """
                    {
                      "parent": "minecraft:block/block",
                      "textures": {
                        "particle": "%s",
                        "wall": "%s"
                      },
                      "elements": %s,
                      "render_type": "minecraft:solid"
                    }
                    """.formatted(base, base, elementsTinted);
        }
        //? if >=1.21.1 {
        /*return """
                {
                  "loader": "neoforge:composite",
                  "children": {
                    "layer0": {
                      "parent": "minecraft:block/block",
                      "textures": {
                        "particle": "minecraft:block/anvil",
                        "wall": "%s"
                      },
                      "elements": %s,
                      "render_type": "minecraft:solid"
                    },
                    "layer1": {
                      "parent": "minecraft:block/block",
                      "textures": {
                        "wall": "%s"
                      },
                      "elements": %s,
                      "render_type": "minecraft:cutout"
                    }
                  }
                }
                """.formatted(base, elementsTinted, overlay, elementsOverlay);
*///?} else {
        return """
                {
                  "loader": "forge:composite",
                  "children": {
                    "layer0": {
                      "parent": "minecraft:block/block",
                      "textures": {
                        "particle": "minecraft:block/anvil",
                        "wall": "%s"
                      },
                      "elements": %s,
                      "render_type": "minecraft:solid"
                    },
                    "layer1": {
                      "parent": "minecraft:block/block",
                      "textures": {
                        "wall": "%s"
                      },
                      "elements": %s,
                      "render_type": "minecraft:cutout"
                    }
                  }
                }
                """.formatted(base, elementsTinted, overlay, elementsOverlay);
//?}
    }

    // === Model path builders ===

    public static ResourceLocation modelPath(String subfolder, MaterialTextureSet set) {
        return Mc.gregtechId("block/machine/" + subfolder + "/" + set.folder() + "/blocksolid");
    }

    public static ResourceLocation modelPath(String subfolder, MaterialTextureSet set, String suffix) {
        return Mc.gregtechId("block/machine/" + subfolder + "/" + set.folder() + "/blocksolid_" + suffix);
    }

    // === Faucet geometry (per-facing) ===

    public static String faucetElements(Direction facing, boolean tinted) {
        String tint = tinted ? ",\"tintindex\":0" : "";
        return switch (facing) {
            case NORTH -> """
                    [
                      {"from":[6,1,0],"to":[10,2,4],"faces":{"down":{"texture":"#wall"%s},"up":{"texture":"#wall"%s},"north":{"texture":"#wall","cullface":"north"%s},"south":{"texture":"#wall"%s},"west":{"texture":"#wall"%s},"east":{"texture":"#wall"%s}}},
                      {"from":[5,2,0],"to":[6,6,4],"faces":{"down":{"texture":"#wall"%s},"up":{"texture":"#wall"%s},"north":{"texture":"#wall","cullface":"north"%s},"south":{"texture":"#wall"%s},"west":{"texture":"#wall"%s},"east":{"texture":"#wall"%s}}},
                      {"from":[10,2,0],"to":[11,6,4],"faces":{"down":{"texture":"#wall"%s},"up":{"texture":"#wall"%s},"north":{"texture":"#wall","cullface":"north"%s},"south":{"texture":"#wall"%s},"west":{"texture":"#wall"%s},"east":{"texture":"#wall"%s}}}
                    ]
                    """.formatted(repeat(tint, 18));
            case WEST -> """
                    [
                      {"from":[0,1,6],"to":[4,2,10],"faces":{"down":{"texture":"#wall"%s},"up":{"texture":"#wall"%s},"north":{"texture":"#wall"%s},"south":{"texture":"#wall"%s},"west":{"texture":"#wall","cullface":"west"%s},"east":{"texture":"#wall"%s}}},
                      {"from":[0,2,5],"to":[4,6,6],"faces":{"down":{"texture":"#wall"%s},"up":{"texture":"#wall"%s},"north":{"texture":"#wall"%s},"south":{"texture":"#wall"%s},"west":{"texture":"#wall","cullface":"west"%s},"east":{"texture":"#wall"%s}}},
                      {"from":[0,2,10],"to":[4,6,11],"faces":{"down":{"texture":"#wall"%s},"up":{"texture":"#wall"%s},"north":{"texture":"#wall"%s},"south":{"texture":"#wall"%s},"west":{"texture":"#wall","cullface":"west"%s},"east":{"texture":"#wall"%s}}}
                    ]
                    """.formatted(repeat(tint, 18));
            case EAST -> """
                    [
                      {"from":[12,1,6],"to":[16,2,10],"faces":{"down":{"texture":"#wall"%s},"up":{"texture":"#wall"%s},"north":{"texture":"#wall"%s},"south":{"texture":"#wall"%s},"west":{"texture":"#wall"%s},"east":{"texture":"#wall","cullface":"east"%s}}},
                      {"from":[12,2,5],"to":[16,6,6],"faces":{"down":{"texture":"#wall"%s},"up":{"texture":"#wall"%s},"north":{"texture":"#wall"%s},"south":{"texture":"#wall"%s},"west":{"texture":"#wall"%s},"east":{"texture":"#wall","cullface":"east"%s}}},
                      {"from":[12,2,10],"to":[16,6,11],"faces":{"down":{"texture":"#wall"%s},"up":{"texture":"#wall"%s},"north":{"texture":"#wall"%s},"south":{"texture":"#wall"%s},"west":{"texture":"#wall"%s},"east":{"texture":"#wall","cullface":"east"%s}}}
                    ]
                    """.formatted(repeat(tint, 18));
            default -> """
                    [
                      {"from":[6,1,12],"to":[10,2,16],"faces":{"down":{"texture":"#wall"%s},"up":{"texture":"#wall"%s},"north":{"texture":"#wall"%s},"south":{"texture":"#wall","cullface":"south"%s},"west":{"texture":"#wall"%s},"east":{"texture":"#wall"%s}}},
                      {"from":[5,2,12],"to":[6,6,16],"faces":{"down":{"texture":"#wall"%s},"up":{"texture":"#wall"%s},"north":{"texture":"#wall"%s},"south":{"texture":"#wall","cullface":"south"%s},"west":{"texture":"#wall"%s},"east":{"texture":"#wall"%s}}},
                      {"from":[10,2,12],"to":[11,6,16],"faces":{"down":{"texture":"#wall"%s},"up":{"texture":"#wall"%s},"north":{"texture":"#wall"%s},"south":{"texture":"#wall","cullface":"south"%s},"west":{"texture":"#wall"%s},"east":{"texture":"#wall"%s}}}
                    ]
                    """.formatted(repeat(tint, 18));
        };
    }

    // === Crossing geometry ===

    public static final String CROSSING_ELEMENTS_TINTED = """
            [
              {"from":[6,1,0],"to":[10,2,6],"faces":{%1$s}},
              {"from":[0,1,6],"to":[16,2,10],"faces":{%1$s}},
              {"from":[6,1,10],"to":[10,2,16],"faces":{%1$s}},
              {"from":[5,2,0],"to":[6,6,5],"faces":{%1$s}},
              {"from":[10,2,0],"to":[11,6,5],"faces":{%1$s}},
              {"from":[0,2,5],"to":[6,6,6],"faces":{%1$s}},
              {"from":[10,2,5],"to":[16,6,6],"faces":{%1$s}},
              {"from":[0,2,10],"to":[6,6,11],"faces":{%1$s}},
              {"from":[10,2,10],"to":[16,6,11],"faces":{%1$s}},
              {"from":[5,2,11],"to":[6,6,16],"faces":{%1$s}},
              {"from":[10,2,11],"to":[11,6,16],"faces":{%1$s}}
            ]
            """.formatted(FACE_TINTED);

    // === Geometry constants for the smeltery block types ===

    /** Crucible bowl: four 2px wall strips (full height) + 2px bottom plate. */
    public static final String BOWL_ELEMENTS_TINTED = """
            [
              {"from":[0,0,0],"to":[2,16,16],"faces":{"west":{"texture":"#wall","cullface":"west","tintindex":0},"east":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","cullface":"north","tintindex":0},"south":{"texture":"#wall","cullface":"south","tintindex":0},"up":{"texture":"#wall","tintindex":0}}},
              {"from":[14,0,0],"to":[16,16,16],"faces":{"east":{"texture":"#wall","cullface":"east","tintindex":0},"west":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","cullface":"north","tintindex":0},"south":{"texture":"#wall","cullface":"south","tintindex":0},"up":{"texture":"#wall","tintindex":0}}},
              {"from":[2,0,0],"to":[14,16,2],"faces":{"north":{"texture":"#wall","cullface":"north","tintindex":0},"south":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","cullface":"west","tintindex":0},"east":{"texture":"#wall","cullface":"east","tintindex":0},"up":{"texture":"#wall","tintindex":0}}},
              {"from":[2,0,14],"to":[14,16,16],"faces":{"south":{"texture":"#wall","cullface":"south","tintindex":0},"north":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","cullface":"west","tintindex":0},"east":{"texture":"#wall","cullface":"east","tintindex":0},"up":{"texture":"#wall","tintindex":0}}},
              {"from":[0,0,0],"to":[16,2,16],"faces":{"down":{"texture":"#wall","cullface":"down","tintindex":0},"up":{"texture":"#wall","tintindex":0}}}
            ]
            """;

    /** Mold basin: 1px wall strips (full height) + 1px bottom plate. */
    public static final String BASIN_ELEMENTS_TINTED = """
            [
              {"from":[0,0,0],"to":[1,16,16],"faces":{"west":{"texture":"#wall","cullface":"west","tintindex":0},"east":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","cullface":"north","tintindex":0},"south":{"texture":"#wall","cullface":"south","tintindex":0},"up":{"texture":"#wall","tintindex":0}}},
              {"from":[15,0,0],"to":[16,16,16],"faces":{"east":{"texture":"#wall","cullface":"east","tintindex":0},"west":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","cullface":"north","tintindex":0},"south":{"texture":"#wall","cullface":"south","tintindex":0},"up":{"texture":"#wall","tintindex":0}}},
              {"from":[1,0,0],"to":[15,16,1],"faces":{"north":{"texture":"#wall","cullface":"north","tintindex":0},"south":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","cullface":"west","tintindex":0},"east":{"texture":"#wall","cullface":"east","tintindex":0},"up":{"texture":"#wall","tintindex":0}}},
              {"from":[1,0,15],"to":[15,16,16],"faces":{"south":{"texture":"#wall","cullface":"south","tintindex":0},"north":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","cullface":"west","tintindex":0},"east":{"texture":"#wall","cullface":"east","tintindex":0},"up":{"texture":"#wall","tintindex":0}}},
              {"from":[0,0,0],"to":[16,1,16],"faces":{"down":{"texture":"#wall","cullface":"down","tintindex":0},"up":{"texture":"#wall","tintindex":0}}}
            ]
            """;

    /** Mold hull: bottom plate + 4 raised edges + 8 corner posts (no inner cavity). */
    public static final String MOLD_ELEMENTS_TINTED = """
            [
              {"from":[0,0,0],"to":[16,1,16],"faces":{"down":{"texture":"#wall","cullface":"down","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","cullface":"north","tintindex":0},"south":{"texture":"#wall","cullface":"south","tintindex":0},"west":{"texture":"#wall","cullface":"west","tintindex":0},"east":{"texture":"#wall","cullface":"east","tintindex":0}}},
              {"from":[14,0,0],"to":[16,4,16],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","tintindex":0},"south":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","tintindex":0},"east":{"texture":"#wall","cullface":"east","tintindex":0}}},
              {"from":[0,0,14],"to":[16,4,16],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","tintindex":0},"south":{"texture":"#wall","cullface":"south","tintindex":0},"west":{"texture":"#wall","tintindex":0},"east":{"texture":"#wall","tintindex":0}}},
              {"from":[0,0,0],"to":[2,4,16],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","tintindex":0},"south":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","cullface":"west","tintindex":0},"east":{"texture":"#wall","tintindex":0}}},
              {"from":[0,0,0],"to":[16,4,2],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","cullface":"north","tintindex":0},"south":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","tintindex":0},"east":{"texture":"#wall","tintindex":0}}},
              {"from":[6,4,0],"to":[7,6,2],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","cullface":"north","tintindex":0},"south":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","tintindex":0},"east":{"texture":"#wall","tintindex":0}}},
              {"from":[9,4,0],"to":[10,6,2],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","cullface":"north","tintindex":0},"south":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","tintindex":0},"east":{"texture":"#wall","tintindex":0}}},
              {"from":[6,6,0],"to":[10,7,2],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","cullface":"north","tintindex":0},"south":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","tintindex":0},"east":{"texture":"#wall","tintindex":0}}},
              {"from":[6,4,14],"to":[7,6,16],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","tintindex":0},"south":{"texture":"#wall","cullface":"south","tintindex":0},"west":{"texture":"#wall","tintindex":0},"east":{"texture":"#wall","tintindex":0}}},
              {"from":[9,4,14],"to":[10,6,16],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","tintindex":0},"south":{"texture":"#wall","cullface":"south","tintindex":0},"west":{"texture":"#wall","tintindex":0},"east":{"texture":"#wall","tintindex":0}}},
              {"from":[6,6,14],"to":[10,7,16],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","tintindex":0},"south":{"texture":"#wall","cullface":"south","tintindex":0},"west":{"texture":"#wall","tintindex":0},"east":{"texture":"#wall","tintindex":0}}},
              {"from":[0,4,6],"to":[2,6,7],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","tintindex":0},"south":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","cullface":"west","tintindex":0},"east":{"texture":"#wall","tintindex":0}}},
              {"from":[0,4,9],"to":[2,6,10],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","tintindex":0},"south":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","cullface":"west","tintindex":0},"east":{"texture":"#wall","tintindex":0}}},
              {"from":[0,6,6],"to":[2,7,10],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","tintindex":0},"south":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","cullface":"west","tintindex":0},"east":{"texture":"#wall","tintindex":0}}},
              {"from":[14,4,6],"to":[16,6,7],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","tintindex":0},"south":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","tintindex":0},"east":{"texture":"#wall","cullface":"east","tintindex":0}}},
              {"from":[14,4,9],"to":[16,6,10],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","tintindex":0},"south":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","tintindex":0},"east":{"texture":"#wall","cullface":"east","tintindex":0}}},
              {"from":[14,6,6],"to":[16,7,10],"faces":{"down":{"texture":"#wall","tintindex":0},"up":{"texture":"#wall","tintindex":0},"north":{"texture":"#wall","tintindex":0},"south":{"texture":"#wall","tintindex":0},"west":{"texture":"#wall","tintindex":0},"east":{"texture":"#wall","cullface":"east","tintindex":0}}}
            ]
            """;

    private static String repeat(String value, int count) {
        return value.repeat(count);
    }
}
