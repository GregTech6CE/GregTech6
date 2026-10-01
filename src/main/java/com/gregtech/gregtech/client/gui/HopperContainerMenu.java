package com.gregtech.gregtech.client.gui;

import com.gregtech.gregtech.registry.GTMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * GT6 hopper / queuehopper container ({@code ContainerCommonDefault}).
 * Dynamically lays out slots based on {@code slotCount} matching the original GT6 layouts.
 */
public class HopperContainerMenu extends AbstractContainerMenu {
    private final IItemHandler inventory;
    private final int slotCount;
    private final int playerInvY;

    /** Server constructor: uses real BE inventory and the correct MenuType for this slot count. */
    public HopperContainerMenu(MenuType<?> type, int containerId, Inventory playerInv, IItemHandler inventory) {
        super(type, containerId);
        this.inventory = inventory;
        this.slotCount = inventory.getSlots();
        this.playerInvY = addHopperSlots();
        addPlayerInventory(playerInv, playerInvY);
    }

    /** Client constructor (vanilla MenuType factory): creates dummy inventory of the right size. */
    public HopperContainerMenu(int containerId, Inventory playerInv, int slotCount) {
        this(GTMenuTypes.forSlotCount(slotCount), containerId, playerInv, slotCount);
    }

    /** Client constructor for subclasses with their own MenuType. */
    protected HopperContainerMenu(MenuType<?> type, int containerId, Inventory playerInv, int slotCount) {
        super(type, containerId);
        this.inventory = new ItemStackHandler(slotCount);
        this.slotCount = slotCount;
        this.playerInvY = addHopperSlots();
        addPlayerInventory(playerInv, playerInvY);
    }

    public int slotCount() { return slotCount; }
    public int playerInvY() { return playerInvY; }

    private int addHopperSlots() {
        int i = 0;
        switch (slotCount) {
            case 1:
                addSlot(new SlotItemHandler(inventory, i++, 80, 35));
                return 84;
            case 2:
                addSlot(new SlotItemHandler(inventory, i++, 71, 35));
                addSlot(new SlotItemHandler(inventory, i++, 89, 35));
                return 84;
            case 3:
                addSlot(new SlotItemHandler(inventory, i++, 62, 35));
                addSlot(new SlotItemHandler(inventory, i++, 80, 35));
                addSlot(new SlotItemHandler(inventory, i++, 98, 35));
                return 84;
            case 4:
                addSlot(new SlotItemHandler(inventory, i++, 71, 26));
                addSlot(new SlotItemHandler(inventory, i++, 89, 26));
                addSlot(new SlotItemHandler(inventory, i++, 71, 44));
                addSlot(new SlotItemHandler(inventory, i++, 89, 44));
                return 84;
            case 5:
                addSlot(new SlotItemHandler(inventory, i++, 44, 35));
                addSlot(new SlotItemHandler(inventory, i++, 62, 35));
                addSlot(new SlotItemHandler(inventory, i++, 80, 35));
                addSlot(new SlotItemHandler(inventory, i++, 98, 35));
                addSlot(new SlotItemHandler(inventory, i++,116, 35));
                return 84;
            case 6:
                addSlot(new SlotItemHandler(inventory, i++, 62, 26));
                addSlot(new SlotItemHandler(inventory, i++, 80, 26));
                addSlot(new SlotItemHandler(inventory, i++, 98, 26));
                addSlot(new SlotItemHandler(inventory, i++, 62, 44));
                addSlot(new SlotItemHandler(inventory, i++, 80, 44));
                addSlot(new SlotItemHandler(inventory, i++, 98, 44));
                return 84;
            case 7:
                addSlot(new SlotItemHandler(inventory, i++, 26, 35));
                addSlot(new SlotItemHandler(inventory, i++, 44, 35));
                addSlot(new SlotItemHandler(inventory, i++, 62, 35));
                addSlot(new SlotItemHandler(inventory, i++, 80, 35));
                addSlot(new SlotItemHandler(inventory, i++, 98, 35));
                addSlot(new SlotItemHandler(inventory, i++,116, 35));
                addSlot(new SlotItemHandler(inventory, i++,134, 35));
                return 84;
            case 8:
                addSlot(new SlotItemHandler(inventory, i++, 53, 26));
                addSlot(new SlotItemHandler(inventory, i++, 71, 26));
                addSlot(new SlotItemHandler(inventory, i++, 89, 26));
                addSlot(new SlotItemHandler(inventory, i++,107, 26));
                addSlot(new SlotItemHandler(inventory, i++, 53, 44));
                addSlot(new SlotItemHandler(inventory, i++, 71, 44));
                addSlot(new SlotItemHandler(inventory, i++, 89, 44));
                addSlot(new SlotItemHandler(inventory, i++,107, 44));
                return 84;
            case 9:
                addSlot(new SlotItemHandler(inventory, i++, 62, 17));
                addSlot(new SlotItemHandler(inventory, i++, 80, 17));
                addSlot(new SlotItemHandler(inventory, i++, 98, 17));
                addSlot(new SlotItemHandler(inventory, i++, 62, 35));
                addSlot(new SlotItemHandler(inventory, i++, 80, 35));
                addSlot(new SlotItemHandler(inventory, i++, 98, 35));
                addSlot(new SlotItemHandler(inventory, i++, 62, 53));
                addSlot(new SlotItemHandler(inventory, i++, 80, 53));
                addSlot(new SlotItemHandler(inventory, i++, 98, 53));
                return 84;
            case 12:
                addSlot(new SlotItemHandler(inventory, i++, 35, 26));
                addSlot(new SlotItemHandler(inventory, i++, 53, 26));
                addSlot(new SlotItemHandler(inventory, i++, 71, 26));
                addSlot(new SlotItemHandler(inventory, i++, 89, 26));
                addSlot(new SlotItemHandler(inventory, i++,107, 26));
                addSlot(new SlotItemHandler(inventory, i++,125, 26));
                addSlot(new SlotItemHandler(inventory, i++, 35, 44));
                addSlot(new SlotItemHandler(inventory, i++, 53, 44));
                addSlot(new SlotItemHandler(inventory, i++, 71, 44));
                addSlot(new SlotItemHandler(inventory, i++, 89, 44));
                addSlot(new SlotItemHandler(inventory, i++,107, 44));
                addSlot(new SlotItemHandler(inventory, i++,125, 44));
                return 84;
            case 14:
                addSlot(new SlotItemHandler(inventory, i++, 26, 26));
                addSlot(new SlotItemHandler(inventory, i++, 44, 26));
                addSlot(new SlotItemHandler(inventory, i++, 62, 26));
                addSlot(new SlotItemHandler(inventory, i++, 80, 26));
                addSlot(new SlotItemHandler(inventory, i++, 98, 26));
                addSlot(new SlotItemHandler(inventory, i++,116, 26));
                addSlot(new SlotItemHandler(inventory, i++,134, 26));
                addSlot(new SlotItemHandler(inventory, i++, 26, 44));
                addSlot(new SlotItemHandler(inventory, i++, 44, 44));
                addSlot(new SlotItemHandler(inventory, i++, 62, 44));
                addSlot(new SlotItemHandler(inventory, i++, 80, 44));
                addSlot(new SlotItemHandler(inventory, i++, 98, 44));
                addSlot(new SlotItemHandler(inventory, i++,116, 44));
                addSlot(new SlotItemHandler(inventory, i++,134, 44));
                return 84;
            case 15:
                addSlot(new SlotItemHandler(inventory, i++, 44, 17));
                addSlot(new SlotItemHandler(inventory, i++, 62, 17));
                addSlot(new SlotItemHandler(inventory, i++, 80, 17));
                addSlot(new SlotItemHandler(inventory, i++, 98, 17));
                addSlot(new SlotItemHandler(inventory, i++,116, 17));
                addSlot(new SlotItemHandler(inventory, i++, 44, 35));
                addSlot(new SlotItemHandler(inventory, i++, 62, 35));
                addSlot(new SlotItemHandler(inventory, i++, 80, 35));
                addSlot(new SlotItemHandler(inventory, i++, 98, 35));
                addSlot(new SlotItemHandler(inventory, i++,116, 35));
                addSlot(new SlotItemHandler(inventory, i++, 44, 53));
                addSlot(new SlotItemHandler(inventory, i++, 62, 53));
                addSlot(new SlotItemHandler(inventory, i++, 80, 53));
                addSlot(new SlotItemHandler(inventory, i++, 98, 53));
                addSlot(new SlotItemHandler(inventory, i++,116, 53));
                return 96;
            case 16:
                addSlot(new SlotItemHandler(inventory, i++, 53,  8));
                addSlot(new SlotItemHandler(inventory, i++, 71,  8));
                addSlot(new SlotItemHandler(inventory, i++, 89,  8));
                addSlot(new SlotItemHandler(inventory, i++,107,  8));
                addSlot(new SlotItemHandler(inventory, i++, 53, 26));
                addSlot(new SlotItemHandler(inventory, i++, 71, 26));
                addSlot(new SlotItemHandler(inventory, i++, 89, 26));
                addSlot(new SlotItemHandler(inventory, i++,107, 26));
                addSlot(new SlotItemHandler(inventory, i++, 53, 44));
                addSlot(new SlotItemHandler(inventory, i++, 71, 44));
                addSlot(new SlotItemHandler(inventory, i++, 89, 44));
                addSlot(new SlotItemHandler(inventory, i++,107, 44));
                addSlot(new SlotItemHandler(inventory, i++, 53, 62));
                addSlot(new SlotItemHandler(inventory, i++, 71, 62));
                addSlot(new SlotItemHandler(inventory, i++, 89, 62));
                addSlot(new SlotItemHandler(inventory, i++,107, 62));
                return 84;
            case 18:
                addSlot(new SlotItemHandler(inventory, i++,  8, 26));
                addSlot(new SlotItemHandler(inventory, i++, 26, 26));
                addSlot(new SlotItemHandler(inventory, i++, 44, 26));
                addSlot(new SlotItemHandler(inventory, i++, 62, 26));
                addSlot(new SlotItemHandler(inventory, i++, 80, 26));
                addSlot(new SlotItemHandler(inventory, i++, 98, 26));
                addSlot(new SlotItemHandler(inventory, i++,116, 26));
                addSlot(new SlotItemHandler(inventory, i++,134, 26));
                addSlot(new SlotItemHandler(inventory, i++,152, 26));
                addSlot(new SlotItemHandler(inventory, i++,  8, 44));
                addSlot(new SlotItemHandler(inventory, i++, 26, 44));
                addSlot(new SlotItemHandler(inventory, i++, 44, 44));
                addSlot(new SlotItemHandler(inventory, i++, 62, 44));
                addSlot(new SlotItemHandler(inventory, i++, 80, 44));
                addSlot(new SlotItemHandler(inventory, i++, 98, 44));
                addSlot(new SlotItemHandler(inventory, i++,116, 44));
                addSlot(new SlotItemHandler(inventory, i++,134, 44));
                addSlot(new SlotItemHandler(inventory, i++,152, 44));
                return 84;
            case 27:
                addSlot(new SlotItemHandler(inventory, i++,  8, 17));
                addSlot(new SlotItemHandler(inventory, i++, 26, 17));
                addSlot(new SlotItemHandler(inventory, i++, 44, 17));
                addSlot(new SlotItemHandler(inventory, i++, 62, 17));
                addSlot(new SlotItemHandler(inventory, i++, 80, 17));
                addSlot(new SlotItemHandler(inventory, i++, 98, 17));
                addSlot(new SlotItemHandler(inventory, i++,116, 17));
                addSlot(new SlotItemHandler(inventory, i++,134, 17));
                addSlot(new SlotItemHandler(inventory, i++,152, 17));
                addSlot(new SlotItemHandler(inventory, i++,  8, 35));
                addSlot(new SlotItemHandler(inventory, i++, 26, 35));
                addSlot(new SlotItemHandler(inventory, i++, 44, 35));
                addSlot(new SlotItemHandler(inventory, i++, 62, 35));
                addSlot(new SlotItemHandler(inventory, i++, 80, 35));
                addSlot(new SlotItemHandler(inventory, i++, 98, 35));
                addSlot(new SlotItemHandler(inventory, i++,116, 35));
                addSlot(new SlotItemHandler(inventory, i++,134, 35));
                addSlot(new SlotItemHandler(inventory, i++,152, 35));
                addSlot(new SlotItemHandler(inventory, i++,  8, 53));
                addSlot(new SlotItemHandler(inventory, i++, 26, 53));
                addSlot(new SlotItemHandler(inventory, i++, 44, 53));
                addSlot(new SlotItemHandler(inventory, i++, 62, 53));
                addSlot(new SlotItemHandler(inventory, i++, 80, 53));
                addSlot(new SlotItemHandler(inventory, i++, 98, 53));
                addSlot(new SlotItemHandler(inventory, i++,116, 53));
                addSlot(new SlotItemHandler(inventory, i++,134, 53));
                addSlot(new SlotItemHandler(inventory, i++,152, 53));
                return 84;
            case 36:
                addSlot(new SlotItemHandler(inventory, i++,  8,  8));
                addSlot(new SlotItemHandler(inventory, i++, 26,  8));
                addSlot(new SlotItemHandler(inventory, i++, 44,  8));
                addSlot(new SlotItemHandler(inventory, i++, 62,  8));
                addSlot(new SlotItemHandler(inventory, i++, 80,  8));
                addSlot(new SlotItemHandler(inventory, i++, 98,  8));
                addSlot(new SlotItemHandler(inventory, i++,116,  8));
                addSlot(new SlotItemHandler(inventory, i++,134,  8));
                addSlot(new SlotItemHandler(inventory, i++,152,  8));
                addSlot(new SlotItemHandler(inventory, i++,  8, 26));
                addSlot(new SlotItemHandler(inventory, i++, 26, 26));
                addSlot(new SlotItemHandler(inventory, i++, 44, 26));
                addSlot(new SlotItemHandler(inventory, i++, 62, 26));
                addSlot(new SlotItemHandler(inventory, i++, 80, 26));
                addSlot(new SlotItemHandler(inventory, i++, 98, 26));
                addSlot(new SlotItemHandler(inventory, i++,116, 26));
                addSlot(new SlotItemHandler(inventory, i++,134, 26));
                addSlot(new SlotItemHandler(inventory, i++,152, 26));
                addSlot(new SlotItemHandler(inventory, i++,  8, 44));
                addSlot(new SlotItemHandler(inventory, i++, 26, 44));
                addSlot(new SlotItemHandler(inventory, i++, 44, 44));
                addSlot(new SlotItemHandler(inventory, i++, 62, 44));
                addSlot(new SlotItemHandler(inventory, i++, 80, 44));
                addSlot(new SlotItemHandler(inventory, i++, 98, 44));
                addSlot(new SlotItemHandler(inventory, i++,116, 44));
                addSlot(new SlotItemHandler(inventory, i++,134, 44));
                addSlot(new SlotItemHandler(inventory, i++,152, 44));
                addSlot(new SlotItemHandler(inventory, i++,  8, 62));
                addSlot(new SlotItemHandler(inventory, i++, 26, 62));
                addSlot(new SlotItemHandler(inventory, i++, 44, 62));
                addSlot(new SlotItemHandler(inventory, i++, 62, 62));
                addSlot(new SlotItemHandler(inventory, i++, 80, 62));
                addSlot(new SlotItemHandler(inventory, i++, 98, 62));
                addSlot(new SlotItemHandler(inventory, i++,116, 62));
                addSlot(new SlotItemHandler(inventory, i++,134, 62));
                addSlot(new SlotItemHandler(inventory, i++,152, 62));
                return 84;
            case 54:
                // Original chests/54.png: first row at 18, player inventory at 140.
                for (int row = 0; row < 6; row++)
                    for (int col = 0; col < 9; col++)
                        addSlot(new SlotItemHandler(inventory, i++, 8 + col * 18, 18 + row * 18));
                return 140;
            default:
                // Fallback: auto-layout in rows of 9
                int cols = Math.min(slotCount, 9);
                for (int idx = 0; idx < slotCount; idx++) {
                    int row = idx / cols;
                    int col = idx % cols;
                    int x = 8 + col * 18;
                    int y = 17 + row * 18;
                    addSlot(new SlotItemHandler(inventory, i++, x, y));
                }
                int rows = (slotCount + cols - 1) / cols;
                return 17 + rows * 18 + 14;
        }
    }

    private void addPlayerInventory(Inventory playerInv, int yOffset) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, yOffset + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, yOffset + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < slotCount) {
            // From hopper to player
            if (!moveItemStackTo(stack, slotCount, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // From player to hopper
            if (!moveItemStackTo(stack, 0, slotCount, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory != null;
    }

    /** Override in subclasses for queuehopper permissions. */
    protected boolean canInsertIntoSlot(int slot) {
        return true;
    }

    protected boolean canExtractFromSlot(int slot) {
        return true;
    }
}
