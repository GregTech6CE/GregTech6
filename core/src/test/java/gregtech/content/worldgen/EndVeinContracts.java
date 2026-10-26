package gregtech.content.worldgen;

import java.util.*;

public final class EndVeinContracts {
    public static void main(String[] args) {
        gregtech.api.mod.ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        gregtech.engine.material.GTMaterialRegistry.init();
        var veins = GTOreVeins.END_VEINS;
        require(veins.size() == 5, "Five original End large veins");
        require(GTOreVeins.END_SMALL_ORES.size() == 32, "Complete End small-ore catalog");
        int total = GTOreVeins.endSelectionWeight(veins);
        require(total == 290, "Preserved End lottery size");
        var counts = new HashMap<String, Integer>();
        for (int roll = 0; roll < total; roll++) {
            var picked = MineralWorldgenRules.selectVein(veins, roll);
            counts.merge(picked == null ? "empty" : picked.name(), 1, Integer::sum);
        }
        require(counts.equals(Map.of("platinum", 5, "molybdenum", 5, "cassiterite", 17,
                "naquadah", 10, "trinium", 10, "empty", 243)), "Every roll maps to the intended weights: " + counts);
        require(String.format(Locale.ROOT, "%.2f", 100d * counts.get("cassiterite") / total).equals("5.86"), "Cassiterite percentage");
        require(String.format(Locale.ROOT, "%.2f", 100d * counts.get("trinium") / total).equals("3.45"), "Trinium percentage");
        var extended = new ArrayList<>(veins);
        extended.add(new GTOreVeins.OreVein("addon", 10, 80, 3, 1, 12, null, null, null, null));
        require(GTOreVeins.endSelectionWeight(extended) == 293, "Addon weights retain empty rolls");
        require(MineralWorldgenRules.selectVein(extended, 47).name().equals("addon"), "Addon first roll");
        require(MineralWorldgenRules.selectVein(extended, 50) == null, "Empty range after addon");
        System.out.println("End vein contracts passed: all 290 rolls, five large veins and 32 small ores");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
