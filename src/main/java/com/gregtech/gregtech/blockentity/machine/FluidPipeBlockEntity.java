package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.fluid.FluidPipeChannels;
import com.gregtech.gregtech.api.fluid.FluidPipeSafety;
import com.gregtech.gregtech.api.fluid.PipeIgnition;
import com.gregtech.gregtech.registry.GTFluids;

import com.gregtech.gregtech.api.fluid.FluidHazards;
import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.inventory.BlockContents;
import com.gregtech.gregtech.api.machine.PipeSpec;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.content.cover.CoverAttachmentBehaviors;
import com.gregtech.gregtech.content.cover.CoverItems;
import com.gregtech.gregtech.content.cover.CoverUtilityBehaviors;
import com.gregtech.gregtech.content.cover.PanelCoverHost;
import com.gregtech.gregtech.content.cover.PanelCoverRuntime;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.util.GTEntityHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * GT6 fluid pipe tile entity. Single class handles all sizes via {@link PipeSpec}.
 *
 * <h2>Covers: a fluid pipe is a cover host, the way GT6's own pipe is</h2>
 *
 * <p>GT6 attaches covers to <em>tiles</em>, and two of its covers are pipe covers first and machine
 * covers second: the pressure valve, whose {@code interceptCoverPlacement} refuses any host that is
 * not a fluid pipe with exactly one tank ({@code CoverPressureValve:44}), and the fluid filter, whose
 * {@code interceptFluidFill}/{@code interceptFluidDrain} are asked from the pipe's own fill path
 * ({@code CoverFilterFluid:117,124}). This class is therefore a {@link PanelCoverHost} exactly the
 * way {@code BasicMachineBlockEntity} is one, with three differences, each of them deliberate:</p>
 *
 * <ul>
 *   <li><b>No {@code MachineControl}.</b> {@code coverControl} is not overridden, so
 *       {@link PanelCoverHost#coverPossible} and {@link PanelCoverHost#coverSupportsPossible} keep
 *       their interface defaults ({@code false}; {@code PanelCoverHost:16-17}) and
 *       {@code MachineControl.find} answers {@code null} for a block entity that is not a
 *       {@code Provider} ({@code MachineControl:25-27}). The two consequences, both of them correct
 *       for a pipe:
 *       <ul>
 *         <li>the panels that <em>read</em> machine state are refused at attach rather than left
 *             broken — {@code PanelCoverRuntime.canAttach:33-41} rejects the status display, the
 *             progress sensor, both energy displays and the three mode selectors because there is no
 *             control and no energy buffer to read, so the player is told the panel is unsupported
 *             instead of getting a dead plate. A stack that a save carries anyway stays inert:
 *             {@code afterTick:95-100} answers 0 for it;</li>
 *         <li>the panels that only <em>emit</em> — the redstone emitter, the conductor pair, the
 *             cover controller and the shutter — work, because {@code afterTick} drives them from the
 *             face's own redstone input and needs no control either.</li>
 *       </ul>
 *       None of the fluid-side covers consults a control: {@link CoverUtilityBehaviors#tickPressureValve}
 *       reads a tank, {@link CoverAttachmentBehaviors#tickDrain} and
 *       {@link CoverAttachmentBehaviors#tickVent} read the world, and the fluid filter reads its own
 *       stack.</li>
 *   <li><b>No inventory.</b> The item retriever moves items into the host's own slots
 *       ({@code CoverUtilityBehaviors.tickRetriever}'s {@code target}); a pipe has no slots, so a
 *       retriever attached to one never acts.</li>
 *   <li><b>Lazily allocated cover state.</b> See the field comments below.</li>
 * </ul>
 *
 * <h2>Which tank a face sees: none — tanks do not map to faces, in GT6 either</h2>
 *
 * <p>GT6's {@code MultiTileEntityPipeFluid.getFluidTanks2(aSide)} returns the whole {@code mTanks}
 * array for <em>every</em> side ({@code MultiTileEntityPipeFluid:477}), and its
 * {@code getFluidTankFillable2} picks the first tank already holding that fluid and otherwise the
 * first empty one ({@code :463-466}). A face only gates <em>connectivity</em>
 * ({@code canAcceptFluidsFrom}/{@code canEmitFluidsTo}, {@code :510-511}). The port is the same
 * shape: sided handlers gate connectivity, filters and channel-local backflow, while unsided
 * access serves the pipe's own cover operations. All faces see the same channels. The one
 * exception is the pressure valve, which hard-codes {@code mTanks[0]}
 * ({@code CoverPressureValve:51}) and can only be attached when the pipe has a single tank, so
 * {@code tanks[0]} and "the pipe" coincide there.</p>
 */
public class FluidPipeBlockEntity extends BlockEntity implements IFluidHandler, PanelCoverHost, com.gregtech.gregtech.api.sensor.CompressionSensorSource {
    @Override public long gibblValue(int side) {
        long total = 0;
        for (var tank : tanks) total += tank.getAmount();
        return total;
    }
    @Override public long gibblMaximum(int side) { return spec.capacity() * tanks.length; }
    private final PipeSpec spec;
    private final FluidTankGT[] tanks;
    private final byte[] lastReceivedFrom;
    private long temperature = GregTechConstants.DEF_ENV_TEMP;
    private long transferredAmount;

    private static final String NBT_TEMPERATURE = "gt.temperature";
    private static final String NBT_TRANSFERRED = "gt.transferred";
    private static final String NBT_LAST_RECEIVED = "gt.last_received";
    private static final String NBT_TANK_PREFIX = "gt.tank.";
    /** One cover per face under the same key the machine uses ({@code BasicMachineBlockEntity:1299}). */
    private static final String NBT_COVER_PREFIX = "gt_cover_";

    private LazyOptional<IFluidHandler> handler = LazyOptional.of(() -> this);

    // ── Cover state: zero cost when unused ───────────────────────────────────
    //
    // A world holds tens of thousands of fluid pipes and serverTick runs for every one of them, so a
    // coverless pipe must not pay for the cover system at all:
    //   * `covers` is null until the first cover is attached — no ItemStack[6] per pipe;
    //   * `hasCovers` is the single boolean serverTick tests, so the whole cover pass is one field
    //     read for a pipe that has none, and no face is walked;
    //   * `panels` is created on first attach rather than in the field initialiser as
    //     BasicMachineBlockEntity:71 does it, because PanelCoverRuntime itself allocates an int[6]
    //     and a boolean[6] (PanelCoverRuntime:12-13) — this is the one place this class deliberately
    //     does not copy the machine's eager field;
    //   * `faceHandlers` is null until a sided capability is requested (see
    //     {@link #faceHandler}).

    /** One cover item per {@link Direction#ordinal()}; {@code null} until a cover is attached. */
    private @Nullable ItemStack[] covers;

    /** Whether any face carries a cover. The cover tick pass is skipped when this is false. */
    private boolean hasCovers;

    /**
     * The cover tick counter, standing in for GT6's {@code aTimer} exactly as
     * {@code BasicMachineBlockEntity.coverTicks:76} does for the machine — the full argument is on
     * {@link CoverAttachmentBehaviors}'s class javadoc: {@code level.getGameTime()} never advances
     * inside a one-tick GameTest body, this counter advances once per covered server tick.
     */
    private long coverTicks;

    /** Created by {@link #panels()} on first use, never in the constructor. */
    private @Nullable PanelCoverRuntime panels;

    /** Per-face capability views, built by {@link #faceHandler} and dropped by {@link #invalidateCaps}. */
    private @Nullable LazyOptional<IFluidHandler>[] faceHandlers;

    public FluidPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.spec = specFromState(state);
        int count = spec.tankCount();
        this.tanks = new FluidTankGT[count];
        for (int i = 0; i < count; i++) {
            this.tanks[i] = new FluidTankGT(spec.capacity())
                    .setOnChanged(this::setChanged)
                    .setGasProof(spec.gasProof())
                    .setAcidProof(spec.acidProof())
                    .setPlasmaProof(spec.plasmaProof())
                    .setMagicProof(spec.magicProof())
                    .setMaxTemperature(spec.maxTemperature());
        }
        this.lastReceivedFrom = new byte[count];
    }

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        this(GTBlockEntities.FLUID_PIPE.get(), pos, state);
    }

    private static PipeSpec specFromState(BlockState state) {
        if (state.getBlock() instanceof FluidPipeBlock pipe) return pipe.spec();
        throw new IllegalStateException("FluidPipeBlockEntity on non-pipe block");
    }

    public PipeSpec spec() { return spec; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidPipeBlockEntity be) {
        if (!level.isClientSide) be.tickServer(state);
    }

    private void tickServer(BlockState state) {
        transferredAmount = 0;
        boolean firstFluid = true;
        for (int i = 0; i < tanks.length; i++) {
            FluidStack fluid = tanks[i].getFluid();
            if (!fluid.isEmpty()) {
                var entry = GTFluids.entryForFluid(fluid.getFluid());
                long fluidTemperature = entry == null
                        ? fluid.getFluid().getFluidType().getTemperature(fluid) : entry.temperature();
                temperature = FluidPipeSafety.observeTemperature(temperature, fluidTemperature, firstFluid);
                firstFluid = false;
                if (!checkSafety(fluid, i)) return;
            }
            // Original checks each channel, even an empty one retaining heat from the previous tick.
            if (temperature > spec.maxTemperature()) {
                PipeIgnition.igniteNeighbors(level, worldPosition);
                if (overheatDestroysPipe()) {
                    for (FluidTankGT tank : tanks) tank.setEmpty();
                    level.setBlock(worldPosition, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 3);
                    return;
                }
            }
            // GT6 processes safety and distribution channel by channel, then clears that channel's mask.
            if (!tanks[i].isEmpty()) distributeChannel(state, tanks[i], i);
            lastReceivedFrom[i] = 0;
        }
        if (firstFluid) temperature = FluidPipeSafety.emptyTemperature(temperature,
                SmelteryBlockEntityHelper.environmentTemperature(level, worldPosition));
        // Zero cost when unused: `hasCovers` is false for every coverless pipe, so this is a single
        // field read and returns — no face is walked, nothing is allocated, nothing is dispatched.
        if (hasCovers) tickCovers();
        setChanged();
    }

    // ── Covers: host API (GT6's fluid pipe as a cover host) ──────────────────

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
     * {@code BasicMachineBlockEntity:302-312} is the machine's copy.
     *
     * <p>The one rule the machine does not need is the pressure valve's host test, which GT6 keeps in
     * the cover itself: {@code CoverPressureValve:44} {@code interceptCoverPlacement} refuses any host
     * that is not a {@code MultiTileEntityPipeFluid} with exactly one tank, and any placement whose
     * neighbour is another fluid pipe. {@link CoverUtilityBehaviors#valveCanAttachTo} is that rule as
     * a predicate and this method is the pipe-host caller it was written for; the tank count comes
     * from {@link PipeSpec#tankCount()}, which is {@code 1} for every size except QUADRUPLE
     * ({@code 4}) and NONUPLE ({@code 9}) — exactly GT6's {@code NBT_TANK_COUNT}
     * ({@code MultiTileEntityPipeFluid:92-98}). Without the test a valve could be attached to a
     * quadruple pipe and would then silently release {@code tanks[0]} only.</p>
     */
    @Override
    public boolean attachCover(Direction side, ItemStack stack) {
        if (stack.isEmpty() || !getCover(side).isEmpty() || !panels().canAttach(side, stack)) return false;
        if (CoverUtilityBehaviors.PRESSURE_VALVE.equals(CoverItems.behavior(stack))
                && !CoverUtilityBehaviors.valveCanAttachTo(spec.tankCount(), neighbourIsFluidPipe(side))) {
            return false;
        }
        if (covers == null) covers = emptyCovers();
        covers[side.ordinal()] = stack.copyWithCount(1);
        hasCovers = true;
        panels().attached(side);
        invalidateFaceHandler(side);
        syncCovers();
        return true;
    }

    /**
     * Removes the cover on a face. Mirrors {@code BasicMachineBlockEntity:315-328}, with
     * {@code panels.afterTick()} in the place of that method's {@code updateCoverSignals()}: the
     * signals a pipe face can emit are the panel ones, and {@code afterTick} is what recomputes and
     * publishes them ({@code PanelCoverRuntime:85-119}).
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
        return panels().removed(side,cover);
    }

    /**
     * The pipe's drop-on-break for covers, i.e. {@code BasicMachineBlockEntity:1494-1499}
     * ({@code dropContents}'s cover loop). Called from {@link FluidPipeBlock#onRemove}, next to
     * {@link #dumpFluidsToAdjacent()} — that is where this pipe's own removal logic lives, and the
     * block entity is still resolvable there, which is the same call site shape the machine has in
     * {@code BasicMachineBlock:254-262}.
     */
    public void dropCovers() {
        if (covers == null) return;
        for (int i = 0; i < covers.length; i++) {
            if(!com.gregtech.gregtech.content.cover.CoverDrops.retained(this))BlockContents.drop(this, covers[i]);
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

    private boolean neighbourIsFluidPipe(Direction side) {
        return level != null && level.getBlockEntity(worldPosition.relative(side)) instanceof FluidPipeBlockEntity;
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

    /** Refresh the controller before its pause gate, then dispatch valid covers with source cadence. */
    private void tickCovers() {
        PanelCoverRuntime runtime = panels();
        coverTicks++;
        runtime.beforeTick();
        if (runtime.stopped()) { runtime.afterTick(); return; }
        for (Direction side : Direction.values()) {
            String id = coverIdOf(side);
            if (id == null) continue;
            switch (id) {
                case CoverItems.PUMP, CoverItems.CONVEYOR, CoverItems.ROBOT_ARM ->
                        com.gregtech.gregtech.content.cover.ComponentCoverRuntime.tick(this,side,level.getGameTime());
                case CoverUtilityBehaviors.PRESSURE_VALVE ->
                        CoverUtilityBehaviors.tickPressureValve(level, worldPosition, side, tanks[0], coverTicks);
                case CoverAttachmentBehaviors.DRAIN ->
                        CoverAttachmentBehaviors.tickDrain(level, worldPosition, side, this, coverTicks);
                case CoverAttachmentBehaviors.AIR_VENT ->
                        CoverAttachmentBehaviors.tickVent(level, worldPosition, side, this, coverTicks);
                default -> { }
            }
        }
        runtime.afterTick();
    }

    /**
     * Two of the three player-facing entry points of a filter cover, copied name for name from
     * {@code BasicMachineBlockEntity:1631-1646} so that both hosts delegate to the same statics and
     * {@code BasicMachineBlock.use}'s call order can be mirrored in {@code FluidPipeBlock.use}.
     *
     * <p>GT6's right-click on a filter cover ({@code CoverFilterItem:88-112},
     * {@code CoverFilterFluid:92-114}) and on the retriever ({@code CoverRetrieverItem:123-136}): the
     * held stack becomes the filter, once.</p>
     *
     * @return whether a filter was stored
     */
    public boolean clickFilterCover(Direction side, ItemStack held) {
        String id = coverIdOf(side);
        if (id == null || held == null || held.isEmpty()) return false;
        ItemStack stack = getCover(side);
        if (CoverUtilityBehaviors.FILTER_ITEM.equals(id) || CoverUtilityBehaviors.RETRIEVER_ITEM.equals(id)) {
            return CoverUtilityBehaviors.setItemFilter(stack, held);
        }
        if (CoverUtilityBehaviors.FILTER_FLUID.equals(id)) {
            var container = held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElse(null);
            if (container == null) return false;
            FluidStack fluid = container.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            return CoverUtilityBehaviors.setFluidFilter(stack, fluid);
        }
        return false;
    }

    /**
     * The screwdriver and soft-hammer halves of the same covers, copied from
     * {@code BasicMachineBlockEntity:1658-1672}: the screwdriver flips whitelist/blacklist
     * ({@code CoverFilterFluid:62-66}) and the soft hammer clears the filter ({@code :67-70}).
     */
    public boolean configureFilterCover(Direction side, boolean screwdriver, boolean softHammer) {
        String id = coverIdOf(side);
        if (id == null) return false;
        boolean filterish = CoverUtilityBehaviors.FILTER_ITEM.equals(id)
                || CoverUtilityBehaviors.FILTER_FLUID.equals(id)
                || CoverUtilityBehaviors.RETRIEVER_ITEM.equals(id);
        if (!filterish || (!screwdriver && !softHammer)) return false;
        if (level != null && level.isClientSide) return true;
        ItemStack stack = getCover(side);
        if (softHammer) CoverUtilityBehaviors.clearFilter(stack);
        else CoverUtilityBehaviors.toggleFilterMode(stack);
        syncCovers();
        return true;
    }

    /** GT6 runs magic, gas, plasma and acid independently against the original channel fluid. */
    private boolean checkSafety(FluidStack fluid, int tankIndex) {
        if (fluid.isEmpty() || level == null) return true;
        FluidTankGT tank = tanks[tankIndex];
        Fluid fluidType = fluid.getFluid();
        int magicLoss = FluidPipeSafety.magicLoss(FluidHazards.isMagic(fluidType),
                FluidHazards.isGas(fluidType), tank.isMagicProof());
        if (magicLoss > 0) {
            leak(tank, magicLoss);
            // Original direct applyPotion has no thermal/chemical armor or creative immunity gate.
            hurtAround(worldPosition, 3.0D, entity -> {
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, 1200, 1));
                }
            });
            if (magicDestroysPipe()) {
                for (FluidTankGT other : tanks) other.setEmpty();
                // GT6's absent-Thaumcraft fallback is air (IL.block -> ST.block(null) -> CS.NB).
                // A future pollution integration must supply the actual mod's registered block.
                level.removeBlock(worldPosition, false);
                return false;
            }
        }
        var losses = FluidPipeSafety.losses(FluidHazards.isGas(fluidType), FluidHazards.isPlasma(fluidType),
                FluidHazards.isAcid(fluidType), tank.isGasProof(), tank.isPlasmaProof(), tank.isAcidProof());
        if (losses.gas() > 0) leakHeat(tank, losses.gas());
        if (losses.plasma() > 0) leakHeat(tank, losses.plasma());
        if (losses.acid() > 0) {
            leak(tank, losses.acid());
            hurtAround(worldPosition, 1.0D, entity ->
                    GTEntityHelper.applyChemDamage(entity, FluidHazards.PIPE_ACID_DAMAGE));
            if (corrosionDestroysPipe()) {
                // Empty before removal: the block's break callback must not export destroyed contents.
                for (FluidTankGT other : tanks) other.setEmpty();
                level.removeBlock(worldPosition, false);
                return false;
            }
        }
        return true;
    }

    /** Random decision isolated so world tests can exercise both corrosion outcomes deterministically. */
    protected boolean corrosionDestroysPipe() {
        return level.random.nextInt(FluidHazards.PIPE_ACID_DESTROY_CHANCE) == 0;
    }

    /** One percent per overheated channel, as in original onServerTickPre. */
    protected boolean overheatDestroysPipe() {
        return level.random.nextInt(100) == 0;
    }

    protected boolean magicDestroysPipe() {
        return level.random.nextInt(FluidHazards.PIPE_MAGIC_DESTROY_CHANCE) == 0;
    }

    private void leak(FluidTankGT tank, int amount) {
        transferredAmount += tank.remove(amount);
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    private void leakHeat(FluidTankGT tank, int amount) {
        leak(tank, amount);
        hurtAround(worldPosition, 2.0D, entity -> GTEntityHelper.applyTemperatureDamage(entity,
                temperature, FluidHazards.PIPE_TEMPERATURE_MULTIPLIER, FluidHazards.PIPE_TEMPERATURE_CAP));
    }

    /**
     * Everything within {@code inflate} blocks of the pipe, hurt by {@code action}. GT6's
     * {@code box(...)} bounds are inclusive on the lower and exclusive on the upper corner, which is
     * what an inflated {@link AABB} around the pipe's own block already is - so {@code inflate(1.0D)}
     * is GT6's 3x3x3 and {@code inflate(2.0D)} its 5x5x5.
     *
     * <p>Only called on the hazard path, never for a pipe whose flags accept its fluid, so the one
     * list allocation per offending tick is not on the ordinary transfer path.
     */
    private void hurtAround(BlockPos pos, double inflate, java.util.function.Consumer<Entity> action) {
        for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(pos).inflate(inflate))) {
            action.accept(entity);
        }
    }

    /**
     * Auto-connect on placement: connects to the clicked face, then scans all 6 sides
     * and auto-connects to any adjacent same-type pipe that already has its connection
     * facing toward this new pipe.
     */
    public void autoConnectOnPlace(@Nullable Direction clickedFace) {
        if (level == null || level.isClientSide) return;

        // First, connect to the clicked face (existing behavior)
        if (clickedFace != null) {
            Direction toNeighbor = clickedFace.getOpposite();
            BlockPos neighborPos = worldPosition.relative(toNeighbor);
            BlockEntity be = level.getBlockEntity(neighborPos);
            if (be instanceof FluidPipeBlockEntity neighborPipe) {
                BlockState state = getBlockState();
                BlockState newState = state.setValue(FluidPipeBlock.propFor(toNeighbor), true);
                level.setBlockAndUpdate(worldPosition, newState);

                BlockState neighborState = neighborPipe.getBlockState();
                if (!neighborState.getValue(FluidPipeBlock.propFor(clickedFace))) {
                    level.setBlockAndUpdate(neighborPos,
                            neighborState.setValue(FluidPipeBlock.propFor(clickedFace), true));
                }
            }
        }

        // Then scan all 6 sides for adjacent pipes already connected toward us
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(neighborPos);
            if (!(be instanceof FluidPipeBlockEntity neighborPipe)) continue;

            BlockState neighborState = neighborPipe.getBlockState();
            if (neighborState.getValue(FluidPipeBlock.propFor(dir.getOpposite()))) {
                BlockState state = getBlockState();
                if (!state.getValue(FluidPipeBlock.propFor(dir))) {
                    level.setBlockAndUpdate(worldPosition, state.setValue(FluidPipeBlock.propFor(dir), true));
                }
            }
        }
    }

    private void distributeChannel(BlockState state, FluidTankGT tank, int channel) {
        // Vanilla cauldrons have no block entity. They take priority over ordinary targets.
        fillCauldrons(state, tank);
        if (tank.isEmpty()) return;
        java.util.List<FluidTankGT> pipes = new java.util.ArrayList<>();
        java.util.List<IFluidHandler> machines = new java.util.ArrayList<>();
        FluidStack fluid = tank.getFluid();
        for (Direction side : Direction.values()) {
            if ((lastReceivedFrom[channel] & (1 << side.ordinal())) != 0
                    || !state.getValue(FluidPipeBlock.propFor(side))
                    || !com.gregtech.gregtech.content.cover.ComponentCoverRuntime.allowsFluid(getCover(side),false) || !coverFluidFilterPermits(side, fluid)) continue;
            BlockPos neighborPos = worldPosition.relative(side);
            if (!level.hasChunkAt(neighborPos)) continue;
            BlockEntity be = level.getBlockEntity(neighborPos);
            if (be instanceof FluidPipeBlockEntity neighbor) {
                Direction incoming = side.getOpposite();
                if (!neighbor.getBlockState().getValue(FluidPipeBlock.propFor(incoming))
                        || !com.gregtech.gregtech.content.cover.ComponentCoverRuntime.allowsFluid(neighbor.getCover(incoming),true) || !neighbor.coverFluidFilterPermits(incoming, fluid)) continue;
                int targetChannel = neighbor.fillableChannel(fluid);
                if (targetChannel < 0) continue;
                FluidTankGT target = neighbor.tanks[targetChannel];
                if (target.getAmount() >= tank.getAmount()) continue;
                // Original GT6 marks eligible channels before distribution, even if capacity blocks the push.
                neighbor.lastReceivedFrom[targetChannel] |= (byte) (1 << incoming.ordinal());
                pipes.add(level.random.nextInt(pipes.size() + 1), target);
            } else {
                IFluidHandler handler = be == null ? null : be.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite()).orElse(null);
                if (handler == null) continue;
                FluidStack probe = fluid.copy();
                probe.setAmount(1);
                if (handler.fill(probe, FluidAction.SIMULATE) > 0
                        || handler.fill(fluid.copy(), FluidAction.SIMULATE) > 0) {
                    machines.add(level.random.nextInt(machines.size() + 1), handler);
                }
            }
        }
        if (pipes.isEmpty() && machines.isEmpty()) return;
        long[] amounts = new long[pipes.size()];
        for (int i = 0; i < amounts.length; i++) amounts[i] = pipes.get(i).getAmount();
        long targetLevel = FluidPipeChannels.distributionLevel(tank.getAmount(), amounts, machines.size());
        for (FluidTankGT target : pipes) transferToPipe(tank, target, targetLevel - target.getAmount());
        for (IFluidHandler target : machines) {
            if (tank.isEmpty()) break;
            FluidStack offered = tank.drain(FluidTankGT.bindInt(targetLevel), FluidAction.SIMULATE);
            int accepted = target.fill(offered, FluidAction.EXECUTE);
            transferredAmount += tank.remove(accepted);
        }
        long pressure = FluidPipeChannels.pressureShare(tank.getAmount(), spec.capacity(), pipes.size());
        if (pressure > 0) for (FluidTankGT target : pipes) transferToPipe(tank, target, pressure);
    }

    /** Selected channels already match full fluid identity; keep internal pipe amounts as longs. */
    private void transferToPipe(FluidTankGT source, FluidTankGT target, long requested) {
        long amount = Math.min(Math.min(requested, source.getAmount()), target.capacity() - target.getAmount());
        if (amount <= 0) return;
        if (target.isEmpty()) target.setFluid(source.getFluidLong(), amount);
        else target.add(amount);
        transferredAmount += source.remove(amount);
    }

    private void fillCauldrons(BlockState state, FluidTankGT tank) {
        FluidStack fluid = tank.getFluid();
        var entry = com.gregtech.gregtech.registry.GTFluids.entryForFluid(fluid.getFluid());
        boolean water = fluid.getFluid() == net.minecraft.world.level.material.Fluids.WATER
                || entry != null && (entry.flags() & com.gregtech.gregtech.data.FluidCatalog.FluidFlags.WATER) != 0;
        if (!water) return;
        for (Direction side : Direction.values()) {
            if (!state.getValue(FluidPipeBlock.propFor(side)) || !com.gregtech.gregtech.content.cover.ComponentCoverRuntime.allowsFluid(getCover(side),false) || !coverFluidFilterPermits(side, fluid)) continue;
            BlockPos pos = worldPosition.relative(side);
            if (!level.hasChunkAt(pos)) continue;
            BlockState cauldron = level.getBlockState(pos);
            int current;
            if (cauldron.is(net.minecraft.world.level.block.Blocks.CAULDRON)) current = 0;
            else if (cauldron.is(net.minecraft.world.level.block.Blocks.WATER_CAULDRON))
                current = cauldron.getValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL);
            else continue;
            int cost = FluidPipeChannels.cauldronCost(current, tank.getAmount());
            if (cost == 0) continue;
            BlockState filled = net.minecraft.world.level.block.Blocks.WATER_CAULDRON.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, current + cost * 3 / 1000);
            if (level.setBlockAndUpdate(pos, filled)) tank.remove(cost);
        }
    }

    /**
     * Original MultiTileEntityPipeFluid.breakBlock: try each connected face while its covers
     * still exist, count only delivered fluid, then discard the residual contents.
     */
    public void dumpFluidsToAdjacent() {
        if (level == null || level.isClientSide) return;
        for (Direction side : Direction.values()) {
            if (!connected(side)
                    || !com.gregtech.gregtech.content.cover.ComponentCoverRuntime.allowsFluid(getCover(side), false)
                    || !level.hasChunkAt(worldPosition.relative(side))) continue;
            BlockEntity be = level.getBlockEntity(worldPosition.relative(side));
            if (be == null) continue;
            IFluidHandler target = be.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite()).orElse(null);
            if (target == null) continue;
            for (FluidTankGT tank : tanks) {
                if (tank.isEmpty() || !coverFluidFilterPermits(side, tank.getFluid())) continue;
                FluidStack offered = tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                int filled = target.fill(offered, IFluidHandler.FluidAction.EXECUTE);
                transferredAmount += tank.remove(filled);
            }
        }
        // Original GarbageGT.trash(mTanks): residual fluid goes to the existing server-wide dump.
        for (FluidTankGT tank : tanks) {
            if (tank.isEmpty()) continue;
            com.gregtech.gregtech.world.GarbageData.get(level).trash(tank.getFluid());
            tank.setEmpty();
        }
    }

    public void printDebug(Player player) {
        StringBuilder sb = new StringBuilder("Pipe ").append(spec.size()).append(": ");
        for (int i = 0; i < tanks.length; i++) {
            FluidStack f = tanks[i].getFluid();
            if (!f.isEmpty()) {
                sb.append("[").append(i).append("] ").append(f.getDisplayName().getString())
                        .append(" ").append(f.getAmount()).append("L, ");
            }
        }
        player.sendSystemMessage(Component.literal(sb.toString()));
    }

    /** Magnifying glass: show this pipe's contents and total network contents in chat. */
    public void printNetworkInfo(Player player) {
        if (level == null) return;

        // Show this pipe's contents first
        boolean pipeHasFluid = false;
        for (FluidTankGT tank : tanks) {
            FluidStack f = tank.getFluid();
            if (!f.isEmpty()) {
                pipeHasFluid = true;
                player.sendSystemMessage(Component.empty()
                        .append(Component.literal(formatLargeNumber(f.getAmount()) + " L of ")
                                .withStyle(ChatFormatting.WHITE))
                        .append(f.getDisplayName().copy().withStyle(ChatFormatting.AQUA)));
            }
        }

        // BFS scan connected fluid pipe network, sum all fluids
        Set<BlockPos> visited = new HashSet<>();
        Deque<FluidPipeBlockEntity> queue = new ArrayDeque<>();
        visited.add(worldPosition);
        queue.add(this);

        // Aggregate fluid amounts by fluid type
        java.util.Map<net.minecraft.world.level.material.Fluid, FluidStack> networkFluids = new java.util.LinkedHashMap<>();

        while (!queue.isEmpty()) {
            FluidPipeBlockEntity current = queue.poll();
            // Sum fluids from this pipe
            for (FluidTankGT tank : current.tanks) {
                FluidStack f = tank.getFluid();
                if (!f.isEmpty()) {
                    networkFluids.merge(f.getFluid(), f.copy(), (a, b) -> {
                        a.grow(b.getAmount());
                        return a;
                    });
                }
            }
            // Enqueue connected neighbors
            BlockState curState = current.getBlockState();
            for (Direction side : Direction.values()) {
                if (!curState.getValue(FluidPipeBlock.propFor(side))) continue;
                BlockPos neighborPos = current.worldPosition.relative(side);
                if (visited.contains(neighborPos)) continue;
                BlockEntity be = level.getBlockEntity(neighborPos);
                if (be instanceof FluidPipeBlockEntity neighborPipe) {
                    visited.add(neighborPos);
                    queue.add(neighborPipe);
                }
            }
        }

        if (networkFluids.isEmpty()) {
            player.sendSystemMessage(Component.literal("=== This Fluid Pipe Network is empty ===")
                    .withStyle(ChatFormatting.WHITE));
        } else {
            player.sendSystemMessage(Component.literal("=== This Fluid Pipe Network contains: ===")
                    .withStyle(ChatFormatting.WHITE));
            for (FluidStack f : networkFluids.values()) {
                player.sendSystemMessage(Component.empty()
                        .append(Component.literal(formatLargeNumber(f.getAmount()) + " L of ")
                                .withStyle(ChatFormatting.WHITE))
                        .append(f.getDisplayName().copy().withStyle(ChatFormatting.AQUA)));
            }
        }
    }

    private static String formatLargeNumber(long value) {
        if (value >= 1_000_000) {
            return String.format(java.util.Locale.ROOT, "%,d", value).replace(',', '_');
        }
        if (value >= 1000) {
            return String.format(java.util.Locale.ROOT, "%,d", value);
        }
        return Long.toString(value);
    }

    // === IFluidHandler ===

    @Override
    public int getTanks() { return tanks.length; }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank >= 0 && tank < tanks.length ? tanks[tank].getFluid() : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank >= 0 && tank < tanks.length ? tanks[tank].getCapacity() : 0;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return tank >= 0 && tank < tanks.length && tanks[tank].isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;
        int channel = fillableChannel(resource);
        return channel < 0 ? 0 : tanks[channel].fill(resource, action);
    }

    /** GT6 and the wolfram port both reserve an existing fluid channel before any empty one. */
    private int fillableChannel(FluidStack resource) {
        if (resource.isEmpty()) return -1;
        return FluidPipeChannels.select(tanks.length, i -> {
            FluidStack existing = tanks[i].getFluid();
            return !existing.isEmpty() && existing.isFluidEqual(resource);
        }, i -> tanks[i].isEmpty());
    }

    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        for (FluidTankGT tank : tanks) {
            FluidStack existing = tank.getFluid();
            if (!existing.isEmpty() && existing.isFluidEqual(resource)) {
                return tank.drain(resource, action);
            }
        }
        return FluidStack.EMPTY;
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        for (FluidTankGT tank : tanks) {
            if (!tank.isEmpty()) {
                return tank.drain(maxDrain, action);
            }
        }
        return FluidStack.EMPTY;
    }

    // === Capability ===

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (side != null) return faceHandler(side).cast();
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    private boolean connected(Direction side) {
        return !isRemoved() && getBlockState().getValue(FluidPipeBlock.propFor(side));
    }

    /** The cached sided view, allocated on first capability request. */
    @SuppressWarnings("unchecked")
    private LazyOptional<IFluidHandler> faceHandler(Direction side) {
        if (faceHandlers == null) faceHandlers = new LazyOptional[6];
        LazyOptional<IFluidHandler> cached = faceHandlers[side.ordinal()];
        if (cached == null) {
            FaceFluidHandler face = new FaceFluidHandler(this, side);
            cached = LazyOptional.of(() -> face);
            faceHandlers[side.ordinal()] = cached;
        }
        return cached;
    }

    private void invalidateFaceHandler(Direction side) {
        if (faceHandlers == null) return;
        LazyOptional<IFluidHandler> cached = faceHandlers[side.ordinal()];
        if (cached == null) return;
        cached.invalidate();
        faceHandlers[side.ordinal()] = null;
    }

    /**
     * GT6 {@code CoverFilterFluid:117-129} asked about the cover on {@code side}; the pipe's copy of
     * {@code BasicMachineBlockEntity.coverFluidFilterPermits:1591-1594}. {@code true} means "may
     * pass", which is the inverse of the original's {@code interceptFluidFill} /
     * {@code interceptFluidDrain}.
     *
     * <p>The §108 rule holds here, verified by reading the implementation rather than assuming it:
     * {@code CoverItems.behavior(ItemStack.EMPTY)} is {@code null} ({@code CoverItems:53}), so
     * {@link CoverUtilityBehaviors#fluidFilterPermits}'s first line,
     * {@code FILTER_FLUID.equals(CoverItems.behavior(cover))}, is false for a face with <b>no cover at
     * all</b> and for a face whose cover is not a fluid filter — both return {@code true}, i.e.
     * permit, without looking at the candidate. An <em>empty filter stack on a real fluid filter
     * cover</em> is the other case and is not permissive: it returns
     * {@code MachineCoverSpec.inverted(cover)}, false in the default whitelist mode and true only in
     * the inverted one — GT6 {@code CoverFilterFluid:120}
     * {@code return aData.mVisuals[aCoverSide] == 0;}. A suspended cover (a controller cover holding
     * the pipe) refuses everything, {@code :119}.</p>
     */
    public boolean coverFluidFilterPermits(Direction side, FluidStack candidate) {
        return (panels==null||!panels.shuttered(side)) && CoverUtilityBehaviors.fluidFilterPermits(getCover(side), panelsStopped(), candidate);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
        if (faceHandlers != null) {
            for (int i = 0; i < faceHandlers.length; i++) {
                if (faceHandlers[i] != null) { faceHandlers[i].invalidate(); faceHandlers[i] = null; }
            }
        }
    }

    // === NBT ===

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        temperature = tag.contains(NBT_TEMPERATURE) ? tag.getLong(NBT_TEMPERATURE) : GregTechConstants.DEF_ENV_TEMP;
        transferredAmount = tag.contains(NBT_TRANSFERRED) ? tag.getLong(NBT_TRANSFERRED) : 0;
        for (int i = 0; i < tanks.length && i < lastReceivedFrom.length; i++) {
            lastReceivedFrom[i] = tag.contains(NBT_LAST_RECEIVED + i) ? tag.getByte(NBT_LAST_RECEIVED + i) : 0;
            if (tag.contains(NBT_TANK_PREFIX + i)) {
                // The registered pipe owns capacity. Preserve overfull legacy contents until drained.
                CompoundTag contents = tag.getCompound(NBT_TANK_PREFIX + i).copy();
                contents.putLong("Capacity", spec.capacity());
                tanks[i].readFromNBT(contents);
            }
        }
        loadCovers(tag);
    }

    /**
     * One cover per face under the machine's own keys, written only for non-empty faces — the same
     * shape as {@code BasicMachineBlockEntity.saveCovers:1296-1302}, and skipped entirely while no
     * cover was ever attached (the array is still null), so a coverless pipe writes nothing.
     */
    private void saveCovers(CompoundTag tag) {
        if (covers == null) return;
        for (int i = 0; i < covers.length; i++) {
            if (!covers[i].isEmpty()) tag.put(NBT_COVER_PREFIX + i, covers[i].save(new CompoundTag()));
        }
    }

    /**
     * {@code BasicMachineBlockEntity.loadCovers:1304-1310}, with one difference the client sync
     * needs: a tag that names no cover <em>clears</em> the faces, so a cover removed on the server
     * disappears on the client through {@link #handleUpdateTag} instead of lingering. A tag without
     * any cover key and a pipe that never had one stay allocation-free.
     */
    private void loadCovers(CompoundTag tag) {
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
                    ? ItemStack.of(tag.getCompound(NBT_COVER_PREFIX + i)) : ItemStack.EMPTY;
            any |= !covers[i].isEmpty();
        }
        hasCovers = any;
        // GT6 re-reads each cover's stored values on load; only worth doing when one is there.
        if (any) panels().loaded();
        for(var side:Direction.values())com.gregtech.gregtech.content.cover.ComponentCoverRuntime.attached(this,side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong(NBT_TEMPERATURE, temperature);
        tag.putLong(NBT_TRANSFERRED, transferredAmount);
        for (int i = 0; i < tanks.length; i++) {
            tag.putByte(NBT_LAST_RECEIVED + i, lastReceivedFrom[i]);
            CompoundTag tankTag = new CompoundTag();
            tanks[i].writeToNBT(tankTag);
            tag.put(NBT_TANK_PREFIX + i, tankTag);
        }
        saveCovers(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putLong(NBT_TEMPERATURE, temperature);
        tag.putLong(NBT_TRANSFERRED, transferredAmount);
        for (int i = 0; i < tanks.length; i++) {
            CompoundTag tankTag = new CompoundTag();
            tanks[i].writeToNBT(tankTag);
            tag.put(NBT_TANK_PREFIX + i, tankTag);
        }
        saveCovers(tag);
        return tag;
    }

    /**
     * The pipe had no update packet before this batch, so covers would never have reached the client
     * renderer; {@code BasicMachineBlockEntity:1326-1335} is the machine's pair of the same two
     * methods, and the payload is the update tag above.
     */
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) loadCovers(pkt.getTag());
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains(NBT_TEMPERATURE)) temperature = tag.getLong(NBT_TEMPERATURE);
        if (tag.contains(NBT_TRANSFERRED)) transferredAmount = tag.getLong(NBT_TRANSFERRED);
        for (int i = 0; i < tanks.length; i++) {
            if (tag.contains(NBT_TANK_PREFIX + i)) {
                // The registered pipe owns capacity. Preserve overfull legacy contents until drained.
                CompoundTag contents = tag.getCompound(NBT_TANK_PREFIX + i).copy();
                contents.putLong("Capacity", spec.capacity());
                tanks[i].readFromNBT(contents);
            }
        }
        loadCovers(tag);
    }

    public long getTransferredAmount() { return transferredAmount; }
    public long getTemperature() { return temperature; }

    /** Container transactions use the clicked face, including its connection, filter and backflow rules. */
    public net.minecraft.world.InteractionResult handleUse(Player player, net.minecraft.world.InteractionHand hand,
                                                           Direction side) {
        if (level == null || level.isClientSide) return net.minecraft.world.InteractionResult.PASS;
        if (!com.gregtech.gregtech.platform.forge.transport.FluidContainerInteraction.use(player, hand, faceHandler(side).orElseThrow(() -> new IllegalStateException("Missing pipe face handler"))))
            return net.minecraft.world.InteractionResult.PASS;
        setChanged();
        return net.minecraft.world.InteractionResult.CONSUME;
    }
    // ── Client model data (dynamic pipe model neighbor sizes) ───────────────

    @Override
    public net.minecraftforge.client.model.data.ModelData getModelData() {
        float[] halves = new float[6];
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            halves[d.ordinal()] = level == null
                    ? (float) com.gregtech.gregtech.api.machine.PipeGeometry.NONE
                    : (float) com.gregtech.gregtech.api.machine.PipeGeometry.fluidNeighborHalf(
                            level.getBlockState(worldPosition.relative(d)), d);
        }
        return net.minecraftforge.client.model.data.ModelData.builder()
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

    // ── The face-aware view a fluid filter needs ─────────────────────────────

    /**
     * Sided capability view: checks current connections and covers on every operation, including
     * calls through a cached handler after a wrench or cover change. Only executed fills mark the
     * receiving channel for backflow prevention. Unsided internal access remains available.
     * Player container interaction uses this same face view.
     */
    @Override public IFluidHandler componentFluids(Direction side) { return new FaceFluidHandler(this,side,true); }

    private static final class FaceFluidHandler implements IFluidHandler {
        private final boolean internal;
        private final FluidPipeBlockEntity pipe;
        private final Direction side;

        FaceFluidHandler(FluidPipeBlockEntity pipe, Direction side) {
            this(pipe,side,false);
        }
        FaceFluidHandler(FluidPipeBlockEntity pipe,Direction side,boolean internal) {
            this.pipe=pipe;this.side=side;this.internal=internal;
        }

        @Override public int getTanks() { return pipe.getTanks(); }

        @NotNull @Override
        public FluidStack getFluidInTank(int tank) { return pipe.getFluidInTank(tank); }

        @Override public int getTankCapacity(int tank) { return pipe.getTankCapacity(tank); }

        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return pipe.isFluidValid(tank, stack);
        }

        /** GT6 {@code CoverFilterFluid:117-122} {@code interceptFluidFill}: refuse, do not partially fill. */
        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!internal && (!com.gregtech.gregtech.content.cover.ComponentCoverRuntime.allowsFluid(pipe.getCover(side),true) || !pipe.connected(side) || !pipe.coverFluidFilterPermits(side, resource))) return 0;
            int channel = pipe.fillableChannel(resource);
            if (channel < 0) return 0;
            int accepted = pipe.tanks[channel].fill(resource, action);
            if (accepted > 0 && action.execute()) pipe.lastReceivedFrom[channel] |= (byte) (1 << side.ordinal());
            return accepted;
        }

        /** GT6 {@code CoverFilterFluid:124-129} {@code interceptFluidDrain}, asked with the stack offered. */
        @NotNull @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!internal && (!com.gregtech.gregtech.content.cover.ComponentCoverRuntime.allowsFluid(pipe.getCover(side),false) || !pipe.connected(side) || !pipe.coverFluidFilterPermits(side, resource))) return FluidStack.EMPTY;
            return pipe.drain(resource, action);
        }

        /**
         * The unsided drain asks about a fluid the caller has not named, so the candidate is the
         * fluid of the tank the pipe would empty — the first non-empty one, which is exactly the
         * tank {@link FluidPipeBlockEntity#drain(int, FluidAction)} picks.
         */
        @NotNull @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!internal && (!com.gregtech.gregtech.content.cover.ComponentCoverRuntime.allowsFluid(pipe.getCover(side),false) || !pipe.connected(side) || !pipe.coverFluidFilterPermits(side, pipe.firstDrainCandidate()))) return FluidStack.EMPTY;
            return pipe.drain(maxDrain, action);
        }
    }

    /** The fluid {@link #drain(int, FluidAction)} would take, without draining it. */
    private FluidStack firstDrainCandidate() {
        for (FluidTankGT tank : tanks) {
            if (!tank.isEmpty()) return tank.getFluid();
        }
        return FluidStack.EMPTY;
    }

}
