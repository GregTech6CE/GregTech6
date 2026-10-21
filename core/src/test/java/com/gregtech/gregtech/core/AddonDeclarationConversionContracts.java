package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.addon.AddonMachine;
import com.gregtech.gregtech.api.addon.AddonMachineContributor;
import com.gregtech.gregtech.api.addon.AddonMachineDefinition;
import com.gregtech.gregtech.api.addon.AddonMachines;
import com.gregtech.gregtech.api.addon.BehaviorMachine;
import com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration;
import com.gregtech.gregtech.api.addon.BehaviorMachineFactories;
import com.gregtech.gregtech.api.addon.CellMatch;
import com.gregtech.gregtech.api.addon.EnergyInput;
import com.gregtech.gregtech.api.addon.EnergyOutput;
import com.gregtech.gregtech.api.addon.FuelGenerator;
import com.gregtech.gregtech.api.addon.FuelGeneratorDeclaration;
import com.gregtech.gregtech.api.addon.FuelMapDeclaration;
import com.gregtech.gregtech.api.addon.FuelOutput;
import com.gregtech.gregtech.api.addon.LayoutPart;
import com.gregtech.gregtech.api.addon.MachineCasing;
import com.gregtech.gregtech.api.addon.MachineDeclaration;
import com.gregtech.gregtech.api.addon.MachineFace;
import com.gregtech.gregtech.api.addon.MachineIo;
import com.gregtech.gregtech.api.addon.MachineLayout;
import com.gregtech.gregtech.api.addon.RecipeMachine;
import com.gregtech.gregtech.api.addon.RecipeMapDeclaration;
import com.gregtech.gregtech.api.addon.RecipeTable;
import com.gregtech.gregtech.api.addon.StructureLayout;
import com.gregtech.gregtech.api.addon.StructurePartDeclaration;
import com.gregtech.gregtech.api.addon.StructurePartEnergy;
import com.gregtech.gregtech.api.addon.StructureRole;
import com.gregtech.gregtech.api.machine.BasicMachineParameters;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.api.multiblock.StructureGrid;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.api.recipe.RecipeMapSpec;
import com.gregtech.gregtech.content.multiblock.SharedLargeMachineLayouts;
import com.gregtech.gregtech.data.ModReferences;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Old sample declarations convert to the same objects as a handwritten new declaration,
 * and the installed rows keep those fields.
 */
public final class AddonDeclarationConversionContracts {
    private static int assertions;

    private AddonDeclarationConversionContracts() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Choose one scenario");
        switch (args[0]) {
            case "equal" -> equal();
            case "installed" -> installed();
            case "mix" -> mix();
            case "disagree" -> disagree();
            default -> throw new IllegalArgumentException("Unknown scenario: " + args[0]);
        }
        System.out.println(args[0] + " passed " + assertions);
    }

    private static void equal() {
        prepare();
        GTMaterialRegistry.init();
        check(AddonMachines.recipeMachine(press()).equals(handPress()), "sample press converts to the handwritten machine");
        check(AddonMachines.definition(AddonMachines.recipeMachine(press()))
                        .equals(AddonMachines.definition(handPress())),
                "converted and handwritten presses install as the same definition");
        check(AddonMachines.recipeMachine(detector()).equals(handDetector()), "sample detector converts");
        check(AddonMachines.recipeMachine(largePress()).equals(handLarge()), "sample large press converts");
        check(!AddonMachines.recipeMachine(largePress()).energy().facesSpecified()
                        && AddonMachines.recipeMachine(largePress()).io() != null
                        && AddonMachines.recipeMachine(largePress()).io().hasTarget(),
                "targets keep energy faces unspecified");
        check(AddonMachines.behaviorMachine(irrigator()).equals(handIrrigator()), "sample irrigator converts");
        check(!AddonMachines.behaviorMachine(irrigator()).energy().facesSpecified(),
                "the default five input faces stay unspecified");
        check(AddonMachines.fuelGenerator(engine()).equals(handEngine()), "sample engine converts");
        check(AddonMachines.fuelGenerator(turbine()).equals(handTurbine()), "sample turbine converts");
        check(AddonMachines.layoutPart(wall()).equals(handWall()), "sample casing converts");
        check(AddonMachines.layoutPart(emitter()).equals(handEmitter()), "sample energy part converts");
        check(AddonMachines.structureLayout(turbineLayout()).equals(handTurbineLayout()), "sample turbine layout converts");
        check(AddonMachines.recipeTable(pressMap()).equals(handPressTable()), "sample recipe table converts");
        check(AddonMachines.fuelTable(gasMap()).equals(handGasTable()), "sample fuel table converts");
        check(!AddonMachines.recipeTable(pressMap()).equals(AddonMachines.fuelTable(gasAsMachineTable())),
                "a fuel table is not a machine table");

        var called = AddonMachines.recipeMachine(facesWithoutEnergy());
        var omitted = AddonMachines.recipeMachine(facesOmitted());
        check(called.energy().facesSpecified() && called.energy().faces().isEmpty(),
                "a face call without energy inputs stores an empty energy list");
        check(!omitted.energy().facesSpecified() && omitted.energy().faces().equals(MachineFace.all()),
                "omitted faces stay unspecified and resolve to six faces");
        check(!called.equals(omitted), "an empty energy list is not the six-face default");
        check(called.io() != null && called.io().itemInputs().equals(List.of(MachineFace.TOP)),
                "the called item face is kept");

        var cheapOff = AddonMachines.recipeMachine(cheap(false));
        var cheapUnset = AddonMachines.recipeMachine(pressBare());
        check(cheapOff.cheapOverclockingSpecified() && !cheapOff.cheapOverclocking(), "an explicit off is recorded");
        check(!cheapUnset.cheapOverclockingSpecified() && !cheapUnset.cheapOverclocking(), "an omitted cheap flag stays unset");
        check(!cheapOff.equals(cheapUnset), "calling cheap overclock false is not the same as leaving it unset");
        check(!AddonMachines.definition(cheapOff).equals(AddonMachines.definition(cheapUnset)),
                "the installed definitions keep the cheap-overclock flag apart");

        long half = BasicMachineParameters.defaultEnergyInMin("EU", 32);
        long doubled = BasicMachineParameters.defaultEnergyInMax("EU", 32);
        var namedRange = AddonMachines.recipeMachine(ranged(half, doubled));
        var openRange = AddonMachines.recipeMachine(largeBare());
        check(namedRange.energy().rangeSpecified() && namedRange.energy().rangeMin() == half
                        && namedRange.energy().rangeMax() == doubled,
                "an explicit half-to-double range is stored");
        check(!openRange.energy().rangeSpecified() && openRange.energy().rangeMin() == 0
                        && openRange.energy().rangeMax() == 0,
                "an omitted large range stays unset");
        check(!namedRange.equals(openRange), "naming the default numbers is not the same as leaving the range unset");
        check(!AddonMachines.definition(namedRange).equals(AddonMachines.definition(openRange)),
                "the installed definitions keep the range flag apart");

        var repeated = AddonMachines.recipeMachine(repeatedTop());
        var once = AddonMachines.recipeMachine(facesWithoutEnergy());
        check(repeated.equals(once), "a repeated item face converts to one face");
        check(repeated.energy().facesSpecified() && repeated.energy().faces().isEmpty(),
                "dedupe still leaves the unnamed energy list empty");
    }

    private static void installed() throws Exception {
        prepare();
        GTMaterialRegistry.init();
        BehaviorMachineFactories.collect(Map.of("aa_sample", Set.of("irrigator")));
        load(InstalledContributor.class);
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        var press = machine("sample_press");
        var large = machine("large_sample_press");
        check(press.equals(AddonMachines.definition(handPress())), "installed press matches the handwritten definition");
        check(large.equals(AddonMachines.definition(handLarge())), "installed large press matches the handwritten definition");
        check(AddonMachines.behaviorMachines().get(0).declaration().equals(handIrrigator()),
                "installed irrigator matches the handwritten machine");
        check(AddonMachines.fuelGenerators().get(0).declaration().equals(handEngine()),
                "installed engine matches the handwritten generator");
        check(AddonMachines.fuelGenerators().get(1).declaration().equals(handTurbine()),
                "installed turbine matches the handwritten generator");
        check(AddonMachines.structureParts().get(0).declaration().equals(handWall()),
                "installed casing matches the handwritten part");
        check(AddonMachines.structureParts().get(1).declaration().equals(handEmitter()),
                "installed energy part matches the handwritten part");
        sameSpec(AddonMachines.recipeMaps().get(0).spec(), AddonMachines.spec(handPressTable()),
                "installed recipe table matches");
        sameSpec(AddonMachines.fuelMaps().get(0).spec(), AddonMachines.spec(handGasTable()),
                "installed fuel table matches");
        var turbine = SharedLargeMachineLayouts.cells("sample_turbine");
        check(turbine != null && turbine.size() == 1 && turbine.get(0).right() == 0 && turbine.get(0).up() == 0
                        && turbine.get(0).back() == 1 && turbine.get(0).part() == 18002
                        && turbine.get(0).role() == StructureGrid.Role.FLUID_INPUT && turbine.get(0).plain() == null,
                "installed turbine cell matches the handwritten layout");
        var pressLayout = SharedLargeMachineLayouts.cells("largesamplepress");
        check(pressLayout != null && pressLayout.size() == 2
                        && pressLayout.get(0).part() == 18002 && pressLayout.get(0).plain() == null
                        && pressLayout.get(1).part() == 0 && "minecraft:glass".equals(pressLayout.get(1).plain())
                        && pressLayout.get(1).role() == StructureGrid.Role.CASING,
                "installed large layout keeps the part and the plain block");
    }

    private static void mix() throws Exception {
        prepare();
        GTMaterialRegistry.init();
        load(MixContributor.class);
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        check(AddonMachines.machines().size() == 2, "old and new recipe machines share the queue");
        check(AddonMachines.recipeMaps().size() == 1 && AddonMachines.fuelMaps().size() == 1,
                "a recipe table and a fuel table stay in their own lists");
        check(AddonMachines.structureParts().size() == 2, "old and new parts share the name check");
        check("samplepress".equals(AddonMachines.machines().get(0).definition().parameters().machineName())
                        && "samplefast".equals(AddonMachines.machines().get(1).definition().parameters().machineName()),
                "both machine types were installed");
    }

    private static void disagree() throws Exception {
        prepare();
        GTMaterialRegistry.init();
        load(DisagreeContributor.class);
        expectInstall("samplepress", "disagrees");
    }

    private static AddonMachineDefinition machine(String id) {
        for (var accepted : AddonMachines.machines())
            if (id.equals(accepted.definition().parameters().id())) return accepted.definition();
        throw new AssertionError(id);
    }

    private static MachineDeclaration press() {
        return MachineDeclaration.builder("sample_press", "Steel")
                .machineType("samplepress").energy("EU", 32).tier(1)
                .recipeMap("addon_sample.recipe.press").basic()
                .itemInputs(BehaviorMachineDeclaration.Face.TOP)
                .itemOutputs(BehaviorMachineDeclaration.Face.BOTTOM)
                .fluidInputs(BehaviorMachineDeclaration.Face.LEFT)
                .fluidOutputs(BehaviorMachineDeclaration.Face.RIGHT)
                .energyInputs(BehaviorMachineDeclaration.Face.BACK)
                .itemAutoOutput(BehaviorMachineDeclaration.Face.BOTTOM)
                .cheapOverclocking(true)
                .minimumLightAbove(14)
                .build();
    }

    private static RecipeMachine handPress() {
        var casing = MachineCasing.builder("sample_press", "Steel").machineType("samplepress").tier(1).build();
        var energy = EnergyInput.builder("EU", 32).faces(MachineFace.BACK).build();
        var io = MachineIo.builder()
                .itemInputs(MachineFace.TOP).itemOutputs(MachineFace.BOTTOM)
                .fluidInputs(MachineFace.LEFT).fluidOutputs(MachineFace.RIGHT)
                .itemAutoOutput(MachineFace.BOTTOM).build();
        return RecipeMachine.builder(casing).energy(energy).io(io)
                .recipeMap("addon_sample.recipe.press").cheapOverclocking(true).minimumLightAbove(14).build();
    }

    private static MachineDeclaration detector() {
        return MachineDeclaration.builder("sample_detector", "Steel")
                .machineType("sampledetector").energy("EU", 32).tier(1)
                .recipeMap("addon_sample.recipe.detector").basic()
                .searchOutputTanks(true).exclusiveOutputs(true).build();
    }

    private static RecipeMachine handDetector() {
        var casing = MachineCasing.builder("sample_detector", "Steel").machineType("sampledetector").tier(1).build();
        return RecipeMachine.builder(casing).energy(EnergyInput.builder("EU", 32).build())
                .recipeMap("addon_sample.recipe.detector").searchOutputTanks(true).exclusiveOutputs(true).build();
    }

    private static MachineDeclaration largePress() {
        return MachineDeclaration.builder("large_sample_press", "Steel")
                .machineType("largesamplepress").energy("EU", 512).tier(1)
                .recipeMap("addon_sample.recipe.large_press").largeRecipe()
                .energyRange(256, 2048)
                .structureKeys(List.of("addon_sample.structure.press.1"))
                .itemOutputTarget(2, 0, 1, BehaviorMachineDeclaration.Face.RIGHT)
                .fluidOutputTarget(-2, 0, 1, BehaviorMachineDeclaration.Face.LEFT)
                .build();
    }

    private static RecipeMachine handLarge() {
        var casing = MachineCasing.builder("large_sample_press", "Steel").machineType("largesamplepress").tier(1).build();
        var all = MachineFace.all().toArray(MachineFace[]::new);
        var io = MachineIo.builder().itemInputs(all).itemOutputs(all).fluidInputs(all).fluidOutputs(all)
                .itemOutputTarget(2, 0, 1, MachineFace.RIGHT)
                .fluidOutputTarget(-2, 0, 1, MachineFace.LEFT).build();
        return RecipeMachine.builder(casing)
                .energy(EnergyInput.builder("EU", 512).range(256, 2048).build())
                .io(io).largeRecipe().recipeMap("addon_sample.recipe.large_press")
                .structureKeys(List.of("addon_sample.structure.press.1")).build();
    }

    private static MachineDeclaration facesWithoutEnergy() {
        return MachineDeclaration.builder("sample_open", "Steel").machineType("sampleopen")
                .energy("EU", 32).recipeMap("addon_sample.recipe.press").basic()
                .itemInputs(BehaviorMachineDeclaration.Face.TOP).build();
    }

    private static MachineDeclaration facesOmitted() {
        return MachineDeclaration.builder("sample_open", "Steel").machineType("sampleopen")
                .energy("EU", 32).recipeMap("addon_sample.recipe.press").basic().build();
    }

    private static MachineDeclaration cheap(boolean value) {
        return MachineDeclaration.builder("sample_cheap", "Steel").machineType("samplecheap")
                .energy("EU", 32).recipeMap("addon_sample.recipe.press").basic()
                .cheapOverclocking(value).build();
    }

    private static MachineDeclaration pressBare() {
        return MachineDeclaration.builder("sample_cheap", "Steel").machineType("samplecheap")
                .energy("EU", 32).recipeMap("addon_sample.recipe.press").basic().build();
    }

    private static MachineDeclaration ranged(long min, long max) {
        return MachineDeclaration.builder("sample_range", "Steel").machineType("samplerange")
                .energy("EU", 32).recipeMap("addon_sample.recipe.press").largeRecipe()
                .energyRange(min, max).build();
    }

    private static MachineDeclaration largeBare() {
        return MachineDeclaration.builder("sample_range", "Steel").machineType("samplerange")
                .energy("EU", 32).recipeMap("addon_sample.recipe.press").largeRecipe().build();
    }

    private static MachineDeclaration repeatedTop() {
        return MachineDeclaration.builder("sample_open", "Steel").machineType("sampleopen")
                .energy("EU", 32).recipeMap("addon_sample.recipe.press").basic()
                .itemInputs(BehaviorMachineDeclaration.Face.TOP, BehaviorMachineDeclaration.Face.TOP).build();
    }

    private static BehaviorMachineDeclaration irrigator() {
        return BehaviorMachineDeclaration.builder("irrigator", "Steel")
                .machineType("irrigator").energy("EU", 32).tier(1)
                .gui("addon_sample:textures/gui/machine.png")
                .item(44, 26, BehaviorMachineDeclaration.SlotRole.INPUT)
                .item(116, 26, BehaviorMachineDeclaration.SlotRole.OUTPUT)
                .fluid(8000, 152, 8, BehaviorMachineDeclaration.SlotRole.INPUT)
                .toggle("addon_sample.irrigator.water", 26, 64, true).build();
    }

    private static BehaviorMachine handIrrigator() {
        var casing = MachineCasing.builder("irrigator", "Steel").machineType("irrigator").tier(1).build();
        return BehaviorMachine.builder(casing).energy(EnergyInput.builder("EU", 32).build())
                .gui("addon_sample:textures/gui/machine.png")
                .item(44, 26, BehaviorMachine.SlotRole.INPUT)
                .item(116, 26, BehaviorMachine.SlotRole.OUTPUT)
                .fluid(8000, 152, 8, BehaviorMachine.SlotRole.INPUT)
                .toggle("addon_sample.irrigator.water", 26, 64, true).build();
    }

    private static FuelGeneratorDeclaration engine() {
        return FuelGeneratorDeclaration.builder("sample_engine", "Steel").machineType("sample_engine")
                .fuelMap("addon_sample.fuel.gas").efficiency(10_000).energy("RU", 32).tank(4_000)
                .outputFront().single().build();
    }

    private static FuelGenerator handEngine() {
        var casing = MachineCasing.builder("sample_engine", "Steel").machineType("sample_engine").build();
        return FuelGenerator.builder(casing)
                .output(FuelOutput.builder("RU", 32).front().build())
                .fuelMap("addon_sample.fuel.gas").efficiency(10_000).tank(4_000).build();
    }

    private static FuelGeneratorDeclaration turbine() {
        return FuelGeneratorDeclaration.builder("sample_turbine", "Steel").machineType("sample_turbine")
                .fuelMap("addon_sample.fuel.gas").efficiency(5_000).energy("RU", 32).tank(8_000)
                .outputBehind(4).multi().build();
    }

    private static FuelGenerator handTurbine() {
        var casing = MachineCasing.builder("sample_turbine", "Steel").machineType("sample_turbine").build();
        return FuelGenerator.builder(casing)
                .output(FuelOutput.builder("RU", 32).behind(4).build())
                .fuelMap("addon_sample.fuel.gas").efficiency(5_000).tank(8_000).multi().build();
    }

    private static StructurePartDeclaration wall() {
        return new StructurePartDeclaration("sample_wall", "Steel", "", 6.0F);
    }

    private static LayoutPart handWall() {
        return LayoutPart.builder("sample_wall", "Steel").hardness(6.0F).build();
    }

    private static StructurePartDeclaration emitter() {
        return new StructurePartDeclaration("sample_emitter", "Steel", "", 6.0F,
                new StructurePartEnergy("EU", 100_000L, List.of(BehaviorMachineDeclaration.Face.FRONT), 32, 4));
    }

    private static LayoutPart handEmitter() {
        return LayoutPart.builder("sample_emitter", "Steel").hardness(6.0F)
                .energyOutput(EnergyOutput.builder("EU", 32, 4).faces(MachineFace.FRONT).build(), 100_000L).build();
    }

    private static MachineLayout turbineLayout() {
        return new MachineLayout("sample_turbine", List.of(
                new MachineLayout.Cell(0, 0, 1, "tank_wall", StructureRole.FLUID_INPUT)));
    }

    private static StructureLayout handTurbineLayout() {
        return StructureLayout.builder("sample_turbine")
                .cell(0, 0, 1, CellMatch.part("tank_wall"), StructureRole.FLUID_INPUT).build();
    }

    private static RecipeMapDeclaration pressMap() {
        return new RecipeMapDeclaration("addon_sample.recipe.press", "Sample Press",
                "addon_sample:textures/gui/machines/press",
                2, 1, 1, 0, 0, 0, 1, true, false, false, false);
    }

    private static RecipeTable handPressTable() {
        return RecipeTable.machine("addon_sample.recipe.press").title("Sample Press")
                .gui("addon_sample:textures/gui/machines/press")
                .inputItems(2).outputItems(1).minimalInputItems(1)
                .inputFluids(0).outputFluids(0).minimalInputFluids(0).minimalInputs(1)
                .needsOutputs(true).combinePower(false).bucketIn(false).bucketOut(false).build();
    }

    private static FuelMapDeclaration gasMap() {
        return new FuelMapDeclaration("addon_sample.fuel.gas", "Sample Gas",
                "addon_sample:textures/gui/machines/press",
                0, 0, 0, 1, 1, 1, 1, false, false, false, false);
    }

    private static FuelMapDeclaration gasAsMachineTable() {
        return new FuelMapDeclaration("addon_sample.recipe.press", "Sample Press",
                "addon_sample:textures/gui/machines/press",
                2, 1, 1, 0, 0, 0, 1, true, false, false, false);
    }

    private static RecipeTable handGasTable() {
        return RecipeTable.fuel("addon_sample.fuel.gas").title("Sample Gas")
                .gui("addon_sample:textures/gui/machines/press")
                .inputItems(0).outputItems(0).minimalInputItems(0)
                .inputFluids(1).outputFluids(1).minimalInputFluids(1).minimalInputs(1)
                .needsOutputs(false).combinePower(false).bucketIn(false).bucketOut(false).build();
    }

    private static void sameSpec(RecipeMapSpec left, RecipeMapSpec right, String message) {
        check(left.mNameInternal.equals(right.mNameInternal) && left.mNameLocal.equals(right.mNameLocal)
                        && left.mGUIPath.equals(right.mGUIPath)
                        && left.mInputItemsCount == right.mInputItemsCount
                        && left.mOutputItemsCount == right.mOutputItemsCount
                        && left.mMinimalInputItems == right.mMinimalInputItems
                        && left.mInputFluidCount == right.mInputFluidCount
                        && left.mOutputFluidCount == right.mOutputFluidCount
                        && left.mMinimalInputFluids == right.mMinimalInputFluids
                        && left.mMinimalInputs == right.mMinimalInputs
                        && left.mNeedsOutputs == right.mNeedsOutputs
                        && left.mCombinePower == right.mCombinePower
                        && left.mUseBucketSizeIn == right.mUseBucketSizeIn
                        && left.mUseBucketSizeOut == right.mUseBucketSizeOut,
                message);
    }

    private static void prepare() {
        ModData.bindPresence(id -> true);
        GTMaterialRegistry.setLogSink((warning, message) -> {});
        PrefixRegistry.ensurePrefixesLoaded();
        ModReferences.UNKNOWN.getClass();
        com.gregtech.gregtech.data.MaterialGroups.Glowstone.getClass();
    }

    private static void load(Class<?> contributor) throws Exception {
        Path root = Files.createTempDirectory("gt-declaration-conversion");
        Path service = root.resolve("META-INF/services/com.gregtech.gregtech.api.addon.AddonMachineContributor");
        Files.createDirectories(service.getParent());
        Files.writeString(service, contributor.getName() + "\n", StandardCharsets.UTF_8);
        try (URLClassLoader loader = new URLClassLoader(new URL[]{root.toUri().toURL()},
                AddonDeclarationConversionContracts.class.getClassLoader())) {
            AddonMachines.collectContributors(loader);
        }
    }

    private static void expectInstall(String... parts) {
        assertions++;
        try {
            AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        } catch (Throwable failure) {
            if (!(failure instanceof IllegalStateException)) throw new AssertionError(String.join(" ", parts), failure);
            String text = failure.getMessage() == null ? "" : failure.getMessage();
            for (String part : parts)
                if (!text.contains(part)) throw new AssertionError(String.join(" ", parts) + " but was: " + text, failure);
            return;
        }
        throw new AssertionError(String.join(" ", parts));
    }

    private static void check(boolean valid, String message) {
        assertions++;
        if (!valid) throw new AssertionError(message);
    }

    public static final class InstalledContributor implements AddonMachineContributor {
        @Override public String addonId() { return "aa_sample"; }
        @Override public List<RecipeMapDeclaration> recipeMaps() {
            return List.of(pressMap(), new RecipeMapDeclaration("addon_sample.recipe.large_press", "Large Sample Press",
                    "addon_sample:textures/gui/machines/large_press",
                    1, 6, 1, 0, 1, 0, 1, true, false, false, false));
        }
        @Override public List<MachineDeclaration> machines() { return List.of(press(), largePress()); }
        @Override public List<MachineLayout> layouts() {
            return List.of(new MachineLayout("largesamplepress", List.of(
                    new MachineLayout.Cell(1, 0, 0, "tank_wall", StructureRole.CASING),
                    new MachineLayout.Cell(2, 0, 0, "minecraft:glass", StructureRole.CASING))), turbineLayout());
        }
        @Override public List<StructurePartDeclaration> parts() { return List.of(wall(), emitter()); }
        @Override public List<BehaviorMachineDeclaration> behaviorMachines() { return List.of(irrigator()); }
        @Override public List<FuelMapDeclaration> fuelMaps() { return List.of(gasMap()); }
        @Override public List<FuelGeneratorDeclaration> fuelGenerators() { return List.of(engine(), turbine()); }
    }

    public static final class MixContributor implements AddonMachineContributor {
        @Override public String addonId() { return "aa_mix"; }
        @Override public List<RecipeMapDeclaration> recipeMaps() { return List.of(pressMap()); }
        @Override public List<RecipeTable> recipeTables() { return List.of(handGasTable()); }
        @Override public List<MachineDeclaration> machines() { return List.of(press()); }
        @Override public List<AddonMachine> addonMachines() {
            var casing = MachineCasing.builder("sample_fast", "Steel").machineType("samplefast").tier(1).build();
            return List.of(RecipeMachine.builder(casing).energy(EnergyInput.builder("EU", 32).build())
                    .recipeMap("addon_sample.recipe.press").build());
        }
        @Override public List<StructurePartDeclaration> parts() { return List.of(wall()); }
        @Override public List<LayoutPart> layoutParts() { return List.of(handEmitter()); }
    }

    public static final class DisagreeContributor implements AddonMachineContributor {
        @Override public String addonId() { return "aa_mix"; }
        @Override public List<RecipeMapDeclaration> recipeMaps() { return List.of(pressMap()); }
        @Override public List<MachineDeclaration> machines() { return List.of(press()); }
        @Override public List<AddonMachine> addonMachines() {
            var casing = MachineCasing.builder("sample_press_2", "Steel").machineType("samplepress").tier(1).build();
            return List.of(RecipeMachine.builder(casing).energy(EnergyInput.builder("EU", 32).build())
                    .recipeMap("addon_sample.recipe.press").parallel(2).build());
        }
    }
}
