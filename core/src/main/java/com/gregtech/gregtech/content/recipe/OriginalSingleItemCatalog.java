/* Gregorius Techneticies / GregTech-6 Team, LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.recipe;

import java.util.List;
import java.util.Map;

/** MultiItemRandomTools:63-73, MultiItemFood and GT_API_Proxy#getBurnTime. */
public final class OriginalSingleItemCatalog {
    private OriginalSingleItemCatalog() {}
    public record Compound(String item, String material) {}
    public static final List<Compound> COMPOUNDS = List.of(
            new Compound("bronze_compound", "Bronze"), new Compound("brass_compound", "Brass"),
            new Compound("bismuth_bronze_compound", "BismuthBronze"),
            new Compound("arsenic_copper_compound", "ArsenicCopper"),
            new Compound("arsenic_bronze_compound", "ArsenicBronze"));
    public static final List<String> ANIMAL_PARTS = List.of("tusk", "hoglin_tusk", "boar_tusk", "elephant_tusk",
            "hoof", "cow_hoof", "horse_hoof", "mule_hoof", "donkey_hoof", "deer_hoof",
            "horn", "cow_horn", "sheep_horn", "antler", "deer_antler");
    public static final List<String> PLANT_REMAINS = List.of("plant_remains", "fruit_remains", "vegetable_remains",
            "nut_remains", "apple_core", "apple_core_2", "apple_core_3", "apple_core_4");
    private static final Map<String, Integer> FUELS = Map.ofEntries(
            Map.entry("grass_2", 50), Map.entry("dry_grass", 100), Map.entry("moldy_grass", 50),
            Map.entry("rotten_grass", 50), Map.entry("rye", 50), Map.entry("oats", 50),
            Map.entry("barley", 50), Map.entry("rice", 50), Map.entry("rubber_resin", 100),
            Map.entry("plant_remains", 50), Map.entry("fruit_remains", 50), Map.entry("vegetable_remains", 50),
            Map.entry("nut_remains", 100), Map.entry("dry_bark", 50), Map.entry("apple_core", 50),
            Map.entry("apple_core_2", 50), Map.entry("apple_core_3", 50), Map.entry("apple_core_4", 50),
            Map.entry("bale_grass", 450), Map.entry("bale_grass_dry", 900), Map.entry("bale_grass_moldy", 450),
            Map.entry("bale_grass_rotten", 450), Map.entry("bale_rye", 450), Map.entry("bale_oat", 450),
            Map.entry("bale_barley", 450), Map.entry("bale_rice", 450));
    /** Null means the original gives this item no explicit override. */
    public static Integer burnTime(String namespace, String path) {
        if (namespace.equals("minecraft") && path.equals("hay_block")) return 450;
        return namespace.equals("gt6ce") ? FUELS.get(path) : null;
    }
}
