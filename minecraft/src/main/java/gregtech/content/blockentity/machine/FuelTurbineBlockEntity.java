package gregtech.content.blockentity.machine;

import gregtech.engine.fluid.FluidPort;
import gregtech.engine.multiblock.MultiblockLayout;
import gregtech.engine.multiblock.MultiblockPortOwner;
import gregtech.engine.multiblock.PartBindings;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Multiblock fuel generator. The structure is an existing large-machine layout of GregTech parts. */
public class FuelTurbineBlockEntity extends FuelGeneratorBlockEntity implements MultiblockPortOwner,
        gregtech.engine.multiblock.tracking.TrackedMultiblock {
    private final PartBindings<BlockPos, MultiblockLayout.Role> bindings = new PartBindings<>();
    private final gregtech.engine.multiblock.tracking.StructureDefinition definition =
            gregtech.engine.multiblock.tracking.FixedStructures.declarative(this, this::structureCells, List.of());

    public FuelTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        formed = false;
    }

    @Override
    protected boolean refreshStructure() {
        return formed;
    }

    @Override
    protected boolean acceptsFluid(@Nullable Direction side) { return side == null || side == front(); }

    public java.util.List<LargeMachineLayouts.Cell> trackedCells() {
        return LargeMachineLayouts.cells(declaration().casing().machineType());
    }

    @Override public gregtech.engine.multiblock.tracking.StructureDefinition structureDefinition() {
        return definition;
    }
    private List<gregtech.engine.multiblock.tracking.CompiledPattern.FixedCell> structureCells(Direction front) {
        var layout = trackedCells();
        if (layout == null) return List.of();
        var cells = new ArrayList<gregtech.engine.multiblock.tracking.CompiledPattern.FixedCell>(layout.size());
        for (var cell : layout) {
            var offset = cell.at(BlockPos.ZERO, front);
            cells.add(new gregtech.engine.multiblock.tracking.CompiledPattern.FixedCell(
                    offset.getX(), offset.getY(), offset.getZ(), cell));
        }
        return cells;
    }
    @Override public boolean applyStructure(gregtech.engine.multiblock.tracking.PatternMatch match) {
        var layout = trackedCells();
        if (level == null || isRemoved() || layout == null) {
            releaseStructureClaims();
            return false;
        }
        var parts = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
        for (var fixed : match.fixedCells()) {
            var cell = fixed.cell();
            if (cell.role() != MultiblockLayout.Role.AIR && !cell.plainBlock())
                parts.put(fixed.pos(), cell.role());
        }
        var claimed = gregtech.engine.multiblock.tracking.StructureClaims.update(this, bindings, parts,
                pos -> ((gregtech.content.blockentity.machine.MultiblockPortBlockEntity) level.getBlockEntity(pos)).canBind(worldPosition),
                (pos, role) -> ((gregtech.content.blockentity.machine.MultiblockPortBlockEntity) level.getBlockEntity(pos)).bind(worldPosition, role),
                this::release);
        if (claimed == gregtech.engine.multiblock.tracking.StructureClaims.Result.APPLIED)
            gregtech.content.multiblock.StructureDesigns.apply(level, worldPosition, front(), layout);
        return claimed != gregtech.engine.multiblock.tracking.StructureClaims.Result.FAILED;
    }
    /** Placed housing parts stay bound while the machine is incomplete so a wand click keeps building. */
    @Override public void applyIncompleteStructure(gregtech.engine.multiblock.tracking.PatternMatch match) {
        if (level == null || isRemoved()) return;
        gregtech.engine.multiblock.tracking.StructureClaims.bindMatched(this, bindings, match, false, this::release);
    }
    @Override public void releaseStructureClaims() {
        bindings.clear(this::release);
        gregtech.engine.multiblock.tracking.StructureClaims.cleared(this);
    }
    @Override public boolean isStructureOk() {
        return gregtech.engine.multiblock.tracking.StructureTracking.query(this);
    }
    @Override public void structureFormedChanged(boolean now) { formed = now; }

    private void release(BlockPos pos) {
        if (level != null && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity part)
            part.release(worldPosition);
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) gregtech.engine.multiblock.tracking.StructureTracking.attach(this);
    }
    @Override public void onChunkUnloaded() {
        gregtech.engine.multiblock.tracking.StructureTracking.onChunkUnloaded(this);
        super.onChunkUnloaded();
    }
    @Override
    public void setRemoved() {
        if (!gregtech.engine.multiblock.tracking.StructureTracking.unloading(this)) bindings.clear(this::release);
        gregtech.engine.multiblock.tracking.StructureTracking.onRemoved(this);
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
        gregtech.engine.multiblock.tracking.ClientFormedSync.write(this, tag);
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
        gregtech.engine.multiblock.tracking.ClientFormedSync.read(this, tag);
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
