package gregtech.content.blockentity.energy;

import gregtech.api.machine.BlockFace;

import gregtech.engine.energy.IEnergyBlock;
import gregtech.engine.energy.IEnergyConductor;
import gregtech.engine.energy.WirePacketRules;
import gregtech.engine.energy.WireSpec;
import gregtech.content.block.energy.ElectricWireBlock;
import gregtech.content.data.GregTechConstants;
import gregtech.content.data.GregTechTags;
import gregtech.content.transport.energy.ElectricNetwork;
import gregtech.content.transport.energy.ElectricNetworks;
//? if >=1.21.1 {
/*import gregtech.content.registry.GTWires;
import net.minecraft.core.HolderLookup;
*///?} else {
import gregtech.content.registry.GTBlockEntities;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Collection;

/** GT6 electric wire block entity (EU cable, pipe-style connection grid). */
public class ElectricWireBlockEntity extends BlockEntity implements IEnergyConductor, IEnergyBlock, gregtech.api.sensor.ProgressSensorSource {
    // Sample the existing transfer accumulator between entity ticks; never substitute energy.
    @Override public long progressValue(BlockFace side) { return transferredAmperes(); }
    @Override public long progressMaximum(BlockFace side) { return amperage(); }
    // GT6 runtime-only wire counters: aggregate transfers between level ticks without a per-wire tick.
    private long counterWindow = Long.MIN_VALUE;
    private long windowAmperes;
    private long windowWattage;
    private long lastWattage;
    private int burnCounter;
    private long timerBase;
    private long decayedSteps;
    private ElectricNetwork.Membership membership;

    public ElectricWireBlockEntity(BlockPos pos, BlockState state) {
//? if >=1.21.1 {
        /*super(GTWires.ELECTRIC_WIRE.get(), pos, state);
*///?} else {
        super(GTBlockEntities.ELECTRIC_WIRE.get(), pos, state);
//?}
    }

    private WireSpec wireSpec() {
        BlockState st = getBlockState();
        if (st.getBlock() instanceof ElectricWireBlock wb) return wb.spec();
        // fallback defaults
        return new WireSpec("copper", null, 1, 32, 1, 1);
    }

    public long voltage()     { return wireSpec().voltage(); }
    public long amperage()    { return wireSpec().amperage(); }
    public long lossPerBlock(){ return wireSpec().lossPerBlock(); }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null) return;
        if (level.isClientSide) {
            requestModelDataUpdate();
            gregtech.engine.machine.PipeModelData.refreshCrossChunkNeighbors(level, worldPosition);
        } else {
            // Counters and the burn cooldown are runtime-only and restart with the block entity.
            counterWindow = Long.MIN_VALUE;
            windowAmperes = 0;
            windowWattage = 0;
            lastWattage = 0;
            burnCounter = 0;
            timerBase = level.getGameTime();
            decayedSteps = 0;
            ElectricNetworks.joined(this);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) ElectricNetworks.left(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide) ElectricNetworks.left(this);
        super.onChunkUnloaded();
    }

    /** Only the six connection properties are layout; waterlogging and the like are not. */
    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState state) {
        int before = connectionMask(getBlockState());
        super.setBlockState(state);
        if (level != null && !level.isClientSide && connectionMask(state) != before) ElectricNetworks.invalidate(this);
    }

    private static int connectionMask(BlockState state) {
        int mask = 0;
        for (Direction face : Direction.values()) {
            if (state.getValue(ElectricWireBlock.propFor(face))) mask |= 1 << face.ordinal();
        }
        return mask;
    }

    public ElectricNetwork network() {
        return membership == null ? null : membership.network();
    }

    public void membership(ElectricNetwork.Membership membership) {
        this.membership = membership;
    }

    /**
     * Auto-connect on placement: connects to the clicked face, then scans all 6 sides
     * and auto-connects to any adjacent same-type wire that already has its connection
     * facing toward this new wire (symmetry healing).
     */
    public void autoConnectOnPlace(@Nullable Direction clickedFace) {
        if (level == null || level.isClientSide) return;

        // First, connect to the clicked face
        if (clickedFace != null) {
            Direction toNeighbor = clickedFace.getOpposite();
            BlockPos neighborPos = worldPosition.relative(toNeighbor);
            BlockEntity be = canConnectOnPlacement(toNeighbor) ? level.getBlockEntity(neighborPos) : null;
            if (be instanceof ElectricWireBlockEntity neighborWire) {
                BlockState state = getBlockState();
                BlockState newState = state.setValue(ElectricWireBlock.propFor(toNeighbor), true);
                level.setBlockAndUpdate(worldPosition, newState);

                BlockState neighborState = neighborWire.getBlockState();
                if (!neighborState.getValue(ElectricWireBlock.propFor(clickedFace))) {
                    level.setBlockAndUpdate(neighborPos,
                            neighborState.setValue(ElectricWireBlock.propFor(clickedFace), true));
                }
            } else if (be != null) {
                var endpoint = gregtech.engine.energy.EnergyBlockViews.view(be);
                if (endpoint != null && (endpoint.isEnergyAcceptingFrom(GregTechTags.Energy.EU, clickedFace, true)
                        || endpoint.isEnergyEmittingTo(GregTechTags.Energy.EU, clickedFace, true))) {
                    BlockState state = getBlockState();
                    level.setBlockAndUpdate(worldPosition, state.setValue(ElectricWireBlock.propFor(toNeighbor), true));
                }
            }
        }

        // Then scan all 6 sides for adjacent wires already connected toward us
        for (Direction dir : Direction.values()) {
            if (!canConnectOnPlacement(dir)) continue;
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(neighborPos);
            if (!(be instanceof ElectricWireBlockEntity neighborWire)) continue;

            BlockState neighborState = neighborWire.getBlockState();
            if (neighborState.getValue(ElectricWireBlock.propFor(dir.getOpposite()))) {
                BlockState state = getBlockState();
                if (!state.getValue(ElectricWireBlock.propFor(dir))) {
                    level.setBlockAndUpdate(worldPosition, state.setValue(ElectricWireBlock.propFor(dir), true));
                }
            }
        }
    }

    private boolean canConnectOnPlacement(Direction side) {
        return level != null && level.hasChunkAt(worldPosition.relative(side))
                && gregtech.content.cover.CoverConnections.canConnect(level, worldPosition, side);
    }

    @Override
//? if >=1.21.1 {
    /*public void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);
*///?} else {
    public void load(CompoundTag tag) {
        super.load(tag);
//?}
    }

    @Override
//? if >=1.21.1 {
    /*public void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);
*///?} else {
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
//?}
    }

    // --- IEnergyBlock ---

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        return energyType == GregTechTags.Energy.EU;
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return GregTechTags.Energy.EU.asList();
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return energyType == GregTechTags.Energy.EU && !isRemoved() && side != null && getBlockState().getValue(ElectricWireBlock.propFor(side));
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return isEnergyAcceptingFrom(energyType,side,theoretical);
    }

    @Override
    public long doEnergyInjection(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doInject) {
        if (level == null || level.isClientSide || size == 0 || size == Long.MIN_VALUE || amount <= 0
                || !isEnergyAcceptingFrom(energyType,side,false)) return 0;
        // GT6's simulation reports local connection acceptance, not a reservation of downstream space.
        if (!doInject) return amount;
        return ElectricNetworks.inject(this, side, size, amount);
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long size) { return 0; }

    @Override
    public long doEnergyExtraction(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doExtract) {
        return 0;
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag energyType, @Nullable Direction side, long size) { return 0; }

    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag energyType, @Nullable Direction side) { return 0; }

    @Override
    public long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable Direction side) { return 0; }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) { return voltage(); }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) { return voltage(); }

    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable Direction side) { return voltage(); }

    @Override
    public long getEnergySizeOutputMax(GregTechTags.Tag energyType, @Nullable Direction side) { return voltage(); }

    /**
     * Publish the previous level tick's totals and apply the burn cooldown that the per-wire tick
     * used to advance. Reads derive everything from the current window, so an idle wire needs no tick.
     */
    private void rollCounters(long now) {
        if (counterWindow == now) return;
        lastWattage = counterWindow == now - 1 ? windowWattage : 0;
        counterWindow = now;
        windowAmperes = 0;
        windowWattage = 0;
        long elapsed = now - timerBase;
        long due = elapsed < 2 ? 0 : (elapsed - 2) / 512 + 1;
        if (due > decayedSteps) {
            long steps = due - decayedSteps;
            burnCounter = steps >= burnCounter ? 0 : burnCounter - (int) steps;
            decayedSteps = due;
        }
    }

    private void rollIfServer() {
        if (level != null && !level.isClientSide) rollCounters(level.getGameTime());
    }

    public boolean isConducting() {
        rollIfServer();
        return level != null && !isRemoved() && lastWattage > 0;
    }

    /** GT6 mWattageLast: actual post-loss wattage accumulated during the previous tick. */
    public long lastWattage() { rollIfServer(); return lastWattage; }
    public long transferredAmperes() { rollIfServer(); return windowAmperes; }
    public int burnCounter() { rollIfServer(); return burnCounter; }

    /** False means an overload consumed the remaining offered packets, as in GT6. */
    public boolean addToEnergyTransferred(long packetVoltage, long packetAmperage) {
        if (packetAmperage <= 0 || packetVoltage == Long.MIN_VALUE || packetVoltage == 0) return true;
        rollIfServer();
        long magnitude = Math.abs(packetVoltage);
        windowAmperes = saturatedAdd(windowAmperes, packetAmperage);
        long watts = WirePacketRules.wattage(magnitude,packetAmperage);
        windowWattage = saturatedAdd(windowWattage, watts);
        if (magnitude > voltage() || windowAmperes > amperage()) {
            if (burnCounter < 16) burnCounter++;
            if (burnCounter >= 16) ElectricNetworks.scheduleBurn(this);
            return false;
        }
        return true;
    }

    private static long saturatedAdd(long left, long right) {
        return WirePacketRules.saturatedAdd(left,right);
    }

    // --- IEnergyConductor ---

    @Override public boolean isEnergyConducting(GregTechTags.Tag energyType) { return energyType == GregTechTags.Energy.EU; }
    @Override public long getEnergyMaxSize(GregTechTags.Tag energyType) { return energyType == GregTechTags.Energy.EU ? voltage() : 0; }
    @Override public long getEnergyMaxPackets(GregTechTags.Tag energyType) { return energyType == GregTechTags.Energy.EU ? amperage() : 0; }
    @Override public long getEnergyLossPerMeter(GregTechTags.Tag energyType) { return energyType == GregTechTags.Energy.EU ? lossPerBlock() : 0; }
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
                    : (float) gregtech.engine.machine.PipeGeometry.wireHalfOf(
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
