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
import gregtech.content.energy.OriginalEnergyDeviceTooltipData;
import gregtech.content.energy.OriginalRotaryConverter;
import gregtech.content.registry.GTEnergyNodes;
import gregtech.engine.energy.EnergyConversionMeter;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyPackets;
import gregtech.engine.energy.EnergyTransfer;
import gregtech.engine.energy.MachineOverload;
import gregtech.engine.inventory.BlockContents;
import gregtech.engine.machine.AdjacentEnergyControl;
import gregtech.engine.stack.NbtCompat;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
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
    private final OriginalConverterNode originalConverter = new OriginalConverterNode(this);
    private final TransformerNode transformer = new TransformerNode(this);
    final SteamTurbineNode turbine = new SteamTurbineNode(this);
    private final ForgeEnergyNodeAdapter forgeEnergy = new ForgeEnergyNodeAdapter(this);
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
        }
    }

    public void setSpec(EnergyNodeSpec spec) {
        this.spec = spec;
        originalConverter.setSpec(spec);
        if (isSolar()) {
            if (solarPanel == null) {
                solarPanel = new SolarPanelNode(this, spec.outputRate());
            } else {
                solarPanel.configure(spec.outputRate());
            }
        }
        if (isBatteryBox()) {
            batteryBox = new BatteryBoxNode(this, spec, batterySlots);
        }
        if (isElectricTransformer()) {
            transformer.configureElectric();
        }
        if (spec.kind() == EnergyNodeSpec.Kind.TURBINE) {
            turbine.configure(spec.inputRate());
        }
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
        if (isSolar()) return solarPanel.energyAvailable();
        if (isBatteryBox()) return batteryBox.energy.buffer();
        return bufferedInput;
    }

    long capacity() {
        if (isBatteryBox()) return batterySlots.installedCapacity();
        return hasOriginalConverter() || isSteamConverter() ? spec.inputRate() * 2 : spec.capacity();
    }

    // Original converters

    public boolean hasOriginalConverter() {
        return originalConverter.hasState();
    }

    public boolean isOriginalRotaryConverter() {
        return originalConverter.isRotary();
    }

    public boolean isOriginalThermalConverter() {
        return originalConverter.isThermal();
    }

    public boolean isOriginalMotor() {
        return originalConverter.isMotor();
    }

    public boolean reverseMotor() {
        return originalConverter.reverseMotor();
    }

    public boolean motorCounterClockwise() {
        return originalConverter.counterClockwise();
    }

    public boolean converterStopped() {
        return converterStopped;
    }

    public boolean converterEmitted() {
        return originalConverter.emitted();
    }

    public boolean converterFast() {
        return originalConverter.fast();
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
        return isMagnet() ? originalConverter.mode() : 0;
    }

    // Transformers

    public boolean isInvertible() {
        return spec != null && spec.kind() == EnergyNodeSpec.Kind.CONVERTER && spec.inType() == spec.outType();
    }

    public boolean isElectricTransformer() {
        return spec != null && spec.id().startsWith("transformer_") && spec.inType() == GregTechTags.Energy.EU;
    }

    public boolean isRotationTransformer() {
        return spec != null && spec.id().startsWith("rotation_transformer_");
    }

    public boolean transformerInverted() {
        return transformer.inverted();
    }

    /** @return the new mode (true for step-up). */
    public boolean toggleInverted() {
        return transformer.toggleInverted();
    }

    long effectiveOutputRate() {
        return transformer.effectiveOutputRate();
    }

    // Solar panels

    public boolean isSolar() {
        return spec != null && spec.kind() == EnergyNodeSpec.Kind.SOLAR;
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
        return spec != null && spec.batterySlots() > 0;
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
        return isElectricTransformer() && transformer.electricControl().running();
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
        if (node.isBatteryBox()) {
            node.batteryBox.tick(level, pos, state);
            return;
        }
        if (!node.hasOriginalConverter() && !node.isSolar()
                && !node.isElectricTransformer() && !node.covers.isEmpty()) {
            node.panels().beforeTick();
            for (Direction side : Direction.values()) {
                ComponentCoverRuntime.tick(node, side, level.getGameTime());
            }
            node.panels().afterTick();
        }
        if (node.isSolar()) {
            node.solarPanel.tick(level, pos);
            return;
        }
        if (node.hasOriginalConverter()) {
            node.originalConverter.tick();
            return;
        }

        node.emitOutput();
        node.syncBufferedInput(level, pos, state);
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

    private void emitOutput() {
        if (level == null || converterStopped) return;
        if (isElectricTransformer()) {
            transformer.emitElectric();
            return;
        }
        if (isRotationTransformer()) {
            transformer.emitRotation();
            return;
        }

        if (bufferedInput < inputCostPerOutputPacket()) return;
        if (spec.outType() == GregTechTags.Energy.RF) {
            forgeEnergy.push();
            return;
        }

        long offeredPackets = bufferedInput / inputCostPerOutputPacket();
        long acceptedPackets = EnergyTransfer.emitEnergyToSide(
                spec.outType(), facing(), effectiveOutputRate(), offeredPackets, this);
        if (acceptedPackets > 0) {
            conversionMeter.output(level.getGameTime(), effectiveOutputRate(), acceptedPackets);
            bufferedInput -= acceptedPackets * inputCostPerOutputPacket();
            setChanged();
        }
    }

    void handleInputOverload(long packetSize) {
        handleConverterOverload(packetSize, spec.inType());
        bufferedInput = 0;
    }

    /** Input units consumed per emitted packet (solar generates, so 1:1 on output units). */
    long inputCostPerOutputPacket() {
        return switch (spec.kind()) {
            case SOLAR, STORAGE -> effectiveOutputRate();
            default -> Math.max(1, transformer.effectiveInputRate());
        };
    }

    // IEnergyBlock

    private boolean acceptsEnergyInput() {
        return spec != null && spec.kind() != EnergyNodeSpec.Kind.SOLAR;
    }

    boolean isOriginalCooler() {
        return originalConverter.isCooler();
    }

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        if (spec == null) return false;
        if (emitting) {
            return energyType == spec.outType() || (isOriginalCooler() && energyType == GregTechTags.Energy.HU);
        }
        return acceptsEnergyInput() && energyType == spec.inType();
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        if (spec == null) return List.of();
        if (isOriginalCooler()) return List.of(spec.inType(), spec.outType(), GregTechTags.Energy.HU);
        if (spec.inType() == spec.outType()) return List.of(spec.outType());
        return List.of(spec.inType(), spec.outType());
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        if (!acceptsEnergyInput() || energyType != spec.inType()) return false;
        if (hasOriginalConverter()) {
            return originalConverter.acceptsInputFrom(side, theoretical)
                    && super.isEnergyAcceptingFrom(energyType, side, theoretical);
        }
        if (isElectricTransformer() || isRotationTransformer()) return transformer.acceptsFrom(side, theoretical);
        if (isSteamConverter()) return !converterStopped && (side == null || side == facing().getOpposite());
        return side == null || side != facing();
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        if (spec == null) return false;
        if (isOriginalCooler()) return originalConverter.emitsCoolerTo(energyType, side);
        if (energyType != spec.outType()) return false;
        if (isSolar()) return solarPanel.emitsTo(side, theoretical);
        if (isBatteryBox()) return batteryBox.emitsTo(side, theoretical);
        if (isMagnet()) return side == null || side == facing() || side == facing().getOpposite();
        if (isElectricTransformer() || isRotationTransformer()) return transformer.emitsTo(side);
        return side == null || side == facing();
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (hasOriginalConverter()) return energyType == spec.inType() ? spec.inputRate() : 0;
        if (!acceptsEnergyInput() || energyType != spec.inType()) return 0;
        return isRotationTransformer() || isElectricTransformer()
                ? spec.inputRate()
                : transformer.effectiveInputRate();
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (hasOriginalConverter()) return energyType == spec.outType() ? spec.outputRate() : 0;
        return spec != null && energyType == spec.outType() ? effectiveOutputRate() : 0;
    }

    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (hasOriginalConverter()) {
            return energyType == spec.inType() ? OriginalRotaryConverter.inputMinimum(spec) : 0;
        }
        if (isBatteryBox()) {
            return energyType == spec.inType()
                    ? OriginalEnergyDeviceTooltipData.batteryInputMinimum(transformer.effectiveInputRate())
                    : 0;
        }
        if (!(isRotationTransformer() || isElectricTransformer()) || energyType != spec.inType()) {
            return super.getEnergySizeInputMin(energyType, side);
        }
        return transformer.minimumInput();
    }

    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (hasOriginalConverter()) return energyType == spec.inType() ? spec.inputRate() * 2 : 0;
        if ((isRotationTransformer() || isElectricTransformer()) && energyType == spec.inType()) {
            return transformer.maximumInput();
        }
        return super.getEnergySizeInputMax(energyType, side);
    }

    @Override
    public long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (hasOriginalConverter()) return energyType == spec.outType() ? spec.outputRate() / 2 : 0;
        if (isSolar()) return solarPanel.minOutput(energyType);
        if (isBatteryBox()) return energyType == spec.outType() ? effectiveOutputRate() : 0;
        if (!(isRotationTransformer() || isElectricTransformer()) || energyType != spec.outType()) {
            return super.getEnergySizeOutputMin(energyType, side);
        }
        return transformer.minimumOutput();
    }

    @Override
    public long getEnergySizeOutputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (hasOriginalConverter()) return energyType == spec.outType() ? spec.outputRate() * 2 : 0;
        if (isSolar()) return solarPanel.maxOutput(energyType);
        if (isBatteryBox()) return energyType == spec.outType() ? effectiveOutputRate() : 0;
        return super.getEnergySizeOutputMax(energyType, side);
    }

    private boolean acceptsBufferedInputFrom(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (!acceptsEnergyInput() || energyType != spec.inType()) return false;
        if (isMagnet() || isElectricTransformer() || isRotationTransformer()) {
            return isEnergyAcceptingFrom(energyType, side, false);
        }
        return true;
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long packetSize) {
        // The original converters are discovered by simulated injection.
        if (hasOriginalConverter()) return 0;
        if (isBatteryBox()) return batteryBox.demanded(energyType, side, packetSize);
        long magnitude = EnergyPackets.magnitude(packetSize, energyType == GregTechTags.Energy.RU);
        if (magnitude == 0 || !acceptsBufferedInputFrom(energyType, side)) return 0;
        return Math.max(0, capacity() - bufferedInput) / magnitude;
    }

    /** These devices push during their own tick and refuse energy pull requests. */
    private boolean usesPushOnlyOutput() {
        return hasOriginalConverter() || isSteamConverter() || isRotationTransformer()
                || isElectricTransformer() || isMagnet() || isBatteryBox();
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag energyType, @Nullable Direction side, long packetSize) {
        if (isSolar()) return solarPanel.offered(energyType, side);
        if (usesPushOnlyOutput()) return 0;
        if (spec == null || energyType != spec.outType() || packetSize <= 0) return 0;
        long availablePackets = bufferedInput / inputCostPerOutputPacket();
        return Math.min(availablePackets, (availablePackets * effectiveOutputRate()) / packetSize);
    }

    @Override
    public long doInject(GregTechTags.Tag energyType, @Nullable Direction side, long packetSize, long packetCount,
            boolean execute) {
        if (hasOriginalConverter()) {
            return originalConverter.inject(energyType, side, packetSize, packetCount, execute);
        }
        if (isBatteryBox()) return batteryBox.inject(energyType, side, packetSize, packetCount, execute);
        if (isSteamConverter()) return turbine.inject(energyType, side, packetSize, packetCount, execute);

        long magnitude = EnergyPackets.magnitude(packetSize, energyType == GregTechTags.Energy.RU);
        if (packetCount <= 0 || magnitude == 0 || !acceptsBufferedInputFrom(energyType, side)) return 0;
        if ((isMagnet() || isElectricTransformer()) && magnitude > getEnergySizeInputMax(energyType, side)) {
            if (execute) {
                if (level != null) conversionMeter.input(level.getGameTime(), magnitude, packetCount);
                handleInputOverload(magnitude);
            }
            return packetCount;
        }
        long freeCapacity = Math.max(0, capacity() - bufferedInput);
        long acceptedPackets = EnergyPackets.fitting(packetCount, freeCapacity, magnitude);
        if (execute && acceptedPackets > 0) {
            if (isRotationTransformer()) transformer.recordRotationInput(packetSize);
            long acceptedEnergy = acceptedPackets * magnitude;
            bufferedInput += acceptedEnergy;
            if (level != null) conversionMeter.input(level.getGameTime(), acceptedEnergy);
            setChanged();
        }
        return acceptedPackets;
    }

    @Override
    public long doExtract(GregTechTags.Tag energyType, @Nullable Direction side, long packetSize, long packetCount,
            boolean execute) {
        if (isSolar()) return solarPanel.extract(energyType, side, packetSize, packetCount, execute);
        if (usesPushOnlyOutput()) return 0;
        if (spec == null || energyType != spec.outType() || packetCount <= 0 || packetSize <= 0) return 0;
        long packetCost = inputCostPerOutputPacket();
        long availablePackets = bufferedInput / packetCost;
        long requestedOutputPackets = (packetCount * packetSize) / Math.max(1, effectiveOutputRate());
        long outputPackets = Math.min(availablePackets, Math.max(0, requestedOutputPackets));
        long extractedPackets = (outputPackets * effectiveOutputRate()) / packetSize;
        if (execute && extractedPackets > 0) {
            if (level != null) conversionMeter.output(level.getGameTime(), packetSize, extractedPackets);
            bufferedInput -= outputPackets * packetCost;
            setChanged();
        }
        return extractedPackets;
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyCapacitorTypes(@Nullable Direction side) {
        if (isSolar()) return List.of();
        if (isBatteryBox()) return List.of(spec.inType());
        return super.getEnergyCapacitorTypes(side);
    }

    @Override
    public long getEnergyStored(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (isSolar()) return 0; // The solar panel has no energy capacitor.
        if (isBatteryBox()) return batteryBox.stored(energyType);
        return spec != null && (energyType == spec.inType() || energyType == spec.outType()) ? bufferedInput : 0;
    }

    @Override
    public long getEnergyCapacity(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (isSolar()) return 0;
        if (isBatteryBox()) return batteryBox.capacity(energyType);
        return spec != null ? capacity() : 0;
    }

    // Machine controls

    @Override
    public AdjacentEnergyControl adjacentEnergyControl() {
        return originalConverter.adjacentEnergyControl();
    }

    @Override
    public MachineControl machineControl(Direction side) {
        if (hasOriginalConverter()) return originalConverter;
        if (isSolar()) return solarPanel;
        if (isElectricTransformer()) return transformer.electricControl();
        if (isSteamConverter()) return turbine;
        if (isBatteryBox()) return batteryBox;
        return new BufferedNodeControl();
    }

    private final class BufferedNodeControl implements MachineControl {
        @Override
        public boolean available() {
            return !isRemoved();
        }

        @Override
        public boolean supportsMode() {
            return false;
        }

        @Override
        public boolean enabled() {
            return !converterStopped;
        }

        @Override
        public boolean setEnabled(boolean enabled) {
            converterStopped = !enabled;
            setChanged();
            return enabled;
        }

        @Override
        public boolean running() {
            return enabled() && stored() > 0;
        }

        @Override
        public boolean active() {
            return running();
        }

        @Override
        public long progress() {
            return stored();
        }

        @Override
        public long progressMax() {
            return capacity();
        }
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
        transformer.save(tag);
        if (isMagnet()) {
            // Older magnet saves use these keys for the converter's stopped state and mode.
            tag.putBoolean("gt.magnet_stopped", converterStopped);
            tag.putByte("gt.magnet_mode", (byte) originalConverter.mode());
            tag.putInt("gt.magnet_overloads", overloadCount);
        }
        turbine.saveConversion(tag);
        tag.putBoolean("gt.converter_stopped", converterStopped);
        originalConverter.save(tag);
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
        if (isElectricTransformer()) transformer.electricControl().load(tag);

        covers.clear();
        for (Direction side : Direction.values()) {
            String coverKey = "gt.battery_cover_" + side.ordinal();
            if (tag.contains(coverKey)) {
                covers.put(side, NbtCompat.readItem(tag.getCompound(coverKey), lookup));
            }
        }
        panels().loaded();

        transformer.loadDirection(tag);
        if (isMagnet()) overloadCount = Math.max(0, tag.getInt("gt.magnet_overloads"));
        turbine.loadConversion(tag);

        // Older magnet saves wrote a separate stopped flag and have no shared mode key.
        boolean usesLegacyMagnetKeys = isMagnet() && !tag.contains("gt.mode");
        converterStopped = tag.getBoolean(usesLegacyMagnetKeys ? "gt.magnet_stopped" : "gt.converter_stopped");
        originalConverter.load(tag, usesLegacyMagnetKeys);
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
