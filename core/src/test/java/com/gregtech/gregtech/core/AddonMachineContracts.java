package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.addon.BehaviorEnergyRules;
import com.gregtech.gregtech.api.addon.AddonOverclock;
import com.gregtech.gregtech.api.addon.AddonMachineDefinition;
import com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration;
import com.gregtech.gregtech.api.addon.CompressedGasTankDeclaration;
import com.gregtech.gregtech.api.addon.MachineDeclaration;
import com.gregtech.gregtech.api.addon.BehaviorRangeRules;
import com.gregtech.gregtech.api.addon.ExclusiveOutputRules;
import com.gregtech.gregtech.api.addon.RecipeOutputChances;
import com.gregtech.gregtech.api.energy.MachineFaceMasks;
import com.gregtech.gregtech.api.addon.AddonMachineLayout;
import com.gregtech.gregtech.api.addon.AddonMachines;
import com.gregtech.gregtech.api.addon.StructurePartDeclaration;
import com.gregtech.gregtech.api.addon.BehaviorMachineFactories;
import com.gregtech.gregtech.api.addon.FuelGeneratorDeclaration;
import com.gregtech.gregtech.api.addon.GregTechMachineContributor;
import com.gregtech.gregtech.api.machine.BasicMachineEnergy;
import com.gregtech.gregtech.api.machine.BasicMachineParameters;
import com.gregtech.gregtech.api.machine.MachineControllerKind;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.api.multiblock.StructureGrid;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.api.recipe.RecipeMapSpec;
import com.gregtech.gregtech.content.multiblock.LargeMachineProcessingRules;
import com.gregtech.gregtech.content.multiblock.SharedLargeMachineLayouts;
import com.gregtech.gregtech.content.multiblock.SharedLargeMachineParts;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.ModReferences;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Addon machine queue. Each scenario is its own JVM because the queue freezes. */
public final class AddonMachineContracts {
    private static int assertions;

    private AddonMachineContracts() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected one scenario");
        Locale.setDefault(Locale.ROOT);
        switch (args[0]) {
            case "kind" -> kind();
            case "duplicate" -> duplicate();
            case "part" -> unknownPart();
            case "partok" -> partOk();
            case "partname" -> partName();
            case "partmat" -> partMaterial();
            case "partdup" -> partDuplicate();
            case "map" -> unknownMap();
            case "energy" -> unknownEnergy();
            case "controller" -> noController();
            case "service" -> service();
            case "overlap" -> overlap();
            case "bounds" -> bounds();
            case "benergy" -> behaviorEnergy();
            case "factory" -> missingFactory();
            case "share" -> sharedBlockEntity();
            case "bconflict" -> behaviorConflict();
            case "bslots" -> behaviorSlots();
            case "blayout" -> behaviorLayout();
            case "fuelok" -> fuelOk();
            case "fuelmap" -> unknownFuelMap();
            case "fuelenergy" -> unknownFuelEnergy();
            case "fuelstruct" -> fuelWithoutStructure();
            case "fuelname" -> fuelNameCollision();
            case "fuelmachine" -> recipeMachineCannotUseFuelMap();
            case "fuelbounds" -> fuelBounds();
            case "knobs" -> knobs();
            case "face" -> faceDisagree();
            case "bstore" -> behaviorStore();
            case "brange" -> behaviorRange();
            case "epart" -> energyPlain();
            case "emax" -> energyMax();
            case "eunknown" -> energyUnknown();
            case "gasok" -> gasOk();
            case "gasdup" -> gasDuplicate();
            case "gasmat" -> gasMaterial();
            case "iotarget" -> ioTarget();
            default -> throw new IllegalArgumentException("Unknown scenario: " + args[0]);
        }
        System.out.println(args[0] + " passed " + assertions);
    }

    private static void knobs() {
        var plain = MachineDeclaration.builder("press", "Steel").energy("EU", 32).recipeMap("aa.recipe.press").basic().build();
        check(!plain.facesSpecified(), "default faces are unspecified");
        check(!plain.cheapOverclocking() && !plain.cheapOverclockingSpecified(), "default cheap overclock is off and unset");
        check(plain.minimumLightAbove() == 0, "default light gate is off");
        check(!plain.searchOutputTanks() && !plain.exclusiveOutputs(), "default output-tank search and exclusive outputs are off");
        check(plain.itemOutputTarget() == null && plain.fluidOutputTarget() == null
                && plain.itemInputSource() == null && plain.fluidInputSource() == null, "default IO targets are absent");
        check(!AddonOverclock.enabled(true, true, plain.cheapOverclocking(), true), "addon basic ignores a cheap built-in name");
        check(AddonOverclock.enabled(false, false, false, true), "built-in legacy cheap stays");
        check(!AddonOverclock.enabled(false, true, false, true), "an explicit off replaces a large machine's legacy cheap rule");

        var declared = MachineDeclaration.builder("press", "Steel").energy("EU", 32).recipeMap("aa.recipe.press")
                .itemInputs(BehaviorMachineDeclaration.Face.TOP)
                .itemOutputs(BehaviorMachineDeclaration.Face.BOTTOM)
                .energyInputs(BehaviorMachineDeclaration.Face.BACK)
                .itemAutoOutput(BehaviorMachineDeclaration.Face.BOTTOM)
                .cheapOverclocking(true)
                .minimumLightAbove(14)
                .build();
        check(declared.facesSpecified() && declared.itemAutoOutput() == BehaviorMachineDeclaration.Face.BOTTOM, "declared faces");
        check(declared.cheapOverclocking() && declared.cheapOverclockingSpecified(), "cheap overclock opt-in");
        check(AddonOverclock.enabled(true, true, declared.cheapOverclocking(), false), "opt-in cheap overclock is on");
        check(declared.minimumLightAbove() == 14, "light gate");
        int top = 0;
        for (var face : declared.itemInputs()) top |= face.bit();
        check(top == (1 << BehaviorMachineDeclaration.Face.TOP.index()), "top bit matches the face index");
        expectThrows(IllegalArgumentException.class, () -> MachineDeclaration.builder("press", "Steel")
                .energy("EU", 32).recipeMap("aa.recipe.press").minimumLightAbove(16).build(), "light");
        int[] certain = RecipeOutputChances.normalize(null, 2);
        check(certain.length == 2 && certain[0] == 10_000 && certain[1] == 10_000, "missing chances are certain");
        check(RecipeOutputChances.normalize(new int[0], 1)[0] == 10_000, "an empty chance list is certain");
        int[] half = RecipeOutputChances.normalize(new int[]{5_000, 10_000}, 2);
        check(half[0] == 5_000 && half[1] == 10_000, "declared chances are kept");
        expectThrows(IllegalArgumentException.class, () -> RecipeOutputChances.normalize(new int[]{5_000}, 2), "does not match");
        expectThrows(IllegalArgumentException.class, () -> RecipeOutputChances.normalize(new int[]{0}, 1), "1..");

        var detector = MachineDeclaration.builder("detector", "Steel").energy("EU", 32).recipeMap("aa.recipe.detector")
                .searchOutputTanks(true).exclusiveOutputs(true).build();
        check(detector.searchOutputTanks() && detector.exclusiveOutputs(), "both switches opt in");
        ExclusiveOutputRules.requirePair(2, 2);
        assertions++;
        check(ExclusiveOutputRules.choose(6_000, bound -> 0) == 0, "a low roll takes the first output");
        check(ExclusiveOutputRules.choose(6_000, bound -> 5_999) == 0, "the last success takes the first output");
        check(ExclusiveOutputRules.choose(6_000, bound -> 6_000) == 1, "the boundary takes the second output");
        check(ExclusiveOutputRules.choose(10_000, bound -> { throw new AssertionError("certain first output"); }) == 0,
                "a certain first output needs no roll");
        expectThrows(IllegalArgumentException.class, () -> ExclusiveOutputRules.requirePair(1, 1), "exactly 2");
        expectThrows(IllegalArgumentException.class, () -> ExclusiveOutputRules.requirePair(2, 1), "exactly 2");
        expectThrows(IllegalArgumentException.class, () -> ExclusiveOutputRules.requirePair(3, 3), "exactly 2");
    }

    private static void faceDisagree() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(map("aa.recipe.press")), List.of(
                faced("press_a", "samplepress", MachineFaceMasks.ALL_SIDES),
                faced("press_b", "samplepress", MachineFaceMasks.TOP_IN_BOTTOM_OUT)), List.of());
        expectInstall("samplepress", "disagrees");
    }

    private static AddonMachineDefinition faced(String id, String machineName, MachineFaceMasks faces) {
        return new AddonMachineDefinition(BasicMachineParameters.builder(id, GTMaterialRegistry.get("Steel"))
                .machineType(machineName).energy(GregTechTags.Energy.EU, 32).tier(1).faces(faces).build(),
                "aa.recipe.press", MachineControllerKind.BASIC, 10_000, true, List.of());
    }

    private static void kind() {
        check(MachineControllerKind.builtIn("massfab") == MachineControllerKind.COMPACT_MATTER, "massfab");
        check(MachineControllerKind.builtIn("largemassfab") == MachineControllerKind.MATTER, "largemassfab keeps its controller ahead of its layout");
        check(MachineControllerKind.builtIn("distillationtower") == MachineControllerKind.DISTILLATION, "distillationtower");
        check(MachineControllerKind.builtIn("largecentrifuge") == MachineControllerKind.LARGE_RECIPE, "largecentrifuge");
        check(MachineControllerKind.builtIn("centrifuge") == MachineControllerKind.BASIC, "centrifuge");
        check(LargeMachineProcessingRules.efficiency("largeoven") == 2500, "large oven efficiency");
        check(LargeMachineProcessingRules.efficiency("largecentrifuge") == 5000, "large centrifuge efficiency");
        check(LargeMachineProcessingRules.efficiency("largecrusher") == 5000, "large crusher efficiency");
        check(LargeMachineProcessingRules.efficiency("centrifuge") == 10000, "single block efficiency");
        check(!LargeMachineProcessingRules.constantEnergy(false, "largecrusher"), "crusher is not constant");
        check(!LargeMachineProcessingRules.constantEnergy(false, "largeshredder"), "shredder is not constant");
        check(!LargeMachineProcessingRules.constantEnergy(false, "largesqueezer"), "squeezer is not constant");
        check(LargeMachineProcessingRules.constantEnergy(false, "largeoven"), "oven is constant");
        check(!LargeMachineProcessingRules.constantEnergy(true, "largeoven"), "timed machines are not constant");
        check(BasicMachineParameters.legacyParallelLimit("largemixer") == 256, "mixer parallel");
        check(BasicMachineParameters.legacyParallelLimit("largecentrifuge") == 16, "centrifuge parallel");
        check(BasicMachineParameters.legacyParallelLimit("centrifuge") == 1, "single block parallel");
    }

    private static void duplicate() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_one", List.of(map("aa.recipe.one")), List.of(machine("press_a", "samplepress", "aa.recipe.one")), List.of());
        AddonMachines.accept("aa_two", List.of(map("aa.recipe.two")), List.of(machine("press_b", "samplepress", "aa.recipe.two")), List.of());
        expectInstall("aa_two", "samplepress", "aa_one");
    }

    private static void unknownPart() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(map("aa.recipe.press")),
                List.of(large("large_press", "largesamplepress", "aa.recipe.press")),
                List.of(new AddonMachineLayout("largesamplepress", List.of(
                        new AddonMachineLayout.Cell(1, 0, 0, "not_a_part", StructureGrid.Role.CASING)))));
        expectInstall("aa_sample", "unknown", "part", "not_a_part");
    }

    private static void partOk() {
        prepare();
        GTMaterialRegistry.init();
        int steel = SharedLargeMachineParts.idForName("steel_wall");
        int walls = SharedLargeMachineParts.DEFINITIONS.size();
        AddonMachines.accept("aa_part", List.of(map("aa.recipe.press")),
                List.of(large("large_press", "largesamplepress", "aa.recipe.press")),
                List.of(new AddonMachineLayout("largesamplepress", List.of(
                        new AddonMachineLayout.Cell(1, 0, 0, "sample_wall", StructureGrid.Role.CASING)))),
                List.of(), List.of(), List.of(),
                List.of(new StructurePartDeclaration("sample_wall", "Steel", "", 6.0F),
                        new StructurePartDeclaration("sample_cap", "Iron", "block/sample_cap", 4.0F)));
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        check(AddonMachines.structureParts().isEmpty() == false && AddonMachines.structureParts().size() == 2, "two parts");
        check(AddonMachines.partId("sample_wall") == 1_000_000, "first runtime id");
        check(AddonMachines.partId("sample_cap") == 1_000_001, "second runtime id");
        check(SharedLargeMachineParts.idForName("sample_wall") == 1_000_000, "lookup sees the addon part");
        check(SharedLargeMachineParts.idForName("steel_wall") == steel && steel == 18009, "steel wall stays 18009");
        check(SharedLargeMachineParts.DEFINITIONS.size() == walls, "built-in table unchanged");
        check(SharedLargeMachineParts.builtinId("sample_wall") < 0, "addon name is not built in");
        var cells = SharedLargeMachineLayouts.cells("largesamplepress");
        check(cells != null && cells.size() == 1 && cells.get(0).part() == 1_000_000, "layout stored the runtime id");
    }

    private static void partName() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_part", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new StructurePartDeclaration("steel_wall", "Steel", "", 6.0F)));
        expectInstall("aa_part", "steel_wall", "built-in");
    }

    private static void partMaterial() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_part", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new StructurePartDeclaration("sample_wall", "NotAMaterial", "", 6.0F)));
        expectInstall("aa_part", "unknown", "material", "NotAMaterial");
    }

    private static void partDuplicate() {
        prepare();
        GTMaterialRegistry.init();
        var part = new StructurePartDeclaration("sample_wall", "Steel", "", 6.0F);
        AddonMachines.accept("aa_one", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(part));
        AddonMachines.accept("aa_two", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(part));
        expectInstall("aa_two", "sample_wall", "aa_one");
    }

    private static void unknownMap() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(),
                List.of(machine("sample_press", "samplepress", "aa.recipe.missing")), List.of());
        expectInstall("aa_sample", "unknown", "recipe", "aa.recipe.missing");
    }

    private static void unknownEnergy() throws Exception {
        prepare();
        GTMaterialRegistry.init();
        expectThrows(IllegalStateException.class, () -> load(EnergyContributor.class), "aa_sample", "Unknown", "energy");
    }

    private static void noController() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(new AddonMachineLayout("orphanpress", List.of(
                new AddonMachineLayout.Cell(1, 0, 0, "tank_wall", StructureGrid.Role.CASING)))));
        expectInstall("aa_sample", "orphanpress", "no", "controller");
    }

    private static void service() throws Exception {
        prepare();
        GTMaterialRegistry.init();
        load(ServiceContributor.class);
        AddonMachines.collectContributors();
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        check(AddonMachines.machines().size() == 1, "one machine accepted");
        check("aa_sample".equals(AddonMachines.machines().get(0).addonId()), "owner is the addon");
        check(AddonMachines.machines().get(0).definition().kind() == MachineControllerKind.LARGE_RECIPE, "large recipe kind");
        var cells = SharedLargeMachineLayouts.cells("largesamplepress");
        check(cells != null && cells.size() == 1 && cells.get(0).part() == 18002, "tank wall name became its numeric id");
        check(AddonMachines.structureParts().isEmpty(), "no addon parts leaves the part table empty");
        check(SharedLargeMachineParts.idForName("steel_wall") == 18009, "steel wall id is unchanged");
        check(AddonMachines.recipeMaps().size() == 1, "one recipe map accepted");
        boolean closed = false;
        try {
            AddonMachines.accept("aa_late", List.of(), List.of(), List.of());
        } catch (IllegalStateException closedQueue) {
            closed = closedQueue.getMessage().contains("closed");
        }
        check(closed, "the queue closes after install");
    }

    private static void load(Class<?> contributor) {
        try {
            Path root = Files.createTempDirectory("gt-addon-machines");
            Path service = root.resolve("META-INF/services/com.gregtech.gregtech.api.addon.GregTechMachineContributor");
            Files.createDirectories(service.getParent());
            Files.writeString(service, contributor.getName() + "\n", StandardCharsets.UTF_8);
            try (URLClassLoader loader = new URLClassLoader(new URL[]{root.toUri().toURL()},
                    AddonMachineContracts.class.getClassLoader())) {
                AddonMachines.collectContributors(loader);
            }
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
    }

    private static void expectInstall(String... parts) {
        expectThrows(IllegalStateException.class,
                () -> AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames()),
                parts);
    }

    private static RecipeMapSpec map(String name) {
        return new RecipeMapSpec(null, name, "Sample", "aa_sample:textures/gui/machines/press",
                1, 1, 1, 0, 0, 0, 1, true, false, false, false);
    }

    private static AddonMachineDefinition machine(String id, String machineName, String recipeMap) {
        return new AddonMachineDefinition(BasicMachineParameters.builder(id, GTMaterialRegistry.get("Steel"))
                .machineType(machineName).energy(GregTechTags.Energy.EU, 32).tier(1).build(),
                recipeMap, MachineControllerKind.BASIC, 10_000, true, List.of());
    }

    private static AddonMachineDefinition large(String id, String machineName, String recipeMap) {
        return new AddonMachineDefinition(BasicMachineParameters.builder(id, GTMaterialRegistry.get("Steel"))
                .machineType(machineName).energy(GregTechTags.Energy.EU, 512).tier(1).build(),
                recipeMap, MachineControllerKind.LARGE_RECIPE, 10_000, true,
                List.of("aa_sample.structure.press.1"));
    }

    private static void overlap() {
        expectThrows(IllegalArgumentException.class, () -> BehaviorMachineDeclaration.builder("bad_press", "Steel")
                .energy("EU", 32).gui("aa_sample:textures/gui/machine.png")
                .fluid(1000, 8, 8, BehaviorMachineDeclaration.SlotRole.INPUT).build(),
                "overlaps");
    }

    private static void behaviorStore() {
        check(BehaviorEnergyRules.finishTick(100, 0, 32) == BasicMachineEnergy.drain(100, 32), "default capacity drains");
        check(BehaviorEnergyRules.finishTick(100, 0, 32) == 68, "drain leaves the remainder");
        long kept = BehaviorEnergyRules.finishTick(100, 1000, 32);
        check(kept == 100, "capacity keeps energy");
        check(BehaviorEnergyRules.finishTick(kept, 1000, 32) == 100, "kept across a second tick");
        check(BehaviorEnergyRules.finishTick(1500, 1000, 32) == 1000, "capacity caps stored energy");
        check(BehaviorEnergyRules.buffer(0, 64) == 64, "drain buffer is the rated maximum");
        check(BehaviorEnergyRules.buffer(1000, 64) == 1000, "storage buffer is the capacity");
        check(BehaviorEnergyRules.readCapacity(false, 0, 500) == 500, "missing gt.capacity uses the declaration");
        check(BehaviorEnergyRules.readCapacity(true, 800, 500) == 800, "gt.capacity is kept");
        expectThrows(IllegalArgumentException.class, () -> BehaviorEnergyRules.readCapacity(true, -1, 500), "gt.capacity");
        BehaviorEnergyRules.requireEmit(32, 1, 32, 4);
        assertions++;
        expectThrows(IllegalArgumentException.class, () -> BehaviorEnergyRules.requireEmit(64, 1, 32, 4), "exceeds");
        expectThrows(IllegalArgumentException.class, () -> BehaviorEnergyRules.requireEmit(32, 5, 32, 4), "exceeds");
        expectThrows(IllegalStateException.class, () -> BehaviorEnergyRules.requireEmit(32, 1, 0, 0), "no energy output");

        var stored = BehaviorMachineDeclaration.builder("ru_buffer", "Steel")
                .energy("RU", 32).gui("aa_sample:textures/gui/machine.png")
                .energyCapacity(32_000)
                .energyFaces(List.of(BehaviorMachineDeclaration.Face.LEFT, BehaviorMachineDeclaration.Face.RIGHT))
                .energyOutputs(BehaviorMachineDeclaration.Face.BACK)
                .energyOut("RU").energyOutMax(32, 1)
                .build();
        check(stored.energyCapacity() == 32_000, "declared capacity");
        check(stored.energyOutSize() == 32 && stored.energyOutAmount() == 1, "output maximum");
        check(stored.emitsEnergy() && stored.energyOutputFaces().size() == 1, "one output face");
        var plain = BehaviorMachineDeclaration.builder("irrigator", "Steel")
                .energy("EU", 32).gui("aa_sample:textures/gui/machine.png").build();
        check(plain.energyCapacity() == 0 && !plain.emitsEnergy() && plain.energyOut().isEmpty(), "storage defaults stay empty");
        expectThrows(IllegalArgumentException.class, () -> BehaviorMachineDeclaration.builder("bad_press", "Steel")
                .energy("EU", 32).gui("aa_sample:textures/gui/machine.png").energyCapacity(-1).build(), "negative energy capacity");
        expectThrows(IllegalArgumentException.class, () -> BehaviorMachineDeclaration.builder("bad_press", "Steel")
                .energy("EU", 32).gui("aa_sample:textures/gui/machine.png").energyOut("RU").build(), "energy output needs");
        expectThrows(IllegalArgumentException.class, () -> BehaviorMachineDeclaration.builder("bad_press", "Steel")
                .energy("EU", 32).gui("aa_sample:textures/gui/machine.png")
                .energyOutputs(BehaviorMachineDeclaration.Face.BACK, BehaviorMachineDeclaration.Face.BACK)
                .energyOut("RU").energyOutMax(32, 1).build(), "repeats an energy output face");

        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(), List.of(
                BehaviorMachineDeclaration.builder("ru_buffer", "Steel").energy("RU", 32)
                        .gui("aa_sample:textures/gui/machine.png")
                        .energyOutputs(BehaviorMachineDeclaration.Face.BACK).energyOut("ZZ").energyOutMax(32, 1).build()));
        expectInstall("aa_sample", "unknown energy output", "ZZ");
    }

    private static void behaviorRange() {
        var plain = BehaviorMachineDeclaration.builder("irrigator", "Steel")
                .energy("EU", 32).gui("aa_sample:textures/gui/machine.png").build();
        check(plain.maxRange() == 0, "range defaults to 0");
        var miner = BehaviorMachineDeclaration.builder("miner", "Steel")
                .energy("EU", 32).gui("aa_sample:textures/gui/machine.png").maxRange(4).build();
        check(miner.maxRange() == 4, "declared range");
        expectThrows(IllegalArgumentException.class, () -> BehaviorMachineDeclaration.builder("bad_press", "Steel")
                .energy("EU", 32).gui("aa_sample:textures/gui/machine.png").maxRange(-1).build(), "range must be");
        expectThrows(IllegalArgumentException.class, () -> BehaviorMachineDeclaration.builder("bad_press", "Steel")
                .energy("EU", 32).gui("aa_sample:textures/gui/machine.png").maxRange(BehaviorRangeRules.LIMIT + 1).build(), "range must be");
        expectThrows(IllegalStateException.class, () -> BehaviorRangeRules.requireUsable(0), "range is 0");
        expectThrows(IllegalStateException.class, () -> BehaviorRangeRules.requireRadius(0, 1), "range is 0");
        expectThrows(IllegalArgumentException.class, () -> BehaviorRangeRules.requireInside(4, 5, 0, 0), "outside range");
        expectThrows(IllegalArgumentException.class, () -> BehaviorRangeRules.requireInside(4, 0, 0, 0), "machine block");
        expectThrows(IllegalArgumentException.class, () -> BehaviorRangeRules.requireRadius(4, 5), "outside range");
        BehaviorRangeRules.requireInside(4, 4, -4, 4);
        assertions++;
        BehaviorRangeRules.requireRadius(4, 0);
        assertions++;
        check(BehaviorRangeRules.inside(4, 4, 0, 0) && !BehaviorRangeRules.inside(0, 1, 0, 0), "closed range rejects a neighbor");
        check(BehaviorRangeRules.BREAKS_PER_TICK == 1 && BehaviorRangeRules.ITEMS_PER_TICK == 16, "tick budget");
    }

    private static void bounds() {
        expectThrows(IllegalArgumentException.class, () -> BehaviorMachineDeclaration.builder("bad_press", "Steel")
                .energy("EU", 32).gui("aa_sample:textures/gui/machine.png").energyGauge(-1, 8).build(),
                "outside");
    }

    private static void behaviorEnergy() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(), List.of(irrigator("bad_press", 8000,
                BehaviorMachineDeclaration.SlotRole.INPUT, "", "ZZ")));
        expectInstall("aa_sample", "unknown", "energy", "ZZ");
    }

    private static void missingFactory() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(), List.of(irrigator("irrigator", 8000,
                BehaviorMachineDeclaration.SlotRole.INPUT, "", "EU")));
        expectInstall("aa_sample", "irrigator", "no", "behavior", "factory");
    }

    private static void sharedBlockEntity() {
        prepare();
        GTMaterialRegistry.init();
        BehaviorMachineFactories.collect(Map.of("aa_sample", Set.of("irrigator")));
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(), List.of(
                irrigator("irrigator", 8000, BehaviorMachineDeclaration.SlotRole.INPUT, "be_irrigator", "EU"),
                irrigator("irrigator_2", 16000, BehaviorMachineDeclaration.SlotRole.INPUT, "be_irrigator", "EU")));
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        check(AddonMachines.behaviorMachines().size() == 2, "both tiers accepted");
        check(AddonMachines.machines().isEmpty(), "behavior machines are not recipe machines");
        for (var machine : AddonMachines.behaviorMachines())
            check("be_irrigator".equals(machine.blockEntityPath()), "shared block entity path");
        check(AddonMachines.behaviorMachines().get(0).declaration().fluids().get(0).capacity() == 8000, "tier one tank");
        check(AddonMachines.behaviorMachines().get(1).declaration().fluids().get(0).capacity() == 16000, "tier two tank");
        check(AddonMachines.behaviorMachines().get(0).energyInMin()
                == BasicMachineParameters.defaultEnergyInMin("EU", 32), "energy minimum");
        check(AddonMachines.behaviorMachines().get(0).energyInMax()
                == BasicMachineParameters.defaultEnergyInMax("EU", 32), "energy maximum");
    }

    private static void behaviorConflict() {
        prepare();
        GTMaterialRegistry.init();
        BehaviorMachineFactories.collect(Map.of("aa_one", Set.of("irrigator")));
        AddonMachines.accept("aa_one", List.of(), List.of(), List.of(), List.of(irrigator("irrigator", 8000,
                BehaviorMachineDeclaration.SlotRole.INPUT, "", "EU")));
        AddonMachines.accept("aa_two", List.of(), List.of(), List.of(), List.of(irrigator("irrigator", 8000,
                BehaviorMachineDeclaration.SlotRole.INPUT, "", "EU")));
        expectInstall("aa_two", "irrigator", "aa_one");
    }

    private static void behaviorSlots() {
        prepare();
        GTMaterialRegistry.init();
        BehaviorMachineFactories.collect(Map.of("aa_sample", Set.of("irrigator")));
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(), List.of(
                irrigator("irrigator", 8000, BehaviorMachineDeclaration.SlotRole.INPUT, "", "EU"),
                irrigator("irrigator_2", 8000, BehaviorMachineDeclaration.SlotRole.OUTPUT, "be_other", "EU")));
        expectInstall("aa_sample", "irrigator", "disagrees");
    }

    private static void behaviorLayout() {
        prepare();
        GTMaterialRegistry.init();
        BehaviorMachineFactories.collect(Map.of("aa_sample", Set.of("irrigator")));
        AddonMachines.accept("aa_sample", List.of(), List.of(),
                List.of(new AddonMachineLayout("irrigator", List.of(
                        new AddonMachineLayout.Cell(1, 0, 0, "tank_wall", StructureGrid.Role.CASING)))),
                List.of(irrigator("irrigator", 8000, BehaviorMachineDeclaration.SlotRole.INPUT, "", "EU")));
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        check(SharedLargeMachineLayouts.cells("irrigator") != null, "behavior machine owns the layout");
        check(AddonMachines.behaviorMachines().size() == 1, "behavior machine accepted");
    }

    private static void energyPlain() {
        var casing = new StructurePartDeclaration("sample_wall", "Steel", "", 6.0F);
        check(casing.energy() == null, "casing has no energy role");
        prepare();
        GTMaterialRegistry.init();
        int steel = SharedLargeMachineParts.idForName("steel_wall");
        AddonMachines.accept("aa_part", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(casing, new StructurePartDeclaration("sample_emitter", "Steel", "", 6.0F,
                        new com.gregtech.gregtech.api.addon.StructurePartEnergy("EU", 1_000,
                                List.of(BehaviorMachineDeclaration.Face.FRONT), 32, 1))));
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        check(AddonMachines.structureParts().get(0).declaration().energy() == null, "installed casing matches card 25");
        var energy = AddonMachines.structureParts().get(1).declaration().energy();
        check(energy != null && "EU".equals(energy.type()) && energy.capacity() == 1_000, "energy role kept");
        check(SharedLargeMachineParts.idForName("steel_wall") == steel && steel == 18009, "steel wall stays 18009");
        check(AddonMachines.partId("sample_wall") == 1_000_000, "casing id still starts at 1000000");
    }

    private static void energyMax() {
        expectThrows(IllegalArgumentException.class, () -> new com.gregtech.gregtech.api.addon.StructurePartEnergy(
                "EU", 0, List.of(BehaviorMachineDeclaration.Face.FRONT), 32, 1), "capacity", "positive");
        expectThrows(IllegalArgumentException.class, () -> new com.gregtech.gregtech.api.addon.StructurePartEnergy(
                "EU", 10, List.of(BehaviorMachineDeclaration.Face.FRONT, BehaviorMachineDeclaration.Face.FRONT), 32, 1),
                "repeats");
        expectThrows(IllegalArgumentException.class, () -> new com.gregtech.gregtech.api.addon.StructurePartEnergy(
                "EU", 10, List.of(), 32, 1), "output face");
        expectThrows(IllegalArgumentException.class, () -> new com.gregtech.gregtech.api.addon.StructurePartEnergy(
                "EU", 10, List.of(BehaviorMachineDeclaration.Face.FRONT), 0, 1), "positive");
        var energy = new com.gregtech.gregtech.api.addon.StructurePartEnergy("RU", 100,
                List.of(BehaviorMachineDeclaration.Face.FRONT), 32, 1);
        check(energy.accept(90, 50) == 10, "charge stops at capacity");
        check(energy.accept(100, 5) == 0, "full buffer accepts nothing");
        expectThrows(IllegalArgumentException.class, () -> energy.requireOutput(64, 1), "exceeds");
        energy.requireOutput(32, 1);
    }

    private static void energyUnknown() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_part", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new StructurePartDeclaration("sample_emitter", "Steel", "", 6.0F,
                        new com.gregtech.gregtech.api.addon.StructurePartEnergy("ZZ", 1_000,
                                List.of(BehaviorMachineDeclaration.Face.FRONT), 32, 1))));
        expectInstall("aa_part", "unknown energy", "ZZ");
    }

    private static BehaviorMachineDeclaration irrigator(String id, int capacity,
                                                        BehaviorMachineDeclaration.SlotRole input,
                                                        String blockEntity, String energy) {
        var builder = BehaviorMachineDeclaration.builder(id, "Steel")
                .machineType("irrigator")
                .energy(energy, 32)
                .gui("aa_sample:textures/gui/machine.png")
                .item(44, 26, input)
                .item(116, 26, BehaviorMachineDeclaration.SlotRole.OUTPUT)
                .fluid(capacity, 152, 8, BehaviorMachineDeclaration.SlotRole.INPUT)
                .toggle("aa.irrigator.water", 26, 64, true);
        if (!blockEntity.isEmpty()) builder.blockEntityId(blockEntity);
        return builder.build();
    }

    private static void fuelOk() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(), List.of(),
                List.of(new AddonMachineLayout("sampleturbine", List.of(
                        new AddonMachineLayout.Cell(0, 0, 1, "tank_wall", StructureGrid.Role.FLUID_INPUT)))),
                List.of(), List.of(fuelMap("aa.fuel.gas")),
                List.of(engine("aa.fuel.gas"), turbine("gt.recipe.fuels.engine")));
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        check(AddonMachines.fuelMaps().size() == 1, "one fuel map accepted");
        check("aa.fuel.gas".equals(AddonMachines.fuelMaps().get(0).spec().mNameInternal), "fuel map name kept");
        check(AddonMachines.fuelGenerators().size() == 2, "engine and turbine accepted");
        check(AddonMachines.fuelGenerators().get(1).declaration().output().distance() == 4, "turbine outputs four blocks behind");
        var cells = SharedLargeMachineLayouts.cells("sampleturbine");
        check(cells != null && cells.size() == 1 && cells.get(0).part() == 18002, "turbine layout uses the tank wall");
        boolean closed = false;
        try {
            AddonMachines.accept("aa_late", List.of(), List.of(), List.of());
        } catch (IllegalStateException failure) {
            closed = failure.getMessage().contains("closed");
        }
        check(closed, "the queue closes after install");
    }

    private static void unknownFuelMap() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(engine("aa.fuel.missing")));
        expectInstall("aa_sample", "unknown", "fuel", "aa.fuel.missing");
    }

    private static void unknownFuelEnergy() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(), List.of(), List.of(fuelMap("aa.fuel.gas")),
                List.of(FuelGeneratorDeclaration.builder("engine", "Steel").fuelMap("aa.fuel.gas")
                        .energy("ZZ", 16).single().build()));
        expectInstall("aa_sample", "unknown", "energy", "ZZ");
    }

    private static void fuelWithoutStructure() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(turbine("gt.recipe.fuels.gas")));
        expectInstall("aa_sample", "sampleturbine", "no", "structure");
    }

    private static void fuelNameCollision() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(), List.of(),
                List.of(fuelMap("gt.recipe.fuels.gas")), List.of());
        expectInstall("aa_sample", "fuel", "gt.recipe.fuels.gas", "already exists");
    }

    private static void recipeMachineCannotUseFuelMap() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(), List.of(machine("sample_press", "samplepress", "aa.fuel.gas")),
                List.of(), List.of(), List.of(fuelMap("aa.fuel.gas")), List.of());
        expectInstall("aa_sample", "unknown", "recipe", "aa.fuel.gas");
    }

    private static void fuelBounds() {
        expectThrows(IllegalArgumentException.class, () -> FuelGeneratorDeclaration.builder("engine", "Steel")
                .fuelMap("aa.fuel.gas").energy("RU", 16).efficiency(0).build(), "efficiency");
        expectThrows(IllegalArgumentException.class, () -> FuelGeneratorDeclaration.builder("engine", "Steel")
                .fuelMap("aa.fuel.gas").energy("RU", 16).efficiency(100_001).build(), "efficiency");
        expectThrows(IllegalArgumentException.class, () -> FuelGeneratorDeclaration.builder("engine", "Steel")
                .fuelMap("aa.fuel.gas").energy("RU", 0).build(), "parameters");
        expectThrows(IllegalArgumentException.class, () -> FuelGeneratorDeclaration.builder("engine", "Steel")
                .fuelMap("aa.fuel.gas").energy("RU", 16).tank(0).build(), "parameters");
        expectThrows(IllegalArgumentException.class, () -> FuelGeneratorDeclaration.builder("engine", "Steel")
                .fuelMap("aa.fuel.gas").energy("RU", 16).outputBehind(0).build(), "distance");
        expectThrows(IllegalArgumentException.class, () -> FuelGeneratorDeclaration.builder("engine", "Steel")
                .fuelMap("aa.fuel.gas").energy("RU", 16).outputBehind(9).build(), "distance");
        expectThrows(IllegalArgumentException.class, () -> new com.gregtech.gregtech.api.addon.FuelMapDeclaration(
                "Not A Map", "Gas", "aa_sample:textures/gui/machines/gas",
                0, 0, 0, 1, 1, 1, 1, false, false, false, false), "fuel");
    }

    private static RecipeMapSpec fuelMap(String name) {
        return new RecipeMapSpec(null, name, "Gas", "aa_sample:textures/gui/machines/gas",
                0, 0, 0, 1, 1, 1, 1, false, false, false, false);
    }

    private static FuelGeneratorDeclaration engine(String fuelMap) {
        return FuelGeneratorDeclaration.builder("engine", "Steel").machineType("sampleengine")
                .fuelMap(fuelMap).efficiency(10_000).energy("RU", 16).tank(4000).outputFront().single().build();
    }

    private static FuelGeneratorDeclaration turbine(String fuelMap) {
        return FuelGeneratorDeclaration.builder("turbine", "Steel").machineType("sampleturbine")
                .fuelMap(fuelMap).efficiency(5_000).energy("RU", 32).tank(8000).outputBehind(4).multi().build();
    }

    private static void ioTarget() {
        var right = BehaviorMachineDeclaration.Face.RIGHT;
        expectThrows(IllegalArgumentException.class, () -> new MachineDeclaration.Target(0, 0, 0, right), "origin");
        expectThrows(IllegalArgumentException.class, () -> new MachineDeclaration.Target(17, 0, 0, right), "out of range");
        expectThrows(IllegalArgumentException.class, () -> new MachineDeclaration.Target(0, -17, 0, right), "out of range");
        expectThrows(IllegalArgumentException.class, () -> new MachineDeclaration.Target(0, 0, 17, right), "out of range");
        expectThrows(IllegalArgumentException.class, () -> new MachineDeclaration.Target(1, 0, 0, null), "receiver");
        var edge = new MachineDeclaration.Target(-16, 16, -16, BehaviorMachineDeclaration.Face.BACK);
        check(edge.right() == -16 && edge.up() == 16 && edge.back() == -16, "±16 stays inside the limit");
        expectThrows(IllegalArgumentException.class, () -> MachineDeclaration.builder("press", "Steel")
                .energy("EU", 32).recipeMap("aa.recipe.press").basic()
                .itemOutputTarget(1, 0, 0, right).build(), "large recipe");
        expectThrows(IllegalArgumentException.class, () -> MachineDeclaration.builder("press", "Steel")
                .energy("EU", 32).recipeMap("aa.recipe.press").largeRecipe()
                .itemAutoOutput(BehaviorMachineDeclaration.Face.BOTTOM)
                .itemOutputTarget(1, 0, 0, right).build(), "conflicts");
        expectThrows(IllegalArgumentException.class, () -> MachineDeclaration.builder("press", "Steel")
                .energy("EU", 32).recipeMap("aa.recipe.press").largeRecipe()
                .fluidAutoOutput(BehaviorMachineDeclaration.Face.BOTTOM)
                .fluidOutputTarget(1, 0, 0, right).build(), "conflicts");
        expectThrows(IllegalArgumentException.class, () -> MachineDeclaration.builder("press", "Steel")
                .energy("EU", 32).recipeMap("aa.recipe.press").largeRecipe()
                .itemAutoInput(BehaviorMachineDeclaration.Face.TOP)
                .itemInputSource(1, 0, 0, right).build(), "conflicts");
        expectThrows(IllegalArgumentException.class, () -> MachineDeclaration.builder("press", "Steel")
                .energy("EU", 32).recipeMap("aa.recipe.press").largeRecipe()
                .fluidAutoInput(BehaviorMachineDeclaration.Face.TOP)
                .fluidInputSource(1, 0, 0, right).build(), "conflicts");
        var plain = MachineDeclaration.builder("plain", "Steel").energy("EU", 512).recipeMap("aa.recipe.press")
                .largeRecipe().structureKeys(List.of("aa.structure.press")).build();
        check(plain.itemOutputTarget() == null && plain.fluidOutputTarget() == null
                && plain.itemInputSource() == null && plain.fluidInputSource() == null
                && plain.itemAutoOutput() == null, "an undeclared machine keeps every target absent");
        var aimed = MachineDeclaration.builder("aimed", "Steel").energy("EU", 512).recipeMap("aa.recipe.press")
                .largeRecipe().structureKeys(List.of("aa.structure.press"))
                .itemOutputTarget(2, 0, 1, BehaviorMachineDeclaration.Face.RIGHT)
                .fluidOutputTarget(-2, 0, 1, BehaviorMachineDeclaration.Face.LEFT)
                .itemInputSource(0, 2, 1, BehaviorMachineDeclaration.Face.BOTTOM)
                .fluidInputSource(0, 1, 2, BehaviorMachineDeclaration.Face.FRONT)
                .build();
        check(aimed.itemAutoOutput() == null && aimed.fluidAutoOutput() == null
                && aimed.itemAutoInput() == null && aimed.fluidAutoInput() == null,
                "a target leaves the automatic face unset");
        check(aimed.itemOutputTarget().right() == 2 && aimed.fluidOutputTarget().right() == -2
                && aimed.itemInputSource().up() == 2 && aimed.fluidInputSource().back() == 2,
                "the four cells are kept");
        prepare();
        GTMaterialRegistry.init();
        var steel = GTMaterialRegistry.get("Steel");
        expectThrows(IllegalArgumentException.class, () -> BasicMachineParameters.builder("press", steel)
                .energy(GregTechTags.Energy.EU, 32)
                .faces(MachineFaceMasks.builder().itemAutoOut(MachineFaceMasks.BOTTOM).build())
                .itemOutputTarget(new MachineDeclaration.Target(1, 0, 0, right))
                .build(), "conflicts");
        var bare = BasicMachineParameters.builder("plain_press", steel).machineType("sampleplain")
                .energy(GregTechTags.Energy.EU, 512).build();
        check(bare.itemOutputTarget() == null && bare.fluidOutputTarget() == null
                && bare.itemInputSource() == null && bare.fluidInputSource() == null
                && bare.faceConfig().itemAutoOutput() == -1 && bare.faceConfig().fluidAutoOutput() == -1,
                "no target leaves the previous face mask");
        var item = new MachineDeclaration.Target(2, 0, 1, BehaviorMachineDeclaration.Face.RIGHT);
        var fluid = new MachineDeclaration.Target(-2, 0, 1, BehaviorMachineDeclaration.Face.LEFT);
        var itemIn = new MachineDeclaration.Target(0, 2, 1, BehaviorMachineDeclaration.Face.BOTTOM);
        var fluidIn = new MachineDeclaration.Target(0, 1, 2, BehaviorMachineDeclaration.Face.FRONT);
        var aimedParameters = BasicMachineParameters.builder("aimed_press", steel).machineType("sampletarget")
                .energy(GregTechTags.Energy.EU, 512)
                .itemOutputTarget(item).fluidOutputTarget(fluid)
                .itemInputSource(itemIn).fluidInputSource(fluidIn).build();
        check(aimedParameters.itemOutputTarget().equals(item) && aimedParameters.faceConfig().itemAutoOutput() == -1
                && aimedParameters.faceConfig().fluidAutoInput() == -1, "parameters keep a target beside an unset automatic face");
        var fermenterItems = com.gregtech.gregtech.content.multiblock.OriginalLargeRecipeMachineData.output("largefermenter", true);
        var fermenterFluids = com.gregtech.gregtech.content.multiblock.OriginalLargeRecipeMachineData.output("largefermenter", false);
        check(fermenterItems.right() == 0 && fermenterItems.up() == 1 && fermenterItems.back() == 5
                && fermenterItems.receiverFace() == com.gregtech.gregtech.content.multiblock.OriginalLargeRecipeMachineData.Face.FRONT,
                "fermenter item target stays five blocks behind");
        check(fermenterFluids.right() == 0 && fermenterFluids.up() == 0 && fermenterFluids.back() == 5
                && fermenterFluids.receiverFace() == com.gregtech.gregtech.content.multiblock.OriginalLargeRecipeMachineData.Face.FRONT,
                "fermenter fluid target stays five blocks behind");
        var usual = new com.gregtech.gregtech.content.multiblock.OriginalLargeRecipeMachineData.Target(0, -1, 0,
                com.gregtech.gregtech.content.multiblock.OriginalLargeRecipeMachineData.Face.UP);
        for (var name : List.of("largecentrifuge", "largeelectrolyzer", "largecoagulator", "largeautoclave", "largebath",
                "largemixer", "largeoven", "largesluice", "largecrusher", "largeshredder", "largesqueezer")) {
            check(com.gregtech.gregtech.content.multiblock.OriginalLargeRecipeMachineData.output(name, true).equals(usual)
                    && com.gregtech.gregtech.content.multiblock.OriginalLargeRecipeMachineData.output(name, false).equals(usual),
                    "default output target " + name);
        }
        check(com.gregtech.gregtech.content.multiblock.OriginalLargeRecipeMachineData.sources("largecentrifuge").size() == 2,
                "centrifuge energy sources stay a pair");
        AddonMachines.accept("aa_io", List.of(map("aa.recipe.press")), List.of(
                new AddonMachineDefinition(bare, "aa.recipe.press", MachineControllerKind.LARGE_RECIPE, 10_000, true,
                        List.of("aa.structure.press")),
                new AddonMachineDefinition(aimedParameters, "aa.recipe.press", MachineControllerKind.LARGE_RECIPE, 10_000, true,
                        List.of("aa.structure.press"))),
                List.of(layout("sampleplain"), layout("sampletarget")));
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        BasicMachineParameters installedPlain = null;
        BasicMachineParameters installedAimed = null;
        for (var machine : AddonMachines.machines()) {
            if ("sampleplain".equals(machine.definition().parameters().machineName())) installedPlain = machine.definition().parameters();
            if ("sampletarget".equals(machine.definition().parameters().machineName())) installedAimed = machine.definition().parameters();
        }
        check(installedPlain != null && installedPlain.itemOutputTarget() == null && installedPlain.fluidOutputTarget() == null
                && installedPlain.itemInputSource() == null && installedPlain.fluidInputSource() == null,
                "an undeclared installed machine still has no targets");
        check(installedAimed != null && item.equals(installedAimed.itemOutputTarget()) && fluid.equals(installedAimed.fluidOutputTarget())
                && itemIn.equals(installedAimed.itemInputSource()) && fluidIn.equals(installedAimed.fluidInputSource()),
                "installed targets match the declaration");
    }

    private static AddonMachineLayout layout(String name) {
        return new AddonMachineLayout(name, List.of(
                new AddonMachineLayout.Cell(1, 0, 0, "tank_wall", StructureGrid.Role.CASING)));
    }

    private static void prepare() {
        ModData.bindPresence(id -> true);
        GTMaterialRegistry.setLogSink((warning, message) -> {});
        PrefixRegistry.ensurePrefixesLoaded();
        ModReferences.UNKNOWN.getClass();
        com.gregtech.gregtech.data.MaterialGroups.Glowstone.getClass();
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

    private static void gasOk() {
        prepare();
        GTMaterialRegistry.init();
        expectThrows(IllegalArgumentException.class,
                () -> new CompressedGasTankDeclaration("sample_drum", "Steel", 0, false, false, false),
                "capacity");
        int melt = GTMaterialRegistry.get("Steel").getMeltingPoint();
        var drum = new CompressedGasTankDeclaration("sample_drum", "Steel", 12_800_000, false, false, false);
        check(drum.gasProof() && !drum.acidProof() && !drum.plasmaProof() && !drum.magicProof(), "gas proof is fixed");
        tanks("aa_gas", drum);
        tanks("aa_other", drum);
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        check(AddonMachines.compressedGasTanks().size() == 2, "same path in two namespaces");
        check(AddonMachines.compressedGasTanks().get(0).meltingPoint() == melt, "melting point");
        check(AddonMachines.compressedGasTanks().get(0).declaration().capacity() == 12_800_000, "capacity kept");
    }

    private static void gasDuplicate() {
        prepare();
        GTMaterialRegistry.init();
        var drum = new CompressedGasTankDeclaration("sample_drum", "Steel", 1000, false, false, false);
        tanks("aa_gas", drum, drum);
        expectInstall("aa_gas", "sample_drum", "twice");
    }

    private static void gasMaterial() {
        prepare();
        GTMaterialRegistry.init();
        tanks("aa_gas", new CompressedGasTankDeclaration("sample_drum", "NotAMaterial", 1000, false, false, false));
        expectInstall("aa_gas", "unknown", "material", "NotAMaterial");
    }

    private static void tanks(String addon, CompressedGasTankDeclaration... drums) {
        AddonMachines.accept(addon, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(drums));
    }

    public static final class EnergyContributor implements GregTechMachineContributor {
        @Override public String addonId() { return "aa_sample"; }
        @Override public List<AddonMachineDefinition> machines() {
            new BasicMachineParameters("bad_press", GTMaterialRegistry.get("Steel"), "badpress", "ZZ",
                    1, 32, 0, 5, 5, com.gregtech.gregtech.api.energy.MachineFaceMasks.ALL_SIDES,
                    List.of(), 1, 16, 64);
            return List.of();
        }
    }

    public static final class ServiceContributor implements GregTechMachineContributor {
        @Override public String addonId() { return "aa_sample"; }
        @Override public List<RecipeMapSpec> recipeMaps() { return List.of(map("aa.recipe.press")); }
        @Override public List<AddonMachineDefinition> machines() {
            return List.of(large("large_press", "largesamplepress", "aa.recipe.press"));
        }
        @Override public List<AddonMachineLayout> layouts() {
            return List.of(new AddonMachineLayout("largesamplepress", List.of(
                    new AddonMachineLayout.Cell(1, 0, 0, "tank_wall", StructureGrid.Role.CASING))));
        }
    }
}
