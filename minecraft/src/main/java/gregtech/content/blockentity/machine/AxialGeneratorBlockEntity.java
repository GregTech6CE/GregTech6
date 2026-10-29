package gregtech.content.blockentity.machine;

import gregtech.engine.energy.EnergyBlockViews;
import gregtech.engine.energy.IEnergyBlock;
import gregtech.engine.multiblock.*;
import gregtech.content.blockentity.GTEnergyBlockEntity;
import gregtech.content.multiblock.AxialGeneratorDefinitions;
import gregtech.content.multiblock.AxialStructureTransform;
import gregtech.content.data.GregTechTags;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}
import java.util.*;

/** Shared solid 3x3x4 assembly and rear shaft output. Storage belongs only to the controller. */
public abstract class AxialGeneratorBlockEntity extends GTEnergyBlockEntity implements MultiblockPortOwner,
        gregtech.api.platform.machine.MachineControl.Provider, gregtech.engine.multiblock.tracking.TrackedMultiblock {
    protected long energy;
    protected boolean stopped, overloaded;
    private boolean converting, emitted;
    private boolean rotorFormed, rotorActive;
    private final gregtech.api.platform.machine.MachineControl control =
            gregtech.engine.machine.MachineControls.switchable(this,
                    () -> !this.stopped,
                    value -> {
                        this.stopped = !value || this.overloaded;
                        setChanged();
                    },
                    () -> this.converting && !this.overloaded && structureOk(),
                    () -> this.emitted && !this.overloaded && structureOk());

    @Override
    public gregtech.api.platform.machine.MachineControl machineControl(Direction side) {
        return control;
    }

    private final PartBindings<BlockPos, MultiblockLayout.Role> bindings = new PartBindings<>();
    private boolean structureFormed;
    @Override public void structureFormedChanged(boolean formed) { structureFormed = formed; }
    protected boolean structureOk() {
        return structureFormed;
    }
    private final gregtech.engine.multiblock.tracking.StructureDefinition definition =
            gregtech.engine.multiblock.tracking.FixedStructures.declarative(this, this::structureCells, List.of());

    protected AxialGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public AxialGeneratorDefinitions.Grade grade() {
        return AxialGeneratorDefinitions.grade(getBlockState().getBlock());
    }

    public Direction front() {
        return getBlockState().getValue(DirectionalBlock.FACING);
    }

    public boolean isStopped() {
        return stopped;
    }

    public boolean isOverloaded() {
        return overloaded;
    }

    /** Synced visual state for the original GT6 large steam-turbine rotor. */
    public boolean isRotorFormed() {
        return rotorFormed;
    }

    public boolean isRotorActive() {
        return rotorActive;
    }

    protected boolean isConvertingThisTick() {
        return converting;
    }

    protected void updateRotorVisual(boolean formed, boolean active) {
        active &= formed;
        if (rotorFormed == formed && rotorActive == active) return;
        rotorFormed = formed;
        rotorActive = active;
        syncToClient();
    }

    public void toggleStopped() {
        if (overloaded) {
            overloaded = false;
            stopped = true;
            energy = 0;
        } else stopped = !stopped;
        setChanged();
    }

    protected void overload() {
        overloaded = true;
        stopped = true;
        energy = 0;
        setChanged();
    }

    public static MultiblockLayout.Role role(BlockPos cell, boolean steam) {
        return role(cell, Direction.NORTH, steam);
    }

    public static MultiblockLayout.Role role(BlockPos cell, Direction front, boolean steam) {
        return AxialStructureTransform.role(front, cell.getX(), cell.getY(), cell.getZ(), steam);
    }

    public java.util.Set<BlockPos> trackedCells() { return grade().cells().keySet(); }

    @Override public gregtech.engine.multiblock.tracking.StructureDefinition structureDefinition() {
        return definition;
    }
    private List<gregtech.engine.multiblock.tracking.CompiledPattern.FixedCell> structureCells(Direction front) {
        var cells = new java.util.ArrayList<gregtech.engine.multiblock.tracking.CompiledPattern.FixedCell>(
                grade().cells().size());
        for (var cell : grade().cells().keySet()) {
            boolean coil = gregtech.content.multiblock.OriginalGeneratorParameters.usesCopperCoil(
                    grade().steam(), cell.getZ());
            int part = coil ? 18040 : grade().materials().wallId();
            var role = AxialStructureTransform.role(front, cell.getX(), cell.getY(), cell.getZ(), grade().steam());
            var spec = new gregtech.content.multiblock.LargeMachineLayouts.Cell(
                    cell.getX(), cell.getY(), cell.getZ(), part, role);
            var offset = AxialStructureTransform.at(BlockPos.ZERO, front, cell.getX(), cell.getY(), cell.getZ());
            cells.add(new gregtech.engine.multiblock.tracking.CompiledPattern.FixedCell(
                    offset.getX(), offset.getY(), offset.getZ(), spec));
        }
        return cells;
    }
    @Override public boolean applyStructure(gregtech.engine.multiblock.tracking.PatternMatch match) {
        if (level == null || isRemoved()) return false;
        var parts = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
        for (var fixed : match.fixedCells()) {
            var cell = fixed.cell();
            if (cell.role() != MultiblockLayout.Role.AIR) parts.put(fixed.pos(), cell.role());
        }
        return gregtech.engine.multiblock.tracking.StructureClaims.update(this, bindings, parts,
                position -> level.getBlockEntity(position) instanceof MultiblockPortBlockEntity port
                        && port.canBind(worldPosition),
                (position, role) -> ((MultiblockPortBlockEntity) level.getBlockEntity(position)).bind(worldPosition, role),
                this::release) != gregtech.engine.multiblock.tracking.StructureClaims.Result.FAILED;
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

    private void release(BlockPos pos) {
        if (level != null && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part) part.release(worldPosition);
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

    protected GregTechTags.Tag outputType() {
        return grade().steam() ? GregTechTags.Energy.RU : GregTechTags.Energy.EU;
    }

    /** GT6 waste-energy converter: one variable-size packet per tick; unused energy dissipates. */
    protected void convertAndEmit() {
        converting = false;
        emitted = false;
        if (energy <= 0) return;
        long output = gregtech.content.multiblock.OriginalGeneratorParameters.convert(energy, grade().output(), grade().input());
        if (output > grade().outputMaximum()) {
            overload();
            return;
        }
        if (output >= grade().output() / 2) {
            converting = true;
            var target = worldPosition.relative(front().getOpposite(), 4);
            IEnergyBlock receiver = level.hasChunkAt(target) ? EnergyBlockViews.view(level.getBlockEntity(target)) : null;
            if (receiver != null) emitted = receiver.doEnergyInjection(outputType(), front(), output, 1, true) > 0;
        }
        energy = Math.max(0, energy - grade().inputMaximum());
        setChanged();
    }

    @Override
    public IFluidHandler portFluids(MultiblockLayout.Role role) {
        return null;
    }

    @Override
    public Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.ENERGY_OUTPUT ? List.of(outputType()) : List.of();
    }

    @Override
    public boolean emitsPortEnergy(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return role == MultiblockLayout.Role.ENERGY_OUTPUT && type == outputType();
    }

    @Override
    public long portEnergyOutputSize(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return emitsPortEnergy(role, type) ? grade().output() : 0;
    }
    // Output is exclusively pushed at the rear port: no second extractable buffer at the main housing.

    @Override
    public boolean isEnergyType(GregTechTags.Tag type, Direction side, boolean emitting) {
        return false;
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(Direction side) {
        return List.of();
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag type, Direction side, boolean theoretical) {
        return false;
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, Direction side, boolean theoretical) {
        return false;
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag type, Direction side) {
        return 0;
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag type, Direction side) {
        return type == outputType() ? grade().output() : 0;
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag type, Direction side, long size) {
        return 0;
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag type, Direction side, long size) {
        return 0;
    }

    @Override
    public long doInject(GregTechTags.Tag type, Direction side, long size, long amount, boolean execute) {
        return 0;
    }

    @Override
    public long getEnergyStored(GregTechTags.Tag type, Direction side) {
        return 0;
    }

    @Override
    public long getEnergyCapacity(GregTechTags.Tag type, Direction side) {
        return 0;
    }
//? if >=1.21.1 {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putLong("gt.axial.energy", energy);
        tag.putBoolean("gt.stopped", stopped);
        tag.putBoolean("gt.overloaded", overloaded);
        tag.putBoolean("gt.rotor_formed", rotorFormed);
        tag.putBoolean("gt.rotor_active", rotorActive);
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        energy = Math.max(0, Math.min(grade().inputMaximum(), tag.getLong("gt.axial.energy")));
        stopped = tag.getBoolean("gt.stopped");
        overloaded = tag.getBoolean("gt.overloaded");
        rotorFormed = tag.getBoolean("gt.rotor_formed");
        rotorActive = rotorFormed && tag.getBoolean("gt.rotor_active");
    }
*///?} else {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("gt.axial.energy", energy);
        tag.putBoolean("gt.stopped", stopped);
        tag.putBoolean("gt.overloaded", overloaded);
        tag.putBoolean("gt.rotor_formed", rotorFormed);
        tag.putBoolean("gt.rotor_active", rotorActive);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = Math.max(0, Math.min(grade().inputMaximum(), tag.getLong("gt.axial.energy")));
        stopped = tag.getBoolean("gt.stopped");
        overloaded = tag.getBoolean("gt.overloaded");
        rotorFormed = tag.getBoolean("gt.rotor_formed");
        rotorActive = rotorFormed && tag.getBoolean("gt.rotor_active");
    }
//?}
}
