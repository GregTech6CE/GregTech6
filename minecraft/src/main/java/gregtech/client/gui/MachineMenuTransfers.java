package gregtech.client.gui;

import java.util.List;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Forward player-to-machine transfer for the behavior and bumbliary menus only. */
final class MachineMenuTransfers {
    private MachineMenuTransfers() {}

    static boolean moveToMachine(ItemStack stack, List<Slot> slots, int machineSlots) {
        boolean moved = false;
        // Keep native merge-before-empty order, but apply the slot's admission rule to both passes.
        if (stack.isStackable()) for (int i = 0; i < machineSlots && !stack.isEmpty(); i++) {
            Slot slot = slots.get(i);
            ItemStack existing = slot.getItem();
            if (existing.isEmpty() || !slot.mayPlace(stack)) continue;
//? if >=1.21.1 {
/*            if (!ItemStack.isSameItemSameComponents(existing, stack)) continue;
*///?} else {
            if (!ItemStack.isSameItemSameTags(existing, stack)) continue;
//?}
            // These menus' limits are pure; do not invoke Forge's clear/simulate/restore probe.
            int limit = Math.min(slot.getMaxStackSize(), stack.getMaxStackSize());
            int count = Math.min(stack.getCount(), limit - existing.getCount());
            if (count <= 0) continue;
            stack.shrink(count);
            existing.grow(count);
            slot.setChanged();
            moved = true;
        }
        if (!stack.isEmpty()) for (int i = 0; i < machineSlots; i++) {
            Slot slot = slots.get(i);
            if (slot.hasItem() || !slot.mayPlace(stack)) continue;
            int count = Math.min(stack.getCount(), Math.min(slot.getMaxStackSize(), stack.getMaxStackSize()));
            if (count <= 0) continue;
            slot.setByPlayer(stack.split(count));
            slot.setChanged();
            moved = true;
            break; // As in native moveItemStackTo, fill at most one empty slot per invocation.
        }
        return moved;
    }
}
