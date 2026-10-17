package com.gregtech.gregtech.content.creative;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Stable family/voltage ordering shared by both native viewer boundaries. */
public final class EmiCatalogOrder {
    private static final List<String> VOLTAGES = List.of("ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "xv", "puv1");
    private static final List<String> HULLS = List.of("galvanized_steel", "aluminium", "stainless_steel", "chromium", "titanium", "tungsten_steel", "iridium", "osmium");
    private static final Pattern LEVEL = Pattern.compile("_(?:t)?(\\d+)$");
    private EmiCatalogOrder() {}
    public static String key(String path, String category) {
        String family = path;
        int tier = 0;
        for (int i = 0; i < VOLTAGES.size(); i++) if (path.endsWith("_" + VOLTAGES.get(i))) {
            family = path.substring(0, path.length() - VOLTAGES.get(i).length() - 1); tier = i + 1; break;
        }
        if (tier == 0) for (int i = 0; i < HULLS.size(); i++) if (path.endsWith("_" + HULLS.get(i))) {
            family = path.substring(0, path.length() - HULLS.get(i).length() - 1); tier = i + 2; break;
        }
        var number = LEVEL.matcher(path);
        if (number.find()) { family = path.substring(0, number.start()); tier = Integer.parseInt(number.group(1)); }
        int section = section(category);
        if (path.startsWith("fluid_item_")) section = 90;
        if (com.gregtech.gregtech.content.multiblock.SharedLargeMachineParts.DEFINITIONS.stream().anyMatch(p -> p.name().equals(path))) section = 50;
        return String.format(Locale.ROOT, "%02d/%s/%04d/%s", section, family, tier, path);
    }
    public static String machineKey(String path, String family, int tier, boolean multiblock) {
        return String.format(Locale.ROOT, "%02d/%s/%04d/%s", multiblock ? 50 : 40, family, tier, path);
    }
    private static int section(String category) {
        return switch (category) {
            case "stones", "construction", "woods", "foam", "panels", "scaffolds" -> 20;
            case "nature_foods", "bottles", "bumblebees", "books" -> 30;
            case "multiblocks" -> 50;
            case "fluids" -> 90;
            case "basic_machines", "engines", "burning_boxes", "steam_boilers", "heat_exchangers", "reactors",
                 "turbines", "heaters", "motors", "magnets", "transformers", "battery_boxes", "solar_panels",
                 "lasers", "dynamos", "quantum_energizers", "crystal_chargers", "laser_absorbers", "coolers",
                 "zpm", "magical_energy_production", "automatic_tools", "pumps", "computing", "storage",
                 "logistics", "hoppers", "sorting", "sensors", "smelting_crucibles", "crucibles_faucets", "molds" -> 40;
            case "pipes", "item_pipes", "wires", "long_distance_transport", "axles", "laser_wires", "redstone_wires" -> 60;
            default -> 10;
        };
    }
}
