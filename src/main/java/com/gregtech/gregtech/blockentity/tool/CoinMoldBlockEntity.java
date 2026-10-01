package com.gregtech.gregtech.blockentity.tool;

import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/** One-plate inventory and GT6 coin-pattern NBT for {@code MultiTileEntityMoldCoinage}. */
public final class CoinMoldBlockEntity extends BlockEntity {
    private final com.gregtech.gregtech.content.tool.CoinStampPattern pattern=new com.gregtech.gregtech.content.tool.CoinStampPattern();
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return acceptsPlate(stack); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return acceptsPlate(getStackInSlot(slot)) ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
        }
        @Override protected void onContentsChanged(int slot) { sync(); }
    };
    private LazyOptional<ItemStackHandler> itemCapability = LazyOptional.of(() -> inventory);

    public CoinMoldBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.COIN_MOLD.get(), pos, state);
    }

    public static boolean acceptsPlate(ItemStack stack) {
        if (stack.isEmpty()) return false;
        MaterialEquivalence.Form form = MaterialEquivalence.form(stack);
        return form != null && form.prefix() == MaterialPrefix.plateTiny
                && !GTItems.getStack(MaterialPrefix.coin, form.material(), 1).isEmpty();
    }

    public ItemStack contents() { return inventory.getStackInSlot(0).copy(); }

    public boolean insertPlate(ItemStack stack, boolean creative) {
        if (!contents().isEmpty() || !acceptsPlate(stack)) return false;
        ItemStack one = stack.copy();
        one.setCount(1);
        inventory.setStackInSlot(0, one);
        if (!creative) stack.shrink(1);
        return true;
    }

    /** Manual retrieval also accepts the unstruck plate; automation still cannot extract plates. */
    public ItemStack takeContents() {
        ItemStack current=inventory.getStackInSlot(0).copy();
        inventory.setStackInSlot(0,ItemStack.EMPTY);
        return current;
    }

    @Nullable public com.gregtech.gregtech.api.material.GTMaterial displayedMaterial() {
        var form=MaterialEquivalence.form(inventory.getStackInSlot(0));
        return form==null ? null : form.material();
    }

    public ItemStack takeCoin() {
        ItemStack current = inventory.getStackInSlot(0);
        if (current.isEmpty() || acceptsPlate(current)) return ItemStack.EMPTY;
        ItemStack output = current.copy();
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        return output;
    }

    public boolean strike() {
        ItemStack plate = inventory.getStackInSlot(0);
        if (!acceptsPlate(plate)) return false;
        ItemStack coin = GTItems.getStack(MaterialPrefix.coin,
                MaterialEquivalence.form(plate).material(), 1);
        if (coin.isEmpty()) return false;
        if (pattern.unique()) coin.getOrCreateTag().putBoolean(com.gregtech.gregtech.content.tool.CoinStampPattern.UNIQUE, true);
        // GT6 writes every row, including zero rows. Otherwise a blank custom die is
        // indistinguishable from an untagged registered coin using the default relief.
        CompoundTag coinTag = coin.getOrCreateTag();
        pattern.write(coinTag::putShort);
        inventory.setStackInSlot(0, coin);
        return true;
    }

    /** The slot is emptied before drop so player breaking and replacement cannot duplicate it. */
    public void dropContents() {
        if (level == null || level.isClientSide) return;
        ItemStack stored = inventory.getStackInSlot(0);
        if (stored.isEmpty()) return;
        ItemStack drop = stored.copy();
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5,
                worldPosition.getZ() + .5, drop);
    }

    public CompoundTag saveItemConfig() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(com.gregtech.gregtech.content.tool.CoinStampPattern.UNIQUE, pattern.unique());
        pattern.write(tag::putShort);
        return tag;
    }

    public void loadItemConfig(CompoundTag tag) {
        pattern.unique(tag.getBoolean(com.gregtech.gregtech.content.tool.CoinStampPattern.UNIQUE));
        pattern.read(tag::getShort);
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Pattern", saveItemConfig());
        tag.put("Inventory", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Pattern")) loadItemConfig(tag.getCompound("Pattern"));
        if (tag.contains("Inventory")) inventory.deserializeNBT(tag.getCompound("Inventory"));
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) load(packet.getTag());
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return capability == ForgeCapabilities.ITEM_HANDLER ? itemCapability.cast() : super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemCapability = LazyOptional.of(() -> inventory);
    }
}
