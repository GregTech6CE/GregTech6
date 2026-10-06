/* Adapted from Gregorius Techneticies, Copyright (c) 2019; LGPL-3.0-or-later.
 * Original MultiTileEntityPanel / Loader_MultiTileEntities; provenance in panels-source-20261006.json. */
package com.gregtech.gregtech.content.transport;

import com.gregtech.gregtech.block.ConstructionRules;
import com.gregtech.gregtech.content.storage.BottleCrateVariants;
import com.gregtech.gregtech.content.tool.PaintingRules;
import java.util.*;

/** Loader_MultiTileEntities:2043-2082; original panels are cover items, never placed blocks. */
public final class PanelCatalog {
    private PanelCatalog() {}
    public record Spec(String material, String kind, int tint, String mapColor, float hardness,
                       String texture, String input, String color, int sourceIndex, boolean canonical) {
        public String id() { return "panel_" + material; }
        public boolean asphalt() { return kind.equals("asphalt"); }
        public int originalId() {
            if (!canonical || sourceIndex < 0) return -1;
            if (kind.equals("wood")) return sourceIndex < 100 ? 32500 + sourceIndex
                    : sourceIndex < 200 ? 32352 + sourceIndex - 100 : 32252 + sourceIndex - 200;
            return switch (kind) { case "concrete" -> 32452; case "cfoam" -> 32468; default -> 32484; } + sourceIndex;
        }
        public int order() {
            if (kind.equals("wood")) return sourceIndex < 0 ? 1000 : 48 + sourceIndex;
            return sourceIndex * 3 + switch (kind) { case "concrete" -> 0; case "cfoam" -> 1; default -> 2; };
        }
    }
    public static final List<Spec> ALL = specs();
    public static final List<Spec> CANONICAL = ALL.stream().filter(Spec::canonical)
            .sorted(Comparator.comparingInt(Spec::order).thenComparing(Spec::id)).toList();
    public static Spec get(String material) { return ALL.stream().filter(s -> s.material().equals(material)).findFirst().orElseThrow(); }
    public static Spec find(String id) { return ALL.stream().filter(s -> s.id().equals(id)).findFirst().orElse(null); }
    private static Spec colored(String suffix, String kind, String color, int index, boolean canonical) {
        return new Spec(suffix, kind, ConstructionRules.tint(color), kind.equals("cfoam") ? "WOOL" : "STONE",
                kind.equals("cfoam") ? 1F : kind.equals("asphalt") ? 3F : 4F,
                "gregtech:block/iconsets/" + (kind.equals("cfoam") ? "cfoam_hardened" : kind),
                "gregtech:" + kind, color, index, canonical);
    }
    private static List<Spec> specs() {
        var result = new ArrayList<Spec>();
        // Keep old item/block IDs for saves; canonical variants use original identities.
        result.add(new Spec("wood", "wood", 0xFFFFFF, "WOOD", 2F, "minecraft:block/oak_planks", "minecraft:oak_planks", "", 0, true));
        result.add(colored("concrete", "concrete", "light_gray", 7, false));
        result.add(colored("cfoam", "cfoam", "white", 15, false));
        result.add(colored("asphalt", "asphalt", "gray", 8, false));
        result.add(colored("colored_gray", "concrete", "gray", 8, false));
        result.add(colored("colored_black", "concrete", "black", 0, false));
        for (var dye : PaintingRules.DYES) for (String kind : List.of("concrete", "cfoam", "asphalt"))
            result.add(colored(kind + "_" + dye.id(), kind, dye.id(), dye.index(), true));
        for (var wood : BottleCrateVariants.WOODS) {
            if (wood.suffix().equals("oak")) continue;
            result.add(new Spec("wood_" + wood.suffix(), "wood", 0xFFFFFF, "WOOD", 2F,
                    wood.texture(), wood.plank(), "", woodIndex(wood.suffix()), true));
        }
        result.add(new Spec("wood_treated", "wood", 0xFFFFFF, "WOOD", 2F,
                "gregtech:block/iconsets/planks_treated", "gregtech:planks_treated", "", 62, true));
        return List.copyOf(result);
    }
    private static int woodIndex(String wood) {
        return switch (wood) {
            case "spruce" -> 1; case "birch" -> 2; case "jungle" -> 3; case "acacia" -> 4; case "dark_oak" -> 5;
            case "rubber" -> 6; case "maple" -> 7; case "willow" -> 37; case "blue_mahoe" -> 38; case "hazel" -> 39;
            case "compressed" -> 54; case "treated" -> 62; case "wood" -> 63;
            case "cinnamon" -> 97; case "coconut" -> 98; case "rainbowood" -> 99;
            case "dry" -> 100; case "rotten" -> 101; case "mossy" -> 102; case "frozen" -> 103; case "bluespruce" -> 239;
            default -> -1; // Available modern/dictionary planks; no guessed original numeric ID.
        };
    }
    public static double[] itemBounds() { return new double[]{0, 0, 7, 16, 16, 9}; }
    /** Existing saved standalone blocks retain their old bounds; new placement is refused. */
    public static double[] bounds(int face) { return switch(face) {
        case 0 -> new double[]{0,0,0,16,1,16}; case 1 -> new double[]{0,15,0,16,16,16};
        case 2 -> new double[]{0,0,0,16,16,1}; case 3 -> new double[]{0,0,15,16,16,16};
        case 4 -> new double[]{0,0,0,1,16,16}; case 5 -> new double[]{15,0,0,16,16,16};
        default -> throw new IllegalArgumentException("Invalid panel face");
    }; }
}
