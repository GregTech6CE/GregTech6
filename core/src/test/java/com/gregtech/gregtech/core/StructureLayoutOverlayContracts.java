package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.addon.AddonMachines;
import com.gregtech.gregtech.api.addon.CellMatch;
import com.gregtech.gregtech.api.addon.LayoutRepeat;
import com.gregtech.gregtech.api.addon.StructureLayout;
import com.gregtech.gregtech.api.addon.StructureRole;
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
        var repeated = StructureLayoutDocuments.parse("scriptpress", """
                {"cells":[{"right":1,"up":0,"back":0,"match":"tank_wall","role":"casing"}],
                 "repeats":[{"key":"absorber","up":1,"min":1,"max":3,"cells":[
                 {"right":2,"up":0,"back":0,"match":"anyopaque","role":"casing"}]}]}""");
        var sharedRepeat = AddonMachines.sharedLayout("test", repeated);
        SharedLargeMachineLayouts.add("repeatpress", sharedRepeat);
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

        var predicates = StructureLayoutDocuments.parse("scriptpress", """
                {"cells":[
                {"right":1,"up":0,"back":0,"match":"anyfluid","role":"casing"},
                {"right":2,"up":0,"back":0,"match":"anyfluid#minecraft:water","role":"casing"},
                {"right":3,"up":0,"back":0,"match":"anyopaque","role":"casing"}]}""");
        check(predicates.cells().get(0).match() instanceof CellMatch.AnyFluid, "any fluid");
        check(predicates.cells().get(1).match().toString().equals("anyfluid#minecraft:water"), "fluid tag");
        check(predicates.cells().get(2).match() instanceof CellMatch.AnyOpaque, "any opaque");
        check(StructureLayoutDocuments.parse("scriptpress", StructureLayoutDocuments.canonical(predicates))
                .equals(predicates), "predicate round trip");
        expectThrows(IllegalArgumentException.class, () -> StructureLayoutDocuments.parse("scriptpress",
                "{\"cells\":[{\"right\":1,\"up\":0,\"back\":0,\"match\":\"anyopaque\",\"role\":\"energy_input\"}]}"), "casing");
        expectThrows(IllegalArgumentException.class, () -> CellMatch.parse("anyfluid#minecraft"), "Invalid block name");

        check(repeated.repeats().size() == 1 && repeated.repeats().get(0).height() == 1, "repeat parse");
        check(StructureLayoutDocuments.parse("scriptpress", StructureLayoutDocuments.canonical(repeated))
                .equals(repeated), "repeat round trip");
        var downward = StructureLayoutDocuments.parse("scriptpress", """
                {"repeats":[{"key":"drill","up":-1,"min":1,"max":-1,"step":-1,"cells":[
                 {"right":1,"up":0,"back":0,"match":"anyopaque","role":"casing"}]}]}""");
        check(downward.repeats().get(0).step() == -1 && downward.repeats().get(0).unbounded(), "downward unbounded");
        expectThrows(IllegalArgumentException.class, () -> StructureLayoutDocuments.parse("scriptpress",
                "{\"repeats\":[{\"key\":\"absorber\",\"up\":1,\"min\":2,\"max\":1,\"cells\":[{\"right\":1,\"up\":0,\"back\":0,\"match\":\"air\",\"role\":\"air\"}]}]}"), "max >= min");
        expectThrows(IllegalArgumentException.class, () -> StructureLayoutDocuments.parse("scriptpress",
                "{\"repeats\":[{\"key\":\"absorber\",\"up\":1,\"min\":1,\"max\":2,\"extra\":1,\"cells\":[{\"right\":1,\"up\":0,\"back\":0,\"match\":\"air\",\"role\":\"air\"}]}]}"), "Unknown field");
        var built = StructureLayout.builder("repeatpress2")
                .repeat(LayoutRepeat.builder("absorber", 1, 1, 4).step(-1)
                        .cell(1, 0, 0, CellMatch.anyOpaque(), StructureRole.CASING).build())
                .build();
        check(built.repeats().get(0).step() == -1, "builder repeat");
        check(sharedRepeat.repeats().size() == 1 && sharedRepeat.cells().size() == 1, "shared repeat");
        check(SharedLargeMachineLayouts.cells("repeatpress").size() == 2, "repeat min expansion");
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
