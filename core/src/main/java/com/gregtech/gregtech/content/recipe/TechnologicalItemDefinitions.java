package com.gregtech.gregtech.content.recipe;
import java.util.*;
/** Original technological IDs, aliases and names; covers and components share these identities. */
public final class TechnologicalItemDefinitions {
    private TechnologicalItemDefinitions() {}
    public static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("shape_press_bulletcasingsmall", "press_bullet_casing_shape_small"),
            Map.entry("shape_press_bulletcasingmedium", "press_bullet_casing_shape_medium"),
            Map.entry("shape_press_bulletcasinglarge", "press_bullet_casing_shape_large"),
            Map.entry("slicer_shape_eigths", "slicer_shape_eights"),
            // IL.Module_*_Generator are multi-items, registered from their display name
            // ("Stone Generator Module" -> GTMultiItemsGen "stone_generator_module"); GT6's
            // Extruder rows name them as recipe keys (Loader_Recipes_Extruder:44-151).
            Map.entry("module_stone_generator", "stone_generator_module"),
            Map.entry("module_basalt_generator", "basalt_generator_module"),
            Map.entry("module_blackstone_generator", "blackstone_generator_module"),
            // Same story for GT6's food, clay and vanilla-mapped items: the transpiler spells them
            // snake(IL field) ("Food_PotatoChips" -> food_potatochips) while the port registers
            // snake(display name) ("Potato Chips" -> potato_chips). Derived by
            // tools/derive_missing_tech_aliases.py from GT6's own registrations.
            Map.entry("food_butter", "butter"),
            Map.entry("food_butter_salted", "salted_butter"),
            Map.entry("food_cheese", "cheese"),
            Map.entry("food_dough", "dough"),
            Map.entry("food_dough_abyssal", "abyssal_dough"),
            Map.entry("food_potatochips", "potato_chips"),
            Map.entry("food_chilichips", "chili_chips"),
            Map.entry("clay_ball_brown", "brown_clay"),
            Map.entry("clay_ball_red", "red_clay"),
            Map.entry("clay_ball_yellow", "yellow_clay"),
            Map.entry("clay_ball_blue", "blue_clay"),
            Map.entry("clay_ball_white", "white_clay"),
            Map.entry("crop_wheat", "minecraft:wheat"),
            Map.entry("bale_wheat", "minecraft:hay_block"),
            Map.entry("dye_bonemeal", "minecraft:bone_meal"),
            Map.entry("food_potato_poisonous", "minecraft:poisonous_potato"),
            Map.entry("remains_plant", "plant_remains"),
            // IL.Dynamite_Strong is the block whose display name gives the port's
            // "strong_dynamite" id (GT6 MultiTileEntityDynamite, Loader_MultiTileEntities:2237).
            Map.entry("dynamite_strong", "strong_dynamite"));

    // ---- English name derivation ----

    private static final Map<String, String> NAME_OVERRIDES = new LinkedHashMap<>();
    static {
        // Cover names
        put("activity_detector_possible", "Activity Detector (Possible)");
        put("activity_detector_processing", "Activity Detector (Processing)");
        put("activity_detector_running", "Activity Detector (Running)");
        put("activity_detector_success", "Activity Detector (Success)");
        put("air_vent", "Air Vent");
        put("auto_reboot_switch_1m", "Auto Reboot Switch (1 min)");
        put("auto_reboot_switch_5m", "Auto Reboot Switch (5 mins)");
        put("auto_reboot_switch_10m", "Auto Reboot Switch (10 mins)");
        put("auto_reboot_switch_20m", "Auto Reboot Switch (20 mins)");
        put("auto_reboot_switch_30m", "Auto Reboot Switch (30 mins)");
        put("auto_redstone_machine_switch", "Auto Redstone Machine Switch");
        put("automatic_machine_switch", "Automatic Machine Switch");
        put("blank_cover", "Blank Cover");
        put("button_panel_selector", "Button Panel Selector");
        put("cover_controller", "Cover Controller");
        put("crafting_table_cover", "Crafting Table Cover");
        put("drain", "Drain");
        put("energy_display_cover", "Energy Display Cover");
        put("energy_sensor", "Energy Sensor");
        put("fluid_filter", "Fluid Filter");
        put("item_filter", "Item Filter");
        put("item_retriever_cover", "Item Retriever Cover");
        put("machine_status_display_cover", "Machine Status Display Cover");
        put("manual_selector", "Manual Selector");
        put("pressure_value", "Pressure Valve");
        put("progress_sensor", "Progress Sensor");
        put("redstone_conductor_cover_accept", "Redstone Conductor Cover (Accept)");
        put("redstone_conductor_cover_emit", "Redstone Conductor Cover (Emit)");
        put("redstone_emitter", "Redstone Emitter");
        put("redstone_machine_switch", "Redstone Machine Switch");
        put("redstone_selector", "Redstone Selector");
        put("shutter_cover", "Shutter Cover");
        put("warning_cover", "Warning Cover");

        // Logistics
        put("filtered_logistics_export_bus_fluid", "Filtered Logistics Export Bus (Fluid)");
        put("filtered_logistics_export_bus_item", "Filtered Logistics Export Bus (Item)");
        put("filtered_logistics_import_bus_fluid", "Filtered Logistics Import Bus (Fluid)");
        put("filtered_logistics_import_bus_item", "Filtered Logistics Import Bus (Item)");
        put("filtered_logistics_storage_bus_fluid", "Filtered Logistics Storage Bus (Fluid)");
        put("filtered_logistics_storage_bus_item", "Filtered Logistics Storage Bus (Item)");
        put("generic_logistics_export_bus", "Generic Logistics Export Bus");
        put("generic_logistics_import_bus", "Generic Logistics Import Bus");
        put("generic_logistics_storage_bus", "Generic Logistics Storage Bus");
        put("logistics_display_cpu_control", "Logistics Display (CPU Control)");
        put("logistics_display_cpu_conversion", "Logistics Display (CPU Conversion)");
        put("logistics_display_cpu_logic", "Logistics Display (CPU Logic)");
        put("logistics_display_cpu_storage", "Logistics Display (CPU Storage)");
        put("logistics_dump_bus_item", "Logistics Dump Bus (Item)");

        // Lasers
        put("laser_emitter_argon", "Argon Laser Emitter");
        put("laser_emitter_carbondioxide", "Carbon Dioxide Laser Emitter");
        put("laser_emitter_carbonmonoxide", "Carbon Monoxide Laser Emitter");
        put("laser_emitter_emptygas", "Empty Gas Laser Emitter");
        put("laser_emitter_helium", "Helium Laser Emitter");
        put("laser_emitter_heliumneon", "Helium-Neon Laser Emitter");
        put("laser_emitter_krypton", "Krypton Laser Emitter");
        put("laser_emitter_neon", "Neon Laser Emitter");
        put("laser_emitter_xenon", "Xenon Laser Emitter");

        // USB
        put("usb1_stick", "USB 1 Memory Stick");
        put("usb2_stick", "USB 2 Memory Stick");
        put("usb3_stick", "USB 3 Memory Stick");
        put("usb4_stick", "USB 4 Memory Stick");
        put("usb1_cable", "USB 1 Cable");
        put("usb2_cable", "USB 2 Cable");
        put("usb3_cable", "USB 3 Cable");
        put("usb4_cable", "USB 4 Cable");
        put("usb1_hdd", "USB 1 HDD");
        put("usb2_hdd", "USB 2 HDD");
        put("usb3_hdd", "USB 3 HDD");
        put("usb4_hdd", "USB 4 HDD");

        // Crystal circuits / processors
        put("crystal_circuit_diamond", "Diamond Crystal Circuit");
        put("crystal_circuit_emerald", "Emerald Crystal Circuit");
        put("crystal_circuit_ruby", "Ruby Crystal Circuit");
        put("crystal_circuit_sapphire", "Sapphire Crystal Circuit");
        put("crystal_processor_diamond", "Diamond Crystal Processor");
        put("crystal_processor_emerald", "Emerald Crystal Processor");
        put("crystal_processor_ruby", "Ruby Crystal Processor");
        put("crystal_processor_sapphire", "Sapphire Crystal Processor");
        put("crystal_processor_socket", "Crystal Processor Socket");

        // Circuit parts
        put("circuit_plate", "Circuit Plate");
        put("circuit_plate_copper", "Circuit Plate (Copper)");
        put("circuit_plate_gold", "Circuit Plate (Gold)");
        put("circuit_plate_platinum", "Circuit Plate (Platinum)");
        put("circuit_plate_magic", "Circuit Plate (Magic)");
        put("circuit_plate_enderium", "Circuit Plate (Enderium)");
        put("circuit_plate_signalum", "Circuit Plate (Signalum)");
        put("circuit_plate_hsla", "Circuit Plate (HSLA)");
        put("circuit_wire_enderium", "Circuit Wiring (Enderium)");
        put("circuit_wire_gold", "Circuit Wiring (Gold)");
        put("circuit_wire_magic", "Circuit Wiring (Magic)");
        put("circuit_wire_platinum", "Circuit Wiring (Platinum)");
        put("circuit_wire_signalum", "Circuit Wiring (Signalum)");
        put("circuit_wiring_copper", "Circuit Wiring (Copper)");
        put("circuit_board_basic", "Circuit Board (Basic)");
        put("circuit_board_good", "Circuit Board (Good)");
        put("circuit_board_advanced", "Circuit Board (Advanced)");
        put("circuit_board_elite", "Circuit Board (Elite)");
        put("circuit_board_master", "Circuit Board (Master)");
        put("circuit_board_ultimate", "Circuit Board (Ultimate)");
        put("circuit_board_magic", "Circuit Board (Magic)");
        put("circuit_board_enderium", "Circuit Board (Enderium)");
        put("circuit_board_signalum", "Circuit Board (Signalum)");
        put("circuit_board_hsla_circuit", "Circuit Board (HSLA)");
        put("circuit_board_power_module", "Power Module Circuit Board");
        put("circuit_part_basic", "Circuit Part (Basic)");
        put("circuit_part_good", "Circuit Part (Good)");
        put("circuit_part_advanced", "Circuit Part (Advanced)");
        put("circuit_part_elite", "Circuit Part (Elite)");
        put("circuit_part_master", "Circuit Part (Master)");
        put("circuit_part_ultimate", "Circuit Part (Ultimate)");
        put("circuit_part_magic", "Circuit Part (Magic)");
        put("circuit_part_enderium", "Circuit Part (Enderium)");
        put("circuit_part_signalum", "Circuit Part (Signalum)");
        put("circuit_part_enderpearl", "Circuit Part (Ender Pearl)");
        put("circuit_part_endereye", "Circuit Part (Ender Eye)");

        // Circuits
        put("circuit_basic", "Basic Circuit");
        put("circuit_good", "Good Circuit");
        put("circuit_advanced", "Advanced Circuit");
        put("circuit_elite", "Elite Circuit");
        put("circuit_master", "Master Circuit");
        put("circuit_ultimate", "Ultimate Circuit");
        put("circuit_magic", "Magic Circuit");
        put("circuit_enderium", "Enderium Circuit");
        put("circuit_signalum", "Signalum Circuit");

        // Battery cells
        put("lead_acid_cell_empty", "Lead-Acid Cell (Empty)");
        put("lead_acid_cell_filled", "Lead-Acid Cell (Filled)");
        put("alkaline_button_cell_empty", "Alkaline Button Cell (Empty)");
        put("alkaline_button_cell_filled", "Alkaline Button Cell (Filled)");
        put("nickel_cadmium_cell_empty", "Nickel-Cadmium Cell (Empty)");
        put("nickel_cadmium_cell_filled", "Nickel-Cadmium Cell (Filled)");
        put("lithium_cobalt_cell_empty", "Lithium-Cobalt Cell (Empty)");
        put("lithium_cobalt_cell_filled", "Lithium-Cobalt Cell (Filled)");
        put("lithium_manganese_cell_empty", "Lithium-Manganese Cell (Empty)");
        put("lithium_manganese_cell_filled", "Lithium-Manganese Cell (Filled)");

        // Slicer shapes
        put("slicer_shape_empty", "Slicer Blade Frame");
        put("slicer_shape_flat", "Slicer Blades (Flat)");
        put("slicer_shape_grid", "Slicer Blades (Grid)");
        put("slicer_shape_eights", "Slicer Blades (Eighths)");
        put("slicer_shape_eights_hollow", "Slicer Blades (Hollow Eighths)");
        put("slicer_shape_split", "Slicer Blades (Split)");
        put("slicer_shape_quaters", "Slicer Blades (Quarters)");
        put("slicer_shape_quaters_hollow", "Slicer Blades (Hollow Quarters)");

        // Press shapes
        put("press_bullet_casing_shape_small", "Bullet Casing Mold (Small)");
        put("press_bullet_casing_shape_medium", "Bullet Casing Mold (Medium)");
        put("press_bullet_casing_shape_large", "Bullet Casing Mold (Large)");
    }

    private static void put(String id, String name) { NAME_OVERRIDES.put(id, name); }

    public static String englishName(String id) {
        String over = NAME_OVERRIDES.get(id);
        if (over != null) return over;

        // Auto-generate: split by _, capitalize, handle voltage suffixes
        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();
        boolean nextCap = true;
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            // Handle double-underscore in "compact_electric_conveyor_*"
            if (i > 0) sb.append(' ');

            // Voltage suffix
            String volt = voltageName(p);
            if (volt != null && i == parts.length - 1) {
                sb.append('(').append(volt.toUpperCase()).append(')');
            } else {
                sb.append(Character.toUpperCase(p.charAt(0)));
                if (p.length() > 1) sb.append(p.substring(1));
            }
        }
        return sb.toString().trim();
    }

    private static String voltageName(String s) {
        return switch (s) {
            case "ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "puv1", "xv" -> s;
            default -> null;
        };
    }

    // ---- Item ID list (matches texture filenames without .png) ----

    public static final String[] IDS = {
        // ===== Voltage-tiered machine components =====
        // Motors
        "compact_electric_motor_ulv", "compact_electric_motor_lv", "compact_electric_motor_mv",
        "compact_electric_motor_hv", "compact_electric_motor_ev", "compact_electric_motor_iv",
        "compact_electric_motor_luv", "compact_electric_motor_zpm", "compact_electric_motor_uv",
        "compact_electric_motor_puv1", "compact_electric_motor_xv",
        // Pumps
        "compact_electric_pump_ulv", "compact_electric_pump_lv", "compact_electric_pump_mv",
        "compact_electric_pump_hv", "compact_electric_pump_ev", "compact_electric_pump_iv",
        "compact_electric_pump_luv", "compact_electric_pump_zpm", "compact_electric_pump_uv",
        "compact_electric_pump_puv1", "compact_electric_pump_xv",
        // Conveyors
        "compact_electric_conveyor_ulv", "compact_electric_conveyor_lv", "compact_electric_conveyor_mv",
        "compact_electric_conveyor_hv", "compact_electric_conveyor_ev", "compact_electric_conveyor_iv",
        "compact_electric_conveyor_luv", "compact_electric_conveyor_zpm", "compact_electric_conveyor_uv",
        "compact_electric_conveyor_puv1", "compact_electric_conveyor_xv",
        // Pistons
        "compact_electric_piston_ulv", "compact_electric_piston_lv", "compact_electric_piston_mv",
        "compact_electric_piston_hv", "compact_electric_piston_ev", "compact_electric_piston_iv",
        "compact_electric_piston_luv", "compact_electric_piston_zpm", "compact_electric_piston_uv",
        "compact_electric_piston_puv1", "compact_electric_piston_xv",
        // Robot Arms
        "compact_robot_arm_ulv", "compact_robot_arm_lv", "compact_robot_arm_mv",
        "compact_robot_arm_hv", "compact_robot_arm_ev", "compact_robot_arm_iv",
        "compact_robot_arm_luv", "compact_robot_arm_zpm", "compact_robot_arm_uv",
        "compact_robot_arm_puv1", "compact_robot_arm_xv",
        // Field Generators
        "compact_force_field_emitter_ulv", "compact_force_field_emitter_lv", "compact_force_field_emitter_mv",
        "compact_force_field_emitter_hv", "compact_force_field_emitter_ev", "compact_force_field_emitter_iv",
        "compact_force_field_emitter_luv", "compact_force_field_emitter_zpm", "compact_force_field_emitter_uv",
        "compact_force_field_emitter_puv1", "compact_force_field_emitter_xv",
        // Emitters
        "compact_signal_emitter_ulv", "compact_signal_emitter_lv", "compact_signal_emitter_mv",
        "compact_signal_emitter_hv", "compact_signal_emitter_ev", "compact_signal_emitter_iv",
        "compact_signal_emitter_luv", "compact_signal_emitter_zpm", "compact_signal_emitter_uv",
        "compact_signal_emitter_puv1", "compact_signal_emitter_xv",
        // Sensors
        "compact_sensor_ulv", "compact_sensor_lv", "compact_sensor_mv",
        "compact_sensor_hv", "compact_sensor_ev", "compact_sensor_iv",
        "compact_sensor_luv", "compact_sensor_zpm", "compact_sensor_uv",
        "compact_sensor_puv1", "compact_sensor_xv",

        // ===== Covers =====
        "blank_cover", "crafting_table_cover", "machine_status_display_cover",
        "automatic_machine_switch", "energy_display_cover", "redstone_machine_switch",
        "auto_redstone_machine_switch", "redstone_selector", "manual_selector",
        "auto_reboot_switch_1m", "auto_reboot_switch_5m", "auto_reboot_switch_10m",
        "auto_reboot_switch_20m", "auto_reboot_switch_30m",
        "energy_sensor", "activity_detector_possible", "activity_detector_running",
        "activity_detector_processing", "progress_sensor", "activity_detector_success",
        "drain", "redstone_emitter", "air_vent", "item_filter", "fluid_filter",
        "cover_controller", "shutter_cover", "button_panel_selector", "warning_cover",
        "redstone_conductor_cover_accept", "redstone_conductor_cover_emit",
        "item_retriever_cover", "pressure_value",

        // The pump/conveyor/robot-arm covers, the redstone torch/repeater covers and the tag
        // selector are NOT separate items: GT6 gives those roles to the compact electric pump /
        // conveyor / robot arm (MultiItemTechnological:50-53), to the vanilla redstone torch and
        // repeater (GT_API:799-802) and to the programmed circuit (ItemIntegratedCircuit:87).
        // See content/cover/CoverItems.

        // ===== Logistics =====
        "logistics_display_cpu_logic", "logistics_display_cpu_control",
        "logistics_display_cpu_storage", "logistics_display_cpu_conversion",
        "filtered_logistics_export_bus_fluid", "filtered_logistics_import_bus_fluid",
        "filtered_logistics_storage_bus_fluid",
        "filtered_logistics_export_bus_item", "filtered_logistics_import_bus_item",
        "filtered_logistics_storage_bus_item",
        "generic_logistics_export_bus", "generic_logistics_import_bus",
        "generic_logistics_storage_bus", "logistics_dump_bus_item",

        // ===== Circuit components =====
        "circuit_plate", "circuit_wiring_copper", "circuit_plate_copper",
        "circuit_wire_gold", "circuit_plate_gold",
        "circuit_wire_platinum", "circuit_plate_platinum",
        "circuit_wire_magic", "circuit_plate_magic",
        "circuit_wire_enderium", "circuit_plate_enderium",
        "circuit_wire_signalum", "circuit_plate_signalum",
        "circuit_plate_hsla",
        "circuit_board_basic", "circuit_board_good", "circuit_board_advanced",
        "circuit_board_elite", "circuit_board_master", "circuit_board_ultimate",
        "circuit_board_magic", "circuit_board_enderium", "circuit_board_signalum",
        "circuit_board_hsla_circuit", "circuit_board_power_module",
        "circuit_part_basic", "circuit_part_good", "circuit_part_advanced",
        "circuit_part_elite", "circuit_part_master", "circuit_part_ultimate",
        "circuit_part_magic", "circuit_part_enderium", "circuit_part_signalum",
        "circuit_part_enderpearl", "circuit_part_endereye",

        // ===== Finished circuits =====
        "circuit_basic", "circuit_good", "circuit_advanced", "circuit_elite",
        "circuit_master", "circuit_ultimate", "circuit_magic", "circuit_enderium",
        "circuit_signalum",

        // ===== Crystal circuits & processors =====
        "crystal_circuit_diamond", "crystal_circuit_ruby", "crystal_circuit_emerald",
        "crystal_circuit_sapphire",
        "crystal_processor_socket",
        "crystal_processor_diamond", "crystal_processor_ruby",
        "crystal_processor_emerald", "crystal_processor_sapphire",

        // ===== USB items =====
        "usb1_stick", "usb2_stick", "usb3_stick", "usb4_stick",
        "usb1_cable", "usb2_cable", "usb3_cable", "usb4_cable",
        "usb1_hdd", "usb2_hdd", "usb3_hdd", "usb4_hdd",

        // ===== Battery cells =====
        "lead_acid_cell_empty", "lead_acid_cell_filled",
        "alkaline_button_cell_empty", "alkaline_button_cell_filled",
        "nickel_cadmium_cell_empty", "nickel_cadmium_cell_filled",
        "lithium_cobalt_cell_empty", "lithium_cobalt_cell_filled",
        "lithium_manganese_cell_empty", "lithium_manganese_cell_filled",

        // ===== Laser emitters =====
        "laser_emitter_emptygas", "laser_emitter_helium", "laser_emitter_neon",
        "laser_emitter_argon", "laser_emitter_krypton", "laser_emitter_xenon",
        "laser_emitter_heliumneon", "laser_emitter_carbonmonoxide", "laser_emitter_carbondioxide",

        // ===== Extruder shapes =====
        "extruder_shape_empty", "extruder_shape_plate", "extruder_shape_longrod",
        "extruder_shape_bolt", "extruder_shape_ring", "extruder_shape_cell",
        "extruder_shape_ingot", "extruder_shape_wire", "extruder_shape_casing",
        "extruder_shape_tinypipe", "extruder_shape_smallpipe", "extruder_shape_mediumpipe",
        "extruder_shape_largepipe", "extruder_shape_hugepipe", "extruder_shape_block",
        "extruder_shape_swordblade", "extruder_shape_pickaxehead", "extruder_shape_shovelhead",
        "extruder_shape_axehead", "extruder_shape_hoehead", "extruder_shape_hammerhead",
        "extruder_shape_filehead", "extruder_shape_sawblade", "extruder_shape_gear",
        "extruder_shape_bottle", "extruder_shape_curvedplate", "extruder_shape_smallgear",
        "extruder_shape_rod", "extruder_shape_capsulecellcontainer", "extruder_shape_foil",
        "extruder_shape_tinyplate", "extruder_shape_finewire",

        // ===== Low-heat extruder shapes =====
        "low_heat_extruder_shape_empty", "low_heat_extruder_shape_plate",
        "low_heat_extruder_shape_longrod", "low_heat_extruder_shape_bolt",
        "low_heat_extruder_shape_ring", "low_heat_extruder_shape_cell",
        "low_heat_extruder_shape_ingot", "low_heat_extruder_shape_wire",
        "low_heat_extruder_shape_casing", "low_heat_extruder_shape_tinypipe",
        "low_heat_extruder_shape_smallpipe", "low_heat_extruder_shape_mediumpipe",
        "low_heat_extruder_shape_largepipe", "low_heat_extruder_shape_hugepipe",
        "low_heat_extruder_shape_block", "low_heat_extruder_shape_swordblade",
        "low_heat_extruder_shape_pickaxehead", "low_heat_extruder_shape_shovelhead",
        "low_heat_extruder_shape_axehead", "low_heat_extruder_shape_hoehead",
        "low_heat_extruder_shape_hammerhead", "low_heat_extruder_shape_filehead",
        "low_heat_extruder_shape_sawblade", "low_heat_extruder_shape_gear",
        "low_heat_extruder_shape_bottle", "low_heat_extruder_shape_curvedplate",
        "low_heat_extruder_shape_smallgear", "low_heat_extruder_shape_rod", "low_heat_extruder_shape_capsulecellcontainer",
        "low_heat_extruder_shape_foil", "low_heat_extruder_shape_tinyplate",
        "low_heat_extruder_shape_finewire",

        // ===== Food mold shapes =====
        "foodmold_shape_empty", "foodmold_shape_bun", "foodmold_shape_bread",
        "foodmold_shape_baguette", "foodmold_shape_cylinder", "foodmold_shape_toast",

        // ===== Press shapes =====
        "press_bullet_casing_shape_small", "press_bullet_casing_shape_medium",
        "press_bullet_casing_shape_large",

        // ===== Slicer shapes =====
        "slicer_shape_empty", "slicer_shape_flat", "slicer_shape_grid",
        "slicer_shape_eights", "slicer_shape_eights_hollow",
        "slicer_shape_split", "slicer_shape_quaters", "slicer_shape_quaters_hollow",
    };

    static {
        // Validate: every ID must have a corresponding texture
        for (String id : IDS) {
            if (id.isEmpty()) throw new IllegalStateException("Empty tech item ID");
        }
    }
}
