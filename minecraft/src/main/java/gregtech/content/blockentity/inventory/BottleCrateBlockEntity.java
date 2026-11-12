package gregtech.content.blockentity.inventory;

import gregtech.content.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
//?}
import org.jetbrains.annotations.Nullable;

/** GT6 nine-slot bottle crate: whole stacks, synchronized contents and packed block drops. */
public class BottleCrateBlockEntity extends BlockEntity {
    /** GT6's {@code mDisplay[9]} - nine bottles, in three rows of three. */
    public static final int SLOTS = 9;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide)
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return gregtech.content.block.inventory.BottleCrateBlock.isBottle(stack);
        }
    };

    private CompoundTag removedInventory;

//? if >=1.21.1 {
//?} else {
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> items);
//?}

    public BottleCrateBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.BOTTLE_CRATE.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    /** The first slot without a bottle, or {@code -1} when the crate is full. */
    public int firstFree() {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (items.getStackInSlot(slot).isEmpty()) return slot;
        }
        return -1;
    }

    /** The highest slot with a bottle, or {@code -1} when the crate is empty (the last one goes out first). */
    public int lastFilled() {
        for (int slot = SLOTS - 1; slot >= 0; slot--) {
            if (!items.getStackInSlot(slot).isEmpty()) return slot;
        }
        return -1;
    }

    /** Insert into the first free slot and return the unaccepted remainder. */
    public ItemStack insert(ItemStack held) {
        int slot = firstFree();
        if (slot < 0 || !gregtech.content.block.inventory.BottleCrateBlock.isBottle(held)) return held;
        return items.insertItem(slot, held, false);
    }

    /** The last bottle out, or an empty stack when the crate is empty. */
    public ItemStack extractLast() {
        int slot = lastFilled();
        return slot < 0 ? ItemStack.EMPTY : items.extractItem(slot, 1, false);
    }

    /** Copies of nonempty slots; block removal retains inventory through packedStack instead. */
    public java.util.List<ItemStack> contents() {
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) out.add(stack.copy());
        }
        return out;
    }

    @Override
//? if >=1.21.1 {
    /*protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        saveData(tag, lookup);
    }
*///?} else {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveData(tag, null);
    }
//?}
    private void saveData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        tag.put("gt_bottles", gregtech.engine.stack.HandlerNbt.save(items, lookup));
    }

    @Override
//? if >=1.21.1 {
    /*protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        loadData(tag, lookup);
    }
*///?} else {
    public void load(CompoundTag tag) {
        super.load(tag);
        loadData(tag, null);
    }
//?}
    private void loadData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        removedInventory = null;
        CompoundTag inventory = tag.getCompound("gt_bottles").copy();
        inventory.putInt("Size", SLOTS);
        gregtech.engine.stack.HandlerNbt.load(items, inventory, lookup);
    }

    /** Preserve a snapshot for vanilla playerDestroy, which requests loot after onRemove. */
    public void detachInventory() {
        if (removedInventory != null) return;
        removedInventory = gregtech.engine.stack.NbtCompat.saveWithoutMetadata(this, level.registryAccess());
        for (int slot = 0; slot < SLOTS; slot++) items.setStackInSlot(slot, ItemStack.EMPTY);
    }

    public ItemStack packedStack() {
        ItemStack result = new ItemStack(getBlockState().getBlock());
        CompoundTag data = removedInventory == null
                ? gregtech.engine.stack.NbtCompat.saveWithoutMetadata(this, level.registryAccess())
                : removedInventory.copy();
        if (!data.getCompound("gt_bottles").getList("Items", 10).isEmpty()) {
            data.putString("id","gt6ce:bottle_crate");
            gregtech.engine.stack.BlockEntityData.write(result, data);
        }
        return result;
    }

//? if >=1.21.1 {
    /*@Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) { return saveWithoutMetadata(lookup); }
*///?} else {
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
//?}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

//? if >=1.21.1 {
//?} else {
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable net.minecraft.core.Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }
        return super.getCapability(capability, side);
    }
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemCapability = LazyOptional.of(() -> items);
    }

//?}
}
