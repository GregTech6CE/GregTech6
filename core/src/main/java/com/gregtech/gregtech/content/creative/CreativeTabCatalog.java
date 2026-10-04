package com.gregtech.gregtech.content.creative;
import java.util.List;
/** Original GT6 pages shared by both platforms; material prefixes keep their own pages. */
public final class CreativeTabCatalog {
    private CreativeTabCatalog() {}
    public static final List<String> FAMILIES=List.of("tools","stones","construction","woods","fluids","pipes","item_pipes","wires","technology","iconsets","chests","safes","crafting_tables","storage","logistics","hoppers","scaffolds","smelting_crucibles","crucibles_faucets","molds","burning_boxes","steam_boilers","engines","heat_exchangers","reactors","turbines","heaters","motors","magnets","transformers","battery_boxes","solar_panels","long_distance_transport","lasers","dynamos","quantum_energizers","crystal_chargers","laser_absorbers","coolers","zpm","magical_energy_production","batteries","portable_power_cells","automatic_tools","pumps","computing","multiblocks","basic_machines","axles","fluid_containers","laser_wires","redstone_wires","extenders","sorting","sensors","portals","foam","untyped","panels","ropes","tool_blocks","coins","equipment","bottles","nature_foods","bumblebees");
    public static String title(String id){return "itemGroup.gregtech."+id;}
}
