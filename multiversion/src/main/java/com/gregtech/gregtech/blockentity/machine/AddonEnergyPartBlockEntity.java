package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.addon.BehaviorEnergyRules;
import com.gregtech.gregtech.api.addon.EnergyInput;
import com.gregtech.gregtech.api.addon.EnergyOutput;
import com.gregtech.gregtech.api.addon.MachineFace;
import com.gregtech.gregtech.api.addon.PartEnergyRules;
import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.machine.BasicMachineEnergy;
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
 * Energy buffer owned by one controller coordinate. An output part pushes only while the
 * cell's role is {@link MultiblockLayout.Role#ENERGY_OUTPUT} and {@link #setOutput} has armed it.
 * An input part keeps a separate buffer, drains its condition each tick, and does not emit.
 */
public class AddonEnergyPartBlockEntity extends MultiblockPortBlockEntity {
    private long stored;
    private long inputStored;
    private boolean satisfied;
    private long outputSize;
    private long outputAmount;
    private boolean stopped = true;
    private GregTechTags.Tag outputTag;
    private GregTechTags.Tag inputTag;

    public AddonEnergyPartBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.ADDON_ENERGY_PART.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AddonEnergyPartBlockEntity part) {
        part.serverTick();
    }

    /** Output buffer, or the input buffer when this part accepts energy. */
    public long stored() { return input() != null ? inputStored : stored; }

    public long capacity() { return input() != null ? input().capacity() : block().capacity(); }

    public boolean condition() { return block().condition(); }

    public boolean satisfied() { return condition() && satisfied; }

    public String path() {
        var id = BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock());
        return id == null ? "" : id.getPath();
    }

    /** Adds up to the free space. The caller decides whether this part is bound. */
    public long charge(long amount) {
        if (input() != null) return storeInput(amount);
        long accepted = accept(stored, block().capacity(), amount);
        if (accepted <= 0) return 0;
        stored += accepted;
        setChanged();
        return accepted;
    }

    public void setOutput(long size, long amount) {
        EnergyOutput output = energy();
        if (output == null) throw new IllegalStateException("Energy part declares no output");
        BehaviorEnergyRules.requireEmit(size, amount, output.maxSize(), output.maxAmount());
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
        if (occupant instanceof LargeRecipeMachineBlockEntity) return false;
        return super.canBind(owner);
    }

    @Override public void release(BlockPos owner) {
        boolean mine = isBoundTo(owner);
        super.release(owner);
        if (mine) stop();
    }

    private void serverTick() {
        if (level == null || level.isClientSide) return;
        drainCondition();
        emitTick();
    }

    private boolean satisfiedStateSynced;

    private void drainCondition() {
        if (!condition()) return;
        var drained = PartEnergyRules.drain(inputStored, block().conditionPerTick());
        boolean changed = drained.stored() != inputStored || drained.satisfied() != satisfied;
        inputStored = drained.stored();
        satisfied = drained.satisfied();
        if (!changed && satisfiedStateSynced) return;
        satisfiedStateSynced = true;
        applySatisfied(satisfied);
        if (changed) setChanged();
    }

    private void applySatisfied(boolean value) {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (state.hasProperty(AddonEnergyPartBlock.SATISFIED) && state.getValue(AddonEnergyPartBlock.SATISFIED) != value)
            level.setBlock(worldPosition, state.setValue(AddonEnergyPartBlock.SATISFIED, value), 3);
    }

    private void emitTick() {
        EnergyOutput output = energy();
        if (level == null || level.isClientSide || output == null || stopped) return;
        if (boundRole() != MultiblockLayout.Role.ENERGY_OUTPUT || !controllerLive()) {
            stop();
            return;
        }
        if (outputSize < 1 || outputAmount < 1 || stored < outputSize) return;
        long left = Math.min(outputAmount, stored / outputSize);
        long sent = 0;
        for (MachineFace face : output.faces()) {
            if (left <= 0) break;
            long used = EnergyTransfer.emitEnergyToSide(outputTag(), world(face), outputSize, left, this);
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
        var occupant = level.getBlockEntity(owner);
        if (occupant == null || occupant.isRemoved()) return false;
        if (occupant instanceof BehaviorMachineBlockEntity machine) return machine.structureFormed();
        return occupant instanceof LargeRecipeMachineBlockEntity;
    }

    private AddonEnergyPartBlock block() {
        return (AddonEnergyPartBlock) getBlockState().getBlock();
    }

    private EnergyOutput energy() { return block().energy(); }

    private EnergyInput input() { return block().input(); }

    /** How much of {@code amount} fits in the remaining capacity. Does not change the buffer. */
    private long accept(long stored, long cap, long amount) {
        if (amount < 0) throw new IllegalArgumentException("negative charge");
        long held = Math.max(0, Math.min(stored, cap));
        return Math.min(amount, cap - held);
    }

    private long storeInput(long amount) {
        long accepted = accept(inputStored, input().capacity(), amount);
        if (accepted <= 0) return 0;
        inputStored += accepted;
        setChanged();
        return accepted;
    }

    private GregTechTags.Tag outputTag() {
        if (outputTag != null) return outputTag;
        outputTag = tag(energy().type());
        return outputTag;
    }

    private GregTechTags.Tag inputTag() {
        if (inputTag != null) return inputTag;
        inputTag = tag(input().type());
        return inputTag;
    }

    private static GregTechTags.Tag tag(String name) {
        return GregTechTags.Energy.ALL.stream().filter(candidate -> candidate.getShortName().equalsIgnoreCase(name))
                .findFirst().orElseThrow(() -> new IllegalStateException("Unknown energy type: " + name));
    }

    private boolean outputSide(Direction side) {
        EnergyOutput output = energy();
        if (output == null || side == null) return false;
        for (MachineFace face : output.faces())
            if (world(face) == side) return true;
        return false;
    }

    private boolean inputSide(Direction side) {
        EnergyInput input = input();
        if (input == null) return false;
        if (side == null) return true;
        for (MachineFace face : input.faces())
            if (world(face) == side) return true;
        return false;
    }

    private boolean acceptsInput(GregTechTags.Tag type, Direction side) {
        return input() != null && type != null && type.equals(inputTag()) && inputSide(side);
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
        if (acceptsInput(input() == null ? null : inputTag(), side)) return List.of(inputTag());
        if (energy() != null && (side == null || outputSide(side))) return List.of(outputTag());
        return List.of();
    }

    @Override public boolean isEnergyType(GregTechTags.Tag type, Direction side, boolean emitting) {
        if (type == null) return false;
        if (energy() != null && type.equals(outputTag()) && (side == null || outputSide(side))) return true;
        return !emitting && acceptsInput(type, side);
    }

    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, Direction side, boolean theoretical) {
        return acceptsInput(type, side);
    }

    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type, Direction side, boolean theoretical) {
        return energy() != null && !stopped && boundRole() == MultiblockLayout.Role.ENERGY_OUTPUT && controllerLive()
                && isEnergyType(type, side, true);
    }

    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type, Direction side) {
        return acceptsInput(type, side) ? input().perTick() : 0;
    }

    @Override public long getEnergySizeInputMin(GregTechTags.Tag type, Direction side) {
        return acceptsInput(type, side) ? input().rangeMin() : 0;
    }

    @Override public long getEnergySizeInputMax(GregTechTags.Tag type, Direction side) {
        return acceptsInput(type, side) ? input().rangeMax() : 0;
    }

    @Override public long getEnergyDemanded(GregTechTags.Tag type, Direction side, long size) {
        if (!acceptsInput(type, side) || size == 0 || size == Long.MIN_VALUE) return 0;
        long sizeAbs = Math.abs(size);
        if (sizeAbs < input().rangeMin() || sizeAbs > input().rangeMax()) return 0;
        return BasicMachineEnergy.demanded(inputStored, input().capacity(), size);
    }

    @Override public long getEnergyOffered(GregTechTags.Tag type, Direction side, long size) {
        if (!isEnergyEmittingTo(type, side, false) || size != outputSize || outputSize < 1) return 0;
        return Math.min(outputAmount, stored / outputSize);
    }

    @Override public long getEnergyStored(GregTechTags.Tag type, Direction side) {
        if (acceptsInput(type, side)) return inputStored;
        return energy() != null && type != null && type.equals(outputTag()) ? stored : 0;
    }

    @Override public long getEnergyCapacity(GregTechTags.Tag type, Direction side) {
        if (acceptsInput(type, side)) return input().capacity();
        return energy() != null && type != null && type.equals(outputTag()) ? block().capacity() : 0;
    }

    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type, Direction side) {
        return isEnergyEmittingTo(type, side, true) ? outputSize : 0;
    }

    @Override public long doInject(GregTechTags.Tag type, Direction side, long size, long amount, boolean execute) {
        if (!acceptsInput(type, side)) return 0;
        var decision = PartEnergyRules.admit(inputStored, input().capacity(), size, amount,
                input().rangeMin(), input().rangeMax(), false);
        if (decision.overload()) {
            if (execute) com.gregtech.gregtech.api.energy.MachineOverload.apply(this, Math.abs(size), type);
            return decision.packets();
        }
        if (!execute || decision.packets() <= 0) return decision.packets();
        inputStored = BasicMachineEnergy.add(inputStored, input().capacity(), size, decision.packets());
        setChanged();
        return decision.packets();
    }

    private void saveEnergy(CompoundTag tag) {
        tag.putLong("gt.part_energy", stored);
        tag.putLong("gt.part_output", outputSize);
        tag.putLong("gt.part_amount", outputAmount);
        tag.putBoolean("gt.part_stopped", stopped);
        if (input() != null) {
            tag.putLong("gt.part_input", inputStored);
            tag.putBoolean("gt.part_satisfied", satisfied);
        }
    }

    private void loadEnergy(CompoundTag tag) {
        stored = Math.max(0, tag.getLong("gt.part_energy"));
        if (energy() != null && stored > block().capacity()) stored = block().capacity();
        outputSize = Math.max(0, tag.getLong("gt.part_output"));
        outputAmount = Math.max(0, tag.getLong("gt.part_amount"));
        stopped = !tag.contains("gt.part_stopped") || tag.getBoolean("gt.part_stopped");
        inputStored = 0;
        satisfied = false;
        if (input() != null && tag.contains("gt.part_input")) {
            inputStored = Math.max(0, Math.min(input().capacity(), tag.getLong("gt.part_input")));
            satisfied = tag.getBoolean("gt.part_satisfied");
        }
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
