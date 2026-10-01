package com.gregtech.gregtech.client.gui;

import com.gregtech.gregtech.registry.GTMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

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

    private int addHopperSlots(){
        var layout=com.gregtech.gregtech.api.inventory.InventorySlotLayout.layout(slotCount);int index=0;
        for(var position:layout.positions())addSlot(new SlotItemHandler(inventory,index++,position.x(),position.y()));
        return layout.playerInventoryY();
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
