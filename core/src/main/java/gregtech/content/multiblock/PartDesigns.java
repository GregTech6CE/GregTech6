package gregtech.content.multiblock;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Original {@code NBT_DESIGNS} and {@code NBT_TEXTURE} for structural parts.
 * {@code max} is the highest index, inclusive. A part whose original count is 0 is absent:
 * a layout may not name a design for it.
 */
public final class PartDesigns {
    public record Spec(int max, String folder) {}

    private static final Map<String, Spec> BY_PATH = specs();

    private PartDesigns() {}

    /** Highest allowed index, or {@code -1} when this part has no design index. */
    public static int max(String path) {
        Spec spec = BY_PATH.get(path);
        return spec == null ? -1 : spec.max();
    }

    /** Texture folder under {@code machines/multiblockparts}, or null when {@link #max} is {@code -1}. */
    public static String folder(String path) {
        Spec spec = BY_PATH.get(path);
        return spec == null ? null : spec.folder();
    }

    public static Set<String> paths() {
        return Collections.unmodifiableSet(BY_PATH.keySet());
    }

    /** Texture folder to the highest index that folder uses. */
    public static Map<String, Integer> folders() {
        var map = new LinkedHashMap<String, Integer>();
        for (Spec spec : BY_PATH.values()) map.merge(spec.folder(), spec.max(), Math::max);
        return Map.copyOf(map);
    }

    private static Map<String, Spec> specs() {
        var map = new LinkedHashMap<String, Spec>();
        metal(
                map,
                "metalwall",
                "lead_wall",
                "bronze_wall",
                "steel_wall",
                "galvanized_steel_wall",
                "tank_wall",
                "invar_wall",
                "titanium_wall",
                "tungstensteel_wall",
                "tungsten_wall",
                "tantalum_hafnium_carbide_wall",
                "adamantium_wall");
        metal(
                map,
                "metalwalldense",
                "dense_lead_wall",
                "dense_bronze_wall",
                "dense_steel_wall",
                "dense_galvanized_steel_wall",
                "tank_wall_dense",
                "dense_invar_wall",
                "dense_titanium_wall",
                "implosion_compressor_wall",
                "dense_tungsten_wall",
                "dense_tantalum_hafnium_carbide_wall",
                "dense_adamantium_wall");
        put(
                map,
                "coil",
                1,
                "large_copper_coil",
                "large_niobium_titanium_coil",
                "large_nichrome_coil",
                "large_carborundum_coil",
                "large_osmium_coil",
                "large_iridium_coil");
        map.put("centrifuge_part", new Spec(8, "centrifugeparts"));
        map.put("electrolyzer_part", new Spec(7, "electrolyzerparts"));
        map.put("distillation_tower_part", new Spec(1, "distillationtowerparts"));
        map.put("sluice_part", new Spec(7, "sluiceparts"));
        map.put("crusher_wheels", new Spec(3, "crusherwheels"));
        map.put("shredder_blades", new Spec(3, "shredderblades"));
        return Map.copyOf(map);
    }

    private static void metal(Map<String, Spec> map, String folder, String... paths) {
        put(map, folder, 7, paths);
    }

    private static void put(Map<String, Spec> map, String folder, int max, String... paths) {
        Spec spec = new Spec(max, folder);
        for (String path : paths) map.put(path, spec);
    }
}
