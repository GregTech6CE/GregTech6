package gregtech.client.gui;

import gregtech.content.blockentity.inventory.EnderGarbageDumpBlockEntity;
import gregtech.content.registry.GTMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
*///?} else {
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
//?}

/**
 * GT6 Ender Garbage Dump GUI: a 54-slot extract-only window over the global
 * garbage piles. Shift-click extracts through the handler because display stacks are copies.
 * Hotbar/offhand swaps also extract explicitly: the live view has no writable setter.
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
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.SWAP && slotId >= 0 && slotId < slotCount()) {
            // Vanilla's empty-hotbar branch transfers getItem() then calls set(EMPTY).
            // That would copy a display stack without deducting this extract-only pool.
            if ((button >= 0 && button < 9 || button == 40)
                    && player.getInventory().getItem(button).isEmpty()) {
                Slot slot = slots.get(slotId);
                if (slot.hasItem() && slot.mayPickup(player)) {
                    ItemStack taken = slot.remove(slot.getItem().getCount());
                    if (!taken.isEmpty()) {
                        player.getInventory().setItem(button, taken);
                        slot.onTake(player, taken);
                    }
                }
            }
            // Occupied targets cannot swap into an extract-only pool.
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slotCount()) return ItemStack.EMPTY; // no inserting trash here
        Slot slot = slots.get(index);
        if (!(slot instanceof SlotItemHandler handlerSlot) || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack taken = handlerSlot.getItemHandler().extractItem(index, 64, false);
        if (taken.isEmpty()) return ItemStack.EMPTY;
        // Inventory.add can report partial success, and creative inventories may void
        // its remainder. Native menu transfer instead leaves every unplaced item here.
        moveItemStackTo(taken, slotCount(), slots.size(), true);
        if (!taken.isEmpty() && !player.level().isClientSide) {
            // Full or partially full inventory: restore every unplaced item.
            gregtech.content.world.GarbageData.get(player.level()).trash(taken);
        }
        broadcastChanges();
        return ItemStack.EMPTY;
    }
}
