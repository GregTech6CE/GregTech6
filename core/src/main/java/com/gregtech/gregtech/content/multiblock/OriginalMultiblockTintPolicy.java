package com.gregtech.gregtech.content.multiblock;
/** Exact original fallback alias material; registered specs/parts take precedence. */
public final class OriginalMultiblockTintPolicy {private OriginalMultiblockTintPolicy(){}public static String material(String path){
        return switch (path) {
            case "heat_transmitter", "heat_exchanger_wall" -> "Invar";
            case "coke_oven_wall", "large_crucible_wall", "coke_oven_main", "large_crucible_main" -> "Ceramic";
            case "bedrock_drill_wall", "implosion_compressor_wall", "implosion_compressor_main" -> "Tungstensteel";
            case "heat_exchanger_main" -> "Tungsten";
            case "bedrock_drill_main" -> "Titanium";
            case "lightning_rod_wall", "lightning_rod_main" -> "Tungsten";
            case "lightning_rod_pillar" -> "SteelGalvanized";
            case "large_niobium_titanium_coil" -> "NiobiumTitanium";
            case "large_dynamo_wall" -> "AnnealedCopper";
            case "centrifuge_part" -> "Tungstensteel";
            case "fusion_reactor_wall" -> "Iridium";
            default -> "StainlessSteel";
        };
}}
