package gregtech.content.blockentity.machine;

import gregtech.api.addon.platform.BehaviorState;
import gregtech.api.addon.platform.EnergyAccess;
import gregtech.api.addon.platform.FluidAccess;
import gregtech.api.addon.platform.MachineBehaviorContext;
import gregtech.api.addon.platform.MachineInventoryAccess;
import gregtech.api.addon.platform.MachinePanel;
import gregtech.api.addon.platform.StructureAccess;
import gregtech.api.addon.platform.WorldAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

final class BehaviorMachineContext implements MachineBehaviorContext {
    private final BehaviorMachineBlockEntity machine;
    private final MachineInventoryAccess inventory;
    private final FluidAccess fluids;
    private final EnergyAccess energy;
    private final StructureAccess structure;
    private final MachinePanel panel;
    private final WorldAccess world;

    BehaviorMachineContext(
            BehaviorMachineBlockEntity machine,
            MachineInventoryAccess inventory,
            FluidAccess fluids,
            EnergyAccess energy,
            StructureAccess structure,
            MachinePanel panel,
            WorldAccess world) {
        this.machine = machine;
        this.inventory = inventory;
        this.fluids = fluids;
        this.energy = energy;
        this.structure = structure;
        this.panel = panel;
        this.world = world;
    }

    @Override
    public Level level() {
        return machine.getLevel();
    }

    @Override
    public BlockPos pos() {
        return machine.getBlockPos();
    }

    @Override
    public Direction facing() {
        return machine.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public int tier() {
        return machine.declaration().casing().tier();
    }

    @Override
    public MachineInventoryAccess inventory() {
        return inventory;
    }

    @Override
    public FluidAccess fluids() {
        return fluids;
    }

    @Override
    public EnergyAccess energy() {
        return energy;
    }

    @Override
    public StructureAccess structure() {
        return structure;
    }

    @Override
    public MachinePanel panel() {
        return panel;
    }

    @Override
    public WorldAccess world() {
        return world;
    }

    @Override
    public BehaviorState state() {
        return machine.state();
    }
}
