package gregtech.content.blockentity.machine;

import gregtech.api.addon.machine.BehaviorMachine;
import gregtech.api.addon.platform.MachineInventoryAccess;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.items.ItemStackHandler;
//?}

import java.util.List;

final class BehaviorMachineItems implements MachineInventoryAccess {
    private final BehaviorMachine declaration;
    private final ItemStackHandler items;

    BehaviorMachineItems(BehaviorMachine declaration, ItemStackHandler items) {
        this.declaration = declaration;
        this.items = items;
    }

    @Override
    public int slots() { return items.getSlots(); }

    @Override
    public ItemStack snapshot(int slot) { return items.getStackInSlot(slot).copy(); }

    @Override
    public void set(int slot, ItemStack stack) {
        items.setStackInSlot(slot, stack == null ? ItemStack.EMPTY : stack.copy());
    }

    @Override
    public ItemStack insertOutput(ItemStack stack) {
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

    boolean dropsFit(List<ItemStack> drops) {
        ItemStack[] before = new ItemStack[items.getSlots()];
        for (int i = 0; i < before.length; i++) before[i] = snapshot(i);
        boolean ok = true;
        for (ItemStack drop : drops) {
            if (drop == null || drop.isEmpty()) continue;
            if (!insertOutput(drop).isEmpty()) {
                ok = false;
                break;
            }
        }
        for (int i = 0; i < before.length; i++) items.setStackInSlot(i, before[i]);
        return ok;
    }
}
