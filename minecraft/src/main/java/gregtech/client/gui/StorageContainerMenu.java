package gregtech.client.gui;

import gregtech.content.registry.GTMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.IItemHandlerModifiable;
*///?} else {
import net.minecraftforge.items.IItemHandlerModifiable;
//?}

/** Ordinary storage pages expose real Container addresses to generic inventory interactions. */
public class StorageContainerMenu extends HopperContainerMenu {
    public StorageContainerMenu(int id, Inventory player, int size) {
        this(id, player, new SimpleContainer(size));
    }

    public StorageContainerMenu(int id, Inventory player, Container storage) {
        super(GTMenuTypes.storageForSlotCount(storage.getContainerSize()), id, player, storage);
    }

    public StorageContainerMenu(int id, Inventory player, IItemHandlerModifiable storage, Runnable changed) {
        this(id, player, new StorageInventory(storage, changed));
    }

    @Override
    protected Slot createStorageSlot(Container storage, int index, int x, int y) {
        return new Slot(storage, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return storage.canPlaceItem(index, stack); }
            @Override public int getMaxStackSize() {
                return storage instanceof StorageInventory view ? view.handler.getSlotLimit(index) : storage.getMaxStackSize();
            }
            @Override public int getMaxStackSize(ItemStack stack) {
                return Math.min(getMaxStackSize(), stack.getMaxStackSize());
            }
        };
    }

    /** This view belongs only to the menu: the world BE must not become a vanilla hopper target. */
    private static final class StorageInventory implements Container {
        private final IItemHandlerModifiable handler;
        private final Runnable changed;

        private StorageInventory(IItemHandlerModifiable handler, Runnable changed) {
            this.handler = handler;
            this.changed = changed;
        }

        @Override public int getContainerSize() { return handler.getSlots(); }
        @Override public boolean isEmpty() {
            for (int slot = 0; slot < getContainerSize(); slot++) if (!getItem(slot).isEmpty()) return false;
            return true;
        }
        @Override public ItemStack getItem(int slot) { return handler.getStackInSlot(slot); }
        @Override public ItemStack removeItem(int slot, int count) { return handler.extractItem(slot, count, false); }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = getItem(slot);
            handler.setStackInSlot(slot, ItemStack.EMPTY);
            return stack;
        }
        @Override public void setItem(int slot, ItemStack stack) {
            int limit = Math.min(handler.getSlotLimit(slot), stack.getMaxStackSize());
            handler.setStackInSlot(slot, stack.getCount() > limit ? stack.copyWithCount(limit) : stack);
        }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return handler.isItemValid(slot, stack); }
        @Override public void setChanged() { changed.run(); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() {
            for (int slot = 0; slot < getContainerSize(); slot++) handler.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }
}
