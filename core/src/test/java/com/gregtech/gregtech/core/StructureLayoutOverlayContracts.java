package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.addon.AddonMachines;
import com.gregtech.gregtech.api.kubejs.StructureLayoutDocuments;
import com.gregtech.gregtech.api.multiblock.StructureGrid;
import com.gregtech.gregtech.content.multiblock.SharedLargeMachineLayouts;

import java.util.List;
import java.util.Map;

/** Reloadable structure layouts, without Minecraft. This JVM freezes the layout table. */
public final class StructureLayoutOverlayContracts {
    private static int assertions;

    private StructureLayoutOverlayContracts() {}

    public static void main(String[] args) {
        var json = """
                {"cells":[{"right":1,"up":0,"back":0,"match":"press_casing","role":"casing"},
                {"right":0,"up":0,"back":1,"match":"tank_wall","role":"energy_input"}]}""";
        var parsed = StructureLayoutDocuments.parse("scriptpress", json);
        check(parsed.name().equals("scriptpress") && parsed.cells().size() == 2, "layout document");
        check(StructureLayoutDocuments.parse("scriptpress", StructureLayoutDocuments.canonical(parsed)).equals(parsed),
                "layout canonical round trip");
        var ignored = StructureLayoutDocuments.parse("scriptpress",
                "{\"_comment\":\"note\",\"cells\":[{\"right\":1,\"up\":0,\"back\":0,\"match\":\"tank_wall\",\"role\":\"casing\"}]}");
        check(ignored.cells().size() == 1, "root annotations ignored");
        expectThrows(IllegalArgumentException.class, () -> StructureLayoutDocuments.parse("scriptpress",
                "{\"cells\":[{\"right\":0,\"up\":0,\"back\":0,\"match\":\"tank_wall\",\"role\":\"casing\"}]}"), "origin");
        expectThrows(IllegalArgumentException.class, () -> StructureLayoutDocuments.parse("scriptpress",
                "{\"cells\":[{\"right\":1,\"up\":0,\"back\":0,\"match\":\"tank_wall\",\"role\":\"casing\"},{\"right\":1,\"up\":0,\"back\":0,\"match\":\"air\",\"role\":\"air\"}]}"),
                "Duplicate");
        expectThrows(IllegalArgumentException.class, () -> StructureLayoutDocuments.parse("scriptpress",
                "{\"cells\":[{\"right\":1,\"up\":0,\"back\":0,\"match\":\"tank_wall\",\"role\":\"nope\"}]}"), "role");
        expectThrows(IllegalArgumentException.class, () -> StructureLayoutDocuments.parse("scriptpress",
                "{\"cells\":[{\"right\":1,\"up\":0,\"back\":0,\"match\":\"minecraft:iron_block\",\"role\":\"energy_input\"}]}"), "casing");
        expectThrows(IllegalArgumentException.class, () -> StructureLayoutDocuments.parse("scriptpress",
                "{\"cells\":[{\"right\":1,\"up\":0,\"back\":0,\"match\":\"tank_wall\",\"role\":\"casing\",\"extra\":1}]}"), "Unknown field");

        var baseline = List.of(new SharedLargeMachineLayouts.Cell(1, 0, 0, 18002, StructureGrid.Role.CASING));
        SharedLargeMachineLayouts.add("scriptpress", baseline);
        expectThrows(IllegalStateException.class, () -> SharedLargeMachineLayouts.overlay(Map.of()), "not ready");
        SharedLargeMachineLayouts.freeze();
        check(SharedLargeMachineLayouts.replaceable("scriptpress"), "addon layout is replaceable");
        check(SharedLargeMachineLayouts.builtIn("largecrusher") && !SharedLargeMachineLayouts.replaceable("largecrusher"),
                "built-in layout is not replaceable");
        expectThrows(IllegalStateException.class, () -> AddonMachines.sharedCells("scriptpress", parsed), "unknown part");
        var known = StructureLayoutDocuments.parse("scriptpress",
                "{\"cells\":[{\"right\":1,\"up\":0,\"back\":0,\"match\":\"tank_wall\",\"role\":\"energy_input\"},{\"right\":0,\"up\":0,\"back\":1,\"match\":\"minecraft:iron_block\",\"role\":\"casing\"}]}");
        var replacement = AddonMachines.sharedCells("scriptpress", known);
        check(replacement.get(0).part() == 18002 && "minecraft:iron_block".equals(replacement.get(1).plain()), "shared cells");
        SharedLargeMachineLayouts.overlay(Map.of("scriptpress", replacement));
        check(SharedLargeMachineLayouts.cells("scriptpress").size() == 2, "overlay replaces cells");
        check(SharedLargeMachineLayouts.baseline("scriptpress").equals(baseline), "baseline stays");
        expectThrows(IllegalArgumentException.class,
                () -> SharedLargeMachineLayouts.overlay(Map.of("largecrusher", replacement)), "cannot be replaced");
        SharedLargeMachineLayouts.overlay(Map.of());
        check(SharedLargeMachineLayouts.cells("scriptpress").equals(baseline), "empty overlay restores the baseline");
        check(SharedLargeMachineLayouts.overlaid().isEmpty(), "overlay is cleared");
        System.out.println("structure layout overlay passed " + assertions);
    }

    private static void expectThrows(Class<? extends Throwable> expected, Runnable action, String part) {
        assertions++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (!expected.isInstance(failure)) throw new AssertionError(part, failure);
            String text = failure.getMessage() == null ? "" : failure.getMessage();
            if (!text.contains(part)) throw new AssertionError(part + " but was: " + text, failure);
            return;
        }
        throw new AssertionError(part);
    }

    private static void check(boolean valid, String message) {
        assertions++;
        if (!valid) throw new AssertionError(message);
    }
}
