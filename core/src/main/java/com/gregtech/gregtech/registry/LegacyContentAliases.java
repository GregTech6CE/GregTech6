package com.gregtech.gregtech.registry;

import java.util.LinkedHashMap;
import java.util.Map;

/** Duplicate port identities, not alternate GT materials or machine grades.
 * Old IDs resolve to the canonical registry object; no second item/block is registered.
 */
public final class LegacyContentAliases {
    private LegacyContentAliases() {}
    public static final Map<String, String> ALIASES = aliases();
    private static Map<String, String> aliases() {
        var result = new LinkedHashMap<String, String>();
        for (var material : com.gregtech.gregtech.content.storage.SafeLockRules.KEY_MATERIALS)
            result.put("key_" + material, material + "_key");
        for (String tier : java.util.List.of("basic", "advanced", "elite", "wireless"))
            result.put("extender_" + tier, "inventory_tank_extender");
        result.put("large_crucible_main", "large_stainless_steel_crucible");
        result.put("large_boiler_main", "stainless_steel_boiler_main_barometer");
        result.put("bedrock_drill_main", "bedrock_mining_drill_controller");
        result.put("lightning_rod_main", "lightning_rod_electric_output");
        result.put("bedrock_drill_wall", "dense_titanium_wall");
        result.put("lightning_rod_wall", "tungsten_wall");
        result.put("lightning_rod_pillar", "lightning_rod");
        result.put("long_dist_wire", "long_dist_wire_ev");
        result.put("long_dist_pipe", "long_dist_pipe_fluid");
        result.put("mass_storage", "mass_storage_stainless_steel");
        result.put("bumble_hive", "bumble_hive_light_gray");
        result.put("fuel_rod_naquadah", "reactor_rod_9440");
        for (int tier = 0; tier < 5; tier++) {
            var battery = new com.gregtech.gregtech.content.energy.ChemicalBatterySpec(
                    com.gregtech.gregtech.content.energy.ChemicalBatterySpec.Chemistry.LITHIUM_COBALT, tier);
            result.put("battery_eu_" + battery.voltage(), battery.id());
        }
        result.put("coke_oven_wall", "fire_bricks");
        result.put("large_crucible_wall", "fire_bricks"); // Original part 18000
        result.put("boiler_wall", "tank_wall"); // 18002
        result.put("turbine_wall", "tank_wall_dense"); // 18022
        result.put("large_gas_turbine_wall", "tank_wall_dense"); // 18022
        result.put("large_dynamo_wall", "large_copper_coil"); // 18040
        result.put("fusion_reactor_wall", "large_iridium_coil"); // 18045
        result.put("heat_exchanger_wall", "heat_transmitter"); // 18101
        result.put("cryo_distillation_wall", "distillation_tower_part"); // 18102
        result.put("zpm_discharger_elite", "zpm_discharger_basic"); // Only 11170 QU / 11171 EU exist in GT6
        result.put("tank_3x3", "small_stainless_steel_tank_main_valve");
        result.put("tank_5x5", "large_stainless_steel_tank_main_valve");
        for (var suffix : java.util.List.of("", "_dry", "_moldy", "_rotten")) result.put("grass" + suffix, "bale_grass" + suffix);
        result.putAll(com.gregtech.gregtech.content.multiblock.OriginalGeneratorTooltipData.aliases());
        return java.util.Collections.unmodifiableMap(result);
    }
    public static String canonical(String path) { return ALIASES.getOrDefault(path, path); }
    public static boolean legacy(String path) { return ALIASES.containsKey(path); }
    public static boolean originalKey(String path) {
        return path.endsWith("_key") && com.gregtech.gregtech.content.storage.SafeLockRules.KEY_MATERIALS
                .contains(path.substring(0, path.length() - 4));
    }
}
