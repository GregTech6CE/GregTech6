package com.gregtech.gregtech.content.cover;

import java.util.List;

/** Original Loader_OreProcessing plate cover texture order, independent of platform item types. */
public final class MaterialCoverRules {
    private MaterialCoverRules() {}
    /** Original stone plates copy untinted rock/vanilla textures rather than generic material icons. */
    public static List<String> stoneTextures(String prefix,String material) {
        if(!"plate".equals(prefix))return List.of();
        for(var stone:com.gregtech.gregtech.block.stone.StoneType.values())if(stone.material().getName().equals(material))
            return java.util.Arrays.stream(com.gregtech.gregtech.block.stone.StoneVariant.values())
                    .map(variant->"gregtech:block/stones/"+stone.textureFolder()+"/"+variant.textureName()).toList();
        return switch(material){
            case "Stone" -> List.of("minecraft:block/stone","minecraft:block/cobblestone","minecraft:block/mossy_cobblestone",
                    "minecraft:block/stone_bricks","minecraft:block/mossy_stone_bricks","minecraft:block/cracked_stone_bricks",
                    "minecraft:block/chiseled_stone_bricks","minecraft:block/smooth_stone","minecraft:block/smooth_stone_slab_side");
            case "Netherrack" -> List.of("minecraft:block/netherrack");
            case "Nether Brick", "BrickNether" -> List.of("minecraft:block/nether_bricks");
            case "Endstone" -> List.of("minecraft:block/end_stone");
            case "Obsidian" -> List.of("minecraft:block/obsidian");
            default -> List.of();
        };
    }
    public static int designCount(String prefix,String material){
        var stone=stoneTextures(prefix,material);return stone.isEmpty()?textures(prefix).size():stone.size();
    }
    public static List<String> textures(String prefix) {
        return switch (prefix) {
            case "plate", "sheetGt" -> List.of("blocksolid", "blockplate", "blockingot", "casingmachine", "blockdust", "blockraw");
            case "plateDouble" -> List.of("casingmachinedouble", "blockplate", "blocksolid", "blockingot", "blockdust", "blockraw");
            case "plateTriple" -> List.of("blockplate", "blocksolid", "blockingot", "casingmachinedouble", "blockdust", "blockraw");
            case "plateQuadruple" -> List.of("blockingot", "blockplate", "blocksolid", "casingmachinequadruple", "blockdust", "blockraw");
            case "plateQuintuple" -> List.of("casingmachinequadruple", "blockingot", "blockplate", "blocksolid", "blockdust", "blockraw");
            case "plateDense" -> List.of("casingmachinedense", "blocksolid", "blockplate", "blockingot", "blockdust", "blockraw");
            case "plateCurved" -> List.of("casingmachine", "blocksolid", "blockplate", "blockingot", "blockdust", "blockraw");
            case "plateGem" -> List.of("blockgem", "blockplategem", "blockdust", "blockraw");
            case "foil" -> List.of("foil");
            default -> List.of();
        };
    }
}
