package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.addon.AddonMachineDefinition;
import com.gregtech.gregtech.api.addon.AddonMachineLayout;
import com.gregtech.gregtech.api.addon.AddonMachines;
import com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration;
import com.gregtech.gregtech.api.addon.BehaviorMachineFactories;
import com.gregtech.gregtech.api.addon.FuelGeneratorDeclaration;
import com.gregtech.gregtech.api.addon.GregTechMachineContributor;
import com.gregtech.gregtech.api.machine.BasicMachineParameters;
import com.gregtech.gregtech.api.machine.MachineControllerKind;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.api.multiblock.StructureGrid;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.api.recipe.RecipeMapSpec;
import com.gregtech.gregtech.content.multiblock.LargeMachineProcessingRules;
import com.gregtech.gregtech.content.multiblock.SharedLargeMachineLayouts;
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
            default -> throw new IllegalArgumentException("Unknown scenario: " + args[0]);
        }
        System.out.println(args[0] + " passed " + assertions);
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
        AddonMachines.accept("aa_sample", List.of(), List.of(),
                List.of(new AddonMachineLayout("irrigator", List.of(
                        new AddonMachineLayout.Cell(1, 0, 0, "tank_wall", StructureGrid.Role.CASING)))),
                List.of(irrigator("irrigator", 8000, BehaviorMachineDeclaration.SlotRole.INPUT, "", "EU")));
        expectInstall("aa_sample", "irrigator", "no", "controller");
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
