package gregtech.client.gui;

import gregtech.content.registry.GTMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
*///?} else {
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
//?}

/**
 * Shared hopper and queue menu using the current {@code InventorySlotLayout} coordinates.
 * Original GT6 menu behavior is audited separately from this shared layout.
 */
public class HopperContainerMenu extends AbstractContainerMenu {
    private final IItemHandler inventory;
    private final int slotCount;
    private final int playerInvY;

    /** Server constructor: uses real BE inventory and the correct MenuType for this slot count. */
    public HopperContainerMenu(MenuType<?> type, int containerId, Inventory playerInv,
                               IItemHandler inventory) {
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

    public int slotCount() {
        return slotCount;
    }

    public int playerInvY() {
        return playerInvY;
    }

    private int addHopperSlots() {
        var layout = gregtech.engine.inventory.InventorySlotLayout.layout(slotCount);
        int index = 0;
        for (var position : layout.positions())
            addSlot(createInventorySlot(inventory, index++, position.x(), position.y()));
        return layout.playerInventoryY();
    }

    /** Called during construction: overrides must use these parameters, not subclass fields. */
    protected Slot createInventorySlot(IItemHandler handler, int index, int x, int y) {
        return new InventorySlot(handler, index, x, y);
    }

    /** Server providers forward live-stack changes to their normal inventory notification path. */
    protected void inventorySlotChanged(int index) {}

    protected class InventorySlot extends SlotItemHandler {
        private final int inventoryIndex;

        protected InventorySlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
            inventoryIndex = index;
        }

        @Override public void setChanged() {
            super.setChanged();
            // Native merges can mutate the handler's live stack without calling its setter.
            HopperContainerMenu.this.inventorySlotChanged(inventoryIndex);
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

    /** Subclass hooks; this base menu's slot factory does not consult them. */
    protected boolean canInsertIntoSlot(int slot) {
        return true;
    }

    protected boolean canExtractFromSlot(int slot) {
        return true;
    }
}
