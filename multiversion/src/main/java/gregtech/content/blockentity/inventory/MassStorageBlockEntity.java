package gregtech.content.blockentity.inventory;

import gregtech.internal.inventory.MassStorageMaterialForms;

//? if >=1.21.1 {
//?} else {
import gregtech.content.item.behavior.BehaviorDuctTape;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
//?}

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Bulk item identity, long count and material fractions share one transaction path.
 * Tape packing, template reset, downward output and the original overflow allowance
 * retain their separate saved mode bits.
 */
//? if >=1.21.1 {
/*public class MassStorageBlockEntity extends BlockEntity implements gregtech.content.item.behavior.BehaviorDuctTape.Tapeable {
*///?} else {
public class MassStorageBlockEntity extends BlockEntity implements BehaviorDuctTape.Tapeable {
//?}
    public static final long CAPACITY = gregtech.content.storage.OriginalStorageTooltipData.MASS_CAPACITY;
    private final gregtech.internal.inventory.BulkStorageState<ItemStack> storage =
//? if >=1.21.1 {
            /*new gregtech.internal.inventory.BulkStorageState<>(CAPACITY, ItemStack::isSameItemSameComponents, stack -> stack.copyWithCount(1));
*///?} else {
            new gregtech.internal.inventory.BulkStorageState<>(CAPACITY, ItemStack::isSameItemSameTags, stack -> stack.copyWithCount(1));
//?}
    private int mode;
    private boolean inventoryChanged = true;
    /** Material below one stored template item; persisted so nugget insertion never loses matter. */
    private long partialUnits;
//? if >=1.21.1 {
//?} else {
    @Nullable
    private LazyOptional<IItemHandler> itemCap;
    @Nullable
    private LazyOptional<IItemHandler> bottomCap;
//?}

    public MassStorageBlockEntity(BlockPos pos, BlockState state) {
//? if >=1.21.1 {
        /*this(gregtech.platform.neoforge.logistics.StorageRegistries.MASS_STORAGE.get(), pos, state);
*///?} else {
        this(gregtech.content.registry.GTBlockEntities.MASS_STORAGE.get(), pos, state);
//?}
    }

    protected MassStorageBlockEntity(BlockEntityType<? extends MassStorageBlockEntity> type,
                                     BlockPos pos, BlockState state) {
        super(type, pos, state);
        storage.setCapacity(baseCapacity());
    }

    public ItemStack template() {
        ItemStack result = storage.filter();
        return result == null ? ItemStack.EMPTY : result;
    }

    public long stored() {
        return storage.count();
    }

    public long partialUnits() {
        return partialUnits;
    }

    public boolean isPacked() {
        return (mode & 8) != 0;
    }

    public int mode() {
        return mode;
    }

    public boolean resetsFilter() {
        return (mode & 2) != 0;
    }

    public boolean autoOutput() {
        return (mode & 1) != 0;
    }

    public boolean emitsOverflow() {
        return (mode & 4) != 0;
    }

    public long baseCapacity() {
        return getBlockState().getBlock() instanceof gregtech.content.block.inventory.MassStorageBlock block ? block.capacity() : CAPACITY;
    }

    public long maximumItems() {
        return baseCapacity() + (emitsOverflow() ? 256 : 0);
    }

    public void toggleAutoOutput() {
        mode ^= 1;
        markUpdated();
    }

    public void toggleOverflow() {
        mode ^= 4;
        storage.setCapacity(maximumItems());
        markUpdated();
    }

    public void toggleFilterReset() {
        mode ^= 2;
        clearFilterIfEmpty();
        markUpdated();
    }

    /** GT6 scissors/knife removes the tape without consuming the stored items. */
    public boolean unseal() {
        if (!isPacked()) return false;
        mode &= ~8;
        markUpdated();
        return true;
    }

    /** GT6 {@code TOOL_ducttape}: one tape use per item, with a minimum cost of 100. */
//? if >=1.21.1 {
//?} else {
    @Override
//?}
    public long onTape(Level level, BlockPos pos, Direction side, @Nullable Player player,
                       ItemStack tape, long uses, long quality, boolean sneaking,
                       float hitX, float hitY, float hitZ) {
        if (level.isClientSide || isPacked() || template().isEmpty() || stored() > uses) return 0;
        mode |= 8;
        markUpdated();
        return Math.max(100, stored());
    }

    /** GT6 non-packed harvest spills inventory; packed harvest keeps it inside the block item. */
    public List<ItemStack> looseDrops() {
        List<ItemStack> drops = new ArrayList<>();
        if (!isPacked() && !template().isEmpty()) {
            long remaining = stored();
            ItemStack key = template();
            int limit = Math.max(1, key.getMaxStackSize());
            while (remaining > 0) {
                int amount = (int) Math.min(remaining, limit);
                drops.add(key.copyWithCount(amount));
                remaining -= amount;
            }
        }
        // GT6 breakBlock drops the fractional material even in taped mode.
        drops.addAll(MassStorageMaterialForms.partialDrops(template(), partialUnits));
        return drops;
    }

    private boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemStack key = template();
//? if >=1.21.1 {
        /*return key.isEmpty() || ItemStack.isSameItemSameComponents(key, stack)
*///?} else {
        return key.isEmpty() || ItemStack.isSameItemSameTags(key, stack)
//?}
                || MassStorageMaterialForms.compatibleUnits(key, stack) > 0;
    }

    public int insert(ItemStack stack) {
        return (int) insert(stack, false);
    }

    private long insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || isPacked()) return 0;
        ItemStack key = template();
        if (key.isEmpty()) {
            long accepted = storage.insert(stack, stack.getCount(), simulate);
            if (!simulate && accepted > 0) markUpdated();
            return accepted;
        }
//? if >=1.21.1 {
        /*boolean exact = ItemStack.isSameItemSameComponents(key, stack);
*///?} else {
        boolean exact = ItemStack.isSameItemSameTags(key, stack);
//?}
        long templateUnits = MassStorageMaterialForms.templateUnits(key);
        if (exact && templateUnits == 0) {
            long accepted = storage.insert(stack, stack.getCount(), simulate);
            if (!simulate && accepted > 0) markUpdated();
            return accepted;
        }
        long offeredUnits = exact ? templateUnits : MassStorageMaterialForms.compatibleUnits(key, stack);
        if (offeredUnits <= 0 || templateUnits <= 0) return 0;
        long availableUnits = Math.max(0, maximumItems() - stored()) * templateUnits - partialUnits;
        if (availableUnits < offeredUnits) return 0;
        long accepted = Math.min(stack.getCount(), availableUnits / offeredUnits);
        if (!simulate && accepted > 0) {
            long totalUnits = partialUnits + accepted * offeredUnits;
            long completeItems = totalUnits / templateUnits;
            if (completeItems > 0) storage.insert(key, completeItems, false);
            partialUnits = totalUnits % templateUnits;
            markUpdated();
        }
        return accepted;
    }

    public ItemStack extractStack() {
        return extract(Integer.MAX_VALUE, false);
    }

    public ItemStack extractAmount(int amount) {
        return extract(amount, false);
    }

    private ItemStack extract(int amount, boolean simulate) {
        if (isPacked()) return ItemStack.EMPTY;
        ItemStack key = template();
        if (key.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        int taken = (int) storage.extract(Math.min(amount, key.getMaxStackSize()), simulate);
        if (taken == 0) return ItemStack.EMPTY;
        if (!simulate) {
            clearFilterIfEmpty();
            markUpdated();
        }
        return key.copyWithCount(taken);
    }

    private void clearFilterIfEmpty() {
        // GT6 keeps a zero-count template while partial material remains, even
        // when the reset-when-empty screwdriver mode is enabled.
        if (resetsFilter() && stored() == 0 && partialUnits == 0) storage.restore(null, 0);
    }

    private static void checkSlot(int slot) {
        if (slot != 0) throw new IllegalArgumentException("Bulk storage slot: " + slot);
    }

    private void markUpdated() {
        inventoryChanged = true;
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // ── Item capability ──────────────────────────────────────────────────────

    private final IItemHandler handler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            checkSlot(slot);
            return extract(Integer.MAX_VALUE, true);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            checkSlot(slot);
            int accepted = (int) insert(stack, simulate);
            return stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            checkSlot(slot);
            return extract(amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            checkSlot(slot);
            return (int) maximumItems();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            checkSlot(slot);
            return !isPacked() && matches(stack);
        }
    };

    private final IItemHandler bottomHandler = new IItemHandler() {
        public int getSlots() {
            return handler.getSlots();
        }

        public ItemStack getStackInSlot(int slot) {
            return handler.getStackInSlot(slot);
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return autoOutput() ? stack : handler.insertItem(slot, stack, simulate);
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return handler.extractItem(slot, amount, simulate);
        }

        public int getSlotLimit(int slot) {
            return handler.getSlotLimit(slot);
        }

        public boolean isItemValid(int slot, ItemStack stack) {
            return !autoOutput() && handler.isItemValid(slot, stack);
        }
    };

    public void tickAutomaticOutput() {
        if (level == null || level.isClientSide || isPacked()) return;
        if (!inventoryChanged && level.getGameTime() % 100 != 0) return;
        inventoryChanged = false;
        if (!autoOutput() && (!emitsOverflow() || stored() <= baseCapacity())) return;
        BlockPos below = worldPosition.below();
        if (!level.hasChunkAt(below)) return;
//? if >=1.21.1 {
        /*IItemHandler target = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, below, Direction.UP);
*///?} else {
        BlockEntity neighbor = level.getBlockEntity(below);
        IItemHandler target = neighbor == null ? null : neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(null);
//?}
        if (target == null) return;
        do {
            int amount = (int) Math.min(64, autoOutput() ? stored() : stored() - baseCapacity());
            ItemStack offered = extract(amount, true);
            if (offered.isEmpty()) return;
//? if >=1.21.1 {
            /*ItemStack rest = net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(target, offered.copy(), true);
*///?} else {
            ItemStack rest = net.minecraftforge.items.ItemHandlerHelper.insertItem(target, offered.copy(), true);
//?}
            int accepted = offered.getCount() - rest.getCount();
            if (accepted <= 0) return;
            ItemStack removed = extract(accepted, false);
//? if >=1.21.1 {
            /*ItemStack returned = net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(target, removed, false);
*///?} else {
            ItemStack returned = net.minecraftforge.items.ItemHandlerHelper.insertItem(target, removed, false);
//?}
            if (!returned.isEmpty()) insert(returned, false);
            if (returned.getCount() >= accepted || autoOutput()) return;
        } while (stored() > baseCapacity());
    }

//? if >=1.21.1 {
    /*public IItemHandler itemHandler() {
        return handler;
    }

    public IItemHandler itemHandler(@Nullable Direction side) {
        return side == Direction.DOWN ? bottomHandler : handler;
    }
*///?} else {
    @Override
    public <T> @org.jetbrains.annotations.NotNull LazyOptional<T> getCapability(
            @org.jetbrains.annotations.NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == Direction.DOWN) {
                if (bottomCap == null || !bottomCap.isPresent()) bottomCap = LazyOptional.of(() -> bottomHandler);
                return bottomCap.cast();
            }
            if (itemCap == null || !itemCap.isPresent()) {
                itemCap = LazyOptional.of(() -> handler);
            }
            return itemCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (itemCap != null) {
            itemCap.invalidate();
            itemCap = null;
        }
        if (bottomCap != null) {
            bottomCap.invalidate();
            bottomCap = null;
        }
    }
//?}

    // ── NBT + client sync (renderer shows content + amount) ─────────────────

    @Override
//? if >=1.21.1 {
    /*protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
*///?} else {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
//?}
        tag.putLong("gt.stored", stored());
        tag.putLong("gt.partial_units", partialUnits);
        tag.putInt("gt.mode", mode);
//? if >=1.21.1 {
        /*if (!template().isEmpty()) tag.put("gt.template", template().save(lookup));
*///?} else {
        if (!template().isEmpty()) tag.put("gt.template", template().save(new CompoundTag()));
//?}
    }

    @Override
//? if >=1.21.1 {
    /*protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
*///?} else {
    public void load(CompoundTag tag) {
        super.load(tag);
//?}
        mode = tag.contains("gt.mode") ? tag.getInt("gt.mode") : (tag.contains("gt.stored") ? 2 : 0);
        storage.setCapacity(maximumItems());
        inventoryChanged = true;
        // BulkStorageState's automatic reset only sees whole items. Keep reset
        // under this class's control so it cannot erase fractional contents.
        storage.setResetWhenEmpty(false);
//? if >=1.21.1 {
        /*ItemStack key = tag.contains("gt.template") ? ItemStack.parseOptional(lookup, tag.getCompound("gt.template")) : ItemStack.EMPTY;
*///?} else {
        ItemStack key = tag.contains("gt.template") ? ItemStack.of(tag.getCompound("gt.template")) : ItemStack.EMPTY;
//?}
        storage.restore(key.isEmpty() ? null : key, tag.getLong("gt.stored"));
        partialUnits = key.isEmpty() ? 0 : Math.max(0, tag.getLong("gt.partial_units"));
        clearFilterIfEmpty();
    }

    @Override
//? if >=1.21.1 {
    /*public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
*///?} else {
    public CompoundTag getUpdateTag() {
//?}
        CompoundTag tag = new CompoundTag();
//? if >=1.21.1 {
        /*saveAdditional(tag, lookup);
*///?} else {
        saveAdditional(tag);
//?}
        return tag;
    }

//? if >=1.21.1 {
//?} else {
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }
//?}

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

//? if >=1.21.1 {
//?} else {
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) load(pkt.getTag());
    }
//?}
}
