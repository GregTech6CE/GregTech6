package com.gregtech.gregtech.client.gui;

import com.gregtech.gregtech.api.addon.LayoutPart;
import com.gregtech.gregtech.blockentity.machine.FunctionalPartRuntime;
import com.gregtech.gregtech.registry.GTMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.SlotItemHandler;
*///?} else {
import net.minecraftforge.items.SlotItemHandler;
//?}

/** The slots of one functional part, plus the player inventory. Tanks and fields are drawn by the screen. */
public class PartMenu extends AbstractContainerMenu {
    private static final int PLAYER_SLOTS = 36;
    private final FunctionalPartRuntime runtime;
    private final ContainerLevelAccess access;
    private final int machineSlots;
    private final PartGuiLayout layout;

    public PartMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, clientHost(inventory, buffer));
    }

    public PartMenu(int id, Inventory inventory, FunctionalPartRuntime.Host host) {
        super(GTMenuTypes.PART.get(), id);
        this.runtime = host.function();
        LayoutPart part = runtime.declaration();
        this.layout = PartGuiLayout.of(part);
        this.access = ContainerLevelAccess.create(host.level(), host.pos());
        for (int slot = 0; slot < runtime.slotCount(); slot++)
            addSlot(new SlotItemHandler(runtime.inventory(), slot, layout.slotX[slot], layout.slotY[slot]));
        machineSlots = runtime.slotCount();
        int x = 8;
        int y = layout.playerY;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, x + column * 18, y + row * 18));
        }
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, x + column * 18, y + 58));
    }

    private static FunctionalPartRuntime.Host clientHost(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        if (entity instanceof FunctionalPartRuntime.Host host && host.function() != null) return host;
        throw new IllegalStateException("No functional part at " + pos);
    }

    public FunctionalPartRuntime runtime() { return runtime; }

    public PartGuiLayout layout() { return layout; }

    public int machineSlotCount() { return machineSlots; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < machineSlots
                ? moveItemStackTo(stack, machineSlots, machineSlots + PLAYER_SLOTS, true)
                : moveItemStackTo(stack, 0, machineSlots, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, player.level().getBlockState(runtime.context().pos()).getBlock());
    }
}
