package com.gregtech.gregtech.blockentity.inventory;

import com.gregtech.gregtech.client.gui.GarbageDumpContainerMenu;
import com.gregtech.gregtech.world.GarbageData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;

/**
 * GT6 Ender Garbage Dump: accesses the global {@link GarbageData} garbage piles.
 * Shows them in a chest-like GUI and continuously pushes retrieved trash into
 * the inventory/tank below (never into another bin).
 */
public class EnderGarbageDumpBlockEntity extends BlockEntity implements MenuProvider {

    /** Slots shown in the GUI (a double-chest page of the garbage piles). */
    public static final int VIEW_SLOTS = 54;

    public EnderGarbageDumpBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.ENDER_GARBAGE_DUMP.get(), pos, state);
    }

    /** Extract-only live view over the global garbage piles. */
    public final class GarbageView implements IItemHandlerModifiable {
        @Override public int getSlots() { return VIEW_SLOTS; }
        @Override public ItemStack getStackInSlot(int slot) {
            return level == null || level.isClientSide ? ItemStack.EMPTY : GarbageData.get(level).viewItem(slot);
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return level == null || level.isClientSide
                    ? ItemStack.EMPTY : GarbageData.get(level).extractItem(slot, amount, simulate);
        }
        @Override public void setStackInSlot(int slot, ItemStack stack) { /* read-only view */ }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    }

    private final GarbageView view = new GarbageView();

    public GarbageView view() { return view; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnderGarbageDumpBlockEntity dump) {
        if (level.getGameTime() % 10 != 0) return;
        BlockEntity below = level.getBlockEntity(pos.below());
        // never feed garbage back into a bin (it would loop forever)
        if (below == null || below instanceof EnderGarbageBlockEntity) return;
        below.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).ifPresent(target -> {
            GarbageData garbage = GarbageData.get(level);
            ItemStack taken = garbage.extractItem(0, 64, true);
            if (taken.isEmpty()) return;
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(target, taken.copy(), false);
            int moved = taken.getCount() - leftover.getCount();
            if (moved > 0) garbage.extractItem(0, moved, false);
        });
        below.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).ifPresent(tank -> {
            GarbageData garbage = GarbageData.get(level);
            FluidStack available = garbage.drain(16000, true);
            if (available.isEmpty()) return;
            int filled = tank.fill(available, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) garbage.drain(filled, false);
        });
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.gregtech.ender_garbage_dump");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new GarbageDumpContainerMenu(containerId, playerInv, view);
    }
}
