package com.gregtech.gregtech.client.gui;

import com.gregtech.gregtech.blockentity.tool.BumbliaryBlockEntity;
import com.gregtech.gregtech.content.bumble.BumbleBeeType;
import com.gregtech.gregtech.registry.GTMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * GT6's bumbliary container ({@code MultiTileEntityBumbliary.MultiTileEntityGUICommonBumbliary:389-441},
 * the advanced one {@code MultiTileEntityBumbliaryAdvanced:390-426}): the machine's slots in GT6's own
 * grid plus the player inventory at GT6's offset.
 *
 * <p>GT6's {@code ContainerCommon.addSlots} returns 84 for both machines ({@code ContainerCommon:289}),
 * so the player rows start at y=84, the hotbar at y=142 and the GUI texture is 176x166 - which is
 * exactly the sheet GT6 ships ({@code textures/gui/machines/Bumbliary.png}, the advanced machine
 * {@code BumbliaryAdvanced.png}). The machine slots sit on GT6's 18-pixel grid: 9 columns from x=8 for
 * the standard machine and 5 columns from x=44 for the advanced one, rows at y=8, 26, 44 and 62
 * ({@code MultiTileEntityBumbliary:396-434}).</p>
 *
 * <h2>Slot rules ({@code :357-369, :395-434})</h2>
 * <p>GT6 builds its GUI from {@code Slot_Normal} with two flags, so the port does the same. The whole
 * table of the standard machine ({@code MultiTileEntityBumbliary:396-434}, the advanced one
 * {@code MultiTileEntityBumbliaryAdvanced:397-419} has the same shape with its own indices):</p>
 * <ul>
 *   <li>{@code setCanPut(F)} on the comb, spare-drone and dead slots: only the player's own two bee
 *       slots take items ({@code isItemValidForSlotGUI:357-364}).</li>
 *   <li>{@code setCanTake(F)} on the main drone slot ({@code :420}) and on the spare drone slots
 *       ({@code :399-421}), so a survival player cannot pull those out, a creative one can
 *       ({@code Slot_Base.canTakeStack:92}). The comb slots and the dead slots keep GT6's default and
 *       stay open to a survival player.</li>
 *   <li>the royal slot is one item deep ({@code getInventoryStackLimitGUI:341}) and its contents only
 *       leave while no live queen sits there ({@code canTakeOutOfSlotGUI:367-369}). GT6 asks the
 *       machine first and then its own flag; the standard GUI marks the royal slot
 *       {@code setCanTake(F)} ({@code :410}) while GT6's scoop GUI leaves that slot unrestricted
 *       ({@code :464}) so a player can take the bees back out. The synced scoop mode
 *       unlocks all take flags, while a living queen remains protected by the inventory rule.</li>
 * </ul>
 *
 * <p>Port difference: GT6's validity check asks the tile entity ({@code isItemValidForSlotGUI});
 * the port answers from the stack itself ({@link BumbleBeeType}), which is the same test GT6 runs -
 * and it makes the client's copy of the menu agree with the server's without a synced flag. The royal
 * slot's take rule works the same way: the server asks the machine
 * ({@link BumbliaryBlockEntity#canTakeOutOfSlot(int)}), the client asks the synced stack
 * ({@link BumbliaryBlockEntity#canLeaveRoyalSlot(ItemStack)}), and both answer GT6's
 * {@code canTakeOutOfSlotGUI}.</p>
 */
public class BumbliaryContainerMenu extends AbstractContainerMenu {

    /** GT6's {@code ContainerCommon.addSlots} offset on both machines ({@code ContainerCommon:289}). */
    public static final int PLAYER_INVENTORY_Y = 84;
    /** GT6's GUI sheet is 176x166 ({@code GuiContainer} defaults, {@code ContainerClient:69-73}). */
    public static final int IMAGE_WIDTH = 176, IMAGE_HEIGHT = 166;

    private final BumbliaryBlockEntity machine;
    private final BumbliaryBlockEntity.Layout layout;
    private final int machineSlots;
    private boolean scoopMode;

    private void syncScoopMode() {
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override public int get() { return scoopMode ? 1 : 0; }
            @Override public void set(int value) { scoopMode = value != 0; }
        });
    }
    public boolean scoopMode() { return scoopMode; }

    /** Server-side constructor: the machine owns the slots. */
    public BumbliaryContainerMenu(int containerId, Inventory playerInventory, BumbliaryBlockEntity machine) {
        this(containerId, playerInventory, machine, false);
    }

    public BumbliaryContainerMenu(int containerId, Inventory playerInventory, BumbliaryBlockEntity machine,
                                  boolean scoopMode) {
        super(machine.layout().advanced() ? GTMenuTypes.ADVANCED_BUMBLIARY.get() : GTMenuTypes.BUMBLIARY.get(),
                containerId);
        this.machine = machine;
        this.scoopMode = scoopMode;
        this.layout = machine.layout();
        this.machineSlots = layout.slots();
        addMachineSlots(machine.inventory(), machine);
        addPlayerSlots(playerInventory);
        syncScoopMode();
    }

    /** Client-side constructor (Forge menu factory): the slots mirror the machine, the items arrive by sync. */
    public BumbliaryContainerMenu(int containerId, Inventory playerInventory, boolean advanced) {
        super(advanced ? GTMenuTypes.ADVANCED_BUMBLIARY.get() : GTMenuTypes.BUMBLIARY.get(), containerId);
        this.machine = null;
        this.layout = advanced ? BumbliaryBlockEntity.ADVANCED_LAYOUT : BumbliaryBlockEntity.LAYOUT;
        this.machineSlots = layout.slots();
        addMachineSlots(new ItemStackHandler(machineSlots), null);
        addPlayerSlots(playerInventory);
        syncScoopMode();
    }

    /** Whether this menu drives GT6's advanced bumbliary, which picks the GUI texture. */
    public boolean advanced() { return layout.advanced(); }

    /** The machine behind this menu, or null on the client. */
    public BumbliaryBlockEntity machine() { return machine; }

    private void addMachineSlots(IItemHandler handler, BumbliaryBlockEntity machine) {
        int columns = layout.advanced() ? 5 : 9;
        int firstX = layout.advanced() ? 44 : 8;
        for (int index = 0; index < machineSlots; index++) {
            int column = index % columns, row = index / columns;
            addSlot(new BumbliarySlot(handler, index, layout, machine, () -> scoopMode, firstX + column * 18, 8 + row * 18));
        }
    }

    private void addPlayerSlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18,
                        PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, PLAYER_INVENTORY_Y + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;   // GT6 canTakeStack:92
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (index < machineSlots) {
            // GT6 transferStackInSlot:498-522, machine -> player.
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, machineSlots, false)) {
            // Player -> machine: only the princess and the drone slot accept anything (:360-361).
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine == null || !machine.isRemoved();
    }

    /** GT6's {@code Slot_Normal} with its flags ({@code :395-434}). */
    private static final class BumbliarySlot extends SlotItemHandler {
        private final boolean canPut, canTake, royal, drone;
        private final java.util.function.BooleanSupplier scoopMode;
        /** The machine this slot belongs to, or null in the client copy of the menu. */
        private final BumbliaryBlockEntity machine;

        BumbliarySlot(IItemHandler handler, int index, BumbliaryBlockEntity.Layout layout,
                      BumbliaryBlockEntity machine, java.util.function.BooleanSupplier scoopMode, int x, int y) {
            super(handler, index, x, y);
            this.scoopMode = scoopMode;
            this.royal = index == layout.royal();
            this.drone = index == layout.drone();
            this.canPut = royal || drone;
            // GT6 marks both bee slots and every spare drone slot with setCanTake(F) (:410, :420 and the
            // spare ones :399-421), so a survival player empties the comb slots and the dead slots only -
            // plus the royal slot, which the machine's canTakeOutOfSlotGUI decides (:367-369).
            this.canTake = !royal && index != layout.drone() && !contains(layout.drones(), index);
            this.machine = machine;
        }

        private static boolean contains(int[] slots, int index) {
            for (int slot : slots) if (slot == index) return true;
            return false;
        }

        /** GT6's {@code isItemValidForSlotGUI:357-364}: a princess or a drone, nothing else. */
        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!canPut || stack.isEmpty()) return false;
            BumbleBeeType type = BumbleBeeType.of(stack);
            if (type == null) return false;
            if (royal) return type.aliveVariant() == BumbleBeeType.PRINCESS;
            if (drone) return type.aliveVariant() == BumbleBeeType.DRONE;
            return false;
        }

        /**
         * GT6's {@code canTakeStack:92}: {@code canTakeOutOfSlotGUI} decides about the royal slot (only
         * a live queen is locked in, {@code :367-369}), a slot with {@code setCanTake(F)} otherwise only
         * opens for a creative player.
         *
         * <p>The server asks the machine; the client copy of the menu has none, so it derives the same
         * answer from the synced stack - GT6's own rule only looks at the slot's contents.</p>
         */
        @Override
        public boolean mayPickup(Player player) {
            boolean inventoryAllows = !royal || (machine != null
                    ? machine.canTakeOutOfSlot(getSlotIndex())
                    : BumbliaryBlockEntity.canLeaveRoyalSlot(getItem()));
            return inventoryAllows && (player.isCreative() || scoopMode.getAsBoolean() || canTake);
        }

        /** GT6's {@code getInventoryStackLimitGUI:341}: the royal slot holds one bee. */
        @Override
        public int getMaxStackSize() {
            return royal ? 1 : 64;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(getMaxStackSize(), stack.getMaxStackSize());
        }
    }
}
