package gregtech.content.blockentity.machine;

import gregtech.internal.multiblock.MultiblockLayout;
import gregtech.internal.multiblock.MultiblockPortOwner;
import gregtech.internal.multiblock.PartBindings;
import gregtech.internal.fluid.FluidTankGT;
import gregtech.internal.inventory.BlockContents;
import gregtech.content.block.machine.LogisticsCoreControllerBlock;
import gregtech.content.blockentity.GTEnergyBlockEntity;
import gregtech.content.logistics.LogisticsHost;
import gregtech.content.logistics.LogisticsCoverHost;
import gregtech.content.logistics.LogisticsCovers;
import gregtech.content.logistics.LogisticsCoverType;
import gregtech.content.logistics.LogisticsCpuDisplay;
import gregtech.content.logistics.LogisticsNetwork;
import gregtech.content.logistics.LogisticsCoreStructure;
import gregtech.content.logistics.LogisticsRoutingPass;
import gregtech.content.data.GregTechTags;
import gregtech.content.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
//?}

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/** GT6 Logistics Core structure, EU input, network discovery and private 108-slot buffers. */
public final class LogisticsCoreControllerBlockEntity extends GTEnergyBlockEntity
implements MultiblockPortOwner, LogisticsCoverHost, BlockContents, gregtech.internal.multiblock.tracking.TrackedMultiblock {
    public static final int BUFFER_SLOTS = LogisticsCoreStructure.MAX_STORAGE_CPU_COUNT;
    public static final int TANK_CAPACITY = 16_000;
    private final PartBindings<BlockPos, MultiblockLayout.Role> bindings = new PartBindings<>();
    private final ItemStackHandler items = new ItemStackHandler(BUFFER_SLOTS) {

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final FluidTankGT[] tanks = makeTanks();
    private final LogisticsCovers covers = new LogisticsCovers(this, this);
    private LogisticsCoreStructure.Counts processors;
    private LogisticsNetwork.Snapshot network = LogisticsNetwork.Snapshot.empty();

    /** Usage at the latest one-second gate; an unpowered pass resets it. */
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

    @Override public gregtech.internal.multiblock.tracking.StructureDefinition structureDefinition() {
        return gregtech.content.multiblock.SpecialStructures.logistics(this);
    }
    @Override public boolean applyStructure(gregtech.internal.multiblock.tracking.PatternMatch match) {
        var candidates = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
        int logic = 0, control = 0, storage = 0, conversion = 0;
        for (var cell : match.fixedCells()) {
            candidates.put(cell.pos(), cell.cell().role());
            if (cell.cell().role() != MultiblockLayout.Role.CASING) continue;
            var contribution = gregtech.content.logistics.LogisticsCoreLayout.contribution(cell.cell().part());
            if (contribution == null) continue;
            logic += contribution.logic(); control += contribution.control();
            storage += contribution.storage(); conversion += contribution.conversion();
        }
        // PER_CELL stays declarative metadata until the user decides which families enable it; the
        // runtime binding below remains atomic.
        var claimed = gregtech.internal.multiblock.tracking.StructureClaims.update(this, bindings, candidates,
                pos -> level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part && part.canBind(worldPosition),
                (pos, role) -> ((MultiblockPortBlockEntity) level.getBlockEntity(pos)).bind(worldPosition, role), this::releasePart);
        if (claimed == gregtech.internal.multiblock.tracking.StructureClaims.Result.FAILED) return unbind();
        processors = new LogisticsCoreStructure.Counts(logic, control,
                Math.min(LogisticsCoreStructure.MAX_STORAGE_CPU_COUNT, storage), conversion);
        return true;
    }
    @Override public void releaseStructureClaims() { unbind(); }
    @Override public boolean isStructureOk() {
        return gregtech.internal.multiblock.tracking.StructureTracking.query(this);
    }
    @Override public void structureFormedChanged(boolean formed) {
        if (!formed) {
            processors = null;
            network = LogisticsNetwork.Snapshot.empty();
        }
    }
    private boolean unbind() {
        gregtech.internal.multiblock.tracking.StructureClaims.runQuiet(() -> bindings.clear(this::releasePart));
        gregtech.internal.multiblock.tracking.StructureClaims.cleared(this);
        processors = null;
        network = LogisticsNetwork.Snapshot.empty();
        return false;
    }

    private void releasePart(BlockPos pos) {
        if (level != null && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part)
        part.release(worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) gregtech.internal.multiblock.tracking.StructureTracking.attach(this);
    }

    @Override
    public void setRemoved() {
        if (!gregtech.internal.multiblock.tracking.StructureTracking.unloading(this)) unbind();
        gregtech.internal.multiblock.tracking.StructureTracking.onRemoved(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        gregtech.internal.multiblock.tracking.StructureTracking.onChunkUnloaded(this);
        super.onChunkUnloaded();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            LogisticsCoreControllerBlockEntity controller) {
        // GT6 routes once per second, but charges standby power on every server tick (:504).
        if (level.getGameTime() % 20 == 0) {
            LogisticsCpuDisplay.Usage previous = controller.usedProcessors;
            controller.usedProcessors = LogisticsCpuDisplay.Usage.ZERO;
            if (controller.processors != null && controller.energy >= controller.processors.routingThreshold()) {
                controller.scanNetwork();
                controller.updateDisplays(previous);
                controller.routeNetworkOnce(true);
            }
            else controller.network = LogisticsNetwork.Snapshot.empty();
            if (!previous.equals(controller.usedProcessors)) controller.setChanged();
        }
        long cost = controller.processors == null ? 20 : controller.processors.fixedEnergyPerTick();
        long next = Math.max(0, controller.energy - cost);
        if (next != controller.energy) {
            controller.energy = next;
            controller.setChanged();
        }
    }

    public LogisticsCoreStructure.Counts processorCounts() {
        return isStructureOk() ? processors : null;
    }

    /** Refresh discovery after validating and binding the structure; also called by the per-second tick. */
    public LogisticsNetwork.Snapshot scanNetwork() {
        if (processors == null) return network;
        network = LogisticsNetwork.scan(level, worldPosition,
                getBlockState().getValue(LogisticsCoreControllerBlock.FACING),
                LogisticsCoreStructure.range(processors));
        return network;
    }

    /** Runs one shared fluid/item pass; the normal server ticker supplies the one-second cadence. */
    public LogisticsRoutingPass.Result routeNetworkOnce() {
        return routeNetworkOnce(false);
    }

    private LogisticsRoutingPass.Result routeNetworkOnce(boolean networkReady) {
        if (level == null || level.isClientSide || processors == null
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

    public LogisticsCpuDisplay.Usage usedProcessorCounts() {
        return usedProcessors;
    }

    public LogisticsNetwork.Snapshot networkSnapshot() {
        return network;
    }

    public ItemStackHandler bufferItems() {
        return items;
    }

    public FluidTankGT bufferTank(int slot) {
        return tanks[slot];
    }

    public int bufferTankCount() {
        return tanks.length;
    }

    @Override
    public boolean canLogistics(Direction side) {
        return !isRemoved();
    }

    @Override
    public LogisticsCovers logisticsCovers() {
        return covers;
    }

    public long storedEU() {
        return energy;
    }

    /** Original doInject accepts EU on every controller face as well as the bound walls. */
    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(Direction side) {
        return List.of(GregTechTags.Energy.EU);
    }

    @Override
    public boolean isEnergyType(GregTechTags.Tag type, Direction side, boolean emitting) {
        return !emitting && type == GregTechTags.Energy.EU;
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, Direction side, boolean theoretical) {
        return isEnergyType(type, side, false);
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag type, Direction side, boolean theoretical) {
        return false;
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag type, Direction side, long size) {
        return type == GregTechTags.Energy.EU ? 1024 : 0;
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag type, Direction side, long size) {
        return 0;
    }

    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag type, Direction side) {
        return type == GregTechTags.Energy.EU ? 256 : 0;
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag type, Direction side) {
        return type == GregTechTags.Energy.EU ? 512 : 0;
    }

    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag type, Direction side) {
        return type == GregTechTags.Energy.EU ? 1024 : 0;
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag type, Direction side) {
        return 0;
    }

    @Override
    public long getEnergyStored(GregTechTags.Tag type, Direction side) {
        return type == GregTechTags.Energy.EU ? energy : 0;
    }

    @Override
    public long getEnergyCapacity(GregTechTags.Tag type, Direction side) {
        return type == GregTechTags.Energy.EU ? energyCapacity() : 0;
    }

    private long energyCapacity() {
        return processors == null ? 128 : processors.energyCapacity();
    }

    @Override
    public long doInject(GregTechTags.Tag type, Direction side, long size, long amount, boolean execute) {
        if (type != GregTechTags.Energy.EU) return 0;
        isStructureOk();
        return injectEU(size, amount, execute);
    }

    @Override
    public IFluidHandler portFluids(MultiblockLayout.Role role) {
        return null;
    }

    @Override
    public Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.ENERGY_INPUT ? List.of(GregTechTags.Energy.EU) : List.of();
    }

    @Override
    public long portEnergyInputMin(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return acceptsEU(role, type) ? 256 : 0;
    }

    @Override
    public long portEnergyInputRecommended(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return acceptsEU(role, type) ? 512 : 0;
    }

    @Override
    public long portEnergyInputMax(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return acceptsEU(role, type) ? 1024 : 0;
    }

    @Override
    public long portEnergyStored(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return acceptsEU(role, type) ? energy : 0;
    }

    @Override
    public long portEnergyCapacity(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return acceptsEU(role, type) && processors != null ? processors.energyCapacity() : 0;
    }

    @Override
    public long portEnergyDemanded(MultiblockLayout.Role role, GregTechTags.Tag type, long size) {
        // GT6 getEnergyDemanded returns the fixed value 1024, even near its nominal capacity.
        return acceptsEU(role, type) && size > 0 && processors != null ? 1024 : 0;
    }

    @Override
    public long injectPortEnergy(MultiblockLayout.Role role, GregTechTags.Tag type,
            long size, long amount, boolean execute) {
        if (!acceptsEU(role, type) || !isStructureOk()) return 0;
        return injectEU(size, amount, execute);
    }

    private long injectEU(long size, long amount, boolean execute) {
        var plan = gregtech.content.logistics.LogisticsCorePowerRules.inject(energy, energyCapacity(), size, amount, execute);
        if (plan.explode()) gregtech.internal.energy.MachineOverload.explode(this, 6);
        else if (execute && plan.accepted() > 0) {
            energy = plan.energy();
            setChanged();
        }
        return plan.accepted();
    }

    private static boolean acceptsEU(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return role == MultiblockLayout.Role.ENERGY_INPUT && type == GregTechTags.Energy.EU;
    }

//? if >=1.21.1 {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
*///?} else {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
//?}
        tag.putLong("gt.energy", energy);
//? if >=1.21.1 {
        /*tag.put("gt.items", items.serializeNBT(lookup));
        covers.save(tag, lookup);
*///?} else {
        tag.put("gt.items", items.serializeNBT());
        covers.save(tag);
//?}
        for (int i = 0; i < tanks.length; i++) {
            if (tanks[i].isEmpty()) continue;
            var saved = new CompoundTag();
//? if >=1.21.1 {
            /*tanks[i].writeToNBT(saved, lookup);
*///?} else {
            tanks[i].writeToNBT(saved);
//?}
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

//? if >=1.21.1 {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
*///?} else {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
//?}
        energy = Math.max(0, tag.getLong("gt.energy"));
        usedProcessors = new LogisticsCpuDisplay.Usage(
                Math.max(0, tag.getInt("gt.cpu.logic.used")),
                Math.max(0, tag.getInt("gt.cpu.control.used")),
                Math.max(0, tag.getInt("gt.cpu.storage.used")),
                Math.max(0, tag.getInt("gt.cpu.conversion.used")));
//? if >=1.21.1 {
        /*covers.load(tag, lookup);
*///?} else {
        covers.load(tag);
//?}
        if (tag.contains("gt.items") && tag.getCompound("gt.items").getInt("Size") == BUFFER_SLOTS)
//? if >=1.21.1 {
        /*items.deserializeNBT(lookup, tag.getCompound("gt.items"));
*///?} else {
        items.deserializeNBT(tag.getCompound("gt.items"));
//?}
        for (int i = 0; i < tanks.length; i++) {
            String key = "gt.tank." + i;
//? if >=1.21.1 {
            /*if (tag.contains(key)) tanks[i].readFromNBT(tag.getCompound(key), lookup);
*///?} else {
            if (tag.contains(key)) tanks[i].readFromNBT(tag.getCompound(key));
//?}
            else tanks[i].setEmpty();
            tanks[i].setCapacity(TANK_CAPACITY);
        }
        // CPU counts are derived from placed parts on the next validation, never trusted from NBT.
        processors = null;
        network = LogisticsNetwork.Snapshot.empty();
    }

    @Override
    public void dropContents() {
        if (level == null || level.isClientSide) return;
        BlockContents.drop(this, items);
        covers.dropAll();
        setChanged();
    }
}
