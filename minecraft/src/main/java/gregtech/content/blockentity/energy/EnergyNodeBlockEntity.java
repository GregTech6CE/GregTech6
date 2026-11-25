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
import gregtech.content.registry.GTEnergyNodes;
import gregtech.engine.energy.EnergyConversionMeter;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyNodeSpec.DeviceType;
import gregtech.engine.energy.MachineOverload;
import gregtech.engine.inventory.BlockContents;
import gregtech.engine.machine.AdjacentEnergyControl;
import gregtech.engine.stack.NbtCompat;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import java.util.Collection;
import java.util.EnumMap;
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
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
//?}

/**
 * Hosts GT6 energy converters, solar panels and battery boxes. The block's specification
 * selects its energy types, packet sizes and device-specific behavior.
 */
public class EnergyNodeBlockEntity extends GTEnergyBlockEntity
        implements AdjacentEnergyControl.Provider, BlockContents, MachineControl.Provider, PanelCoverHost {
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
    @Nullable
    private TransformerNode.Rotation rotationTransformer;
    @Nullable
    private TransformerNode.Electric electricTransformer;
    final SteamTurbineNode turbine = new SteamTurbineNode(this);
    final ForgeEnergyNodeAdapter forgeEnergy = new ForgeEnergyNodeAdapter(this);
    private final BatteryBoxInventory batterySlots = new BatteryBoxInventory(this);
    @Nullable
    private BatteryBoxNode batteryBox;
    @Nullable
    private SolarPanelNode solarPanel;

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

    public void setSpec(EnergyNodeSpec spec) {
        this.spec = spec;
        behavior = switch (spec.deviceType()) {
            case MOTOR, DYNAMO, HEATER -> new OriginalConverterNode(this, spec);
            case COOLER -> new CoolerNode(this, spec);
            case MAGNET -> new MagnetNode(this, spec);
            case STEAM_TURBINE -> new OriginalConverterNode.SteamConverter(this, spec);
            case ELECTRIC_TRANSFORMER -> {
                if (electricTransformer == null) electricTransformer = new TransformerNode.Electric(this);
                else electricTransformer.configure();
                yield electricTransformer;
            }
            case ROTATION_TRANSFORMER -> {
                if (rotationTransformer == null) rotationTransformer = new TransformerNode.Rotation(this);
                yield rotationTransformer;
            }
            case BATTERY_BOX -> {
                batteryBox = new BatteryBoxNode(this, spec, batterySlots);
                yield batteryBox;
            }
            case SOLAR -> {
                if (solarPanel == null) solarPanel = new SolarPanelNode(this, spec.outputRate());
                else solarPanel.configure(spec.outputRate());
                yield solarPanel;
            }
            case BUFFERED -> switch (spec.kind()) {
                case TURBINE -> new BufferedEnergyNode.Turbine(this);
                case MAGNET -> new BufferedEnergyNode.Magnet(this);
                default -> new BufferedEnergyNode.Pullable(this);
            };
        };
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (isSolar()) solarPanel.onLoad();
    }

    public EnergyNodeSpec spec() {
        return spec;
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

    public boolean isOriginalRotaryConverter() {
        return spec != null && (spec.deviceType() == DeviceType.MOTOR || spec.deviceType() == DeviceType.DYNAMO);
    }

    public boolean isOriginalThermalConverter() {
        return spec != null && (spec.deviceType() == DeviceType.HEATER || spec.deviceType() == DeviceType.COOLER);
    }

    public boolean isOriginalMotor() {
        return spec != null && (spec.deviceType() == DeviceType.MOTOR || spec.deviceType() == DeviceType.STEAM_TURBINE);
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

    public boolean isMagnet() {
        return spec != null && spec.kind() == EnergyNodeSpec.Kind.MAGNET;
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

    public boolean isInvertible() {
        return spec != null && spec.kind() == EnergyNodeSpec.Kind.CONVERTER && spec.inType() == spec.outType();
    }

    public boolean isElectricTransformer() {
        return spec != null && spec.deviceType() == DeviceType.ELECTRIC_TRANSFORMER;
    }

    public boolean isRotationTransformer() {
        return spec != null && spec.deviceType() == DeviceType.ROTATION_TRANSFORMER;
    }

    public boolean transformerInverted() {
        return inverted;
    }

    /** @return the new mode (true for step-up). */
    public boolean toggleInverted() {
        inverted = !inverted;
        if (behavior instanceof TransformerNode) {
            bufferedInput = 0;
            if (rotationTransformer != null) rotationTransformer.negativeInput = false;
            if (behavior instanceof TransformerNode.Electric electric) electric.control().select(inverted);
        }
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
        return solarPanel != null && solarPanel.skyVisible();
    }

    public void checkSolarSky() {
        if (isSolar()) solarPanel.checkSky();
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

    public boolean isTurbine() {
        return spec != null && spec.kind() == EnergyNodeSpec.Kind.TURBINE;
    }

    public long turbineSteamAmount() {
        return turbine.steamAmount();
    }

    public long turbineSteamCapacity() {
        return turbine.steamCapacity();
    }

    public static boolean isRotorItem(ItemStack stack) {
        return SteamTurbineNode.isRotorItem(stack);
    }

    public boolean hasRotor() {
        return turbine.hasRotor();
    }

    /** Turbines consume their rotor in the crafting recipe. */
    public boolean installRotor(ItemStack stack) {
        return false;
    }

    public ItemStack removeRotor() {
        return turbine.removeRotor();
    }

    public long purgeTurbineSteam() {
        return turbine.purge();
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
        return batteryBox == null ? null : batteryBox.energy;
    }

    public static long batteryCapacityOf(ItemStack stack) {
        return batteryCapacityOf(stack, GregTechTags.Energy.EU);
    }

    public static long batteryCapacityOf(ItemStack stack, GregTechTags.Tag type) {
        return BatteryBoxInventory.capacityOf(stack, type);
    }

    public boolean installBattery(ItemStack stack) {
        return batterySlots.install(stack);
    }

    public ItemStack removeBattery() {
        return batterySlots.remove();
    }

    public IItemHandlerModifiable batteryInventory() {
        return batterySlots;
    }

    public MenuProvider batteryMenu() {
        return batterySlots.menu();
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
        if (isBatteryBox()) return batteryBox.energy.buffer() > spec.outputRate();
        return isElectricTransformer() && behavior.control().running();
    }

    /** Mirrors the host's tool actions so unhandled tools reach the cover. */
    @Override
    public boolean consumesTool(ItemStack tool) {
        if (PanelCoverHost.blockConsumesTool(this, tool)) return true;
        if (isOriginalMotor() && (GTToolHelper.isMonkeyWrench(tool) || GTToolHelper.isMagnifyingGlass(tool))) {
            return true;
        }
        if (isInvertible()) {
            boolean switchesMode = isRotationTransformer() || isElectricTransformer()
                    ? GTToolHelper.isMonkeyWrench(tool)
                    : GTToolHelper.isSoftHammer(tool);
            if (switchesMode) return true;
        }
        if (isTurbine() && GTToolHelper.matchesTool(tool, GTToolType.PLUNGER)) return true;
        if (isTurbine() && hasRotor() && GTToolHelper.matchesTool(tool, GTToolType.CROWBAR)) return true;
        return isBatteryBox() && GTToolHelper.matchesTool(tool, GTToolType.CROWBAR);
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
        for (Direction side : Direction.values()) {
            ComponentCoverRuntime.tick(this, side, level.getGameTime());
        }
        panels().afterTick();
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
        return spec != null && energyType == spec.outType() ? behavior.outputRate() : 0;
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
        return isBatteryBox() ? batterySlots.capability(side) : null;
    }

    public IFluidHandler fluidCapability(@Nullable Direction side) {
        return turbine.exposesInlet(side) ? turbine.capability(side) : null;
    }
*///?} else {
    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ENERGY && forgeEnergy.exposes(side)) {
            return forgeEnergy.capability(side).cast();
        }

        if (capability == ForgeCapabilities.ITEM_HANDLER && isBatteryBox()) {
            return batterySlots.capability(side).cast();
        }

        if (capability == ForgeCapabilities.FLUID_HANDLER && turbine.exposesInlet(side)) {
            return turbine.capability(side).cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        batterySlots.invalidateCaps();
        turbine.invalidateCaps();
        forgeEnergy.invalidateCaps();
    }
//?}

    // Saved state

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        if (isSolar()) solarPanel.save(tag);
        else tag.putLong("gt.buffer", bufferedInput);
        if (isBatteryBox()) batteryBox.energy.save(tag);
        batterySlots.save(tag, lookup);
        covers.forEach((side, stack) ->
                tag.put("gt.battery_cover_" + side.ordinal(), NbtCompat.saveItem(stack, lookup)));
        if (behavior instanceof TransformerNode.Electric electric) electric.control().save(tag);
        tag.putBoolean("gt.inverted", inverted);
        if (behavior instanceof TransformerNode.Rotation rotation) tag.putBoolean("gt.rotation_negative_input", rotation.negativeInput);
        if (isMagnet()) {
            // Older magnet saves use these keys for the converter's stopped state and mode.
            tag.putBoolean("gt.magnet_stopped", converterStopped);
            tag.putByte("gt.magnet_mode", (byte) magnetMode());
            tag.putInt("gt.magnet_overloads", overloadCount);
        }
        turbine.saveConversion(tag);
        tag.putBoolean("gt.converter_stopped", converterStopped);
        if (behavior instanceof OriginalConverterNode converter) converter.save(tag);
        turbine.saveContents(tag, lookup);
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        bufferedInput = tag.getLong("gt.buffer");
        if (isSolar()) {
            solarPanel.load(tag, bufferedInput);
            bufferedInput = 0;
        }
        if (isBatteryBox()) batteryBox.energy.load(tag);
        batterySlots.load(tag, lookup);
        if (behavior instanceof TransformerNode.Electric electric) electric.control().load(tag);

        covers.clear();
        for (Direction side : Direction.values()) {
            String coverKey = "gt.battery_cover_" + side.ordinal();
            if (tag.contains(coverKey)) {
                covers.put(side, NbtCompat.readItem(tag.getCompound(coverKey), lookup));
            }
        }
        panels().loaded();

        inverted = tag.getBoolean("gt.inverted");
        if (behavior instanceof TransformerNode.Rotation rotation) rotation.negativeInput = tag.getBoolean("gt.rotation_negative_input");
        if (isMagnet()) overloadCount = Math.max(0, tag.getInt("gt.magnet_overloads"));
        turbine.loadConversion(tag);

        // Older magnet saves wrote a separate stopped flag and have no shared mode key.
        boolean usesLegacyMagnetKeys = isMagnet() && !tag.contains("gt.mode");
        converterStopped = tag.getBoolean(usesLegacyMagnetKeys ? "gt.magnet_stopped" : "gt.converter_stopped");
        if (behavior instanceof OriginalConverterNode converter) converter.load(tag, usesLegacyMagnetKeys);
        turbine.loadContents(tag, lookup);
    }

    @Override
    public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (ItemStack cover : covers.values()) {
            BlockContents.drop(this, cover);
        }
        covers.clear();
        batterySlots.dropContents();
        turbine.dropContents();
        setChanged();
    }
}
