package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import com.gregtech.gregtech.api.multiblock.MultiblockPortOwner;
import com.gregtech.gregtech.api.multiblock.PartBindings;
import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.inventory.BlockContents;
import com.gregtech.gregtech.block.machine.LogisticsCoreControllerBlock;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.content.logistics.LogisticsHost;
import com.gregtech.gregtech.content.logistics.LogisticsCoverHost;
import com.gregtech.gregtech.content.logistics.LogisticsCovers;
import com.gregtech.gregtech.content.logistics.LogisticsCoverType;
import com.gregtech.gregtech.content.logistics.LogisticsCoverHost;
import com.gregtech.gregtech.content.logistics.LogisticsCpuDisplay;
import com.gregtech.gregtech.content.logistics.LogisticsNetwork;
import com.gregtech.gregtech.content.logistics.LogisticsCoreStructure;
import com.gregtech.gregtech.content.logistics.LogisticsRoutingPass;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/** GT6 Logistics Core structure, EU input, network discovery and private 108-slot buffers. */
public final class LogisticsCoreControllerBlockEntity extends GTEnergyBlockEntity
        implements MultiblockPortOwner, LogisticsCoverHost, BlockContents {
    public static final int BUFFER_SLOTS = LogisticsCoreStructure.MAX_STORAGE_CPU_COUNT;
    public static final int TANK_CAPACITY = 16_000;
    private final PartBindings<BlockPos, MultiblockLayout.Role> bindings = new PartBindings<>();
    private final ItemStackHandler items = new ItemStackHandler(BUFFER_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final FluidTankGT[] tanks = makeTanks();
    private final LogisticsCovers covers = new LogisticsCovers(this, this);
    private LogisticsCoreStructure.Counts processors;
    private LogisticsNetwork.Snapshot network = LogisticsNetwork.Snapshot.empty();
    /** GT6 oCPU_*: usage from the most recent powered one-second pass. */
    private LogisticsCpuDisplay.Usage usedProcessors = LogisticsCpuDisplay.Usage.ZERO;
    private long energy;

    private FluidTankGT[] makeTanks() {
        var result = new FluidTankGT[BUFFER_SLOTS];
        for (int i = 0; i < result.length; i++)
            result[i] = new FluidTankGT(TANK_CAPACITY).setOnChanged(this::setChanged);
        return result;
    }

    public LogisticsCoreControllerBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.LOGISTICS_CORE.get(), pos, state);
    }

    @Override public boolean isStructureOk() {
        if (level == null || isRemoved()) return false;
        Direction front = getBlockState().getValue(LogisticsCoreControllerBlock.FACING);
        LogisticsCoreStructure.Counts found = LogisticsCoreStructure.counts(level, worldPosition, front);
        if (found == null || !found.valid()) return unbind();

        var candidates = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
        for (var cell : LogisticsCoreStructure.cells()) {
            BlockPos pos = cell.at(worldPosition, front);
            if (!(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity)) return unbind();
            candidates.put(pos, switch (cell.kind()) {
                case WALL -> MultiblockLayout.Role.ENERGY_INPUT;
                case VENT -> MultiblockLayout.Role.LOGISTICS;
                case CPU -> MultiblockLayout.Role.CASING;
            });
        }
        if (!level.isClientSide && !bindings.update(candidates,
                pos -> level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part
                        && part.canBind(worldPosition),
                (pos, role) -> ((MultiblockPortBlockEntity) level.getBlockEntity(pos)).bind(worldPosition, role),
                this::releasePart)) return unbind();
        processors = found;
        return true;
    }

    private boolean unbind() {
        bindings.clear(this::releasePart);
        processors = null;
        network = LogisticsNetwork.Snapshot.empty();
        return false;
    }

    private void releasePart(BlockPos pos) {
        if (level != null && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part)
            part.release(worldPosition);
    }

    @Override public void setRemoved() {
        unbind();
        super.setRemoved();
    }

    @Override public void onChunkUnloaded() {
        unbind();
        super.onChunkUnloaded();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  LogisticsCoreControllerBlockEntity core) {
        core.isStructureOk();
        // GT6 onServerTickPre wraps both routing and its fixed charge in SYNC_SECOND.
        if (level.getGameTime() % 20 != 0) return;
        LogisticsCpuDisplay.Usage previous = core.usedProcessors;
        core.usedProcessors = LogisticsCpuDisplay.Usage.ZERO;
        if (core.processors != null && core.energy >= core.processors.routingThreshold()) {
            core.scanNetwork();
            core.updateDisplays(previous);
            core.routeNetworkOnce(true);
        }
        else core.network = LogisticsNetwork.Snapshot.empty();
        if (!previous.equals(core.usedProcessors)) core.setChanged();
        long cost = core.processors == null ? 20 : core.processors.fixedEnergyPerPass();
        long next = Math.max(0, core.energy - cost);
        if (next != core.energy) {
            core.energy = next;
            core.setChanged();
        }
    }

    public LogisticsCoreStructure.Counts processorCounts() {
        return isStructureOk() ? processors : null;
    }

    /** Explicit read-only discovery for W3 routing; the per-second tick also updates this snapshot. */
    public LogisticsNetwork.Snapshot scanNetwork() {
        if (!isStructureOk()) return network;
        network = LogisticsNetwork.scan(level, worldPosition,
                getBlockState().getValue(LogisticsCoreControllerBlock.FACING),
                LogisticsCoreStructure.range(processors));
        return network;
    }

    /** Fluid and item routes share the same logic-CPU passes and one-second gate. */
    public LogisticsRoutingPass.Result routeNetworkOnce() {
        return routeNetworkOnce(false);
    }

    private LogisticsRoutingPass.Result routeNetworkOnce(boolean networkReady) {
        if (level == null || level.isClientSide || !isStructureOk()
                || energy < processors.routingThreshold()) return new LogisticsRoutingPass.Result(0, 0, 0, 0);
        if (!networkReady) scanNetwork();
        var result = LogisticsRoutingPass.route(level, network, processors.logic(), processors.conversion());
        usedProcessors = new LogisticsCpuDisplay.Usage(
                Math.max(usedProcessors.logic(), result.usedLogic()),
                Math.max(usedProcessors.control(), controlUsage()),
                0, Math.max(usedProcessors.conversion(), result.usedConversion()));
        if (result.energyCost() > 0) {
            energy = Math.max(0, energy - result.energyCost());
        }
        setChanged();
        return result;
    }

    /** Original scan counts every open face within range, even when no adjacent host is found. */
    private int controlUsage() {
        BlockPos center = worldPosition.relative(
                getBlockState().getValue(LogisticsCoreControllerBlock.FACING).getOpposite(),
                LogisticsCoreStructure.RADIUS);
        int range = LogisticsCoreStructure.range(processors);
        int maximum = 0;
        for (BlockPos pos : network.positions()) {
            if (!(level.getBlockEntity(pos) instanceof LogisticsHost host)) continue;
            for (Direction side : Direction.values()) {
                if (!host.canLogistics(side)) continue;
                int distance = LogisticsNetwork.chebyshev(center, pos.relative(side));
                if (distance <= range) maximum = Math.max(maximum, distance - LogisticsCoreStructure.RADIUS);
            }
        }
        return maximum;
    }

    private void updateDisplays(LogisticsCpuDisplay.Usage previous) {
        for (BlockPos pos : network.positions()) {
            if (!(level.getBlockEntity(pos) instanceof LogisticsCoverHost host)) continue;
            for (Direction side : Direction.values()) {
                LogisticsCoverType type = LogisticsCoverType.of(host.logisticsCovers().get(side));
                if (type == null || type.role() != LogisticsCoverType.Role.DISPLAY) continue;
                int total = switch (type) {
                    case CPU_LOGIC -> processors.logic();
                    case CPU_CONTROL -> processors.control();
                    case CPU_STORAGE -> processors.storage();
                    case CPU_CONVERSION -> processors.conversion();
                    default -> 0;
                };
                host.logisticsCovers().setDisplay(side, previous.forCover(type), total);
            }
        }
    }

    public LogisticsCpuDisplay.Usage usedProcessorCounts() { return usedProcessors; }

    public LogisticsNetwork.Snapshot networkSnapshot() { return network; }
    public ItemStackHandler bufferItems() { return items; }
    public FluidTankGT bufferTank(int slot) { return tanks[slot]; }
    public int bufferTankCount() { return tanks.length; }
    @Override public boolean canLogistics(Direction side) { return !isRemoved(); }
    @Override public LogisticsCovers logisticsCovers() { return covers; }

    public long storedEU() { return energy; }

    /** Direct power input to the main block is forbidden; the 44 galvanized walls are the sockets. */
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side) { return List.of(); }
    @Override public boolean isEnergyType(GregTechTags.Tag type, Direction side, boolean emitting) { return false; }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, Direction side, boolean theoretical) { return false; }
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type, Direction side, boolean theoretical) { return false; }
    @Override public long getEnergyDemanded(GregTechTags.Tag type, Direction side, long size) { return 0; }
    @Override public long getEnergyOffered(GregTechTags.Tag type, Direction side, long size) { return 0; }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type, Direction side) { return 0; }
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type, Direction side) { return 0; }
    @Override public long doInject(GregTechTags.Tag type, Direction side, long size, long amount, boolean execute) { return 0; }

    @Override public IFluidHandler portFluids(MultiblockLayout.Role role) { return null; }
    @Override public Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.ENERGY_INPUT ? List.of(GregTechTags.Energy.EU) : List.of();
    }
    @Override public long portEnergyInputMin(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return acceptsEU(role, type) ? 256 : 0;
    }
    @Override public long portEnergyInputRecommended(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return acceptsEU(role, type) ? 512 : 0;
    }
    @Override public long portEnergyInputMax(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return acceptsEU(role, type) ? 1024 : 0;
    }
    @Override public long portEnergyStored(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return acceptsEU(role, type) ? energy : 0;
    }
    @Override public long portEnergyCapacity(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return acceptsEU(role, type) && processors != null ? processors.energyCapacity() : 0;
    }
    @Override public long portEnergyDemanded(MultiblockLayout.Role role, GregTechTags.Tag type, long size) {
        // GT6 getEnergyDemanded returns a fixed 1024 packets, even near its nominal capacity.
        return acceptsEU(role, type) && size > 0 && processors != null ? 1024 : 0;
    }
    @Override public long injectPortEnergy(MultiblockLayout.Role role, GregTechTags.Tag type,
                                           long size, long amount, boolean execute) {
        if(!acceptsEU(role,type)||!isStructureOk())return 0;
        var plan=com.gregtech.gregtech.content.logistics.LogisticsCorePowerRules.inject(energy,processors.energyCapacity(),size,amount,execute);
        if(plan.explode()){if(level!=null&&!level.isClientSide)level.explode(null,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,6,Level.ExplosionInteraction.BLOCK);}
        else if(execute&&plan.accepted()>0){energy=plan.energy();setChanged();}
        return plan.accepted();
    }

    private static boolean acceptsEU(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return role == MultiblockLayout.Role.ENERGY_INPUT && type == GregTechTags.Energy.EU;
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("gt.energy", energy);
        tag.put("gt.items", items.serializeNBT());
        covers.save(tag);
        for (int i = 0; i < tanks.length; i++) {
            if (tanks[i].isEmpty()) continue;
            var saved = new CompoundTag();
            tanks[i].writeToNBT(saved);
            tag.put("gt.tank." + i, saved);
        }
        if (processors != null) {
            tag.putInt("gt.cpu.logic", processors.logic());
            tag.putInt("gt.cpu.control", processors.control());
            tag.putInt("gt.cpu.storage", processors.storage());
            tag.putInt("gt.cpu.conversion", processors.conversion());
        }
        tag.putInt("gt.cpu.logic.used", usedProcessors.logic());
        tag.putInt("gt.cpu.control.used", usedProcessors.control());
        tag.putInt("gt.cpu.storage.used", usedProcessors.storage());
        tag.putInt("gt.cpu.conversion.used", usedProcessors.conversion());
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        energy = Math.max(0, tag.getLong("gt.energy"));
        usedProcessors = new LogisticsCpuDisplay.Usage(
                Math.max(0, tag.getInt("gt.cpu.logic.used")),
                Math.max(0, tag.getInt("gt.cpu.control.used")),
                Math.max(0, tag.getInt("gt.cpu.storage.used")),
                Math.max(0, tag.getInt("gt.cpu.conversion.used")));
        covers.load(tag);
        if (tag.contains("gt.items") && tag.getCompound("gt.items").getInt("Size") == BUFFER_SLOTS)
            items.deserializeNBT(tag.getCompound("gt.items"));
        for (int i = 0; i < tanks.length; i++) {
            String key = "gt.tank." + i;
            if (tag.contains(key)) tanks[i].readFromNBT(tag.getCompound(key));
            else tanks[i].setEmpty();
            tanks[i].setCapacity(TANK_CAPACITY);
        }
        // CPU counts are derived from placed parts on the next validation, never trusted from NBT.
        processors = null;
        network = LogisticsNetwork.Snapshot.empty();
    }

    @Override public void dropContents() {
        if (level == null || level.isClientSide) return;
        BlockContents.drop(this, items);
        covers.dropAll();
        setChanged();
    }
}
