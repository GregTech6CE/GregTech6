package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.addon.AddonMachine;
import com.gregtech.gregtech.api.addon.BehaviorEnergyRules;
import com.gregtech.gregtech.api.addon.BehaviorMachine;
import com.gregtech.gregtech.api.addon.BehaviorRecipeCatalysts;
import com.gregtech.gregtech.api.addon.AddonOverclock;
import com.gregtech.gregtech.api.addon.AddonMachineContributor;
import com.gregtech.gregtech.api.addon.AddonMachineDefinition;
import com.gregtech.gregtech.api.addon.CellMatch;
import com.gregtech.gregtech.api.addon.CompressedGasTankDeclaration;
import com.gregtech.gregtech.api.addon.EnergyInput;
import com.gregtech.gregtech.api.addon.EnergyOutput;
import com.gregtech.gregtech.api.addon.FuelGenerator;
import com.gregtech.gregtech.api.addon.FuelOutput;
import com.gregtech.gregtech.api.addon.LayoutCell;
import com.gregtech.gregtech.api.addon.MachineCasing;
import com.gregtech.gregtech.api.addon.MachineIo;
import com.gregtech.gregtech.api.addon.RecipeMachine;
import com.gregtech.gregtech.api.addon.RecipeTable;
import com.gregtech.gregtech.api.addon.StructureLayout;
import com.gregtech.gregtech.api.addon.StructureRole;
import com.gregtech.gregtech.api.addon.BehaviorRangeRules;
import com.gregtech.gregtech.api.addon.ExclusiveOutputRules;
import com.gregtech.gregtech.api.addon.RecipeOutputChances;
import com.gregtech.gregtech.api.energy.MachineFaceMasks;
import com.gregtech.gregtech.api.addon.AddonMachineLayout;
import com.gregtech.gregtech.api.addon.AddonMachines;
import com.gregtech.gregtech.api.addon.BehaviorMachineFactories;
import com.gregtech.gregtech.api.addon.LayoutPart;
import com.gregtech.gregtech.api.addon.LayoutPartFactories;
import com.gregtech.gregtech.api.addon.PartSlots;
import com.gregtech.gregtech.api.addon.IoTarget;
import com.gregtech.gregtech.api.addon.MachineFace;
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
            case "partfn" -> partFunctional();
            case "partmiss" -> partMissingFactory();
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
            case "binput" -> behaviorInput();
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
            case "erange" -> energyRange();
            case "erangedisagree" -> energyRangeDisagree();
            case "erangetu" -> energyRangeTu();
            case "plainblock" -> plainBlock();
            case "plainrole" -> plainRole();
            case "bcatalyst" -> behaviorCatalyst();
            case "bunknown" -> unknownBehaviorMap();
            case "bforeign" -> foreignBehaviorMap();
            default -> throw new IllegalArgumentException("Unknown scenario: " + args[0]);
        }
        System.out.println(args[0] + " passed " + assertions);
    }

    private static void knobs() {
        var plain = RecipeMachine.builder(MachineCasing.builder("press", "Steel").build())
                .energy(EnergyInput.builder("EU", 32).build())
                .recipeMap("aa.recipe.press").basic().build();
        check(plain.io() == null && !plain.energy().facesSpecified(), "default faces are unspecified");
        check(!plain.cheapOverclocking() && !plain.cheapOverclockingSpecified(), "default cheap overclock is off and unset");
        check(plain.minimumLightAbove() == 0, "default light gate is off");
        check(!plain.searchOutputTanks() && !plain.exclusiveOutputs(), "default output-tank search and exclusive outputs are off");
        check(!AddonOverclock.enabled(true, true, plain.cheapOverclocking(), true), "addon basic ignores a cheap built-in name");
        check(AddonOverclock.enabled(false, false, false, true), "built-in legacy cheap stays");
        check(!AddonOverclock.enabled(false, true, false, true), "an explicit off replaces a large machine's legacy cheap rule");

        var declared = RecipeMachine.builder(MachineCasing.builder("press", "Steel").build())
                .energy(EnergyInput.builder("EU", 32).faces(MachineFace.BACK).build())
                .io(MachineIo.builder()
                        .itemInputs(MachineFace.TOP)
                        .itemOutputs(MachineFace.BOTTOM)
                        .itemAutoOutput(MachineFace.BOTTOM)
                        .build())
                .recipeMap("aa.recipe.press")
                .cheapOverclocking(true)
                .minimumLightAbove(14)
                .build();
        check(declared.io() != null && declared.io().itemAutoOutput() == MachineFace.BOTTOM, "declared faces");
        check(declared.cheapOverclocking() && declared.cheapOverclockingSpecified(), "cheap overclock opt-in");
        check(AddonOverclock.enabled(true, true, declared.cheapOverclocking(), false), "opt-in cheap overclock is on");
        check(declared.minimumLightAbove() == 14, "light gate");
        int top = 0;
        for (var face : declared.io().itemInputs()) top |= face.bit();
        check(top == (1 << MachineFace.TOP.index()), "top bit matches the face index");
        expectThrows(IllegalArgumentException.class, () -> RecipeMachine.builder(MachineCasing.builder("press", "Steel").build())
                .energy(EnergyInput.builder("EU", 32).build())
                .recipeMap("aa.recipe.press").minimumLightAbove(16).build(), "light");
        int[] certain = RecipeOutputChances.normalize(null, 2);
        check(certain.length == 2 && certain[0] == 10_000 && certain[1] == 10_000, "missing chances are certain");
        check(RecipeOutputChances.normalize(new int[0], 1)[0] == 10_000, "an empty chance list is certain");
        int[] half = RecipeOutputChances.normalize(new int[]{5_000, 10_000}, 2);
        check(half[0] == 5_000 && half[1] == 10_000, "declared chances are kept");
        expectThrows(IllegalArgumentException.class, () -> RecipeOutputChances.normalize(new int[]{5_000}, 2), "does not match");
        expectThrows(IllegalArgumentException.class, () -> RecipeOutputChances.normalize(new int[]{0}, 1), "1..");

        var detector = RecipeMachine.builder(MachineCasing.builder("detector", "Steel").build())
                .energy(EnergyInput.builder("EU", 32).build())
                .recipeMap("aa.recipe.detector")
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
                List.of(LayoutPart.builder("sample_wall", "Steel").hardness(6.0F).build(),
                        LayoutPart.builder("sample_cap", "Iron").texture("block/sample_cap").hardness(4.0F).build()));
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

    private static void partMissingFactory() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.acceptLayoutParts("aa_part", List.of(computePart()));
        expectInstall("aa_part", "sample_compute", "no part factory");
    }

    private static void partFunctional() {
        prepare();
        GTMaterialRegistry.init();
        LayoutPartFactories.collect(Map.of("aa_part", Set.of("sample_compute")));
        var declared = computePart();
        AddonMachines.acceptLayoutParts("aa_part", List.of(declared));
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        var stored = AddonMachines.structureParts();
        check(stored.size() == 1 && stored.get(0).declaration().equals(declared) && stored.get(0).declaration().functional(),
                "a functional part installs through its factory");
    }

    private static LayoutPart computePart() {
        return LayoutPart.builder("sample_compute", "Steel").hardness(6.0F)
                .slots(PartSlots.builder(4).stackLimit(1).drops(true).build())
                .tick().use().field("power").field("running").build();
    }

    private static void partName() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_part", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(LayoutPart.builder("steel_wall", "Steel").hardness(6.0F).build()));
        expectInstall("aa_part", "steel_wall", "built-in");
    }

    private static void partMaterial() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_part", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(LayoutPart.builder("sample_wall", "NotAMaterial").hardness(6.0F).build()));
        expectInstall("aa_part", "unknown", "material", "NotAMaterial");
    }

    private static void partDuplicate() {
        prepare();
        GTMaterialRegistry.init();
        var part = LayoutPart.builder("sample_wall", "Steel").hardness(6.0F).build();
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
        expectThrows(IllegalArgumentException.class, () -> BehaviorMachine.builder(
                MachineCasing.builder("bad_press", "Steel").build())
                .energy(EnergyInput.builder("EU", 32).build())
                .gui("aa_sample:textures/gui/machine.png")
                .fluid(1000, 8, 8, BehaviorMachine.SlotRole.INPUT).build(),
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

        var stored = BehaviorMachine.builder(MachineCasing.builder("ru_buffer", "Steel").build())
                .energy(EnergyInput.builder("RU", 32).capacity(32_000)
                        .faces(MachineFace.LEFT, MachineFace.RIGHT).build())
                .gui("aa_sample:textures/gui/machine.png")
                .energyOutput(EnergyOutput.builder("RU", 32, 1).faces(MachineFace.BACK).build())
                .build();
        check(stored.energy().capacity() == 32_000, "declared capacity");
        check(stored.output().maxSize() == 32 && stored.output().maxAmount() == 1, "output maximum");
        check(stored.emitsEnergy() && stored.output().faces().size() == 1, "one output face");
        var plain = BehaviorMachine.builder(MachineCasing.builder("irrigator", "Steel").build())
                .energy(EnergyInput.builder("EU", 32).build())
                .gui("aa_sample:textures/gui/machine.png").build();
        check(plain.energy().capacity() == 0 && !plain.emitsEnergy() && plain.output() == null, "storage defaults stay empty");
        expectThrows(IllegalArgumentException.class, () -> EnergyInput.builder("EU", 32).capacity(-1).build(), "Negative energy capacity");
        expectThrows(IllegalArgumentException.class, () -> EnergyOutput.builder("RU", 32, 1).build(), "needs a face");
        expectThrows(IllegalArgumentException.class, () -> EnergyOutput.builder("RU", 32, 1)
                .faces(MachineFace.BACK, MachineFace.BACK).build(), "Repeated");

        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(), List.of(
                BehaviorMachine.builder(MachineCasing.builder("ru_buffer", "Steel").build())
                        .energy(EnergyInput.builder("RU", 32).build())
                        .gui("aa_sample:textures/gui/machine.png")
                        .energyOutput(EnergyOutput.builder("ZZ", 32, 1).faces(MachineFace.BACK).build()).build()));
        expectInstall("aa_sample", "unknown energy output", "ZZ");
    }

    private static void behaviorRange() {
        var plain = BehaviorMachine.builder(MachineCasing.builder("irrigator", "Steel").build())
                .energy(EnergyInput.builder("EU", 32).build())
                .gui("aa_sample:textures/gui/machine.png").build();
        check(plain.maxRange() == 0, "range defaults to 0");
        var miner = BehaviorMachine.builder(MachineCasing.builder("miner", "Steel").build())
                .energy(EnergyInput.builder("EU", 32).build())
                .gui("aa_sample:textures/gui/machine.png").maxRange(4).build();
        check(miner.maxRange() == 4, "declared range");
        expectThrows(IllegalArgumentException.class, () -> BehaviorMachine.builder(MachineCasing.builder("bad_press", "Steel").build())
                .energy(EnergyInput.builder("EU", 32).build())
                .gui("aa_sample:textures/gui/machine.png").maxRange(-1).build(), "range must be");
        expectThrows(IllegalArgumentException.class, () -> BehaviorMachine.builder(MachineCasing.builder("bad_press", "Steel").build())
                .energy(EnergyInput.builder("EU", 32).build())
                .gui("aa_sample:textures/gui/machine.png").maxRange(BehaviorRangeRules.LIMIT + 1).build(), "range must be");
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
        expectThrows(IllegalArgumentException.class, () -> BehaviorMachine.builder(MachineCasing.builder("bad_press", "Steel").build())
                .energy(EnergyInput.builder("EU", 32).build())
                .gui("aa_sample:textures/gui/machine.png").energyGauge(-1, 8).build(),
                "outside");
    }

    private static void behaviorInput() {
        var wheel = BehaviorMachine.builder(MachineCasing.builder("sample_wheel", "Steel").machineType("sample_wheel").build())
                .gui("aa_sample:textures/gui/machine.png")
                .energyOutput(EnergyOutput.builder("EU", 32, 1).faces(MachineFace.TOP).build())
                .build();
        var wide = BehaviorMachine.builder(MachineCasing.builder("sample_wide", "Steel").machineType("sample_wide").build())
                .energy(EnergyInput.builder("EU", 32).anyPacket().capacity(10_000).build())
                .gui("aa_sample:textures/gui/machine.png")
                .build();
        check(wheel.energy() == null && wide.energy().anyPacket(), "declarations keep the new input rules");
        prepare();
        GTMaterialRegistry.init();
        BehaviorMachineFactories.collect(Map.of("aa_sample", Set.of("sample_wheel", "sample_wide")));
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(), List.of(wheel, wide));
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        check(AddonMachines.behaviorMachines().size() == 2, "both machines accepted");
        var installedWheel = AddonMachines.behaviorMachines().get(0);
        var installedWide = AddonMachines.behaviorMachines().get(1);
        check(installedWheel.declaration().energy() == null && installedWheel.energyInMin() == 0
                        && installedWheel.energyInMax() == 0, "output only has no input window");
        check(installedWide.energyInMin() == 1 && installedWide.energyInMax() == Long.MAX_VALUE,
                "anyPacket has no maximum");
    }

    private static void behaviorEnergy() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(), List.of(irrigator("bad_press", 8000,
                BehaviorMachine.SlotRole.INPUT, "", "ZZ")));
        expectInstall("aa_sample", "unknown", "energy", "ZZ");
    }

    private static void missingFactory() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(), List.of(irrigator("irrigator", 8000,
                BehaviorMachine.SlotRole.INPUT, "", "EU")));
        expectInstall("aa_sample", "irrigator", "no", "behavior", "factory");
    }

    private static void sharedBlockEntity() {
        prepare();
        GTMaterialRegistry.init();
        BehaviorMachineFactories.collect(Map.of("aa_sample", Set.of("irrigator")));
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(), List.of(
                irrigator("irrigator", 8000, BehaviorMachine.SlotRole.INPUT, "be_irrigator", "EU"),
                irrigator("irrigator_2", 16000, BehaviorMachine.SlotRole.INPUT, "be_irrigator", "EU")));
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
                BehaviorMachine.SlotRole.INPUT, "", "EU")));
        AddonMachines.accept("aa_two", List.of(), List.of(), List.of(), List.of(irrigator("irrigator", 8000,
                BehaviorMachine.SlotRole.INPUT, "", "EU")));
        expectInstall("aa_two", "irrigator", "aa_one");
    }

    private static void behaviorSlots() {
        prepare();
        GTMaterialRegistry.init();
        BehaviorMachineFactories.collect(Map.of("aa_sample", Set.of("irrigator")));
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(), List.of(
                irrigator("irrigator", 8000, BehaviorMachine.SlotRole.INPUT, "", "EU"),
                irrigator("irrigator_2", 8000, BehaviorMachine.SlotRole.OUTPUT, "be_other", "EU")));
        expectInstall("aa_sample", "irrigator", "disagrees");
    }

    private static void behaviorLayout() {
        prepare();
        GTMaterialRegistry.init();
        BehaviorMachineFactories.collect(Map.of("aa_sample", Set.of("irrigator")));
        AddonMachines.accept("aa_sample", List.of(), List.of(),
                List.of(new AddonMachineLayout("irrigator", List.of(
                        new AddonMachineLayout.Cell(1, 0, 0, "tank_wall", StructureGrid.Role.CASING)))),
                List.of(irrigator("irrigator", 8000, BehaviorMachine.SlotRole.INPUT, "", "EU")));
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        check(SharedLargeMachineLayouts.cells("irrigator") != null, "behavior machine owns the layout");
        check(AddonMachines.behaviorMachines().size() == 1, "behavior machine accepted");
    }

    private static void energyPlain() {
        var casing = LayoutPart.builder("sample_wall", "Steel").hardness(6.0F).build();
        check(casing.energy() == null, "casing has no energy role");
        prepare();
        GTMaterialRegistry.init();
        int steel = SharedLargeMachineParts.idForName("steel_wall");
        AddonMachines.accept("aa_part", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(casing, LayoutPart.builder("sample_emitter", "Steel").hardness(6.0F)
                        .energyOutput(EnergyOutput.builder("EU", 32, 1).faces(MachineFace.FRONT).build(), 1_000).build()));
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        check(AddonMachines.structureParts().get(0).declaration().energy() == null, "installed casing matches card 25");
        var energy = AddonMachines.structureParts().get(1).declaration().energy();
        check(energy != null && "EU".equals(energy.type())
                && AddonMachines.structureParts().get(1).declaration().energyCapacity() == 1_000, "energy role kept");
        check(SharedLargeMachineParts.idForName("steel_wall") == steel && steel == 18009, "steel wall stays 18009");
        check(AddonMachines.partId("sample_wall") == 1_000_000, "casing id still starts at 1000000");
    }

    private static void energyMax() {
        expectThrows(IllegalArgumentException.class, () -> LayoutPart.builder("sample_emitter", "Steel").hardness(6.0F)
                .energyOutput(EnergyOutput.builder("EU", 32, 1).faces(MachineFace.FRONT).build(), 0).build(), "capacity", "positive");
        expectThrows(IllegalArgumentException.class, () -> EnergyOutput.builder("EU", 32, 1)
                .faces(MachineFace.FRONT, MachineFace.FRONT).build(), "Repeated");
        expectThrows(IllegalArgumentException.class, () -> EnergyOutput.builder("EU", 32, 1)
                .faces().build(), "needs a face");
        expectThrows(IllegalArgumentException.class, () -> EnergyOutput.builder("EU", 0, 1)
                .faces(MachineFace.FRONT).build(), "positive");
        check(BasicMachineEnergy.accepted(90, 100, 1, 50) == 10, "charge stops at capacity");
        check(BasicMachineEnergy.accepted(100, 100, 1, 5) == 0, "full buffer accepts nothing");
        expectThrows(IllegalArgumentException.class, () -> BehaviorEnergyRules.requireEmit(64, 1, 32, 1), "exceeds");
        BehaviorEnergyRules.requireEmit(32, 1, 32, 1);
    }

    private static void energyUnknown() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_part", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(LayoutPart.builder("sample_emitter", "Steel").hardness(6.0F)
                        .energyOutput(EnergyOutput.builder("ZZ", 32, 1).faces(MachineFace.FRONT).build(), 1_000).build()));
        expectInstall("aa_part", "unknown energy", "ZZ");
    }

    private static BehaviorMachine irrigator(String id, int capacity,
                                             BehaviorMachine.SlotRole input,
                                             String blockEntity, String energy) {
        var builder = BehaviorMachine.builder(MachineCasing.builder(id, "Steel").machineType("irrigator").build())
                .energy(EnergyInput.builder(energy, 32).build())
                .gui("aa_sample:textures/gui/machine.png")
                .item(44, 26, input)
                .item(116, 26, BehaviorMachine.SlotRole.OUTPUT)
                .fluid(capacity, 152, 8, BehaviorMachine.SlotRole.INPUT)
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
        check(AddonMachines.fuelGenerators().get(1).declaration().output().behind() == 4, "turbine outputs four blocks behind");
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
                List.of(FuelGenerator.builder(MachineCasing.builder("engine", "Steel").build())
                        .fuelMap("aa.fuel.gas")
                        .output(FuelOutput.builder("ZZ", 16).front().build()).single().build()));
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
        var casing = MachineCasing.builder("engine", "Steel").build();
        var out = FuelOutput.builder("RU", 16).front().build();
        expectThrows(IllegalArgumentException.class, () -> FuelGenerator.builder(casing)
                .fuelMap("aa.fuel.gas").output(out).efficiency(0).build(), "efficiency");
        expectThrows(IllegalArgumentException.class, () -> FuelGenerator.builder(casing)
                .fuelMap("aa.fuel.gas").output(out).efficiency(100_001).build(), "efficiency");
        expectThrows(IllegalArgumentException.class, () -> FuelOutput.builder("RU", 0).front().build(), "per tick");
        expectThrows(IllegalArgumentException.class, () -> FuelGenerator.builder(casing)
                .fuelMap("aa.fuel.gas").output(out).tank(0).build(), "parameters");
        expectThrows(IllegalArgumentException.class, () -> FuelOutput.builder("RU", 16).behind(0).build(), "distance");
        expectThrows(IllegalArgumentException.class, () -> FuelOutput.builder("RU", 16).behind(9).build(), "distance");
        expectThrows(IllegalArgumentException.class, () -> RecipeTable.fuel("Not A Map")
                .title("Gas").gui("aa_sample:textures/gui/machines/gas")
                .inputFluids(1).outputFluids(1).minimalInputFluids(1).minimalInputs(1).build(), "fuel");
    }

    private static RecipeMapSpec fuelMap(String name) {
        return new RecipeMapSpec(null, name, "Gas", "aa_sample:textures/gui/machines/gas",
                0, 0, 0, 1, 1, 1, 1, false, false, false, false);
    }

    private static FuelGenerator engine(String fuelMap) {
        return FuelGenerator.builder(MachineCasing.builder("engine", "Steel").machineType("sampleengine").build())
                .fuelMap(fuelMap).efficiency(10_000)
                .output(FuelOutput.builder("RU", 16).front().build())
                .tank(4000).single().build();
    }

    private static FuelGenerator turbine(String fuelMap) {
        return FuelGenerator.builder(MachineCasing.builder("turbine", "Steel").machineType("sampleturbine").build())
                .fuelMap(fuelMap).efficiency(5_000)
                .output(FuelOutput.builder("RU", 32).behind(4).build())
                .tank(8000).multi().build();
    }

    private static void ioTarget() {
        var right = MachineFace.RIGHT;
        expectThrows(IllegalArgumentException.class, () -> IoTarget.at(0, 0, 0, right), "origin");
        expectThrows(IllegalArgumentException.class, () -> IoTarget.at(17, 0, 0, right), "out of range");
        expectThrows(IllegalArgumentException.class, () -> IoTarget.at(0, -17, 0, right), "out of range");
        expectThrows(IllegalArgumentException.class, () -> IoTarget.at(0, 0, 17, right), "out of range");
        expectThrows(IllegalArgumentException.class, () -> IoTarget.at(1, 0, 0, null), "receiver");
        var edge = IoTarget.at(-16, 16, -16, MachineFace.BACK);
        check(edge.right() == -16 && edge.up() == 16 && edge.back() == -16, "±16 stays inside the limit");
        var casing = MachineCasing.builder("press", "Steel").build();
        var energy = EnergyInput.builder("EU", 32).build();
        expectThrows(IllegalArgumentException.class, () -> RecipeMachine.builder(casing)
                .energy(energy).recipeMap("aa.recipe.press").basic()
                .io(MachineIo.builder().itemOutputTarget(1, 0, 0, right).build()).build(), "large recipe");
        expectThrows(IllegalArgumentException.class, () -> MachineIo.builder()
                .itemAutoOutput(MachineFace.BOTTOM)
                .itemOutputTarget(1, 0, 0, right).build(), "conflicts");
        expectThrows(IllegalArgumentException.class, () -> MachineIo.builder()
                .fluidAutoOutput(MachineFace.BOTTOM)
                .fluidOutputTarget(1, 0, 0, right).build(), "conflicts");
        expectThrows(IllegalArgumentException.class, () -> MachineIo.builder()
                .itemAutoInput(MachineFace.TOP)
                .itemInputSource(1, 0, 0, right).build(), "conflicts");
        expectThrows(IllegalArgumentException.class, () -> MachineIo.builder()
                .fluidAutoInput(MachineFace.TOP)
                .fluidInputSource(1, 0, 0, right).build(), "conflicts");
        var plain = RecipeMachine.builder(MachineCasing.builder("plain", "Steel").build())
                .energy(EnergyInput.builder("EU", 512).build()).recipeMap("aa.recipe.press")
                .largeRecipe().structureKeys(List.of("aa.structure.press")).build();
        check(plain.io() == null, "an undeclared machine keeps every target absent");
        var aimed = RecipeMachine.builder(MachineCasing.builder("aimed", "Steel").build())
                .energy(EnergyInput.builder("EU", 512).build()).recipeMap("aa.recipe.press")
                .largeRecipe().structureKeys(List.of("aa.structure.press"))
                .io(MachineIo.builder()
                        .itemOutputTarget(2, 0, 1, MachineFace.RIGHT)
                        .fluidOutputTarget(-2, 0, 1, MachineFace.LEFT)
                        .itemInputSource(0, 2, 1, MachineFace.BOTTOM)
                        .fluidInputSource(0, 1, 2, MachineFace.FRONT).build())
                .build();
        check(aimed.io().itemAutoOutput() == null && aimed.io().fluidAutoOutput() == null
                && aimed.io().itemAutoInput() == null && aimed.io().fluidAutoInput() == null,
                "a target leaves the automatic face unset");
        check(aimed.io().itemOutputTarget().right() == 2 && aimed.io().fluidOutputTarget().right() == -2
                && aimed.io().itemInputSource().up() == 2 && aimed.io().fluidInputSource().back() == 2,
                "the four cells are kept");
        prepare();
        GTMaterialRegistry.init();
        var steel = GTMaterialRegistry.get("Steel");
        expectThrows(IllegalArgumentException.class, () -> BasicMachineParameters.builder("press", steel)
                .energy(GregTechTags.Energy.EU, 32)
                .faces(MachineFaceMasks.builder().itemAutoOut(MachineFaceMasks.BOTTOM).build())
                .itemOutputTarget(IoTarget.at(1, 0, 0, MachineFace.RIGHT))
                .build(), "conflicts");
        var bare = BasicMachineParameters.builder("plain_press", steel).machineType("sampleplain")
                .energy(GregTechTags.Energy.EU, 512).build();
        check(bare.itemOutputTarget() == null && bare.fluidOutputTarget() == null
                && bare.itemInputSource() == null && bare.fluidInputSource() == null
                && bare.faceConfig().itemAutoOutput() == -1 && bare.faceConfig().fluidAutoOutput() == -1,
                "no target leaves the previous face mask");
        var item = IoTarget.at(2, 0, 1, MachineFace.RIGHT);
        var fluid = IoTarget.at(-2, 0, 1, MachineFace.LEFT);
        var itemIn = IoTarget.at(0, 2, 1, MachineFace.BOTTOM);
        var fluidIn = IoTarget.at(0, 1, 2, MachineFace.FRONT);
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

    private static void energyRange() throws Exception {
        var casing = MachineCasing.builder("press", "Steel").build();
        var unset = RecipeMachine.builder(casing).energy(EnergyInput.builder("EU", 32).build())
                .recipeMap("aa.recipe.press").basic().build();
        check(!unset.energy().rangeSpecified() && unset.energy().rangeMin() == 0 && unset.energy().rangeMax() == 0,
                "an unset range is 0..0");
        expectThrows(IllegalArgumentException.class, () -> EnergyInput.builder("EU", 32).range(0, 0).build(),
                "Invalid energy range");
        var outside = RecipeMachine.builder(casing)
                .energy(EnergyInput.builder("EU", 32).range(64, 256).build())
                .recipeMap("aa.recipe.press").build();
        check(outside.energy().rangeSpecified() && outside.energy().perTick() == 32
                && outside.energy().rangeMin() == 64 && outside.energy().rangeMax() == 256,
                "nominal may sit outside the range");
        check(RecipeMachine.builder(casing).energy(EnergyInput.builder("EU", 32).range(1, 1).build())
                .recipeMap("aa.recipe.press").build().energy().rangeMax() == 1,
                "a single accepted size is allowed");
        expectThrows(IllegalArgumentException.class, () -> EnergyInput.builder("EU", 32).range(8, 4).build(), "Invalid energy range");
        expectThrows(IllegalArgumentException.class, () -> EnergyInput.builder("EU", 32).range(0, 8).build(), "Invalid energy range");
        expectThrows(IllegalArgumentException.class, () -> EnergyInput.builder("EU", 32).range(8, 0).build(), "Invalid energy range");
        expectThrows(IllegalArgumentException.class, () -> RecipeMachine.builder(MachineCasing.builder("time_press", "Steel").build())
                .energy(EnergyInput.builder("TU", 1).range(1, 16).build()).recipeMap("aa.recipe.press").build(), "TU", "energy range");
        expectThrows(IllegalArgumentException.class, () -> RecipeMachine.builder(MachineCasing.builder("time_press", "Steel").build())
                .energy(EnergyInput.builder("tu", 1).range(1, 32).build()).recipeMap("aa.recipe.press").largeRecipe().build(),
                "TU", "energy range");
        prepare();
        GTMaterialRegistry.init();
        var steel = GTMaterialRegistry.get("Steel");
        var basic = BasicMachineParameters.builder("plain_press", steel).machineType("sampleplain")
                .energy(GregTechTags.Energy.EU, 32).build();
        check(!basic.energyRangeSpecified() && basic.energyInMin() == 16 && basic.energyInMax() == 64,
                "an undeclared single block is half to double");
        check(basic.acceptedEnergyInMin(false) == 16 && basic.acceptedEnergyInMax(false) == 64,
                "accepted single block range matches the stored default");
        var timed = BasicMachineParameters.builder("time_press", steel).machineType("sampletime")
                .energy(GregTechTags.Energy.TU, 1).build();
        check(!timed.energyRangeSpecified() && timed.energyInMin() == 1 && timed.energyInMax() == 16,
                "an undeclared TU single block stays 1..16");
        var large = BasicMachineParameters.builder("large_press", steel).machineType("largesamplepress")
                .energy(GregTechTags.Energy.EU, 512).omitEnergyRange().build();
        check(!large.energyRangeSpecified() && large.energyInMin() == 0 && large.energyInMax() == 0,
                "an undeclared large machine records no range");
        check(large.acceptedEnergyInMin(true) == 512 && large.acceptedEnergyInMax(true) == 4096,
                "an undeclared large EU machine accepts 512..4096");
        var heat = BasicMachineParameters.builder("large_heat", steel).machineType("largeheat")
                .energy(GregTechTags.Energy.HU, 512).omitEnergyRange().build();
        check(heat.acceptedEnergyInMin(true) == 1 && heat.acceptedEnergyInMax(true) == 4096,
                "an undeclared large HU machine accepts 1..4096");
        var largeTime = BasicMachineParameters.builder("large_time", steel).machineType("largetime")
                .energy(GregTechTags.Energy.TU, 1).omitEnergyRange().build();
        check(largeTime.acceptedEnergyInMin(true) == 1 && largeTime.acceptedEnergyInMax(true) == 16,
                "an undeclared large TU machine accepts 1..16");
        check(large.acceptedEnergyInMin(false) == BasicMachineParameters.defaultEnergyInMin("EU", 512)
                && large.acceptedEnergyInMax(false) == BasicMachineParameters.defaultEnergyInMax("EU", 512),
                "the same unset numbers read as a single block are half to double");
        var declared = BasicMachineParameters.builder("wide_press", steel).machineType("samplewide")
                .energy(GregTechTags.Energy.EU, 32).energyRange(64, 256).build();
        check(declared.energyRangeSpecified() && declared.energyIn() == 32
                && declared.acceptedEnergyInMin(true) == 64 && declared.acceptedEnergyInMax(true) == 256,
                "a declared range replaces the large rule");
        expectThrows(IllegalArgumentException.class, () -> BasicMachineParameters.builder("bad_press", steel)
                .energy(GregTechTags.Energy.EU, 32).energyRange(8, 4).build(), "Invalid energy range");
        loadMachine(RangeContributor.class);
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        var byId = new java.util.HashMap<String, BasicMachineParameters>();
        for (var machine : AddonMachines.machines())
            byId.put(machine.definition().parameters().id(), machine.definition().parameters());
        var slow = byId.get("press_slow");
        check(slow != null && !slow.energyRangeSpecified() && slow.energyInMin() == 16 && slow.energyInMax() == 64,
                "installed single block keeps half to double");
        var fast = byId.get("press_fast");
        check(fast != null && !fast.energyRangeSpecified() && fast.energyInMin() == 32 && fast.energyInMax() == 128,
                "a second tier with no declaration keeps its own half to double");
        var time = byId.get("time_press");
        check(time != null && !time.energyRangeSpecified() && time.energyInMin() == 1 && time.energyInMax() == 16,
                "installed TU single block stays 1..16");
        var installedLarge = byId.get("large_press");
        check(installedLarge != null && !installedLarge.energyRangeSpecified()
                && installedLarge.energyInMin() == 0 && installedLarge.energyInMax() == 0
                && installedLarge.acceptedEnergyInMin(true) == LargeMachineProcessingRules.inputMinimum(false, false)
                && installedLarge.acceptedEnergyInMax(true) == LargeMachineProcessingRules.inputMaximum(false),
                "installed large EU machine stays on the processing rule");
        var installedHeat = byId.get("large_heat");
        check(installedHeat != null && installedHeat.acceptedEnergyInMin(true) == 1 && installedHeat.acceptedEnergyInMax(true) == 4096,
                "installed large HU machine stays on 1..4096");
        var wide = byId.get("large_wide");
        check(wide != null && wide.energyRangeSpecified() && wide.energyIn() == 512
                && wide.energyInMin() == 256 && wide.energyInMax() == 2048,
                "installed declaration replaces the large rule");
        var low = byId.get("large_low");
        var high = byId.get("large_high");
        check(low != null && high != null && low.energyRangeSpecified() && high.energyRangeSpecified()
                && low.energyInMin() == 64 && low.energyInMax() == 256
                && high.energyIn() == 256 && high.energyInMin() == 64 && high.energyInMax() == 256,
                "two nominals can share one declared range");
    }

    private static void energyRangeDisagree() throws Exception {
        prepare();
        GTMaterialRegistry.init();
        loadMachine(RangeDisagreeContributor.class);
        expectInstall("largesame", "disagrees");
    }

    private static void energyRangeTu() {
        prepare();
        GTMaterialRegistry.init();
        var timed = BasicMachineParameters.builder("time_press", GTMaterialRegistry.get("Steel"))
                .machineType("sampletime").energy(GregTechTags.Energy.TU, 1).energyRange(1, 32).build();
        AddonMachines.accept("aa_sample", List.of(map("aa.recipe.press")), List.of(
                new AddonMachineDefinition(timed, "aa.recipe.press", MachineControllerKind.BASIC, 10_000, true, List.of())),
                List.of());
        expectInstall("sampletime", "TU", "energy range");
    }

    private static void plainBlock() {
        expectThrows(IllegalArgumentException.class,
                () -> LayoutCell.at(1, 0, 0, CellMatch.block("minecraft:glass"), StructureRole.ENERGY_INPUT),
                "casing");
        for (String bad : new String[] {"Minecraft:Glass", "minecraft:", "#", "#c:", "mine craft:stone"}) {
            expectThrows(IllegalArgumentException.class,
                    () -> CellMatch.parse(bad),
                    "Invalid block name");
        }
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.accept("aa_sample", List.of(map("aa.recipe.press")),
                List.of(large("large_press", "largesamplepress", "aa.recipe.press")),
                List.of(new AddonMachineLayout("largesamplepress", List.of(
                        new AddonMachineLayout.Cell(1, 0, 0, "tank_wall", StructureGrid.Role.CASING),
                        new AddonMachineLayout.Cell(2, 0, 0, "minecraft:glass", StructureGrid.Role.CASING),
                        new AddonMachineLayout.Cell(3, 0, 0, "#c:glass_blocks", StructureGrid.Role.CASING),
                        new AddonMachineLayout.Cell(4, 0, 0, "minecraft:not_a_block", StructureGrid.Role.CASING)))));
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        var cells = SharedLargeMachineLayouts.cells("largesamplepress");
        check(cells != null && cells.size() == 4, "layout kept every cell");
        var wall = cells.get(0);
        var glass = cells.get(1);
        var tag = cells.get(2);
        check(wall.part() == 18002 && wall.plain() == null, "a part cell keeps its numeric id");
        check(glass.part() == 0 && "minecraft:glass".equals(glass.plain())
                && glass.role() == StructureGrid.Role.CASING, "a block id is stored without a part id");
        check(tag.part() == 0 && "#c:glass_blocks".equals(tag.plain()), "a tag is stored without a part id");
        expectThrows(IllegalStateException.class,
                () -> AddonMachines.requireKnownBlocks(name -> name.equals("minecraft:glass")),
                "aa_sample", "largesamplepress", "minecraft:not_a_block");
    }

    private static void plainRole() {
        prepare();
        GTMaterialRegistry.init();
        expectThrows(IllegalStateException.class, () -> AddonMachines.accept("aa_sample", List.of(map("aa.recipe.press")),
                List.of(large("large_press", "largesamplepress", "aa.recipe.press")),
                List.of(new AddonMachineLayout("largesamplepress", List.of(
                        new AddonMachineLayout.Cell(1, 0, 0, "minecraft:glass", StructureGrid.Role.ENERGY_INPUT))))),
                "aa_sample", "largesamplepress", "casing");
    }

    private static StructureLayout wall(String name) {
        return StructureLayout.builder(name)
                .cell(1, 0, 0, CellMatch.part("tank_wall"), StructureRole.CASING).build();
    }

    private static RecipeTable pressMap() {
        return RecipeTable.machine("aa.recipe.press").title("Sample").gui("aa_sample:textures/gui/machines/press")
                .inputItems(1).outputItems(1).minimalInputItems(1).minimalInputs(1).needsOutputs(true).build();
    }

    private static void loadMachine(Class<?> contributor) {
        try {
            Path root = Files.createTempDirectory("gt-addon-machines");
            Path service = root.resolve("META-INF/services/com.gregtech.gregtech.api.addon.AddonMachineContributor");
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

    private static void behaviorCatalyst() {
        var index = new java.util.LinkedHashMap<String, List<String>>();
        var compressor = new java.util.ArrayList<>(List.of("centrifuge"));
        index.put("gt.recipe.compressor", compressor);
        BehaviorRecipeCatalysts.add(index, List.of(), "boiler", name -> name, Object::equals);
        check(index.size() == 1 && index.get("gt.recipe.compressor") == compressor,
                "an empty name list leaves the catalyst index unchanged");
        BehaviorRecipeCatalysts.add(index, List.of("missing"), "boiler", name -> null, Object::equals);
        check(index.size() == 1 && index.get("gt.recipe.compressor") == compressor,
                "an unresolved map adds no catalyst");
        BehaviorRecipeCatalysts.add(index, List.of("aa.fuel.solid", "gt.recipe.fuels.engine"), "boiler",
                name -> name, Object::equals);
        check(index.get("gt.recipe.compressor") == compressor, "existing catalysts stay in place");
        check(List.of("boiler").equals(index.get("aa.fuel.solid")), "the behavior machine is a catalyst");
        check(List.of("boiler").equals(index.get("gt.recipe.fuels.engine")), "a second map is listed");
        BehaviorRecipeCatalysts.add(index, List.of("aa.fuel.solid"), "boiler", name -> name, Object::equals);
        check(index.get("aa.fuel.solid").size() == 1, "the same machine is not listed twice");

        var plain = BehaviorMachine.builder(MachineCasing.builder("irrigator", "Steel").build())
                .energy(EnergyInput.builder("EU", 32).build()).gui("aa_sample:textures/gui/machine.png").build();
        check(plain.recipeMaps().isEmpty(), "recipe maps default to empty");
        expectThrows(IllegalArgumentException.class, () -> BehaviorMachine.builder(MachineCasing.builder("bad_press", "Steel").build())
                .energy(EnergyInput.builder("EU", 32).build()).gui("aa_sample:textures/gui/machine.png")
                .recipeMaps("aa.fuel.solid", "aa.fuel.solid").build(), "repeats");
        expectThrows(IllegalArgumentException.class, () -> BehaviorMachine.builder(MachineCasing.builder("bad_press", "Steel").build())
                .energy(EnergyInput.builder("EU", 32).build()).gui("aa_sample:textures/gui/machine.png")
                .recipeMaps("Not A Map").build(), "Invalid recipe map");

        prepare();
        GTMaterialRegistry.init();
        BehaviorMachineFactories.collect(Map.of("aa_sample", Set.of("boiler")));
        AddonMachines.accept("aa_sample", List.of(map("aa.recipe.press")), List.of(), List.of(),
                List.of(boiler("aa.recipe.press", "gt.recipe.fuels.engine", "aa.fuel.solid")),
                List.of(fuelMap("aa.fuel.solid")), List.of());
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        check(AddonMachines.behaviorMachines().size() == 1, "the boiler is accepted");
        check(List.of("aa.recipe.press", "gt.recipe.fuels.engine", "aa.fuel.solid")
                        .equals(AddonMachines.behaviorMachines().get(0).declaration().recipeMaps()),
                "own maps and a built-in fuel map are kept");
    }

    private static void unknownBehaviorMap() {
        prepare();
        GTMaterialRegistry.init();
        BehaviorMachineFactories.collect(Map.of("aa_sample", Set.of("boiler")));
        AddonMachines.accept("aa_sample", List.of(), List.of(), List.of(), List.of(boiler("aa.fuel.missing")));
        expectInstall("aa_sample", "boiler", "unknown", "aa.fuel.missing");
    }

    private static void foreignBehaviorMap() {
        prepare();
        GTMaterialRegistry.init();
        BehaviorMachineFactories.collect(Map.of("aa_two", Set.of("boiler")));
        AddonMachines.accept("aa_one", List.of(), List.of(), List.of(), List.of(),
                List.of(fuelMap("aa.fuel.solid")), List.of());
        AddonMachines.accept("aa_two", List.of(), List.of(), List.of(), List.of(boiler("aa.fuel.solid")));
        expectInstall("aa_two", "boiler", "unknown", "aa.fuel.solid");
    }

    private static BehaviorMachine boiler(String... maps) {
        return BehaviorMachine.builder(MachineCasing.builder("boiler", "Steel").machineType("boiler").build())
                .energy(EnergyInput.builder("EU", 32).build()).gui("aa_sample:textures/gui/machine.png").recipeMaps(maps).build();
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

    public static final class RangeContributor implements AddonMachineContributor {
        @Override public String addonId() { return "aa_sample"; }
        @Override public List<RecipeTable> recipeTables() { return List.of(pressMap()); }
        @Override public List<AddonMachine> addonMachines() {
            return List.of(
                    RecipeMachine.builder(MachineCasing.builder("press_slow", "Steel").machineType("samplepress").build())
                            .energy(EnergyInput.builder("EU", 32).build()).recipeMap("aa.recipe.press").basic().build(),
                    RecipeMachine.builder(MachineCasing.builder("press_fast", "Steel").machineType("samplepress").build())
                            .energy(EnergyInput.builder("EU", 64).build()).recipeMap("aa.recipe.press").basic().build(),
                    RecipeMachine.builder(MachineCasing.builder("time_press", "Steel").machineType("sampletime").build())
                            .energy(EnergyInput.builder("TU", 1).build()).recipeMap("aa.recipe.press").basic().build(),
                    RecipeMachine.builder(MachineCasing.builder("large_press", "Steel").machineType("largesamplepress").build())
                            .energy(EnergyInput.builder("EU", 512).build()).recipeMap("aa.recipe.press").largeRecipe().build(),
                    RecipeMachine.builder(MachineCasing.builder("large_heat", "Steel").machineType("largeheat").build())
                            .energy(EnergyInput.builder("HU", 512).build()).recipeMap("aa.recipe.press").largeRecipe().build(),
                    RecipeMachine.builder(MachineCasing.builder("large_wide", "Steel").machineType("largewide").build())
                            .energy(EnergyInput.builder("EU", 512).range(256, 2048).build()).recipeMap("aa.recipe.press").largeRecipe().build(),
                    RecipeMachine.builder(MachineCasing.builder("large_low", "Steel").machineType("largerange").build())
                            .energy(EnergyInput.builder("EU", 128).range(64, 256).build()).recipeMap("aa.recipe.press").largeRecipe().build(),
                    RecipeMachine.builder(MachineCasing.builder("large_high", "Steel").machineType("largerange").build())
                            .energy(EnergyInput.builder("EU", 256).range(64, 256).build()).recipeMap("aa.recipe.press").largeRecipe().build());
        }
        @Override public List<StructureLayout> structureLayouts() {
            return List.of(wall("largesamplepress"), wall("largeheat"), wall("largewide"), wall("largerange"));
        }
    }

    public static final class RangeDisagreeContributor implements AddonMachineContributor {
        @Override public String addonId() { return "aa_sample"; }
        @Override public List<RecipeTable> recipeTables() { return List.of(pressMap()); }
        @Override public List<AddonMachine> addonMachines() {
            return List.of(
                    RecipeMachine.builder(MachineCasing.builder("large_a", "Steel").machineType("largesame").build())
                            .energy(EnergyInput.builder("EU", 512).range(256, 2048).build())
                            .recipeMap("aa.recipe.press").largeRecipe().build(),
                    RecipeMachine.builder(MachineCasing.builder("large_b", "Steel").machineType("largesame").build())
                            .energy(EnergyInput.builder("EU", 512).build())
                            .recipeMap("aa.recipe.press").largeRecipe().build());
        }
        @Override public List<StructureLayout> structureLayouts() { return List.of(wall("largesame")); }
    }
}
