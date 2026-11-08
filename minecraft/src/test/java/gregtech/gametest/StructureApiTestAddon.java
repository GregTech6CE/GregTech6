package gregtech.gametest;

import gregtech.api.addon.machine.*;
import gregtech.api.addon.platform.*;
import gregtech.api.energy.EnergyInput;
import gregtech.api.energy.EnergyOutput;
import gregtech.api.energy.EnergyType;
import gregtech.api.machine.MachineCasing;
import gregtech.api.machine.MachineFace;
import gregtech.api.multiblock.*;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** A test-only external consumer: its declaration and behavior import no host implementation classes. */
public final class StructureApiTestAddon implements AddonMachineContributor, BehaviorMachineContributor {
    @Override public String addonId() { return "gt6ce_api_test"; }
    @Override public Set<String> machineTypes() { return Set.of("structure_api_test", "ports_api_test", "ports_api_receiver"); }

    @Override public List<AddonMachine> addonMachines() {
        var coils = CellMatch.oneOf(CellMatch.part("steel_wall"), CellMatch.part("titanium_wall"));
        var layout = StructureLayout.pattern("api_test_shape").layer(0, "SABC").ignore('.')
                .repeat("upper", 1, 1, 2, ".ABC")
                .where('A', coils, StructureRole.ENERGY_INPUT, 3)
                .where('B', CellMatch.oneOf(CellMatch.block("minecraft:stone"), CellMatch.block("minecraft:dirt")),
                        StructureRole.CASING)
                .where('C', CellMatch.tag("minecraft:logs"), StructureRole.CASING)
                .require(StructureConstraint.sameBlock(coils))
                .require(StructureConstraint.sameBlock(CellMatch.tag("minecraft:logs")))
                .require(StructureConstraint.exactly(CellMatch.block("minecraft:stone"), 2)).build();
        return List.of(BehaviorMachine.builder(MachineCasing.builder("structure_api_test", "Iron").build())
                .energy(EnergyInput.builder(EnergyType.EU, 32).build())
                .gui("minecraft:textures/gui/container/furnace.png").build().withStructure(layout), portsMachine(),
                BehaviorMachine.builder(MachineCasing.builder("ports_api_receiver", "Iron").build())
                        .energy(EnergyInput.builder(EnergyType.EU, 32).capacity(1024).faces(MachineFace.values()).build())
                        .item(30, 9, BehaviorMachine.SlotRole.INPUT)
                        .fluid(1000, 30, 90, BehaviorMachine.SlotRole.INPUT)
                        .gui("minecraft:textures/gui/container/furnace.png").guiSize(176, 256).build(), fuelMachine());
    }

    /** Single-block fixture for the fuel generator's attachment access. */
    private static AddonMachine fuelMachine() {
        return FuelGenerator.builder(MachineCasing.builder("fuel_api_test", "Iron").build())
                .fuelMap("gt.recipe.fuels.engine")
                .output(FuelOutput.builder(EnergyType.EU, 32).build())
                .tank(4000)
                .single()
                .build();
    }

    static StructureLayout portsLayout() {
        var ports = StructurePorts.builder()
                .itemInput(PortRule.faces(MachineFace.FRONT).slots(1).filter("minecraft:iron_ingot"))
                .itemOutput(PortRule.faces(MachineFace.BACK).slots(3).filter("minecraft:coal"))
                .fluidInput(PortRule.faces(MachineFace.FRONT).slots(1).filter("#minecraft:water"))
                .fluidOutput(PortRule.faces(MachineFace.BACK).slots(3).filter("minecraft:lava")).build();
        return StructureLayout.pattern("api_test_ports").layer(0, "SAB", "C..").ignore('.')
                .repeat("upper", 1, 1, 2, ".A.", "...")
                .where('A', CellMatch.part("steel_wall"), StructureRole.ITEM_FLUID_IO, ports)
                .where('B', CellMatch.part("steel_wall"), StructureRole.ITEM_FLUID_ENERGY,
                        StructurePorts.builder().energyInput(PortRule.faces(MachineFace.LEFT)).energyOutput(PortRule.NONE).build())
                .where('C', CellMatch.part("steel_wall"), StructureRole.ENERGY_OUTPUT,
                        StructurePorts.builder().energyOutput(PortRule.faces(MachineFace.BACK)).build()).build();
    }

    private static AddonMachine portsMachine() {
        return BehaviorMachine.builder(MachineCasing.builder("ports_api_test", "Iron").build())
                .energy(EnergyInput.builder(EnergyType.EU, 32).capacity(512).build())
                .energyOutput(EnergyOutput.builder(EnergyType.EU, 32, 1).remote().build())
                .gui("minecraft:textures/gui/container/furnace.png").guiSize(176, 256)
                .item(30, 9, BehaviorMachine.SlotRole.INPUT).item(48, 9, BehaviorMachine.SlotRole.INPUT)
                .item(30, 27, BehaviorMachine.SlotRole.OUTPUT).item(48, 27, BehaviorMachine.SlotRole.OUTPUT)
                .fluid(1000, 30, 90, BehaviorMachine.SlotRole.INPUT)
                .fluid(1000, 50, 90, BehaviorMachine.SlotRole.INPUT)
                .fluid(1000, 70, 90, BehaviorMachine.SlotRole.OUTPUT)
                .fluid(1000, 90, 90, BehaviorMachine.SlotRole.OUTPUT)
                .build().withStructure(portsLayout());
    }

    @Override public MachineBehavior create(String machineType) {
        return machineType.equals("ports_api_test") ? new PortsBehavior()
                : machineType.equals("ports_api_receiver") ? new ReceiverBehavior() : new Behavior();
    }

    public static final class ReceiverBehavior implements MachineBehavior {
        @Override public void tick(MachineBehaviorContext context) {}
        @Override public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) { return true; }
        @Override public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) { return true; }
    }

    public static final class PortsBehavior implements MachineBehavior {
        boolean seed, seeded;
        boolean emit;
        long emitted = -1;
        net.minecraft.core.BlockPos directTarget;
        MachineFace itemFace, fluidFace, energyFace;
        @Override public void tick(MachineBehaviorContext context) {
            if (emit) {
                emit = false;
                context.energy().generateMain(32);
                emitted = (directTarget == null ? context.energy().emitFromPorts(32, 1)
                        : context.energy().emitTo(directTarget, net.minecraft.core.Direction.DOWN, 32, 1)).transferredUnits();
            }
            if (!seed) return;
            seed = false;
            context.inventory().set(2, new ItemStack(Items.IRON_INGOT, 2));
            context.inventory().set(3, new ItemStack(Items.COAL, 4));
            context.fluids().fill(2, new FluidStack(Fluids.WATER, 300), false);
            context.fluids().fill(3, new FluidStack(Fluids.LAVA, 400), false);
            seeded = true;
        }
        @Override public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) { return stack.is(Items.IRON_INGOT); }
        @Override public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) {
            return stack.getFluid() == Fluids.WATER;
        }
        @Override public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack, MachineFace face) {
            itemFace = face;
            return true;
        }
        @Override public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack, MachineFace face) {
            fluidFace = face;
            return canFill(context, tank, stack);
        }
        @Override public boolean acceptsEnergy(MachineBehaviorContext context, EnergyType type, MachineFace face,
                                               long size, long amount) {
            energyFace = face;
            return true;
        }
    }

    public static final class Behavior implements MachineBehavior {
        int formations, losses, repeats;
        boolean ordered = true;
        boolean diagnose;
        List<StructureProblem> problems = List.of();
        List<StructureCell> cells = List.of();

        @Override public void tick(MachineBehaviorContext context) {
            cells = context.structure().cells();
            repeats = context.structure().repeats("upper");
            if (diagnose) {
                diagnose = false;
                problems = context.structure().diagnose();
            }
        }
        @Override public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) { return false; }
        @Override public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) { return false; }
        @Override public void onStructureFormed(MachineBehaviorContext context) {
            formations++;
            ordered &= context.structure().formed() && !context.structure().cells().isEmpty();
        }
        @Override public void onStructureBroken(MachineBehaviorContext context) {
            losses++;
            ordered &= !context.structure().formed() && context.structure().cells().isEmpty()
                    && context.structure().parts().isEmpty() && context.structure().energyParts().isEmpty();
        }
    }
}
