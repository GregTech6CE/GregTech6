package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.addon.BehaviorEnergyRules;
import com.gregtech.gregtech.api.addon.EnergyOutput;
import com.gregtech.gregtech.api.addon.MachineFace;
import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import com.gregtech.gregtech.block.machine.AddonEnergyPartBlock;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;
import java.util.List;

/**
 * Energy buffer owned by one controller coordinate. Emission is push-only and only while the
 * cell's role is {@link MultiblockLayout.Role#ENERGY_OUTPUT} and {@link #setOutput} has armed it.
 */
public class AddonEnergyPartBlockEntity extends MultiblockPortBlockEntity {
    private long stored;
    private long outputSize;
    private long outputAmount;
    private boolean stopped = true;
    private GregTechTags.Tag energyTag;

    public AddonEnergyPartBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.ADDON_ENERGY_PART.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AddonEnergyPartBlockEntity part) {
        part.emitTick();
    }

    public long stored() { return stored; }

    public long capacity() { return block().capacity(); }

    public String path() {
        var id = BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock());
        return id == null ? "" : id.getPath();
    }

    /** Adds up to the free space. The caller decides whether this part is bound. */
    public long charge(long amount) {
        long accepted = accept(stored, amount);
        if (accepted <= 0) return 0;
        stored += accepted;
        setChanged();
        return accepted;
    }

    public void setOutput(long size, long amount) {
        BehaviorEnergyRules.requireEmit(size, amount, energy().maxSize(), energy().maxAmount());
        if (boundRole() != MultiblockLayout.Role.ENERGY_OUTPUT)
            throw new IllegalStateException("Energy part is not an ENERGY_OUTPUT cell");
        outputSize = size;
        outputAmount = amount;
        stopped = false;
        setChanged();
    }

    public void stop() {
        if (!stopped) {
            stopped = true;
            setChanged();
        }
    }

    @Override public boolean canBind(BlockPos owner) {
        BlockPos held = controllerPos();
        if (held == null || owner.equals(held)) return true;
        if (level == null || !level.hasChunkAt(held)) return true;
        var occupant = level.getBlockEntity(held);
        if (occupant == null || occupant.isRemoved()) return true;
        if (occupant instanceof BehaviorMachineBlockEntity) return false;
        return super.canBind(owner);
    }

    @Override public void release(BlockPos owner) {
        boolean mine = isBoundTo(owner);
        super.release(owner);
        if (mine) stop();
    }

    private void emitTick() {
        if (level == null || level.isClientSide || stopped) return;
        if (boundRole() != MultiblockLayout.Role.ENERGY_OUTPUT || !controllerLive()) {
            stop();
            return;
        }
        if (outputSize < 1 || outputAmount < 1 || stored < outputSize) return;
        long left = Math.min(outputAmount, stored / outputSize);
        long sent = 0;
        for (MachineFace face : energy().faces()) {
            if (left <= 0) break;
            long used = EnergyTransfer.emitEnergyToSide(energyTag(), world(face), outputSize, left, this);
            sent += used;
            left -= used;
        }
        if (sent > 0) {
            stored -= Math.multiplyExact(outputSize, sent);
            setChanged();
        }
    }

    private boolean controllerLive() {
        BlockPos owner = controllerPos();
        if (owner == null || level == null || !level.hasChunkAt(owner)) return false;
        return level.getBlockEntity(owner) instanceof BehaviorMachineBlockEntity machine
                && !machine.isRemoved() && machine.structureFormed();
    }

    private AddonEnergyPartBlock block() {
        return (AddonEnergyPartBlock) getBlockState().getBlock();
    }

    private EnergyOutput energy() { return block().energy(); }

    /** How much of {@code amount} fits in the remaining capacity. Does not change {@code stored}. */
    private long accept(long stored, long amount) {
        if (amount < 0) throw new IllegalArgumentException("negative charge");
        long cap = capacity();
        long held = Math.max(0, Math.min(stored, cap));
        return Math.min(amount, cap - held);
    }

    private GregTechTags.Tag energyTag() {
        if (energyTag != null) return energyTag;
        String name = energy().type();
        energyTag = GregTechTags.Energy.ALL.stream().filter(tag -> tag.getShortName().equalsIgnoreCase(name))
                .findFirst().orElseThrow(() -> new IllegalStateException("Unknown energy type: " + name));
        return energyTag;
    }

    private boolean outputSide(Direction side) {
        if (side == null) return false;
        for (MachineFace face : energy().faces())
            if (world(face) == side) return true;
        return false;
    }

    private Direction world(MachineFace face) {
        Direction facing = getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        return switch (face) {
            case BOTTOM -> Direction.DOWN;
            case TOP -> Direction.UP;
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
        };
    }

    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side) {
        return outputSide(side) || side == null ? List.of(energyTag()) : List.of();
    }

    @Override public boolean isEnergyType(GregTechTags.Tag type, Direction side, boolean emitting) {
        return type != null && type.equals(energyTag()) && (side == null || outputSide(side));
    }

    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, Direction side, boolean theoretical) {
        return false;
    }

    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type, Direction side, boolean theoretical) {
        return !stopped && boundRole() == MultiblockLayout.Role.ENERGY_OUTPUT && controllerLive()
                && isEnergyType(type, side, true);
    }

    @Override public long getEnergyOffered(GregTechTags.Tag type, Direction side, long size) {
        if (!isEnergyEmittingTo(type, side, false) || size != outputSize || outputSize < 1) return 0;
        return Math.min(outputAmount, stored / outputSize);
    }

    @Override public long getEnergyStored(GregTechTags.Tag type, Direction side) {
        return type != null && type.equals(energyTag()) ? stored : 0;
    }

    @Override public long getEnergyCapacity(GregTechTags.Tag type, Direction side) {
        return type != null && type.equals(energyTag()) ? capacity() : 0;
    }

    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type, Direction side) {
        return isEnergyEmittingTo(type, side, true) ? outputSize : 0;
    }

    @Override public long doInject(GregTechTags.Tag type, Direction side, long size, long amount, boolean execute) {
        return 0;
    }

    private void saveEnergy(CompoundTag tag) {
        tag.putLong("gt.part_energy", stored);
        tag.putLong("gt.part_output", outputSize);
        tag.putLong("gt.part_amount", outputAmount);
        tag.putBoolean("gt.part_stopped", stopped);
    }

    private void loadEnergy(CompoundTag tag) {
        stored = Math.max(0, tag.getLong("gt.part_energy"));
        if (stored > capacity()) stored = capacity();
        outputSize = Math.max(0, tag.getLong("gt.part_output"));
        outputAmount = Math.max(0, tag.getLong("gt.part_amount"));
        stopped = !tag.contains("gt.part_stopped") || tag.getBoolean("gt.part_stopped");
    }

//? if >=1.21.1 {
    /*@Override protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        saveEnergy(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        loadEnergy(tag);
    }
*///?} else {
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveEnergy(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        loadEnergy(tag);
    }
//?}
}
