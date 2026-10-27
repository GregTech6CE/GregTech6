package gregtech.client.gui;

import gregtech.content.registry.GTMenuTypes;
import gregtech.content.blockentity.inventory.MetalChestBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** Plain chest storage: real vanilla Container/Slot contracts on both sides. */
public class MetalChestMenu extends HopperContainerMenu {
    private final Container chest;

    public MetalChestMenu(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(54));
    }

    public MetalChestMenu(int containerId, Inventory playerInv, MetalChestBlockEntity chest) {
        this(containerId, playerInv, new ChestInventory(chest));
    }

    private MetalChestMenu(int containerId, Inventory playerInv, Container chest) {
        super(GTMenuTypes.METAL_CHEST.get(), containerId, playerInv, chest);
        checkContainerSize(chest, 54);
        this.chest = chest;
    }

    @Override
    public boolean stillValid(Player player) {
        return chest.stillValid(player);
    }

    /** Menu-only view: do not expose the BE as Container to world automation. */
    private static final class ChestInventory implements Container {
        private final MetalChestBlockEntity owner;

        private ChestInventory(MetalChestBlockEntity owner) {
            this.owner = owner;
        }

        @Override public int getContainerSize() { return owner.inventory().getSlots(); }
        @Override public boolean isEmpty() {
            for (int slot = 0; slot < getContainerSize(); slot++)
                if (!getItem(slot).isEmpty()) return false;
            return true;
        }
        @Override public ItemStack getItem(int slot) {
            return owner.inventory().getStackInSlot(slot);
        }
        @Override public ItemStack removeItem(int slot, int amount) {
            return owner.inventory().extractItem(slot, amount, false);
        }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = getItem(slot);
            owner.inventory().setStackInSlot(slot, ItemStack.EMPTY);
            return stack;
        }
        @Override public void setItem(int slot, ItemStack stack) {
            int limit = Math.min(owner.inventory().getSlotLimit(slot), stack.getMaxStackSize());
            if (stack.getCount() > limit) stack = stack.copyWithCount(limit);
            owner.inventory().setStackInSlot(slot, stack);
        }
        @Override public void setChanged() { owner.setChanged(); }
        @Override public boolean stillValid(Player player) { return owner.canPlayerUse(player); }
        @Override public void clearContent() {
            for (int slot = 0; slot < getContainerSize(); slot++)
                owner.inventory().setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

}
