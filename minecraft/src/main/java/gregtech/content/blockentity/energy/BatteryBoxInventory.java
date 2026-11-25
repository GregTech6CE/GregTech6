package gregtech.content.blockentity.energy;

import gregtech.client.gui.HopperContainerMenu;
import gregtech.content.cover.ComponentCoverRuntime;
import gregtech.content.data.GregTechTags;
import gregtech.content.energy.BatteryBoxEnergy;
import gregtech.content.registry.GTMenuTypes;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.item.IItemEnergy;
import gregtech.engine.inventory.BlockContents;
import gregtech.engine.stack.NbtCompat;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
*///?} else {
import java.util.EnumMap;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
//?}

/** The menu, automation and energy transfer all see the same fixed battery slots. */
final class BatteryBoxInventory implements IItemHandlerModifiable {
    private final EnergyNodeBlockEntity host;
    private final GregTechTags.Tag energyType;
    final NonNullList<ItemStack> batteries;
    private boolean detached;

    BatteryBoxInventory(EnergyNodeBlockEntity host, EnergyNodeSpec spec) {
        this.host = host;
        energyType = spec.inType();
        batteries = NonNullList.withSize(spec.batterySlots(), ItemStack.EMPTY);
    }

    static long capacityOf(ItemStack stack, GregTechTags.Tag type) {
        if (!BatteryBoxEnergy.accepts(stack, type)) return 0;
        return ((IItemEnergy) stack.getItem()).getEnergyCapacity(stack, type);
    }

    long installedCapacity() {
        long capacity = 0;
        for (var stack : batteries) {
            if (stack.getItem() instanceof IItemEnergy item) {
                capacity += item.getEnergyCapacity(stack, energyType);
            }
        }
        return capacity;
    }

    boolean install(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (int slot = 0; slot < batteries.size(); slot++) {
            if (insertItem(slot, stack.copyWithCount(1), false).isEmpty()) return true;
        }
        return false;
    }

    ItemStack remove() {
        for (int slot = batteries.size() - 1; slot >= 0; slot--) {
            if (!batteries.get(slot).isEmpty()) return extractItem(slot, 1, false);
        }
        return ItemStack.EMPTY;
    }

    private void checkSlot(int slot) {
        if (slot < 0 || slot >= batteries.size()) throw new IndexOutOfBoundsException("Battery slot " + slot);
    }

    private boolean available() {
        return !detached && !host.isRemoved();
    }

    @Override
    public int getSlots() {
        return batteries.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        checkSlot(slot);
        return available() ? batteries.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        checkSlot(slot);
        if (stack.isEmpty() || !isItemValid(slot, stack) || !batteries.get(slot).isEmpty()) return stack;
        if (!simulate) {
            batteries.set(slot, stack.copyWithCount(1));
            host.setChanged();
        }
        return stack.copyWithCount(stack.getCount() - 1);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        checkSlot(slot);
        if (!available() || amount <= 0 || batteries.get(slot).isEmpty()) return ItemStack.EMPTY;
        var result = batteries.get(slot).copy();
        if (!simulate) {
            batteries.set(slot, ItemStack.EMPTY);
            host.setChanged();
        }
        return result;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        checkSlot(slot);
        if (!available()) return;
        if (!stack.isEmpty() && !isItemValid(slot, stack)) throw new IllegalArgumentException("Not an energy item");
        batteries.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        host.setChanged();
    }

    @Override
    public int getSlotLimit(int slot) {
        checkSlot(slot);
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        checkSlot(slot);
        return available() && BatteryBoxEnergy.accepts(stack, energyType);
    }

    MenuProvider menu() {
        return new SimpleMenuProvider(
                (id, playerInventory, player) -> new HopperContainerMenu(
                        GTMenuTypes.forSlotCount(getSlots()), id, playerInventory, this) {
                    @Override
                    public boolean stillValid(Player viewer) {
                        var level = host.getLevel();
                        var pos = host.getBlockPos();
                        return available() && level != null && viewer.level() == level
                                && viewer.distanceToSqr(pos.getCenter()) <= 64
                                && level.hasChunkAt(pos) && level.getBlockEntity(pos) == host;
                    }
                },
                Component.translatable(host.getBlockState().getBlock().getDescriptionId()));
    }

    private IItemHandler sided(Direction side) {
        return new IItemHandler() {
            // Native battery automation checks shutters and component direction. Item filters are separate.
            private boolean allowsTransfer(boolean insert) {
                return available() && !host.panels().shuttered(side)
                        && ComponentCoverRuntime.allowsItem(host.getCover(side), insert);
            }

            @Override
            public int getSlots() {
                return BatteryBoxInventory.this.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return BatteryBoxInventory.this.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return allowsTransfer(true) ? BatteryBoxInventory.this.insertItem(slot, stack, simulate) : stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return allowsTransfer(false) ? BatteryBoxInventory.this.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return BatteryBoxInventory.this.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return BatteryBoxInventory.this.isItemValid(slot, stack) && !host.panels().shuttered(side);
            }
        };
    }

//? if >=1.21.1 {
    /*@Nullable IItemHandler capability(@Nullable Direction side) {
        if (!available()) return null;
        return side == null ? this : sided(side);
    }
*///?} else {
    @Nullable private LazyOptional<IItemHandler> unsidedCapability;
    @Nullable private EnumMap<Direction, LazyOptional<IItemHandler>> sidedCapabilities;

    LazyOptional<IItemHandler> capability(@Nullable Direction side) {
        if (!available()) return LazyOptional.empty();
        if (side != null) {
            if (sidedCapabilities == null) sidedCapabilities = new EnumMap<>(Direction.class);
            return sidedCapabilities.computeIfAbsent(side, face -> LazyOptional.of(() -> sided(face)));
        }
        if (unsidedCapability == null || !unsidedCapability.isPresent()) {
            unsidedCapability = LazyOptional.of(() -> this);
        }
        return unsidedCapability;
    }

    void invalidateCaps() {
        if (unsidedCapability != null) {
            unsidedCapability.invalidate();
            unsidedCapability = null;
        }
        if (sidedCapabilities != null) {
            sidedCapabilities.values().forEach(LazyOptional::invalidate);
            sidedCapabilities.clear();
        }
    }
//?}

    void detach() {
        detached = true;
//? if <1.21.1 {
        invalidateCaps();
//?}
    }

    void save(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        if (batteries.isEmpty()) return;
        ListTag list = new ListTag();
        for (var stack : batteries) list.add(NbtCompat.saveItem(stack, lookup));
        tag.put("gt.batteries", list);
    }

    void load(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        batteries.clear();
        ListTag list = tag.getList("gt.batteries", Tag.TAG_COMPOUND);
        for (int slot = 0; slot < Math.min(batteries.size(), list.size()); slot++) {
            batteries.set(slot, NbtCompat.readItem(list.getCompound(slot), lookup).copyWithCount(1));
        }
    }

    void dropContents() {
        for (var stack : batteries) BlockContents.drop(host, stack);
        batteries.clear();
    }
}
