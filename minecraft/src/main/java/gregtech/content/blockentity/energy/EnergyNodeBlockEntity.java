package gregtech.content.blockentity.energy;

import gregtech.api.platform.machine.MachineControl;
import gregtech.content.block.energy.EnergyNodeBlock;
import gregtech.content.blockentity.GTEnergyBlockEntity;
import gregtech.content.cover.ComponentCoverRuntime;
import gregtech.content.cover.CoverAdmission;
import gregtech.content.cover.CoverItems;
import gregtech.content.cover.PanelCover;
import gregtech.content.cover.PanelCoverHost;
import gregtech.content.cover.PanelCoverRuntime;
import gregtech.content.data.GregTechTags;
import gregtech.content.energy.BatteryBoxEnergy;
import gregtech.content.energy.EnergyNodeSpecProvider;
import gregtech.content.registry.GTEnergyNodes;
import gregtech.engine.energy.EnergyConversionMeter;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyNodeSpec.DeviceType;
import gregtech.engine.energy.MachineOverload;
import gregtech.engine.inventory.BlockContents;
import gregtech.engine.machine.AdjacentEnergyControl;
import gregtech.engine.stack.NbtCompat;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
//?}

/**
 * Hosts GT6 energy converters, solar panels and battery boxes. The block's specification
 * selects its energy types, packet sizes and device-specific behavior.
 */
public class EnergyNodeBlockEntity extends GTEnergyBlockEntity
        implements EnergyNodeSpecProvider, AdjacentEnergyControl.Provider, BlockContents, MachineControl.Provider, PanelCoverHost {
    private EnergyNodeSpec spec;
    private final EnergyConversionMeter conversionMeter = new EnergyConversionMeter();
    /** Stored in input units (litres of steam for turbines). */
    long bufferedInput;
    boolean converterStopped;
    private long lastSyncedBuffer = -1;

    private final EnumMap<Direction, ItemStack> covers = new EnumMap<>(Direction.class);
    private PanelCoverRuntime controlPanels;

    private int overloadCount;
    private EnergyDeviceBehavior behavior;
    private boolean inverted;
    final ForgeEnergyNodeAdapter forgeEnergy = new ForgeEnergyNodeAdapter(this);

    public EnergyNodeBlockEntity(BlockPos pos, BlockState state) {
        this(GTEnergyNodes.ENERGY_NODE.get(), pos, state);
    }

    public EnergyNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        if (state.getBlock() instanceof EnergyNodeBlock node) {
            setSpec(node.spec());
        } else {
            behavior = new BufferedEnergyNode.Pullable(this);
        }
    }

    /** An attached node may tune its existing device. Its block still supplies the spec on reload. */
    public void setSpec(EnergyNodeSpec spec) {
        Objects.requireNonNull(spec, "Energy node spec");
        var previousSpec = this.spec;
        if (spec.equals(previousSpec)) return;
        boolean compatible = previousSpec != null
                && previousSpec.deviceType() == spec.deviceType()
                && previousSpec.kind() == spec.kind()
                && previousSpec.inType() == spec.inType()
                && previousSpec.outType() == spec.outType()
                && previousSpec.batterySlots() == spec.batterySlots();
        if (level != null && !compatible) {
            throw new IllegalArgumentException(
                    "Cannot change the device family, energy types or slot layout of an attached energy node");
        }

        var next = EnergyDeviceBehavior.create(this, spec);
        var previous = behavior;
        if (!compatible) {
            bufferedInput = 0;
            converterStopped = false;
            inverted = false;
            overloadCount = 0;
            lastSyncedBuffer = -1;
        }
        this.spec = spec;
        behavior = next;
        next.onBound();
        if (previous == null) return;

        previous.detach();
        if (level != null && !level.isClientSide) {
            previous.dropContents();
            setChanged();
        }
//? if >=1.21.1 {
        /*if (level != null) level.invalidateCapabilities(worldPosition);
*///?} else {
        forgeEnergy.invalidateCaps();
//?}
    }

    @Override
    public void onLoad() {
        super.onLoad();
        behavior.onLoad();
    }

    @Override
    public EnergyNodeSpec spec() {
        return spec;
    }

    boolean isCurrent(EnergyDeviceBehavior candidate) {
        return behavior == candidate && !isRemoved();
    }

    Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(DirectionalBlock.FACING) ? state.getValue(DirectionalBlock.FACING) : Direction.NORTH;
    }

    public EnergyConversionMeter conversionMeter() {
        return conversionMeter;
    }

    public long lastMeasuredInput() {
        return level == null ? 0 : conversionMeter.input(level.getGameTime());
    }

    public long lastMeasuredOutput() {
        return level == null ? 0 : conversionMeter.output(level.getGameTime());
    }

    public long lastMeasuredAuxOutput() {
        return level == null ? 0 : conversionMeter.auxiliaryOutput(level.getGameTime());
    }

    public long stored() {
        return behavior.stored();
    }

    long capacity() {
        return behavior.capacity();
    }

    // Original converters

    @Nullable
    OriginalConverterNode originalConverter() {
        return behavior instanceof OriginalConverterNode converter ? converter : null;
    }

    public boolean hasOriginalConverter() {
        return behavior instanceof OriginalConverterNode;
    }

    public boolean reverseMotor() {
        var converter = originalConverter();
        return converter != null && converter.reverseMotor();
    }

    public boolean motorCounterClockwise() {
        var converter = originalConverter();
        return converter != null && converter.counterClockwise();
    }

    public boolean converterStopped() {
        return converterStopped;
    }

    public boolean converterEmitted() {
        var converter = originalConverter();
        return converter != null && converter.emitted();
    }

    public boolean converterFast() {
        var converter = originalConverter();
        return converter != null && converter.fast();
    }

    public boolean magnetEnabled() {
        return isMagnet() && !converterStopped;
    }

    public boolean toggleMagnetEnabled() {
        if (!isMagnet()) return false;
        return machineControl(null).setEnabled(!magnetEnabled());
    }

    public int magnetMode() {
        return isMagnet() ? behavior.control().mode() : 0;
    }

    // Transformers

    public boolean transformerInverted() {
        return inverted;
    }

    /** @return the new mode (true for step-up). */
    public boolean toggleInverted() {
        inverted = !inverted;
        if (behavior instanceof TransformerNode transformer) transformer.inversionChanged();
        setChanged();
        return inverted;
    }

    long effectiveInputRate() {
        return inverted ? spec.outputRate() : spec.inputRate();
    }

    long effectiveOutputRate() {
        return inverted ? spec.inputRate() : spec.outputRate();
    }

    // Solar panels

    public boolean isSolar() {
        return behavior instanceof SolarPanelNode;
    }

    public boolean solarSkyVisible() {
        return behavior instanceof SolarPanelNode solar && solar.skyVisible();
    }

    public void checkSolarSky() {
        if (behavior instanceof SolarPanelNode solar) solar.checkSky();
    }

    @Override
    public boolean isRainProof(Direction side) {
        return isSolar();
    }

    @Override
    public boolean isThunderProof(Direction side) {
        return isSolar();
    }

    // Steam turbines

    public long turbineSteamAmount() {
        var turbine = behavior.turbine();
        return turbine == null ? 0 : turbine.steamAmount();
    }

    public long turbineSteamCapacity() {
        var turbine = behavior.turbine();
        return turbine == null ? 0 : turbine.steamCapacity();
    }

    public static boolean isRotorItem(ItemStack stack) {
        return SteamTurbineNode.isRotorItem(stack);
    }

    public boolean hasRotor() {
        var turbine = behavior.turbine();
        return turbine != null && turbine.hasRotor();
    }

    /** Turbines consume their rotor in the crafting recipe. */
    public boolean installRotor(ItemStack stack) {
        return false;
    }

    public ItemStack removeRotor() {
        var turbine = behavior.turbine();
        return turbine == null ? ItemStack.EMPTY : turbine.removeRotor();
    }

    public long purgeTurbineSteam() {
        var turbine = behavior.turbine();
        return turbine == null ? 0 : turbine.purge();
    }

    public boolean isSteamConverter() {
        return isTurbine();
    }

    // Battery boxes

    public boolean isBatteryBox() {
        return behavior instanceof BatteryBoxNode;
    }

    @Nullable
    public BatteryBoxEnergy batteryEnergy() {
        return behavior instanceof BatteryBoxNode battery ? battery.energy : null;
    }

    @Nullable
    private BatteryBoxInventory batterySlots() {
        return behavior instanceof BatteryBoxNode battery ? battery.inventory : null;
    }

    public static long batteryCapacityOf(ItemStack stack) {
        return batteryCapacityOf(stack, GregTechTags.Energy.EU);
    }

    public static long batteryCapacityOf(ItemStack stack, GregTechTags.Tag type) {
        return BatteryBoxInventory.capacityOf(stack, type);
    }

    public boolean installBattery(ItemStack stack) {
        var slots = batterySlots();
        return slots != null && slots.install(stack);
    }

    public ItemStack removeBattery() {
        var slots = batterySlots();
        return slots == null ? ItemStack.EMPTY : slots.remove();
    }

    public IItemHandlerModifiable batteryInventory() {
        var slots = batterySlots();
        return slots == null ? new ItemStackHandler(0) : slots;
    }

    @Nullable
    public MenuProvider batteryMenu() {
        var slots = batterySlots();
        return slots == null ? null : slots.menu();
    }

    // Covers and tool interactions

    public boolean hasControlPanels() {
        return hasOriginalConverter() || isBatteryBox() || isElectricTransformer() || isSolar();
    }

    @Override
    public ItemStack getCover(Direction side) {
        return covers.getOrDefault(side, ItemStack.EMPTY);
    }

    @Override
    public PanelCoverRuntime panels() {
        if (controlPanels == null) {
            controlPanels = new PanelCoverRuntime(this);
        }
        return controlPanels;
    }

    @Override
    public boolean coverSupportsPossible() {
        return hasControlPanels();
    }

    @Override
    public boolean coverPossible(Direction side) {
        if (hasOriginalConverter() || isSolar()) return true;
        if (behavior instanceof BatteryBoxNode battery) return battery.energy.buffer() > spec.outputRate();
        return isElectricTransformer() && behavior.control().running();
    }

    @Override
    public boolean consumesTool(ItemStack tool) {
        return PanelCoverHost.blockConsumesTool(this, tool) || EnergyNodeBlock.consumesDeviceTool(this, tool);
    }

    @Override
    public boolean attachCover(Direction side, ItemStack stack) {
        if (!CoverItems.isCover(stack) || !getCover(side).isEmpty() || !panels().canAttach(side, stack)
                || !CoverAdmission.allowCover(this, side, stack)) {
            return false;
        }
        covers.put(side, stack.copyWithCount(1));
        panels().attached(side);
        return true;
    }

    @Override
    public ItemStack removeCover(Direction side) {
        ItemStack removedCover = covers.remove(side);
        if (removedCover == null) return ItemStack.EMPTY;
        PanelCover panel = PanelCover.of(removedCover);
        if (panel != null && panel.selector()) {
            machineControl(side).setMode(0);
        }
        return panels().removed(side, removedCover);
    }

    // Server tick

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyNodeBlockEntity node) {
        if (node.spec == null) return;
        node.behavior.tickCoversBeforeDevice(level);
        // A cover can synchronously bind a different device.
        node.behavior.tick(level, pos, state);
    }

    void tickComponentCovers(Level level) {
        if (covers.isEmpty()) return;
        panels().beforeTick();
        tickCoverComponents(level);
        panels().afterTick();
    }

    void tickCoverComponents(Level level) {
        long time = level.getGameTime();
        for (Direction side : Direction.values()) {
            ComponentCoverRuntime.tick(this, side, time);
        }
    }

    void syncBufferedInput(Level level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % 20 == 0 && lastSyncedBuffer != bufferedInput) {
            lastSyncedBuffer = bufferedInput;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    // Client synchronization

    /** The charge bar uses the node's saved state. */
    @Override
    protected void writeUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        saveEntityAdditional(tag, lookup);
    }

    @Override
    protected void readUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        loadFromTag(tag, lookup);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void handleDataPacket(
            Connection connection, ClientboundBlockEntityDataPacket packet, @Nullable HolderLookup.Provider lookup) {
        if (packet.getTag() != null) {
            loadFromTag(packet.getTag(), lookup);
        }
    }

    public int converterOverloads() {
        return overloadCount;
    }

    void restoreOverloads(int count) {
        overloadCount = Math.max(0, count);
    }

    void coolDownOverload(boolean conversionPossible) {
        if (level.getGameTime() % 600 == 5 && !conversionPossible && overloadCount > 0) overloadCount--;
    }

    void handleConverterOverload(long packetSize, GregTechTags.Tag energyType) {
        if (overloadCount < 100) {
            bufferedInput = 0;
            overloadCount++;
            setChanged();
            return;
        }
        MachineOverload.apply(this, packetSize, energyType);
        setChanged();
    }

    void handleInputOverload(long packetSize) {
        handleConverterOverload(packetSize, spec.inType());
        bufferedInput = 0;
    }

    /** Input units consumed per emitted packet (solar generates, so 1:1 on output units). */
    long inputCostPerOutputPacket() {
        return switch (spec.kind()) {
            case SOLAR, STORAGE -> effectiveOutputRate();
            default -> Math.max(1, effectiveInputRate());
        };
    }

    // IEnergyBlock

    boolean isOriginalCooler() {
        return spec != null && spec.deviceType() == DeviceType.COOLER;
    }

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        return emitting ? behavior.acceptsOutputType(energyType) : behavior.acceptsInputType(energyType);
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return behavior.types();
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return behavior.acceptsInputType(energyType) && behavior.acceptsFrom(side, theoretical);
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return behavior.acceptsOutputType(energyType) && behavior.emitsTo(energyType, side, theoretical);
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return behavior.acceptsInputType(energyType) ? behavior.inputRate() : 0;
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return behavior.recommendedOutput(energyType, side);
    }

    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        return behavior.minInput(energyType, side);
    }

    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        return behavior.maxInput(energyType, side);
    }

    @Override
    public long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        return behavior.minOutput(energyType, side);
    }

    @Override
    public long getEnergySizeOutputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        return behavior.maxOutput(energyType, side);
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long packetSize) {
        return behavior.demanded(energyType, side, packetSize);
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag energyType, @Nullable Direction side, long packetSize) {
        return behavior.offered(energyType, side, packetSize);
    }

    @Override
    public long doInject(GregTechTags.Tag energyType, @Nullable Direction side, long packetSize, long packetCount,
            boolean execute) {
        return behavior.inject(energyType, side, packetSize, packetCount, execute);
    }

    @Override
    public long doExtract(GregTechTags.Tag energyType, @Nullable Direction side, long packetSize, long packetCount,
            boolean execute) {
        return behavior.extract(energyType, side, packetSize, packetCount, execute);
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyCapacitorTypes(@Nullable Direction side) {
        return behavior.capacitorTypes(side);
    }

    @Override
    public long getEnergyStored(GregTechTags.Tag energyType, @Nullable Direction side) {
        return behavior.energyStored(energyType);
    }

    @Override
    public long getEnergyCapacity(GregTechTags.Tag energyType, @Nullable Direction side) {
        return behavior.energyCapacity(energyType);
    }

    // Machine controls

    @Override
    public AdjacentEnergyControl adjacentEnergyControl() {
        return behavior.adjacentEnergyControl();
    }

    @Override
    public MachineControl machineControl(Direction side) {
        return behavior.control();
    }

    Collection<GregTechTags.Tag> defaultCapacitorTypes(@Nullable Direction side) {
        return super.getEnergyCapacitorTypes(side);
    }

    // Platform capabilities

//? if >=1.21.1 {
    /*public IEnergyStorage energyCapability(@Nullable Direction side) {
        return forgeEnergy.exposes(side) ? forgeEnergy.capability(side) : null;
    }

    public IItemHandler itemCapability(@Nullable Direction side) {
        var slots = batterySlots();
        return slots == null ? null : slots.capability(side);
    }

    public IFluidHandler fluidCapability(@Nullable Direction side) {
        var turbine = behavior.turbine();
        return turbine != null && turbine.exposesInlet(side) ? turbine.capability(side) : null;
    }
*///?} else {
    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ENERGY && forgeEnergy.exposes(side)) {
            return forgeEnergy.capability(side).cast();
        }

        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            var slots = batterySlots();
            if (slots != null) return slots.capability(side).cast();
        }

        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            var turbine = behavior.turbine();
            if (turbine != null && turbine.exposesInlet(side)) return turbine.capability(side).cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        var slots = batterySlots();
        if (slots != null) slots.invalidateCaps();
        var turbine = behavior.turbine();
        if (turbine != null) turbine.invalidateCaps();
        forgeEnergy.invalidateCaps();
    }
//?}

    // Saved state

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        covers.forEach((side, stack) ->
                tag.put("gt.battery_cover_" + side.ordinal(), NbtCompat.saveItem(stack, lookup)));
        tag.putBoolean("gt.inverted", inverted);
        tag.putBoolean("gt.converter_stopped", converterStopped);
        behavior.save(tag, lookup);
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        covers.clear();
        for (Direction side : Direction.values()) {
            String coverKey = "gt.battery_cover_" + side.ordinal();
            if (tag.contains(coverKey)) {
                covers.put(side, NbtCompat.readItem(tag.getCompound(coverKey), lookup));
            }
        }
        inverted = tag.getBoolean("gt.inverted");
        converterStopped = tag.getBoolean("gt.converter_stopped");
        behavior.load(tag, lookup);
        panels().loaded();
    }

    @Override
    public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (ItemStack cover : covers.values()) {
            BlockContents.drop(this, cover);
        }
        covers.clear();
        behavior.dropContents();
        setChanged();
    }
}
