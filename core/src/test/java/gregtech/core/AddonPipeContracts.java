package gregtech.core;

import gregtech.internal.addon.AddonMachines;
import gregtech.internal.addon.AddonPipes;
import gregtech.api.addon.FluidPipeDeclaration;
import gregtech.api.addon.ItemPipeDeclaration;
import gregtech.internal.machine.ItemPipeSpec;
import gregtech.internal.machine.PipeSpec;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.mod.ModData;
import gregtech.internal.prefix.PrefixRegistry;
import gregtech.content.transport.ItemPipeCatalog;
import gregtech.content.transport.fluid.FluidTransportDefinitions;
import gregtech.content.data.ModReferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Addon pipe rows. Each scenario is its own JVM because the queue freezes. */
public final class AddonPipeContracts {
    private static int assertions;

    private AddonPipeContracts() {}

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("Expected one scenario");
        Locale.setDefault(Locale.ROOT);
        switch (args[0]) {
            case "plain" -> plain();
            case "expand" -> expand();
            case "fluidhit" -> fluidHit();
            case "itemhit" -> itemHit();
            case "dup" -> duplicate();
            case "twice" -> twice();
            case "order" -> order();
            case "shape" -> shape();
            case "missing" -> missing();
            case "derived" -> derived();
            case "share" -> share();
            default -> throw new IllegalArgumentException("Unknown scenario: " + args[0]);
        }
        System.out.println(args[0] + " passed " + assertions);
    }

    private static void plain() {
        ready();
        var first = ids(FluidTransportDefinitions.pipes());
        var second = ids(FluidTransportDefinitions.pipes());
        check(first.size() == 280, "built-in fluid pipes stay 280");
        check(first.equals(second), "a second read is the same table");
        check("pipe_tiny_wood".equals(first.get(0)), "wood tiny is first");
        check("pipe_nonuple_wood".equals(first.get(6)), "wood nonuple closes the first material");
        check("pipe_tiny_treated_wood".equals(first.get(7)), "treated wood follows wood");
        check("pipe_nonuple_infinity".equals(first.get(279)), "infinity nonuple is last");
        check(ItemPipeCatalog.ITEM_PIPE_MATS.length == 21, "built-in item rows stay 21");
        var mats = ItemPipeCatalog.mats();
        check(mats.size() == 21 && "brass".equals(mats.get(0).idSuffix())
                && "vibranium_silver".equals(mats.get(20).idSuffix()), "item rows stay brass through vibranium silver");
        check(AddonPipes.owner("pipe_tiny_wood") == null, "a built-in path has no addon owner");
        check("gt6ce".equals(AddonPipes.namespace("pipe_tiny_wood")), "built-in pipes stay gt6ce");
        check("gt6ce".equals(AddonPipes.namespace("item_pipe_medium_brass")), "built-in item pipes stay gt6ce");
    }

    private static void expand() {
        ready();
        var before = ids(FluidTransportDefinitions.pipes());
        accept("aa_pipe", List.of(fluid("sample_fluid", "Steel", 125, true, true, false, false, 600, 0)),
                List.of(item("sample_item", "Steel", 32768, 1)));
        var pipes = FluidTransportDefinitions.pipes();
        var after = ids(pipes);
        check(before.size() == 280 && after.size() == 287, "one fluid row adds seven sizes");
        check(after.subList(0, 280).equals(before), "built-in fluid ids stay in front");
        var tiny = pipes.get(280);
        check("pipe_tiny_sample_fluid".equals(tiny.id()) && tiny.capacity() == 125 && tiny.tankCount() == 1,
                "tiny capacity is the declared base");
        check(tiny.gasProof() && tiny.acidProof() && !tiny.plasmaProof() && !tiny.magicProof(), "declared proofs");
        check(tiny.maxTemperature() == 600 && tiny.flammability() == 0, "declared temperature and flammability");
        var nonuple = pipes.get(286);
        check("pipe_nonuple_sample_fluid".equals(nonuple.id()) && nonuple.capacity() == 250 && nonuple.tankCount() == 9,
                "nonuple keeps the size multiplier and nine tanks");
        check("aa_pipe".equals(AddonPipes.namespace(tiny.id())), "addon fluid pipes use the addon namespace");
        check(ItemPipeCatalog.ITEM_PIPE_MATS.length == 21, "the built-in item array is unchanged");
        var mats = ItemPipeCatalog.mats();
        check(mats.size() == 22 && "sample_item".equals(mats.get(21).idSuffix()), "the item row is appended");
        var mat = mats.get(21);
        var huge = ItemPipeSpec.of("item_pipe_huge_sample_item", mat.material(), ItemPipeSpec.ItemPipeSize.HUGE,
                mat.stepSize(), mat.invSize(), true);
        check(huge.stepSize() == 32768 / 4 && huge.invSize() == 4, "huge divides the step and multiplies the slots");
        check(ItemPipeSpec.ItemPipeSize.values().length == 6, "item pipes expand to six sizes");
        check("aa_pipe".equals(AddonPipes.namespace("item_pipe_restrictive_huge_sample_item")),
                "addon item pipes use the addon namespace");
        check(ids(FluidTransportDefinitions.pipes()).equals(after), "a second expansion read is stable");
    }

    private static void fluidHit() {
        ready();
        accept("aa_pipe", List.of(fluid("wood", "Steel", 100, false, false, false, false, 0, 0)), List.of());
        expectThrows(IllegalStateException.class, FluidTransportDefinitions::pipes, "aa_pipe", "wood", "built-in");
    }

    private static void itemHit() {
        ready();
        accept("aa_pipe", List.of(), List.of(item("brass", "Steel", 64, 1)));
        expectThrows(IllegalStateException.class, ItemPipeCatalog::mats, "aa_pipe", "brass", "built-in");
    }

    private static void duplicate() {
        ready();
        accept("aa_one", List.of(fluid("sample_a", "Steel", 100, true, false, false, false, 0, 0)), List.of());
        accept("aa_two", List.of(fluid("sample_a", "Steel", 100, true, false, false, false, 0, 0)), List.of());
        expectThrows(IllegalStateException.class, FluidTransportDefinitions::pipes, "aa_two", "sample_a", "aa_one");
    }

    private static void twice() {
        ready();
        accept("aa_pipe", List.of(
                fluid("sample_a", "Steel", 100, true, false, false, false, 0, 0),
                fluid("sample_a", "Steel", 100, true, false, false, false, 0, 0)), List.of());
        expectThrows(IllegalStateException.class, FluidTransportDefinitions::pipes, "aa_pipe", "sample_a", "twice");
    }

    private static void order() {
        ready();
        accept("bb_pipe", List.of(fluid("sample_c", "Steel", 100, true, false, false, false, 0, 0)), List.of());
        accept("aa_pipe", List.of(
                fluid("sample_a", "Steel", 100, true, false, false, false, 0, 0),
                fluid("sample_b", "Steel", 100, true, false, false, false, 0, 0)), List.of());
        var ids = ids(FluidTransportDefinitions.pipes());
        check("pipe_tiny_sample_a".equals(ids.get(280)), "lower addon id comes first");
        check("pipe_tiny_sample_b".equals(ids.get(287)), "declaration order stays inside one addon");
        check("pipe_tiny_sample_c".equals(ids.get(294)), "a later addon id follows");
    }

    private static void shape() {
        expectThrows(IllegalArgumentException.class,
                () -> new FluidPipeDeclaration("Bad", "Steel", 1, false, false, false, false, 0, 0), "lower-case");
        expectThrows(IllegalArgumentException.class,
                () -> new FluidPipeDeclaration("sample", "Steel", 0, false, false, false, false, 0, 0), "positive");
        expectThrows(IllegalArgumentException.class,
                () -> new FluidPipeDeclaration("sample", "Steel", 1, false, false, false, false, 0, -1), "flammability");
        expectThrows(IllegalArgumentException.class,
                () -> new ItemPipeDeclaration("sample", "Steel", 3, 1), "step size");
    }

    private static void missing() {
        ready();
        accept("aa_pipe", List.of(fluid("sample_x", "NotAMaterial", 100, true, false, false, false, 0, 0)), List.of());
        expectThrows(IllegalStateException.class, FluidTransportDefinitions::pipes, "unknown material", "NotAMaterial");
    }

    private static void derived() {
        ready();
        accept("aa_pipe", List.of(fluid("sample_steel", "Steel", 100, true, false, false, false, 0, 0)), List.of());
        long expected = (long) (GTMaterialRegistry.get("Steel").getMeltingPoint() * 1.25D);
        var tiny = FluidTransportDefinitions.pipes().get(280);
        check("pipe_tiny_sample_steel".equals(tiny.id()) && tiny.maxTemperature() == expected,
                "zero temperature uses melting point times 1.25");
    }

    private static void share() {
        ready();
        accept("aa_pipe", List.of(
                fluid("brass", "Steel", 100, true, false, false, false, 0, 0),
                fluid("shared", "Steel", 100, true, false, false, false, 0, 0)), List.of(
                item("wood", "Steel", 64, 1),
                item("shared", "Steel", 64, 1)));
        var pipes = FluidTransportDefinitions.pipes();
        check(pipes.size() == 294, "a fluid suffix from the item catalog is still added");
        check("aa_pipe".equals(AddonPipes.namespace("pipe_tiny_brass")), "fluid brass is the addon pipe");
        check("aa_pipe".equals(AddonPipes.namespace("item_pipe_medium_wood")), "item wood is the addon pipe");
        check("aa_pipe".equals(AddonPipes.namespace("pipe_tiny_shared"))
                && "aa_pipe".equals(AddonPipes.namespace("item_pipe_medium_shared")),
                "one suffix may name both catalogs");
        check("gt6ce".equals(AddonPipes.namespace("item_pipe_medium_brass")), "the built-in brass item pipe stays");
        check("gt6ce".equals(AddonPipes.namespace("pipe_tiny_wood")), "the built-in wood fluid pipe stays");
    }

    private static FluidPipeDeclaration fluid(String suffix, String material, long capacity,
                                              boolean gas, boolean acid, boolean plasma, boolean magic,
                                              long temperature, int flammability) {
        return new FluidPipeDeclaration(suffix, material, capacity, gas, acid, plasma, magic, temperature, flammability);
    }

    private static ItemPipeDeclaration item(String suffix, String material, long step, int slots) {
        return new ItemPipeDeclaration(suffix, material, step, slots);
    }

    private static void accept(String addonId, List<FluidPipeDeclaration> fluid, List<ItemPipeDeclaration> item) {
        AddonMachines.accept(addonId, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), fluid, item);
    }

    private static void ready() {
        ModData.bindPresence(id -> true);
        GTMaterialRegistry.setLogSink((warning, message) -> {});
        PrefixRegistry.ensurePrefixesLoaded();
        ModReferences.UNKNOWN.getClass();
        gregtech.content.data.MaterialGroups.Glowstone.getClass();
        GTMaterialRegistry.init();
    }

    private static List<String> ids(List<PipeSpec> pipes) {
        var ids = new ArrayList<String>();
        for (var pipe : pipes) ids.add(pipe.id());
        return ids;
    }

    private static void expectThrows(Class<? extends Throwable> expected, Runnable action, String... parts) {
        assertions++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (!expected.isInstance(failure)) throw new AssertionError(String.join(" ", parts), failure);
            String text = (failure.getMessage() == null ? "" : failure.getMessage()) + " "
                    + (failure.getCause() == null || failure.getCause().getMessage() == null ? "" : failure.getCause().getMessage());
            for (String part : parts) {
                if (!text.contains(part)) throw new AssertionError(String.join(" ", parts) + " but was: " + text, failure);
            }
            return;
        }
        throw new AssertionError(String.join(" ", parts));
    }

    private static void check(boolean valid, String message) {
        assertions++;
        if (!valid) throw new AssertionError(message);
    }
}
