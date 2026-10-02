package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.inventory.BlockContents;
import com.gregtech.gregtech.api.machine.ITileEntityAdjacentInventoryUpdatable;
import com.gregtech.gregtech.api.machine.ItemPipeSpec;
import com.gregtech.gregtech.block.machine.ItemPipeBlock;
import com.gregtech.gregtech.content.cover.CoverItems;
import com.gregtech.gregtech.content.cover.CoverUtilityBehaviors;
import com.gregtech.gregtech.content.cover.MachineCoverSpec;
import com.gregtech.gregtech.content.cover.PanelCoverHost;
import com.gregtech.gregtech.content.cover.PanelCoverRuntime;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * GT6 item pipe block entity. Routes items through a pipe network with step-size based priority.
 *
 * <h2>Covers: an item pipe is a cover host, the way GT6's own pipe is</h2>
 *
 * <p>GT6 attaches covers to <em>tiles</em>, and both item-side utility covers are pipe covers: the
 * item filter, whose {@code interceptItemInsert}/{@code interceptItemExtract} are asked from the
 * host's own item handling ({@code CoverFilterItem:115-127}), and the item retriever, whose
 * {@code interceptCoverPlacement} refuses any host that is not a tickable item pipe
 * ({@code CoverRetrieverItem:50}). This class is therefore a {@link PanelCoverHost} exactly the way
 * {@link FluidPipeBlockEntity} became one in §111, and with the same three properties:</p>
 *
 * <ul>
 *   <li><b>No {@code MachineControl}.</b> {@code coverControl} is not overridden, so
 *       {@link PanelCoverHost#coverPossible} and {@link PanelCoverHost#coverSupportsPossible} keep
 *       their interface defaults ({@code false}; {@code PanelCoverHost:16-17}) and
 *       {@code MachineControl.find} answers {@code null} for a block entity that is not a
 *       {@code Provider} ({@code MachineControl:25-27}). A control panel is therefore refused at
 *       attach by {@code PanelCoverRuntime.canAttach:33-41} — no control means no selector
 *       ({@code :36}), no progress panel ({@code :37}), no status panel ({@code :38}) and, with no
 *       energy buffer either, no energy display ({@code :39}) — which is the same outcome a fluid
 *       pipe has and needs no special case here. What still works is what only <em>emits</em>: the
 *       redstone emitter, the conductor pair, the cover controller and the shutter, because
 *       {@code afterTick} reads the face's own redstone input and needs no control. Neither item-side
 *       cover consults one either: the filter reads its own stack ({@code CoverFilterItem:118-119})
 *       and the retriever reads the world ({@code CoverRetrieverItem:69-72}).</li>
 *   <li><b>The retriever's host test is satisfied by construction.</b>
 *       {@code CoverRetrieverItem:50} refuses a host that is not {@code canTick()} and an
 *       {@code ITileEntityItemPipe}; this <em>is</em> the item pipe and it ticks
 *       ({@link ItemPipeBlock#getTicker}), so there is no predicate to write — unlike the fluid
 *       pipe, where the pressure valve's one-tank rule had to be carried into {@code attachCover}.
 *       The same class's {@code pipeCapacityCheck()} gate ({@code :61}) is the receiver side of
 *       {@code ST.move} and stays where GT6 puts it: {@code CoverUtilityBehaviors.tickRetriever}
 *       simulates the insert into the target before extracting ({@code :301-303}).</li>
 *   <li><b>Lazily allocated cover state.</b> See the field comments below: a world holds thousands of
 *       item pipes and {@link #tickServer} runs for every one of them, so a coverless pipe pays one
 *       boolean read per tick and allocates nothing.</li>
 * </ul>
 *
 * <h2>Deliberate deviations from GT6's item-filter placement rule</h2>
 *
 * <p>{@code CoverFilterItem:135-137} refuses to place the item filter when the host is an item pipe
 * <em>and</em> the neighbour across the cover's face is another item pipe, and
 * {@code CoverFilterItem:129-133} additionally disconnects the two pipes ({@code interceptConnect}).
 * Neither is mirrored, for a reason that is the whole point of requirement §112.4: this port makes a
 * face filter gate the pipe-to-pipe traffic of the very face it sits on (GT6 asks
 * {@code interceptItemInsert} per face, so a filter facing a pipe would only ever be asked on a path
 * GT6 does not have — its pipes hand items to the neighbour's inventory, not to the neighbour's
 * filter). Refusing placement there would make the sender-side half of the rule unreachable and would
 * silently fail an attach on a pipe that is connected to another pipe. The retriever has no such rule
 * — it declares no {@code interceptConnect} and no {@code interceptCoverPlacement} beyond the host
 * test above — so its own face stays wholly its own.</p>
 */
public class ItemPipeBlockEntity extends BlockEntity
        implements IItemHandler, ITileEntityAdjacentInventoryUpdatable, PanelCoverHost {
    private final ItemPipeSpec spec;
    private final NonNullList<ItemStack> inventory;
    private long transferredThisSecond;
    private int ticksUntilReset;
    private byte lastReceivedFrom = -1;
    private byte disabledInputs;
    private byte disabledOutputs;

    private static final String NBT_ITEMS = "gt.items";
    private static final String NBT_LAST_RECEIVED = "gt.last_received";
    private static final String NBT_DISABLED_INPUTS = "gt.disabled_inputs";
    private static final String NBT_DISABLED_OUTPUTS = "gt.disabled_outputs";
    private static final String NBT_TRANSFERRED = "gt.transferred";
    /** One cover per face under the same key the machine and the fluid pipe use ({@code BasicMachineBlockEntity:1299}). */
    private static final String NBT_COVER_PREFIX = "gt_cover_";


    // ── Cover state: zero cost when unused ───────────────────────────────────
    //
    // An item pipe is the hottest host the port has: serverTick runs for every pipe of every loaded
    // chunk, and the routing below only runs on every fourth tick. So a coverless pipe must not pay
    // for the cover system at all:
    //   * `covers` is null until the first cover is attached — no ItemStack[6] per pipe;
    //   * `hasCovers` is the single boolean the tick pass and the capability test, so a pipe without
    //     covers does one field read and never walks a face;
    //   * `panels` is created on first attach rather than in the field initialiser the way
    //     BasicMachineBlockEntity:71 does it, because PanelCoverRuntime itself allocates an int[6] and
    //     a boolean[6] (PanelCoverRuntime:12-13); this is the one place this class deliberately does
    //     not copy the machine's eager field;
    //   * `faceHandlers` is null until a face actually carries an item filter or a retriever;
    //   * the retriever's pending flags are six bits of one byte rather than the machine's
    //     `boolean[6]` (`BasicMachineBlockEntity:1523`) — see `retrieverPending`.

    /** One cover item per {@link Direction#ordinal()}; {@code null} until a cover is attached. */
    private @Nullable ItemStack[] covers;

    /** Whether any face carries a cover. The cover tick pass and the face handler both hang off this. */
    private boolean hasCovers;

    /**
     * The cover tick counter, standing in for GT6's {@code SERVER_TIME} / {@code aTimer} exactly as
     * {@code BasicMachineBlockEntity.coverTicks:76} does for the machine — the full argument is on
     * {@link com.gregtech.gregtech.content.cover.CoverAttachmentBehaviors}'s class javadoc:
     * {@code level.getGameTime()} never advances inside a one-tick GameTest body, this counter
     * advances once per covered server tick.
     */
    private long coverTicks;

    /** Created by {@link #panels()} on first use, never in the constructor. */
    private @Nullable PanelCoverRuntime panels;

    /** Per-face filter views, built by {@link #faceHandler} and dropped by {@link #invalidateCaps}. */
    private @Nullable IItemHandler[] faceHandlers;

    /**
     * Whether the covers were suspended by a controller cover on the previous cover tick, i.e. the
     * {@code !aStopped} edge of GT6 {@code CoverRetrieverItem:55-57}
     * ({@code onStoppedUpdate} sets the retriever's pending value to 1 when it resumes). The machine
     * keeps the same field ({@code BasicMachineBlockEntity:1512}).
     */
    private boolean utilityWasStopped;

    /**
     * GT6's {@code aData.mValues[aSide]} for the retriever ({@code CoverRetrieverItem:56,62}): a
     * per-face "run on the next tick even though the 20-tick clock has not come round" flag, set on
     * resume and cleared once the cover has actually moved something.
     *
     * <p>The machine keeps this as a {@code boolean[6]} ({@code BasicMachineBlockEntity:1523}); it is
     * six bits of one byte here because the flag really is one bit per face and a pipe has no reason
     * to allocate an array for it. Not persisted, for the machine's reason: the flag is worth at most
     * one tick and a reload re-arms it on the first tick after the cover stops being suspended.</p>
     */
    private byte retrieverPending;

    public ItemPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.spec = specFromState(state);
        this.inventory = NonNullList.withSize(1, ItemStack.EMPTY);
    }

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        this(GTBlockEntities.ITEM_PIPE.get(), pos, state);
    }

    private static ItemPipeSpec specFromState(BlockState state) {
        if (state.getBlock() instanceof ItemPipeBlock block) return block.spec();
        throw new IllegalStateException("ItemPipeBlockEntity on non-item-pipe block");
    }

    public ItemPipeSpec spec() { return spec; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemPipeBlockEntity be) {
        if (!level.isClientSide) be.tickServer(state);
    }

    private void tickServer(BlockState state) {
        if (level == null) return;

        // Covers first, ahead of every early return below. Two reasons, both GT6's:
        //   * a cover ticks in `onTickPre` (CoverRetrieverItem:60), i.e. before the tile's own tick
        //     body, so a cover is never skipped because the host had nothing to route;
        //   * coverTicks is the retriever's clock and GT6's clock is the global one
        //     (`SERVER_TIME % 20 == 15`, CoverRetrieverItem:61), so it must advance once per server
        //     tick and must NOT sit behind the `% 4` routing gate further down (which would make the
        //     retriever fire every 80 ticks instead of every 20).
        // Zero cost when unused: `hasCovers` is false for every coverless pipe, so this is a single
        // field read and returns — no face is walked, nothing is allocated, nothing is dispatched.
        if (hasCovers) tickCovers();

        // Reset throughput counter every 20 ticks
        if (--ticksUntilReset <= 0) {
            ticksUntilReset = 20;
            transferredThisSecond = 0;
        }

        // Route items every 4 ticks
        if (level.getGameTime() % 4 != 0) return;
        if (isEmpty()) {
            lastReceivedFrom = -1;
            return;
        }

        // Try direct push to adjacent non-pipe inventory first
        if (tryPushToAdjacent()) {
            setChanged();
            return;
        }

        // No direct target — forward items to the best adjacent pipe toward an exit
        if (forwardToBestPipe(state)) {
            setChanged();
        }
    }

    // ── Covers: host API (GT6's item pipe as a cover host) ───────────────────

    @Override
    public ItemStack getCover(Direction side) {
        return covers == null ? ItemStack.EMPTY : covers[side.ordinal()];
    }

    /** Whether this pipe carries at least one cover. The renderer uses it to skip coverless pipes. */
    public boolean hasCovers() {
        return hasCovers;
    }

    @Override
    public PanelCoverRuntime panels() {
        if (panels == null) panels = new PanelCoverRuntime(this);
        return panels;
    }

    /**
     * Attaches one cover to a face — GT6's {@code CoverData} placement path, of which
     * {@code BasicMachineBlockEntity:302-312} is the machine's copy and
     * {@code FluidPipeBlockEntity.attachCover} the pipe's.
     *
     * <p>Neither of the two placement questions GT6's item-side covers ask survives as a test here, and
     * both for a stated reason: the item filter's host rule ({@code CoverFilterItem:135-137}, refuse
     * when the cover's face points at another item pipe) is deliberately not mirrored — see the class
     * javadoc — and the retriever's ({@code CoverRetrieverItem:50}, the host must be a tickable item
     * pipe) is satisfied by construction. So the only question left is the ordinary one every cover
     * gets — is this face free — which {@code BasicMachineBlockEntity:302-312} asks the same way.</p>
     */
    @Override
    public boolean attachCover(Direction side, ItemStack stack) {
        if (stack.isEmpty() || !getCover(side).isEmpty() || !panels().canAttach(side, stack)) return false;
        if (covers == null) covers = emptyCovers();
        covers[side.ordinal()] = stack.copyWithCount(1);
        hasCovers = true;
        panels().attached(side);
        invalidateFaceHandler(side);
        syncCovers();
        return true;
    }

    /**
     * Removes the cover on a face. Mirrors {@code BasicMachineBlockEntity:315-328} and the fluid
     * pipe's copy of it, with {@code panels.afterTick()} in the place of that method's
     * {@code updateCoverSignals()}: the signals a pipe face can emit are the panel ones, and
     * {@code afterTick} is what recomputes and publishes them ({@code PanelCoverRuntime:85-119}).
     */
    @Override
    public ItemStack removeCover(Direction side) {
        if (covers == null) return ItemStack.EMPTY;
        ItemStack cover = covers[side.ordinal()];
        if (cover.isEmpty()) return ItemStack.EMPTY;
        covers[side.ordinal()] = ItemStack.EMPTY;
        hasCovers = false;
        for (ItemStack other : covers) if (!other.isEmpty()) { hasCovers = true; break; }
        invalidateFaceHandler(side);
        panels().beforeTick();
        panels().afterTick();
        syncCovers();
        return cover;
    }

    /**
     * The pipe's drop-on-break for covers, i.e. {@code BasicMachineBlockEntity:1494-1499}
     * ({@code dropContents}'s cover loop) and {@code FluidPipeBlockEntity.dropCovers}. Called from
     * {@link ItemPipeBlock#onRemove}, next to {@link #dropStoredItems()} — that is where this pipe's
     * own removal logic lives, and the block entity is still resolvable there, which is the same call
     * site shape the machine has in {@code BasicMachineBlock:254-262}.
     */
    public void dropCovers() {
        if (covers == null) return;
        for (int i = 0; i < covers.length; i++) {
            BlockContents.drop(this, covers[i]);
            covers[i] = ItemStack.EMPTY;
        }
        hasCovers = false;
        setChanged();
    }

    private static ItemStack[] emptyCovers() {
        ItemStack[] array = new ItemStack[6];
        java.util.Arrays.fill(array, ItemStack.EMPTY);
        return array;
    }

    /** Cover behaviour id of a face; {@code null} when no cover is attached ({@code CoverItems:52}). */
    private String coverIdOf(Direction side) {
        return CoverItems.behavior(getCover(side));
    }

    /** Whether the covers were suspended by a controller cover, without creating the runtime. */
    private boolean panelsStopped() {
        return panels != null && panels.stopped();
    }

    private void syncCovers() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private boolean retrieverPending(int face) {
        return (retrieverPending & (1 << face)) != 0;
    }

    private void setRetrieverPending(int face, boolean value) {
        if (value) retrieverPending |= (byte) (1 << face);
        else retrieverPending &= (byte) ~(1 << face);
    }

    /**
     * One cover pass over all six faces, run from {@link #tickServer} only when {@link #hasCovers}.
     *
     * <p>The order is {@code BasicMachineBlockEntity}'s, and it differs from the fluid pipe's
     * {@code tickCovers} in exactly the two places that the retriever needs: the tick counter advances
     * <em>before</em> the suspend gate, and {@code panels.afterTick()} runs on every path.</p>
     * <ul>
     *   <li>{@code coverTicks++} — {@code BasicMachineBlockEntity:162}. GT6's retriever clock is the
     *       global {@code SERVER_TIME} ({@code CoverRetrieverItem:61}), not the cover's own
     *       {@code aTimer}, so suspending the covers does not stop the clock; the fluid pipe's
     *       pressure valve does use the cover's own {@code aTimer}
     *       ({@code CoverPressureValve:50}), which is why that class freezes it while suspended.</li>
     *   <li>{@code panels.beforeTick()} — {@code :163}; the controller cover's {@code aData.mStopped}
     *       is refreshed inside it ({@code PanelCoverRuntime:70-71}), so it has to run before the gate
     *       below rather than after it.</li>
     *   <li>the suspend gate — {@code tickUtilityCovers:1541}, with the machine's
     *       {@code utilityWasStopped} flag: a retriever that was held back fires immediately on
     *       resume ({@code CoverRetrieverItem:55-57}) instead of waiting out the rest of its cycle.
     *       {@code afterTick} still runs, the way {@code BasicMachineBlockEntity:171} and
     *       {@code :235} call {@code updateCoverSignals()} on every path.</li>
     *   <li>the per-face dispatch — {@code tickUtilityCovers:1545-1568}. Only
     *       {@link CoverUtilityBehaviors#RETRIEVER_ITEM} is dispatched: the item filter has no tick
     *       behaviour ({@code CoverFilterItem} implements {@code interceptItemInsert}/
     *       {@code interceptItemExtract} and nothing periodic), and everything else falls through on
     *       purpose — the pump, conveyor and robot arm need a {@code MachineControl} or the machine's
     *       own slots, and the machine switches, detectors and panels need the
     *       {@code MachineControl} this host does not provide.</li>
     * </ul>
     *
     * <p>The retriever's target is this pipe, taken through its own {@link IItemHandler}: GT6 hands
     * {@code tTarget = aData.mTileEntity.getAdjacentTileEntity(aSide)} ({@code CoverRetrieverItem:67})
     * — the inventory across the cover's face — and {@code CoverUtilityBehaviors.tickRetriever} has
     * always substituted "the host's own slots" for the network side of that walk (its javadoc says
     * so). Here the host's own slots are the pipe's one-slot buffer, so a retriever on an item pipe
     * pulls the filtered items out of the inventory it faces and into the pipe — the reverse of the
     * original's direction, and the same substitution the machine already makes, now that the host
     * really has slots to give.</p>
     */
    private void tickCovers() {
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
                case CoverUtilityBehaviors.RETRIEVER_ITEM -> {
                    ItemStack cover = getCover(side);
                    boolean acted = CoverUtilityBehaviors.tickRetriever(
                            level, worldPosition, side, this,
                            CoverUtilityBehaviors.itemFilter(cover,level.registryAccess()),
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

    /**
     * Two of the three player-facing entry points of a filter cover, copied name for name from
     * {@code BasicMachineBlockEntity:1631-1646} and {@code FluidPipeBlockEntity:382-396} so that every
     * host delegates to the same statics and {@code BasicMachineBlock.use}'s call order can be
     * mirrored in {@code ItemPipeBlock.use}.
     *
     * <p>GT6's right-click on an item filter ({@code CoverFilterItem:88-112}) and on the retriever
     * ({@code CoverRetrieverItem:123-136}): the held stack becomes the filter, once. The fluid half of
     * the machine's method is deliberately absent — an item pipe cannot host a fluid filter
     * ({@code CoverFilterFluid} asks a tank, and {@code FluidPipeBlockEntity} is its host).</p>
     *
     * @return whether a filter was stored
     */
    public boolean clickFilterCover(Direction side, ItemStack held) {
        String id = coverIdOf(side);
        if (id == null || held == null || held.isEmpty()) return false;
        if (CoverUtilityBehaviors.FILTER_ITEM.equals(id) || CoverUtilityBehaviors.RETRIEVER_ITEM.equals(id)) {
            return CoverUtilityBehaviors.setItemFilter(getCover(side),held,level.registryAccess());
        }
        return false;
    }

    /**
     * The screwdriver and soft-hammer halves of the same two covers, copied from
     * {@code BasicMachineBlockEntity:1658-1672}: the screwdriver flips whitelist/blacklist
     * ({@code CoverFilterItem:58-62}, {@code CoverRetrieverItem:94-98}) and the soft hammer clears the
     * filter ({@code CoverFilterItem:63-66}, {@code CoverRetrieverItem:99-102}).
     */
    public boolean configureFilterCover(Direction side, boolean screwdriver, boolean softHammer) {
        String id = coverIdOf(side);
        if (id == null) return false;
        boolean filterish = CoverUtilityBehaviors.FILTER_ITEM.equals(id)
                || CoverUtilityBehaviors.RETRIEVER_ITEM.equals(id);
        if (!filterish || (!screwdriver && !softHammer)) return false;
        if (level != null && level.isClientSide) return true;
        ItemStack stack = getCover(side);
        if (softHammer) CoverUtilityBehaviors.clearFilter(stack);
        else CoverUtilityBehaviors.toggleFilterMode(stack);
        syncCovers();
        return true;
    }

    /**
     * GT6 {@code CoverFilterItem:115-127} asked about a live face: does the item filter on {@code side}
     * let {@code candidate} through? The pipe's copy of
     * {@code BasicMachineBlockEntity.coverFilterPermits:1585-1588}.
     *
     * <p>The §108 rule holds here, verified by reading the implementation rather than assuming it:
     * {@code CoverItems.behavior(ItemStack.EMPTY)} is {@code null} ({@code CoverItems:53}), so
     * {@link CoverUtilityBehaviors#itemFilterPermits}'s first line,
     * {@code FILTER_ITEM.equals(CoverItems.behavior(cover))}, is false for a face with <b>no cover at
     * all</b> and for a face whose cover is not an item filter — both return {@code true}, i.e.
     * permit, without looking at the candidate. An <em>empty filter stack on a real item filter
     * cover</em> is the other case and is not permissive: it returns
     * {@code !MachineCoverSpec.inverted(cover)}, false in the default whitelist mode and true only in
     * the inverted one — GT6 {@code CoverFilterItem:118} {@code return aData.mVisuals[aCoverSide] == 0;}
     * read as "intercept". A suspended cover refuses everything ({@code :117}).</p>
     */
    public boolean coverFilterPermits(Direction side, ItemStack candidate) {
        return CoverUtilityBehaviors.itemFilterPermits(getCover(side),panelsStopped(),candidate,level.registryAccess());
    }

    /**
     * GT6 {@code CoverRetrieverItem:138-139} — {@code return aCoverSide == aSide;}, on both
     * {@code interceptItemInsert} and {@code interceptItemExtract}: the retriever refuses
     * <em>every</em> item insert and extract on the face it owns, so a pipe can neither push into nor
     * pull out of that face; only the retriever itself moves items there. The machine's
     * {@code coverBlocksItemTraffic:1603-1606} is the same predicate.
     */
    public boolean coverBlocksItemTraffic(Direction side) {
        return CoverUtilityBehaviors.RETRIEVER_ITEM.equals(coverIdOf(side));
    }

    /**
     * The two item-cover interception rules of one face as the single question every traffic path
     * asks: may {@code candidate} cross {@code side}? GT6 asks them as two hooks on the same face
     * ({@code CoverFilterItem:115-127}, {@code CoverRetrieverItem:138-139}) and the port composes them
     * here for the same reason {@code BasicMachineBlockEntity} keeps them as two methods and calls
     * both — this is the one place they are combined, and it is the only predicate
     * {@link #tickServer}'s push sites, the capability view and the retriever's own face consult.
     *
     * <p>A pipe without covers answers {@code true} on the first line, so the whole cover system costs
     * one boolean read on the transfer path.</p>
     */
    public boolean coverPermitsItemTraffic(Direction side, ItemStack candidate) {
        if (!hasCovers) return true;
        if (coverBlocksItemTraffic(side)) return false;
        return coverFilterPermits(side, candidate);
    }

    /**
     * Forward items to the best adjacent pipe that leads to a non-pipe inventory.
     * Avoids sending back to the side items were received from.
     */
    private boolean forwardToBestPipe(BlockState state) {
        ItemStack offered = inventory.get(0);
        if (offered.isEmpty()) return false;
        // Find the adjacent pipe with the best (lowest step-distance) path to an exit
        java.util.Map<ItemPipeBlockEntity, Direction> candidates = new java.util.LinkedHashMap<>();

        for (Direction side : Direction.values()) {
            if (!canEmitTo(side)) continue;
            if (lastReceivedFrom >= 0 && lastReceivedFrom == side.ordinal()) continue;
            if (!state.getValue(ItemPipeBlock.propFor(side)) || !coverPermitsItemTraffic(side, offered)) continue;

            BlockPos neighborPos = worldPosition.relative(side);
            if (!level.hasChunkAt(neighborPos)) continue;
            BlockEntity be = level.getBlockEntity(neighborPos);
            if (!(be instanceof ItemPipeBlockEntity neighborPipe)) continue;
            if (!neighborPipe.canAcceptFrom(side.getOpposite())
                    || !neighborPipe.getBlockState().getValue(ItemPipeBlock.propFor(side.getOpposite()))
                    || !neighborPipe.coverPermitsItemTraffic(side.getOpposite(),offered)) continue;

            candidates.put(neighborPipe, side);
        }
        var exit = findBestExit(candidates.keySet(), offered);
        return exit != null && pushItemsToPipe(exit.firstHop(), candidates.get(exit.firstHop()));
    }

    /** Shared minimum-weight search over currently loaded pipes and accepting inventory faces. */
    private com.gregtech.gregtech.content.transport.WeightedItemPipeRoutes.Exit<ItemPipeBlockEntity> findBestExit(
            Iterable<ItemPipeBlockEntity> starts, ItemStack offered) {
        return com.gregtech.gregtech.content.transport.WeightedItemPipeRoutes.bestExit(starts,
                pipe -> pipe.spec.stepSize(), pipe -> {
                    java.util.List<ItemPipeBlockEntity> adjacent = new java.util.ArrayList<>();
                    BlockState current = pipe.getBlockState();
                    for (Direction face : Direction.values()) {
                        if (!current.getValue(ItemPipeBlock.propFor(face)) || !pipe.canEmitTo(face)
                                || !pipe.coverPermitsItemTraffic(face,offered)) continue;
                        BlockPos next = pipe.worldPosition.relative(face);
                        if (!level.hasChunkAt(next)) continue;
                        if (level.getBlockEntity(next) instanceof ItemPipeBlockEntity neighbor && neighbor != this
                                && neighbor.getBlockState().getValue(ItemPipeBlock.propFor(face.getOpposite()))
                                && neighbor.canAcceptFrom(face.getOpposite())
                                && neighbor.coverPermitsItemTraffic(face.getOpposite(),offered)) adjacent.add(neighbor);
                    }
                    return adjacent;
                }, pipe -> pipe.hasAdjacentNonPipeTarget(offered),
                com.gregtech.gregtech.content.transport.WeightedItemPipeRoutes.MAX_VISITED_PIPES);
    }

    private IItemHandler itemHandler(BlockEntity entity,Direction face) {
        if (entity == null) return null;
        return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,entity.getBlockPos(),face);
    }

    private IItemHandler itemHandlerAt(BlockPos pos,Direction face) {
        return level != null && level.hasChunkAt(pos) ? itemHandler(level.getBlockEntity(pos),face) : null;
    }

    /** Presence alone is insufficient: a full or filtered endpoint is not a usable exit. */
    private boolean hasAdjacentNonPipeTarget(ItemStack offered) {
        BlockState state = getBlockState();
        for (Direction side : Direction.values()) {
            if (!canEmitTo(side) || lastReceivedFrom == side.ordinal()
                    || !state.getValue(ItemPipeBlock.propFor(side))
                    || !coverPermitsItemTraffic(side,offered)) continue;
            BlockPos next = worldPosition.relative(side);
            if (!level.hasChunkAt(next)) continue;
            BlockEntity entity = level.getBlockEntity(next);
            if (entity instanceof ItemPipeBlockEntity) continue;
            if (com.gregtech.gregtech.content.transport.ItemPipeTransferAdapter.canAccept(offered,
                    () -> itemHandlerAt(next, side.getOpposite()))) return true;
        }
        return false;
    }

    /** Push items from this pipe's inventory into an adjacent pipe. */
    private boolean pushItemsToPipe(ItemPipeBlockEntity target, Direction side) {
        IItemHandler receiving = itemHandler(target,side.getOpposite());
        if (receiving == null) return false;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.get(slot);
            if (stack.isEmpty()) continue;

            ItemStack toSend = stack.copy();
            // §112 (the pipe-to-pipe push site): the cover on the face the items leave through gates
            // them exactly like the receiver's own capability does on the other side. Without this the
            // sender's filter would only stop arrivals and never departures — the half §111 had to add
            // to `distribute` after the fact.
            if (!coverPermitsItemTraffic(side, toSend)) continue;
            int accepted = com.gregtech.gregtech.content.transport.ItemPipeTransferAdapter.transfer(toSend,
                    () -> itemHandlerAt(target.worldPosition,side.getOpposite())).accepted();
            if (accepted <= 0) continue;
            stack.shrink(accepted);
            transferredThisSecond += accepted;
            if (stack.isEmpty()) {
                inventory.set(slot, ItemStack.EMPTY);
            }
            return true;
        }
        return false;
    }

    /** Try to push one item stack from this pipe's inventory to an adjacent non-pipe inventory. */
    boolean tryPushToAdjacent() {
        if (level == null) return false;

        // Random start offset for round-robin distribution
        int start = level.random.nextInt(6);
        for (int i = 0; i < 6; i++) {
            Direction side = Direction.values()[(start + i) % 6];
            if (!canEmitTo(side) || !getBlockState().getValue(ItemPipeBlock.propFor(side))) continue;
            // Don't push back to where items came from
            if (lastReceivedFrom >= 0 && lastReceivedFrom == side.ordinal()) continue;

            // Don't push to other item pipes (handled by forwardToBestPipe)
            BlockPos targetPos = worldPosition.relative(side);
            if (!level.hasChunkAt(targetPos)) continue;
            BlockEntity be = level.getBlockEntity(targetPos);
            if (be instanceof ItemPipeBlockEntity) continue;

            if (be == null) continue;
            IItemHandler cap=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,be.getBlockPos(),side.getOpposite());
            if (cap==null) continue;
            IItemHandler target=cap;
            if (target == null) continue;

            // Try to move one item stack from inventory to target
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.get(slot);
                if (stack.isEmpty()) continue;

                // Simulate first
                ItemStack toSend = stack.copy();
                // §112 (the machine/destination push site): the same sending-face rule as
                // `pushItemsToPipe` — the face's own cover decides what leaves through it. Only a
                // filter can differ per stack, so the test sits inside the slot loop and the loop
                // keeps trying the other slots.
                if (!coverPermitsItemTraffic(side, toSend)) continue;
                int accepted = com.gregtech.gregtech.content.transport.ItemPipeTransferAdapter.transfer(toSend,
                        () -> itemHandlerAt(targetPos,side.getOpposite())).accepted();
                if (accepted <= 0) continue;
                stack.shrink(accepted);
                transferredThisSecond += accepted;
                if (stack.isEmpty()) {
                    inventory.set(slot, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    private boolean canEmitTo(Direction side) {
        return (disabledOutputs & (1 << side.ordinal())) == 0;
    }

    private boolean canAcceptFrom(Direction side) {
        return (disabledInputs & (1 << side.ordinal())) == 0
                && (lastReceivedFrom < 0 || lastReceivedFrom == side.ordinal());
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
                int moved = com.gregtech.gregtech.content.transport.ItemPipeTransferAdapter.transfer(stack.copy(),
                        () -> itemHandlerAt(targetPos, side.getOpposite())).accepted();
                if (moved > 0) {
                    stack.shrink(moved);
                    if (stack.isEmpty()) inventory.set(slot, ItemStack.EMPTY);
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
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.set(slot, ItemStack.EMPTY);
        }
    }

    /** Auto-connect on placement: connects to clicked face + scans all 6 sides for adjacent pipes. */
    public void autoConnectOnPlace(@Nullable Direction clickedFace) {
        if (level == null || level.isClientSide) return;

        // First, connect to the clicked face (existing behavior)
        if (clickedFace != null) {
            Direction toNeighbor = clickedFace.getOpposite();
            BlockPos neighborPos = worldPosition.relative(toNeighbor);
            BlockEntity be = level.getBlockEntity(neighborPos);
            if (be instanceof ItemPipeBlockEntity neighborPipe) {
                BlockState state = getBlockState();
                BlockState newState = state.setValue(ItemPipeBlock.propFor(toNeighbor), true);
                level.setBlockAndUpdate(worldPosition, newState);

                BlockState neighborState = neighborPipe.getBlockState();
                if (!neighborState.getValue(ItemPipeBlock.propFor(clickedFace))) {
                    level.setBlockAndUpdate(neighborPos,
                            neighborState.setValue(ItemPipeBlock.propFor(clickedFace), true));
                }
            }
        }

        // Then scan all 6 sides for adjacent pipes already connected toward us
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(neighborPos);
            if (!(be instanceof ItemPipeBlockEntity neighborPipe)) continue;

            BlockState neighborState = neighborPipe.getBlockState();
            if (neighborState.getValue(ItemPipeBlock.propFor(dir.getOpposite()))) {
                BlockState state = getBlockState();
                if (!state.getValue(ItemPipeBlock.propFor(dir))) {
                    level.setBlockAndUpdate(worldPosition, state.setValue(ItemPipeBlock.propFor(dir), true));
                }
            }
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
    }

    // === IItemHandler ===

    @Override
    public int getSlots() { return inventory.size(); }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot >= 0 && slot < inventory.size() ? inventory.get(slot) : ItemStack.EMPTY;
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        // Find first empty or matching slot
        int targetSlot = -1;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack existing = inventory.get(i);
            if (existing.isEmpty()) {
                if (targetSlot < 0) targetSlot = i;
            } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                targetSlot = i;
                break;
            }
        }
        if (targetSlot < 0) return stack;

        ItemStack existing = inventory.get(targetSlot);
        int slotLimit = getSlotLimit(targetSlot);
        int limit = Math.min(slotLimit, existing.isEmpty() ? slotLimit : existing.getMaxStackSize());
        int insertable = Math.min(stack.getCount(), limit - existing.getCount());
        if (insertable <= 0) return stack;

        if (!simulate) {
            if (existing.isEmpty()) {
                ItemStack toInsert = stack.copy();
                toInsert.setCount(insertable);
                inventory.set(targetSlot, toInsert);
            } else {
                existing.grow(insertable);
            }
            setChanged();
        }

        if (insertable >= stack.getCount()) return ItemStack.EMPTY;
        ItemStack remainder = stack.copy();
        remainder.shrink(insertable);
        return remainder;
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
            if (stack.isEmpty()) inventory.set(slot, ItemStack.EMPTY);
            setChanged();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) { return spec.invSize(); }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) { return true; }

    // === Capability ===

    public IItemHandler capabilityHandler(@Nullable Direction side) {
        if(side!=null)return hasCovers&&isItemCoverFace(side)?faceHandler(side):sideHandler(side);return this;
    }

    /**
     * The pre-existing per-side view, unchanged: one {@link SideAwareItemHandler} per face, which
     * records {@code lastReceivedFrom} on a successful insert.
     *
     * <p><b>Deviation from the fluid-pipe template.</b> {@code FluidPipeBlockEntity:811-812} falls back
     * to {@code handler.cast()} for an ordinary face, because on that class the no-face handler and the
     * per-face one are the same object. Here they are not: {@code handler} is the unsided view, and a
     * <em>sided</em> query has always been answered by this wrapper since §97/§99 (the capability
     * handle-stability work). Returning {@code handler.cast()} from the new branch would silently drop
     * the backflow bookkeeping for every covered pipe, so the "pre-existing handler" of requirement
     * §112.3 is this method.</p>
     */
    private IItemHandler sideHandler(Direction side) {
        return sideCaps.computeIfAbsent(side,
                SideAwareItemHandler::new);
    }

    private final java.util.Map<Direction,IItemHandler> sideCaps =
            new java.util.EnumMap<>(Direction.class);

    /** Whether this face's cover gates item traffic — the item filter or the item retriever. */
    private boolean isItemCoverFace(Direction side) {
        String id = coverIdOf(side);
        return CoverUtilityBehaviors.FILTER_ITEM.equals(id)
                || CoverUtilityBehaviors.RETRIEVER_ITEM.equals(id);
    }

    /** The cached face view of a covered face, built on first request and dropped on any cover change. */
    @SuppressWarnings("unchecked")
    private IItemHandler faceHandler(Direction side) {
        if(faceHandlers==null)faceHandlers=new IItemHandler[6];var cached=faceHandlers[side.ordinal()];if(cached==null)faceHandlers[side.ordinal()]=cached=new FaceItemHandler(side);return cached;
    }

    private void invalidateFaceHandler(Direction side) {
        if(faceHandlers!=null)faceHandlers[side.ordinal()]=null;if(level!=null)level.invalidateCapabilities(worldPosition);
    }

    public void invalidateItemCapabilities() {
        sideCaps.clear();faceHandlers=null;if(level!=null)level.invalidateCapabilities(worldPosition);
    }

    /** Wraps this pipe's IItemHandler to record which side items entered from on insertItem.
     *  Forge's IItemHandler.insertItem() doesn't provide direction info, so external callers
     *  (hoppers, other inventories pushing into pipes) can't set lastReceivedFrom.
     *  This wrapper intercepts insertItem to call setReceivedFrom before delegating. */
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
            ItemStack result = ItemPipeBlockEntity.this.insertItem(slot, stack, simulate);
            if (!simulate && result.getCount() < stack.getCount()) {
                setReceivedFrom(sideOrdinal);
            }
            return result;
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

    // ── The face-aware view the item covers need ─────────────────────────────

    /**
     * A pipe's {@code insertItem}/{@code extractItem} take no face, and {@link #getCapability} hands
     * out a per-side view for every side, so the face has to be carried <em>by the handler</em>. This is
     * that handler — {@link SideAwareItemHandler} plus the two cover rules — and it is built only where
     * an item filter or a retriever actually sits ({@link #getCapability}), never for a coverless pipe.
     * It is the item-side counterpart of {@code FluidPipeBlockEntity.FaceFluidHandler:1084-1128}.
     *
     * <p>Everything except the two intercepts delegates straight through, by inheritance: that is the
     * pipe's own answer for {@code getSlots}, {@code getStackInSlot}, {@code getSlotLimit} and
     * {@code isItemValid} <em>and</em> the pre-existing {@code lastReceivedFrom} bookkeeping on
     * insert, so a mod that queries a covered face still sees exactly the pipe it saw before, with one
     * rule added. GT6 has no interception hook for item validity either — only
     * {@code interceptItemInsert} and {@code interceptItemExtract} — so {@code isItemValid} is left
     * alone on purpose.</p>
     *
     * <p><b>Not intercepted:</b> the player's own {@code use}/{@code printDebug} paths, which never go
     * through a face's handler; and the retriever's own movement, which {@link #tickCovers} hands this
     * block entity <em>directly</em> as {@code CoverUtilityBehaviors.tickRetriever}'s {@code target} and
     * not the face view, so the pipe's own insert is not refused by the blanket rule the retriever
     * applies to its face. GT6 draws that line the same way from the other end: its retriever inserts
     * into the inventory across the face ({@code CoverRetrieverItem:67,72}) and only extracts through
     * the pipe's sides, and the port's substitution of "the host's own slots" for that target has always
     * bypassed the host's own intercepts — the machine's retriever does too
     * ({@code BasicMachineBlockEntity:1553-1557}).</p>
     */
    private class FaceItemHandler extends SideAwareItemHandler {
        private final Direction side;

        FaceItemHandler(Direction side) {
            super(side);
            this.side = side;
        }

        /**
         * GT6 {@code CoverFilterItem:115-120} {@code interceptItemInsert} and
         * {@code CoverRetrieverItem:138}: refuse, never partially insert.
         */
        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!coverPermitsItemTraffic(side, stack)) return stack;
            return super.insertItem(slot, stack, simulate);
        }

        /**
         * GT6 {@code CoverFilterItem:122-127} {@code interceptItemExtract} and
         * {@code CoverRetrieverItem:139}, asked with the stack the slot holds — which is the stack the
         * original is handed ({@code interceptItemExtract(..., ItemStack aStack, ...)}).
         */
        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack present = getStackInSlot(slot);
            if (present.isEmpty() || !coverPermitsItemTraffic(side, present)) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }
    }

    // === NBT ===

    /** 1.21's ordinary stack codec accepts count <=99; large GT pipe buffers retain an explicit count. */
    private void saveInventory(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        NonNullList<ItemStack> encoded=NonNullList.withSize(inventory.size(),ItemStack.EMPTY);
        for(int i=0;i<inventory.size();i++){var stack=inventory.get(i);encoded.set(i,stack.getCount()>99?stack.copyWithCount(1):stack);}
        ContainerHelper.saveAllItems(tag,encoded,lookup);
        var items=tag.getList("Items",net.minecraft.nbt.Tag.TAG_COMPOUND);
        for(int i=0;i<items.size();i++){var row=items.getCompound(i);int slot=row.getByte("Slot")&255;if(slot<inventory.size()&&inventory.get(slot).getCount()>99)row.putInt("gt.pipe_count",inventory.get(slot).getCount());}
    }
    private void loadInventory(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        ContainerHelper.loadAllItems(tag,inventory,lookup);
        var items=tag.getList("Items",net.minecraft.nbt.Tag.TAG_COMPOUND);
        for(int i=0;i<items.size();i++){var row=items.getCompound(i);int slot=row.getByte("Slot")&255;if(slot<inventory.size()&&row.contains("gt.pipe_count")&&!inventory.get(slot).isEmpty())inventory.get(slot).setCount(Math.max(1,Math.min(spec.invSize(),row.getInt("gt.pipe_count"))));}
    }

    @Override
    protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        lastReceivedFrom = tag.contains(NBT_LAST_RECEIVED) ? tag.getByte(NBT_LAST_RECEIVED) : -1;
        disabledInputs = tag.contains(NBT_DISABLED_INPUTS) ? tag.getByte(NBT_DISABLED_INPUTS) : 0;
        disabledOutputs = tag.contains(NBT_DISABLED_OUTPUTS) ? tag.getByte(NBT_DISABLED_OUTPUTS) : 0;
        transferredThisSecond = tag.contains(NBT_TRANSFERRED) ? tag.getLong(NBT_TRANSFERRED) : 0;
        if (tag.contains(NBT_ITEMS)) {
            loadInventory(tag.getCompound(NBT_ITEMS),lookup);
        }
        loadCovers(tag,lookup);
    }

    /**
     * One cover per face under the machine's own keys, written only for non-empty faces — the same
     * shape as {@code BasicMachineBlockEntity.saveCovers:1296-1302} and
     * {@code FluidPipeBlockEntity.saveCovers}, and skipped entirely while no cover was ever attached
     * (the array is still null), so a coverless pipe writes nothing.
     */
    private void saveCovers(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        if (covers == null) return;
        for (int i = 0; i < covers.length; i++) {
            if (!covers[i].isEmpty()) tag.put(NBT_COVER_PREFIX + i, covers[i].save(lookup));
        }
    }

    /**
     * {@code BasicMachineBlockEntity.loadCovers:1304-1310}, with the fluid pipe's one difference: a
     * tag that names no cover <em>clears</em> the faces, so a cover removed on the server disappears on
     * the client through {@link #handleUpdateTag} instead of lingering. A tag without any cover key and
     * a pipe that never had one stay allocation-free.
     *
     * <p>{@link #hasCovers} is recomputed here and not only in the tag-writing direction: it is the
     * only thing the tick pass and {@link #getCapability} test, so a load that brings covers back
     * re-arms both.</p>
     */
    private void loadCovers(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        boolean present = false;
        for (int i = 0; i < 6 && !present; i++) present = tag.contains(NBT_COVER_PREFIX + i);
        if (!present) {
            if (covers != null) java.util.Arrays.fill(covers, ItemStack.EMPTY);
            hasCovers = false;
            return;
        }
        if (covers == null) covers = emptyCovers();
        boolean any = false;
        for (int i = 0; i < 6; i++) {
            covers[i] = tag.contains(NBT_COVER_PREFIX + i)
                    ? ItemStack.parseOptional(lookup,tag.getCompound(NBT_COVER_PREFIX+i)) : ItemStack.EMPTY;
            any |= !covers[i].isEmpty();
        }
        hasCovers = any;
        // GT6 re-reads each cover's stored values on load; only worth doing when one is there.
        if (any) panels().loaded();
    }

    @Override
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.putByte(NBT_LAST_RECEIVED, lastReceivedFrom);
        tag.putByte(NBT_DISABLED_INPUTS, disabledInputs);
        tag.putByte(NBT_DISABLED_OUTPUTS, disabledOutputs);
        tag.putLong(NBT_TRANSFERRED, transferredThisSecond);
        CompoundTag itemsTag = new CompoundTag();
        saveInventory(itemsTag,lookup);
        tag.put(NBT_ITEMS, itemsTag);
        saveCovers(tag,lookup);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        CompoundTag tag = super.getUpdateTag(lookup);
        tag.putByte(NBT_LAST_RECEIVED, lastReceivedFrom);
        tag.putByte(NBT_DISABLED_INPUTS, disabledInputs);
        tag.putByte(NBT_DISABLED_OUTPUTS, disabledOutputs);
        CompoundTag itemsTag = new CompoundTag();
        saveInventory(itemsTag,lookup);
        tag.put(NBT_ITEMS, itemsTag);
        saveCovers(tag,lookup);
        return tag;
    }

    /**
     * The pipe had no update packet before this batch, so covers would never have reached the client
     * renderer; {@code BasicMachineBlockEntity:1326-1335} is the machine's pair of the same two
     * methods, and the payload is the update tag above. Same pair, same reason, as
     * {@code FluidPipeBlockEntity.getUpdatePacket}.
     */
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,net.minecraft.core.HolderLookup.Provider lookup) {
        if (pkt.getTag() != null) loadCovers(pkt.getTag(),lookup);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.handleUpdateTag(tag,lookup);
        if (tag.contains(NBT_LAST_RECEIVED)) lastReceivedFrom = tag.getByte(NBT_LAST_RECEIVED);
        if (tag.contains(NBT_DISABLED_INPUTS)) disabledInputs = tag.getByte(NBT_DISABLED_INPUTS);
        if (tag.contains(NBT_DISABLED_OUTPUTS)) disabledOutputs = tag.getByte(NBT_DISABLED_OUTPUTS);
        if (tag.contains(NBT_TRANSFERRED)) transferredThisSecond = tag.getLong(NBT_TRANSFERRED);
        if (tag.contains(NBT_ITEMS)) {
            loadInventory(tag.getCompound(NBT_ITEMS),lookup);
        }
        loadCovers(tag,lookup);
    }

    public long getTransferredThisSecond() { return transferredThisSecond; }
    public byte getDisabledInputs() { return disabledInputs; }
    public byte getDisabledOutputs() { return disabledOutputs; }

    /** Record which side items were received from (called externally after insert). */
    public void setReceivedFrom(byte side) {
        this.lastReceivedFrom = side;
    }

    public void setReceivedFrom(@Nullable Direction side) {
        this.lastReceivedFrom = side == null ? -1 : (byte) side.ordinal();
    }

    @Override
    public void adjacentInventoryUpdated(Direction side, BlockPos sourcePos) {
        // GT6: wake up the pipe when an adjacent inventory changes
        setChanged();
    }
    // ── Client model data (dynamic pipe model neighbor sizes) ───────────────

    @Override
    public net.neoforged.neoforge.client.model.data.ModelData getModelData() {
        float[] halves = new float[6];
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            halves[d.ordinal()] = level == null
                    ? (float) com.gregtech.gregtech.api.machine.PipeGeometry.NONE
                    : (float) com.gregtech.gregtech.api.machine.PipeGeometry.pipeHalfOf(
                            level.getBlockState(worldPosition.relative(d)));
        }
        return net.neoforged.neoforge.client.model.data.ModelData.builder()
                .with(com.gregtech.gregtech.api.machine.PipeModelData.NEIGHBOR_HALVES, halves)
                .build();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            com.gregtech.gregtech.api.machine.PipeModelData.refreshCrossChunkNeighbors(level, worldPosition);
        }
    }

}
