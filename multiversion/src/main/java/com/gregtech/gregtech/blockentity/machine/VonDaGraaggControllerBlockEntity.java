package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import com.gregtech.gregtech.api.multiblock.MultiblockPortOwner;
import com.gregtech.gregtech.api.multiblock.PartBindings;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.content.multiblock.VonDaGraaggSpawnInhibitor;
import com.gregtech.gregtech.content.multiblock.VonDaGraaggRules;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/** GT6 17996: 41 galvanized base walls, 5 copper coils and the dense-steel top. */
public final class VonDaGraaggControllerBlockEntity extends GTEnergyBlockEntity implements MultiblockPortOwner,
        com.gregtech.gregtech.api.sensor.ProgressSensorSource, com.gregtech.gregtech.api.sensor.CompressionSensorSource,
        com.gregtech.gregtech.api.multiblock.tracking.TrackedMultiblock {
    public static final long CAPACITY = VonDaGraaggRules.CAPACITY;
    private final PartBindings<BlockPos, MultiblockLayout.Role> bindings = new PartBindings<>();
    private final com.gregtech.gregtech.api.multiblock.tracking.StructureDefinition definition =
            com.gregtech.gregtech.api.multiblock.tracking.FixedStructures.declarative(this, this::structureCells, List.of());
    private long energy;
    private int range;

    public VonDaGraaggControllerBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.VON_DA_GRAAGG.get(), pos, state);
    }

    public long storedEnergy() {
        return energy;
    }

    public int currentRange() {
        return range;
    }

    @Override
    public long progressValue(int side) {
        return range;
    }

    @Override
    public long progressMaximum(int side) {
        return com.gregtech.gregtech.content.multiblock.VonDaGraaggRules.SENSOR_MAXIMUM;
    }

    @Override
    public long gibblValue(int side) {
        return 1000L * range;
    }

    @Override
    public long gibblMaximum(int side) {
        return 1000L * progressMaximum(side);
    }

    @Override public com.gregtech.gregtech.api.multiblock.tracking.StructureDefinition structureDefinition() {
        return definition;
    }
    private List<com.gregtech.gregtech.api.multiblock.tracking.CompiledPattern.FixedCell> structureCells(Direction front) {
        var defs = VonDaGraaggRules.layout();
        var cells = new java.util.ArrayList<com.gregtech.gregtech.api.multiblock.tracking.CompiledPattern.FixedCell>(defs.size());
        for (var cell : defs) {
            var role = cell.energyInput() ? MultiblockLayout.Role.ENERGY_INPUT : MultiblockLayout.Role.CASING;
            var spec = new com.gregtech.gregtech.content.multiblock.LargeMachineLayouts.Cell(
                    cell.x(), cell.y(), cell.z(), cell.partId(), role);
            cells.add(new com.gregtech.gregtech.api.multiblock.tracking.CompiledPattern.FixedCell(
                    cell.x(), cell.y(), cell.z(), spec));
        }
        return cells;
    }
    @Override public boolean applyStructure(com.gregtech.gregtech.api.multiblock.tracking.PatternMatch match) {
        if (level == null || isRemoved()) return false;
        var parts = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
        for (var fixed : match.fixedCells()) {
            var cell = fixed.cell();
            if (cell.role() != MultiblockLayout.Role.AIR) parts.put(fixed.pos(), cell.role());
        }
        return com.gregtech.gregtech.api.multiblock.tracking.StructureClaims.update(this, true, bindings, parts, 0,
                position -> level.getBlockEntity(position) instanceof MultiblockPortBlockEntity port
                        && port.canBind(worldPosition),
                (position, role) -> ((MultiblockPortBlockEntity) level.getBlockEntity(position)).bind(worldPosition, role),
                this::release) != com.gregtech.gregtech.api.multiblock.tracking.StructureClaims.Result.FAILED;
    }
    @Override public void releaseStructureClaims() {
        bindings.clear(this::release);
        com.gregtech.gregtech.api.multiblock.tracking.StructureClaims.cleared(this);
    }
    @Override public boolean isStructureOk() {
        return com.gregtech.gregtech.api.multiblock.tracking.StructureTracking.query(this);
    }
    @Override
    public boolean scanStructure() {
        if (level == null || isRemoved() || worldPosition.getY() + 7 >= level.getMaxBuildHeight())
            return invalid();
        var candidateParts = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
        for (var cell : VonDaGraaggRules.layout())
            if (!collectPart(candidateParts, cell.x(), cell.y(), cell.z(), cell.partId(),
                    cell.energyInput() ? MultiblockLayout.Role.ENERGY_INPUT : MultiblockLayout.Role.CASING)) return invalid();
        return com.gregtech.gregtech.api.multiblock.tracking.StructureClaims.update(this, true, bindings, candidateParts, 0,
                pos -> level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity port
                        && port.canBind(worldPosition),
                (pos, role) -> ((MultiblockPortBlockEntity) level.getBlockEntity(pos)).bind(worldPosition, role),
                this::release) != com.gregtech.gregtech.api.multiblock.tracking.StructureClaims.Result.FAILED;
    }

    private boolean collectPart(LinkedHashMap<BlockPos, MultiblockLayout.Role> candidateParts,
            int x, int y, int z, int originalId, MultiblockLayout.Role role) {
        BlockPos partPos = worldPosition.offset(x, y, z);
        if (!level.hasChunkAt(partPos) || !level.getBlockState(partPos).is(LargeMachineParts.block(originalId))
                || !(level.getBlockEntity(partPos) instanceof MultiblockPortBlockEntity)) return false;
        candidateParts.put(partPos, role);
        return true;
    }

    private boolean invalid() {
        bindings.clear(this::release);
        com.gregtech.gregtech.api.multiblock.tracking.StructureClaims.cleared(this);
        return false;
    }

    private void release(BlockPos pos) {
        if (level != null && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity port)
            port.release(worldPosition);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            VonDaGraaggControllerBlockEntity machine) {
        if (!(level instanceof ServerLevel server)) return;
        boolean formed = machine.isStructureOk();
        int nextRange = VonDaGraaggRules.range(machine.energy, formed);
        long nextEnergy = VonDaGraaggRules.afterDrain(machine.energy);
        boolean changed = nextRange != machine.range || nextEnergy != machine.energy;
        machine.range = nextRange;
        machine.energy = nextEnergy;
        if (machine.range > 0) VonDaGraaggSpawnInhibitor.register(server, pos);
        else VonDaGraaggSpawnInhibitor.unregister(server, pos);
        if (changed) machine.syncToClient();
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) com.gregtech.gregtech.api.multiblock.tracking.StructureTracking.attach(this);
    }
    @Override public void onChunkUnloaded() {
        com.gregtech.gregtech.api.multiblock.tracking.StructureTracking.onChunkUnloaded(this);
        super.onChunkUnloaded();
    }
    @Override
    public void setRemoved() {
        if (!com.gregtech.gregtech.api.multiblock.tracking.StructureTracking.unloading(this)) bindings.clear(this::release);
        com.gregtech.gregtech.api.multiblock.tracking.StructureTracking.onRemoved(this);
        if (level instanceof ServerLevel server) VonDaGraaggSpawnInhibitor.unregister(server, worldPosition);
        super.setRemoved();
    }

    /** Current port: EU enters the controller bottom or the base walls. */
    @Override
    public boolean isEnergyType(GregTechTags.Tag type, @Nullable Direction side, boolean emitting) {
        return !emitting && type == GregTechTags.Energy.ELECTRICITY && (side == null || side == Direction.DOWN);
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return side == null || side == Direction.DOWN ? List.of(GregTechTags.Energy.ELECTRICITY) : List.of();
    }

    @Override
    public boolean hasEnergySurface(@Nullable Direction side) {
        return side == null || side == Direction.DOWN;
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag type, @Nullable Direction side) {
        return 2048;
    }

    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag type, @Nullable Direction side) {
        return 256;
    }

    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag type, @Nullable Direction side) {
        return 4096;
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag type, @Nullable Direction side) {
        return 0;
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag type, @Nullable Direction side, long size) {
        return 0;
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag type, @Nullable Direction side, long size) {
        return type == GregTechTags.Energy.ELECTRICITY && size > 0 && size <= 4096
        ? Math.max(0, CAPACITY - energy) / size : 0;
    }

    @Override
    public long doInject(GregTechTags.Tag type, @Nullable Direction side,
            long size, long amount, boolean execute) {
        if (!isEnergyType(type, side, false) || size == Long.MIN_VALUE || amount <= 0) return 0;
        long packet = Math.abs(size);
        if (packet < 256 || packet > 4096) return 0;
        // GT6's doInject accumulates every same-tick packet and drains only 4096 EU per tick.
        // The reported capacitor size is 4096, but direct wall injection may temporarily exceed it.
        long accepted = Math.min(amount, (Long.MAX_VALUE - energy) / packet);
        if (execute && accepted > 0) {
            energy += packet * accepted;
            setChanged();
        }
        return accepted;
    }

    @Override
    public long getEnergyStored(GregTechTags.Tag type, @Nullable Direction side) {
        return type == GregTechTags.Energy.ELECTRICITY ? energy : 0;
    }

    @Override
    public long getEnergyCapacity(GregTechTags.Tag type, @Nullable Direction side) {
        return type == GregTechTags.Energy.ELECTRICITY ? CAPACITY : 0;
    }

    @Override
    public IFluidHandler portFluids(MultiblockLayout.Role role) {
        return null;
    }

    @Override
    public Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.ENERGY_INPUT ? List.of(GregTechTags.Energy.ELECTRICITY) : List.of();
    }

    @Override
    public long portEnergyInputRecommended(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return 2048;
    }

    @Override
    public long portEnergyInputMin(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return 256;
    }

    @Override
    public long portEnergyInputMax(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return 4096;
    }

    @Override
    public long portEnergyStored(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return getEnergyStored(type, null);
    }

    @Override
    public long portEnergyCapacity(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return getEnergyCapacity(type, null);
    }

    @Override
    public long injectPortEnergy(MultiblockLayout.Role role, GregTechTags.Tag type,
            long size, long amount, boolean execute) {
        return role == MultiblockLayout.Role.ENERGY_INPUT && isStructureOk()
        ? doInject(type, null, size, amount, execute) : 0;
    }

//? if >=1.21.1 {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
*///?} else {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
//?}
        tag.putLong("gt.energy", energy);
        tag.putInt("gt.range", range);
    }
//? if >=1.21.1 {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
*///?} else {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
//?}
        energy = Math.max(0, tag.getLong("gt.energy"));
        range = Math.max(0, Math.min(255, tag.getInt("gt.range")));
    }
}
