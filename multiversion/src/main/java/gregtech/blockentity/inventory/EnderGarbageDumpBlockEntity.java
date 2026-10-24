package gregtech.blockentity.inventory;

import gregtech.client.gui.GarbageDumpContainerMenu;
import gregtech.world.GarbageData;
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
//? if >=1.21.1 {

/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
*///?} else {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
//?}

import javax.annotation.Nullable;

/**
 * GT6 Ender Garbage Dump: accesses the global {@link GarbageData} garbage piles.
 * Shows them in a chest-like GUI and continuously pushes retrieved trash into
 * the inventory/tank below (never into another bin).
 */
public class EnderGarbageDumpBlockEntity extends BlockEntity implements MenuProvider {

    /** Slots shown in the GUI (a double-chest page of the garbage piles). */
    public static final int VIEW_SLOTS = gregtech.content.storage.GarbageStorageRules.VIEW_SLOTS;

    public EnderGarbageDumpBlockEntity(BlockPos pos, BlockState state) {
        super(gregtech.registry.GTBlockEntities.ENDER_GARBAGE_DUMP.get(), pos, state);
    }

    /** Extract-only live view over the global garbage piles. */
    public final class GarbageView implements IItemHandlerModifiable {

        @Override
        public int getSlots() {
            return VIEW_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return level == null || level.isClientSide ? ItemStack.EMPTY : GarbageData.get(level).viewItem(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return level == null || level.isClientSide
                    ? ItemStack.EMPTY : GarbageData.get(level).extractItem(slot, amount, simulate);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            /* read-only view */
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

    private final GarbageView view = new GarbageView();

    public GarbageView view() {
        return view;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnderGarbageDumpBlockEntity dump) {
        if (!gregtech.content.storage.GarbageStorageRules.dumpDue(level.getGameTime())) return;
        BlockEntity below = level.getBlockEntity(pos.below());
        // never feed garbage back into a bin (it would loop forever)
        if (below == null || below instanceof EnderGarbageBlockEntity) return;
//? if >=1.21.1 {
        /*var itemTarget = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP);
        if (itemTarget != null) {
*///?} else {
        below.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).ifPresent(itemTarget -> {
//?}
            GarbageData garbage = GarbageData.get(level);
            ItemStack taken = garbage.extractItem(0, 64, true);
//? if >=1.21.1 {
            /*if (!taken.isEmpty()) {
*///?} else {
            if (taken.isEmpty()) return;
//?}
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(itemTarget, taken.copy(), false);
            int moved = taken.getCount() - leftover.getCount();
            if (moved > 0) garbage.extractItem(0, moved, false);
//? if >=1.21.1 {
            /*}
        }
        var fluidTarget = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos.below(), Direction.UP);
        if (fluidTarget != null) {
*///?} else {
        });
        below.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).ifPresent(fluidTarget -> {
//?}
            GarbageData garbage = GarbageData.get(level);
            FluidStack available = garbage.drain(16000, true);
//? if >=1.21.1 {
            /*if (!available.isEmpty()) {
*///?} else {
            if (available.isEmpty()) return;
//?}
            int filled = fluidTarget.fill(available, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) garbage.drain(filled, false);
//? if >=1.21.1 {
            /*}
        }
*///?} else {
        });
//?}
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.gt6ce.ender_garbage_dump");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new GarbageDumpContainerMenu(containerId, playerInv, view) {
            @Override
            public boolean stillValid(Player player) {
                return !isRemoved() && level != null && player.level() == level
                        && level.getBlockEntity(worldPosition) == EnderGarbageDumpBlockEntity.this
                        && player.distanceToSqr(worldPosition.getX() + .5,
                                worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64;
            }
        };
    }
}
