package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.addon.AddonMachines;
import com.gregtech.gregtech.api.addon.BehaviorEnergyRules;
import com.gregtech.gregtech.api.addon.BehaviorMachine;
import com.gregtech.gregtech.api.addon.MachineFace;
import com.gregtech.gregtech.api.addon.PartEnergyRules;
import com.gregtech.gregtech.api.addon.BehaviorRangeRules;
import com.gregtech.gregtech.api.addon.BehaviorMachineHosts;
import com.gregtech.gregtech.api.addon.platform.BehaviorState;
//? if >=1.21.1 {
/*import com.gregtech.gregtech.api.addon.platform.EnergyBlock;
*///?} else {
//?}
import com.gregtech.gregtech.api.addon.platform.EnergyPart;
import com.gregtech.gregtech.api.addon.platform.MachineBehavior;
import com.gregtech.gregtech.api.addon.platform.MachineBehaviorContext;
import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import com.gregtech.gregtech.api.multiblock.PartBindings;
import com.gregtech.gregtech.content.multiblock.LargeMachineLayouts;
import com.gregtech.gregtech.api.energy.EnergyBlockViews;
import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.api.machine.BasicMachineEnergy;
import com.gregtech.gregtech.block.machine.BehaviorMachineBlock;
import com.gregtech.gregtech.client.gui.BehaviorMachineMenu;
import com.gregtech.gregtech.data.GregTechTags;
//? if >=1.21.1 {
/**///?} else {
import com.gregtech.gregtech.api.addon.platform.EnergyBlock;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if >=1.21.1 {
/*import net.minecraft.core.HolderLookup;
*///?} else {
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
//?}

//? if >=1.21.1 {
/**///?} else {
import javax.annotation.Nonnull;
//?}
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Shell block entity. The addon behavior never sees this class. */
public class BehaviorMachineBlockEntity extends BlockEntity implements EnergyBlock, MenuProvider {
    private final BehaviorMachine declaration;
    private final AddonMachines.AcceptedBehavior accepted;
    private final MachineBehavior behavior;
    private final ItemStackHandler items;
    private final Tank[] tanks;
    private final StoredState state = new StoredState();
    private final Context context = new Context();
    private final PipeItems pipeItems = new PipeItems();
    private final PipeFluids pipeFluids = new PipeFluids();
//? if >=1.21.1 {
/**///?} else {
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> pipeItems);
    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> pipeFluids);
//?}
    private final GregTechTags.Tag energyTag;
    private final GregTechTags.Tag energyOutTag;
    private final GregTechTags.Tag auxiliaryTag;
    private long stored;
    private long auxiliaryStored;
    private long capacity;
    private boolean capacitySet;
    private int toggles;
    private int progress;
    private boolean working;
    private boolean powered;
    private final PartBindings<BlockPos, MultiblockLayout.Role> structureBindings = new PartBindings<>();
    private boolean formed;
    private List<EnergyPart> energyParts = List.of();
    private int breaksLeft;
    private int itemsLeft;
    @Nullable private UUID placerId;
    private String placerName = "";

    public BehaviorMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        BehaviorMachineBlock block = (BehaviorMachineBlock) state.getBlock();
        this.accepted = block.accepted();
        this.declaration = accepted.declaration();
        this.behavior = BehaviorMachineHosts.create(accepted.addonId(), declaration.casing().machineType());
        this.items = new ItemStackHandler(declaration.items().size()) {
            @Override protected void onContentsChanged(int slot) { setChanged(); }
        };
        this.tanks = new Tank[declaration.fluids().size()];
        for (int i = 0; i < tanks.length; i++) tanks[i] = new Tank(declaration.fluids().get(i));
        this.toggles = declaration.initialToggles();
        this.capacity = declaration.energy().capacity();
        this.energyTag = GregTechTags.Energy.ALL.stream()
                .filter(tag -> tag.getShortName().equalsIgnoreCase(declaration.energy().type()))
                .findFirst().orElseThrow();
        this.energyOutTag = energyOutName().isBlank() ? null : GregTechTags.Energy.ALL.stream()
                .filter(tag -> tag.getShortName().equalsIgnoreCase(energyOutName()))
                .findFirst().orElseThrow();
        this.auxiliaryTag = declaration.auxiliary() == null ? null : GregTechTags.Energy.ALL.stream()
                .filter(tag -> tag.getShortName().equalsIgnoreCase(declaration.auxiliary().type()))
                .findFirst().orElseThrow();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BehaviorMachineBlockEntity machine) {
        machine.tickServer(level, pos);
    }

    public BehaviorMachine declaration() { return declaration; }
    public AddonMachines.AcceptedBehavior accepted() { return accepted; }
    public BehaviorState state() { return state; }
    public ItemStackHandler inventory() { return items; }
    public IItemHandler items() { return pipeItems; }
    public IFluidHandler fluids() { return pipeFluids; }
    public int tanks() { return tanks.length; }
    public long storedEnergy() { return stored; }
    public long energyCapacity() { return capacity; }
    /** Gauge maximum: the storage cap, or the rated input when the machine does not store. */
    public long energyBarMax() { return bufferCapacity(); }
    public boolean onUse(Player player, InteractionHand hand) { return behavior.onUse(context, player, hand); }
    public boolean toggle(int index) { return context.toggle(index); }
//? if >=1.21.1 {
/*
    public IItemHandler itemCapability(@Nullable Direction side) { return pipeItems; }

    @Nullable
    public IFluidHandler fluidCapability(@Nullable Direction side) {
        return tanks.length == 0 ? null : pipeFluids;
    }
*///?} else {
//?}

    public boolean behaviorAllows(int slot, ItemStack stack) {
        return behavior.canInsert(context, slot, stack);
    }

    public boolean flip(int index) {
        if (index < 0 || index >= declaration.toggles().size()) return false;
        context.setToggle(index, !context.toggle(index));
        behavior.onToggle(context, index);
        setChanged();
        return true;
    }

    public void rememberPlacer(Player player) {
        if (player == null) return;
        placerId = player.getUUID();
        String name = player.getGameProfile().getName();
        placerName = name == null ? "" : name;
        setChanged();
    }

    public void dropContents() {
        if (level == null) return;
        behavior.beforeBreak(context);
        for (int slot = 0; slot < items.getSlots(); slot++)
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(slot));
        for (ItemStack stack : state.items.values()) {
            if (!stack.isEmpty())
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
        }
    }

    public boolean structureFormed() { return formed; }

    private void tickServer(Level level, BlockPos pos) {
        breaksLeft = BehaviorRangeRules.BREAKS_PER_TICK;
        itemsLeft = BehaviorRangeRules.ITEMS_PER_TICK;
        pollEnergy(level, pos);
        pollAuxiliary(level, pos);
        refreshStructure();
        powered = stored > 0;
        working = false;
        behavior.tick(context);
        stored = BehaviorEnergyRules.finishTick(stored, capacity, accepted.energyInMax());
        BlockState state = getBlockState();
        if (state.getValue(BehaviorMachineBlock.RUNNING) != powered || state.getValue(BehaviorMachineBlock.LIT) != working)
            level.setBlock(pos, state.setValue(BehaviorMachineBlock.RUNNING, powered).setValue(BehaviorMachineBlock.LIT, working), 3);
    }

    private void pollEnergy(Level level, BlockPos pos) {
        long size = declaration.energy().perTick();
        for (Direction side : Direction.values()) {
            if (!accepts(side)) continue;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(side));
            IEnergyBlock source = EnergyBlockViews.view(neighbor);
            if (source == null || !source.isEnergyEmittingTo(energyTag, side.getOpposite(), false)) continue;
            long offered = source.getEnergyOffered(energyTag, side.getOpposite(), size);
            long requested = BasicMachineEnergy.accepted(stored, bufferCapacity(), size, offered);
            if (requested <= 0) continue;
            long extracted = source.doEnergyExtraction(energyTag, side.getOpposite(), size, requested, true);
            if (extracted > 0) stored = BasicMachineEnergy.add(stored, bufferCapacity(), size, extracted);
        }
    }

    private void pollAuxiliary(Level level, BlockPos pos) {
        if (auxiliaryTag == null || declaration.auxiliary() == null) return;
        long size = declaration.auxiliary().perTick();
        long cap = declaration.auxiliary().capacity();
        for (Direction side : Direction.values()) {
            if (!auxiliaryFace(side)) continue;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(side));
            IEnergyBlock source = EnergyBlockViews.view(neighbor);
            if (source == null || !source.isEnergyEmittingTo(auxiliaryTag, side.getOpposite(), false)) continue;
            long offered = source.getEnergyOffered(auxiliaryTag, side.getOpposite(), size);
            long requested = BasicMachineEnergy.accepted(auxiliaryStored, cap, size, offered);
            if (requested <= 0) continue;
            long extracted = source.doEnergyExtraction(auxiliaryTag, side.getOpposite(), size, requested, true);
            if (extracted > 0) auxiliaryStored = BasicMachineEnergy.add(auxiliaryStored, cap, size, extracted);
        }
    }

    private boolean accepts(Direction side) {
        return side == null || FaceConfig.has(declaration.energyMask(), relative(side));
    }

    private boolean auxiliaryFace(Direction side) {
        return declaration.auxiliary() != null && side != null
                && FaceConfig.has(declaration.auxiliaryMask(), relative(side));
    }

    private boolean auxiliaryAccepts(String energy, Direction side) {
        return auxiliaryFace(side) && energy != null && energy.equalsIgnoreCase(declaration.auxiliary().type());
    }

    private boolean sameEnergy(String energy, String type) {
        return energy != null && type != null && energy.equalsIgnoreCase(type);
    }

    private String energyOutName() {
        return declaration.output() == null ? "" : declaration.output().type();
    }

    private long energyOutSize() {
        return declaration.output() == null ? 0 : declaration.output().maxSize();
    }

    private long energyOutAmount() {
        return declaration.output() == null ? 0 : declaration.output().maxAmount();
    }

    private boolean emits(Direction side) {
        return declaration.emitsEnergy() && (side == null || FaceConfig.has(declaration.energyOutMask(), relative(side)));
    }

    private long outputSize(String energy, Direction side) {
        return isEnergyEmittingTo(energy, side, true) ? energyOutSize() : 0;
    }

    private long bufferCapacity() {
        return BehaviorEnergyRules.buffer(capacity, accepted.energyInMax());
    }

    private long emit(long size, long amount) {
        BehaviorEnergyRules.requireEmit(size, amount, energyOutSize(), energyOutAmount());
        if (energyOutTag == null || level == null) return 0;
        long affordable = stored / size;
        long left = Math.min(amount, Math.max(0, affordable));
        long sent = 0;
        for (MachineFace face : declaration.output() == null ? List.<MachineFace>of() : declaration.output().faces()) {
            if (left <= 0) break;
            long used = com.gregtech.gregtech.api.energy.EnergyTransfer.emitEnergyToSide(
                    energyOutTag, world(face), size, left, this);
            sent += used;
            left -= used;
        }
        if (sent > 0) {
            stored -= Math.multiplyExact(size, sent);
            setChanged();
        }
        return sent;
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

    private int relative(Direction side) {
        Direction facing = getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        if (side == Direction.DOWN) return FaceConfig.BOTTOM;
        if (side == Direction.UP) return FaceConfig.TOP;
        return switch (facing) {
            case SOUTH -> switch (side) {
                case SOUTH -> FaceConfig.FRONT; case NORTH -> FaceConfig.BACK;
                case EAST -> FaceConfig.RIGHT; case WEST -> FaceConfig.LEFT;
                default -> FaceConfig.FRONT;
            };
            case WEST -> switch (side) {
                case WEST -> FaceConfig.FRONT; case EAST -> FaceConfig.BACK;
                case NORTH -> FaceConfig.LEFT; case SOUTH -> FaceConfig.RIGHT;
                default -> FaceConfig.FRONT;
            };
            case EAST -> switch (side) {
                case EAST -> FaceConfig.FRONT; case WEST -> FaceConfig.BACK;
                case SOUTH -> FaceConfig.LEFT; case NORTH -> FaceConfig.RIGHT;
                default -> FaceConfig.FRONT;
            };
            default -> switch (side) {
                case NORTH -> FaceConfig.FRONT; case SOUTH -> FaceConfig.BACK;
                case WEST -> FaceConfig.RIGHT; case EAST -> FaceConfig.LEFT;
                default -> FaceConfig.FRONT;
            };
        };
    }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BehaviorMachineMenu(id, inventory, this);
    }

    public ContainerData menuData() {
        return new ContainerData() {
            @Override public int get(int index) { return menuValue(index); }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 3 + tanks.length * 2; }
        };
    }

    private int menuValue(int index) {
        if (index == 0) return (int) Math.min(Integer.MAX_VALUE, stored);
        if (index == 1) return progress;
        if (index == 2) return toggles;
        int tanksStart = 3;
        if (index < tanksStart + tanks.length) return tanks[index - tanksStart].amount;
        int fluid = index - tanksStart - tanks.length;
        if (fluid < 0 || fluid >= tanks.length || tanks[fluid].amount <= 0 || tanks[fluid].fluid == Fluids.EMPTY) return -1;
        return BuiltInRegistries.FLUID.getId(tanks[fluid].fluid);
    }

//? if >=1.21.1 {
/**///?} else {
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER && tanks.length > 0) return fluidCap.cast();
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
        fluidCap.invalidate();
    }

//?}
    @Override
    public boolean isEnergyType(String energy, Direction side, boolean emitting) {
        if (energy == null) return false;
        if (emitting) return declaration.emitsEnergy() && energy.equalsIgnoreCase(energyOutName());
        return sameEnergy(energy, declaration.energy().type())
                || (declaration.auxiliary() != null && sameEnergy(energy, declaration.auxiliary().type()));
    }

    @Override public java.util.Collection<String> getEnergyTypes(Direction side) {
        var names = new java.util.ArrayList<String>();
        names.add(declaration.energy().type());
        if (declaration.auxiliary() != null && !containsEnergy(names, declaration.auxiliary().type()))
            names.add(declaration.auxiliary().type());
        if (declaration.emitsEnergy() && !containsEnergy(names, energyOutName()))
            names.add(energyOutName());
        return List.copyOf(names);
    }

    private static boolean containsEnergy(java.util.List<String> names, String energy) {
        for (String name : names) if (name.equalsIgnoreCase(energy)) return true;
        return false;
    }

    @Override
    public boolean isEnergyAcceptingFrom(String energy, Direction side, boolean theoretical) {
        if (auxiliaryAccepts(energy, side)) return true;
        return sameEnergy(energy, declaration.energy().type()) && accepts(side);
    }

    @Override
    public boolean isEnergyEmittingTo(String energy, Direction side, boolean theoretical) {
        return isEnergyType(energy, side, true) && emits(side);
    }

    @Override
    public long doInject(String energy, Direction side, long size, long amount, boolean doInject) {
        if (auxiliaryAccepts(energy, side)) return injectAuxiliary(energy, size, amount, doInject);
        if (!isEnergyAcceptingFrom(energy, side, false) || size == 0 || size == Long.MIN_VALUE || amount <= 0) return 0;
        long sizeAbs = Math.abs(size);
        if (sizeAbs > accepted.energyInMax()) {
            if (doInject) com.gregtech.gregtech.api.energy.MachineOverload.apply(this, sizeAbs, energy);
            return amount;
        }
        long taken = BasicMachineEnergy.accepted(stored, bufferCapacity(), size, amount);
        if (doInject && taken > 0) {
            stored = BasicMachineEnergy.add(stored, bufferCapacity(), size, taken);
            setChanged();
        }
        return taken;
    }

    private long injectAuxiliary(String energy, long size, long amount, boolean doInject) {
        var auxiliary = declaration.auxiliary();
        boolean anyPacket = auxiliary.anyPacket();
        var decision = PartEnergyRules.admit(auxiliaryStored, auxiliary.capacity(), size, amount,
                anyPacket ? 1 : auxiliary.rangeMin(), anyPacket ? Long.MAX_VALUE : auxiliary.rangeMax(), anyPacket);
        if (decision.overload()) {
            if (doInject) com.gregtech.gregtech.api.energy.MachineOverload.apply(this, Math.abs(size), energy);
            return decision.packets();
        }
        if (doInject && decision.packets() > 0) {
            auxiliaryStored = BasicMachineEnergy.add(auxiliaryStored, auxiliary.capacity(), size, decision.packets());
            setChanged();
        }
        return decision.packets();
    }

    @Override
    public long getEnergyDemanded(String energy, Direction side, long size) {
        if (auxiliaryAccepts(energy, side)) {
            if (size == 0 || size == Long.MIN_VALUE) return 0;
            var auxiliary = declaration.auxiliary();
            if (!auxiliary.anyPacket()) {
                long sizeAbs = Math.abs(size);
                if (sizeAbs < auxiliary.rangeMin() || sizeAbs > auxiliary.rangeMax()) return 0;
            }
            return BasicMachineEnergy.demanded(auxiliaryStored, auxiliary.capacity(), size);
        }
        if (!isEnergyAcceptingFrom(energy, side, false) || size == 0 || size == Long.MIN_VALUE) return 0;
        return BasicMachineEnergy.demanded(stored, bufferCapacity(), size);
    }

    @Override public long getEnergyOffered(String energy, Direction side, long size) { return 0; }
    @Override public long getEnergySizeInputRecommended(String energy, Direction side) {
        if (auxiliaryAccepts(energy, side)) return declaration.auxiliary().perTick();
        return declaration.energy().perTick();
    }
    @Override public long getEnergySizeOutputRecommended(String energy, Direction side) { return outputSize(energy, side); }
    @Override public long getEnergySizeOutputMin(String energy, Direction side) { return outputSize(energy, side); }
    @Override public long getEnergySizeOutputMax(String energy, Direction side) { return outputSize(energy, side); }
    @Override public long getEnergySizeInputMin(String energy, Direction side) {
        if (auxiliaryAccepts(energy, side))
            return declaration.auxiliary().anyPacket() ? 1 : declaration.auxiliary().rangeMin();
        return accepted.energyInMin();
    }
    @Override public long getEnergySizeInputMax(String energy, Direction side) {
        if (auxiliaryAccepts(energy, side))
            return declaration.auxiliary().anyPacket() ? Long.MAX_VALUE : declaration.auxiliary().rangeMax();
        return accepted.energyInMax();
    }

    @Override public boolean isEnergyCapacitorType(String energy, Direction side) {
        if (auxiliaryAccepts(energy, side)) return true;
        return capacity > 0 && sameEnergy(energy, declaration.energy().type()) && !auxiliaryFace(side);
    }

    @Override public long getEnergyStored(String energy, Direction side) {
        if (auxiliaryAccepts(energy, side)) return auxiliaryStored;
        return isEnergyCapacitorType(energy, side) ? stored : 0;
    }

    @Override public long getEnergyCapacity(String energy, Direction side) {
        if (auxiliaryAccepts(energy, side)) return declaration.auxiliary().capacity();
        return isEnergyCapacitorType(energy, side) ? capacity : 0;
    }

    @Override public java.util.Collection<String> getEnergyCapacitorTypes(Direction side) {
        var names = new java.util.ArrayList<String>();
        if (capacity > 0) names.add(declaration.energy().type());
        if (declaration.auxiliary() != null && !containsEnergy(names, declaration.auxiliary().type()))
            names.add(declaration.auxiliary().type());
        return List.copyOf(names);
    }

    @Override
//? if >=1.21.1 {
/*    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        writeMachine(tag, lookup);
*///?} else {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeMachine(tag);
//?}
    }

    @Override
//? if >=1.21.1 {
/*    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        readMachine(tag, lookup);
*///?} else {
    public void load(CompoundTag tag) {
        super.load(tag);
        readMachine(tag);
//?}
    }

//? if >=1.21.1 {
/*    public void writeMachine(CompoundTag tag, HolderLookup.Provider lookup) {
*///?} else {
    public void writeMachine(CompoundTag tag) {
//?}
        tag.putLong("gt.energy", stored);
        if (declaration.auxiliary() != null) tag.putLong("gt.aux_energy", auxiliaryStored);
        if (capacitySet) tag.putLong("gt.capacity", capacity);
        tag.putInt("gt.progress", progress);
        tag.putInt("gt.toggles", toggles);
        if (placerId != null) {
            tag.putString("gt.placer", placerId.toString());
            tag.putString("gt.placer_name", placerName);
        }
//? if >=1.21.1 {
/*        tag.put("gt.items", items.serializeNBT(lookup));
*///?} else {
        tag.put("gt.items", items.serializeNBT());
//?}
        ListTag fluids = new ListTag();
        for (Tank tank : tanks) {
            CompoundTag row = new CompoundTag();
            ResourceLocation name = BuiltInRegistries.FLUID.getKey(tank.fluid);
            row.putString("Fluid", name == null ? "" : name.toString());
            row.putInt("Amount", tank.amount);
            fluids.add(row);
        }
        tag.put("gt.fluids", fluids);
//? if >=1.21.1 {
/*        tag.put("gt.behavior", state.save(lookup));
*///?} else {
        tag.put("gt.behavior", state.save());
//?}
    }

//? if >=1.21.1 {
/*    public void readMachine(CompoundTag tag, HolderLookup.Provider lookup) {
*///?} else {
    public void readMachine(CompoundTag tag) {
//?}
        capacity = BehaviorEnergyRules.readCapacity(tag.contains("gt.capacity"), tag.getLong("gt.capacity"), declaration.energy().capacity());
        capacitySet = tag.contains("gt.capacity");
        stored = Math.max(0, Math.min(bufferCapacity(), tag.getLong("gt.energy")));
        auxiliaryStored = 0;
        if (declaration.auxiliary() != null && tag.contains("gt.aux_energy"))
            auxiliaryStored = Math.max(0, Math.min(declaration.auxiliary().capacity(), tag.getLong("gt.aux_energy")));
        progress = tag.getInt("gt.progress");
        toggles = tag.getInt("gt.toggles");
        placerId = null;
        placerName = "";
        if (tag.contains("gt.placer")) {
            try {
                placerId = UUID.fromString(tag.getString("gt.placer"));
                placerName = tag.getString("gt.placer_name");
            } catch (IllegalArgumentException ignored) {
                placerId = null;
            }
        }
//? if >=1.21.1 {
/*        if (tag.contains("gt.items")) items.deserializeNBT(lookup, tag.getCompound("gt.items"));
*///?} else {
        if (tag.contains("gt.items")) items.deserializeNBT(tag.getCompound("gt.items"));
//?}
        ListTag fluids = tag.getList("gt.fluids", Tag.TAG_COMPOUND);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].amount = 0;
            tanks[i].fluid = Fluids.EMPTY;
            if (i >= fluids.size()) continue;
            CompoundTag row = fluids.getCompound(i);
            ResourceLocation name = ResourceLocation.tryParse(row.getString("Fluid"));
            Fluid fluid = name == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(name);
            int amount = row.getInt("Amount");
            if (fluid != null && fluid != Fluids.EMPTY && amount > 0) {
                tanks[i].fluid = fluid;
                tanks[i].amount = Math.min(tanks[i].capacity, amount);
            }
        }
//? if >=1.21.1 {
/*        if (tag.contains("gt.behavior")) state.load(lookup, tag.getCompound("gt.behavior"));
*///?} else {
        if (tag.contains("gt.behavior")) state.load(tag.getCompound("gt.behavior"));
//?}
    }

    private boolean breakOne(BlockPos target) {
        if (level == null || level.isClientSide || !(level instanceof ServerLevel server) || target == null) return false;
        int dx = target.getX() - worldPosition.getX();
        int dy = target.getY() - worldPosition.getY();
        int dz = target.getZ() - worldPosition.getZ();
        if (!BehaviorRangeRules.inside(declaration.maxRange(), dx, dy, dz)) return false;
        if (breaksLeft <= 0 || !level.hasChunkAt(target)) return false;
        BlockState state = level.getBlockState(target);
        if (state.isAir() || state.getDestroySpeed(level, target) < 0) return false;
        Player actor = actor(server);
        List<ItemStack> drops = Block.getDrops(state, server, target, level.getBlockEntity(target), actor, ItemStack.EMPTY);
        if (!dropsFit(drops) || !breakAllowed(server, target, state, actor)) return false;
        if (!level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) return false;
        for (ItemStack drop : drops) context.insertOutput(drop);
        breaksLeft--;
        setChanged();
        return true;
    }

    private int collect(int radius, boolean voidThem) {
        if (level == null || level.isClientSide || !(level instanceof ServerLevel server)) return 0;
        if (!BehaviorRangeRules.radiusAllowed(declaration.maxRange(), radius) || itemsLeft <= 0) return 0;
        var box = new AABB(worldPosition.getX() - radius, worldPosition.getY() - radius, worldPosition.getZ() - radius,
                worldPosition.getX() + radius + 1.0, worldPosition.getY() + radius + 1.0, worldPosition.getZ() + radius + 1.0);
        int taken = 0;
        for (ItemEntity entity : server.getEntitiesOfClass(ItemEntity.class, box)) {
            if (itemsLeft <= 0) break;
            if (!entity.isAlive()) continue;
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) continue;
            if (voidThem) {
                entity.discard();
            } else {
                int before = stack.getCount();
                ItemStack left = context.insertOutput(stack.copy());
                if (left.getCount() == before) continue;
                if (left.isEmpty()) entity.discard();
                else entity.setItem(left);
            }
            itemsLeft--;
            taken++;
        }
        if (taken > 0) setChanged();
        return taken;
    }

    private boolean dropsFit(List<ItemStack> drops) {
        ItemStack[] before = new ItemStack[items.getSlots()];
        for (int i = 0; i < before.length; i++) before[i] = items.getStackInSlot(i).copy();
        boolean ok = true;
        for (ItemStack drop : drops) {
            if (drop == null || drop.isEmpty()) continue;
            if (!context.insertOutput(drop).isEmpty()) { ok = false; break; }
        }
        for (int i = 0; i < before.length; i++) items.setStackInSlot(i, before[i]);
        return ok;
    }

    private Player actor(ServerLevel server) {
        if (placerId != null && server.getServer() != null) {
            ServerPlayer online = server.getServer().getPlayerList().getPlayer(placerId);
            if (online != null) return online;
        }
        UUID id = placerId == null ? new UUID(0, 0) : placerId;
        String name = placerName == null || placerName.isBlank() ? "gt6ce" : placerName;
        if (name.length() > 16) name = name.substring(0, 16);
        var profile = new com.mojang.authlib.GameProfile(id, name);
//? if >=1.21.1 {
/*        var fake = net.neoforged.neoforge.common.util.FakePlayerFactory.get(server, profile);
*///?} else {
        var fake = net.minecraftforge.common.util.FakePlayerFactory.get(server, profile);
//?}
        fake.setPos(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);
        return fake;
    }

    private static boolean breakAllowed(ServerLevel server, BlockPos pos, BlockState state, Player player) {
//? if >=1.21.1 {
/*        var event = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(server, pos, state, player);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
*///?} else {
        var event = new net.minecraftforge.event.level.BlockEvent.BreakEvent(server, pos, state, player);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
//?}
    }

    private boolean insertable(int slot) {
        var role = declaration.items().get(slot).role();
        return role == BehaviorMachine.SlotRole.INPUT || role == BehaviorMachine.SlotRole.ANY;
    }

    private boolean extractable(int slot) {
        var role = declaration.items().get(slot).role();
        return role == BehaviorMachine.SlotRole.OUTPUT || role == BehaviorMachine.SlotRole.ANY;
    }

    private boolean fillable(int tank) {
        var role = declaration.fluids().get(tank).role();
        return role == BehaviorMachine.SlotRole.INPUT || role == BehaviorMachine.SlotRole.ANY;
    }

    private boolean drainable(int tank) {
        var role = declaration.fluids().get(tank).role();
        return role == BehaviorMachine.SlotRole.OUTPUT || role == BehaviorMachine.SlotRole.ANY;
    }

    private final class Context implements MachineBehaviorContext {
        @Override public Level level() { return BehaviorMachineBlockEntity.this.level; }
        @Override public BlockPos pos() { return worldPosition; }
        @Override public Direction facing() {
            return getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        }
        @Override public int tier() { return declaration.casing().tier(); }
        @Override public int itemSlots() { return items.getSlots(); }
        @Override public ItemStack item(int slot) { return items.getStackInSlot(slot); }
        @Override public void setItem(int slot, ItemStack stack) { items.setStackInSlot(slot, stack == null ? ItemStack.EMPTY : stack.copy()); }
        @Override public ItemStack insertOutput(ItemStack stack) {
            ItemStack left = stack == null ? ItemStack.EMPTY : stack.copy();
            for (int slot = 0; slot < items.getSlots() && !left.isEmpty(); slot++) {
                if (declaration.items().get(slot).role() == BehaviorMachine.SlotRole.OUTPUT)
                    left = items.insertItem(slot, left, false);
            }
            for (int slot = 0; slot < items.getSlots() && !left.isEmpty(); slot++) {
                if (declaration.items().get(slot).role() == BehaviorMachine.SlotRole.ANY)
                    left = items.insertItem(slot, left, false);
            }
            return left;
        }
        @Override public int tanks() { return tanks.length; }
        @Override public FluidStack fluid(int tank) { return tanks[tank].copy(); }
        @Override public int capacity(int tank) { return tanks[tank].capacity; }
        @Override public int fill(int tank, FluidStack stack, boolean simulate) {
            int taken = tanks[tank].fill(stack, simulate);
            if (!simulate && taken > 0) setChanged();
            return taken;
        }
        @Override public FluidStack drain(int tank, int amount, boolean simulate) {
            FluidStack drained = tanks[tank].drain(amount, simulate);
            if (!simulate && !drained.isEmpty()) setChanged();
            return drained;
        }
        @Override public long energy() { return stored; }
        @Override public boolean consume(long amount) {
            if (amount < 0) throw new IllegalArgumentException("negative energy");
            if (stored < amount) return false;
            stored -= amount;
            setChanged();
            return true;
        }
        @Override public long energy(String type) {
            if (declaration.auxiliary() == null || !sameEnergy(type, declaration.auxiliary().type())) return 0;
            return auxiliaryStored;
        }
        @Override public boolean consume(String type, long amount) {
            if (amount < 0) throw new IllegalArgumentException("negative energy");
            if (declaration.auxiliary() == null || !sameEnergy(type, declaration.auxiliary().type())) return false;
            if (auxiliaryStored < amount) return false;
            auxiliaryStored -= amount;
            setChanged();
            return true;
        }
        @Override public long generate(long amount) {
            if (amount < 0) throw new IllegalArgumentException("negative energy");
            long room = bufferCapacity() - Math.min(stored, bufferCapacity());
            long added = Math.min(amount, Math.max(0, room));
            if (added <= 0) return 0;
            stored += added;
            setChanged();
            return added;
        }
        @Override public long capacity() { return capacity; }
        @Override public void setCapacity(long value) {
            if (value < 0) throw new IllegalArgumentException("gt.capacity " + value);
            capacity = value;
            capacitySet = true;
            setChanged();
        }
        @Override public long emit(long size, long amount) { return BehaviorMachineBlockEntity.this.emit(size, amount); }
        @Override public boolean toggle(int index) { return (toggles & (1 << index)) != 0; }
        @Override public void setToggle(int index, boolean on) {
            if (index < 0 || index >= declaration.toggles().size())
                throw new IllegalArgumentException("toggle " + index);
            toggles = on ? toggles | (1 << index) : toggles & ~(1 << index);
            setChanged();
        }
        @Override public float progress() { return progress / 1000.0F; }
        @Override public void progress(float fraction) {
            float clamped = Math.max(0.0F, Math.min(1.0F, fraction));
            int next = Math.round(clamped * 1000.0F);
            if (next != progress) {
                progress = next;
                setChanged();
            }
        }
        @Override public void working(boolean value) { working = value; }
        @Override public boolean structureFormed() { return formed; }
        @Override public List<EnergyPart> energyParts() { return formed ? energyParts : List.of(); }
        @Override public boolean conditionsMet() {
            for (EnergyPart part : energyParts()) if (part.condition() && !part.satisfied()) return false;
            return true;
        }
        @Override public BehaviorState state() { return state; }
        @Override public boolean breakBlock(BlockPos pos) { return breakOne(pos); }
        @Override public int collectItems(int radius, boolean voidThem) { return collect(radius, voidThem); }
        @Override public int pushCompressedGas(Direction face, FluidStack stack) {
            if (face == null || stack == null || stack.isEmpty() || level == null) return 0;
            BlockPos target = worldPosition.relative(face);
            if (!level.hasChunkAt(target)) return 0;
            if (!(level.getBlockEntity(target) instanceof CompressedGasTankBlockEntity drum)) return 0;
            return drum.fillCompressed(stack);
        }
    }

    private void refreshStructure() {
        var layout = LargeMachineLayouts.cells(declaration.casing().machineType());
        if (layout == null) {
            formed = true;
            energyParts = List.of();
            return;
        }
        formed = claimStructure(layout);
        if (!formed) energyParts = List.of();
    }

    private boolean claimStructure(List<LargeMachineLayouts.Cell> layout) {
        if (level == null || isRemoved()) {
            structureBindings.clear(this::releaseStructure);
            return false;
        }
        Direction front = getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        var parts = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
        for (var cell : layout) {
            BlockPos cellPos = cell.at(worldPosition, front);
            if (!level.hasChunkAt(cellPos)) {
                structureBindings.clear(this::releaseStructure);
                return false;
            }
            if (cell.role() == MultiblockLayout.Role.AIR) {
                if (level.isEmptyBlock(cellPos)) continue;
                structureBindings.clear(this::releaseStructure);
                return false;
            }
            if (cell.plainBlock()) {
                if (!cell.matches(level, level.getBlockState(cellPos))) {
                    structureBindings.clear(this::releaseStructure);
                    return false;
                }
                continue;
            }
            if (level.getBlockState(cellPos).getBlock() != cell.block()
                    || !(level.getBlockEntity(cellPos) instanceof MultiblockPortBlockEntity)) {
                structureBindings.clear(this::releaseStructure);
                return false;
            }
            parts.put(cellPos, cell.role());
        }
        boolean claimed = structureBindings.update(parts,
                pos -> ((MultiblockPortBlockEntity) level.getBlockEntity(pos)).canBind(worldPosition),
                (pos, role) -> ((MultiblockPortBlockEntity) level.getBlockEntity(pos)).bind(worldPosition, role),
                this::releaseStructure);
        if (!claimed) return false;
        var found = new ArrayList<EnergyPart>();
        for (BlockPos cellPos : parts.keySet()) {
            if (level.getBlockEntity(cellPos) instanceof AddonEnergyPartBlockEntity energy)
                found.add(new EnergyPartView(energy));
        }
        energyParts = List.copyOf(found);
        return true;
    }

    private void releaseStructure(BlockPos pos) {
        if (level != null && level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part)
            part.release(worldPosition);
    }

    @Override public void setRemoved() {
        structureBindings.clear(this::releaseStructure);
        energyParts = List.of();
        formed = false;
        super.setRemoved();
    }

    private final class EnergyPartView implements EnergyPart {
        private final AddonEnergyPartBlockEntity part;
        private EnergyPartView(AddonEnergyPartBlockEntity part) { this.part = part; }
        @Override public BlockPos pos() { return part.getBlockPos(); }
        @Override public String name() { return part.path(); }
        @Override public long stored() { return part.stored(); }
        @Override public long capacity() { return part.capacity(); }
        @Override public long charge(long amount) {
            return part.isBoundTo(worldPosition) ? part.charge(amount) : 0;
        }
        @Override public void setOutput(long size, long amount) {
            if (!part.isBoundTo(worldPosition))
                throw new IllegalStateException("Energy part is not bound to this machine");
            part.setOutput(size, amount);
        }
        @Override public void stop() {
            if (part.isBoundTo(worldPosition)) part.stop();
        }
        @Override public boolean condition() { return part.condition(); }
        @Override public boolean satisfied() { return part.satisfied(); }
    }

    private final class PipeItems implements IItemHandler {
        @Override public int getSlots() { return items.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!insertable(slot) || stack == null || stack.isEmpty() || !behavior.canInsert(context, slot, stack)) return stack;
            return items.insertItem(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return extractable(slot) ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return insertable(slot) && behavior.canInsert(context, slot, stack);
        }
    }

    private final class PipeFluids implements IFluidHandler {
        @Override public int getTanks() { return tanks.length; }
        @Override public FluidStack getFluidInTank(int tank) { return tanks[tank].copy(); }
        @Override public int getTankCapacity(int tank) { return tanks[tank].capacity; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return fillable(tank) && behavior.canFill(context, tank, stack);
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty()) return 0;
            for (int tank = 0; tank < tanks.length; tank++) {
                if (!fillable(tank)) continue;
                int taken = behavior.acceptFluid(context, tank, resource, action.simulate());
                if (taken > 0) return taken;
            }
            return 0;
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty()) return FluidStack.EMPTY;
            for (int tank = 0; tank < tanks.length; tank++) {
                if (!drainable(tank) || tanks[tank].fluid != resource.getFluid()) continue;
                FluidStack drained = tanks[tank].drain(resource.getAmount(), action.simulate());
                if (!action.simulate() && !drained.isEmpty()) setChanged();
                return drained;
            }
            return FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            for (int tank = 0; tank < tanks.length; tank++) {
                if (!drainable(tank)) continue;
                FluidStack drained = tanks[tank].drain(maxDrain, action.simulate());
                if (!drained.isEmpty()) {
                    if (!action.simulate()) setChanged();
                    return drained;
                }
            }
            return FluidStack.EMPTY;
        }
    }

    private static final class Tank {
        final int capacity;
        Fluid fluid = Fluids.EMPTY;
        int amount;

        Tank(BehaviorMachine.FluidSlot slot) { this.capacity = slot.capacity(); }

        FluidStack copy() {
            return amount <= 0 || fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, amount);
        }

        int fill(FluidStack stack, boolean simulate) {
            if (stack == null || stack.isEmpty()) return 0;
            if (amount > 0 && fluid != stack.getFluid()) return 0;
            int taken = Math.min(capacity - amount, stack.getAmount());
            if (taken <= 0) return 0;
            if (!simulate) {
                fluid = stack.getFluid();
                amount += taken;
            }
            return taken;
        }

        FluidStack drain(int requested, boolean simulate) {
            if (amount <= 0 || requested <= 0 || fluid == Fluids.EMPTY) return FluidStack.EMPTY;
            int taken = Math.min(amount, requested);
            Fluid drained = fluid;
            if (!simulate) {
                amount -= taken;
                if (amount == 0) fluid = Fluids.EMPTY;
            }
            return new FluidStack(drained, taken);
        }
    }

    private final class StoredState implements BehaviorState {
        final Map<String, Integer> ints = new LinkedHashMap<>();
        final Map<String, Long> longs = new LinkedHashMap<>();
        final Map<String, Boolean> bools = new LinkedHashMap<>();
        final Map<String, String> strings = new LinkedHashMap<>();
        final Map<String, ItemStack> items = new LinkedHashMap<>();

        @Override public int getInt(String key) { return ints.getOrDefault(check(key), 0); }
        @Override public void setInt(String key, int value) { ints.put(check(key), value); setChanged(); }
        @Override public long getLong(String key) { return longs.getOrDefault(check(key), 0L); }
        @Override public void setLong(String key, long value) { longs.put(check(key), value); setChanged(); }
        @Override public boolean getBoolean(String key) { return bools.getOrDefault(check(key), false); }
        @Override public void setBoolean(String key, boolean value) { bools.put(check(key), value); setChanged(); }
        @Override public String getString(String key) { return strings.getOrDefault(check(key), ""); }
        @Override public void setString(String key, String value) {
            if (value == null) throw new IllegalArgumentException("null behavior string");
            if (value.length() > 256) throw new IllegalArgumentException("behavior string is too long");
            strings.put(check(key), value);
            setChanged();
        }
        @Override public ItemStack item(String key) {
            ItemStack stack = items.get(check(key));
            return stack == null ? ItemStack.EMPTY : stack.copy();
        }
        @Override public void setItem(String key, ItemStack stack) {
            String checked = check(key);
            if (stack == null || stack.isEmpty()) items.remove(checked);
            else items.put(checked, stack.copy());
            setChanged();
        }

//? if >=1.21.1 {
/*        CompoundTag save(HolderLookup.Provider lookup) {
*///?} else {
        CompoundTag save() {
//?}
            CompoundTag tag = new CompoundTag();
            CompoundTag intTag = new CompoundTag();
            ints.forEach(intTag::putInt);
            CompoundTag longTag = new CompoundTag();
            longs.forEach(longTag::putLong);
            CompoundTag boolTag = new CompoundTag();
            bools.forEach(boolTag::putBoolean);
            CompoundTag stringTag = new CompoundTag();
            strings.forEach(stringTag::putString);
            CompoundTag itemTag = new CompoundTag();
//? if >=1.21.1 {
/*            items.forEach((key, stack) -> { if (!stack.isEmpty()) itemTag.put(key, stack.save(lookup)); });
*///?} else {
            items.forEach((key, stack) -> { if (!stack.isEmpty()) itemTag.put(key, stack.save(new CompoundTag())); });
//?}
            tag.put("i", intTag);
            tag.put("l", longTag);
            tag.put("b", boolTag);
            tag.put("s", stringTag);
            tag.put("t", itemTag);
            return tag;
        }

//? if >=1.21.1 {
/*        void load(HolderLookup.Provider lookup, CompoundTag tag) {
*///?} else {
        void load(CompoundTag tag) {
//?}
            ints.clear(); longs.clear(); bools.clear(); strings.clear(); items.clear();
            CompoundTag intTag = tag.getCompound("i");
            for (String key : intTag.getAllKeys()) ints.put(key, intTag.getInt(key));
            CompoundTag longTag = tag.getCompound("l");
            for (String key : longTag.getAllKeys()) longs.put(key, longTag.getLong(key));
            CompoundTag boolTag = tag.getCompound("b");
            for (String key : boolTag.getAllKeys()) bools.put(key, boolTag.getBoolean(key));
            CompoundTag stringTag = tag.getCompound("s");
            for (String key : stringTag.getAllKeys()) strings.put(key, stringTag.getString(key));
            CompoundTag itemTag = tag.getCompound("t");
            for (String key : itemTag.getAllKeys()) {
//? if >=1.21.1 {
/*                ItemStack stack = ItemStack.parseOptional(lookup, itemTag.getCompound(key));
*///?} else {
                ItemStack stack = ItemStack.of(itemTag.getCompound(key));
//?}
                if (!stack.isEmpty()) items.put(key, stack);
            }
        }

        private static String check(String key) {
            if (key == null || !key.matches("[a-z0-9_.]{1,64}"))
                throw new IllegalArgumentException("Invalid behavior state key: " + key);
            return key;
        }
    }
}
