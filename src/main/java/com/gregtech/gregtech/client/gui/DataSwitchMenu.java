package com.gregtech.gregtech.client.gui;

import com.gregtech.gregtech.block.inventory.UsbSwitchBlock;
import com.gregtech.gregtech.blockentity.inventory.UsbSwitchBlockEntity;
import com.gregtech.gregtech.content.data.UsbDataMedia;
import com.gregtech.gregtech.registry.GTMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/** Original switch layout: four-by-four USB rack or a single HDD, then the player inventory. */
public final class DataSwitchMenu extends AbstractContainerMenu {
    private final UsbSwitchBlockEntity machine;
    private final UsbSwitchBlock.Kind kind;
    private final int machineSlots;
    private final ContainerData mode;

    public DataSwitchMenu(int id, Inventory playerInventory, UsbSwitchBlockEntity machine) {
        super(machine.kind() == UsbSwitchBlock.Kind.USB
                ? GTMenuTypes.USB_SWITCH.get() : GTMenuTypes.HDD_SWITCH.get(), id);
        this.machine = machine;
        this.kind = machine.kind();
        this.machineSlots = machine.items().getSlots();
        this.mode = new ContainerData() {
            @Override public int get(int index) { return machine.mode(); }
            @Override public void set(int index, int value) { machine.setMode(value); }
            @Override public int getCount() { return 1; }
        };
        addDataSlots(mode);
        addSlots(machine.items(), playerInventory);
    }

    public DataSwitchMenu(int id, Inventory playerInventory, UsbSwitchBlock.Kind kind) {
        super(kind == UsbSwitchBlock.Kind.USB ? GTMenuTypes.USB_SWITCH.get() : GTMenuTypes.HDD_SWITCH.get(), id);
        this.machine = null;
        this.kind = kind;
        this.machineSlots = kind == UsbSwitchBlock.Kind.USB ? UsbSwitchBlockEntity.USB_SLOTS : UsbSwitchBlockEntity.HDD_SLOTS;
        this.mode = new SimpleContainerData(1);
        addDataSlots(mode);
        addSlots(new ItemStackHandler(machineSlots) {
            @Override public int getSlotLimit(int slot) { return 1; }
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return kind == UsbSwitchBlock.Kind.USB ? UsbDataMedia.isStick(stack) : UsbDataMedia.isDrive(stack);
            }
        }, playerInventory);
    }

    private void addSlots(IItemHandler handler, Inventory player) {
        for (int slot = 0; slot < machineSlots; slot++) {
            int x = kind == UsbSwitchBlock.Kind.USB ? 53 + slot % 4 * 18 : 80;
            int y = kind == UsbSwitchBlock.Kind.USB ? 8 + slot / 4 * 18 : 35;
            addSlot(new SlotItemHandler(handler, slot, x, y));
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(player, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 142));
    }

    public UsbSwitchBlock.Kind kind() { return kind; }
    public int selectedSlot() { return mode.get(0); }

    @Override public boolean stillValid(Player player) {
        return machine == null || !machine.isRemoved() && machine.getLevel() == player.level()
                && player.distanceToSqr(machine.getBlockPos().getCenter()) <= 64;
    }
    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem().copy();
        if (slotIndex < machineSlots) {
            if (!moveItemStackTo(slot.getItem(), machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(slot.getItem(), 0, machineSlots, false)) return ItemStack.EMPTY;
        if (slot.getItem().isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
