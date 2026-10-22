package com.gregtech.gregtech.core;

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

        fails(() -> behavior(casing, EnergyInput.builder("EU", 32).range(1, 64).build()), "energy range");
        fails(() -> BehaviorMachine.builder(casing).energy(energy).gui(GUI).io(io).build(), "item and fluid");
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
        fails(() -> behavior(casing("press"), EnergyInput.builder("EU", 32).anyPacket().build()), "packet size");

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
        fails(() -> BehaviorMachine.builder(casing("deburn")).energy(eu(32)).gui(GUI)
                .auxiliaryEnergy(EnergyInput.builder("HU", 32).anyPacket().capacity(50_000).faces(MachineFace.TOP).build())
                .build(), "overlaps");
        fails(() -> BehaviorMachine.builder(casing("deburn")).energy(eu(32)).gui(GUI)
                .auxiliaryEnergy(EnergyInput.builder("HU", 32).capacity(50_000).faces(MachineFace.FRONT).build()).build(),
                "anyPacket");
        fails(() -> BehaviorMachine.builder(casing("deburn")).energy(eu(32)).gui(GUI)
                .auxiliaryEnergy(EnergyInput.builder("HU", 32).anyPacket().faces(MachineFace.FRONT).build()).build(),
                "positive");

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
