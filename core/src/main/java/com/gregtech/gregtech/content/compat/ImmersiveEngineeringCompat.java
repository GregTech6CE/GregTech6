package com.gregtech.gregtech.content.compat;

import java.util.ArrayList;
import java.util.List;

/** Rows remapped from GT6's Immersive Engineering compat onto the 1.20.1 and 1.21.1 jars. */
public final class ImmersiveEngineeringCompat {
    private static final String SOURCE = "Compat_Recipes_ImmersiveEngineering.java";
    private static final String[] WOODS = {
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"};
    private static final String[] OILS = {
            "Oil_Seed", "Oil_Lin", "Oil_Hemp", "Oil_Nut", "Oil_Olive", "Oil_Sunflower", "Oil_Creosote"};
    private static final String[] HAMMERED_PLATES = {
            "aluminum", "constantan", "copper", "electrum", "gold", "iron", "lead", "nickel", "silver", "steel", "uranium"};

    private ImmersiveEngineeringCompat() {}

    public static CompatSpecs.Module module() {
        var machines = new ArrayList<CompatSpecs.MachineRow>();
        machines.add(new CompatSpecs.MachineRow("saw/wooden_barrel", "Cutter", CompatSpecs.Op.SAW,
                List.of(item("immersiveengineering:wooden_barrel", 1)),
                List.of(item("minecraft:oak_planks", 6), form("dustSmall", "Wood", 2)),
                null, 0, 16, 96, false, 100, false, SOURCE + ":48"));
        machines.add(generify("generify/gt_to_ie", "gregtech:planks_treated", "immersiveengineering:treated_wood_horizontal", SOURCE + ":50"));
        machines.add(generify("generify/ie_to_gt", "immersiveengineering:treated_wood_horizontal", "gregtech:planks_treated", SOURCE + ":52"));
        machines.add(new CompatSpecs.MachineRow("compress/coke", "Compressor", CompatSpecs.Op.ONE,
                List.of(form("plateGem", "CoalCoke", 8)),
                List.of(item("immersiveengineering:coke", 1)),
                null, 0, 64, 64, false, 0, false, SOURCE + ":57"));
        machines.add(new CompatSpecs.MachineRow("shred/coke", "Shredder", CompatSpecs.Op.ONE,
                List.of(item("immersiveengineering:coke", 1)),
                List.of(item("immersiveengineering:dust_coke", 1)),
                null, 0, 16, 64, false, 0, false, SOURCE + ":60"));
        machines.add(new CompatSpecs.MachineRow("loom/hemp_fabric", "Loom", CompatSpecs.Op.TWO,
                List.of(new CompatSpecs.Stack.Tag("forge:fiber_hemp", "c:fiber_hemp", 8),
                        new CompatSpecs.Stack.Tag("forge:rods/wooden", "c:rods/wooden", 1)),
                List.of(item("immersiveengineering:hemp_fabric", 1)),
                null, 0, 16, 64, false, 0, true, SOURCE + ":63-66"));
        baths(machines);
        var crafting = List.of(
                new CompatSpecs.CraftingRow("compat/immersiveengineering/packaged_to_gt",
                        item("gregtech:planks_treated", 1),
                        List.of(item("immersiveengineering:treated_wood_packaged", 1)),
                        SOURCE + ":53"),
                new CompatSpecs.CraftingRow("compat/immersiveengineering/gt_to_horizontal",
                        item("immersiveengineering:treated_wood_horizontal", 1),
                        List.of(item("gregtech:planks_treated", 1)),
                        SOURCE + ":54"));
        var removals = new ArrayList<CompatSpecs.Removal>();
        for (String metal : HAMMERED_PLATES) {
            removals.add(new CompatSpecs.Removal("immersiveengineering:crafting/plate_" + metal + "_hammering",
                    SOURCE + ":69-93"));
        }
        return new CompatSpecs.Module("immersiveengineering", true, true, "Compat_Recipes_ImmersiveEngineering",
                List.copyOf(machines), crafting, List.of(), List.copyOf(removals), List.of(
                        SOURCE + ":51 fireproof treated planks are not registered",
                        SOURCE + ":55 fireproof treated planks are not registered",
                        SOURCE + ":58 external IC2 compressor bridge is not restored",
                        SOURCE + ":61 external pulverizer bridge is not restored",
                        "RecipeMapBath.java:70-74 plank oil baths already output gregtech:planks_treated",
                        "Loader_Recipes_Woods.java:157-172 Railcraft tie and creosote-wood outputs stay deferred"));
    }

    private static void baths(List<CompatSpecs.MachineRow> rows) {
        for (String wood : WOODS) for (String oil : OILS) {
            rows.add(bath("bath/stairs/" + wood + "/" + oil, "minecraft:" + wood + "_stairs",
                    "immersiveengineering:stairs_treated_wood_horizontal", oil, 75, 102, "RecipeMapBath.java:82-86"));
            rows.add(bath("bath/slab/" + wood + "/" + oil, "minecraft:" + wood + "_slab",
                    "immersiveengineering:slab_treated_wood_horizontal", oil, 50, 72, "RecipeMapBath.java:95-99"));
        }
    }

    private static CompatSpecs.MachineRow bath(String id, String input, String output, String fluid, int amount,
                                               long duration, String source) {
        return new CompatSpecs.MachineRow(id, "Bath", CompatSpecs.Op.BATH,
                List.of(item(input, 1)), List.of(item(output, 1)), fluid, amount, 0, duration, false, 0, false, source);
    }

    private static CompatSpecs.MachineRow generify(String id, String from, String to, String source) {
        return new CompatSpecs.MachineRow(id, "Generifier", CompatSpecs.Op.GENERIFY,
                List.of(item(from, 1)), List.of(item(to, 1)), null, 0, 0, 1, false, 0, false, source);
    }

    private static CompatSpecs.Stack.Item item(String id, int count) { return new CompatSpecs.Stack.Item(id, count); }

    private static CompatSpecs.Stack.Form form(String prefix, String material, int count) {
        return new CompatSpecs.Stack.Form(prefix, material, count);
    }
}
