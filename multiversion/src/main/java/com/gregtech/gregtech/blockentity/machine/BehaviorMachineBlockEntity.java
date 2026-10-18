package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.addon.AddonMachines;
import com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration;
import com.gregtech.gregtech.api.addon.BehaviorMachineHosts;
import com.gregtech.gregtech.api.addon.platform.BehaviorState;
//? if >=1.21.1 {
/*import com.gregtech.gregtech.api.addon.platform.EnergyBlock;
*///?} else {
//?}
import com.gregtech.gregtech.api.addon.platform.MachineBehavior;
import com.gregtech.gregtech.api.addon.platform.MachineBehaviorContext;
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
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shell block entity. The addon behavior never sees this class. */
public class BehaviorMachineBlockEntity extends BlockEntity implements EnergyBlock, MenuProvider {
    private final BehaviorMachineDeclaration declaration;
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
    private long stored;
    private int toggles;
    private int progress;
    private boolean working;
    private boolean powered;

    public BehaviorMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        BehaviorMachineBlock block = (BehaviorMachineBlock) state.getBlock();
        this.accepted = block.accepted();
        this.declaration = accepted.declaration();
        this.behavior = BehaviorMachineHosts.create(accepted.addonId(), declaration.machineType());
        this.items = new ItemStackHandler(declaration.items().size()) {
            @Override protected void onContentsChanged(int slot) { setChanged(); }
        };
        this.tanks = new Tank[declaration.fluids().size()];
        for (int i = 0; i < tanks.length; i++) tanks[i] = new Tank(declaration.fluids().get(i));
        this.toggles = declaration.initialToggles();
        this.energyTag = GregTechTags.Energy.ALL.stream()
                .filter(tag -> tag.getShortName().equalsIgnoreCase(declaration.energy()))
                .findFirst().orElseThrow();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BehaviorMachineBlockEntity machine) {
        machine.tickServer(level, pos);
    }

    public BehaviorMachineDeclaration declaration() { return declaration; }
    public AddonMachines.AcceptedBehavior accepted() { return accepted; }
    public BehaviorState state() { return state; }
    public ItemStackHandler inventory() { return items; }
    public IItemHandler items() { return pipeItems; }
    public IFluidHandler fluids() { return pipeFluids; }
    public int tanks() { return tanks.length; }
    public long storedEnergy() { return stored; }
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

    private void tickServer(Level level, BlockPos pos) {
        pollEnergy(level, pos);
        powered = stored > 0;
        working = false;
        behavior.tick(context);
        stored = BasicMachineEnergy.drain(stored, accepted.energyInMax());
        BlockState state = getBlockState();
        if (state.getValue(BehaviorMachineBlock.RUNNING) != powered || state.getValue(BehaviorMachineBlock.LIT) != working)
            level.setBlock(pos, state.setValue(BehaviorMachineBlock.RUNNING, powered).setValue(BehaviorMachineBlock.LIT, working), 3);
    }

    private void pollEnergy(Level level, BlockPos pos) {
        long size = declaration.energyPerTick();
        for (Direction side : Direction.values()) {
            if (!accepts(side)) continue;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(side));
            IEnergyBlock source = EnergyBlockViews.view(neighbor);
            if (source == null || !source.isEnergyEmittingTo(energyTag, side.getOpposite(), false)) continue;
            long offered = source.getEnergyOffered(energyTag, side.getOpposite(), size);
            long requested = BasicMachineEnergy.accepted(stored, accepted.energyInMax(), size, offered);
            if (requested <= 0) continue;
            long extracted = source.doEnergyExtraction(energyTag, side.getOpposite(), size, requested, true);
            if (extracted > 0) stored = BasicMachineEnergy.add(stored, accepted.energyInMax(), size, extracted);
        }
    }

    private boolean accepts(Direction side) {
        return side == null || FaceConfig.has(declaration.energyMask(), relative(side));
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
        return !emitting && energy != null && energy.equalsIgnoreCase(declaration.energy());
    }

    @Override public java.util.Collection<String> getEnergyTypes(Direction side) { return List.of(declaration.energy()); }

    @Override
    public boolean isEnergyAcceptingFrom(String energy, Direction side, boolean theoretical) {
        return isEnergyType(energy, side, false) && accepts(side);
    }

    @Override public boolean isEnergyEmittingTo(String energy, Direction side, boolean theoretical) { return false; }

    @Override
    public long doInject(String energy, Direction side, long size, long amount, boolean doInject) {
        if (!isEnergyAcceptingFrom(energy, side, false) || size == 0 || size == Long.MIN_VALUE || amount <= 0) return 0;
        long sizeAbs = Math.abs(size);
        if (sizeAbs > accepted.energyInMax()) {
            if (doInject) explode(sizeAbs);
            return amount;
        }
        long taken = BasicMachineEnergy.accepted(stored, accepted.energyInMax(), size, amount);
        if (doInject && taken > 0) {
            stored = BasicMachineEnergy.add(stored, accepted.energyInMax(), size, taken);
            setChanged();
        }
        return taken;
    }

    private void explode(long size) {
        if (level == null || level.isClientSide) return;
        if (!com.gregtech.gregtech.GregTechConfig.machineOvervoltageExplosions()) {
            stored = 0;
            return;
        }
        float over = (float) ((double) size / Math.max(1, accepted.energyInMax()));
        float power = Math.min(8.0F, 3.0F + (float) (Math.log(over) / Math.log(2.0)));
        BlockPos pos = worldPosition;
        level.removeBlock(pos, false);
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                Math.max(3.0F, power), Level.ExplosionInteraction.BLOCK);
    }

    @Override
    public long getEnergyDemanded(String energy, Direction side, long size) {
        if (!isEnergyAcceptingFrom(energy, side, false) || size == 0 || size == Long.MIN_VALUE) return 0;
        return BasicMachineEnergy.demanded(stored, accepted.energyInMax(), size);
    }

    @Override public long getEnergyOffered(String energy, Direction side, long size) { return 0; }
    @Override public long getEnergySizeInputRecommended(String energy, Direction side) { return declaration.energyPerTick(); }
    @Override public long getEnergySizeOutputRecommended(String energy, Direction side) { return 0; }
    @Override public long getEnergySizeInputMin(String energy, Direction side) { return accepted.energyInMin(); }
    @Override public long getEnergySizeInputMax(String energy, Direction side) { return accepted.energyInMax(); }

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
        tag.putInt("gt.progress", progress);
        tag.putInt("gt.toggles", toggles);
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
        stored = Math.max(0, Math.min(accepted.energyInMax(), tag.getLong("gt.energy")));
        progress = tag.getInt("gt.progress");
        toggles = tag.getInt("gt.toggles");
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

    private boolean insertable(int slot) {
        var role = declaration.items().get(slot).role();
        return role == BehaviorMachineDeclaration.SlotRole.INPUT || role == BehaviorMachineDeclaration.SlotRole.ANY;
    }

    private boolean extractable(int slot) {
        var role = declaration.items().get(slot).role();
        return role == BehaviorMachineDeclaration.SlotRole.OUTPUT || role == BehaviorMachineDeclaration.SlotRole.ANY;
    }

    private boolean fillable(int tank) {
        var role = declaration.fluids().get(tank).role();
        return role == BehaviorMachineDeclaration.SlotRole.INPUT || role == BehaviorMachineDeclaration.SlotRole.ANY;
    }

    private boolean drainable(int tank) {
        var role = declaration.fluids().get(tank).role();
        return role == BehaviorMachineDeclaration.SlotRole.OUTPUT || role == BehaviorMachineDeclaration.SlotRole.ANY;
    }

    private final class Context implements MachineBehaviorContext {
        @Override public Level level() { return BehaviorMachineBlockEntity.this.level; }
        @Override public BlockPos pos() { return worldPosition; }
        @Override public Direction facing() {
            return getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        }
        @Override public int tier() { return declaration.tier(); }
        @Override public int itemSlots() { return items.getSlots(); }
        @Override public ItemStack item(int slot) { return items.getStackInSlot(slot); }
        @Override public void setItem(int slot, ItemStack stack) { items.setStackInSlot(slot, stack == null ? ItemStack.EMPTY : stack.copy()); }
        @Override public ItemStack insertOutput(ItemStack stack) {
            ItemStack left = stack == null ? ItemStack.EMPTY : stack.copy();
            for (int slot = 0; slot < items.getSlots() && !left.isEmpty(); slot++) {
                if (declaration.items().get(slot).role() == BehaviorMachineDeclaration.SlotRole.OUTPUT)
                    left = items.insertItem(slot, left, false);
            }
            for (int slot = 0; slot < items.getSlots() && !left.isEmpty(); slot++) {
                if (declaration.items().get(slot).role() == BehaviorMachineDeclaration.SlotRole.ANY)
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
        @Override public BehaviorState state() { return state; }
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

        Tank(BehaviorMachineDeclaration.FluidSlot slot) { this.capacity = slot.capacity(); }

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
