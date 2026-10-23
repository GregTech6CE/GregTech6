package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.addon.BehaviorEnergyRules;
import com.gregtech.gregtech.api.addon.BehaviorMachine;
import com.gregtech.gregtech.api.addon.CellMatch;
import com.gregtech.gregtech.api.addon.EnergyInput;
import com.gregtech.gregtech.api.addon.EnergyOutput;
import com.gregtech.gregtech.api.addon.FuelGenerator;
import com.gregtech.gregtech.api.addon.FuelOutput;
import com.gregtech.gregtech.api.addon.IoTarget;
import com.gregtech.gregtech.api.addon.LayoutCell;
import com.gregtech.gregtech.api.addon.LayoutPart;
import com.gregtech.gregtech.api.addon.MachineCasing;
import com.gregtech.gregtech.api.addon.MachineFace;
import com.gregtech.gregtech.api.addon.MachineIo;
import com.gregtech.gregtech.api.addon.PartEnergyRules;
import com.gregtech.gregtech.api.addon.PartSlotRules;
import com.gregtech.gregtech.api.addon.PartSlots;
import com.gregtech.gregtech.api.addon.PartTankRules;
import com.gregtech.gregtech.api.addon.PartTanks;
import com.gregtech.gregtech.api.addon.AddonMachines;
import com.gregtech.gregtech.api.kubejs.StructureLayoutDocuments;
import com.gregtech.gregtech.content.multiblock.PartDesigns;
import com.gregtech.gregtech.api.addon.RecipeMachine;
import com.gregtech.gregtech.api.addon.RecipeTable;
import com.gregtech.gregtech.api.addon.StructureLayout;
import com.gregtech.gregtech.api.addon.StructureRole;

import java.util.List;

/** New declaration types. Nothing here is installed, so the old machine queue stays untouched. */
public final class AddonDeclarationTypeContracts {
    private static int assertions;

    private AddonDeclarationTypeContracts() {}

    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("This contract takes no scenario");
        rejectedComponents();
        energyRules();
        ioAndFuel();
        tables();
        structure();
        secondEnergy();
        mainEnergy();
        functionalParts();
        partAdditions();
        System.out.println("passed " + assertions);
    }

    private static void rejectedComponents() {
        var casing = casing("press");
        var energy = eu(32);
        var emitted = emitted();
        var fuel = fuelPacket();
        var io = MachineIo.builder().itemInputs(MachineFace.FRONT).build();

        fails(() -> RecipeMachine.builder(casing).energy(energy).recipeMap("aa.recipe.press")
                .energyOutput(emitted).build(), "energy output");
        fails(() -> RecipeMachine.builder(casing).energy(energy).recipeMap("aa.recipe.press")
                .output(fuel).build(), "fuel output");
        fails(() -> RecipeMachine.builder(casing).energy(EnergyInput.builder("EU", 32).capacity(0).build())
                .recipeMap("aa.recipe.press").build(), "energy capacity");

        var faced = BehaviorMachine.builder(casing).energy(energy).gui(GUI).io(io).build();
        check(faced.io() != null && faced.itemInputMask() == MachineFace.FRONT.bit()
                && faced.itemOutputMask() == 0 && faced.fluidInputMask() == 0 && faced.fluidOutputMask() == 0,
                "behavior machine keeps declared faces and closes the rest");
        var open = BehaviorMachine.builder(casing).energy(energy).gui(GUI).build();
        check(open.io() == null && open.itemInputMask() == (1 << 6) - 1 && open.fluidOutputMask() == (1 << 6) - 1,
                "an unset behavior io keeps every face open");
        check(faced.equals(BehaviorMachine.builder(casing).energy(energy).gui(GUI).io(io).build())
                && !faced.equals(open), "behavior io is part of equality");
        var signaling = BehaviorMachine.builder(casing).energy(energy).gui(GUI).signalOutput(true).build();
        check(!open.signalOutput() && signaling.signalOutput(), "behavior signal output is declared");
        check(!signaling.equals(open), "behavior signal output is part of equality");
        var syncing = BehaviorMachine.builder(casing).energy(energy).gui(GUI).clientState("armed").build();
        check(open.clientState().isEmpty() && syncing.clientState().equals(List.of("armed")),
                "behavior client state is declared");
        check(!syncing.equals(open), "behavior client state is part of equality");
        fails(() -> BehaviorMachine.builder(casing).energy(energy).gui(GUI).clientState("Armed").build(),
                "client state key");
        fails(() -> BehaviorMachine.builder(casing).energy(energy).gui(GUI).clientState("armed", "armed").build(),
                "repeats client state");
        fails(() -> BehaviorMachine.builder(casing).energy(energy).gui(GUI)
                .io(MachineIo.builder().itemAutoOutput(MachineFace.BOTTOM).build()).build(), "automatic face");
        fails(() -> BehaviorMachine.builder(casing).energy(energy).gui(GUI)
                .io(MachineIo.builder().itemOutputTarget(1, 0, 0, MachineFace.FRONT).build()).build(), "IO target");
        fails(() -> BehaviorMachine.builder(casing).energy(energy).gui(GUI).output(fuel).build(), "fuel output");

        var tiered = MachineCasing.builder("press", "Steel").tier(1).build();
        fails(() -> FuelGenerator.builder(tiered).fuelMap("aa.fuel.solid").output(fuel).build(), "tier");
        fails(() -> FuelGenerator.builder(casing).fuelMap("aa.fuel.solid").output(fuel).energy(energy).build(),
                "energy input");
        fails(() -> FuelGenerator.builder(casing).fuelMap("aa.fuel.solid").output(fuel).energyOutput(emitted).build(),
                "energy output");
        fails(() -> FuelGenerator.builder(casing).fuelMap("aa.fuel.solid").output(fuel).io(io).build(),
                "item and fluid");
    }

    private static void energyRules() {
        var huge = RecipeMachine.builder(casing("press")).energy(eu(Long.MAX_VALUE)).recipeMap("aa.recipe.press").build();
        check(huge.energy().perTick() == Long.MAX_VALUE, "recipe per tick has no upper bound");
        check(recipe(eu(0)).energy().perTick() == 0, "recipe per tick may be zero");
        fails(() -> recipe(eu(-1)), "per tick");

        var open = recipe(eu(32));
        check(!open.energy().rangeSpecified() && open.energy().rangeMin() == 0 && open.energy().rangeMax() == 0,
                "an unset recipe range stays unset");
        check(!open.energy().facesSpecified() && open.energy().faces().equals(MachineFace.all()),
                "recipe faces default to six");
        check(!open.energy().capacitySpecified() && open.energy().capacity() == 0, "recipe capacity stays unset");
        fails(() -> open.energy().faces().add(MachineFace.FRONT), UnsupportedOperationException.class, null);

        var ranged = recipe(EnergyInput.builder("EU", 32).range(8, 64).build());
        check(ranged.energy().rangeSpecified() && ranged.energy().rangeMin() == 8 && ranged.energy().rangeMax() == 64,
                "recipe range is stored");
        fails(() -> recipe(EnergyInput.builder("TU", 16).range(1, 16).build()), "TU");
        fails(() -> recipe(EnergyInput.builder("tu", 16).range(1, 32).build()), "TU");
        var emptyFaces = recipe(EnergyInput.builder("EU", 32).faces().build());
        check(emptyFaces.energy().facesSpecified() && emptyFaces.energy().faces().isEmpty(),
                "an empty recipe face list is no energy input");
        var namedSix = recipe(EnergyInput.builder("EU", 32).faces(MachineFace.all()).build());
        check(namedSix.energy().facesSpecified() && !open.equals(namedSix),
                "naming all six faces is not the same as leaving them unset");
        fails(() -> EnergyInput.builder("EU", 32).faces(MachineFace.FRONT, MachineFace.FRONT).build(), "Repeated");
        fails(() -> EnergyInput.builder("EU", 32).range(0, 0).build(), "energy range");

        var built = behavior(casing("press"), eu(32));
        check(!built.energy().facesSpecified() && built.energy().faces().equals(MachineFace.exceptFront()),
                "behavior faces default to every side but the front");
        check(built.energyMask() == (MachineFace.all().stream().mapToInt(MachineFace::bit).sum() - MachineFace.FRONT.bit()),
                "behavior energy mask skips the front");
        check(!built.energy().capacitySpecified() && built.energy().capacity() == 0, "behavior capacity defaults to zero");
        var zeroCapacity = behavior(casing("press"), EnergyInput.builder("EU", 32).capacity(0).build());
        check(built.equals(zeroCapacity) && built.hashCode() == zeroCapacity.hashCode(),
                "naming a zero behavior capacity is the default");
        var stored = behavior(casing("buffer"), EnergyInput.builder("RU", 32).capacity(32_000)
                .faces(MachineFace.LEFT, MachineFace.RIGHT).build());
        check(stored.energy().capacitySpecified() && stored.energy().capacity() == 32_000, "behavior capacity is stored");
        check(stored.energy().facesSpecified() && stored.energy().faces().equals(List.of(MachineFace.LEFT, MachineFace.RIGHT)),
                "behavior faces stay in the written order");
        fails(() -> behavior(casing("press"), eu(0)), "per tick");
        check(behavior(casing("press"), eu(1_000_000_000_000L)).energy().perTick() == 1_000_000_000_000L,
                "behavior per tick allows 1e12");
        fails(() -> behavior(casing("press"), eu(1_000_000_000_001L)), "per tick");
        fails(() -> behavior(casing("press"), EnergyInput.builder("EU", 32).faces().build()), "no face");
        fails(() -> EnergyInput.builder("EU", 32).capacity(-1).build(), "Negative");

        var wide = RecipeMachine.builder(MachineCasing.builder("press", "Steel").tier(256).build())
                .energy(eu(32)).recipeMap("aa.recipe.press").build();
        check(wide.casing().tier() == 256 && wide.casing().tierSpecified(), "recipe tier has no upper bound");
        fails(() -> RecipeMachine.builder(MachineCasing.builder("press", "Steel").tier(0).build())
                .energy(eu(32)).recipeMap("aa.recipe.press").build(), "tier");
        fails(() -> behavior(MachineCasing.builder("press", "Steel").tier(256).build(), eu(32)), "tier");
        fails(() -> recipe(MachineCasing.builder("a/b", "Steel").build(), eu(32)), "machine type");
        var slashed = behavior(MachineCasing.builder("a/b", "Steel").build(), eu(32));
        check(slashed.casing().machineType().equals("a_b") && slashed.blockEntityPath().equals("be_a_b"),
                "behavior machine type replaces slashes");
        fails(() -> FuelGenerator.builder(MachineCasing.builder("a/b", "Steel").build())
                .fuelMap("aa.fuel.solid").output(fuelPacket()).build(), "machine type");

        var plain = recipe(eu(32));
        var cheapOff = RecipeMachine.builder(casing("press")).energy(eu(32)).recipeMap("aa.recipe.press")
                .cheapOverclocking(false).build();
        check(!plain.cheapOverclocking() && !plain.cheapOverclockingSpecified(), "cheap overclock starts unset");
        check(!cheapOff.cheapOverclocking() && cheapOff.cheapOverclockingSpecified() && !plain.equals(cheapOff),
                "an explicit cheap-overclock off is not the unset default");
        var again = recipe(eu(32));
        check(plain.equals(again) && plain.hashCode() == again.hashCode(), "equal recipe declarations match");
    }

    private static void ioAndFuel() {
        var unset = recipe(eu(32));
        check(unset.io() == null && unset.basic() && unset.parallel() == 1 && unset.efficiency() == 10_000
                        && unset.constantEnergy() && unset.minimumLightAbove() == 0
                        && !unset.searchOutputTanks() && !unset.exclusiveOutputs() && unset.structureKeys().isEmpty(),
                "recipe defaults");
        var written = RecipeMachine.builder(casing("press")).energy(eu(32)).recipeMap("aa.recipe.press")
                .io(MachineIo.builder().fluidInputs(MachineFace.TOP).build()).build();
        check(written.io().itemInputs().isEmpty() && written.io().fluidInputs().equals(List.of(MachineFace.TOP)),
                "a written io object leaves unnamed channels empty");
        fails(() -> MachineIo.builder().itemInputs(MachineFace.FRONT, MachineFace.FRONT).build(), "Repeated");

        fails(() -> IoTarget.at(0, 0, 0, MachineFace.FRONT), "origin");
        fails(() -> IoTarget.at(17, 0, 0, MachineFace.FRONT), "out of range");
        fails(() -> IoTarget.at(0, -17, 0, MachineFace.FRONT), "out of range");
        var edge = IoTarget.at(-16, 16, 1, MachineFace.BACK);
        check(edge.right() == -16 && edge.up() == 16 && edge.back() == 1 && edge.receiver() == MachineFace.BACK,
                "io target accepts the ±16 edge");
        fails(() -> RecipeMachine.builder(casing("press")).energy(eu(32)).recipeMap("aa.recipe.press")
                .io(MachineIo.builder().itemOutputTarget(edge).build()).build(), "large recipe");
        var large = RecipeMachine.builder(casing("press")).energy(eu(32)).recipeMap("aa.recipe.press")
                .largeRecipe().io(MachineIo.builder().itemOutputTarget(1, 0, 0, MachineFace.FRONT).build()).build();
        check(large.largeRecipe() && large.io().itemOutputTarget().right() == 1, "large recipe keeps its target");
        fails(() -> MachineIo.builder().itemOutputTarget(edge).itemAutoOutput(MachineFace.BOTTOM).build(), "conflicts");

        fails(() -> FuelOutput.builder("RU", 0).build(), "per tick");
        fails(() -> FuelOutput.builder("RU", 1_000_000_000_001L).build(), "per tick");
        fails(() -> FuelOutput.builder("RU", 32).behind(0).build(), "1..8");
        fails(() -> FuelOutput.builder("RU", 32).behind(9).build(), "1..8");
        check(FuelOutput.builder("RU", 32).build().front() && FuelOutput.builder("RU", 32).back().build().back(),
                "fuel output defaults to the front and can name the back");
        check(FuelOutput.builder("RU", 32).behind(1).build().behind() == 1
                        && FuelOutput.builder("RU", 32).behind(FuelGenerator.MAX_BEHIND).build().behind()
                        == FuelGenerator.MAX_BEHIND,
                "fuel output behind is 1..8");
        var fuel = FuelGenerator.builder(casing("turbine")).fuelMap("aa.fuel.solid").output(fuelPacket()).build();
        check(fuel.single() && fuel.efficiency() == 10_000 && fuel.tankCapacity() == 4_000 && fuel.output().front()
                        && !fuel.casing().tierSpecified() && fuel.casing().machineType().equals("turbine"),
                "fuel defaults");
        fails(() -> FuelGenerator.builder(casing("turbine")).fuelMap("aa.fuel.solid").output(fuelPacket())
                .tank(0).build(), "parameters");
        fails(() -> FuelGenerator.builder(casing("turbine")).fuelMap("aa.fuel.solid").output(fuelPacket())
                .tank(FuelGenerator.MAX_TANK + 1).build(), "parameters");
    }

    private static void tables() {
        var machine = RecipeTable.machine("press").title("Press").gui("gui").build();
        var fuel = RecipeTable.fuel("press").title("Press").gui("gui").build();
        check(machine.machine() && !machine.fuel() && fuel.fuel() && !fuel.machine(), "table factories stay apart");
        check(!machine.equals(fuel), "a machine table and a fuel table do not share a namespace");
        check(machine.inputItems() == 0 && machine.outputFluids() == 0 && machine.minimalInputs() == 0
                        && !machine.needsOutputs() && !machine.combinePower() && !machine.bucketIn() && !machine.bucketOut()
                        && machine.title().equals(fuel.title()) && machine.gui().equals(fuel.gui()),
                "table fields match and default to zero or false");
        var counted = RecipeTable.fuel("solid").title("Solid").gui("not-a-png").inputItems(1).needsOutputs(true).build();
        check(counted.fuel() && counted.inputItems() == 1 && counted.needsOutputs() && counted.gui().equals("not-a-png"),
                "a fuel table gui is not a behavior texture");
        fails(() -> RecipeTable.machine("press").title("Press").gui("gui").inputItems(-1).build(), "Negative");
        fails(() -> RecipeTable.machine("Press").title("Press").gui("gui").build(), "recipe map");
        fails(() -> BehaviorMachine.builder(casing("press")).energy(eu(32)).gui("not-a-png").build(), "gui texture");
    }

    private static void structure() {
        fails(() -> LayoutCell.at(0, 0, 0, CellMatch.part("wall"), StructureRole.CASING), "origin");
        var far = LayoutCell.at(100, -3, 20, CellMatch.part("wall"), StructureRole.ENERGY_OUTPUT);
        check(far.right() == 100 && far.role() == StructureRole.ENERGY_OUTPUT, "structure cells have no ±16 limit");
        fails(() -> LayoutCell.at(1, 0, 0, CellMatch.block("minecraft:glass"), StructureRole.ENERGY_INPUT), "casing role");
        fails(() -> LayoutCell.at(1, 0, 0, CellMatch.tag("c:glass"), StructureRole.ENERGY_INPUT), "casing role");
        var glass = LayoutCell.at(1, 0, 0, CellMatch.block("minecraft:glass"), StructureRole.CASING);
        check(glass.match().equals(CellMatch.parse("minecraft:glass")), "a block id parses as a block");
        check(CellMatch.parse("air").equals(CellMatch.air()) && CellMatch.part("air").equals(CellMatch.air()),
                "air is its own match");
        check(CellMatch.parse("#c:glass_blocks").equals(CellMatch.tag("c:glass_blocks")), "a hash parses as a tag");
        check(CellMatch.parse("tank_wall").equals(CellMatch.part("tank_wall")), "other names stay part names");
        fails(() -> CellMatch.parse("#nope"), "block name");
        fails(() -> CellMatch.block("glass"), "block name");
        fails(() -> StructureLayout.builder("press").build(), "empty");
        fails(() -> StructureLayout.builder("press")
                .cell(1, 0, 0, CellMatch.part("wall"), StructureRole.CASING)
                .cell(1, 0, 0, CellMatch.air(), StructureRole.AIR).build(), "Duplicate");
        var layout = StructureLayout.builder("press")
                .cell(far)
                .cell(2, 0, 0, CellMatch.air(), StructureRole.AIR)
                .build();
        check(layout.name().equals("press") && layout.cells().size() == 2, "layout keeps its cells");
        fails(() -> layout.cells().clear(), UnsupportedOperationException.class, null);

        var wall = LayoutPart.builder("sample_wall", "Steel").hardness(6.0F).build();
        check(wall.energy() == null && wall.energyCapacity() == 0 && wall.texture().isEmpty(),
                "a part with no capability is a casing");
        fails(() -> LayoutPart.builder("sample_wall", "Steel").build(), "hardness");
        fails(() -> LayoutPart.builder("sample_wall", "Steel").hardness(6.0F)
                .energyOutput(EnergyOutput.builder("eu-1", 32, 1).faces(MachineFace.BACK).build(), 1).build(),
                "invalid");
        fails(() -> LayoutPart.builder("sample_emitter", "Steel").hardness(6.0F)
                .energyOutput(emitted(), 0).build(), "positive");
        var emitter = LayoutPart.builder("sample_emitter", "Steel").hardness(6.0F).texture("block/sample_emitter")
                .energyOutput(emitted(), 1_000).build();
        check(emitter.energy().equals(emitted()) && emitter.energyCapacity() == 1_000, "energy part stores its output");
        check(emitter.energyInput() == null && emitter.conditionPerTick() == 0 && !emitter.condition(),
                "an output part does not accept");

        var machine = behavior(casing("miner"), eu(32));
        check(!machine.emitsEnergy() && machine.energyOutMask() == 0 && machine.output() == null, "no behavior output");
        check(machine.auxiliary() == null && machine.auxiliaryMask() == 0, "no auxiliary input");
        var emitting = BehaviorMachine.builder(casing("miner")).energy(eu(32)).gui(GUI)
                .energyOutput(emitted()).build();
        check(emitting.emitsEnergy() && emitting.energyOutMask() == MachineFace.BACK.bit()
                        && emitting.output().maxSize() == 32 && emitting.output().maxAmount() == 1,
                "behavior output is one object");
        fails(() -> BehaviorMachine.builder(casing("bad")).energy(eu(32)).gui(GUI)
                .fluid(1000, 8, 8, BehaviorMachine.SlotRole.INPUT).build(), "overlaps");
    }

    private static void secondEnergy() {
        var input = EnergyInput.builder("EU", 128).range(64, 256).capacity(10_000).faces(MachineFace.FRONT).build();
        var condition = LayoutPart.builder("sample_condition", "Steel").hardness(6.0F)
                .energyInput(input).condition(192).build();
        check(condition.energy() == null && condition.energyCapacity() == 0 && condition.condition()
                        && condition.conditionPerTick() == 192 && condition.energyInput().capacity() == 10_000
                        && condition.energyInput().rangeMin() == 64 && condition.energyInput().rangeMax() == 256,
                "a condition part stores its input");
        fails(() -> LayoutPart.builder("sample_condition", "Steel").hardness(6.0F)
                .energyOutput(emitted(), 1_000).energyInput(input).condition(192).build(), "both");
        var internal = EnergyInput.builder("EU", 128).range(64, 256).capacity(10_000).build();
        var twoWay = LayoutPart.builder("sample_two_way", "Steel").hardness(6.0F)
                .energyOutput(emitted(), 1_000).energyInput(internal).build();
        check(twoWay.energy() != null && twoWay.energyInput() != null && !twoWay.condition(),
                "a two-way part keeps both roles");
        fails(() -> LayoutPart.builder("sample_two_way", "Steel").hardness(6.0F)
                .energyOutput(emitted(), 1_000).energyInput(input).build(), "both");
        fails(() -> LayoutPart.builder("sample_two_way", "Steel").hardness(6.0F)
                .energyOutput(emitted(), 1_000).energyInput(internal).condition(64).build(), "both");
        fails(() -> LayoutPart.builder("sample_condition", "Steel").hardness(6.0F).condition(192).build(), "condition");
        fails(() -> LayoutPart.builder("sample_condition", "Steel").hardness(6.0F).energyInput(input).build(), "condition");
        fails(() -> LayoutPart.builder("sample_condition", "Steel").hardness(6.0F)
                .energyInput(EnergyInput.builder("EU", 128).range(64, 256).capacity(10_000)
                        .faces(MachineFace.FRONT).anyPacket().build()).condition(192).build(), "any packet");
        fails(() -> LayoutPart.builder("sample_condition", "Steel").hardness(6.0F)
                .energyInput(EnergyInput.builder("EU", 32).range(64, 256).capacity(10_000).faces(MachineFace.FRONT).build())
                .condition(192).build(), "packet range");
        fails(() -> LayoutPart.builder("sample_condition", "Steel").hardness(6.0F).energyInput(input).condition(20_000).build(),
                "capacity");

        var open = EnergyInput.builder("HU", 32).anyPacket().build();
        check(open.anyPacket() && !open.rangeSpecified(), "anyPacket leaves the range unset");
        fails(() -> EnergyInput.builder("HU", 32).range(1, 64).anyPacket().build(), "any packet");
        fails(() -> recipe(EnergyInput.builder("EU", 32).anyPacket().build()), "packet size");
        fails(() -> behavior(casing("press"), EnergyInput.builder("EU", 32).anyPacket().build()), "positive");

        var auxiliary = EnergyInput.builder("HU", 32).anyPacket().capacity(50_000).faces(MachineFace.FRONT).build();
        var machine = BehaviorMachine.builder(casing("deburn")).energy(eu(32)).gui(GUI).auxiliaryEnergy(auxiliary).build();
        check(machine.energy().type().equals("EU") && machine.energy().perTick() == 32 && !machine.energy().anyPacket()
                        && !machine.energy().rangeSpecified() && machine.energy().faces().equals(MachineFace.exceptFront())
                        && machine.auxiliary().anyPacket() && machine.auxiliary().capacity() == 50_000
                        && machine.auxiliaryMask() == MachineFace.FRONT.bit()
                        && (machine.energyMask() & machine.auxiliaryMask()) == 0,
                "auxiliary energy is a separate buffer on its own face");
        check(!behavior(casing("deburn"), eu(32)).equals(machine), "auxiliary energy changes the machine");
        var ranged = BehaviorMachine.builder(casing("heater")).energy(EnergyInput.builder("EU", 32)
                        .faces(MachineFace.BACK).build()).gui(GUI)
                .auxiliaryEnergy(EnergyInput.builder("HU", 64).range(32, 128).capacity(8_000).faces(MachineFace.TOP).build())
                .build();
        check(!ranged.auxiliary().anyPacket() && ranged.auxiliary().rangeMin() == 32 && ranged.auxiliary().rangeMax() == 128,
                "auxiliary range stays on the second input");
        var sharedFace = BehaviorMachine.builder(casing("deburn")).energy(eu(32)).gui(GUI)
                .auxiliaryEnergy(EnergyInput.builder("HU", 32).anyPacket().capacity(50_000).faces(MachineFace.TOP).build())
                .build();
        check((sharedFace.energyMask() & sharedFace.auxiliaryMask()) == MachineFace.TOP.bit(),
                "different types share the top face");
        fails(() -> BehaviorMachine.builder(casing("deburn")).energy(eu(32)).gui(GUI)
                .auxiliaryEnergy(EnergyInput.builder("EU", 32).anyPacket().capacity(50_000).faces(MachineFace.TOP).build())
                .build(), "overlaps");
        fails(() -> BehaviorMachine.builder(casing("deburn")).energy(eu(32)).gui(GUI)
                .auxiliaryEnergy(EnergyInput.builder("HU", 32).capacity(50_000).faces(MachineFace.FRONT).build()).build(),
                "anyPacket");
        fails(() -> BehaviorMachine.builder(casing("deburn")).energy(eu(32)).gui(GUI)
                .auxiliaryEnergy(EnergyInput.builder("HU", 32).anyPacket().faces(MachineFace.FRONT).build()).build(),
                "positive");

        var dynamic = BehaviorMachine.builder(casing("quantum")).energy(
                EnergyInput.builder("EU", 32).anyType().build()).gui(GUI).build();
        check(dynamic.energy().dynamic() && dynamic.energy().type().equals("*"), "a behavior main input can pick its type");
        fails(() -> recipe(EnergyInput.builder("EU", 32).anyType().build()), "pick its energy type");
        fails(() -> BehaviorMachine.builder(casing("quantum")).energy(eu(32)).gui(GUI)
                .auxiliaryEnergy(EnergyInput.builder("HU", 32).anyType().anyPacket().capacity(1_000)
                        .faces(MachineFace.TOP).build()).build(), "auxiliary energy cannot pick");

        var recipeAux = RecipeMachine.builder(casing("auxpress")).energy(eu(32))
                .auxiliaryEnergy(EnergyInput.builder("LU", 16).anyPacket().capacity(8_000)
                        .faces(MachineFace.BACK).build())
                .startupEnergy(true).recipeMap("aa.recipe.press").build();
        check(recipeAux.auxiliary() != null && recipeAux.auxiliary().type().equals("LU")
                && recipeAux.startupEnergy() && recipeAux.auxiliary().capacity() == 8_000,
                "a recipe machine keeps its second input and startup flag");
        fails(() -> RecipeMachine.builder(casing("auxpress")).energy(eu(32)).startupEnergy(true)
                .recipeMap("aa.recipe.press").build(), "needs auxiliary energy");
        fails(() -> RecipeMachine.builder(casing("auxpress")).energy(eu(32))
                .auxiliaryEnergy(EnergyInput.builder("LU", 16).capacity(8_000)
                        .faces(MachineFace.BACK).build())
                .recipeMap("aa.recipe.press").build(), "anyPacket");
        fails(() -> RecipeMachine.builder(casing("auxpress")).energy(eu(32))
                .auxiliaryEnergy(EnergyInput.builder("EU", 16).anyPacket().capacity(8_000)
                        .faces(MachineFace.FRONT).build())
                .recipeMap("aa.recipe.press").build(), "overlaps an input face");

        var over = PartEnergyRules.admit(0, 1_000, 300, 2, 64, 256, false);
        check(over.overload() && over.packets() == 2, "an oversized packet overloads and is consumed");
        var small = PartEnergyRules.admit(0, 1_000, 32, 1, 64, 256, false);
        check(!small.overload() && small.packets() == 0, "an undersized packet is not stored");
        var fit = PartEnergyRules.admit(0, 1_000, 128, 4, 64, 256, false);
        check(!fit.overload() && fit.packets() == 4, "in-range packets fit");
        var capped = PartEnergyRules.admit(900, 1_000, 128, 4, 64, 256, false);
        check(!capped.overload() && capped.packets() == 1, "the buffer caps how many packets fit");
        var any = PartEnergyRules.admit(0, 1_000, 10_000, 1, 0, 0, true);
        check(!any.overload() && any.packets() == 1, "anyPacket does not overload");
        check(com.gregtech.gregtech.api.machine.BasicMachineEnergy.add(0, 1_000, 10_000, any.packets()) == 1_000,
                "anyPacket fills up to capacity");
        var dry = PartEnergyRules.drain(100, 192);
        check(!dry.satisfied() && dry.stored() == 100, "a short condition buffer is not drained");
        var wet = PartEnergyRules.drain(192, 192);
        check(wet.satisfied() && wet.stored() == 0, "an exact condition buffer is drained");
    }

    private static void mainEnergy() {
        var ranged = behavior(casing("press"), EnergyInput.builder("EU", 32).range(16, 64).build());
        check(ranged.energy().rangeSpecified() && ranged.energy().rangeMin() == 16 && ranged.energy().rangeMax() == 64
                        && ranged.energy().overload(),
                "main input keeps a packet range and overloads by default");
        fails(() -> behavior(casing("press"), EnergyInput.builder("EU", 32).range(64, 256).build()), "packet range");
        var open = behavior(casing("press"), EnergyInput.builder("EU", 32).anyPacket().capacity(10_000).build());
        check(open.energy().anyPacket() && open.energy().capacity() == 10_000
                        && open.energy().faces().equals(MachineFace.exceptFront()),
                "main anyPacket uses the default faces");
        var quiet = behavior(casing("press"), EnergyInput.builder("EU", 32).overload(false).build());
        check(!quiet.energy().overload() && !quiet.energy().rangeSpecified(), "the default window can refuse overload");
        fails(() -> recipe(EnergyInput.builder("EU", 32).overload(false).build()), "overload");
        fails(() -> LayoutPart.builder("sample_condition", "Steel").hardness(6.0F)
                .energyInput(EnergyInput.builder("EU", 128).range(64, 256).capacity(10_000)
                        .faces(MachineFace.FRONT).overload(false).build())
                .condition(192).build(), "overload");

        var wheel = BehaviorMachine.builder(casing("wheel")).gui(GUI)
                .energyOutput(EnergyOutput.builder("EU", 32, 1).faces(MachineFace.TOP).build()).build();
        check(wheel.energy() == null && wheel.energyMask() == 0 && wheel.emitsEnergy()
                        && wheel.output().faces().equals(List.of(MachineFace.TOP)),
                "an output-only machine has no input");
        var remote = BehaviorMachine.builder(casing("mirror")).gui(GUI)
                .energyOutput(EnergyOutput.builder("HU", 16, 1).remote().build()).build();
        check(remote.emitsEnergy() && remote.output().remote() && remote.output().faces().isEmpty()
                        && remote.energyOutMask() == 0,
                "a remote output needs no face");
        check(EnergyOutput.builder("HU", 16, 1).remote().build().equals(remote.output()),
                "the remote flag is part of output equality");
        fails(() -> LayoutPart.builder("sample_remote", "Steel").hardness(6.0F)
                .energyOutput(EnergyOutput.builder("HU", 16, 1).remote().build(), 1_000).build(), "remote");
        fails(() -> BehaviorMachine.builder(casing("wheel")).gui(GUI).build(), "Missing energy");
        var slotted = BehaviorMachine.builder(casing("wheel")).gui(GUI)
                .energyOutput(EnergyOutput.builder("EU", 32, 1).faces(MachineFace.TOP).build())
                .item(8, 8, BehaviorMachine.SlotRole.OUTPUT).build();
        check(slotted.items().size() == 1, "the energy gauge is absent, so a slot can use its place");
        var emittingAux = BehaviorMachine.builder(casing("wheel")).gui(GUI)
                .energyOutput(EnergyOutput.builder("EU", 32, 1).faces(MachineFace.TOP).build())
                .auxiliaryEnergy(EnergyInput.builder("HU", 16).range(8, 32).capacity(1_000).faces(MachineFace.BACK).build())
                .build();
        check(emittingAux.energy() == null && emittingAux.auxiliary().type().equals("HU"),
                "an output-only machine can still take a second input");

        var over = BehaviorEnergyRules.admitInput(0, 64, 128, 2, 16, 64, false, true, false);
        check(over.overload() && over.packets() == 2, "the default window still overloads above the maximum");
        var small = BehaviorEnergyRules.admitInput(0, 64, 8, 1, 16, 64, false, true, false);
        check(!small.overload() && small.packets() == 1, "the default window still stores an undersized packet");
        var refused = BehaviorEnergyRules.admitInput(0, 64, 128, 2, 16, 64, false, false, false);
        check(!refused.overload() && refused.packets() == 0, "overload off refuses an oversized packet");
        var low = BehaviorEnergyRules.admitInput(0, 64, 8, 1, 16, 64, false, false, false);
        check(low.packets() == 0, "overload off refuses an undersized packet");
        var narrow = BehaviorEnergyRules.admitInput(0, 1_000, 8, 1, 16, 64, false, true, true);
        check(narrow.packets() == 0 && !narrow.overload(), "a declared range refuses an undersized packet");
        var any = BehaviorEnergyRules.admitInput(0, 1_000, 10_000, 1, 0, 0, true, true, true);
        check(!any.overload() && any.packets() == 1, "main anyPacket does not overload");
    }

    private static void functionalParts() {
        var wall = LayoutPart.builder("sample_wall", "Steel").hardness(6.0F).build();
        check(!wall.functional() && wall.slots() == null && !wall.ticks() && !wall.handlesUse() && wall.fields().isEmpty(),
                "a casing has no functional capability");

        var emitter = LayoutPart.builder("sample_emitter", "Steel").hardness(6.0F).texture("block/sample_emitter")
                .energyOutput(EnergyOutput.builder("RU", 32, 1).faces(MachineFace.FRONT).build(), 1_000).build();
        check(!emitter.functional(), "an energy-only part is not functional");

        var compute = LayoutPart.builder("sample_compute", "Steel").hardness(6.0F)
                .slots(PartSlots.builder(4).stackLimit(1).drops(true).build())
                .tick().use().field("power").field("running").build();
        check(compute.functional() && compute.slots().count() == 4 && compute.slots().stackLimit() == 1
                        && compute.slots().drops() && compute.slots().insertFaces().isEmpty()
                        && compute.ticks() && compute.handlesUse()
                        && compute.fields().equals(List.of("power", "running")),
                "a compute part keeps slots, a tick, a click and its fields");

        fails(() -> PartSlots.builder(0).build(), "1 to");
        fails(() -> PartSlots.builder(17).build(), "1 to");
        fails(() -> PartSlots.builder(1).stackLimit(0).build(), "stack limit");
        fails(() -> PartSlots.builder(1).stackLimit(65).build(), "stack limit");
        fails(() -> LayoutPart.builder("sample_compute", "Steel").hardness(6.0F).slots(null).build(), "Missing");
        fails(() -> PartSlots.builder(1).insert(MachineFace.FRONT, MachineFace.FRONT).build(), "Repeated");
        fails(() -> PartSlots.builder(1).insert(new MachineFace[] {null}).build(), "Missing");
        fails(() -> LayoutPart.builder("sample_compute", "Steel").hardness(6.0F).field("Power").build(), "Invalid");
        fails(() -> LayoutPart.builder("sample_compute", "Steel").hardness(6.0F).field("power").field("power").build(),
                "Repeated");
        var many = LayoutPart.builder("sample_compute", "Steel").hardness(6.0F);
        for (int i = 0; i < LayoutPart.MAX_FIELDS; i++) many.field("f" + i);
        check(many.build().fields().size() == LayoutPart.MAX_FIELDS, "sixteen fields fit");
        fails(() -> many.field("extra"), "Too many");

        var combined = LayoutPart.builder("sample_emitter", "Steel").hardness(6.0F)
                .energyOutput(EnergyOutput.builder("RU", 32, 1).faces(MachineFace.FRONT).build(), 1_000)
                .slots(PartSlots.builder(1).build()).build();
        check(combined.functional() && combined.energy() != null, "energy and slots can share a part");
        var input = EnergyInput.builder("EU", 128).range(64, 256).capacity(10_000).faces(MachineFace.FRONT).build();
        fails(() -> LayoutPart.builder("sample_condition", "Steel").hardness(6.0F)
                .energyOutput(EnergyOutput.builder("RU", 32, 1).faces(MachineFace.BACK).build(), 1_000)
                .energyInput(input).condition(192).build(), "both");
        var ticking = LayoutPart.builder("sample_condition", "Steel").hardness(6.0F)
                .energyInput(input).condition(192).tick().build();
        check(ticking.functional() && ticking.condition() && ticking.ticks(), "a condition part may tick");

        var faced = PartSlots.builder(2).insert(MachineFace.FRONT).extract(MachineFace.BACK).build();
        check(PartSlotRules.inserts(faced, MachineFace.FRONT) && !PartSlotRules.inserts(faced, MachineFace.BACK),
                "insert faces are the declared list");
        check(PartSlotRules.extracts(faced, MachineFace.BACK) && !PartSlotRules.extracts(faced, null),
                "extract faces are the declared list");
        check(!PartSlotRules.inserts(null, MachineFace.FRONT), "missing slots do not insert");
        check(PartSlotRules.accepted(0, 1, 4) == 1, "a stack limit of one accepts one");
        check(PartSlotRules.accepted(1, 1, 4) == 0, "a full slot accepts nothing");
        check(PartSlotRules.accepted(0, 64, 0) == 0, "nothing offered fits nothing");

        var model = new ComputeModel();
        check(model.insert(0, "redstone") && model.power() == 1, "redstone is one power");
        check(model.insert(1, "diamond") && model.power() == 5, "diamond is four power");
        check(!model.insert(1, "redstone"), "an occupied slot rejects another item");
        check(!model.tryStart(6) && !model.running(), "start needs enough power");
        check(model.tryStart(5) && model.running(), "start locks the part");
        check(!model.insert(2, "redstone") && model.extract(0).isEmpty(), "a running part rejects items");
        model.stop();
        check(!model.running() && model.extract(0).equals("redstone") && model.power() == 4, "stop releases the items");
        check(model.item(1).equals("diamond"), "the other slot stays");
    }

    private static void partAdditions() {
        var tanks = PartTanks.builder(2).capacity(8_000).insert(MachineFace.TOP).extract(MachineFace.BOTTOM).build();
        var part = LayoutPart.builder("sample_tank", "Steel").hardness(6.0F)
                .tanks(tanks).gui("aa_sample:textures/gui/part.png").build();
        check(part.functional() && part.tanks().count() == 2 && part.tanks().capacity() == 8_000
                        && part.gui().equals("aa_sample:textures/gui/part.png"),
                "tanks and a menu make a part functional");
        check(PartTankRules.inserts(tanks, MachineFace.TOP) && !PartTankRules.inserts(tanks, MachineFace.BOTTOM)
                        && !PartTankRules.extracts(tanks, null) && !PartTankRules.inserts(null, MachineFace.TOP),
                "tank faces follow the declared lists");
        fails(() -> PartTanks.builder(0).capacity(1).build(), "1 to");
        fails(() -> PartTanks.builder(9).capacity(1).build(), "1 to");
        fails(() -> PartTanks.builder(1).build(), "capacity");
        fails(() -> PartTanks.builder(1).capacity(0).build(), "capacity");
        fails(() -> PartTanks.builder(1).capacity(PartTanks.MAX_CAPACITY + 1).build(), "capacity");
        fails(() -> LayoutPart.builder("sample_tank", "Steel").hardness(6.0F).tanks(null).build(), "Missing");
        fails(() -> LayoutPart.builder("sample_tank", "Steel").hardness(6.0F).gui("textures/gui/part.png").build(),
                "gui");

        var valve = BehaviorMachine.builder(casing("press")).energy(eu(32)).gui(GUI).gasValve(MachineFace.BACK).build();
        var closed = BehaviorMachine.builder(casing("press")).energy(eu(32)).gui(GUI).build();
        check(valve.gasValve() == MachineFace.BACK && closed.gasValve() == null && !valve.equals(closed),
                "a behavior machine can name one compressed-gas valve");
        fails(() -> BehaviorMachine.builder(casing("press")).energy(eu(32)).gui(GUI).gasValve(null).build(), "valve");

        check(PartDesigns.max("steel_wall") == 7 && PartDesigns.max("wood_wall") == -1
                        && PartDesigns.folder("centrifuge_part").equals("centrifugeparts"),
                "design support follows the original part table");
        var designed = StructureLayout.builder("samplepress")
                .cell(1, 0, 0, CellMatch.parse("steel_wall"), StructureRole.CASING, 7).build();
        check(designed.cells().get(0).design() == 7, "a cell can name a design");
        var json = StructureLayoutDocuments.canonical(designed);
        check(json.contains("\"design\":7") && StructureLayoutDocuments.parse("samplepress", json).equals(designed),
                "a layout document keeps the design");
        var shared = AddonMachines.sharedCells("test", designed);
        check(shared.size() == 1 && shared.get(0).design() == 7, "install keeps a supported design");
        fails(() -> AddonMachines.sharedCells("test", StructureLayout.builder("samplepress")
                .cell(1, 0, 0, CellMatch.parse("steel_wall"), StructureRole.CASING, 8).build()),
                IllegalStateException.class, "does not support");
        fails(() -> AddonMachines.sharedCells("test", StructureLayout.builder("samplepress")
                .cell(1, 0, 0, CellMatch.parse("wood_wall"), StructureRole.CASING, 1).build()),
                IllegalStateException.class, "does not support");
        fails(() -> AddonMachines.sharedCells("test", StructureLayout.builder("samplepress")
                .cell(1, 0, 0, CellMatch.parse("minecraft:iron_block"), StructureRole.CASING, 1).build()),
                IllegalStateException.class, "does not support");
        fails(() -> LayoutCell.at(1, 0, 0, CellMatch.parse("steel_wall"), StructureRole.CASING).withDesign(-1),
                "0 to 255");
    }

    /** The sample compute part, without a block entity: four stack-1 slots, redstone 1, diamond 4. */
    private static final class ComputeModel {
        private final String[] items = new String[4];
        private int power;
        private boolean running;

        int power() { return power; }
        boolean running() { return running; }
        String item(int slot) { return items[slot] == null ? "" : items[slot]; }

        boolean insert(int slot, String item) {
            if (running || slot < 0 || slot >= items.length || item == null || item.isEmpty() || items[slot] != null)
                return false;
            items[slot] = item;
            recount();
            return true;
        }

        String extract(int slot) {
            if (running || slot < 0 || slot >= items.length || items[slot] == null) return "";
            String taken = items[slot];
            items[slot] = null;
            recount();
            return taken;
        }

        boolean tryStart(int needed) {
            if (power < needed) return false;
            running = true;
            return true;
        }

        void stop() { running = false; }

        private void recount() {
            power = 0;
            for (String item : items) {
                if ("redstone".equals(item)) power += 1;
                else if ("diamond".equals(item)) power += 4;
            }
        }
    }

    private static final String GUI = "aa_sample:textures/gui/machine.png";

    private static MachineCasing casing(String id) {
        return MachineCasing.builder(id, "Steel").build();
    }

    private static EnergyInput eu(long perTick) {
        return EnergyInput.builder("EU", perTick).build();
    }

    private static EnergyOutput emitted() {
        return EnergyOutput.builder("RU", 32, 1).faces(MachineFace.BACK).build();
    }

    private static FuelOutput fuelPacket() {
        return FuelOutput.builder("RU", 32).build();
    }

    private static RecipeMachine recipe(EnergyInput energy) {
        return recipe(casing("press"), energy);
    }

    private static RecipeMachine recipe(MachineCasing casing, EnergyInput energy) {
        return RecipeMachine.builder(casing).energy(energy).recipeMap("aa.recipe.press").build();
    }

    private static BehaviorMachine behavior(MachineCasing casing, EnergyInput energy) {
        return BehaviorMachine.builder(casing).energy(energy).gui(GUI).build();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        assertions++;
    }

    private static void fails(Runnable action, String part) {
        fails(action, IllegalArgumentException.class, part);
    }

    private static void fails(Runnable action, Class<? extends RuntimeException> type, String part) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            if (!type.isInstance(ex) || (part != null && (ex.getMessage() == null || !ex.getMessage().contains(part))))
                throw new AssertionError(ex.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
            assertions++;
            return;
        }
        throw new AssertionError("expected failure containing " + part);
    }
}
