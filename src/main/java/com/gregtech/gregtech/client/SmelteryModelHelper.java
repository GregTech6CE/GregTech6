package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
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

    /** Solid face templates without tint (overlay layer). */
    public static final String FACE_OVERLAY = """
            "down":{"texture":"#wall"},"up":{"texture":"#wall"},"north":{"texture":"#wall"},"south":{"texture":"#wall"},"west":{"texture":"#wall"},"east":{"texture":"#wall"}
            """;

    // === Composite JSON template ===

    /** Build a {@code forge:composite} JSON model with a tinted solid layer and a cutout overlay layer. */
    public static String compositeModelJson(MaterialTextureSet set, String elementsTinted, String elementsOverlay) {
        ResourceLocation base = BlockMaterialIcons.baseTexture(set, BlockMaterialPrefix.blockSolid);
        ResourceLocation overlay = BlockMaterialIcons.overlayTexture(set, BlockMaterialPrefix.blockSolid);
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
    }

    /** Build an overlay-only elements string by stripping tintindex from the tinted version. */
    public static String overlayOnly(String tintedElements) {
        return tintedElements.replace(",\"tintindex\":0", "");
    }

    // === Model path builders ===

    public static ResourceLocation modelPath(String subfolder, MaterialTextureSet set) {
        return GregTech.id("block/machine/" + subfolder + "/" + set.folder() + "/blocksolid");
    }

    public static ResourceLocation modelPath(String subfolder, MaterialTextureSet set, String suffix) {
        return GregTech.id("block/machine/" + subfolder + "/" + set.folder() + "/blocksolid_" + suffix);
    }

    // === Faucet geometry (per-facing) ===

    /** Base texture prefix for tint string injection. */
    private static final String TINT_PREFIX = ",\"tintindex\":0\"";

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

    public static final String CROSSING_ELEMENTS_OVERLAY = """
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
            """.formatted(FACE_OVERLAY);

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

    // === Hopper geometry (per-facing nozzle) ===

    /** Generate the hopper geometry for a given facing: bowl + funnel + per-facing nozzle (4px high for sides, none for UP). */
    public static String hopperElements(Direction facing, boolean tinted) {
        String tint = tinted ? ",\"tintindex\":0" : "";
        String bowl = cubeElement(0, 10, 0, 16, 16, 16, tint, null, null, null);
        // Funnel: skip "up" face (not visible, covers nozzle connection area)
        String funnel = cubeElement(4, 4, 4, 12, 10, 12, tint, null, java.util.Set.of("up"), null);
        String nozzle = switch (facing) {
            // Nozzle: "up" face uses #bottom texture (nozzle interior bottom)
            case NORTH -> cubeElement(6, 4, 0, 10, 8, 4, tint, "north", null, java.util.Map.of("up", "#down"));
            case SOUTH -> cubeElement(6, 4, 12, 10, 8, 16, tint, "south", null, java.util.Map.of("up", "#down"));
            case WEST  -> cubeElement(0, 4, 6, 4, 8, 10, tint, "west", null, java.util.Map.of("up", "#down"));
            case EAST  -> cubeElement(12, 4, 6, 16, 8, 10, tint, "east", null, java.util.Map.of("up", "#down"));
            case DOWN  -> cubeElement(6, 0, 6, 10, 4, 10, tint, "down", null, java.util.Map.of("up", "#down"));
            case UP    -> null;
        };
        if (nozzle == null) {
            return "[" + bowl + "," + funnel + "]";
        }
        return "[" + bowl + "," + funnel + "," + nozzle + "]";
    }

    /** Build a cube element JSON with per-face UVs mapped to world-space position in the 16x16 texture, optional skipped faces, and per-face texture overrides. */
    private static String cubeElement(int x1, int y1, int z1, int x2, int y2, int z2,
                                      String tint, @Nullable String cull,
                                      @Nullable java.util.Set<String> skipFaces,
                                      @Nullable java.util.Map<String, String> textureOverrides) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{\"from\":[").append(x1).append(",").append(y1).append(",").append(z1)
          .append("],\"to\":[").append(x2).append(",").append(y2).append(",").append(z2)
          .append("],\"faces\":{");
        // UV maps to the element's position in the 16x16 block texture.
        // down/up: (x,z) → (u,v). Side faces: v is inverted because texture v=0 is world y=16 (top).
        faceJson(sb, "down",  textureOverrides, skipFaces, tint, cull, x1, z1, x2, z2, "down");
        faceJson(sb, "up",    textureOverrides, skipFaces, tint, cull, x1, z1, x2, z2, "up");
        faceJson(sb, "north", textureOverrides, skipFaces, tint, cull, x1, 16 - y2, x2, 16 - y1, "north");
        faceJson(sb, "south", textureOverrides, skipFaces, tint, cull, x1, 16 - y2, x2, 16 - y1, "south");
        faceJson(sb, "west",  textureOverrides, skipFaces, tint, cull, z1, 16 - y2, z2, 16 - y1, "west");
        faceJson(sb, "east",  textureOverrides, skipFaces, tint, cull, z1, 16 - y2, z2, 16 - y1, "east");
        // Remove trailing comma from last face
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.setLength(sb.length() - 1);
        }
        sb.append("}}");
        return sb.toString();
    }

    private static void faceJson(StringBuilder sb, String face,
                                  @Nullable java.util.Map<String, String> textureOverrides,
                                  @Nullable java.util.Set<String> skipFaces,
                                  String tint, @Nullable String cull,
                                  int u1, int v1, int u2, int v2, String cullKey) {
        if (skipFaces != null && skipFaces.contains(face)) return;
        // Use override texture or default #face reference
        String tex = (textureOverrides != null && textureOverrides.containsKey(face))
                ? textureOverrides.get(face)
                : "#" + face;
        sb.append("\"").append(face).append("\":{\"uv\":[").append(u1).append(",").append(v1)
          .append(",").append(u2).append(",").append(v2)
          .append("],\"texture\":\"").append(tex).append("\"");
        if (tint != null && !tint.isEmpty()) {
            sb.append(tint);
        }
        if (cull != null && cull.equals(cullKey)) {
            sb.append(",\"cullface\":\"").append(cull).append("\"");
        }
        sb.append("},");
    }

    /** Legacy overload for callers that haven't been updated. */
    private static String cubeElement(int x1, int y1, int z1, int x2, int y2, int z2,
                                      String tint, @Nullable String cull) {
        return cubeElement(x1, y1, z1, x2, y2, z2, tint, cull, null, null);
    }

    /** Compose a {@code forge:composite} JSON string for a hopper model with the given textures and elements. */
    public static String hopperCompositeJson(String texPath, String elementsTinted, String elementsOverlay) {
        return """
                {
                  "loader": "forge:composite",
                  "children": {
                    "layer0": {
                      "parent": "minecraft:block/block",
                      "textures": {
                        "down": "gregtech:block/machines/%s/colored/bottom",
                        "up": "gregtech:block/machines/%s/colored/top",
                        "north": "gregtech:block/machines/%s/colored/side",
                        "south": "gregtech:block/machines/%s/colored/side",
                        "west": "gregtech:block/machines/%s/colored/side",
                        "east": "gregtech:block/machines/%s/colored/side"
                      },
                      "elements": %s,
                      "render_type": "minecraft:solid"
                    },
                    "layer1": {
                      "parent": "minecraft:block/block",
                      "textures": {
                        "down": "gregtech:block/machines/%s/overlay/bottom",
                        "up": "gregtech:block/machines/%s/overlay/top",
                        "north": "gregtech:block/machines/%s/overlay/side",
                        "south": "gregtech:block/machines/%s/overlay/side",
                        "west": "gregtech:block/machines/%s/overlay/side",
                        "east": "gregtech:block/machines/%s/overlay/side"
                      },
                      "elements": %s,
                      "render_type": "minecraft:cutout"
                    }
                  }
                }
                """.formatted(texPath, texPath, texPath, texPath, texPath, texPath,
                        elementsTinted,
                        texPath, texPath, texPath, texPath, texPath, texPath,
                        elementsOverlay);
    }

    private static String repeat(String value, int count) {
        return value.repeat(count);
    }
}
