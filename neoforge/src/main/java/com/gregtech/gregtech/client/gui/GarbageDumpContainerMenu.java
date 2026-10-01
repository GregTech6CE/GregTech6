package com.gregtech.gregtech.client.gui;

import com.gregtech.gregtech.blockentity.inventory.EnderGarbageDumpBlockEntity;
import com.gregtech.gregtech.registry.GTMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * GT6 Ender Garbage Dump GUI: a 54-slot extract-only window over the global
 * garbage piles. Pile counts can exceed a stack, so all removals go through
 * the handler instead of mutating the displayed stacks.
 */
public class GarbageDumpContainerMenu extends HopperContainerMenu {

    /** Server: live view over the global garbage. */
    public GarbageDumpContainerMenu(int containerId, Inventory playerInv, IItemHandler garbageView) {
        super(GTMenuTypes.GARBAGE_DUMP.get(), containerId, playerInv, garbageView);
    }

    /** Client: dummy inventory mirrored from the server. */
    public GarbageDumpContainerMenu(int containerId, Inventory playerInv) {
        super(GTMenuTypes.GARBAGE_DUMP.get(), containerId, playerInv,
                EnderGarbageDumpBlockEntity.VIEW_SLOTS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= slotCount()) return ItemStack.EMPTY; // no inserting trash here
        Slot slot = slots.get(index);
        if (!(slot instanceof SlotItemHandler handlerSlot) || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack taken = handlerSlot.getItemHandler().extractItem(index, 64, false);
        if (taken.isEmpty()) return ItemStack.EMPTY;
        if (!player.getInventory().add(taken) && !taken.isEmpty() && !player.level().isClientSide) {
            // inventory full: put the rest back on the garbage pile
            com.gregtech.gregtech.world.GarbageData.get(player.level()).trash(taken);
        }
        broadcastChanges();
        return ItemStack.EMPTY;
    }
}
