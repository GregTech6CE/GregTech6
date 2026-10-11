package com.gregtech.gregtech.content.compat;

import java.util.ArrayList;
import java.util.List;

/**
 * Rows remapped from GT6's HarvestCraft compat onto Pam's HarvestCraft 2 Food Core.
 * Crops, trees and food extended are separate mods and stay out of this runtime.
 */
public final class HarvestCraftCompat {
    private static final String SOURCE = "Compat_Recipes_HarvestCraft.java";
    private static final String MOD = "pamhc2foodcore";
    private static final String[] WATERS = {"Water", "MnWtr", "DistW", "SpDew"};

    private HarvestCraftCompat() {}

    public static CompatSpecs.Module module() {
        var machines = new ArrayList<CompatSpecs.MachineRow>();
        machines.add(item("shred/sunflower", "Shredder", CompatSpecs.Op.ONE,
                List.of(item("minecraft:sunflower", 1)),
                List.of(item(MOD + ":sunflowerseedsitem", 1)),
                16, 16, SOURCE + ":103"));
        machines.add(pair("mix/beefjerky/small", item("minecraft:beef", 1), form("dustSmall", "Salt", 1),
                item(MOD + ":beefjerkyitem", 1), 16, SOURCE + ":117"));
        machines.add(pair("mix/beefjerky/tiny", item("minecraft:beef", 1), form("dustTiny", "Salt", 3),
                item(MOD + ":beefjerkyitem", 1), 16, SOURCE + ":119"));
        machines.add(pair("mix/beefjerky/dust", item("minecraft:beef", 4), form("dust", "Salt", 1),
                item(MOD + ":beefjerkyitem", 4), 64, SOURCE + ":121"));
        machines.add(pair("mix/powdereddonut/small", item(MOD + ":plaindonutitem", 1), form("dustSmall", "Sugar", 1),
                item(MOD + ":powdereddonutitem", 1), 16, SOURCE + ":281"));
        machines.add(pair("mix/powdereddonut/tiny", item(MOD + ":plaindonutitem", 1), form("dustTiny", "Sugar", 3),
                item(MOD + ":powdereddonutitem", 1), 16, SOURCE + ":282"));
        machines.add(pair("mix/powdereddonut/dust", item(MOD + ":plaindonutitem", 4), form("dust", "Sugar", 1),
                item(MOD + ":powdereddonutitem", 4), 64, SOURCE + ":283"));
        machines.add(fluid("bath/chocolatedonut", "Bath", item(MOD + ":plaindonutitem", 1),
                item(MOD + ":chocolatedonutitem", 1), "GenMolten_Chocolate", 36, 0, 16, SOURCE + ":279"));
        for (String water : WATERS) {
            machines.add(fluid("mix/dough/" + water.toLowerCase(), "Mixer", item(MOD + ":flouritem", 1),
                    item("gregtech:dough", 1), water, 1000, 16, 16, SOURCE + ":128-129"));
        }
        return new CompatSpecs.Module(MOD, true, true, "Compat_Recipes_HarvestCraft",
                List.copyOf(machines), List.of(), List.of(), List.of(
                        new CompatSpecs.Removal(MOD + ":beefjerkyitem", SOURCE + ":54"),
                        new CompatSpecs.Removal(MOD + ":powdereddonutitem", SOURCE + ":54"),
                        new CompatSpecs.Removal(MOD + ":chocolatedonutitem", SOURCE + ":54"),
                        new CompatSpecs.Removal(MOD + ":doughitem_x2", SOURCE + ":54")),
                List.of(
                        SOURCE + ":57-60 candles are not in food core",
                        SOURCE + ":68-82 shapeless food rewriting stays deferred",
                        SOURCE + ":84-90 nutrition and missing meat smelting stay deferred",
                        SOURCE + ":92-101 hardened leather is not in food core",
                        SOURCE + ":105-126 toast, chili chocolate, bait, zombie jerky and peppermint stay deferred",
                        SOURCE + ":131-216 marzipan, yogurt, milkshakes and eggnog stay deferred",
                        SOURCE + ":218-338 ore listeners other than plain donut stay deferred",
                        SOURCE + ":340-342 wax laminator stays deferred",
                        "Crops, trees and food extended are not part of this food-core runtime"));
    }

    private static CompatSpecs.MachineRow item(String id, String map, CompatSpecs.Op op,
                                               List<CompatSpecs.Stack> inputs, List<CompatSpecs.Stack> outputs,
                                               long eut, long duration, String source) {
        return new CompatSpecs.MachineRow(id, map, op, inputs, outputs, null, 0, eut, duration, false, 0, true, source);
    }

    private static CompatSpecs.MachineRow pair(String id, CompatSpecs.Stack left, CompatSpecs.Stack right,
                                               CompatSpecs.Stack output, long duration, String source) {
        return item(id, "Mixer", CompatSpecs.Op.TWO, List.of(left, right), List.of(output), 16, duration, source);
    }

    private static CompatSpecs.MachineRow fluid(String id, String map, CompatSpecs.Stack input, CompatSpecs.Stack output,
                                                String fluid, int amount, long eut, long duration, String source) {
        return new CompatSpecs.MachineRow(id, map, CompatSpecs.Op.BATH,
                List.of(input), List.of(output), fluid, amount, eut, duration, false, 0, true, source);
    }

    private static CompatSpecs.Stack.Item item(String id, int count) { return new CompatSpecs.Stack.Item(id, count); }

    private static CompatSpecs.Stack.Form form(String prefix, String material, int count) {
        return new CompatSpecs.Stack.Form(prefix, material, count);
    }
}
