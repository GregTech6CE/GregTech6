package gregtech.blockentity.machine;

import gregtech.api.addon.CrucibleContent;
import gregtech.api.addon.LayoutPart;
import gregtech.api.addon.PartSlotRules;
import gregtech.api.addon.PartSlots;
import gregtech.api.addon.PartTankRules;
import gregtech.api.addon.PartTanks;
import gregtech.api.fluid.FluidTankGT;
import gregtech.api.addon.LayoutPartHosts;
import gregtech.api.addon.platform.BehaviorState;
import gregtech.api.addon.platform.MachineBehavior;
import gregtech.api.addon.platform.PartBehavior;
import gregtech.api.addon.platform.PartContext;
import gregtech.api.addon.platform.PartHit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Slots, tanks, tick, click and named fields for one part. Absent on a casing and on an energy-only part.
 * Saved under {@code gt.part_slots}, {@code gt.part_tanks} and {@code gt.part_fields}, never under the energy keys.
 */
public final class FunctionalPartRuntime {
    /** The block entity that owns this runtime. */
    public interface Host {
        Level level();

        BlockPos pos();

        BlockState blockState();

        String addonId();

        LayoutPart declaration();

        boolean removed();

        @Nullable BlockPos controllerPos();

        boolean boundTo(BlockPos owner);

        void markChanged();

        void syncToClient();

        @Nullable FunctionalPartRuntime function();
    }

    private final Host host;
    @Nullable private final PartSlots slots;
    @Nullable private final PartTanks tankRules;
    private final boolean ticks;
    private final boolean handlesUse;
    private final Set<String> fieldNames;
    private final PartBehavior behavior;
    private final Slots items;
    private final FluidTankGT[] tanks;
    private final DeclaredState state = new DeclaredState();
    private final Context context = new Context();
    private final Object[] sided = new Object[7];
    private final Object[] fluidSided = new Object[7];
    private boolean loading;
    private int notifying;

    public FunctionalPartRuntime(Host host) {
        this.host = host;
        LayoutPart part = host.declaration();
        this.slots = part.slots();
        this.tankRules = part.tanks();
        this.ticks = part.ticks();
        this.handlesUse = part.handlesUse();
        this.fieldNames = Set.copyOf(part.fields());
        this.behavior = LayoutPartHosts.create(host.addonId(), part.name());
        this.items = new Slots(slots == null ? 0 : slots.count());
        this.tanks = new FluidTankGT[tankRules == null ? 0 : tankRules.count()];
        for (int i = 0; i < tanks.length; i++)
            tanks[i] = new FluidTankGT(tankRules.capacity()).setOnChanged(this::tankChanged);
    }

    public LayoutPart declaration() { return host.declaration(); }

    public boolean opensMenu() { return host.declaration().gui() != null; }

    public PartBehavior behavior() { return behavior; }

    public PartContext context() { return context; }

    public int slotCount() { return items.getSlots(); }

    public int tankCount() { return tanks.length; }

    public boolean handlesUse() { return handlesUse; }

    public ItemStack item(int slot) {
        if (slot < 0 || slot >= items.getSlots()) return ItemStack.EMPTY;
        return items.getStackInSlot(slot).copy();
    }

    public void tick() {
        Level level = host.level();
        if (!ticks || level == null || level.isClientSide) return;
        behavior.tick(context);
    }

    public boolean onUse(Player player, InteractionHand hand, PartHit hit) {
        return handlesUse && behavior.onUse(context, player, hand, hit);
    }

    public boolean onWrench(Player player, PartHit hit) {
        return behavior.onWrench(context, player, hit);
    }

    public List<Component> describe() {
        var lines = new ArrayList<Component>(behavior.describe(context));
        for (int tank = 0; tank < tanks.length; tank++) {
            FluidStack fluid = tanks[tank].getFluid();
            String name = fluid.isEmpty() ? "empty" : fluid.getDisplayName().getString();
            lines.add(Component.literal("tank " + tank + " " + name + " "
                    + tanks[tank].getAmount() + "/" + tanks[tank].getCapacity()));
        }
        return lines;
    }

    private void tankChanged() {
        if (loading) return;
        host.markChanged();
        host.syncToClient();
    }

//? if >=1.21.1 {
/*    public net.neoforged.neoforge.items.IItemHandler items(Direction side) {
*///?} else {
    public net.minecraftforge.items.IItemHandler items(Direction side) {
//?}
        if (slots == null) return null;
        int index = side == null ? 6 : side.ordinal();
        if (sided[index] == null) sided[index] = new Sided(side);
//? if >=1.21.1 {
/*        return (net.neoforged.neoforge.items.IItemHandler) sided[index];
*///?} else {
        return (net.minecraftforge.items.IItemHandler) sided[index];
//?}
    }

//? if >=1.21.1 {
/*    public net.neoforged.neoforge.items.IItemHandler inventory() { return items; }
    public IFluidHandler fluids(Direction side) {
*///?} else {
    public net.minecraftforge.items.IItemHandler inventory() { return items; }
    public IFluidHandler fluids(Direction side) {
//?}
        if (tanks.length == 0) return null;
        int index = side == null ? 6 : side.ordinal();
        if (fluidSided[index] == null) fluidSided[index] = new SidedFluid(side);
        return (IFluidHandler) fluidSided[index];
    }

    public void dropInto(Level level, BlockPos pos) {
        if (level == null || level.isClientSide) return;
        for (ItemStack stack : spilled())
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        clearSpilled();
    }

    /** Stacks to spill when the block breaks. Empty when the declaration does not drop them. */
    public List<ItemStack> spilled() {
        if (!spill()) return List.of();
        var out = new ArrayList<ItemStack>();
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) out.add(stack.copy());
        }
        for (String key : fieldNames) {
            ItemStack stack = state.item(key);
            if (!stack.isEmpty()) out.add(stack.copy());
        }
        return out;
    }

    public void clearSpilled() {
        loading = true;
        try {
            for (int slot = 0; slot < items.getSlots(); slot++) items.setStackInSlot(slot, ItemStack.EMPTY);
            for (String key : fieldNames) state.setItem(key, ItemStack.EMPTY);
        } finally {
            loading = false;
        }
    }

    private boolean spill() { return slots == null || slots.drops(); }

    private Direction facing() { return PartFaces.facing(host.blockState()); }

    private boolean inserts(Direction side) {
        return slots != null && side != null && PartSlotRules.inserts(slots, PartFaces.relative(facing(), side));
    }

    private boolean extracts(Direction side) {
        return slots != null && side != null && PartSlotRules.extracts(slots, PartFaces.relative(facing(), side));
    }

    private boolean fills(Direction side) {
        return tankRules != null && side != null && PartTankRules.inserts(tankRules, PartFaces.relative(facing(), side));
    }

    private boolean drains(Direction side) {
        return tankRules != null && side != null && PartTankRules.extracts(tankRules, PartFaces.relative(facing(), side));
    }

//? if >=1.21.1 {
/*    public void save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        saveSlots(tag, lookup);
        saveTanks(tag, lookup);
        tag.put("gt.part_fields", state.save(lookup));
    }

    public void load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        loading = true;
        try {
            loadSlots(tag, lookup);
            loadTanks(tag, lookup);
            if (tag.contains("gt.part_fields")) state.load(lookup, tag.getCompound("gt.part_fields"));
        } finally {
            loading = false;
        }
    }

    private void saveSlots(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        if (slots == null) return;
        ListTag list = new ListTag();
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            list.add(stack.isEmpty() ? new CompoundTag() : stack.save(lookup));
        }
        tag.put("gt.part_slots", list);
    }

    private void loadSlots(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        if (slots == null || !tag.contains("gt.part_slots")) return;
        ListTag list = tag.getList("gt.part_slots", Tag.TAG_COMPOUND);
        for (int slot = 0; slot < items.getSlots() && slot < list.size(); slot++)
            items.setStackInSlot(slot, ItemStack.parseOptional(lookup, list.getCompound(slot)));
    }

    private void saveTanks(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        if (tanks.length == 0) return;
        ListTag list = new ListTag();
        for (FluidTankGT tank : tanks) {
            CompoundTag saved = new CompoundTag();
            tank.writeToNBT(saved, lookup);
            list.add(saved);
        }
        tag.put("gt.part_tanks", list);
    }

    private void loadTanks(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        if (tanks.length == 0 || !tag.contains("gt.part_tanks")) return;
        ListTag list = tag.getList("gt.part_tanks", Tag.TAG_COMPOUND);
        for (int tank = 0; tank < tanks.length && tank < list.size(); tank++) {
            tanks[tank].readFromNBT(list.getCompound(tank), lookup);
            tanks[tank].setCapacity(tankRules.capacity());
        }
    }
*///?} else {
    public void save(CompoundTag tag) {
        saveSlots(tag);
        saveTanks(tag);
        tag.put("gt.part_fields", state.save());
    }

    public void load(CompoundTag tag) {
        loading = true;
        try {
            loadSlots(tag);
            loadTanks(tag);
            if (tag.contains("gt.part_fields")) state.load(tag.getCompound("gt.part_fields"));
        } finally {
            loading = false;
        }
    }

    private void saveSlots(CompoundTag tag) {
        if (slots == null) return;
        ListTag list = new ListTag();
        for (int slot = 0; slot < items.getSlots(); slot++)
            list.add(items.getStackInSlot(slot).save(new CompoundTag()));
        tag.put("gt.part_slots", list);
    }

    private void loadSlots(CompoundTag tag) {
        if (slots == null || !tag.contains("gt.part_slots")) return;
        ListTag list = tag.getList("gt.part_slots", Tag.TAG_COMPOUND);
        for (int slot = 0; slot < items.getSlots() && slot < list.size(); slot++)
            items.setStackInSlot(slot, ItemStack.of(list.getCompound(slot)));
    }

    private void saveTanks(CompoundTag tag) {
        if (tanks.length == 0) return;
        ListTag list = new ListTag();
        for (FluidTankGT tank : tanks) {
            CompoundTag saved = new CompoundTag();
            tank.writeToNBT(saved);
            list.add(saved);
        }
        tag.put("gt.part_tanks", list);
    }

    private void loadTanks(CompoundTag tag) {
        if (tanks.length == 0 || !tag.contains("gt.part_tanks")) return;
        ListTag list = tag.getList("gt.part_tanks", Tag.TAG_COMPOUND);
        for (int tank = 0; tank < tanks.length && tank < list.size(); tank++) {
            tanks[tank].readFromNBT(list.getCompound(tank));
            tanks[tank].setCapacity(tankRules.capacity());
        }
    }
//?}

//? if >=1.21.1 {
/*    private final class Slots extends net.neoforged.neoforge.items.ItemStackHandler {
*///?} else {
    private final class Slots extends net.minecraftforge.items.ItemStackHandler {
//?}
        Slots(int count) { super(count); }

        @Override public int getSlotLimit(int slot) { return slots == null ? 0 : slots.stackLimit(); }

        @Override protected void onContentsChanged(int slot) {
            if (loading || notifying > 0) return;
            notifying++;
            try {
                host.markChanged();
                behavior.onSlotsChanged(context, slot);
                host.syncToClient();
            } finally {
                notifying--;
            }
        }
    }

//? if >=1.21.1 {
/*    private final class Sided implements net.neoforged.neoforge.items.IItemHandler {
*///?} else {
    private final class Sided implements net.minecraftforge.items.IItemHandler {
//?}
        private final Direction side;

        Sided(Direction side) { this.side = side; }

        @Override public int getSlots() { return items.getSlots(); }

        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }

        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!inserts(side) || stack == null || stack.isEmpty() || !behavior.canInsert(context, slot, stack))
                return stack;
            return items.insertItem(slot, stack, simulate);
        }

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!extracts(side) || !behavior.canExtract(context, slot)) return ItemStack.EMPTY;
            return items.extractItem(slot, amount, simulate);
        }

        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }

        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return inserts(side) && behavior.canInsert(context, slot, stack);
        }
    }

    private final class SidedFluid implements IFluidHandler {
        private final Direction side;

        SidedFluid(Direction side) { this.side = side; }

        @Override public int getTanks() { return tanks.length; }

        @Override public FluidStack getFluidInTank(int tank) {
            return tank < 0 || tank >= tanks.length ? FluidStack.EMPTY : tanks[tank].getFluid();
        }

        @Override public int getTankCapacity(int tank) {
            return tank < 0 || tank >= tanks.length ? 0 : tanks[tank].getCapacity();
        }

        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return tank >= 0 && tank < tanks.length && fills(side) && behavior.canFill(context, tank, stack);
        }

        @Override public int fill(FluidStack resource, FluidAction action) {
            if (!fills(side) || resource == null || resource.isEmpty()) return 0;
            for (int tank = 0; tank < tanks.length; tank++) {
                if (!behavior.canFill(context, tank, resource)) continue;
                int taken = tanks[tank].fill(resource, action);
                if (taken > 0) return taken;
            }
            return 0;
        }

        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!drains(side) || resource == null || resource.isEmpty()) return FluidStack.EMPTY;
            for (int tank = 0; tank < tanks.length; tank++) {
                if (!behavior.canDrain(context, tank)) continue;
                FluidStack drained = tanks[tank].drain(resource, action);
                if (!drained.isEmpty()) return drained;
            }
            return FluidStack.EMPTY;
        }

        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            if (!drains(side)) return FluidStack.EMPTY;
            for (int tank = 0; tank < tanks.length; tank++) {
                if (!behavior.canDrain(context, tank)) continue;
                FluidStack drained = tanks[tank].drain(maxDrain, action);
                if (!drained.isEmpty()) return drained;
            }
            return FluidStack.EMPTY;
        }
    }

    private final class Context implements PartContext {
        @Override public Level level() { return host.level(); }

        @Override public BlockPos pos() { return host.pos(); }

        @Override public Direction facing() { return FunctionalPartRuntime.this.facing(); }

        @Override public String name() { return host.declaration().name(); }

        @Override public int slots() { return items.getSlots(); }

        @Override public ItemStack item(int slot) { return FunctionalPartRuntime.this.item(slot); }

        @Override public void setItem(int slot, ItemStack stack) {
            if (slot < 0 || slot >= items.getSlots())
                throw new IllegalArgumentException("slot " + slot);
            ItemStack next = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            int limit = items.getSlotLimit(slot);
            if (!next.isEmpty() && next.getCount() > limit) next.setCount(limit);
            items.setStackInSlot(slot, next);
        }

        @Override public int tanks() { return FunctionalPartRuntime.this.tanks.length; }

        @Override public FluidStack fluid(int tank) {
            if (tank < 0 || tank >= tanks()) return FluidStack.EMPTY;
            return FunctionalPartRuntime.this.tanks[tank].getFluid();
        }

        @Override public int tankCapacity(int tank) {
            if (tank < 0 || tank >= tanks()) return 0;
            return FunctionalPartRuntime.this.tanks[tank].getCapacity();
        }

        @Override public int fill(int tank, FluidStack stack, boolean simulate) {
            if (tank < 0 || tank >= tanks()) throw new IllegalArgumentException("tank " + tank);
            return FunctionalPartRuntime.this.tanks[tank].fill(stack,
                    simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
        }

        @Override public FluidStack drain(int tank, int amount, boolean simulate) {
            if (tank < 0 || tank >= tanks()) throw new IllegalArgumentException("tank " + tank);
            return FunctionalPartRuntime.this.tanks[tank].drain(amount,
                    simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
        }

        @Override public boolean bound() {
            BlockPos owner = host.controllerPos();
            if (owner == null || host.removed() || !host.boundTo(owner)) return false;
            Level level = host.level();
            if (level == null || !level.hasChunkAt(owner)) return false;
            var occupant = level.getBlockEntity(owner);
            return occupant != null && !occupant.isRemoved();
        }

        @Override public BlockPos controllerPos() { return bound() ? host.controllerPos() : null; }

        @Override public MachineBehavior controller() {
            BlockPos owner = controllerPos();
            Level level = host.level();
            if (owner == null || level == null) return null;
            if (!(level.getBlockEntity(owner) instanceof BehaviorMachineBlockEntity machine)) return null;
            if (!machine.structureFormed() || !host.boundTo(owner)) return null;
            return machine.behavior();
        }

        @Override public CrucibleContent crucible() {
            BlockPos owner = controllerPos();
            Level level = host.level();
            if (owner == null || level == null) return CrucibleContent.EMPTY;
            if (!(level.getBlockEntity(owner) instanceof BehaviorMachineBlockEntity machine)) return CrucibleContent.EMPTY;
            if (!machine.structureFormed() || machine.isRemoved() || !host.boundTo(owner)) return CrucibleContent.EMPTY;
            return machine.crucibleContent();
        }

        @Override public BehaviorState state() { return state; }
    }

    private final class DeclaredState implements BehaviorState {
        final Map<String, Integer> ints = new LinkedHashMap<>();
        final Map<String, Long> longs = new LinkedHashMap<>();
        final Map<String, Boolean> bools = new LinkedHashMap<>();
        final Map<String, String> strings = new LinkedHashMap<>();
        final Map<String, ItemStack> storedItems = new LinkedHashMap<>();

        @Override public int getInt(String key) { return ints.getOrDefault(check(key), 0); }
        @Override public void setInt(String key, int value) { ints.put(check(key), value); touched(); }
        @Override public long getLong(String key) { return longs.getOrDefault(check(key), 0L); }
        @Override public void setLong(String key, long value) { longs.put(check(key), value); touched(); }
        @Override public boolean getBoolean(String key) { return bools.getOrDefault(check(key), false); }
        @Override public void setBoolean(String key, boolean value) { bools.put(check(key), value); touched(); }
        @Override public String getString(String key) { return strings.getOrDefault(check(key), ""); }

        @Override public void setString(String key, String value) {
            if (value == null) throw new IllegalArgumentException("null part string");
            if (value.length() > 256) throw new IllegalArgumentException("part string is too long");
            strings.put(check(key), value);
            touched();
        }

        @Override public ItemStack item(String key) {
            ItemStack stack = storedItems.get(check(key));
            return stack == null ? ItemStack.EMPTY : stack.copy();
        }

        @Override public void setItem(String key, ItemStack stack) {
            String checked = check(key);
            if (stack == null || stack.isEmpty()) storedItems.remove(checked);
            else storedItems.put(checked, stack.copy());
            touched();
        }

        private void touched() {
            if (loading) return;
            host.markChanged();
            host.syncToClient();
        }

        private String check(String key) {
            if (key == null || !fieldNames.contains(key))
                throw new IllegalArgumentException("Undeclared part field: " + key);
            return key;
        }

//? if >=1.21.1 {
/*        CompoundTag save(net.minecraft.core.HolderLookup.Provider lookup) {
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
/*            storedItems.forEach((key, stack) -> { if (!stack.isEmpty()) itemTag.put(key, stack.save(lookup)); });
*///?} else {
            storedItems.forEach((key, stack) -> { if (!stack.isEmpty()) itemTag.put(key, stack.save(new CompoundTag())); });
//?}
            tag.put("i", intTag);
            tag.put("l", longTag);
            tag.put("b", boolTag);
            tag.put("s", stringTag);
            tag.put("t", itemTag);
            return tag;
        }

//? if >=1.21.1 {
/*        void load(net.minecraft.core.HolderLookup.Provider lookup, CompoundTag tag) {
*///?} else {
        void load(CompoundTag tag) {
//?}
            ints.clear(); longs.clear(); bools.clear(); strings.clear(); storedItems.clear();
            readInts(tag.getCompound("i"));
            readLongs(tag.getCompound("l"));
            readBools(tag.getCompound("b"));
            readStrings(tag.getCompound("s"));
            CompoundTag itemTag = tag.getCompound("t");
            for (String key : itemTag.getAllKeys()) {
                if (!fieldNames.contains(key)) continue;
//? if >=1.21.1 {
/*                ItemStack stack = ItemStack.parseOptional(lookup, itemTag.getCompound(key));
*///?} else {
                ItemStack stack = ItemStack.of(itemTag.getCompound(key));
//?}
                if (!stack.isEmpty()) storedItems.put(key, stack);
            }
        }

        private void readInts(CompoundTag tag) {
            for (String key : tag.getAllKeys()) if (fieldNames.contains(key)) ints.put(key, tag.getInt(key));
        }

        private void readLongs(CompoundTag tag) {
            for (String key : tag.getAllKeys()) if (fieldNames.contains(key)) longs.put(key, tag.getLong(key));
        }

        private void readBools(CompoundTag tag) {
            for (String key : tag.getAllKeys()) if (fieldNames.contains(key)) bools.put(key, tag.getBoolean(key));
        }

        private void readStrings(CompoundTag tag) {
            for (String key : tag.getAllKeys()) if (fieldNames.contains(key)) strings.put(key, tag.getString(key));
        }
    }

}
