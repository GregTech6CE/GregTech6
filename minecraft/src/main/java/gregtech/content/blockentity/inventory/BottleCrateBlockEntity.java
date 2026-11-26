package gregtech.content.blockentity.inventory;

import gregtech.content.blockentity.GTBlockEntity;
import gregtech.content.registry.GTBlockEntities;
import gregtech.engine.stack.BlockEntityData;
import gregtech.engine.stack.HandlerNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
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
public class BottleCrateBlockEntity extends GTBlockEntity {
    /** GT6's {@code mDisplay[9]} - nine bottles, in three rows of three. */
    public static final int SLOTS = 9;

    private static final String BOTTLES_KEY = "gt_bottles";

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
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        tag.put(BOTTLES_KEY, HandlerNbt.save(items, lookup));
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        removedInventory = null;
        loadBottles(items, tag, lookup);
    }

    public static IItemHandler readPackedBottles(ItemStack stack, @Nullable HolderLookup.Provider lookup) {
        var bottles = new ItemStackHandler(SLOTS);
        var data = BlockEntityData.read(stack);
        if (data != null) loadBottles(bottles, data, lookup);
        return bottles;
    }

    private static void loadBottles(ItemStackHandler bottles, CompoundTag data, @Nullable HolderLookup.Provider lookup) {
        CompoundTag inventory = data.getCompound(BOTTLES_KEY).copy();
        inventory.putInt("Size", SLOTS);
        HandlerNbt.load(bottles, inventory, lookup);
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
        if (!data.getCompound(BOTTLES_KEY).getList("Items", 10).isEmpty()) {
            data.putString("id","gt6ce:bottle_crate");
            BlockEntityData.write(result, data);
        }
        return result;
    }

    @Override
    protected void writeUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        writeWithoutMetadata(tag, lookup);
    }

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
