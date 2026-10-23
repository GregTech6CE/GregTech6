package com.gregtech.gregtech.client.gui;

import com.gregtech.gregtech.block.misc.AdvancedCraftingTableBlockEntity;
import com.gregtech.gregtech.registry.GTMenuTypes;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.*;
*///?} else {
import net.minecraftforge.items.*;
//?}
import javax.annotation.Nullable;

/** AdvancedCraftingTable.png slot coordinates and server-driven result clicks. */
public final class AdvancedCraftingMenu extends AbstractContainerMenu {
    public static final int GRID_START = 22, GRID_END = 31, RESULT = 33, FLUSH = 34, STORE = 35, PLAYER_START = 36;
    @Nullable private final AdvancedCraftingTableBlockEntity table;
    private final ItemStackHandler inventory;
    // EMI uses the output's container address when issuing its instant-craft click. Keep the
    // virtual addresses equal to their menu addresses instead of silently clicking blueprint 0.
    private final SimpleContainer virtual = new SimpleContainer(36);
    private final DataSlot modes = DataSlot.standalone();

    public AdvancedCraftingMenu(int id, Inventory player) {
        this(id, player, null);
    }

    public AdvancedCraftingMenu(int id, Inventory player, @Nullable AdvancedCraftingTableBlockEntity table) {
        super(GTMenuTypes.ADVANCED_CRAFTING.get(), id);
        this.table = table;
        inventory = table == null ? new ItemStackHandler(71) : table.items();
        addSlot(new TableSlot(30, 135, 28) {

            @Override
            public boolean mayPlace(ItemStack stack) {
                return AdvancedCraftingTableBlockEntity.isBlueprint(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < 16; i++) addSlot(new TableSlot(i, 7 + (i % 4) * 18, 8 + (i / 4) * 18));
        for (int i = 0; i < 5; i++) addSlot(new TableSlot(16 + i, 80 + i * 18, 8));
        for (int i = 0; i < 9; i++) addSlot(new TableSlot(21 + i, 80 + (i % 3) * 18, 28 + (i / 3) * 18));
        addSlot(new TableSlot(33, 153, 28));
        addSlot(new TableSlot(34, 153, 64));
        for (int i = 0; i < 3; i++) {
            int x = i == 1 ? 153 : 135, y = i == 0 ? 64 : 46;
            addSlot(new Slot(virtual, RESULT + i, x, y) {

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player p) {
                    return false;
                }
            });
        }
        for (int row = 0; row<3; row++) for (int col = 0; col<9; col++) addSlot(new Slot(player, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col<9; col++) addSlot(new Slot(player, col, 8 + col * 18, 142));
        addDataSlot(modes);
        broadcastChanges();
    }
    private class TableSlot extends SlotItemHandler {
        TableSlot(int slot, int x, int y) {
            super(inventory, slot, x, y);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            // A capacity query must not temporarily empty the grid and cancel flush mode.
            return Math.min(stack.getMaxStackSize(), getMaxStackSize());
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (table != null) table.inventoryChanged();
        }
    }

    /** Only real ingredient/storage slots are available to recipe viewers; never a ghost output. */
    public java.util.List<Slot> craftingSlots() {
        return java.util.List.copyOf(slots.subList(GRID_START, GRID_END));
    }

    public java.util.List<Slot> transferInventorySlots() {
        var result = new java.util.ArrayList<Slot>(slots.subList(1, GRID_START));
        result.addAll(slots.subList(PLAYER_START, slots.size()));
        return result;
    }

    public java.util.List<Slot> transferInputSources() {
        var result = new java.util.ArrayList<Slot>(craftingSlots());
        result.addAll(transferInventorySlots());
        return result;
    }

    public int modes() {
        return modes.get();
    }

    @Override
    public boolean stillValid(Player player) {
        return table == null || table.usableBy(player);
    }

    @Override
    public void broadcastChanges() {
        if (table != null) {
            virtual.setItem(RESULT, table.preview());
            modes.set(table.modes());
        }
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slot, int button, ClickType click, Player player) {
        if (slot >= RESULT && slot <= STORE) {
            if (table == null || !stillValid(player)) return;
            if (slot == FLUSH) table.flushGrid();
            else if (slot == STORE) table.storeGrid();
            else if (click == ClickType.PICKUP || click == ClickType.QUICK_MOVE) craft(player, click == ClickType.QUICK_MOVE, button == 1);
            broadcastChanges();
            return;
        }
        if (table != null && slot == 0 && click == ClickType.QUICK_MOVE && button == 0 && table.writeBlueprint()) {
            broadcastChanges();
            return;
        }
        super.clicked(slot, button, click, player);
        if (table != null) broadcastChanges();
    }

    private void craft(Player player, boolean shift, boolean right) {
        ItemStack first = table.craftOutput();
        if (first.isEmpty()) return;
        int limit = right || shift ? Math.max(1, first.getMaxStackSize() / first.getCount()) : 1;
        if (shift && right) limit *= 36;
        int destination = -1;
        for (int n = 0; n < limit; n++) {
            ItemStack output = table.craftOutput();
            if (!ItemStack.matches(first, output)) break;
            ItemStack held;
            if (shift) {
                if (destination < 0 || !fits(player.getInventory().getItem(destination), output)) {
                    if (destination >= 0 && !right) break;
                    destination = -1;
                    for (int i = 0; i < 36; i++) if (fits(player.getInventory().getItem(i), output)) {
                        destination = i;
                        break;
                    }
                }
                if (destination < 0) break;
                held = player.getInventory().getItem(destination);
            } else {
                held = getCarried();
                if (!fits(held, output)) break;
            }
            ItemStack result = table.craftOne(player);
            if (result.isEmpty()) break;
            ItemStack combined = result.copyWithCount(result.getCount() + held.getCount());
            if (shift) {
                player.getInventory().setItem(destination, combined);
                player.getInventory().setChanged();
            } else setCarried(combined);
        }
    }
//? if >=1.21.1 {
    /*private static boolean fits(ItemStack held, ItemStack result) {
        return (held.isEmpty() || ItemStack.isSameItemSameComponents(held,
                result)) && held.getCount() + result.getCount() <= result.getMaxStackSize();
    }
*///?} else {
    private static boolean fits(ItemStack held, ItemStack result) {
        return (held.isEmpty() || ItemStack.isSameItemSameTags(held, result)) && held.getCount() + result.getCount() <= result.getMaxStackSize();
    }
//?}
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index<0 || index >= slots.size() || index >= RESULT && index <= STORE) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index<PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
        }
        else if (!moveItemStackTo(stack, 1, 22, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
