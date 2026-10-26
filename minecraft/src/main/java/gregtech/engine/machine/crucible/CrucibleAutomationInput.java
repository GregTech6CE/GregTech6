package gregtech.engine.machine.crucible;

import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.items.IItemHandler;
//?}

/** GT6 small crucible automation: one empty cache slot, insertion only. Expose on the top face. */
public final class CrucibleAutomationInput implements IItemHandler {
    private final IItemHandler cache;

    public CrucibleAutomationInput(IItemHandler cache) { this.cache = cache; }

    @Override public int getSlots() { return 1; }
    @Override public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? cache.getStackInSlot(0).copy() : ItemStack.EMPTY;
    }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return isItemValid(slot, stack) ? cache.insertItem(0, stack, simulate) : stack;
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
    @Override public int getSlotLimit(int slot) { return slot == 0 ? cache.getSlotLimit(0) : 0; }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 && !stack.isEmpty() && cache.getStackInSlot(0).isEmpty()
                && cache.isItemValid(0, stack);
    }
}
