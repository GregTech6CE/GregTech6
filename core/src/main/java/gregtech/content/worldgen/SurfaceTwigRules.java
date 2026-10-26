package gregtech.content.worldgen;

import java.util.Locale;
import java.util.Arrays;
import java.util.List;
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
        String[] outcomes = outcomes(dimension, biome);
        return outcomes[outcomes.length == 1 ? 0 : random.applyAsInt(outcomes.length)];
    }
    /** Read-only preview of the same drop rule; null denotes a vanilla stick. Never rolls world RNG. */
    public static List<String> possibleMaterials(String dimension, String biome) {
        return Arrays.stream(outcomes(dimension, biome)).distinct().toList();
    }
    private static String[] outcomes(String dimension, String biome) {
        String dim = dimension.toLowerCase(Locale.ROOT);
        if (dim.startsWith("aether:")) return weighted("Skyroot", "WoodDead", 3);
        if (dim.startsWith("erebus:")) return weighted("WoodDead", "PetrifiedWood", 8);
        if (dim.startsWith("thebetweenlands:")) return weighted("Weedwood", "WoodRotten", 3);
        if (dim.startsWith("atum:")) return weighted("WoodCoconut", "WoodDead", 4);
        if (dim.startsWith("tropicraft:")) return weighted("WoodCoconut", "WoodMahogany", 2);
        if (dim.startsWith("alfheim:")) return weighted("Livingwood", "Dreamwood", 8);
        String name = biome.toLowerCase(Locale.ROOT).replace('_', ' ');
        if (name.equals("minecraft:dark forest")) name = "roofed forest";
        for (String[] row : BIOMES) if (name.contains(row[0])) {
            if (row[1].startsWith("!")) return new String[]{row[1].substring(1)};
            return mixed(row[1].isEmpty() ? null : row[1]);
        }
        return mixed(null);
    }
    private static String[] weighted(String common, String rare, int bound) {
        String[] outcomes = new String[bound];
        Arrays.fill(outcomes, common);
        outcomes[0] = rare;
        return outcomes;
    }
    private static String[] mixed(String wood) {
        String[] outcomes = weighted(wood, "WoodDead", 16);
        outcomes[1] = "WoodMossy";
        outcomes[2] = "WoodRotten";
        return outcomes;
    }
}
