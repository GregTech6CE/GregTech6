package gregtech.engine.machine;

import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.items.ItemStackHandler;
//?}
import org.jetbrains.annotations.NotNull;

/** Slot layout for a basic machine: input slots first, output slots after them. */
public class MachineItemHandler extends ItemStackHandler {
    private final int inputCount;
    private final Runnable onChanged;

    public MachineItemHandler(int totalSlots, int inputCount, Runnable onChanged) {
        super(totalSlots);
        this.inputCount = inputCount;
        this.onChanged = onChanged;
    }

    public int inputCount() { return inputCount; }

    public Iterable<ItemStack> stacks() {
        return () -> new java.util.Iterator<>() {
            int idx;
            @Override public boolean hasNext() { return idx < getSlots(); }
            @Override public ItemStack next() { return getStackInSlot(idx++); }
        };
    }

    /** Insert into output slots — bypasses the player-facing restriction. */
    public void insertOutput(ItemStack stack) {
        if (stack.isEmpty()) return;
        int outIdx = inputCount;
        // Merge with existing stacks first
        for (int i = outIdx; i < getSlots(); i++) {
            ItemStack existing = getStackInSlot(i);

//? if >=1.21.1 {
/*            if (ItemStack.isSameItemSameComponents(existing, stack)) {
*///?} else {
            if (ItemStack.isSameItemSameTags(existing, stack)) {
//?}

                int space = Math.min(getSlotLimit(i), existing.getMaxStackSize()) - existing.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, stack.getCount());
                    existing.grow(toAdd);
                    stack.shrink(toAdd);
                    onContentsChanged(i);
                    if (stack.isEmpty()) return;
                }
            }
        }
        // Place in empty slot
        for (int i = outIdx; i < getSlots(); i++) {
            if (getStackInSlot(i).isEmpty()) {
                setStackInSlot(i, stack);
                onContentsChanged(i);
                return;
            }
        }
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (slot >= inputCount) return stack;
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChanged.run();
    }
}
