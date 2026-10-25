package gregtech.content.blockentity.machine;

import gregtech.internal.addon.BehaviorEnergyRules;
import gregtech.api.addon.EnergyInput;
import gregtech.api.addon.EnergyOutput;
import gregtech.api.addon.LayoutPart;
import gregtech.api.addon.MachineFace;
import gregtech.internal.addon.PartEnergyRules;
import gregtech.api.addon.platform.PartClientView;
import gregtech.internal.energy.EnergyTransfer;
import gregtech.internal.machine.BasicMachineEnergy;
import gregtech.internal.multiblock.MultiblockLayout;
import gregtech.content.block.machine.AddonEnergyPartBlock;
import gregtech.content.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
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
public class AddonEnergyPartBlockEntity extends MultiblockPortBlockEntity
        implements FunctionalPartRuntime.Host, PartClientView {
    @javax.annotation.Nullable private final FunctionalPartRuntime function;
    private long stored;
    private long inputStored;
    private boolean satisfied;
    private long outputSize;
    private long outputAmount;
    private boolean stopped = true;
    private GregTechTags.Tag outputTag;
    private GregTechTags.Tag inputTag;

//? if >=1.21.1 {
//?} else {
    private final java.util.EnumMap<Direction, net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>> itemCaps =
            new java.util.EnumMap<>(Direction.class);
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> unsided =
            net.minecraftforge.common.util.LazyOptional.empty();
    private final java.util.EnumMap<Direction, net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler>> fluidCaps =
            new java.util.EnumMap<>(Direction.class);
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> unsidedFluid =
            net.minecraftforge.common.util.LazyOptional.empty();
//?}

    public AddonEnergyPartBlockEntity(BlockPos pos, BlockState state) {
        super(gregtech.content.registry.GTBlockEntities.ADDON_ENERGY_PART.get(), pos, state);
        this.function = block().functional() ? new FunctionalPartRuntime(this) : null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AddonEnergyPartBlockEntity part) {
        part.serverTick();
    }

    @Override public FunctionalPartRuntime function() { return function; }
    @Override public Level level() { return level; }
    @Override public BlockPos pos() { return worldPosition; }
    @Override public BlockState blockState() { return getBlockState(); }
    @Override public String addonId() { return block().addonId(); }
    @Override public LayoutPart declaration() { return block().declaration(); }
    @Override public boolean removed() { return isRemoved(); }
    @Override public BlockPos controllerPos() { return super.controllerPos(); }
    @Override public boolean boundTo(BlockPos owner) { return owner != null && isBoundTo(owner); }
    @Override public void markChanged() { setChanged(); }
    @Override public void syncToClient() {
        if (level != null && !level.isClientSide && function != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override public String name() { return path(); }
    @Override public int slots() { return function == null ? 0 : function.slotCount(); }
    @Override public ItemStack item(int slot) { return function == null ? ItemStack.EMPTY : function.item(slot); }
    @Override public boolean occupied(int slot) { return !item(slot).isEmpty(); }
    @Override public int getInt(String key) { return function == null ? 0 : function.context().state().getInt(key); }
    @Override public long getLong(String key) { return function == null ? 0L : function.context().state().getLong(key); }
    @Override public boolean getBoolean(String key) { return function != null && function.context().state().getBoolean(key); }
    @Override public String getString(String key) { return function == null ? "" : function.context().state().getString(key); }

    /** Output buffer, or the input buffer when this part only accepts energy. */
    public long stored() { return energy() != null ? stored : inputStored; }

    public long capacity() { return energy() != null ? block().capacity() : input() != null ? input().capacity() : 0; }

    public boolean condition() { return block().condition(); }

    public boolean satisfied() { return condition() && satisfied; }

    public String path() {
        var id = BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock());
        return id == null ? "" : id.getPath();
    }

    /** Adds up to the free space. The caller decides whether this part is bound. */
    public long charge(long amount) {
        if (energy() != null) {
            long accepted = accept(stored, block().capacity(), amount);
            if (accepted <= 0) return 0;
            stored += accepted;
            setChanged();
            return accepted;
        }
        if (input() != null) return storeInput(amount);
        return 0;
    }

    public void setOutput(long size, long amount) {
        EnergyOutput output = energy();
        if (output == null) throw new IllegalStateException("Energy part declares no output");
        BehaviorEnergyRules.requireEmit(size, amount, output.minSize(), output.maxSize(), output.maxAmount(),
                output.allowNegative());
        if (boundRole() != MultiblockLayout.Role.ENERGY_OUTPUT)
            throw new IllegalStateException("Energy part is not an ENERGY_OUTPUT cell");
        if (outputSize == size && outputAmount == amount && !stopped) return;
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
        if (!gregtech.internal.multiblock.tracking.StructureTrackingMode.legacy()) return super.canBind(owner);
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
        if (function != null) function.tick();
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

    private long packetMagnitude(EnergyOutput output) {
        if (output == null || outputSize == Long.MIN_VALUE) return 0;
        return output.allowNegative() ? Math.abs(outputSize) : outputSize;
    }

    private void emitTick() {
        EnergyOutput output = energy();
        if (level == null || level.isClientSide || output == null || stopped) return;
        if (boundRole() != MultiblockLayout.Role.ENERGY_OUTPUT || !controllerLive()) {
            stop();
            return;
        }
        long magnitude = packetMagnitude(output);
        if (magnitude < 1 || outputAmount < 1 || stored < magnitude) return;
        long left = Math.min(outputAmount, stored / magnitude);
        long sent = 0;
        for (MachineFace face : output.faces()) {
            if (left <= 0) break;
            long used = EnergyTransfer.emitEnergyToSide(outputTag(), world(face), outputSize, left, this);
            sent += used;
            left -= used;
        }
        if (sent > 0) {
            stored -= Math.multiplyExact(magnitude, sent);
            setChanged();
        }
    }

    private boolean controllerLive() {
        BlockPos owner = controllerPos();
        if (owner == null || level == null || !level.hasChunkAt(owner)) return false;
        var occupant = level.getBlockEntity(owner);
        if (occupant == null || occupant.isRemoved()) return false;
        if (!gregtech.internal.multiblock.tracking.StructureTrackingMode.legacy()) {
            var tracker = gregtech.internal.multiblock.tracking.StructureTracking.tracker(occupant);
            return tracker != null && tracker.formed();
        }
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

    /** The part's output energy type, or an empty string when it does not emit. */
    public String outputType() { return energy() == null ? "" : energy().type(); }

    /** The part's output tag, or null when it does not emit. */
    public GregTechTags.Tag outputEnergyTag() { return energy() == null ? null : outputTag(); }

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
        // A two-way part names no input faces; its input is the controller channel only.
        if (side == null) return !input.faces().isEmpty();
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
        long magnitude = packetMagnitude(energy());
        if (!isEnergyEmittingTo(type, side, false) || size != outputSize || magnitude < 1) return 0;
        return Math.min(outputAmount, stored / magnitude);
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
            if (execute) gregtech.internal.energy.MachineOverload.apply(this, Math.abs(size), type);
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

//? if >=1.21.1 {
/*    private void saveFunction(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        if (function != null) function.save(tag, lookup);
    }
    private void loadFunction(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        if (function != null) function.load(tag, lookup);
    }
*///?} else {
    private void saveFunction(CompoundTag tag) {
        if (function != null) function.save(tag);
    }
    private void loadFunction(CompoundTag tag) {
        if (function != null) function.load(tag);
    }
//?}

    private void loadEnergy(CompoundTag tag) {
        stored = Math.max(0, tag.getLong("gt.part_energy"));
        if (energy() != null && stored > block().capacity()) stored = block().capacity();
        long savedOutput = tag.getLong("gt.part_output");
        outputSize = energy() != null && energy().allowNegative() ? savedOutput : Math.max(0, savedOutput);
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
        saveFunction(tag, lookup);
    }
    @Override protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        loadEnergy(tag);
        loadFunction(tag, lookup);
    }
    @Override public net.neoforged.neoforge.items.IItemHandler itemCapability(Direction side) {
        if (isRemoved()) return null;
        if (function != null) {
            var own = function.items(side);
            if (own != null) return own;
        }
        return super.itemCapability(side);
    }
    @Override public net.neoforged.neoforge.fluids.capability.IFluidHandler fluidCapability(Direction side) {
        if (isRemoved()) return null;
        if (function != null) {
            var own = function.fluids(side);
            if (own != null) return own;
        }
        return super.fluidCapability(side);
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return function == null ? null : net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void onDataPacket(net.minecraft.network.Connection net,
            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,
            net.minecraft.core.HolderLookup.Provider lookup) {
        if (packet.getTag() != null) loadAdditional(packet.getTag(), lookup);
    }
*///?} else {
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveEnergy(tag);
        saveFunction(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        loadEnergy(tag);
        loadFunction(tag);
    }
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @javax.annotation.Nullable Direction side) {
        if (function != null && cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER
                && !isRemoved() && function.items(side) != null) {
            if (side == null) {
                if (!unsided.isPresent()) unsided = net.minecraftforge.common.util.LazyOptional.of(() -> function.items(null));
                return unsided.cast();
            }
            return itemCaps.computeIfAbsent(side, found -> net.minecraftforge.common.util.LazyOptional.of(() -> function.items(found))).cast();
        }
        if (function != null && cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER
                && !isRemoved() && function.fluids(side) != null) {
            if (side == null) {
                if (!unsidedFluid.isPresent()) unsidedFluid = net.minecraftforge.common.util.LazyOptional.of(() -> function.fluids(null));
                return unsidedFluid.cast();
            }
            return fluidCaps.computeIfAbsent(side, found -> net.minecraftforge.common.util.LazyOptional.of(() -> function.fluids(found))).cast();
        }
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        unsided.invalidate();
        itemCaps.values().forEach(net.minecraftforge.common.util.LazyOptional::invalidate);
        itemCaps.clear();
        unsidedFluid.invalidate();
        fluidCaps.values().forEach(net.minecraftforge.common.util.LazyOptional::invalidate);
        fluidCaps.clear();
    }
    @Override public void reviveCaps() {
        super.reviveCaps();
        unsided = net.minecraftforge.common.util.LazyOptional.empty();
        unsidedFluid = net.minecraftforge.common.util.LazyOptional.empty();
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return function == null ? null : net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void onDataPacket(net.minecraft.network.Connection net,
            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) load(packet.getTag());
    }
//?}
}
