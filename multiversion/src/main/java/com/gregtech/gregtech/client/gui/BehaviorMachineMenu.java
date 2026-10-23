package com.gregtech.gregtech.client.gui;

import com.gregtech.gregtech.api.addon.BehaviorMachine;
import com.gregtech.gregtech.api.addon.platform.BehaviorMachineMenuView;
import com.gregtech.gregtech.blockentity.machine.BehaviorMachineBlockEntity;
import com.gregtech.gregtech.registry.GTMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.SlotItemHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.SlotItemHandler;
//?}

/** One menu for every behavior machine. Slot positions come from the declaration. */
public class BehaviorMachineMenu extends AbstractContainerMenu implements BehaviorMachineMenuView {
    private static final int PLAYER_SLOTS = 36;
    private final BehaviorMachineBlockEntity machine;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final int machineSlots;

    public BehaviorMachineMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, clientMachine(inventory, buffer));
    }

    public BehaviorMachineMenu(int id, Inventory inventory, BehaviorMachineBlockEntity machine) {
        super(GTMenuTypes.BEHAVIOR_MACHINE.get(), id);
        this.machine = machine;
        int values = 3 + machine.tanks() * 2;
        this.data = inventory.player.level().isClientSide ? new SimpleContainerData(values) : machine.menuData();
        this.access = ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos());
        addDataSlots(data);
        BehaviorMachine declaration = machine.declaration();
        for (int slot = 0; slot < declaration.items().size(); slot++) {
            var placed = declaration.items().get(slot);
            int index = slot;
            boolean output = placed.role() == BehaviorMachine.SlotRole.OUTPUT;
            addSlot(new SlotItemHandler(machine.inventory(), slot, placed.x(), placed.y()) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return !output && machine.behaviorAllows(index, stack);
                }
            });
        }
        machineSlots = declaration.items().size();
        int x = 8;
        int y = declaration.playerInventoryY();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, x + column * 18, y + row * 18));
        }
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, x + column * 18, y + 58));
    }

    private static BehaviorMachineBlockEntity clientMachine(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        if (entity instanceof BehaviorMachineBlockEntity machine) return machine;
        throw new IllegalStateException("No behavior machine at " + pos);
    }

    public BehaviorMachineBlockEntity machine() { return machine; }
    public int machineSlotCount() { return machineSlots; }
    @Override public int energy() { return data.get(0); }
    @Override public float progress() { return data.get(1) / 1000.0F; }
    @Override public boolean isOn(int toggle) { return (data.get(2) & (1 << toggle)) != 0; }
    @Override public int tankAmount(int tank) { return data.get(3 + tank); }
    @Override public int containerId() { return containerId; }

    @Override public FluidStack tankFluid(int tank) {
        int id = data.get(3 + machine.tanks() + tank);
        if (id < 0 || tankAmount(tank) <= 0) return FluidStack.EMPTY;
        var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(id);
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, tankAmount(tank));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return machine.flip(id);
    }

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
        return stillValid(access, player, machine.getBlockState().getBlock());
    }
}
