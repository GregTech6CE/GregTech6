package com.gregtech.gregtech.worldgen;

import java.util.Locale;
import java.util.function.IntUnaryOperator;

/** Gregorius Techneticies' MultiTileEntityStick#getDefaultStick/getStick (LGPL-3.0-or-later). */
public final class SurfaceTwigRules {
    private SurfaceTwigRules() {}
    // Keep source order: e.g. a cold taiga still yields spruce rather than frozen wood.
    private static final String[][] BIOMES = {
        {"rainforest", ""}, {"firefly", ""}, {"dark forest", "WoodTowerwood"},
        {"silver pine", "WoodSilverPine"}, {"redwood", "WoodRedwood"}, {"cypress", "WoodCypress"},
        {"maple", "WoodMaple"}, {"tropic", "WoodCoconut"}, {"aspen", "WoodAspen"},
        {"autumn", "WoodAutumn"}, {"spruce", "WoodSpruce"}, {"taiga", "WoodSpruce"},
        {"boreal", "WoodSpruce"}, {"birch", "WoodBirch"}, {"jungle", "WoodJungle"},
        {"savann", "WoodAcacia"}, {"roofed", "WoodDarkOak"}, {"dark oak", "WoodDarkOak"},
        {"oak", "WoodOak"}, {"alpine", "!WoodFrozen"}, {"pine", "WoodPine"},
        {"fire", "!WoodScorched"}, {"fir", "WoodFir"}, {"volcan", "!WoodScorched"},
        {"glacier", "!WoodFrozen"}, {"ice", "!WoodFrozen"}, {"cold", "!WoodFrozen"},
        {"snow", "!WoodFrozen"}, {"frost", "!WoodFrozen"}, {"polar", "!WoodFrozen"},
        {"swamp", "!WoodMossy"}, {"marsh", "!WoodMossy"}, {"moor", "!WoodMossy"},
        {"mire", "!WoodMossy"}, {"bog", "!WoodMossy"}, {"mesa", "!WoodDead"},
        {"badlands", "!WoodDead"}, {"desert", "!WoodDead"}, {"sahara", "!WoodDead"}, {"waste", "!WoodDead"}
    };
    /** Null denotes a vanilla stick; other returns are canonical material names. */
    public static String material(String dimension, String biome, IntUnaryOperator random) {
        String dim = dimension.toLowerCase(Locale.ROOT);
        if (dim.startsWith("aether:")) return random.applyAsInt(3)>0 ? "Skyroot" : "WoodDead";
        if (dim.startsWith("erebus:")) return random.applyAsInt(8)>0 ? "WoodDead" : "PetrifiedWood";
        if (dim.startsWith("thebetweenlands:")) return random.applyAsInt(3)>0 ? "Weedwood" : "WoodRotten";
        if (dim.startsWith("atum:")) return random.applyAsInt(4)>0 ? "WoodCoconut" : "WoodDead";
        if (dim.startsWith("tropicraft:")) return random.applyAsInt(2)>0 ? "WoodCoconut" : "WoodMahogany";
        if (dim.startsWith("alfheim:")) return random.applyAsInt(8)>0 ? "Livingwood" : "Dreamwood";
        String name = biome.toLowerCase(Locale.ROOT).replace('_', ' ');
        if (name.equals("minecraft:dark forest")) name = "roofed forest";
        for (String[] row : BIOMES) if (name.contains(row[0])) {
            if (row[1].startsWith("!")) return row[1].substring(1);
            return mixed(row[1].isEmpty() ? null : row[1], random);
        }
        return mixed(null, random);
    }
    private static String mixed(String wood, IntUnaryOperator random) {
        return switch (random.applyAsInt(16)) {
            case 0 -> "WoodDead"; case 1 -> "WoodMossy"; case 2 -> "WoodRotten"; default -> wood;
        };
    }
}
