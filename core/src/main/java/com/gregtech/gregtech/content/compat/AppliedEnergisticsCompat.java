package com.gregtech.gregtech.content.compat;

import java.util.ArrayList;
import java.util.List;

/**
 * Rows remapped from GT6's Applied Energistics compat onto the 1.20.1 and 1.21.1 jars.
 * Crystal seeds, the inscriber-grind bridge and sky-stone shape sets are not emitted.
 */
public final class AppliedEnergisticsCompat {
    private static final String SOURCE = "Compat_Recipes_AppliedEnergistics.java";
    private static final String[] IRON_OR_STEEL = {
            "Iron", "WroughtIron", "CastIron", "IronCompressed", "PigIron", "MeteoricIron", "Meteorite",
            "Steel", "Knightmetal", "MeteoricSteel"};
    private static final String[] COPPER = {"Copper", "AnnealedCopper"};
    private static final String[] OTHER_INGOTS = {
            "Tin", "Lead", "Silver", "Nickel", "Aluminium", "Brass", "Bronze", "Invar"};
    private static final String[] DIAMONDS = {
            "Diamond", "BlueDiamond", "GreenDiamond", "PurpleDiamond", "RedDiamond", "YellowDiamond",
            "PinkDiamond", "DiamondIndustrial"};
    private static final String[][] SMASH = {
            {"quartz_block", "CertusQuartz", "4"},
            {"quartz_pillar", "CertusQuartz", "4"},
            {"chiseled_quartz_block", "CertusQuartz", "4"},
            {"fluix_block", "Fluix", "4"},
            {"quartz_stairs", "CertusQuartz", "6"},
            {"quartz_pillar_stairs", "CertusQuartz", "6"},
            {"chiseled_quartz_stairs", "CertusQuartz", "6"},
            {"fluix_stairs", "Fluix", "6"},
            {"quartz_slab", "CertusQuartz", "2"},
            {"quartz_pillar_slab", "CertusQuartz", "2"},
            {"chiseled_quartz_slab", "CertusQuartz", "2"},
            {"fluix_slab", "Fluix", "2"}};

    private AppliedEnergisticsCompat() {}

    public static CompatSpecs.Module module() {
        var machines = new ArrayList<CompatSpecs.MachineRow>();
        presses(machines);
        saws(machines);
        compressors(machines);
        smashes(machines);
        var shaped = List.of(
                new CompatSpecs.ShapedRow("compat/ae2/quartz_glass", item("ae2:quartz_glass", 4),
                        List.of("QGQ", "GQG", "QGQ"),
                        List.of(key("Q", new CompatSpecs.Stack.Tag("forge:dusts/quartz", "c:dusts/quartz", 1)),
                                key("G", new CompatSpecs.Stack.Tag("forge:glass/colorless", "c:glass_blocks/colorless", 1))),
                        SOURCE + ":48"),
                new CompatSpecs.ShapedRow("compat/ae2/quartz_vibrant_glass", item("ae2:quartz_vibrant_glass", 1),
                        List.of("GQG"),
                        List.of(key("G", new CompatSpecs.Stack.Tag("forge:dusts/glowstone", "c:dusts/glowstone", 1)),
                                key("Q", item("ae2:quartz_glass", 1))),
                        SOURCE + ":49"));
        var removals = List.of(
                new CompatSpecs.Removal("ae2:decorative/quartz_glass", SOURCE + ":48"),
                new CompatSpecs.Removal("ae2:decorative/quartz_vibrant_glass", SOURCE + ":49"));
        return new CompatSpecs.Module("ae2", true, true, "Compat_Recipes_AppliedEnergistics",
                List.copyOf(machines), List.of(), shaped, removals, List.of(
                        SOURCE + ":41-43 external AE grinder bridge is not restored",
                        SOURCE + ":45-46 DidYouKnow cutter rows are not restored",
                        SOURCE + ":82-98 crystal seeds are not registered; autoclave and mixer rows are skipped",
                        SOURCE + ":101-127 sky stone shapes and block-dust mortar stay deferred",
                        SOURCE + ":57 Silicon has no gem plate in this port",
                        SOURCE + ":60-63 press copies collapse because the press is both input and output",
                        SOURCE + ":90 nether quartz already compresses to the vanilla quartz block",
                        SOURCE + ":130-206 ore-dict grinder and unconsumed laser-lens rows are not restored"));
    }

    private static void presses(List<CompatSpecs.MachineRow> rows) {
        press2(rows, "press/calculation/crystal", "ae2:calculation_processor_press",
                item("ae2:certus_quartz_crystal", 1), "ae2:printed_calculation_processor", SOURCE + ":51");
        press2(rows, "press/calculation/plate", "ae2:calculation_processor_press",
                form("plateGem", "CertusQuartz", 1), "ae2:printed_calculation_processor", SOURCE + ":52");
        for (String diamond : DIAMONDS) {
            press2(rows, "press/engineering/" + diamond, "ae2:engineering_processor_press",
                    form("plateGem", diamond, 1), "ae2:printed_engineering_processor", SOURCE + ":53-54");
        }
        press2(rows, "press/logic/gold", "ae2:logic_processor_press",
                form("plate", "Gold", 1), "ae2:printed_logic_processor", SOURCE + ":55");
        press2(rows, "press/silicon/plate", "ae2:silicon_press",
                form("plate", "Silicon", 1), "ae2:printed_silicon", SOURCE + ":56");
        press3(rows, "press/processor/calculation", "ae2:printed_calculation_processor", "ae2:calculation_processor", SOURCE + ":78");
        press3(rows, "press/processor/engineering", "ae2:printed_engineering_processor", "ae2:engineering_processor", SOURCE + ":79");
        press3(rows, "press/processor/logic", "ae2:printed_logic_processor", "ae2:logic_processor", SOURCE + ":80");
    }

    private static void saws(List<CompatSpecs.MachineRow> rows) {
        for (String material : IRON_OR_STEEL) saw(rows, material, SOURCE + ":65");
        for (String material : COPPER) saw(rows, material, SOURCE + ":68");
        for (String material : OTHER_INGOTS) saw(rows, material, SOURCE + ":69-76");
    }

    private static void compressors(List<CompatSpecs.MachineRow> rows) {
        one(rows, "compress/certus_gem", "Compressor", form("gem", "CertusQuartz", 4),
                item("ae2:quartz_block", 1), 16, 16, SOURCE + ":86");
        one(rows, "compress/certus_crystal", "Compressor", item("ae2:certus_quartz_crystal", 8),
                item("ae2:quartz_block", 1), 16, 16, SOURCE + ":87");
        one(rows, "compress/fluix_gem", "Compressor", form("gem", "Fluix", 4),
                item("ae2:fluix_block", 1), 16, 16, SOURCE + ":88");
        one(rows, "compress/fluix_crystal", "Compressor", item("ae2:fluix_crystal", 8),
                item("ae2:fluix_block", 1), 16, 16, SOURCE + ":89");
    }

    private static void smashes(List<CompatSpecs.MachineRow> rows) {
        for (String[] smash : SMASH) {
            var input = item("ae2:" + smash[0], 1);
            var output = form("gem", smash[1], Integer.parseInt(smash[2]));
            one(rows, "hammer/" + smash[0], "Hammer", input, output, 16, 16, SOURCE + ":101-112");
            one(rows, "crusher/" + smash[0], "Crusher", input, output, 16, 32, SOURCE + ":101-112");
        }
    }

    private static void saw(List<CompatSpecs.MachineRow> rows, String material, String source) {
        rows.add(new CompatSpecs.MachineRow("saw/anchor/" + material, "Cutter", CompatSpecs.Op.SAW,
                List.of(form("ingot", material, 1)), List.of(item("ae2:cable_anchor", 3)),
                null, 0, 16, 16, false, 10, false, source));
    }

    private static void press2(List<CompatSpecs.MachineRow> rows, String id, String press, CompatSpecs.Stack input,
                               String output, String source) {
        rows.add(new CompatSpecs.MachineRow(id, "Press", CompatSpecs.Op.TWO,
                List.of(item(press, 1), input), List.of(item(output, 1)),
                null, 0, 16, 64, false, 0, true, source));
    }

    private static void press3(List<CompatSpecs.MachineRow> rows, String id, String printed, String output, String source) {
        rows.add(new CompatSpecs.MachineRow(id, "Press", CompatSpecs.Op.THREE,
                List.of(item(printed, 1), form("dust", "Redstone", 1), item("ae2:printed_silicon", 1)),
                List.of(item(output, 1)),
                null, 0, 16, 64, false, 0, true, source));
    }

    private static void one(List<CompatSpecs.MachineRow> rows, String id, String map, CompatSpecs.Stack input,
                            CompatSpecs.Stack output, long eut, long duration, String source) {
        rows.add(new CompatSpecs.MachineRow(id, map, CompatSpecs.Op.ONE,
                List.of(input), List.of(output), null, 0, eut, duration, false, 0, true, source));
    }

    private static CompatSpecs.ShapedKey key(String symbol, CompatSpecs.Stack stack) {
        return new CompatSpecs.ShapedKey(symbol, stack);
    }

    private static CompatSpecs.Stack.Item item(String id, int count) { return new CompatSpecs.Stack.Item(id, count); }

    private static CompatSpecs.Stack.Form form(String prefix, String material, int count) {
        return new CompatSpecs.Stack.Form(prefix, material, count);
    }
}
