package com.gregtech.gregtech.blockentity.tool;

import com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput;
import com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack;
import com.gregtech.gregtech.blockentity.machine.SmeltingCrucibleBlockEntity;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.MaterialItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * GT6 Dust Funnel: buffers dusts and trickles them into the smelting crucible
 * below (dusts thrown into a crucible by hand would normally puff away).
 */
public class DustFunnelBlockEntity extends BlockEntity {

    private final ItemStackHandler buffer = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) { return isDust(stack); }
    };

    public DustFunnelBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.DUST_FUNNEL.get(), pos, state);
    }

    public static boolean isDust(ItemStack stack) {
        if (!(stack.getItem() instanceof MaterialItem mat)) return false;
        MaterialPrefix prefix = mat.getPrefix();
        return (prefix == MaterialPrefix.dust || prefix == MaterialPrefix.dustSmall
                || prefix == MaterialPrefix.dustTiny || prefix == MaterialPrefix.dustDiv72)
                && CrucibleItemInput.isValid(stack);
    }

    /** @return how many items were taken from the held stack */
    public int insert(ItemStack held) {
        if (!isDust(held)) return 0;
        ItemStack leftover = buffer.insertItem(0, held.copy(), false);
        return held.getCount() - leftover.getCount();
    }

    public ItemStack retrieve() {
        ItemStack out = buffer.getStackInSlot(0);
        buffer.setStackInSlot(0, ItemStack.EMPTY);
        return out;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DustFunnelBlockEntity funnel) {
        if (level.getGameTime() % 5 != 0) return;
        ItemStack stack = funnel.buffer.getStackInSlot(0);
        if (stack.isEmpty()) return;
        if (!(level.getBlockEntity(pos.below()) instanceof SmeltingCrucibleBlockEntity crucible)) return;
        List<CrucibleMaterialStack> incoming = CrucibleItemInput.parse(stack);
        if (incoming.isEmpty()) return;
        if (crucible.addMaterialStacks(incoming, crucible.getTemperature())) {
            stack.shrink(1);
            funnel.buffer.setStackInSlot(0, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
    }

    public void dropContents() {
        if (level == null) return;
        ItemStack stack = buffer.getStackInSlot(0);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack);
            buffer.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    private LazyOptional<IItemHandler> cap = LazyOptional.of(() -> buffer);

    @Override
    public <T> @org.jetbrains.annotations.NotNull LazyOptional<T> getCapability(
            @org.jetbrains.annotations.NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return cap.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        cap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        cap = LazyOptional.of(() -> buffer);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Buffer", buffer.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Buffer")) buffer.deserializeNBT(tag.getCompound("Buffer"));
    }
}
