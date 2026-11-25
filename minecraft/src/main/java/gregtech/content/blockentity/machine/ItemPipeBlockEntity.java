package gregtech.content.blockentity.machine;

import gregtech.engine.machine.ITileEntityAdjacentInventoryUpdatable;
import gregtech.engine.machine.ItemPipeSpec;
import gregtech.content.block.machine.ItemPipeBlock;
import gregtech.content.blockentity.GTBlockEntity;
import gregtech.content.cover.ComponentCoverRuntime;
import gregtech.content.cover.CoverItems;
import gregtech.content.cover.CoverStore;
import gregtech.content.cover.CoverUtilityBehaviors;
import gregtech.content.cover.MachineCoverSpec;
import gregtech.content.cover.PanelCoverRuntime;
import gregtech.content.cover.PipeCoverHost;
import gregtech.content.registry.GTBlockEntities;
import gregtech.content.transport.ItemPipeNetwork;
import gregtech.content.transport.ItemPipeNetwork.Membership;
import gregtech.content.transport.ItemPipeNetworkTransfer;
import gregtech.content.transport.ItemPipeNetworks;
import gregtech.engine.stack.NbtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * Native item inventory, network routing endpoint and lazily allocated cover host.
 *
 * <p>The pipe does not tick. {@link ItemPipeNetworks} schedules it: a pipe holding items is a
 * source and delivers whole stacks straight into the nearest inventory beside its network that
 * takes them, along a route cached per network layout ({@link ItemPipeNetwork}). Items only rest in
 * the pipe they entered. The pipe has {@code invSize} slots of one stack each and carries
 * {@code invSize} stacks per second; every pipe on a delivery's path pays that bandwidth. A stack is
 * never delivered back into its recorded origin block or the other half of a vanilla double chest.</p>
 *
 * <p>Face handlers apply insertion/extraction cover rules. Placement refuses an item
 * filter facing another item pipe. Covers are allocated on demand and tick before routing.
 *
 * <p>The host does not expose MachineControl or ProgressSensorSource. In particular,
 * a transferred-item counter alone does not implement GT6's progress interface.
 */
public class ItemPipeBlockEntity extends GTBlockEntity
        implements IItemHandler, ITileEntityAdjacentInventoryUpdatable, PipeCoverHost {
    /** Bandwidth window: transfer counters restart every 20 ticks. */
    private static final int BANDWIDTH_WINDOW = 20;
    /** Each slot holds one ordinary stack. */
    private static final int STACK_LIMIT = 64;
    private static final byte NO_ORIGIN = -1;

    private final ItemPipeSpec spec;
    private final NonNullList<ItemStack> inventory;
    /**
     * Per slot, the face its stack entered through ({@link #NO_ORIGIN} for an unsided insert). The
     * inventory across that face is never a delivery target for that stack.
     */
    private final byte[] slotOrigins;
    /** Stacks carried in window {@link #transferWindow}; see {@link #syncTransferWindow}. */
    private long transferredThisSecond;
    private long transferWindow = Long.MIN_VALUE;
    /**
     * The membership cell of the snapshot {@link ItemPipeNetworks} last put this pipe in. Invalidating
     * the snapshot empties the cell, so this pipe needs no visit. Not persisted.
     */
    private @Nullable Membership membership;
    private byte disabledInputs;
    private byte disabledOutputs;
    /** Faces the wrench disconnected; auto-connect leaves them alone. Persisted so a reload keeps them. */
    private int manualDisconnects;

    private static final String NBT_ITEMS = "gt.items";
    private static final String COVER_UPDATE = "gt.cover_sync";
    /** Read only, to give stacks in older saves the single input side the pipe used to track. */
    private static final String NBT_LAST_RECEIVED = "gt.last_received";
    private static final String NBT_SLOT_ORIGINS = "gt.slot_origins";
    private static final String NBT_DISABLED_INPUTS = "gt.disabled_inputs";
    private static final String NBT_DISABLED_OUTPUTS = "gt.disabled_outputs";
    private static final String NBT_MANUAL_DISCONNECTS = "gt.manual_disconnects";
    private static final String NBT_TRANSFERRED = "gt.transferred";
    private static final String NBT_TRANSFER_WINDOW = "gt.transfer_window";

//? if >=1.21.1 {
//?} else {
    private LazyOptional<IItemHandler> handler = LazyOptional.of(() -> this);
//?}

    // ── Cover state: zero cost when unused ───────────────────────────────────
    //
    // An item pipe is the most numerous host the port has. It does not tick; ItemPipeNetworks ticks
    // only the pipes that carry covers. CoverStore keeps the face array null and the panel runtime
    // uncreated until the first cover is attached, so a coverless pipe pays one boolean read on the
    // transfer path and never walks a face:
    //   * the retriever's pending flags are six bits of one byte rather than the machine's
    //     `boolean[6]` — see `retrieverPending`.

    /** Shared face storage, lazy panel runtime and `gt_cover_*` persistence. */
    private final CoverStore coverStore = new CoverStore(this);

    /**
     * The cover tick counter, standing in for GT6's {@code SERVER_TIME} / {@code aTimer} exactly as
     * {@link BasicMachineBlockEntity}'s own cover tick field does: this counter advances once per
     * covered server tick, independent of {@code level.getGameTime()}.
     */
    private long coverTicks;

    /**
     * Whether the covers were suspended by a controller cover on the previous cover tick, i.e. the
     * {@code !aStopped} edge of GT6 {@code CoverRetrieverItem}
     * ({@code onStoppedUpdate} sets the retriever's pending value to 1 when it resumes).
     * {@link BasicMachineBlockEntity} keeps the same field.
     */
    private boolean utilityWasStopped;

    /**
     * GT6's {@code aData.mValues[aSide]} for the retriever ({@code CoverRetrieverItem}): a
     * per-face "run on the next tick even though the 20-tick clock has not come round" flag, set on
     * resume and currently cleared only after moving items. GT6 cleared it before the
     * eligible attempt, even when the search found nothing.
     *
     * <p>The machine uses a boolean array; this pipe uses six bits. Neither pending
     * representation is persisted, and success-based retry can last beyond one tick.</p>
     */
    private byte retrieverPending;

    public ItemPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.spec = specFromState(state);
        int slots = Math.max(1, spec.invSize());
        this.inventory = NonNullList.withSize(slots, ItemStack.EMPTY);
        this.slotOrigins = new byte[slots];
        Arrays.fill(slotOrigins, NO_ORIGIN);
    }

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        this(GTBlockEntities.ITEM_PIPE.get(), pos, state);
    }

    private static ItemPipeSpec specFromState(BlockState state) {
        if (state.getBlock() instanceof ItemPipeBlock block) return block.spec();
        throw new IllegalStateException("ItemPipeBlockEntity on non-item-pipe block");
    }

    public ItemPipeSpec spec() { return spec; }

    // ── Scheduling hooks: the pipe has no ticker ─────────────────────────────

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null) return;
        if (level.isClientSide) {
            requestModelDataUpdate();
            gregtech.engine.machine.PipeModelData.refreshCrossChunkNeighbors(level, worldPosition);
        } else {
            // Connection repair and snapshot invalidation wait for the end of the tick, when the
            // neighbours loaded together with this pipe are in place.
            ItemPipeNetworks.joined(this);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) ItemPipeNetworks.left(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide) ItemPipeNetworks.left(this);
        super.onChunkUnloaded();
    }

    /** Only the six connection properties are layout; waterlogging and the like are not. */
    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState state) {
        int before = AUTO_CONNECTIONS.connectionMask(getBlockState());
        super.setBlockState(state);
        if (level != null && !level.isClientSide && AUTO_CONNECTIONS.connectionMask(state) != before) {
            ItemPipeNetworks.invalidate(this);
        }
    }

    public @Nullable ItemPipeNetwork network() {
        return membership == null ? null : membership.network();
    }

    public void membership(@Nullable Membership membership) {
        this.membership = membership;
    }

    // ── Covers: host API (GT6's item pipe as a cover host) ───────────────────

    @Override
    public ItemStack getCover(Direction side) {
        return coverStore.get(side);
    }

    public boolean hasCovers() { return coverStore.hasCovers(); }
    @Override public PanelCoverRuntime panels() {
        return coverStore.panels();
    }

    @Override
    public Family pipeFamily() {
        return Family.ITEM;
    }

    @Override
    public boolean attachCover(Direction side, ItemStack stack) {
        return coverStore.attach(side, stack);
    }

    @Override
    public void attached(Direction side, ItemStack stack) {
        coversChanged(stack);
    }

    /** The signals a pipe face can emit are the panel ones; {@code afterTick} publishes them. */
    @Override
    public void removed(Direction side, ItemStack stack) {
        panels().afterTick();
        coversChanged(stack);
    }

    private void coversChanged(ItemStack stack) {
//? if >=1.21.1 {
        /*if (level != null) level.invalidateCapabilities(worldPosition);
*///?}
        syncCovers();
        ItemPipeNetworks.coversChanged(this, CoverUtilityBehaviors.RETRIEVER_ITEM.equals(CoverItems.behavior(stack)));
    }

    @Override
    public ItemStack removeCover(Direction side) {
        return coverStore.remove(side);
    }

    /**
     * The pipe's drop-on-break for covers; called from {@link ItemPipeBlock#onRemove}, next to
     * {@link #dropStoredItems()} — that is where this pipe's own removal logic lives and the block
     * entity is still resolvable there.
     */
    public void dropCovers() {
        if (!coverStore.hasCovers()) return;
        coverStore.drop();
        setChanged();
    }

    /** Cover behaviour id of a face; {@code null} when no cover is attached. */
    private String coverIdOf(Direction side) {
        return CoverItems.behavior(getCover(side));
    }

    private boolean retrieverPending(int face) {
        return (retrieverPending & (1 << face)) != 0;
    }

    private void setRetrieverPending(int face, boolean value) {
        if (value) retrieverPending |= (byte) (1 << face);
        else retrieverPending &= (byte) ~(1 << face);
    }

    /**
     * Dispatch inventory covers against this pipe; controller pause gates transfer operations. Called
     * every server tick by {@link ItemPipeNetworks} while the pipe carries covers.
     */
    public void tickCovers() {
        if (level == null) return;
        PanelCoverRuntime runtime = panels();
        coverTicks++;
        runtime.beforeTick();
        if (runtime.stopped()) {
            utilityWasStopped = true;
            runtime.afterTick();
            return;
        }
        boolean resumed = utilityWasStopped;
        utilityWasStopped = false;

        for (Direction side : Direction.values()) {
            String id = coverIdOf(side);
            if (id == null) continue;
            int face = side.ordinal();
            switch (id) {
                case CoverItems.PUMP, CoverItems.CONVEYOR, CoverItems.ROBOT_ARM ->
                        gregtech.content.cover.ComponentCoverRuntime.tick(this,side,level.getGameTime());
                case CoverUtilityBehaviors.RETRIEVER_ITEM -> {
                    ItemStack cover = getCover(side);
                    boolean acted = CoverUtilityBehaviors.tickRetriever(
                            level, worldPosition, side, this,
                            CoverUtilityBehaviors.itemFilter(cover, level.registryAccess()),
                            MachineCoverSpec.inverted(cover),
                            coverTicks, retrieverPending(face) || resumed);
                    if (acted) setRetrieverPending(face, false);
                    else if (resumed) setRetrieverPending(face, true);
                }
                default -> { }
            }
        }
        runtime.afterTick();
    }

    @Override
    public boolean supportsFilterCover(String behavior) {
        return CoverUtilityBehaviors.FILTER_ITEM.equals(behavior) || CoverUtilityBehaviors.RETRIEVER_ITEM.equals(behavior);
    }

    /**
     * GT6 {@code CoverFilterItem} asked about a live face: does the item filter on {@code side}
     * let {@code candidate} through? The pipe's counterpart of
     * {@link BasicMachineBlockEntity#coverFilterPermits(Direction, ItemStack)}.
     *
     * <p>A face with <b>no cover at all</b> and a face whose cover is not an item filter both permit:
     * {@code CoverItems.behavior(ItemStack.EMPTY)} is {@code null}, so the {@code FILTER_ITEM} test in
     * {@link CoverUtilityBehaviors#itemFilterPermits} fails and the candidate is never looked at. An
     * <em>empty filter stack on a real item filter cover</em> is the other case and is not permissive:
     * it returns {@code !MachineCoverSpec.inverted(cover)}, false in the default whitelist mode and
     * true only in the inverted one — GT6 {@code CoverFilterItem}
     * {@code return aData.mVisuals[aCoverSide] == 0;} read as "intercept". A suspended cover refuses
     * everything.</p>
     */
    public boolean coverFilterPermits(Direction side, ItemStack candidate) {
        return !coverStore.shuttered(side) && CoverUtilityBehaviors.itemFilterPermits(getCover(side), coverStore.stopped(), candidate, level.registryAccess());
    }

    /**
     * GT6 {@code CoverRetrieverItem} — {@code return aCoverSide == aSide;}, on both
     * {@code interceptItemInsert} and {@code interceptItemExtract}: the retriever refuses
     * <em>every</em> item insert and extract on the face it owns, so a pipe can neither push into nor
     * pull out of that face; only the retriever itself moves items there.
     */
    public boolean coverBlocksItemTraffic(Direction side) {
        return CoverUtilityBehaviors.RETRIEVER_ITEM.equals(coverIdOf(side));
    }

    /**
     * The two item-cover interception rules of one face as the single question every traffic path
     * asks: may {@code candidate} cross {@code side}? GT6 asks them as two hooks on the same face
     * ({@code CoverFilterItem}, {@code CoverRetrieverItem}) and the port composes them
     * here for the same reason {@code BasicMachineBlockEntity} keeps them as two methods and calls
     * both — this is the one place they are combined, and it is the only predicate
     * {@link #sendTo}, the capability view and the retriever's own face consult.
     *
     * <p>A pipe without covers answers {@code true} on the first line, so the whole cover system costs
     * one boolean read on the transfer path.</p>
     */
    public boolean coverPermitsItemTraffic(Direction side, ItemStack candidate) {
        if (!coverStore.hasCovers()) return true;
        if (coverBlocksItemTraffic(side)) return false;
        return coverFilterPermits(side, candidate);
    }

    // ── Network endpoint ─────────────────────────────────────────────────────

    /**
     * One delivery pass as a source, then wake whatever fed the pipe so it can push again.
     *
     * @return the number of stacks moved
     */
    public int deliverThrough(ItemPipeNetwork network) {
        int origins = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (!inventory.get(slot).isEmpty() && slotOrigins[slot] >= 0) origins |= 1 << slotOrigins[slot];
        }
        int moved = network.deliverFrom(this);
        if (moved > 0) {
            setChanged();
            for (Direction side : Direction.values()) {
                if ((origins & (1 << side.ordinal())) == 0) continue;
                BlockPos neighbor = worldPosition.relative(side);
                if (level.hasChunkAt(neighbor)
                        && level.getBlockEntity(neighbor) instanceof ITileEntityAdjacentInventoryUpdatable updatable) {
                    updatable.adjacentInventoryUpdated(side.getOpposite(), worldPosition);
                }
            }
        }
        return moved;
    }

    public boolean connected(Direction face) {
        return getBlockState().getValue(ItemPipeBlock.propFor(face));
    }

    /** Layout half of {@link #canEmitTo}: the wrench setting and a retriever, both rebuild the snapshot. */
    public boolean linksOut(Direction face) {
        return (disabledOutputs & (1 << face.ordinal())) == 0 && !coverBlocksItemTraffic(face);
    }

    /** Layout half of {@link #canLinkIn}. */
    public boolean linksIn(Direction face) {
        return (disabledInputs & (1 << face.ordinal())) == 0 && !coverBlocksItemTraffic(face);
    }

    /**
     * Move one of {@code source}'s stacks into the inventory across {@code face} of this pipe, the
     * exit. Checks everything the snapshot does not: shutters and cover modes, a hopper or dispenser
     * now facing the pipe, the exit face's filter per stack, and each stack's origin.
     */
    public boolean sendTo(Direction face, ItemPipeBlockEntity source) {
        if (level == null || isRemoved() || source.isRemoved() || !connected(face) || !canEmitTo(face)) return false;
        ItemPipeNetwork route = source.network();
        if (route == null || !route.valid() || network() != route) return false;
        BlockPos targetPos = worldPosition.relative(face);
        if (!level.hasChunkAt(targetPos)) return false;
        BlockEntity target = level.getBlockEntity(targetPos);
        if (target == null || target instanceof ItemPipeBlockEntity) return false;
        if (outputsInto(level.getBlockState(targetPos), face.getOpposite())) return false;
        for (int slot = 0; slot < source.inventory.size(); slot++) {
            ItemStack stack = source.inventory.get(slot);
            if (stack.isEmpty() || source.cameFrom(slot, targetPos) || !coverPermitsItemTraffic(face, stack)) continue;
            int accepted = gregtech.content.transport.ItemPipeTransferAdapter.transfer(
                    stack.copyWithCount(Math.min(stack.getCount(), Math.min(STACK_LIMIT, stack.getMaxStackSize()))),
                    () -> route.valid() && network() == route && source.network() == route
                            ? itemHandlerAt(targetPos, face.getOpposite()) : null).accepted();
            if (accepted <= 0) continue;
            stack.shrink(accepted);
            if (stack.isEmpty()) source.clearSlot(slot);
            source.setChanged();
            return true;
        }
        return false;
    }

    /** Whether the stack in {@code slot} entered from the inventory at {@code pos}. */
    private boolean cameFrom(int slot, BlockPos pos) {
        byte origin = slotOrigins[slot];
        if (origin < 0) return false;
        BlockPos originPos = worldPosition.relative(Direction.from3DDataValue(origin));
        if (originPos.equals(pos)) return true;
        BlockState target = level.getBlockState(pos);
        return target.getBlock() instanceof net.minecraft.world.level.block.ChestBlock
                && target.getValue(net.minecraft.world.level.block.ChestBlock.TYPE)
                        != net.minecraft.world.level.block.state.properties.ChestType.SINGLE
                && pos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(target)).equals(originPos);
    }

    /** A vanilla hopper or dispenser facing the pipe would hand the stack straight back. */
    private static boolean outputsInto(BlockState target, Direction towardPipe) {
        if (target.getBlock() instanceof net.minecraft.world.level.block.HopperBlock) {
            return target.getValue(net.minecraft.world.level.block.HopperBlock.FACING) == towardPipe;
        }
        if (target.getBlock() instanceof net.minecraft.world.level.block.DispenserBlock) {
            return target.getValue(net.minecraft.world.level.block.DispenserBlock.FACING) == towardPipe;
        }
        return false;
    }

    private IItemHandler itemHandler(BlockEntity entity,Direction face) {
        if (entity == null) return null;
//? if >=1.21.1 {
        /*return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,entity.getBlockPos(),face);
*///?} else {
        return entity.getCapability(ForgeCapabilities.ITEM_HANDLER,face).orElse(null);
//?}
    }

    private IItemHandler itemHandlerAt(BlockPos pos,Direction face) {
        return level != null && level.hasChunkAt(pos) ? itemHandler(level.getBlockEntity(pos),face) : null;
    }

    // ── Bandwidth: invSize stacks per second ─────────────────────────────────

    /**
     * Counters belong to a world-time window rather than to this pipe, so a charge made by another
     * pipe's delivery is never lost to a reset, whichever runs first.
     */
    private void syncTransferWindow() {
        if (level == null || level.isClientSide) return;
        long window = level.getGameTime() / BANDWIDTH_WINDOW;
        if (window != transferWindow) {
            transferWindow = window;
            transferredThisSecond = 0;
        }
    }

    public boolean pipeCapacityCheck() {
        syncTransferWindow();
        return ItemPipeNetworkTransfer.hasCapacity(transferredThisSecond, spec.invSize());
    }

    public void chargeTransfer() {
        syncTransferWindow();
        transferredThisSecond++;
        // Transit pipes also persist the charge even when their inventory never changes.
        if (level != null && !level.isClientSide) level.blockEntityChanged(worldPosition);
    }

    /** The game time at which this pipe's bandwidth is restored. */
    public long nextBandwidthWindow() {
        return level == null ? 0 : (level.getGameTime() / BANDWIDTH_WINDOW + 1) * BANDWIDTH_WINDOW;
    }

    /** Whether items may leave through {@code side} right now. */
    public boolean canEmitTo(Direction side) {
        return !coverStore.shuttered(side) && gregtech.content.cover.ComponentCoverRuntime.allowsItem(getCover(side),false) && (disabledOutputs & (1 << side.ordinal())) == 0;
    }

    /** Whether items may enter through {@code side} right now. */
    public boolean canLinkIn(Direction side) {
        return !coverStore.shuttered(side) && gregtech.content.cover.ComponentCoverRuntime.allowsItem(getCover(side),true) && (disabledInputs & (1 << side.ordinal())) == 0;
    }

    public boolean hasStoredItems() {
        return !isEmpty();
    }
    private boolean isEmpty() {
        for (ItemStack s : inventory) if (!s.isEmpty()) return false;
        return true;
    }

    /** Dump items to adjacent inventories when pipe is broken (Factorio-style). */
    public void dumpItemsToAdjacent() {
        if (level == null || level.isClientSide) return;
        for (Direction side : Direction.values()) {
            if (isEmpty()) break;
            BlockPos targetPos = worldPosition.relative(side);
            if (!level.hasChunkAt(targetPos)) continue;
            BlockEntity entity = level.getBlockEntity(targetPos);
            if (entity == null || entity instanceof ItemPipeBlockEntity) continue;
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.get(slot);
                if (stack.isEmpty()) continue;
                int moved = gregtech.content.transport.ItemPipeTransferAdapter.transfer(stack.copy(),
                        () -> itemHandlerAt(targetPos, side.getOpposite())).accepted();
                if (moved > 0) {
                    stack.shrink(moved);
                    if (stack.isEmpty()) clearSlot(slot);
                    setChanged();
                }
            }
        }
    }

    /** Drop all stored items on the ground when the pipe block is broken. */
    public void dropStoredItems() {
        if (level == null || level.isClientSide) return;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.get(slot);
            if (stack.isEmpty()) continue;
            gregtech.util.GTItemDrops.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            clearSlot(slot);
        }
    }

    private void clearSlot(int slot) {
        setSlot(slot, ItemStack.EMPTY, NO_ORIGIN);
    }

    private void setSlot(int slot, ItemStack stack, byte origin) {
        inventory.set(slot, stack);
        slotOrigins[slot] = origin;
    }

    private static final PipeAutoConnections<ItemPipeBlockEntity> AUTO_CONNECTIONS = new PipeAutoConnections<>(
            ItemPipeBlockEntity.class, ItemPipeBlock::propFor, ItemPipeBlockEntity::isFaceManuallyDisconnected,
            ItemPipeBlockEntity::isConnectableNeighbor, ItemPipeBlockEntity::isNativePlacementEndpoint);

    public void autoConnectOnPlace(@Nullable Direction clickedFace) {
        AUTO_CONNECTIONS.onPlace(this, clickedFace);
    }

    public boolean tryConnectTo(Direction side) {
        return AUTO_CONNECTIONS.tryConnect(this, side);
    }

    /** Ordinary capability endpoints connect only when explicitly clicked during placement. */
    private boolean isNativePlacementEndpoint(Direction side) {
        IItemHandler handler = itemHandlerAt(worldPosition.relative(side), side.getOpposite());
        return handler != null && handler.getSlots() > 0;
    }

    private boolean isConnectableNeighbor(Direction side) {
        BlockPos neighborPos = worldPosition.relative(side);
        BlockState neighborState = level.getBlockState(neighborPos);
        Direction neighborSide = side.getOpposite();
        if (neighborState.getBlock() instanceof gregtech.api.transport.ItemPipeConnectable block
                && block.canConnectItemPipe(neighborSide)) return true;
        return level.getBlockEntity(neighborPos) instanceof gregtech.api.transport.ItemPipeConnectable host
                && host.canConnectItemPipe(neighborSide);
    }

    public boolean isFaceManuallyDisconnected(Direction side) {
        return (manualDisconnects & (1 << side.ordinal())) != 0;
    }

    /** Called after a wrench toggled {@code side}; the bit is persisted with the pipe. */
    public void setFaceManuallyDisconnected(Direction side, boolean disconnected) {
        int next = disconnected
                ? manualDisconnects | (1 << side.ordinal())
                : manualDisconnects & ~(1 << side.ordinal());
        if (next != manualDisconnects) {
            manualDisconnects = next;
            setChanged();
        }
    }

    /** Toggle item pipe side I/O with monkey wrench. */
    public void toggleSideIO(Direction side) {
        boolean isConnected = getBlockState().getValue(ItemPipeBlock.propFor(side));
        if (!isConnected) {
            // Connect
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(ItemPipeBlock.propFor(side), true));
            return;
        }

        // Cycle: enabled -> disable output -> disable input -> disable both -> back to enabled
        boolean outDisabled = (disabledOutputs & (1 << side.ordinal())) != 0;
        boolean inDisabled = (disabledInputs & (1 << side.ordinal())) != 0;

        if (!outDisabled && !inDisabled) {
            disabledOutputs |= (1 << side.ordinal());
        } else if (outDisabled && !inDisabled) {
            disabledOutputs &= ~(1 << side.ordinal());
            disabledInputs |= (1 << side.ordinal());
        } else if (!outDisabled) {
            disabledOutputs |= (1 << side.ordinal());
        } else {
            disabledOutputs &= ~(1 << side.ordinal());
            disabledInputs &= ~(1 << side.ordinal());
        }
        setChanged();
        ItemPipeNetworks.invalidate(this);
    }

    // === IItemHandler ===

    @Override
    public int getSlots() { return inventory.size(); }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot >= 0 && slot < inventory.size() ? inventory.get(slot) : ItemStack.EMPTY;
    }

    /**
     * A stack goes only into the requested slot while it is empty, up to one ordinary stack; a partly
     * filled slot is not topped up. The unsided view records no origin; {@link SideAwareItemHandler}
     * records the face. Any insert wakes the pipe as a source.
     */
    @NotNull
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return insertItem(slot, stack, simulate, NO_ORIGIN);
    }

    private ItemStack insertItem(int slot, ItemStack stack, boolean simulate, byte origin) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (slot < 0 || slot >= inventory.size() || !inventory.get(slot).isEmpty()) return stack;
        int insertable = Math.min(stack.getCount(), Math.min(getSlotLimit(slot), stack.getMaxStackSize()));
        if (insertable <= 0) return stack;
        if (!simulate) {
            setSlot(slot, stack.copyWithCount(insertable), origin);
            setChanged();
            if (level != null && !level.isClientSide) ItemPipeNetworks.wake(this);
        }
        return insertable >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - insertable);
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= inventory.size() || amount <= 0) return ItemStack.EMPTY;
        ItemStack stack = inventory.get(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int toExtract = Math.min(amount, stack.getCount());
        ItemStack result = stack.copy();
        result.setCount(toExtract);
        if (!simulate) {
            stack.shrink(toExtract);
            if (stack.isEmpty()) clearSlot(slot);
            setChanged();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) { return STACK_LIMIT; }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) { return true; }

    // === Capability ===

//? if >=1.21.1 {
    /*public IItemHandler capabilityHandler(@Nullable Direction side) {
        return side == null ? this : sideHandler(side);
*///?} else {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (isRemoved() || !handler.isPresent()) return LazyOptional.empty();
            return side == null ? handler.cast() : sideHandler(side).cast();
        }
        return super.getCapability(cap, side);
//?}
    }

    /** One external view per side, recording the input face and consulting live cover rules. */
//? if >=1.21.1 {
    /*private IItemHandler sideHandler(Direction side) {
        return sideCaps.computeIfAbsent(side, ExternalSideItemHandler::new);
    }

    private final Map<Direction, IItemHandler> sideCaps = new EnumMap<>(Direction.class);
*///?} else {
    private LazyOptional<IItemHandler> sideHandler(Direction side) {
        return sideCaps.computeIfAbsent(side, face -> LazyOptional.of(() -> new ExternalSideItemHandler(face)));
    }

    private final Map<Direction, LazyOptional<IItemHandler>> sideCaps = new EnumMap<>(Direction.class);

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
        sideCaps.values().forEach(LazyOptional::invalidate);
        sideCaps.clear();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        handler = LazyOptional.of(() -> this);
    }
//?}

    /** Wraps this pipe's IItemHandler to record the face each inserted stack came through.
     *  IItemHandler.insertItem() carries no direction, so the sided view supplies it; the origin
     *  keeps that stack from being delivered back into the inventory it came from. */
    @Override public IItemHandler componentItems(Direction side) { return new SideAwareItemHandler(side); }

    private class SideAwareItemHandler implements IItemHandler {
        private final byte sideOrdinal;

        SideAwareItemHandler(Direction side) {
            this.sideOrdinal = (byte) side.ordinal();
        }

        @Override
        public int getSlots() { return ItemPipeBlockEntity.this.getSlots(); }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) { return ItemPipeBlockEntity.this.getStackInSlot(slot); }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if ((disabledInputs & (1 << sideOrdinal)) != 0) return stack;
            return ItemPipeBlockEntity.this.insertItem(slot, stack, simulate, sideOrdinal);
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if ((disabledOutputs & (1 << sideOrdinal)) != 0) return ItemStack.EMPTY;
            return ItemPipeBlockEntity.this.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) { return ItemPipeBlockEntity.this.getSlotLimit(slot); }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return ItemPipeBlockEntity.this.isItemValid(slot, stack);
        }
    }

    /** Consults current covers even through a handle acquired before a cover was attached. */
    private final class ExternalSideItemHandler extends SideAwareItemHandler {
        private final Direction side;

        ExternalSideItemHandler(Direction side) {
            super(side);
            this.side = side;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return permits(stack, true) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack stack = getStackInSlot(slot);
            return !stack.isEmpty() && permits(stack, false)
                    ? super.extractItem(slot, amount, simulate)
                    : ItemStack.EMPTY;
        }

        private boolean permits(ItemStack stack, boolean insert) {
            if (isRemoved() || coverStore.shuttered(side)) return false;
            ItemStack cover = getCover(side);
            return cover.isEmpty()
                    || (ComponentCoverRuntime.allowsItem(cover, insert) && coverPermitsItemTraffic(side, stack));
        }
    }

    // === NBT ===

    /** Integer slot indices preserve inventories larger than ContainerHelper's 256-slot format. */
    private void saveInventory(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        ListTag items = new ListTag();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.get(slot);
            if (stack.isEmpty()) continue;
            ItemStack encoded = stack.getCount() > 99 ? stack.copyWithCount(1) : stack;
            CompoundTag row = (CompoundTag) NbtCompat.saveItem(encoded, lookup);
            row.putInt("Slot", slot);
            if (stack.getCount() > 99) row.putInt("gt.pipe_count", stack.getCount());
            items.add(row);
        }
        tag.put("Items", items);
    }

    private void loadInventory(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        inventory.clear();
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag row = items.getCompound(i);
            int slot = row.contains("Slot", Tag.TAG_BYTE) ? row.getByte("Slot") & 255 : row.getInt("Slot");
            if (slot < 0 || slot >= inventory.size()) continue;
            ItemStack stack = NbtCompat.readItem(row, lookup);
            if (!stack.isEmpty() && row.contains("gt.pipe_count")) {
                stack.setCount(Math.max(1, row.getInt("gt.pipe_count")));
            }
            inventory.set(slot, stack);
        }
    }

    /** Split legacy single-slot buffers without discarding items or their input face. */
    private void splitLegacyStacks() {
        int empty = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.get(slot);
            int limit = Math.min(STACK_LIMIT, stack.getMaxStackSize());
            while (!stack.isEmpty() && stack.getCount() > limit) {
                while (empty < inventory.size() && !inventory.get(empty).isEmpty()) empty++;
                // Keep any overflow in its existing slot if an addon reduced the pipe's capacity.
                if (empty == inventory.size()) return;
                setSlot(empty, stack.split(limit), slotOrigins[slot]);
            }
        }
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        if (tag.contains(COVER_UPDATE, Tag.TAG_COMPOUND)) {
            coverStore.load(tag.getCompound(COVER_UPDATE), lookup);
            return;
        }
        disabledInputs = tag.contains(NBT_DISABLED_INPUTS) ? tag.getByte(NBT_DISABLED_INPUTS) : 0;
        disabledOutputs = tag.contains(NBT_DISABLED_OUTPUTS) ? tag.getByte(NBT_DISABLED_OUTPUTS) : 0;
        manualDisconnects = tag.contains(NBT_MANUAL_DISCONNECTS) ? tag.getByte(NBT_MANUAL_DISCONNECTS) & 0x3F : 0;
        // A counter from a legacy save has no window and cannot consume the current allowance.
        transferWindow = tag.contains(NBT_TRANSFER_WINDOW, Tag.TAG_LONG)
                ? tag.getLong(NBT_TRANSFER_WINDOW)
                : Long.MIN_VALUE;
        transferredThisSecond = transferWindow == Long.MIN_VALUE ? 0L : Math.max(0L, tag.getLong(NBT_TRANSFERRED));
        if (tag.contains(NBT_ITEMS)) {
            loadInventory(tag.getCompound(NBT_ITEMS), lookup);
        }
        loadOrigins(tag);
        splitLegacyStacks();
        coverStore.load(tag, lookup);
        // /data and structure tools can replace an already-loaded pipe's inventory or covers.
        if (level != null && !level.isClientSide) ItemPipeNetworks.joined(this);
    }

    private void loadOrigins(CompoundTag tag) {
        Arrays.fill(slotOrigins, NO_ORIGIN);
        if (tag.contains(NBT_SLOT_ORIGINS)) {
            byte[] saved = tag.getByteArray(NBT_SLOT_ORIGINS);
            System.arraycopy(saved, 0, slotOrigins, 0, Math.min(saved.length, slotOrigins.length));
        } else if (tag.contains(NBT_LAST_RECEIVED)) {
            byte side = tag.getByte(NBT_LAST_RECEIVED);
            for (int slot = 0; slot < inventory.size(); slot++) {
                if (!inventory.get(slot).isEmpty()) slotOrigins[slot] = side;
            }
        }
        for (int slot = 0; slot < slotOrigins.length; slot++) {
            if (inventory.get(slot).isEmpty() || slotOrigins[slot] < NO_ORIGIN || slotOrigins[slot] > 5) {
                slotOrigins[slot] = NO_ORIGIN;
            }
        }
    }

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        tag.putByteArray(NBT_SLOT_ORIGINS, slotOrigins.clone());
        tag.putByte(NBT_DISABLED_INPUTS, disabledInputs);
        tag.putByte(NBT_DISABLED_OUTPUTS, disabledOutputs);
        tag.putByte(NBT_MANUAL_DISCONNECTS, (byte) manualDisconnects);
        tag.putLong(NBT_TRANSFERRED, transferredThisSecond);
        tag.putLong(NBT_TRANSFER_WINDOW, transferWindow);
        CompoundTag itemsTag = new CompoundTag();
        saveInventory(itemsTag, lookup);
        tag.put(NBT_ITEMS, itemsTag);
        coverStore.save(tag, lookup);
    }

    @Override
    protected void writeUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.writeUpdateTag(tag, lookup);
        CompoundTag covers = new CompoundTag();
        coverStore.save(covers, lookup);
        // Forge drops an empty packet tag. Keep a snapshot even when the last cover was removed.
        tag.put(COVER_UPDATE, covers);
    }

    /**
     * Covers have to reach the client renderer, so the pipe sends the update tag built above; this is
     * the same packet pair as {@link BasicMachineBlockEntity#getUpdatePacket()} and
     * {@link FluidPipeBlockEntity#getUpdatePacket()}.
     */
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void handleDataPacket(net.minecraft.network.Connection net,
            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,
            @Nullable HolderLookup.Provider lookup) {
        if (pkt.getTag() != null) readUpdateTag(pkt.getTag(), lookup);
    }

    public long getTransferredThisSecond() {
        syncTransferWindow();
        return transferredThisSecond;
    }
    public byte getDisabledInputs() { return disabledInputs; }
    public byte getDisabledOutputs() { return disabledOutputs; }

    @Override
    public void adjacentInventoryUpdated(Direction side, BlockPos sourcePos) {
        // An inventory beside the network may have room now: sources waiting on it retry.
        if (level != null && !level.isClientSide) ItemPipeNetworks.inventoryUpdated(this);
    }
    // ── Client model data (dynamic pipe model neighbor sizes) ───────────────

    @Override
//? if >=1.21.1 {
    /*public net.neoforged.neoforge.client.model.data.ModelData getModelData() {
*///?} else {
    public net.minecraftforge.client.model.data.ModelData getModelData() {
//?}
        float[] halves = new float[6];
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            halves[d.ordinal()] = level == null
                    ? (float) gregtech.engine.machine.PipeGeometry.NONE
                    : (float) gregtech.engine.machine.PipeGeometry.pipeHalfOf(
                            level.getBlockState(worldPosition.relative(d)));
        }
//? if >=1.21.1 {
        /*return net.neoforged.neoforge.client.model.data.ModelData.builder()
*///?} else {
        return net.minecraftforge.client.model.data.ModelData.builder()
//?}
                .with(gregtech.engine.machine.PipeModelData.NEIGHBOR_HALVES, halves)
                .build();
    }


}
