package gregtech.blockentity.machine;

import gregtech.api.fluid.FluidPort;
import gregtech.api.multiblock.MultiblockLayout;
import gregtech.api.multiblock.MultiblockPortOwner;
import gregtech.api.multiblock.PartBindings;
import gregtech.content.multiblock.LargeMachineLayouts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

import javax.annotation.Nullable;
import java.util.LinkedHashMap;

/** Multiblock fuel generator. The structure is an existing large-machine layout of GregTech parts. */
public class FuelTurbineBlockEntity extends FuelGeneratorBlockEntity implements MultiblockPortOwner,
        gregtech.api.multiblock.tracking.TrackedMultiblock {
    private final PartBindings<BlockPos, MultiblockLayout.Role> bindings = new PartBindings<>();

    public FuelTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        formed = false;
    }

    @Override
    protected boolean refreshStructure() { return isStructureOk(); }

    @Override
    protected boolean acceptsFluid(@Nullable Direction side) { return side == null || side == front(); }

    public java.util.List<LargeMachineLayouts.Cell> trackedCells() {
        return LargeMachineLayouts.cells(declaration().casing().machineType());
    }

    @Override public gregtech.api.multiblock.tracking.StructureDefinition structureDefinition() {
        return gregtech.api.multiblock.tracking.CustomStructures.fuel(this);
    }
    @Override public boolean isStructureOk() {
        return gregtech.api.multiblock.tracking.StructureTracking.query(this);
    }
    @Override public void structureFormedChanged(boolean now) { formed = now; }
    @Override
    public boolean scanStructure() {
        if (level == null || isRemoved()) {
            formed = false;
            return false;
        }
        var layout = LargeMachineLayouts.cells(declaration().casing().machineType());
        if (layout == null) {
            bindings.clear(this::release);
            gregtech.api.multiblock.tracking.StructureClaims.cleared(this);
            formed = false;
            return false;
        }
        var front = front();
        var parts = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
        for (var cell : layout) {
            BlockPos pos = cell.at(worldPosition, front);
            if (!level.hasChunkAt(pos)) {
                bindings.clear(this::release);
                gregtech.api.multiblock.tracking.StructureClaims.cleared(this);
                formed = false;
                return false;
            }
            if (cell.role() == MultiblockLayout.Role.AIR) {
                if (level.isEmptyBlock(pos)) continue;
                bindings.clear(this::release);
                gregtech.api.multiblock.tracking.StructureClaims.cleared(this);
                formed = false;
                return false;
            }
            if (cell.plainBlock()) {
                if (!cell.matches(level, pos, level.getBlockState(pos))) {
                    bindings.clear(this::release);
                    gregtech.api.multiblock.tracking.StructureClaims.cleared(this);
                    formed = false;
                    return false;
                }
                continue;
            }
            if (level.getBlockState(pos).getBlock() != cell.block()
                    || !(level.getBlockEntity(pos) instanceof gregtech.blockentity.machine.MultiblockPortBlockEntity)) {
                bindings.clear(this::release);
                gregtech.api.multiblock.tracking.StructureClaims.cleared(this);
                formed = false;
                return false;
            }
            parts.put(pos, cell.role());
        }
        var claimed = gregtech.api.multiblock.tracking.StructureClaims.update(this, true, bindings, parts,
                fuelDesignHash(layout),
                pos -> ((gregtech.blockentity.machine.MultiblockPortBlockEntity) level.getBlockEntity(pos)).canBind(worldPosition),
                (pos, role) -> ((gregtech.blockentity.machine.MultiblockPortBlockEntity) level.getBlockEntity(pos)).bind(worldPosition, role),
                this::release);
        formed = claimed != gregtech.api.multiblock.tracking.StructureClaims.Result.FAILED;
        if (claimed == gregtech.api.multiblock.tracking.StructureClaims.Result.APPLIED)
            gregtech.content.multiblock.StructureDesigns.apply(level, worldPosition, front, layout);
        return formed;
    }

    private static int fuelDesignHash(java.util.List<LargeMachineLayouts.Cell> cells) {
        int hash = 1;
        for (var cell : cells) hash = 31 * hash + cell.design();
        return hash;
    }

    private void release(BlockPos pos) {
        if (level != null && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof gregtech.blockentity.machine.MultiblockPortBlockEntity part)
            part.release(worldPosition);
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) gregtech.api.multiblock.tracking.StructureTracking.attach(this);
    }
    @Override public void onChunkUnloaded() {
        gregtech.api.multiblock.tracking.StructureTracking.onChunkUnloaded(this);
        super.onChunkUnloaded();
    }
    @Override
    public void setRemoved() {
        if (!gregtech.api.multiblock.tracking.StructureTracking.unloading(this)) bindings.clear(this::release);
        gregtech.api.multiblock.tracking.StructureTracking.onRemoved(this);
        super.setRemoved();
    }

    @Override
//? if >=1.21.1 {
    /*public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        net.minecraft.nbt.CompoundTag tag = super.getUpdateTag(lookup);
*///?} else {
    public net.minecraft.nbt.CompoundTag getUpdateTag() {
        net.minecraft.nbt.CompoundTag tag = super.getUpdateTag();
//?}
        gregtech.api.multiblock.tracking.ClientFormedSync.write(this, tag);
        return tag;
    }

    @Override
//? if >=1.21.1 {
    /*public void handleUpdateTag(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.handleUpdateTag(tag, lookup);
*///?} else {
    public void handleUpdateTag(net.minecraft.nbt.CompoundTag tag) {
        super.handleUpdateTag(tag);
//?}
        gregtech.api.multiblock.tracking.ClientFormedSync.read(this, tag);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public IFluidHandler portFluids(MultiblockLayout.Role role) {
        boolean input = input(role);
        boolean output = output(role);
        if (!input && !output) return null;
        return new FluidPort(this, input, output);
    }

    private static boolean input(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.FLUID_INPUT || role == MultiblockLayout.Role.FLUID_IO
                || role == MultiblockLayout.Role.ITEM_FLUID_IO || role == MultiblockLayout.Role.ITEM_FLUID_INPUT
                || role == MultiblockLayout.Role.ITEM_FLUID_ENERGY_INPUT || role == MultiblockLayout.Role.ITEM_FLUID_ENERGY;
    }

    private static boolean output(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.FLUID_OUTPUT || role == MultiblockLayout.Role.FLUID_IO
                || role == MultiblockLayout.Role.ITEM_FLUID_IO || role == MultiblockLayout.Role.ITEM_FLUID_OUTPUT
                || role == MultiblockLayout.Role.ITEM_FLUID_ENERGY;
    }
}
