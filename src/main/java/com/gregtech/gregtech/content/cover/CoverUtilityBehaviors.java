package com.gregtech.gregtech.content.cover;

import com.gregtech.gregtech.api.fluid.FluidHazards;
import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.content.logistics.FilterRules;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.util.GTEntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * The second batch of GUI-less covers: everything GT6 attaches to a face whose whole behaviour is a
 * rule plus one tick or one interaction, and which the port therefore implements as static functions
 * instead of a panel.
 *
 * <p>The split is the same one {@link CoverAttachmentBehaviors} uses: this class owns the rules and
 * the entry points, {@code BasicMachineBlockEntity.tickUtilityCovers} owns the dispatch. Every method
 * here is either a pure function (assertable on its own, including its boundary and its negative
 * case) or a tick/interaction entry that touches the objects GT6's own code touches.</p>
 *
 * <h2>Which items these are, and which of them the port already recognises</h2>
 *
 * <table>
 *   <caption>Behaviour id, GT6 registration site, port item</caption>
 *   <tr><th>behaviour id</th><th>GT6 class</th><th>GT6 registration</th></tr>
 *   <tr><td>{@value #CRAFTING_TABLE}</td><td>{@code CoverCrafting}</td>
 *       <td>{@code MultiItemTechnological:60}</td></tr>
 *   <tr><td>{@value #RETRIEVER_ITEM}</td><td>{@code CoverRetrieverItem}</td>
 *       <td>{@code MultiItemTechnological:90}</td></tr>
 *   <tr><td>{@value #FILTER_ITEM}</td><td>{@code CoverFilterItem}</td>
 *       <td>{@code MultiItemTechnological:82}</td></tr>
 *   <tr><td>{@value #FILTER_FLUID}</td><td>{@code CoverFilterFluid}</td>
 *       <td>{@code MultiItemTechnological:83}</td></tr>
 *   <tr><td>{@value #PRESSURE_VALVE}</td><td>{@code CoverPressureValve}</td>
 *       <td>{@code MultiItemTechnological:176}</td></tr>
 *   <tr><td>{@value #ASPHALT_PANEL}</td><td>{@code CoverAsphalt}</td>
 *       <td>{@code Loader_MultiTileEntities:2054}</td></tr>
 *   <tr><td>{@value #BLANK_COVER}</td><td>{@code CoverTextureMulti}</td>
 *       <td>{@code MultiItemTechnological:59}</td></tr>
 *   <tr><td>{@value #WARNING_COVER}</td><td>{@code CoverTextureMulti}</td>
 *       <td>{@code MultiItemTechnological:87}</td></tr>
 *   <tr><td>{@link CoverItems#REDSTONE_TORCH}</td><td>{@code CoverRedstoneTorch}</td>
 *       <td>{@code GT_API:799-801}</td></tr>
 *   <tr><td>{@link CoverItems#REDSTONE_REPEATER}</td><td>{@code CoverRedstoneRepeater}</td>
 *       <td>{@code GT_API:802}</td></tr>
 * </table>
 *
 * <p>{@link CoverItems#behavior} recognises all of these except two, and that is a defect of that
 * one method rather than of this class: its {@code portCoverId} guard {@code CoverItems:75-82} lists
 * {@code drain}, {@code air_vent}, {@code item_filter}, {@code fluid_filter} and the
 * {@code *_cover} / {@code *_selector} / {@code machine_switch} / detector families, but
 * <b>{@value #PRESSURE_VALVE} and {@value #ASPHALT_PANEL} match none of those tests</b>. A stack of
 * either item therefore reports {@code null} from {@link CoverItems#behavior}, so a face can hold it
 * but {@code BasicMachineBlockEntity.getCoverId} never dispatches it. The two-line fix belongs in
 * {@code CoverItems}, which this batch may not touch; it is reported to the integrator and pinned by
 * {@code CoverBehaviorTests.pressureValveAndAsphaltStillNeedTheirCoverItemsEntry}.</p>
 *
 * <h2>The filters: what GT6 matches, and what the port matches instead</h2>
 *
 * <p>GT6's item filter does not consult the ore dictionary at all. Its whole rule is
 * {@code ST.equal(filter, candidate, T)} ({@code CoverFilterItem:119,126}) with {@code T} = "ignore
 * NBT" — and {@code ST.equal(..., aIgnoreNBT)} is
 * {@code item_(a) == item_(b) && equal(meta_(a), meta_(b))} ({@code ST.java:94}), i.e. <em>item plus
 * metadata, NBT ignored</em>, where {@code ST.equal(long,long)} also lets the wildcard metadata
 * {@code W} match anything ({@code ST.java:118}). The fluid filter is the same shape with
 * {@code FL.equal(a, b, T)}, i.e. {@code UT.Code.equal} — <em>same fluid, NBT ignored</em>
 * ({@code UT.java:113}), driven from {@code CoverFilterFluid:121,128}.</p>
 *
 * <p>The port has neither metadata nor a wildcard meta value, so the equivalence is:</p>
 *
 * <ul>
 *   <li><b>item + meta</b> becomes the registry item plus the port's NBT wildcard, which the
 *       logistics filter block already established: {@code FilterRules.itemMatches}
 *       ({@code FilterRules:9-12}) accepts when the item is the same <em>and</em> the stored template
 *       carries no tag, and demands equal tags otherwise. That is stricter than GT6 for a template
 *       that carries NBT: GT6 ignores the tag on both sides, the port compares it. It is the same
 *       answer for every filter GT6 itself can store, because the cover saves
 *       {@code ST.make(ST.item_(tStack), 1, ST.meta_(tStack))} — item and metadata only, never NBT
 *       ({@code CoverFilterItem:95,100,104}).</li>
 *   <li><b>the ore dictionary</b> is not used by these two covers at all, so nothing is lost here.
 *       The one place a tag rule does exist in the port is the logistics <em>filter block</em>'s
 *       prefix mode, which reads Forge item tags ({@code FilterRules:24-25}); it is deliberately not
 *       reused, because GT6's covers never had a prefix or tag mode. The only tag-shaped thing a
 *       filter cover can hold is what the player right-clicks into it.</li>
 *   <li><b>the wildcard metadata {@code W}</b> has no 1.20.1 counterpart and no GT6 counterpart in
 *       the covers either — {@code CoverFilterItem:99-107} stores {@code W} only to mark "the same
 *       item, all variants", which is exactly the untagged-template rule above.</li>
 * </ul>
 *
 * <h2>Where the mode (whitelist / blacklist) lives</h2>
 *
 * <p>GT6 keeps it in {@code aData.mVisuals[aSide]} — 0 is the whitelist ("Normal Filter"), 1 the
 * blacklist ("Inverted Filter"), toggled by a screwdriver ({@code CoverFilterItem:58-62},
 * {@code CoverFilterFluid:62-66}, and for the retriever {@code CoverRetrieverItem:94-98}). The port
 * already has one boolean per cover for exactly this, {@code gt.cover.inverted}, read by
 * {@link MachineCoverSpec#inverted(ItemStack)} and written by the same screwdriver path as the
 * machine switches ({@code BasicMachineBlockEntity:554-562}); this class reuses it instead of
 * inventing a second flag.</p>
 *
 * <h2>The pressure valve, and the two hosts it is ticked from</h2>
 *
 * <p>GT6's valve is a <em>pipe</em> cover: {@code interceptCoverPlacement} refuses any host that is
 * not a {@code MultiTileEntityPipeFluid} with exactly one tank ({@code CoverPressureValve:44}), and
 * the behaviour dereferences {@code ((MultiTileEntityPipeFluid)aData.mTileEntity).mTanks[0]}
 * ({@code CoverPressureValve:51}). The port now has that host: {@code FluidPipeBlockEntity} is a
 * cover host, it calls {@link #valveCanAttachTo} — the original's placement rule kept here as a
 * predicate, written with a pipe-host caller in mind — from its {@code attachCover}, and it ticks a
 * pipe-hosted valve with its own {@code mTanks[0]} equivalent, the pipe's single tank. The machine
 * host keeps its earlier, wider substitution: {@code BasicMachineBlockEntity.tickUtilityCovers}
 * ticks a machine-hosted valve with the machine's own output tank ({@code :1561-1565}), because GT6
 * has no machine-hosted valve at all. Both callers pass the same {@link #tickPressureValve}; the
 * only difference between them is which tank that call releases.</p>
 *
 * <h2>Throttling uses the owner's cover tick counter, like the previous batch</h2>
 *
 * <p>{@link #retrieverDue} ported {@code SERVER_TIME % 20 == 15} ({@code CoverRetrieverItem:61}) and
 * {@link #tickPressureValve} ported {@code aTimer > 2} ({@code CoverPressureValve:50}). Both read
 * the counter the caller hands them, which is {@code BasicMachineBlockEntity.coverTicks}: frozen
 * {@code level.getGameTime()} cannot be observed from inside a GameTest body. See
 * {@link CoverAttachmentBehaviors} for the full argument.</p>
 */
public final class CoverUtilityBehaviors {

    // ── Behaviour ids (the registry paths of the port's own cover items) ─────────────

    /** Behaviour id of {@code gregtech:crafting_table_cover} — GT6 {@code IL.Cover_Crafting}. */
    public static final String CRAFTING_TABLE = "crafting_table_cover";

    /** Behaviour id of {@code gregtech:item_retriever_cover} — GT6 {@code IL.Cover_Retriever_Item}. */
    public static final String RETRIEVER_ITEM = "item_retriever_cover";

    /** Behaviour id of {@code gregtech:item_filter} — GT6 {@code IL.Cover_Filter_Item}. */
    public static final String FILTER_ITEM = "item_filter";

    /** Behaviour id of {@code gregtech:fluid_filter} — GT6 {@code IL.Cover_Filter_Fluid}. */
    public static final String FILTER_FLUID = "fluid_filter";

    /** Behaviour id of {@code gregtech:pressure_value} — GT6 {@code IL.Cover_Pressure_Valve}. */
    public static final String PRESSURE_VALVE = "pressure_value";

    /** Behaviour id of {@code gregtech:panel_asphalt} — GT6's "Asphalt Panel" cover. */
    public static final String ASPHALT_PANEL = "panel_asphalt";

    /** Behaviour id of {@code gregtech:blank_cover} — GT6 {@code IL.Cover_Blank}. */
    public static final String BLANK_COVER = "blank_cover";

    /** Behaviour id of {@code gregtech:warning_cover} — GT6 {@code IL.Cover_Warning}. */
    public static final String WARNING_COVER = "warning_cover";

    // ── NBT keys, spelled exactly as GT6 spells them ────────────────────────────────

    /** GT6 {@code CoverFilterItem:47,65,95} / {@code CoverRetrieverItem:65}: the filtered item. */
    public static final String FILTER_ITEM_KEY = "gt.filter.item";

    /** GT6 {@code CoverFilterFluid:51,68,95}: the filtered fluid. */
    public static final String FILTER_FLUID_KEY = "gt.filter.fluid";

    /** Where GT6 keeps {@code aData.mVisuals}: the chisel-selected design of a texture cover. */
    public static final String VISUAL_KEY = "gt.cover.visual";

    // ── CoverCrafting ──────────────────────────────────────────────────────────────

    /**
     * GT6 {@code CoverCrafting:49}: {@code new S2DPacketOpenWindow(id, 1, "Crafting", 9, T)} — the
     * window type is the vanilla workbench and {@code 9} is its nine slots, i.e. the 3×3 grid of
     * {@code ContainerWorkbench} opened on line {@code :50}.
     */
    public static final int CRAFTING_GRID_SLOTS = 9;

    /** GT6 {@code CoverCrafting:57} {@code isSealable} — the crafting cover never seals its face. */
    public static boolean craftingCoverSealsFace() {
        return false;
    }

    /** GT6 {@code CoverCrafting:58} {@code isDecorative}. */
    public static boolean craftingCoverIsDecorative() {
        return true;
    }

    /** GT6 {@code CoverCrafting:59} {@code showsConnectorFront} — no pipe/wire stub is drawn. */
    public static boolean craftingCoverShowsConnectorFront() {
        return false;
    }

    /**
     * GT6 {@code CoverCrafting:46-55}: right-clicking the cover opens a workbench.
     *
     * <p>The original guards the container on {@code aPlayer instanceof EntityPlayerMP}
     * ({@code :47}) and then returns {@code T} unconditionally ({@code :54}), so the click is
     * consumed whether or not a GUI opened; that shape is kept. The 1.20.1 counterpart of
     * {@code S2DPacketOpenWindow(..., "Crafting", 9, T)} plus the {@code ContainerWorkbench} built
     * around the tile's coordinates ({@code :49-52}) is vanilla's own {@link CraftingMenu} over a
     * {@link ContainerLevelAccess} on the same position, opened through
     * {@link ServerPlayer#openMenu(net.minecraft.world.MenuProvider)} — no new menu type, no new
     * packet, no client class.</p>
     *
     * <p><b>Difference:</b> the original overrides {@code canInteractWith} to {@code T}
     * ({@code CoverCrafting:50}), so its workbench stays open at any distance. {@link CraftingMenu}
     * keeps vanilla's own eight-block reach test, and GT6's override has no 1.20.1 equivalent that
     * does not mean writing a menu class of our own.</p>
     *
     * @return whether the click was consumed, which is always true — {@code CoverCrafting:54}
     */
    public static boolean clickCraftingCover(Entity player, ServerLevel level, BlockPos pos) {
        if (player instanceof ServerPlayer server && level != null && pos != null) {
            server.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new CraftingMenu(id, inventory,
                            ContainerLevelAccess.create(level, pos)),
                    Component.translatable("container.crafting")));
        }
        return true;
    }

    // ── CoverRetrieverItem ─────────────────────────────────────────────────────────

    /** GT6 {@code CoverRetrieverItem:61}: {@code SERVER_TIME % 20 == 15}. */
    public static final int RETRIEVER_PERIOD = 20, RETRIEVER_PHASE = 15;

    /**
     * GT6 {@code CoverRetrieverItem:72}: {@code ST.move(..., 64, 1, 64, 1)} — {@code aMaxSize 64},
     * {@code aMinSize 1}, {@code aMaxMove 64}, {@code aMinMove 1}. The two 64s are one stack, which
     * is also the per-operation ceiling asserted here.
     */
    public static final int RETRIEVER_MAX_MOVE = 64, RETRIEVER_MIN_MOVE = 1;

    /**
     * GT6 {@code CoverRetrieverItem:61}: the retriever fires when the clock reaches
     * {@code % 20 == 15} <em>or</em> when its own pending flag is still set.
     *
     * <p>That flag is GT6's {@code aData.mValues[aSide]}: {@code onStoppedUpdate} sets it to 1 when
     * the cover stops being suspended ({@code CoverRetrieverItem:55-57}) and the tick body clears it
     * once it has run ({@code :62}), so a cover that was held back by a controller cover catches up
     * immediately instead of waiting out the rest of the 20-tick cycle.</p>
     */
    public static boolean retrieverDue(long tickCounter, boolean pending) {
        return pending || tickCounter % RETRIEVER_PERIOD == RETRIEVER_PHASE;
    }

    /**
     * One retriever operation: GT6 {@code CoverRetrieverItem:60-79} — pull filtered items out of the
     * pipe network into the inventory the cover faces.
     *
     * <p>The original walks the pipe graph from its own tile ({@code :69}) and, for every inventory
     * hanging off it that is not itself a pipe ({@code :72}), moves up to 64 matching items into
     * {@code tTarget}, the inventory adjacent to the cover ({@code :67}). The port has no pipe-graph
     * walker available to a cover, so the inventory directly in front of the face plays the part of
     * the network's nearest inventory and {@code target} is the machine — everything the filter, the
     * 64-item ceiling and the {@code % 20 == 15} cadence do is unchanged.</p>
     *
     * @param target      what GT6's {@code tTarget} is: the destination, i.e. the host's own slots
     * @param filter      the stack stored under {@link #FILTER_ITEM_KEY}, or empty for "no filter"
     *                    ({@code CoverRetrieverItem:65-66}, where an invalid stack becomes a
     *                    {@code null} filter rather than an empty one)
     * @param blacklist   GT6's {@code aData.mVisuals[aSide] != 0}, the inverted-filter flag
     * @param tickCounter the owner's cover tick counter
     * @param pending     the cover's pending flag; see {@link #retrieverDue}
     * @return whether any item moved
     */
    public static boolean tickRetriever(Level level, BlockPos machinePos, Direction side,
                                        IItemHandler target, ItemStack filter, boolean blacklist,
                                        long tickCounter, boolean pending) {
        if (target == null || level == null) return false;
        if (!retrieverDue(tickCounter, pending)) return false;
        BlockPos front = machinePos.relative(side);
        if (!level.hasChunkAt(front)) return false;
        BlockEntity be = level.getBlockEntity(front);
        if (be == null) return false;
        IItemHandler source = be.getCapability(ForgeCapabilities.ITEM_HANDLER, side.getOpposite())
                .resolve().orElse(null);
        if (source == null) return false;

        boolean present = !filter.isEmpty();
        boolean moved = false;
        int budget = RETRIEVER_MAX_MOVE;
        for (int slot = 0; slot < source.getSlots() && budget > 0; slot++) {
            ItemStack stack = source.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (!filterPermits(present, blacklist, false, FilterRules.itemMatches(filter, stack))) continue;
            ItemStack simulated = source.extractItem(slot, Math.min(budget, stack.getCount()), true);
            if (simulated.isEmpty()) continue;
            int accepted = simulated.getCount() - insert(target, simulated.copy(), true).getCount();
            if (accepted <= 0) continue;
            ItemStack reallyTaken = source.extractItem(slot, accepted, false);
            if (reallyTaken.isEmpty()) continue;
            var delivered = com.gregtech.gregtech.content.transport.ItemPipeTransferAdapter.transfer(
                    reallyTaken.copy(), () -> target);
            ItemStack leftover = reallyTaken.copyWithCount(reallyTaken.getCount() - delivered.accepted());
            if (!leftover.isEmpty()) {
                var returned = com.gregtech.gregtech.content.transport.ItemPipeTransferAdapter.transfer(
                        leftover.copy(), () -> source);
                leftover.shrink(returned.accepted());
                if (!leftover.isEmpty()) net.minecraft.world.level.block.Block.popResource(level, machinePos, leftover);
            }
            budget -= delivered.accepted();
            moved |= delivered.accepted() > 0;
        }
        return moved;
    }

    /** Insert into every slot of a handler in turn; returns whatever did not fit. */
    private static ItemStack insert(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }

    // ── CoverFilterItem / CoverFilterFluid ─────────────────────────────────────────

    /**
     * The whitelist/blacklist core of both filter covers, read straight off the original.
     *
     * <p>GT6 spells it twice, once for items ({@code CoverFilterItem:115-127}) and once for fluids
     * ({@code CoverFilterFluid:117-129}), with identical bodies:</p>
     *
     * <pre>
     *   if (aCoverSide != aSide) return F;                                    // other faces: not ours
     *   if (aData.mStopped) return T;                                         // suspended: block all
     *   if (no filter stored) return aData.mVisuals[aCoverSide] == 0;         // empty whitelist blocks
     *   return (aData.mVisuals[aCoverSide] == 0) != ST.equal(filter, stack, T);
     * </pre>
     *
     * <p>{@code true} means "intercept", i.e. <em>refuse</em>, which is why the two middle lines
     * look inverted: an empty whitelist admits nothing and an empty blacklist admits everything,
     * while a suspended cover admits nothing at all. This method returns the inverse — whether the
     * candidate may pass — because every caller in the port asks that question.</p>
     *
     * <p>The face test on the first line is the caller's business, not this predicate's: it decides
     * <em>which</em> face the rule is asked about, and the caller already knows that.</p>
     *
     * @param filterPresent whether a filter stack is stored at all
     * @param blacklist     GT6's {@code mVisuals != 0} — the "Inverted Filter" half
     * @param stopped       GT6's {@code aData.mStopped}
     * @param matches       the raw match result, {@code ST.equal(filter, candidate, T)}
     */
    public static boolean filterPermits(boolean filterPresent, boolean blacklist, boolean stopped,
                                        boolean matches) {
        if (stopped) return false;                       // :117 / :119, "suspended blocks all"
        if (!filterPresent) return blacklist;            // :118 / :120, "empty whitelist blocks"
        return blacklist != matches;                     // :119 / :121
    }

    /**
     * GT6 {@code CoverFilterItem:115-127} for a live machine face: the stored item filter, the
     * screwdriver-selected mode and the candidate stack.
     *
     * <p>A face whose cover is <em>not</em> a filter admits everything: GT6 only ever asks this from
     * the filter cover that is attached to the face ({@code interceptItemInsert} is a method of
     * {@code CoverFilterItem}, and the retriever's own blanket rule lives in
     * {@code BasicMachineBlockEntity#coverBlocksItemTraffic}), so a pump or a blank cover must not
     * start refusing pipe traffic by accident.</p>
     */
    public static boolean itemFilterPermits(ItemStack cover, boolean stopped, ItemStack candidate) {
        String id = CoverItems.behavior(cover);
        if (!FILTER_ITEM.equals(id)) return true;
        if (candidate == null || candidate.isEmpty()) return !stopped && !MachineCoverSpec.inverted(cover);
        ItemStack filter = itemFilter(cover);
        return filterPermits(!filter.isEmpty(), MachineCoverSpec.inverted(cover), stopped,
                FilterRules.itemMatches(filter, candidate));
    }

    /**
     * GT6 {@code CoverFilterFluid:117-129}. The original's match is {@code FL.equal(a, b, T)}, which
     * is {@code UT.Code.equal} — the same fluid and NBT ignored ({@code UT.java:113}) — so the
     * port's {@link FluidStack#isFluidEqual(FluidStack)} is the exact counterpart: it compares the
     * fluid and, in 1.20.1, deliberately not the tag.
     */
    public static boolean fluidFilterPermits(ItemStack cover, boolean stopped, FluidStack candidate) {
        // A face without a fluid filter admits everything (see itemFilterPermits): GT6 asks this only
        // from the CoverFilterFluid that is attached to that face.
        if (!FILTER_FLUID.equals(CoverItems.behavior(cover))) return true;
        if (stopped) return false;
        FluidStack filter = fluidFilter(cover);
        if (filter.isEmpty()) return MachineCoverSpec.inverted(cover);
        if (candidate == null || candidate.isEmpty()) return false;
        // UT.java:113 with aIgnoreNBT = T: same fluid, tags ignored — not FluidStack#isFluidEqual,
        // which would also compare the tag and therefore be stricter than the original.
        return filterPermits(true, MachineCoverSpec.inverted(cover), false,
                filter.getFluid() == candidate.getFluid());
    }

    /** The item stored under {@link #FILTER_ITEM_KEY}, or empty ({@code CoverFilterItem:47,74,93}). */
    public static ItemStack itemFilter(ItemStack cover) {
        CompoundTag tag = cover == null ? null : cover.getTag();
        if (tag == null || !tag.contains(FILTER_ITEM_KEY)) return ItemStack.EMPTY;
        return ItemStack.of(tag.getCompound(FILTER_ITEM_KEY));
    }

    /**
     * The fluid stored under {@link #FILTER_FLUID_KEY}, or empty
     * ({@code CoverFilterFluid:51,78,95}; GT6 writes it with {@code FL.save(null, ...)} on
     * {@code :106}).
     */
    public static FluidStack fluidFilter(ItemStack cover) {
        CompoundTag tag = cover == null ? null : cover.getTag();
        if (tag == null || !tag.contains(FILTER_FLUID_KEY)) return FluidStack.EMPTY;
        FluidStack stored = FluidStack.loadFluidStackFromNBT(tag.getCompound(FILTER_FLUID_KEY));
        return stored == null ? FluidStack.EMPTY : stored;
    }

    /**
     * GT6 {@code CoverFilterItem:92-97} / {@code CoverFilterFluid:96-109}: right-clicking the cover
     * with a stack stores that stack as the filter, one item deep — the original never consumes the
     * held stack, and neither does this.
     *
     * @return whether a filter was stored
     */
    public static boolean setItemFilter(ItemStack cover, ItemStack held) {
        if (cover == null || cover.isEmpty() || held == null || held.isEmpty()) return false;
        CompoundTag tag = cover.getOrCreateTag();
        if (tag.contains(FILTER_ITEM_KEY)) return false;   // "already configured" (CoverFilterItem:94)
        tag.put(FILTER_ITEM_KEY, held.copyWithCount(1).save(new CompoundTag()));
        return true;
    }

    /** GT6 {@code CoverFilterFluid:94-111}: the same one-shot right-click rule for a fluid. */
    public static boolean setFluidFilter(ItemStack cover, FluidStack held) {
        if (cover == null || cover.isEmpty() || held == null || held.isEmpty()) return false;
        CompoundTag tag = cover.getOrCreateTag();
        if (tag.contains(FILTER_FLUID_KEY)) return false;
        CompoundTag stored = new CompoundTag();
        held.copy().writeToNBT(stored);
        tag.put(FILTER_FLUID_KEY, stored);
        return true;
    }

    /**
     * The screwdriver toggle, GT6 {@code CoverFilterItem:58-62} / {@code CoverFilterFluid:62-66} /
     * {@code CoverRetrieverItem:94-98}: {@code aData.visual(side, visuals == 0 ? 1 : 0)}. In the port
     * that is the existing {@code gt.cover.inverted} flag, so this returns the new blacklist state.
     *
     * <p>The caller owns {@code setChanged()} and the block update, exactly as
     * {@code BasicMachineBlockEntity.configureControlCover} does for the same flag
     * ({@code BasicMachineBlockEntity:554-562}).</p>
     */
    public static boolean toggleFilterMode(ItemStack cover) {
        CompoundTag tag = cover.getOrCreateTag();
        boolean blacklist = !MachineCoverSpec.inverted(cover);
        tag.putBoolean("gt.cover.inverted", blacklist);
        return blacklist;
    }

    /**
     * GT6 {@code CoverFilterItem:63-66} / {@code CoverFilterFluid:67-70}: the soft hammer removes the
     * filter, and only the filter — the mode survives.
     *
     * @return whether there was one
     */
    public static boolean clearFilter(ItemStack cover) {
        CompoundTag tag = cover == null ? null : cover.getTag();
        if (tag == null) return false;
        boolean had = tag.contains(FILTER_ITEM_KEY) || tag.contains(FILTER_FLUID_KEY);
        tag.remove(FILTER_ITEM_KEY);
        tag.remove(FILTER_FLUID_KEY);
        return had;
    }

    // ── CoverPressureValve ─────────────────────────────────────────────────────────

    /** GT6 {@code CoverPressureValve:50}: {@code aTimer > 2} — the valve ignores the first ticks. */
    public static final int VALVE_MIN_TICKS = 2;

    /**
     * GT6 {@code CoverPressureValve:59}: {@code aData.box(-2, -2, -2, +3, +3, +3)}, the 5×5×5 cube
     * the escaping gas damages. The port reads GT6 boxes as an {@link AABB} around the block
     * inflated by the lower offset, which is what {@code FluidPipeBlockEntity:160-171} already does
     * for the very same GT6 code.
     */
    public static final double VALVE_VENT_INFLATE = 2.0D;

    /** What one valve tick does. GT6 {@code CoverPressureValve:52-61}. */
    public enum ValveAction {
        /** Tank not full, or full with nowhere to go. */
        IDLE,
        /** {@code FL.move(tTank, tDelegator)} — a tank is in front ({@code :54-56}). */
        INTO_TANK,
        /** The gas escapes into the air and hurts what is standing there ({@code :57-61}). */
        VENT
    }

    /**
     * GT6 {@code CoverPressureValve:44}: the valve only goes on a fluid pipe with exactly one tank,
     * and only when the block it faces is <em>not</em> another fluid pipe.
     *
     * <p>{@code interceptCoverPlacement} returns {@code !(... || ... || ...)}: the first two
     * terms reject the host, the third rejects the placement. Flipped into a positive predicate the
     * rule is "host is a single-tank fluid pipe and the neighbour is not a fluid pipe".</p>
     */
    public static boolean valveCanAttachTo(int hostTankCount, boolean neighbourIsFluidPipe) {
        return hostTankCount == 1 && !neighbourIsFluidPipe;
    }

    /** GT6 {@code CoverPressureValve:45}: {@code interceptConnect} exactly when a pipe faces it. */
    public static boolean valveConnectsThrough(boolean neighbourIsFluidPipe) {
        return neighbourIsFluidPipe;
    }

    /**
     * GT6 {@code CoverPressureValve:53-61}: a full tank empties into the tank in front, and if there
     * is none, a gas escapes into the open air — a liquid does not, which is the tooltip's "Liquids
     * require Tank in front!" ({@code CoverPressureValve:70}).
     */
    public static ValveAction valveAction(boolean tankFull, boolean gas, boolean tankInFront,
                                          boolean collisionInFront) {
        if (!tankFull) return ValveAction.IDLE;                 // :53
        if (tankInFront) return ValveAction.INTO_TANK;          // :54-56
        if (gas && !collisionInFront) return ValveAction.VENT;  // :57
        return ValveAction.IDLE;
    }

    /**
     * One pressure valve operation: GT6 {@code CoverPressureValve:49-64}.
     *
     * <p>The order of the original is kept: the clock gate first ({@code :50}), then the full-tank
     * test ({@code :53}), then the decision ({@code :54-61}). {@code tank} is the single tank the
     * original dereferences as {@code mTanks[0]} ({@code :51}); see the class javadoc for why a
     * machine supplies it where GT6 had a fluid pipe.</p>
     *
     * <p>Not ported: the {@code disconnect(aSide, T)} call on {@code :52}, which releases the pipe's
     * connection for the duration of the operation. The port's covers do not own pipe connections —
     * {@code BasicMachineBlockEntity} has no connection state to release — so there is nothing to
     * call; a pipe-hosted valve would have to bring that call with it.</p>
     *
     * @param tank        the tank that is released when full
     * @param tickCounter the owner's cover tick counter, standing in for GT6's {@code aTimer}
     * @return whether the valve acted
     */
    public static boolean tickPressureValve(Level level, BlockPos machinePos, Direction side,
                                            FluidTankGT tank, long tickCounter) {
        if (level == null || tank == null || tank.isEmpty()) return false;
        if (tickCounter <= VALVE_MIN_TICKS) return false;                 // :50 `aTimer > 2`
        FluidStack content = tank.getFluid();
        if (content.isEmpty()) return false;
        BlockPos front = machinePos.relative(side);
        if (!level.hasChunkAt(front)) return false;

        IFluidHandler frontTank = null;
        BlockEntity be = level.getBlockEntity(front);
        if (be != null) {
            frontTank = be.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite())
                    .resolve().orElse(null);
        }
        boolean collision = !level.getBlockState(front).getCollisionShape(level, front).isEmpty();
        boolean full = tank.getAmount() >= tank.capacity();

        return switch (valveAction(full, FluidHazards.isGas(content.getFluid()), frontTank != null, collision)) {
            case INTO_TANK -> moveInto(frontTank, tank);
            case VENT -> vent(level, machinePos, tank, content);
            case IDLE -> false;
        };
    }

    /** GT6 {@code CoverPressureValve:56} {@code FL.move(tTank, tDelegator)}: as much as fits. */
    private static boolean moveInto(IFluidHandler frontTank, FluidTankGT tank) {
        FluidStack offered = tank.drain(intClamp(tank.getAmount()), IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) return false;
        int accepted = frontTank.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return false;
        offered.setAmount(Math.min(accepted, offered.getAmount()));
        int delivered = frontTank.fill(offered.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (delivered < 0 || delivered > offered.getAmount())
            throw new IllegalStateException("Fluid handler returned an invalid accepted amount: " + delivered);
        if (delivered <= 0) return false;
        offered.setAmount(delivered);
        tank.drain(offered, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    /**
     * GT6 {@code CoverPressureValve:57-61}: the fizz, the temperature damage to everything inside
     * {@link #VALVE_VENT_INFLATE} blocks, and {@code GarbageGT.trash(tTank)}.
     *
     * <p>The damage is the port's existing {@code GTEntityHelper.applyTemperatureDamage} with
     * exactly the arguments the original passes — {@code 2.0F} multiplier and {@code 10.0F} cap,
     * which are {@code FluidHazards.PIPE_TEMPERATURE_MULTIPLIER} and
     * {@code FluidHazards.PIPE_TEMPERATURE_CAP} ({@code FluidHazards:96-99}, from
     * {@code MultiTileEntityPipeFluid:299,305}). The temperature comes from
     * {@link #fluidTemperature}. The fizz is vanilla's {@code FIRE_EXTINGUISH}, the same substitution
     * {@code FluidPipeBlockEntity:139-140} makes for the same GT6 sound.</p>
     */
    private static boolean vent(Level level, BlockPos machinePos, FluidTankGT tank, FluidStack content) {
        level.playSound(null, machinePos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
        long temperature = fluidTemperature(content.getFluid());
        for (Entity entity : level.getEntitiesOfClass(Entity.class,
                new AABB(machinePos).inflate(VALVE_VENT_INFLATE))) {
            GTEntityHelper.applyTemperatureDamage(entity, temperature,
                    FluidHazards.PIPE_TEMPERATURE_MULTIPLIER, FluidHazards.PIPE_TEMPERATURE_CAP);
        }
        tank.setEmpty();                                                  // GarbageGT.trash(tTank)
        return true;
    }

    /**
     * GT6 {@code FL.temperature(aFluid)}: the fluid's own Kelvin value, which is what
     * {@code CoverPressureValve:59} feeds to {@code applyTemperatureDamage}.
     *
     * <p>The port carries it in the fluid's registry metadata ({@code RegisteredFluids.FluidEntry}),
     * reached through {@code GTFluids.entryForFluid} — the same route {@code FluidHazards.isGas}
     * takes. A fluid GT6 never declared falls back to Forge's own {@code FluidType} temperature.</p>
     */
    public static long fluidTemperature(Fluid fluid) {
        if (fluid == null) return 0L;
        var entry = GTFluids.entryForFluid(fluid);
        if (entry != null) return entry.temperature();
        return fluid.getFluidType().getTemperature();
    }

    private static int intClamp(long amount) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, amount));
    }

    // ── CoverAsphalt ───────────────────────────────────────────────────────────────

    /**
     * GT6 {@code CoverAsphalt:39}: {@code aEntity.motionX *= 1.3; aEntity.motionZ *= 1.3;} — the
     * asphalt cover is a speed boost, and it is the only material cover in GT6 that has a behaviour
     * at all.
     */
    public static final double ASPHALT_BOOST = 1.3D;

    /**
     * GT6 {@code CoverAsphalt:39} as a pure multiplier: {@code 1.3} while the entity is moving, is
     * not in water and is not sneaking, {@code 1.0} otherwise.
     *
     * <p>All three of the original's conditions are negative gates — no motion means nothing to
     * boost, water and sneaking mean the player does not want to be boosted — so a pinned-down
     * entity, a swimming one and a crouching one all come out at {@code 1.0} and are therefore
     * distinguishable from the boosted case in a test.</p>
     */
    public static double asphaltFactor(double motionX, double motionZ, boolean inWater,
                                       boolean sneaking) {
        if (motionX == 0 && motionZ == 0) return 1.0D;
        if (inWater || sneaking) return 1.0D;
        return ASPHALT_BOOST;
    }

    /**
     * GT6 {@code CoverAsphalt:38-41} {@code onWalkOver}: the same rule applied to a live entity.
     *
     * <p>{@code motionX}/{@code motionZ} are the 1.20.1 {@code getDeltaMovement()} components — the
     * original mutates the fields in place, which 1.20.1 does through
     * {@link Entity#setDeltaMovement(Vec3)}. The vertical component is left alone, exactly as the
     * original leaves {@code motionY} alone.</p>
     *
     * @return whether the entity was boosted. GT6 returns {@code T} even when it was not, so the
     *         return value here is the port's own "did anything happen" answer rather than the
     *         original's constant; the caller that needs GT6's constant simply ignores it.
     */
    public static boolean walkOverAsphalt(Entity entity) {
        if (entity == null) return false;
        Vec3 motion = entity.getDeltaMovement();
        double factor = asphaltFactor(motion.x, motion.z, entity.isInWater(), entity.isShiftKeyDown());
        if (factor == 1.0D) return false;
        entity.setDeltaMovement(motion.x * factor, motion.y, motion.z * factor);
        return true;
    }

    // ── CoverRedstoneTorch / CoverRedstoneRepeater ─────────────────────────────────

    /**
     * GT6 {@code CoverRedstoneTorch:42-44}: the torch's condition is
     * {@code ((MultiTileEntityWireRedstoneInsulated)aData.mTileEntity).mRedstone > 0} — the wire it
     * sits on carries a signal.
     *
     * <p>{@code AbstractCoverAttachmentTorch:60-68} turns a true condition into {@code mVisuals = 1}
     * and a false one into {@code mVisuals = 0}, and {@code CoverRedstoneTorch:31-33} maps
     * {@code mVisuals == 0} to the <em>lit</em> textures. So the torch is lit exactly when the wire
     * is <em>not</em> powered: it is an inverter, which {@code getRedstoneOutWeak/Strong}
     * ({@code AbstractCoverAttachmentTorch:50-57}, {@code mVisuals == 0 ? 15 : 0}) confirms.</p>
     */
    public static final int TORCH_VISUAL_LIT = 0, TORCH_VISUAL_DARK = 1;

    /**
     * GT6 {@code CoverRedstoneTorch:43}: {@code mRedstone > 0}. The return value is the original's
     * {@code condition}, i.e. the argument of {@code onTickPost}'s {@code if}
     * ({@code AbstractCoverAttachmentTorch:62}).
     */
    public static boolean torchCondition(boolean wirePowered) {
        return wirePowered;
    }

    /**
     * GT6 {@code CoverRedstoneRepeater:43}: {@code mRedstone <= 0} — the repeater lights exactly
     * when the wire <em>is</em> powered, so it is a follower where the torch is an inverter.
     */
    public static boolean repeaterCondition(boolean wirePowered) {
        return !wirePowered;
    }

    /**
     * GT6 {@code AbstractCoverAttachmentTorch:60-68} for the torch: {@code mVisuals} becomes 1 when
     * the condition holds and 0 when it does not, which {@code CoverRedstoneTorch:31-33} renders as
     * off/on. {@code CoverRedstoneTorch:43} makes the condition "wire powered", so a powered wire
     * darkens the torch.
     */
    public static int torchVisual(boolean wirePowered) {
        return torchCondition(wirePowered) ? TORCH_VISUAL_DARK : TORCH_VISUAL_LIT;
    }

    /** The same for {@code CoverRedstoneRepeater:43}: a powered wire lights it. */
    public static int repeaterVisual(boolean wirePowered) {
        return repeaterCondition(wirePowered) ? TORCH_VISUAL_DARK : TORCH_VISUAL_LIT;
    }

    /**
     * GT6 {@code AbstractCoverAttachmentTorch:50-57} {@code getRedstoneOutStrong} and
     * {@code getRedstoneOutWeak} — byte for byte the same body, {@code mVisuals == 0 ? 15 : 0}.
     */
    public static int torchSignal(int visual) {
        return visual == TORCH_VISUAL_LIT ? 15 : 0;
    }

    /**
     * GT6 {@code AbstractCoverAttachmentTorch:35}: {@code interceptCoverPlacement} refuses every
     * host that is not a {@code MultiTileEntityWireRedstoneInsulated} — the redstone torch and the
     * repeater are covers <em>for the insulated redstone wire</em>, not for machines
     * ({@code GT_API:799-802} registers them on the vanilla blocks, but the placement rule is the
     * wire's).
     */
    public static boolean torchCoverCanAttachTo(boolean hostIsRedstoneWire) {
        return hostIsRedstoneWire;
    }

    /** GT6 {@code AbstractCoverAttachmentTorch:36}: {@code interceptConnect} — always true. */
    public static boolean torchCoverInterceptsConnect() {
        return true;
    }

    // ── CoverTextureMulti: the blank and warning covers ────────────────────────────

    /**
     * GT6 {@code MultiItemTechnological:59}: {@code new CoverTextureMulti(T, T, "machines/covers/
     * blank/", 6)} — the blank cover has six designs.
     */
    public static final int BLANK_COVER_DESIGNS = 6;

    /**
     * GT6 {@code MultiItemTechnological:87}: {@code new CoverTextureMulti(T, T, "machines/covers/
     * warning/", 20)} — the warning cover has twenty.
     */
    public static final int WARNING_COVER_DESIGNS = 20;

    /**
     * The number of designs a texture cover can cycle through, by behaviour id; {@code 0} for
     * anything that is not one of the two texture covers.
     */
    public static int designCount(String behaviourId) {
        if (BLANK_COVER.equals(behaviourId)) return BLANK_COVER_DESIGNS;
        if (WARNING_COVER.equals(behaviourId)) return WARNING_COVER_DESIGNS;
        return 0;
    }

    /**
     * GT6 {@code CoverTextureMulti:63-66}: a chisel click is
     * {@code aData.visual(aSide, (visuals + 1) % mTextures.length)} — the whole behaviour of the
     * blank and warning covers.
     *
     * <p>{@code floorMod} rather than {@code %} so that a negative {@code current} — which a
     * corrupted save could produce — still lands inside the design range instead of selecting a
     * negative texture index.</p>
     */
    public static int nextDesign(int current, int count) {
        if (count <= 0) return 0;
        return Math.floorMod(current + 1, count);
    }

    /** GT6 {@code CoverTextureMulti:77}: {@code isDecorative} is true for both texture covers. */
    public static boolean textureCoverIsDecorative() {
        return true;
    }

    /** GT6 {@code CoverTextureMulti:78}: {@code needsVisualsSaved} — the chosen design persists. */
    public static boolean textureCoverNeedsVisualsSaved() {
        return true;
    }

    /** The chisel-selected design of a texture cover, from the port's {@link #VISUAL_KEY}. */
    public static int design(ItemStack cover) {
        CompoundTag tag = cover == null ? null : cover.getTag();
        return tag == null ? 0 : tag.getInt(VISUAL_KEY);
    }

    /** Advance a texture cover's design by one, GT6 {@code CoverTextureMulti:64}. */
    public static int cycleDesign(ItemStack cover, int count) {
        if (cover == null || cover.isEmpty() || count <= 1) return design(cover);
        int next = nextDesign(design(cover), count);
        cover.getOrCreateTag().putInt(VISUAL_KEY, next);
        return next;
    }

    private CoverUtilityBehaviors() {}
}
