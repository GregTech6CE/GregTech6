package com.gregtech.gregtech.content.creative;
import java.util.List;
/** Stable source tab identities and translation contract, shared by both platforms. */
public final class CreativeTabCatalog {private CreativeTabCatalog(){}public static final List<String> FAMILIES=List.of("tools","stones","smelting_crucibles","burning_boxes","basic_machines","fluids","pipes","item_pipes","fluid_containers","hoppers","wires","engines","axles","technology","iconsets","storage","energy_nodes","multiblocks","multi_items");public static String title(String id){return "itemGroup.gregtech."+id;}}
