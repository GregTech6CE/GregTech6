package gregtech.content.multiblock;

/** Original fallback material for the remaining standalone controllers; typed parts take precedence. */
public final class OriginalMultiblockTintPolicy {

    private OriginalMultiblockTintPolicy() {
    }

    public static String material(String path) {
        return switch (path) {
            case "heat_transmitter" -> "Invar";
            case "fire_bricks", "coke_oven_main" -> "Ceramic";
            case "implosion_compressor_wall", "implosion_compressor_main", "centrifuge_part" -> "Tungstensteel";
            case "heat_exchanger_main" -> "Tungsten";
            case "large_niobium_titanium_coil" -> "NiobiumTitanium";
            default -> "StainlessSteel";
        };
    }
}
