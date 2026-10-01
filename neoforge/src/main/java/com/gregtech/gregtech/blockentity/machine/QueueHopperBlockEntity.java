package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.machine.HopperSpec;
import com.gregtech.gregtech.api.machine.ITileEntityAdjacentInventoryUpdatable;
import com.gregtech.gregtech.block.machine.QueueHopperBlock;
import com.gregtech.gregtech.client.gui.HopperContainerMenu;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.world.level.block.DirectionalBlock.FACING;

/**
 * GT6 queuehopper block entity ({@code MultiTileEntityQueueHopper}).
 * <p>
 * Strict FIFO queue: slot 0 is input-only, last slot is output-only.
 * Items cascade-shift internally toward the output end.
 * Screwdriver adjusts per-slot capacity (mMode 1-64).
 */
public class QueueHopperBlockEntity extends BlockEntity implements IItemHandler, IItemHandlerModifiable, MenuProvider {
    private static final String NBT_MODE = "gt.mode";
    private static final String NBT_CHECK = "gt.check";
    private static final String NBT_ITEMS = "gt.items";

    private final HopperSpec spec;
    private final NonNullList<ItemStack> inventory;
    private byte mMode = 64;
    private boolean mLock;
    private byte mCheck = 3;

    private boolean inventoryChanged;
    private boolean blockUpdated;
    private byte mSuctionTimer = 20;


    public QueueHopperBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.spec = specFromState(state);
        this.inventory = NonNullList.withSize(Math.max(2, spec.slotCount()), ItemStack.EMPTY);
    }

    public QueueHopperBlockEntity(BlockPos pos, BlockState state) {
        this(GTBlockEntities.QUEUE_HOPPER.get(), pos, state);
    }

    private static HopperSpec specFromState(BlockState state) {
        if (state.getBlock() instanceof QueueHopperBlock block) return block.spec();
        throw new IllegalStateException("QueueHopperBlockEntity on non-queuehopper block");
    }

    public HopperSpec spec() { return spec; }
    private int invsize() { return inventory.size(); }
    private int lastSlot() { return invsize() - 1; }

    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(FACING)) return state.getValue(FACING);
        return Direction.NORTH;
    }

    // ── Server tick ──────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, QueueHopperBlockEntity be) {
        if (!level.isClientSide) be.tickServer();
    }

    private void tickServer() {
        int tMovedItems = 0;
        boolean processed = false;
        if (mCheck > 0) {
            mCheck--;
        } else if ((mCheck == 0 || inventoryChanged || blockUpdated) && !hasRedstoneIncomingFromNonRail()) {
            processed = true;
            Direction facing = getFacing();
            int last = lastSlot();

            // Phase 1: Output from last slot only, up to mMode items per tick
            if (facing != Direction.UP && slotHas(last)) {
                IItemHandler target = findOutputTarget(facing);
                if (target != null) {
                    while (tMovedItems < mMode) {
                        mLock = true;
                        int moved = pushFromSlot(last, target, mMode - tMovedItems);
                        mLock = false;
                        if (moved <= 0) break;
                        tMovedItems += moved;
                    }
                } else if (level.isEmptyBlock(worldPosition.relative(facing))) {
                    tMovedItems += dropLastToWorld(facing);
                }
                // GT6: non-air solid block with no inventory → items stay in hopper
            }

            // Phase 2: Input — pull from above (rail → minecart, or BE, or world suction)
            if (!slotFull(0)) {
                IItemHandler source = findInputSource();
                if (source != null) {
                    tMovedItems += pullIntoSlot0(source);
                } else if (level.getBlockEntity(worldPosition.above()) == null) {
                    suckWorldItems();
                }
            }

            if (tMovedItems > 0) mCheck = 3;
            else mCheck = -1;

            // Phase 3: Cascade shift — continuously move items toward output end
            if (inventoryChanged) {
                int oMovedItems = -1;
                while (oMovedItems != tMovedItems) {
                    oMovedItems = tMovedItems;
                    int limit = getSlotLimitFor(0);
                    for (int i = last; i > 0; i--) {
                        if (!slotHas(i - 1)) continue;
                        ItemStack src = inventory.get(i - 1);
                        if (!slotHas(i)) {
                            tMovedItems += moveInternal(i - 1, i);
                        } else if (ItemStack.isSameItemSameComponents(src, inventory.get(i))) {
                            int space = Math.min(limit, inventory.get(i).getMaxStackSize()) - inventory.get(i).getCount();
                            if (space > 0) {
                                int toMove = Math.min(src.getCount(), space);
                                if (toMove > 0) {
                                    inventory.get(i).grow(toMove);
                                    src.shrink(toMove);
                                    if (src.isEmpty()) inventory.set(i - 1, ItemStack.EMPTY);
                                    tMovedItems += toMove;
                                }
                            }
                        }
                    }
                }
            }

            // If cascade moved items into the last slot, output them in the same tick
            if (slotHas(last) && facing != Direction.UP) {
                IItemHandler target = findOutputTarget(facing);
                if (target != null) {
                    while (tMovedItems < mMode) {
                        mLock = true;
                        int moved = pushFromSlot(last, target, mMode - tMovedItems);
                        mLock = false;
                        if (moved <= 0) break;
                        tMovedItems += moved;
                    }
                } else if (level.isEmptyBlock(worldPosition.relative(facing))) {
                    tMovedItems += dropLastToWorld(facing);
                }
            }

            // Phase 4: Notify adjacent ITileEntityAdjacentInventoryUpdatable neighbors
            if (tMovedItems > 0) {
                notifyAdjacentInventoryUpdatables(facing);
            }
        }
        // Retain work notifications until the cooldown/redstone gate permits processing.
        if (processed) { inventoryChanged = false; blockUpdated = false; }
        if (tMovedItems > 0) setChanged();

        // Suction: idle check every 20t, switch to every-tick active while items present
        if (--mSuctionTimer <= 0) {
            if (!hasRedstoneIncomingFromNonRail() && suckWorldItems()) {
                mSuctionTimer = 1; // active mode: check every tick
            } else {
                mSuctionTimer = 20; // idle mode: check every second
            }
        }
    }

    /** GT6: detect incoming redstone from non-rail sides (blocks operation). Ignores rail redstone for minecart support. */
    private boolean hasRedstoneIncomingFromNonRail() {
        if (level == null) return false;
        if (!level.hasNeighborSignal(worldPosition)) return false;
        Direction facing = getFacing();
        if (level.getBlockState(worldPosition.relative(facing)).getBlock() instanceof BaseRailBlock) return false;
        if (level.getBlockState(worldPosition.above()).getBlock() instanceof BaseRailBlock) return false;
        return true;
    }

    /** Find output target: rail→minecart inventory, or block entity handler. */
    @Nullable
    private IItemHandler findOutputTarget(Direction facing) {
        BlockPos targetPos = worldPosition.relative(facing);
        if (level.getBlockState(targetPos).getBlock() instanceof BaseRailBlock) {
            for (AbstractMinecart cart : level.getEntitiesOfClass(AbstractMinecart.class, new AABB(targetPos))) {
                IItemHandler h = cart.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ENTITY);
                if (h != null) return h;
            }
            return null;
        }
        BlockEntity be = level.getBlockEntity(targetPos);
        if (be != null) return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,targetPos,facing.getOpposite());
        return null;
    }

    /** Find input source above: rail→minecart, or block entity (skipping anvils). Returns null → use world suction. */
    @Nullable
    private IItemHandler findInputSource() {
        BlockPos abovePos = worldPosition.above();
        if (level.getBlockState(abovePos).getBlock() instanceof BaseRailBlock) {
            for (AbstractMinecart cart : level.getEntitiesOfClass(AbstractMinecart.class, new AABB(abovePos))) {
                IItemHandler h = cart.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ENTITY);
                if (h != null) return h;
            }
        }
        BlockEntity aboveBe = level.getBlockEntity(abovePos);
        if (aboveBe != null) {
            if (aboveBe.getClass().getName().contains("Anvil")) return null; // skip anvils, use world suction
            return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,abovePos,Direction.DOWN);
        }
        return null;
    }

    private void notifyAdjacentInventoryUpdatables(Direction facing) {
        for (Direction side : Direction.values()) {
            if (side == Direction.UP || side == facing) continue;
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(side));
            if (neighbor instanceof ITileEntityAdjacentInventoryUpdatable u) {
                u.adjacentInventoryUpdated(side.getOpposite(), worldPosition);
            }
        }
    }

    private boolean slotHas(int slot) {
        return !inventory.get(slot).isEmpty();
    }

    private boolean slotFull(int slot) {
        ItemStack stack = inventory.get(slot);
        if (stack.isEmpty()) return false;
        return stack.getCount() >= Math.min(getSlotLimitFor(slot), stack.getMaxStackSize());
    }

    // ── Item movement ────────────────────────────────────────────────────

    /** Push items from a specific slot to a target handler, limited to maxTransfer. */
    private int pushFromSlot(int slot, IItemHandler target, int maxTransfer) {
        ItemStack stack = inventory.get(slot);
        if (stack.isEmpty()) return 0;
        int toPush = Math.min(stack.getCount(), maxTransfer);
        ItemStack remainder = ItemHandlerHelper.insertItem(target, stack.copyWithCount(toPush), false);
        int accepted = toPush - remainder.getCount();
        if (accepted > 0) {
            stack.shrink(accepted);
            if (stack.isEmpty()) inventory.set(slot, ItemStack.EMPTY);
            setChanged();
        }
        return accepted;
    }

    /** GT6: snow golem walking over collects a snowball into the last slot. */
    public void collectSnowball() {
        int last = lastSlot();
        if (!slotHas(last)) {
            inventory.set(last, new ItemStack(Items.SNOWBALL, 1));
            setChanged();
        }
    }

    /** Pull from source handler into slot 0 only. */
    private int dropLastToWorld(Direction facing) {
        int last = lastSlot();
        ItemStack stack = inventory.get(last);
        if (stack.isEmpty()) return 0;
        int toDrop = Math.min(stack.getCount(), mMode);
        ItemStack dropStack = stack.split(toDrop);
        if (stack.isEmpty()) inventory.set(last, ItemStack.EMPTY);
        BlockPos targetPos = worldPosition.relative(facing);
        spawnItemEntity(dropStack, targetPos);
        setChanged();
        return toDrop;
    }

    /** Spawn an ItemEntity at the given position with zero velocity (no scatter). */
    private void spawnItemEntity(ItemStack stack, BlockPos pos) {
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        entity.setDeltaMovement(0, 0, 0);
        entity.setPickUpDelay(10);
        level.addFreshEntity(entity);
    }

    private int pullIntoSlot0(IItemHandler source) {
        int totalMoved = 0;
        int limit = getSlotLimitFor(0);
        ItemStack existing = inventory.get(0);
        int space = existing.isEmpty() ? Math.min(limit, 64)
                : Math.min(limit, existing.getMaxStackSize()) - existing.getCount();
        if (space <= 0) return 0;

        for (int srcSlot = 0; srcSlot < source.getSlots() && space > 0; srcSlot++) {
            ItemStack simExtract = source.extractItem(srcSlot, space, true);
            if (simExtract.isEmpty()) continue;
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, simExtract)) continue;

            int toTake = Math.min(simExtract.getCount(), space);
            ItemStack extracted = source.extractItem(srcSlot, toTake, false);
            if (!extracted.isEmpty()) {
                if (existing.isEmpty()) {
                    inventory.set(0, extracted.copy());
                    existing = inventory.get(0);
                } else {
                    existing.grow(extracted.getCount());
                }
                space -= extracted.getCount();
                totalMoved += extracted.getCount();
                setChanged();
                inventoryChanged = true;
            }
        }
        return totalMoved;
    }

    /** GT6: suck dropped item stacks into slot 0. Merges into existing stack of same type.
     * @return true if at least one item was accepted; partial entity stacks remain in the world */
    private boolean suckWorldItems() {
        if (level == null) return false;
        AABB area = new AABB(
                worldPosition.getX() + 2 / 16.0, worldPosition.getY() + 10 / 16.0, worldPosition.getZ() + 2 / 16.0,
                worldPosition.getX() + 14 / 16.0, worldPosition.getY() + 1.25, worldPosition.getZ() + 14 / 16.0);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (entity.isRemoved()) continue;
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) continue;
            ItemStack remainder = insertItem(0, stack, false);
            if (remainder.getCount() == stack.getCount()) continue;
            if (remainder.isEmpty()) entity.discard();
            else entity.setItem(remainder);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            return true;
        }
        return false;
    }

    private int moveInternal(int from, int to) {
        ItemStack src = inventory.get(from);
        if (src.isEmpty()) return 0;
        inventory.set(to, src.copy());
        inventory.set(from, ItemStack.EMPTY);
        int moved = src.getCount();
        return moved;
    }

    // ── IItemHandler ─────────────────────────────────────────────────────

    @Override
    public int getSlots() { return invsize(); }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot >= 0 && slot < invsize() ? inventory.get(slot) : ItemStack.EMPTY;
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (slot < 0 || slot >= invsize() || stack.isEmpty()) return stack;
        // Only slot 0 accepts new items (FIFO entry point)
        if (slot != 0) return stack;
        int limit = getSlotLimitFor(slot);
        ItemStack existing = inventory.get(0);
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(existing, stack)) return stack;
            int space = Math.min(limit, existing.getMaxStackSize()) - existing.getCount();
            if (space <= 0) return stack;
            int toInsert = Math.min(stack.getCount(), space);
            if (!simulate) {
                existing.grow(toInsert);
                setChanged();
                inventoryChanged = true;
            }
            return toInsert >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - toInsert);
        }
        int toInsert = Math.min(stack.getCount(), Math.min(limit, stack.getMaxStackSize()));
        if (!simulate) {
            inventory.set(0, stack.copyWithCount(toInsert));
            setChanged();
            inventoryChanged = true;
        }
        return toInsert >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - toInsert);
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= invsize() || amount <= 0) return ItemStack.EMPTY;
        // Only last slot allows extraction (FIFO exit point)
        if (slot != lastSlot()) return ItemStack.EMPTY;
        return extractItemInternal(slot, amount, simulate);
    }

    private ItemStack extractItemInternal(int slot, int amount, boolean simulate) {
        ItemStack stack = inventory.get(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int toExtract = Math.min(amount, stack.getCount());
        if (!simulate) {
            ItemStack result = stack.split(toExtract);
            if (stack.isEmpty()) inventory.set(slot, ItemStack.EMPTY);
            inventoryChanged = true;
            setChanged();
            return result;
        }
        return stack.copyWithCount(toExtract);
    }

    @Override
    public int getSlotLimit(int slot) { return getSlotLimitFor(slot); }

    /** GT6: mMode directly sets per-slot capacity (min 1, max 64, default 64). */
    private int getSlotLimitFor(int slot) {
        return com.gregtech.gregtech.content.transport.HopperControlRules.slotLimit(mMode,true);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) { return true; }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (slot >= 0 && slot < invsize()) {
            inventory.set(slot, stack);
            setChanged();
            inventoryChanged = true;
        }
    }

    // ── Side-aware IItemHandler wrapper ──────────────────────────────────

    private class SidedHandler implements IItemHandler {
        private final Direction side;

        SidedHandler(Direction side) { this.side = side; }

        @Override public int getSlots() { return 2; } // Only slots 0 and last are accessible

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot == 0) return QueueHopperBlockEntity.this.getStackInSlot(0);
            if (slot == 1) return QueueHopperBlockEntity.this.getStackInSlot(lastSlot());
            return ItemStack.EMPTY;
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            // GT6: canInsert = slot == 0 && side != facing
            if (side == getFacing()) return stack;
            if (slot == 0) return QueueHopperBlockEntity.this.insertItem(0, stack, simulate);
            if (slot == 1) return QueueHopperBlockEntity.this.insertItem(lastSlot(), stack, simulate);
            return stack;
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // GT6: canExtract = last slot && (mLock || aSide != mFacing)
            if (!mLock && side == getFacing()) return ItemStack.EMPTY;
            if (slot == 0) return QueueHopperBlockEntity.this.extractItem(0, amount, simulate);
            if (slot == 1) return QueueHopperBlockEntity.this.extractItemInternal(lastSlot(), amount, simulate);
            return ItemStack.EMPTY;
        }

        @Override public int getSlotLimit(int slot) { return QueueHopperBlockEntity.this.getSlotLimit(slot); }

        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return true; }
    }

    // ── Capability ──────────────────────────────────────────────────────

    public IItemHandler capabilityHandler(@Nullable Direction side) {
        return side==null?this:sideCaps.computeIfAbsent(side,SidedHandler::new);
    }

    private final java.util.Map<Direction,IItemHandler> sideCaps =
            new java.util.EnumMap<>(Direction.class);

    public void invalidateItemCapabilities() {
        sideCaps.clear();if(level!=null)level.invalidateCapabilities(worldPosition);
    }

    // ── Wrench / tool interaction ───────────────────────────────────────

    public void cycleMode(boolean sneak) {
        mMode=com.gregtech.gregtech.content.transport.HopperControlRules.cycle(mMode,sneak,true);setChanged();syncToClient();
    }

    public void resetMode() {
        mMode = 64;
        setChanged();
        syncToClient();
    }

    public byte getMode() { return mMode; }

    public String getModeDescription() {
        return "Slot Size: " + (mMode <= 0 ? 64 : mMode);
    }

    // ── Inventory helpers ───────────────────────────────────────────────

    public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (int i = 0; i < invsize(); i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty()) {
                spawnItemEntity(stack, worldPosition);
                inventory.set(i, ItemStack.EMPTY);
            }
        }
    }

    public void onNeighborChanged() {
        blockUpdated = true;
        if (mCheck < 0) mCheck = 0;
    }

    // ── Sync ────────────────────────────────────────────────────────────

    private void syncToClient() {
        if (level == null || level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        setChanged();
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this);
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.5;
        double z = worldPosition.getZ() + 0.5;
        double distSq = 64.0 * 64.0;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(x, y, z) <= distSq) {
                player.connection.send(packet);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        CompoundTag tag = super.getUpdateTag(lookup);
        tag.putByte(NBT_MODE, mMode);
        ContainerHelper.saveAllItems(tag,inventory,lookup);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt,net.minecraft.core.HolderLookup.Provider lookup) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) handleUpdateTag(tag,lookup);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.handleUpdateTag(tag,lookup);
        if (tag.contains(NBT_MODE)) mMode = tag.getByte(NBT_MODE);
        inventory.clear();
        ContainerHelper.loadAllItems(tag,inventory,lookup);
    }

    // ── GUI ──────────────────────────────────────────────────────────────

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.gregtech." + spec.id());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new HopperContainerMenu(GTMenuTypes.forSlotCount(invsize()), containerId, playerInv, this);
    }

    // ── NBT ─────────────────────────────────────────────────────────────

    @Override
    protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        mMode = tag.contains(NBT_MODE) ? tag.getByte(NBT_MODE) : 64;
        mCheck = tag.contains(NBT_CHECK) ? tag.getByte(NBT_CHECK) : 3;
        inventory.clear();
        ContainerHelper.loadAllItems(tag,inventory,lookup);
    }

    @Override
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.putByte(NBT_MODE, mMode);
        if (mCheck != 3) tag.putByte(NBT_CHECK, mCheck);
        ContainerHelper.saveAllItems(tag,inventory,lookup);
    }
}
