package com.gregtech.gregtech.content.compat;

import java.util.List;

/**
 * Rows remapped from GT6's Project Red compat onto the 1.20.1 and 1.21.1 core jars.
 * Marble and basalt stone sets stay deferred.
 */
public final class ProjectRedCompat {
    private static final String SOURCE = "Compat_Recipes_ProjectRed.java";

    private ProjectRedCompat() {}

    public static CompatSpecs.Module module() {
        var saw = new CompatSpecs.MachineRow("saw/silicon", "Cutter", CompatSpecs.Op.SAW,
                List.of(item("projectred_core:boule", 1)), List.of(item("projectred_core:silicon", 16)),
                null, 0, 64, 64, false, 1000, false, SOURCE + ":42");
        var shaped = List.of(
                new CompatSpecs.ShapedRow("compat/projectred/red_iron_comp", item("projectred_core:red_iron_comp", 1),
                        List.of(" D ", "DID", " D "),
                        List.of(key("D", tag("forge:dusts/redstone", "c:dusts/redstone")),
                                key("I", tag("forge:ingots/copper", "c:ingots/copper"))),
                        SOURCE + ":38"),
                new CompatSpecs.ShapedRow("compat/projectred/silicon", item("projectred_core:silicon", 4),
                        List.of(" X", "s "),
                        List.of(key("X", form("plateGem", "Silicon", 1)),
                                key("s", item("gregtech:tool_saw", 1))),
                        SOURCE + ":39"),
                new CompatSpecs.ShapedRow("compat/projectred/infused_silicon", item("projectred_core:infused_silicon", 4),
                        List.of(" X", "s "),
                        List.of(key("X", form("plateGem", "RedstoneAlloy", 1)),
                                key("s", item("gregtech:tool_saw", 1))),
                        SOURCE + ":40"));
        return new CompatSpecs.Module("projectred_core", true, true, "Compat_Recipes_ProjectRed",
                List.of(saw), List.of(), shaped,
                List.of(new CompatSpecs.Removal("projectred_core:red_iron_comp", SOURCE + ":36")),
                List.of(SOURCE + ":44-64 marble and basalt stone sets stay deferred"));
    }

    private static CompatSpecs.ShapedKey key(String symbol, CompatSpecs.Stack stack) {
        return new CompatSpecs.ShapedKey(symbol, stack);
    }

    private static CompatSpecs.Stack.Item item(String id, int count) { return new CompatSpecs.Stack.Item(id, count); }

    private static CompatSpecs.Stack.Tag tag(String forge, String neo) {
        return new CompatSpecs.Stack.Tag(forge, neo, 1);
    }

    private static CompatSpecs.Stack.Form form(String prefix, String material, int count) {
        return new CompatSpecs.Stack.Form(prefix, material, count);
    }
}
