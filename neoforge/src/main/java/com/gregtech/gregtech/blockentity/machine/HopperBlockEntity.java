package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.machine.HopperSpec;
import com.gregtech.gregtech.api.machine.ITileEntityAdjacentInventoryUpdatable;
import com.gregtech.gregtech.block.machine.HopperBlock;
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

import java.util.List;

import static net.minecraft.world.level.block.DirectionalBlock.FACING;

/**
 * GT6 hopper block entity ({@code MultiTileEntityHopper}).
 * <p>
 * Parallel-slot inventory: all slots are equal peers. Items enter from top, exit toward facing side.
 * Screwdriver adjusts emission count (mMode 0-64). Monkey wrench toggles exact/divisible mode.
 */
public class HopperBlockEntity extends BlockEntity implements IItemHandler, IItemHandlerModifiable, MenuProvider {
    private static final String NBT_MODE = "gt.mode";
    private static final String NBT_EXACT = "gt.exact";
    private static final String NBT_CHECK = "gt.check";
    private static final String NBT_ITEMS = "gt.items";

    private final HopperSpec spec;
    private final NonNullList<ItemStack> inventory;
    private byte mMode;
    private boolean mExactMode;
    private boolean mLock;
    private byte mCheck = 3;

    private boolean inventoryChanged;
    private boolean blockUpdated;
    private byte mSuctionTimer = 20;


    public HopperBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.spec = specFromState(state);
        this.inventory = NonNullList.withSize(spec.slotCount(), ItemStack.EMPTY);
    }

    public HopperBlockEntity(BlockPos pos, BlockState state) {
        this(GTBlockEntities.HOPPER.get(), pos, state);
    }

    private static HopperSpec specFromState(BlockState state) {
        if (state.getBlock() instanceof HopperBlock block) return block.spec();
        throw new IllegalStateException("HopperBlockEntity on non-hopper block");
    }

    public HopperSpec spec() { return spec; }
    private int invsize() { return inventory.size(); }

    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(FACING)) return state.getValue(FACING);
        return Direction.NORTH;
    }

    // ── Server tick ──────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, HopperBlockEntity be) {
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
            // Phase 1: Output — push items out the facing side (skip UP, skip rail-without-cart)
            if (facing != Direction.UP && !invEmpty()) {
                IItemHandler target = findOutputTarget(facing);
                if (target != null) {
                    while (tMovedItems + (mMode <= 0 ? 1 : mMode) <= 64) {
                        mLock = true;
                        int moved = moveTo(target, mMode <= 0 ? 64 - tMovedItems : mMode, mMode <= 0 ? 1 : mMode);
                        mLock = false;
                        if (moved <= 0) break;
                        tMovedItems += moved;
                        if (mExactMode) break;
                    }
                } else if (level.isEmptyBlock(worldPosition.relative(facing))) {
                    tMovedItems += dropToWorld(facing);
                }
                // GT6: non-air solid block with no inventory → items stay in hopper
            }

            // Phase 2: Input — pull items from above (rail → minecart, or block entity, or world suction)
            IItemHandler source = findInputSource();
            if (source != null) {
                tMovedItems += moveFrom(source);
            } else if (level.getBlockEntity(worldPosition.above()) == null) {
                suckWorldItems();
            }

            if (tMovedItems > 0) mCheck = 3;
            else mCheck = -1;

            // Phase 3: Defragment inventory (merge partial stacks)
            if (inventoryChanged) {
                int stackLimit = getSlotLimitFor(0);
                for (int i = 0; i < invsize(); i++) {
                    for (int j = i + 1; j < invsize(); j++) {
                        if (!slotHas(j)) continue;
                        int maxSize = Math.min(stackLimit, inventory.get(j).getMaxStackSize());
                        if (slotHas(i)) {
                            if (inventory.get(i).getCount() < maxSize && ItemStack.isSameItemSameComponents(inventory.get(i), inventory.get(j))) {
                                tMovedItems += moveInternal(j, i);
                                if (inventory.get(i).getCount() >= maxSize) break;
                            }
                        } else {
                            tMovedItems += moveInternal(j, i);
                            if (slotHas(i) && inventory.get(i).getCount() >= maxSize) break;
                        }
                    }
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

    private boolean invEmpty() {
        for (ItemStack stack : inventory) if (!stack.isEmpty()) return false;
        return true;
    }

    private boolean slotHas(int slot) {
        return !inventory.get(slot).isEmpty();
    }

    // ── Item movement ────────────────────────────────────────────────────

    /** Move items from this hopper to a target handler. */
    private int moveTo(IItemHandler target, int maxAmount, int minAmount) {
        int totalMoved = 0;
        for (int slot = 0; slot < invsize() && totalMoved < maxAmount; slot++) {
            ItemStack stack = inventory.get(slot);
            if (stack.isEmpty()) continue;
            int toMove = Math.min(stack.getCount(), maxAmount - totalMoved);
            if (toMove < minAmount) continue;

            ItemStack simExtract = extractItemInternal(slot, toMove, true);
            if (simExtract.isEmpty()) continue;

            int prevCount = simExtract.getCount();
            ItemStack remainder = ItemHandlerHelper.insertItem(target, simExtract, false);
            int accepted = prevCount - remainder.getCount();
            if (accepted > 0) {
                extractItemInternal(slot, accepted, false);
                totalMoved += accepted;
            }
        }
        return totalMoved;
    }

    /** Pull items from a source handler into this hopper. */
    private int moveFrom(IItemHandler source) {
        int totalMoved = 0;
        for (int srcSlot = 0; srcSlot < source.getSlots(); srcSlot++) {
            ItemStack simExtract = source.extractItem(srcSlot, 64, true);
            if (simExtract.isEmpty()) continue;

            ItemStack remainder = ItemHandlerHelper.insertItem(this, simExtract, true);
            int acceptable = simExtract.getCount() - remainder.getCount();
            if (acceptable <= 0) continue;

            ItemStack extracted = source.extractItem(srcSlot, acceptable, false);
            if (!extracted.isEmpty()) {
                int actuallyInserted = acceptable - ItemHandlerHelper.insertItem(this, extracted, false).getCount();
                totalMoved += actuallyInserted;
                if (actuallyInserted < acceptable) {
                    // Return excess to source
                    ItemStack excess = extracted.copyWithCount(acceptable - actuallyInserted);
                    source.insertItem(srcSlot, excess, false);
                }
            }
        }
        return totalMoved;
    }

    /** Drop one stack through the output face into the world (air only). Spawns ItemEntity with zero velocity. */
    private int dropToWorld(Direction facing) {
        BlockPos targetPos = worldPosition.relative(facing);
        for (int slot = 0; slot < invsize(); slot++) {
            ItemStack stack = inventory.get(slot);
            if (stack.isEmpty()) continue;
            int toDrop = mMode <= 0 ? stack.getCount() : Math.min(stack.getCount(), mMode);
            ItemStack dropStack = stack.split(toDrop);
            if (stack.isEmpty()) inventory.set(slot, ItemStack.EMPTY);
            spawnItemEntity(dropStack, targetPos);
            setChanged();
            return toDrop;
        }
        return 0;
    }

    /** Spawn an ItemEntity at the given position with zero velocity (no scatter). */
    private void spawnItemEntity(ItemStack stack, BlockPos pos) {
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        entity.setDeltaMovement(0, 0, 0);
        entity.setPickUpDelay(10);
        level.addFreshEntity(entity);
    }

    /** GT6: suck dropped item stacks from the world into the hopper. Merges into existing partial stacks first.
     * @return true if at least one item was sucked (entity discarded) */
    private boolean suckWorldItems() {
        if (level == null) return false;
        AABB area = new AABB(
                worldPosition.getX() + 2 / 16.0, worldPosition.getY() + 10 / 16.0, worldPosition.getZ() + 2 / 16.0,
                worldPosition.getX() + 14 / 16.0, worldPosition.getY() + 1.25, worldPosition.getZ() + 14 / 16.0);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (entity.isRemoved()) continue;
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) continue;
            // Merge into existing partial stacks of the same type
            for (int i = 0; i < invsize() && !stack.isEmpty(); i++) {
                ItemStack existing = inventory.get(i);
                if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) continue;
                int limit = Math.min(getSlotLimitFor(i), existing.getMaxStackSize());
                int space = limit - existing.getCount();
                if (space <= 0) continue;
                int toMove = Math.min(stack.getCount(), space);
                existing.grow(toMove);
                stack.shrink(toMove);
            }
            if (stack.isEmpty()) {
                entity.discard();
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                return true;
            }
            // Then try empty slots
            for (int i = 0; i < invsize(); i++) {
                if (!slotHas(i)) {
                    inventory.set(i, stack.copy());
                    entity.discard();
                    setChanged();
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    return true;
                }
            }
        }
        return false;
    }

    /** Move items from one internal slot to another (defragmentation). */
    private int moveInternal(int from, int to) {
        ItemStack src = inventory.get(from);
        ItemStack dst = inventory.get(to);
        if (src.isEmpty()) return 0;
        if (dst.isEmpty()) {
            inventory.set(to, src.copy());
            inventory.set(from, ItemStack.EMPTY);
            int moved = src.getCount();
            return moved;
        }
        if (ItemStack.isSameItemSameComponents(src, dst)) {
            int space = dst.getMaxStackSize() - dst.getCount();
            int moved = Math.min(src.getCount(), space);
            if (moved > 0) {
                dst.grow(moved);
                src.shrink(moved);
                if (src.isEmpty()) inventory.set(from, ItemStack.EMPTY);
                return moved;
            }
        }
        return 0;
    }

    // ── IItemHandler (slot permissions mirror GT6 ISidedInventory) ─────────

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
        int limit = getSlotLimitFor(slot);
        ItemStack existing = inventory.get(slot);
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
            inventory.set(slot, stack.copyWithCount(toInsert));
            setChanged();
            inventoryChanged = true;
        }
        return toInsert >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - toInsert);
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= invsize() || amount <= 0) return ItemStack.EMPTY;
        // Block extraction during locked output phase, or when not the facing side
        // In GT6: canExtract = mLock || aSide != mFacing
        // This is checked per-side via getCapability with side parameter
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

    private int getSlotLimitFor(int slot) {
        return com.gregtech.gregtech.content.transport.HopperControlRules.slotLimit(mMode,false);
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

    /**
     * GT6 ISidedInventory mirror: extraction is blocked from the facing side
     * unless mLock is engaged (during output phase).
     */
    private class SidedHandler implements IItemHandler {
        private final Direction side;

        SidedHandler(Direction side) { this.side = side; }

        @Override public int getSlots() { return invsize(); }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) { return HopperBlockEntity.this.getStackInSlot(slot); }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            // GT6: canInsert = aSide != mFacing
            if (side == getFacing()) return stack;
            return HopperBlockEntity.this.insertItem(slot, stack, simulate);
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // GT6: canExtract = mLock || aSide != mFacing
            if (!mLock && side == getFacing()) return ItemStack.EMPTY;
            return HopperBlockEntity.this.extractItemInternal(slot, amount, simulate);
        }

        @Override public int getSlotLimit(int slot) { return HopperBlockEntity.this.getSlotLimit(slot); }

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

    // ── Placement: auto-connect to adjacent item pipe ────────────────────

    public void autoConnectOnPlace(@Nullable Direction placementFace) {
        if (level == null || level.isClientSide) return;
        Direction facing = getFacing();
        if (facing.getAxis().isHorizontal()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(facing));
            if (neighbor instanceof ItemPipeBlockEntity) {
                // The pipe will detect the hopper on its next tick via capability
                setChanged();
            }
        }
    }

    // ── Wrench / tool interaction ───────────────────────────────────────

    public void cycleMode(boolean sneak) {
        mMode=com.gregtech.gregtech.content.transport.HopperControlRules.cycle(mMode,sneak,false);setChanged();syncToClient();
    }

    public void toggleExactMode() {
        mExactMode = !mExactMode;
        setChanged();
        syncToClient();
    }

    public void resetMode() {
        mExactMode = false;
        mMode = 0;
        setChanged();
        syncToClient();
    }

    public byte getMode() { return mMode; }
    public boolean getExactMode() { return mExactMode; }

    /** GT6: snow golem walking over collects a snowball into any free slot. */
    public void collectSnowball() {
        for (int i = 0; i < invsize(); i++) {
            if (!slotHas(i)) {
                inventory.set(i, new ItemStack(Items.SNOWBALL, 1));
                setChanged();
                return;
            }
        }
    }

    public String getModeDescription() {
        if (mMode <= 0) return mExactMode ? "Emits up to 1 Stack" : "Emits up to 64 Items";
        return (mExactMode ? "Emits exact Stacksize of: " : "Emits divisible Stacksize of: ") + mMode;
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
        tag.putBoolean(NBT_EXACT, mExactMode);
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
        if (tag.contains(NBT_EXACT)) mExactMode = tag.getBoolean(NBT_EXACT);
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
        mMode = tag.getByte(NBT_MODE);
        mExactMode = tag.getBoolean(NBT_EXACT);
        mCheck = tag.contains(NBT_CHECK) ? tag.getByte(NBT_CHECK) : 3;
        ContainerHelper.loadAllItems(tag,inventory,lookup);
    }

    @Override
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.putByte(NBT_MODE, mMode);
        if (mExactMode) tag.putBoolean(NBT_EXACT, true);
        if (mCheck != 3) tag.putByte(NBT_CHECK, mCheck);
        ContainerHelper.saveAllItems(tag,inventory,lookup);
    }
}
