package gregtech.content.transport.pipe.fluid;

import gregtech.api.machine.BlockFace;

import gregtech.engine.fluid.FluidPipeChannels;
import gregtech.engine.fluid.FluidPipeSafety;
import gregtech.engine.fluid.PipeIgnition;
import gregtech.content.registry.GTFluids;

import gregtech.engine.fluid.FluidHazards;
import gregtech.engine.fluid.FluidTankGT;
import gregtech.engine.machine.PipeSpec;
import gregtech.engine.stack.NbtCompat;
import gregtech.engine.stack.StackCompat;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.cover.CoverAttachmentBehaviors;
import gregtech.content.cover.CoverItems;
import gregtech.content.cover.CoverUtilityBehaviors;
import gregtech.content.cover.PanelCoverRuntime;
import gregtech.content.data.GregTechConstants;
import gregtech.content.transport.pipe.PipeAutoConnections;
import gregtech.content.transport.pipe.PipeBlockEntity;

import gregtech.content.registry.GTBlockEntities;
import gregtech.util.GTEntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.level.material.Fluid;
import gregtech.engine.fluid.FluidContainerInteraction;
import gregtech.engine.fluid.FluidTransportEnvironment;
import net.minecraft.world.phys.AABB;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.capabilities.Capabilities;


import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * GT6 fluid pipe tile entity. Single class handles all sizes via {@link PipeSpec}.
 *
 * <h2>Covers: a fluid pipe is a cover host, the way GT6's own pipe is</h2>
 *
 * <p>GT6 attaches covers to <em>tiles</em>, and two of its covers are pipe covers first and machine
 * covers second: the pressure valve, whose {@code interceptCoverPlacement} refuses any host that is
 * not a fluid pipe with exactly one tank ({@code CoverPressureValve}), and the fluid filter, whose
 * {@code interceptFluidFill}/{@code interceptFluidDrain} are asked from the pipe's own fill path
 * ({@code CoverFilterFluid}). The native cover host follows these rules:</p>
 *
 * <ul>
 *   <li><b>No switch/mode control.</b> The pipe is not a {@code MachineControl.Provider}.
 *       Its separate {@code ProgressSensorSource} reports the native transferred-volume
 *       counter, so progress covers work without inventing machine on/off or running state.
 *       Fluid attachments continue using the host's tank and world interfaces.</li>
 *   <li><b>No inventory.</b> The item retriever moves items into the host's own slots
 *       ({@code CoverUtilityBehaviors.tickRetriever}'s {@code target}); a pipe has no slots, so a
 *       retriever attached to one never acts.</li>
 *   <li><b>Lazily allocated cover state.</b> See the field comments below.</li>
 * </ul>
 *
 * <h2>Which tank a face sees: none — tanks do not map to faces, in GT6 either</h2>
 *
 * <p>GT6's {@code MultiTileEntityPipeFluid.getFluidTanks2(aSide)} returns the whole {@code mTanks}
 * array for <em>every</em> side ({@code MultiTileEntityPipeFluid}), and its
 * {@code getFluidTankFillable2} picks the first tank already holding that fluid and otherwise the
 * first empty one. A face only gates <em>connectivity</em>
 * ({@code canAcceptFluidsFrom}/{@code canEmitFluidsTo}). The port is the same
 * shape: sided handlers gate connectivity, filters and channel-local backflow, while unsided
 * access serves the pipe's own cover operations. All faces see the same channels. The one
 * exception is the pressure valve, which hard-codes {@code mTanks[0]}
 * ({@code CoverPressureValve}) and can only be attached when the pipe has a single tank, so
 * {@code tanks[0]} and "the pipe" coincide there.</p>
 */
public class FluidPipeBlockEntity extends PipeBlockEntity<IFluidHandler> implements IFluidHandler, gregtech.api.sensor.CompressionSensorSource, gregtech.api.sensor.TemperatureSource, gregtech.api.sensor.ProgressSensorSource {
    @Override public long gibblValue(BlockFace side) {
        long total = 0;
        for (var tank : tanks) total += tank.getAmount();
        return total;
    }
    @Override public long gibblMaximum(BlockFace side) { return spec.capacity() * tanks.length; }
    private final PipeSpec spec;
    private final FluidTankGT[] tanks;
    private final byte[] lastReceivedFrom;
    private long temperature = GregTechConstants.DEF_ENV_TEMP;
    private long transferredAmount;
    /** Set on load; the next server tick repairs half-connections that the load order hid. */
    private boolean reconnectPending = true;
    /** Next tick at which the component may run a balanced-distribution pass. */
    private long balanceDue;
    /** Scheduler bookkeeping: generation and order of the block-entity pass that offered this pipe. */
    private long schedulerGeneration = -1;
    private long schedulerRank;
    /** Shared-pool snapshot membership; null until the first pool operation. */
    private FluidPipeNetwork.Membership membership;

    private static final String NBT_TEMPERATURE = "gt.temperature";
    private static final String NBT_TRANSFERRED = "gt.transferred";
    private static final String NBT_LAST_RECEIVED = "gt.last_received";
    private static final String NBT_TANK_PREFIX = "gt.tank.";


    /**
     * Local counter advanced by the covered tick pass. It is not the original global
     * SERVER_TIME used by CoverDrain and CoverVent and is not persisted by this pipe.
     */
    private long coverTicks;

    public FluidPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.spec = specFromState(state);
        int count = spec.tankCount();
        this.tanks = new FluidTankGT[count];
        for (int i = 0; i < count; i++) {
            this.tanks[i] = new FluidTankGT(spec.capacity())
                    .setOnChanged(this::onTankChanged)
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

    public FluidPipeNetwork network() {
        return membership == null ? null : membership.network();
    }

    void membership(FluidPipeNetwork.Membership membership) {
        this.membership = membership;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidPipeBlockEntity be) {
        if (!level.isClientSide) be.tickServer(state);
    }

    private void tickServer(BlockState state) {
        if (reconnectPending) {
            reconnectPending = false;
            autoConnectOnPlace(null);
        }
        long temperatureBefore = temperature;
        boolean transferredBefore = transferredAmount != 0;
        boolean receivedBefore = false;
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
            receivedBefore |= lastReceivedFrom[i] != 0;
            lastReceivedFrom[i] = 0;
        }
        if (firstFluid) temperature = FluidPipeSafety.emptyTemperature(temperature, environmentTemperature());
        // Zero cost when unused: `hasCovers` is false for every coverless pipe, so this is a single
        // field read and returns — no face is walked, nothing is allocated, nothing is dispatched.
        if (coverStore.hasCovers()) tickCovers();
        if (gregtech.GregTechConfig.fluidDistribution() == gregtech.GregTechConfig.FluidDistribution.BALANCED
                && level.getGameTime() >= balanceDue) {
            FluidPipeBalancer.balanceComponent(level, this);
        }
        // Every tank mutation already marks the pipe through its change callback. Marking an idle pipe
        // every tick would dirty its chunk and run the comparator neighbour update for nothing, so only
        // the state this method derives itself is checked here.
        if (temperature != temperatureBefore || transferredAmount != 0 || transferredBefore || receivedBefore) setChanged();
    }

    /** Stored fluid, backflow state, covers, reconnect and residual heat all keep a pipe awake. */
    public boolean isIdle() {
        if (reconnectPending || coverStore.hasCovers()) return false;
        for (int i = 0; i < tanks.length; i++) {
            if (!tanks[i].isEmpty() || lastReceivedFrom[i] != 0) return false;
        }
        return temperature == environmentTemperature();
    }

    long schedulerGeneration() { return schedulerGeneration; }

    long schedulerRank() { return schedulerRank; }

    final void deferBalanceUntil(long tick) { balanceDue = tick; }

    final FluidTankGT[] channelTanks() { return tanks; }

    void schedulerOffered(long generation, long rank) {
        schedulerGeneration = generation;
        schedulerRank = rank;
    }

    private void onTankChanged() {
        setChanged();
        // Waking this pipe is enough: its next pass distributes, which wakes the neighbours that
        // actually receive fluid. Avoiding a six-face walk per tank mutation keeps bulk moves cheap.
        FluidPipeTickPhases.wake(this);
    }

    private long environmentTemperature() {
        return FluidTransportEnvironment.environmentTemperature(level, worldPosition);
    }

    // ── Covers: host API (GT6's fluid pipe as a cover host) ──────────────────

    @Override
    public Family pipeFamily() {
        return Family.FLUID;
    }

    /** A pressure valve operates on a single tank. */
    @Override
    public boolean supportsPressureValve() {
        return spec.tankCount() == 1;
    }

    @Override
    public void attached(Direction side, ItemStack stack) {
        invalidatePipeFace(side);
        syncCovers();
        FluidPipeTickPhases.wakeAround(this);
    }

    /** The signals a pipe face can emit are the panel ones; {@code afterTick} publishes them. */
    @Override
    public void removed(Direction side, ItemStack stack) {
        invalidatePipeFace(side);
        panels().afterTick();
        syncCovers();
        FluidPipeTickPhases.wakeAround(this);
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
                        gregtech.content.cover.ComponentCoverRuntime.tick(this,side,level.getGameTime());
                case CoverUtilityBehaviors.PRESSURE_VALVE -> {
                    // CoverPressureValve.onTickPost disconnects even an empty tank before release.
                    if (coverTicks > 2) gregtech.content.cover.CoverConnections.update(this, side, false);
                    CoverUtilityBehaviors.tickPressureValve(level, worldPosition, side, tanks[0], coverTicks);
                }
                case CoverAttachmentBehaviors.DRAIN ->
                        CoverAttachmentBehaviors.tickDrain(level, worldPosition, side, this, coverTicks);
                case CoverAttachmentBehaviors.AIR_VENT ->
                        CoverAttachmentBehaviors.tickVent(level, worldPosition, side, this, coverTicks);
                default -> { }
            }
        }
        runtime.afterTick();
    }

    @Override
    public boolean supportsFilterCover(String behavior) {
        return CoverUtilityBehaviors.FILTER_ITEM.equals(behavior) || CoverUtilityBehaviors.FILTER_FLUID.equals(behavior);
    }

    @Override
    public void wakeAfterFilterChange() {
        FluidPipeTickPhases.wakeAround(this);
    }

    /** GT6 runs magic, gas, plasma and acid independently against the original channel fluid. */
    private boolean checkSafety(FluidStack fluid, int tankIndex) {
        if (fluid.isEmpty() || level == null) return true;
        FluidTankGT tank = tanks[tankIndex];
        Fluid fluidType = fluid.getFluid();
        int magicLoss = FluidHazards.magicLoss(fluidType, spec.protections());
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
        var losses = FluidHazards.pipeLosses(fluidType, spec.protections());
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

    private static final PipeAutoConnections<FluidPipeBlockEntity> AUTO_CONNECTIONS = new PipeAutoConnections<>(
            FluidPipeBlockEntity.class, FluidPipeBlock::propFor, FluidPipeBlockEntity::isFaceManuallyDisconnected,
            FluidPipeBlockEntity::isConnectableNeighbor, FluidPipeBlockEntity::isNativePlacementEndpoint);

    void autoConnectOnPlace(@Nullable Direction clickedFace) {
        AUTO_CONNECTIONS.onPlace(this, clickedFace);
    }

    boolean tryConnectTo(Direction side) {
        return AUTO_CONNECTIONS.tryConnect(this, side);
    }

    /** Ordinary capability endpoints connect only when explicitly clicked during placement. */
    private boolean isNativePlacementEndpoint(Direction side) {
        BlockPos target = worldPosition.relative(side);
        if (level == null || !level.hasChunkAt(target)) return false;
//? if >=1.21.1 {
        /*IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, target, side.getOpposite());
*///?} else {
        BlockEntity entity = level.getBlockEntity(target);
        IFluidHandler handler = entity == null ? null
                : entity.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite()).orElse(null);
//?}
        return handler != null && handler.getTanks() > 0;
    }

    private boolean isConnectableNeighbor(Direction side) {
        BlockPos neighborPos = worldPosition.relative(side);
        BlockState neighborState = level.getBlockState(neighborPos);
        Direction neighborSide = side.getOpposite();
        if (neighborState.getBlock() instanceof gregtech.api.transport.FluidPipeConnectable block
                && block.canConnectFluidPipe(neighborSide)) return true;
        return level.getBlockEntity(neighborPos) instanceof gregtech.api.transport.FluidPipeConnectable host
                && host.canConnectFluidPipe(neighborSide);
    }

    @Override
    protected void manualDisconnectChanged() {
        FluidPipeTickPhases.wakeAround(this);
    }

    private void distributeChannel(BlockState state, FluidTankGT tank, int channel) {
        // Vanilla cauldrons have no block entity. They take priority over ordinary targets.
        fillCauldrons(state, tank);
        if (tank.isEmpty()) return;
        // BALANCED replaces inter-pipe diffusion with its own pass; SHARED keeps the ambient
        // diffusion and adds the component-wide pool for endpoint fill/drain.
        boolean balanced = gregtech.GregTechConfig.fluidDistribution() == gregtech.GregTechConfig.FluidDistribution.BALANCED;
        java.util.List<FluidTankGT> pipes = new java.util.ArrayList<>();
        java.util.List<IFluidHandler> machines = new java.util.ArrayList<>();
        FluidStack fluid = tank.getFluid();
        for (Direction side : Direction.values()) {
            if ((lastReceivedFrom[channel] & (1 << side.ordinal())) != 0
                    || !state.getValue(FluidPipeBlock.propFor(side))
                    || !gregtech.content.cover.ComponentCoverRuntime.allowsFluid(getCover(side),false) || !coverFluidFilterPermits(side, fluid)) continue;
            BlockPos neighborPos = worldPosition.relative(side);
            if (!level.hasChunkAt(neighborPos)) continue;
            BlockEntity be = level.getBlockEntity(neighborPos);
            if (be instanceof FluidPipeBlockEntity neighbor) {
                Direction incoming = side.getOpposite();
                if (!neighbor.getBlockState().getValue(FluidPipeBlock.propFor(incoming))
                        || !gregtech.content.cover.ComponentCoverRuntime.allowsFluid(neighbor.getCover(incoming),true) || !neighbor.coverFluidFilterPermits(incoming, fluid)) continue;
                int targetChannel = neighbor.fillableChannel(fluid);
                if (targetChannel < 0) continue;
                FluidTankGT target = neighbor.tanks[targetChannel];
                if (target.getAmount() >= tank.getAmount()) continue;
                // Original GT6 marks eligible channels before distribution, even if capacity blocks the push.
                if (!balanced) {
                    neighbor.lastReceivedFrom[targetChannel] |= (byte) (1 << incoming.ordinal());
                    pipes.add(level.random.nextInt(pipes.size() + 1), target);
                } else {
                    // Balanced mode only needs the amounts here; the order is never used.
                    pipes.add(target);
                }
            } else {
//? if >=1.21.1 {
                /*IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, side.getOpposite());
*///?} else {
                IFluidHandler handler = be == null ? null : be.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite()).orElse(null);
//?}
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
        if (!balanced) for (FluidTankGT target : pipes) transferToPipe(tank, target, targetLevel - target.getAmount());
        for (IFluidHandler target : machines) {
            if (tank.isEmpty()) break;
            FluidStack offered = tank.drain(FluidTankGT.bindInt(targetLevel), FluidAction.SIMULATE);
            int accepted = target.fill(offered, FluidAction.EXECUTE);
            transferredAmount += tank.remove(accepted);
        }
        if (!balanced) {
            long pressure = FluidPipeChannels.pressureShare(tank.getAmount(), spec.capacity(), pipes.size());
            if (pressure > 0) for (FluidTankGT target : pipes) transferToPipe(tank, target, pressure);
        }
    }

    /** Selected channels already match full fluid identity; keep internal pipe amounts as longs. */
    final long transferToPipe(FluidTankGT source, FluidTankGT target, long requested) {
        long amount = Math.min(Math.min(requested, source.getAmount()), target.capacity() - target.getAmount());
        if (amount <= 0) return 0;
        if (target.isEmpty()) target.setFluid(source.getFluidLong(), amount);
        else target.add(amount);
        transferredAmount += source.remove(amount);
        return amount;
    }

    private void fillCauldrons(BlockState state, FluidTankGT tank) {
        FluidStack fluid = tank.getFluid();
        var entry = gregtech.content.registry.GTFluids.entryForFluid(fluid.getFluid());
        boolean water = fluid.getFluid() == net.minecraft.world.level.material.Fluids.WATER
                || entry != null && (entry.flags() & gregtech.content.data.FluidCatalog.FluidFlags.WATER) != 0;
        if (!water) return;
        for (Direction side : Direction.values()) {
            if (!state.getValue(FluidPipeBlock.propFor(side)) || !gregtech.content.cover.ComponentCoverRuntime.allowsFluid(getCover(side),false) || !coverFluidFilterPermits(side, fluid)) continue;
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
    void dumpFluidsToAdjacent() {
        if (level == null || level.isClientSide) return;
        for (Direction side : Direction.values()) {
            if (!connected(side)
                    || !gregtech.content.cover.ComponentCoverRuntime.allowsFluid(getCover(side), false)
                    || !level.hasChunkAt(worldPosition.relative(side))) continue;
            BlockEntity be = level.getBlockEntity(worldPosition.relative(side));
            if (be == null) continue;
//? if >=1.21.1 {
            /*IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK,
                    worldPosition.relative(side), side.getOpposite());
*///?} else {
            IFluidHandler target = be.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite()).orElse(null);
//?}
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
            gregtech.content.world.GarbageData.get(level).trash(tank.getFluid());
            tank.setEmpty();
        }
    }

    void printDebug(Player player) {
        StringBuilder sb = new StringBuilder("Pipe ").append(spec.size()).append(": ");
        for (int i = 0; i < tanks.length; i++) {
            FluidTankGT tank = tanks[i];
            if (tank.isEmpty()) continue;
            sb.append("[").append(i).append("] ").append(tank.getFluidLong().getDisplayName().getString())
                    .append(" ").append(tank.getAmount()).append(gregtech.engine.fluid.FluidUnits.unit() + ", ");
        }
        player.sendSystemMessage(Component.literal(sb.toString()));
    }

    /** Magnifying glass: show this pipe's contents and total network contents in chat. */
    public void printNetworkInfo(Player player) {
        FluidPipeContents.describe(this).forEach(player::sendSystemMessage);
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
        FluidPipeNetwork network = sharedPoolNetwork();
        if (network != null) return SharedFluidPool.fill(network, resource, action);
        int channel = fillableChannel(resource);
        return channel < 0 ? 0 : tanks[channel].fill(resource, action);
    }

    private @Nullable FluidPipeNetwork sharedPoolNetwork() {
        return gregtech.GregTechConfig.fluidDistribution() == gregtech.GregTechConfig.FluidDistribution.SHARED
                ? SharedFluidPool.networkFrom(this) : null;
    }

    /** GT6 and the wolfram port both reserve an existing fluid channel before any empty one. */
    final int fillableChannel(FluidStack resource) {
        if (resource.isEmpty()) return -1;
        return FluidPipeChannels.select(tanks.length, i -> {
            FluidStack existing = tanks[i].getFluid();
            return !existing.isEmpty() && StackCompat.sameFluid(existing, resource);
        }, i -> tanks[i].isEmpty());
    }

    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        FluidPipeNetwork network = sharedPoolNetwork();
        if (network != null) return SharedFluidPool.drain(network, resource, resource.getAmount(), action);
        for (FluidTankGT tank : tanks) {
            FluidStack existing = tank.getFluid();
            if (!existing.isEmpty() && StackCompat.sameFluid(existing, resource)) return tank.drain(resource, action);
        }
        return FluidStack.EMPTY;
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidPipeNetwork network = sharedPoolNetwork();
        if (network != null) return SharedFluidPool.drain(network, FluidStack.EMPTY, maxDrain, action);
        for (FluidTankGT tank : tanks) {
            if (!tank.isEmpty()) return tank.drain(maxDrain, action);
        }
        return FluidStack.EMPTY;
    }

    @Override
    protected IFluidHandler unsidedPipeHandler() {
        return this;
    }

    @Override
    protected IFluidHandler createFaceHandler(Direction side) {
        return new FaceFluidHandler(this, side);
    }

//? if <1.21.1 {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        return capability == ForgeCapabilities.FLUID_HANDLER
                ? pipeCapability(side).cast()
                : super.getCapability(capability, side);
    }
//?}

    final boolean connected(Direction side) {
        return !isRemoved() && getBlockState().getValue(FluidPipeBlock.propFor(side));
    }

    /**
     * GT6 {@code CoverFilterFluid} asked about the cover on {@code side}; the pipe's
     * counterpart of {@link BasicMachineBlockEntity#coverFluidFilterPermits(Direction, FluidStack)}.
     * {@code true} means "may pass", which is the inverse of the original's
     * {@code interceptFluidFill} / {@code interceptFluidDrain}.
     *
     * <p>A face with <b>no cover at all</b> and a face whose cover is not a fluid filter both permit:
     * {@code CoverItems.behavior(ItemStack.EMPTY)} is {@code null}, so the {@code FILTER_FLUID} test
     * in {@link CoverUtilityBehaviors#fluidFilterPermits} fails and the candidate is never looked at.
     * An <em>empty filter stack on a real fluid filter cover</em> is the other case and is not
     * permissive: it returns {@code MachineCoverSpec.inverted(cover)}, false in the default whitelist
     * mode and true only in the inverted one — GT6 {@code CoverFilterFluid}
     * {@code return aData.mVisuals[aCoverSide] == 0;}. A suspended cover (a controller cover holding
     * the pipe) refuses everything.</p>
     */
    boolean coverFluidFilterPermits(Direction side, FluidStack candidate) {
        return !coverStore.shuttered(side) && CoverUtilityBehaviors.fluidFilterPermits(getCover(side), coverStore.stopped(), candidate,level.registryAccess());
    }

    // === NBT ===

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        temperature = tag.contains(NBT_TEMPERATURE) ? tag.getLong(NBT_TEMPERATURE) : GregTechConstants.DEF_ENV_TEMP;
        transferredAmount = tag.contains(NBT_TRANSFERRED) ? tag.getLong(NBT_TRANSFERRED) : 0;
        loadManualDisconnects(tag);
        for (int i = 0; i < tanks.length && i < lastReceivedFrom.length; i++) {
            lastReceivedFrom[i] = tag.contains(NBT_LAST_RECEIVED + i) ? tag.getByte(NBT_LAST_RECEIVED + i) : 0;
            if (tag.contains(NBT_TANK_PREFIX + i)) {
                // The registered pipe owns capacity. Preserve overfull legacy contents until drained.
                CompoundTag contents = tag.getCompound(NBT_TANK_PREFIX + i).copy();
                contents.putLong("Capacity", spec.capacity());
                NbtCompat.readTank(tanks[i],contents,lookup);
            }
        }
        coverStore.load(tag, lookup);
    }

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        tag.putLong(NBT_TEMPERATURE, temperature);
        tag.putLong(NBT_TRANSFERRED, transferredAmount);
        saveManualDisconnects(tag);
        for (int i = 0; i < tanks.length; i++) {
            tag.putByte(NBT_LAST_RECEIVED + i, lastReceivedFrom[i]);
            CompoundTag tankTag = new CompoundTag();
            NbtCompat.writeTank(tanks[i],tankTag,lookup);
            tag.put(NBT_TANK_PREFIX + i, tankTag);
        }
        coverStore.save(tag, lookup);
    }

    @Override
    protected void writeUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.writeUpdateTag(tag, lookup);
        tag.putLong(NBT_TEMPERATURE, temperature);
        tag.putLong(NBT_TRANSFERRED, transferredAmount);
        for (int i = 0; i < tanks.length; i++) {
            CompoundTag tankTag = new CompoundTag();
            NbtCompat.writeTank(tanks[i],tankTag,lookup);
            tag.put(NBT_TANK_PREFIX + i, tankTag);
        }
        coverStore.save(tag, lookup);
    }

    // Counter lifetime is the existing native pipe tick interval, not tank occupancy.
    @Override public long progressValue(BlockFace side) { return transferredAmount; }
    @Override public long progressMaximum(BlockFace side) { return spec.capacity() * tanks.length; }
    long getTransferredAmount() { return transferredAmount; }
    @Override public long temperatureK(BlockFace side) { return temperature; }
    @Override public long maximumK(BlockFace side) { return spec.maxTemperature(); }
    long getTemperature() { return temperature; }

    /** Container transactions use the clicked face, including its connection, filter and backflow rules. */
    net.minecraft.world.InteractionResult handleUse(Player player, net.minecraft.world.InteractionHand hand,
                                                           Direction side) {
        if (level == null) return net.minecraft.world.InteractionResult.PASS;
        if (level.isClientSide) return FluidContainerInteraction.clientResult(player, hand);
        IFluidHandler face = externalPipeHandler(side);
        if (!FluidContainerInteraction.use(player, hand, face))
            return net.minecraft.world.InteractionResult.PASS;
        setChanged();
        return net.minecraft.world.InteractionResult.CONSUME;
    }
    @Override
    protected double neighborHalf(BlockState state, Direction side) {
        return gregtech.engine.machine.PipeGeometry.fluidNeighborHalf(state, side);
    }

    @Override
    protected int connectionMask(BlockState state) {
        return AUTO_CONNECTIONS.connectionMask(state);
    }

    @Override
    protected void joinedLevel() {
        balanceDue = 0;
        FluidPipeTickPhases.joined(this);
    }

    @Override
    protected void leftLevel() {
        FluidPipeTickPhases.left(this);
        FluidPipeNetworks.left(this);
    }

    @Override
    protected void connectionsChanged() {
        FluidPipeTickPhases.wakeAround(this);
        FluidPipeNetworks.invalidate(this);
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

        /** GT6 {@code CoverFilterFluid} {@code interceptFluidFill}: refuse, do not partially fill. */
        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!internal && (!gregtech.content.cover.ComponentCoverRuntime.allowsFluid(pipe.getCover(side),true) || !pipe.connected(side) || !pipe.coverFluidFilterPermits(side, resource))) return 0;
            int channel = pipe.fillableChannel(resource);
            long before = channel < 0 ? 0 : pipe.tanks[channel].getAmount();
            int accepted = pipe.fill(resource, action);
            // Mark only the entry channel, so a shared-pool fill through another pipe does not
            // invent a backflow mask on this face.
            if (accepted > 0 && action.execute() && channel >= 0 && pipe.tanks[channel].getAmount() > before)
                pipe.lastReceivedFrom[channel] |= (byte) (1 << side.ordinal());
            return accepted;
        }

        /** GT6 {@code CoverFilterFluid} {@code interceptFluidDrain}, asked with the stack offered. */
        @NotNull @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!internal && (!gregtech.content.cover.ComponentCoverRuntime.allowsFluid(pipe.getCover(side),false) || !pipe.connected(side) || !pipe.coverFluidFilterPermits(side, resource))) return FluidStack.EMPTY;
            return pipe.drain(resource, action);
        }

        /** Fluid filter covers compare against the first stored fluid when the caller names none. */
        @NotNull @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!internal) {
                ItemStack cover = pipe.getCover(side);
                if (!pipe.connected(side) || pipe.coverStore.shuttered(side)
                        || !gregtech.content.cover.ComponentCoverRuntime.allowsFluid(cover, false))
                    return FluidStack.EMPTY;
                if (CoverUtilityBehaviors.FILTER_FLUID.equals(CoverItems.behavior(cover))) {
                    FluidStack candidate = SharedFluidPool.firstDrainCandidate(pipe, pipe.sharedPoolNetwork());
                    if (!pipe.coverFluidFilterPermits(side, candidate)) return FluidStack.EMPTY;
                }
            }
            return pipe.drain(maxDrain, action);
        }
    }

}
