package gregtech.content.blockentity.energy;

import gregtech.api.platform.machine.MachineControl;
import gregtech.content.block.energy.ElectricTransformerBlock;
import gregtech.content.block.energy.EnergyNodeBlock;
import gregtech.content.block.energy.MagnetMachineBlock;
import gregtech.content.block.energy.OriginalMotorBlock;
import gregtech.content.block.energy.RotaryConverterBlock;
import gregtech.content.block.energy.SolarPanelBlock;
import gregtech.content.blockentity.GTEnergyBlockEntity;
import gregtech.content.cover.ComponentCoverAccess;
import gregtech.content.cover.ComponentCoverRuntime;
import gregtech.content.cover.CoverAdmission;
import gregtech.content.cover.CoverItems;
import gregtech.content.cover.PanelCover;
import gregtech.content.cover.PanelCoverHost;
import gregtech.content.cover.PanelCoverRuntime;
import gregtech.content.data.GregTechTags;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.energy.BatteryBoxEnergy;
import gregtech.content.energy.ElectricTransformerControl;
import gregtech.content.energy.MagnetMachineDefinitions;
import gregtech.content.energy.OriginalAdjacentEnergyRules;
import gregtech.content.energy.OriginalEnergyDeviceTooltipData;
import gregtech.content.energy.OriginalRotaryConverter;
import gregtech.content.energy.OriginalSteamTurbines;
import gregtech.content.energy.OriginalThermalConverter;
import gregtech.content.registry.GTEnergyNodes;
import gregtech.content.registry.GTFluids;
import gregtech.engine.energy.EnergyConversionMeter;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyPackets;
import gregtech.engine.energy.EnergyTransfer;
import gregtech.engine.energy.MachineOverload;
import gregtech.engine.energy.SolarPanelEnergy;
import gregtech.engine.energy.SteamTurbineConversion;
import gregtech.engine.fluid.FluidTankGT;
import gregtech.engine.inventory.BlockContents;
import gregtech.engine.machine.AdjacentEnergyControl;
import gregtech.engine.material.MaterialFormItem;
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
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
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
    private long bufferedInput;
    private long lastSyncedBuffer = -1;

    private final EnumMap<Direction, ItemStack> covers = new EnumMap<>(Direction.class);
    private PanelCoverRuntime controlPanels;

    private OriginalRotaryConverter.State converterState;
    private boolean converterStopped;
    private int overloadCount;

    private ElectricTransformerControl transformerControl;
    private boolean inverted;
    private boolean negativeRotationInput;

    private SolarPanelEnergy solarEnergy;
    private final BatteryBoxInventory batterySlots = new BatteryBoxInventory(this);
    @Nullable
    private BatteryBoxNode batteryBox;

    @Nullable
    private FluidTankGT steamTank;
    private long pendingTurbineEnergy;
    /** Steam counted towards the next litre of condensate. */
    private long steamRemainder;
    /** Returned to players who installed a rotor in an older version. */
    private ItemStack legacyRotor = ItemStack.EMPTY;

//? if <1.21.1 {
    private final EnumMap<Direction, LazyOptional<IFluidHandler>> sidedSteamCapabilities =
            new EnumMap<>(Direction.class);
    @Nullable
    private LazyOptional<IFluidHandler> unsidedSteamCapability;
    @Nullable
    private LazyOptional<IEnergyStorage> unsidedEnergyCapability;
    private final EnumMap<Direction, LazyOptional<IEnergyStorage>> sidedEnergyCapabilities =
            new EnumMap<>(Direction.class);
//?}

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
        boolean usesConverterState = OriginalRotaryConverter.handles(spec)
                || OriginalThermalConverter.handles(spec)
                || MagnetMachineDefinitions.handles(spec)
                || OriginalSteamTurbines.handles(spec);
        converterState = usesConverterState ? new OriginalRotaryConverter.State(spec) : null;

        if (isSolar()) {
            solarEnergy = new SolarPanelEnergy(spec.outputRate(), worldPosition.hashCode());
        }
        if (isBatteryBox()) {
            batteryBox = new BatteryBoxNode(this, spec, batterySlots);
        }
        if (isElectricTransformer()) {
            transformerControl = new ElectricTransformerControl(this::setChanged);
        }
        if (spec.kind() == EnergyNodeSpec.Kind.TURBINE) {
            steamTank = new FluidTankGT(spec.inputRate() * 4).setOnChanged(this::setChanged);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (isSolar() && level != null && !level.isClientSide && !isRemoved() && facing() == Direction.UP) {
            level.setBlock(
                    worldPosition, getBlockState().setValue(DirectionalBlock.FACING, Direction.DOWN), Block.UPDATE_ALL);
        }
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
        if (isSolar()) return solarEnergy.energy();
        if (isBatteryBox()) return batteryBox.energy.buffer();
        return bufferedInput;
    }

    private long capacity() {
        if (isBatteryBox()) return batterySlots.installedCapacity();
        return hasOriginalConverter() || isSteamConverter() ? spec.inputRate() * 2 : spec.capacity();
    }

    // Original converters

    public boolean hasOriginalConverter() {
        return converterState != null;
    }

    public boolean isOriginalRotaryConverter() {
        return hasOriginalConverter() && OriginalRotaryConverter.handles(spec);
    }

    public boolean isOriginalThermalConverter() {
        return hasOriginalConverter() && OriginalThermalConverter.handles(spec);
    }

    public boolean isOriginalMotor() {
        return hasOriginalConverter() && OriginalRotaryConverter.motorBehavior(spec);
    }

    public boolean reverseMotor() {
        bufferedInput = 0;
        boolean counterClockwise = converterState.reverse();
        setChanged();
        return counterClockwise;
    }

    public boolean motorCounterClockwise() {
        return isOriginalMotor() && converterState.counterClockwise();
    }

    public boolean converterStopped() {
        return converterStopped;
    }

    public boolean converterEmitted() {
        return converterState != null && converterState.emitted();
    }

    public boolean converterFast() {
        return converterState != null && converterState.fast();
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
        return isMagnet() ? converterState.mode() : 0;
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
        return inverted;
    }

    /** @return the new mode (true for step-up). */
    public boolean toggleInverted() {
        if (isRotationTransformer() || isElectricTransformer()) {
            bufferedInput = 0;
            negativeRotationInput = false;
        }
        inverted = !inverted;
        if (isElectricTransformer()) transformerControl.select(inverted);
        setChanged();
        return inverted;
    }

    private long effectiveInputRate() {
        return inverted ? spec.outputRate() : spec.inputRate();
    }

    long effectiveOutputRate() {
        return inverted ? spec.inputRate() : spec.outputRate();
    }

    private Direction rotationInputFace() {
        return inverted ? facing().getOpposite() : facing();
    }

    private Direction rotationOutputFace() {
        return inverted ? facing() : facing().getOpposite();
    }

    /** Both modes use the original input rating as the recommended packet size. */
    private long recommendedTransformerInput() {
        return spec.inputRate();
    }

    private long maximumTransformerInput() {
        return inverted ? Math.max(spec.inputRate(), spec.outputRate() * 8) : spec.inputRate() * 2;
    }

    private long rotationOutputSize() {
        // In reverse, input and output use the same rating, so the packet holds the entire buffer.
        return inverted ? bufferedInput : bufferedInput * spec.outputRate() / spec.inputRate();
    }

    // Solar panels

    public boolean isSolar() {
        return spec != null && spec.kind() == EnergyNodeSpec.Kind.SOLAR;
    }

    public boolean solarSkyVisible() {
        return solarEnergy != null && solarEnergy.skyVisible();
    }

    public void checkSolarSky() {
        if (isSolar()) solarEnergy.checkSky();
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
        return steamTank == null ? 0 : steamTank.getAmount();
    }

    public long turbineSteamCapacity() {
        return steamTank == null ? 0 : steamTank.capacity();
    }

    public static boolean isRotorItem(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof MaterialFormItem materialItem
                && materialItem.getPrefix() == MaterialPrefix.rotor;
    }

    public boolean hasRotor() {
        return !legacyRotor.isEmpty();
    }

    /** Turbines consume their rotor in the crafting recipe. */
    public boolean installRotor(ItemStack stack) {
        return false;
    }

    public ItemStack removeRotor() {
        ItemStack removedRotor = legacyRotor;
        legacyRotor = ItemStack.EMPTY;
        setChanged();
        return removedRotor;
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
        return isElectricTransformer() && transformerControl.running();
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
            node.tickSolar(level, pos);
            return;
        }
        if (node.hasOriginalConverter()) {
            node.tickOriginalConverter();
            return;
        }

        node.emitOutput();
        if (level.getGameTime() % 20 == 0 && node.lastSyncedBuffer != node.bufferedInput) {
            node.lastSyncedBuffer = node.bufferedInput;
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

    // Steam turbine

    public long purgeTurbineSteam() {
        if (!isTurbine() || steamTank == null) return 0;
        long removedSteam = steamTank.getAmount();
        steamTank.remove(removedSteam);
        setChanged();
        return removedSteam;
    }

    public boolean isSteamConverter() {
        return isTurbine();
    }

    public int converterOverloads() {
        return overloadCount;
    }

    private void tickTurbine() {
        if (steamTank == null || spec.inputRate() <= 0) return;
        // Older saves can contain other fluids. Discard them before converting steam into RU.
        if (!steamTank.getFluid().isEmpty() && !turbineInlet().isFluidValid(0, steamTank.getFluid())) {
            steamTank.remove(steamTank.getAmount());
            setChanged();
        }

        var conversion = SteamTurbineConversion.step(
                bufferedInput, pendingTurbineEnergy, steamTank.getAmount(), steamRemainder, spec.inputRate());
        bufferedInput = conversion.energy();
        pendingTurbineEnergy = conversion.pending();
        steamRemainder = conversion.remainder();
        if (conversion.consumed() > 0) {
            steamTank.remove(conversion.consumed());
        }
        emitCondensate(conversion.condensate());
        setChanged();
    }

    private void emitCondensate(long amount) {
        var distilledWater = GTFluids.still("DistW");
        if (level == null || amount <= 0 || distilledWater == null) return;
        var condensate = new FluidStack(distilledWater.get(), (int) Math.min(Integer.MAX_VALUE, amount));
        for (Direction side : Direction.values()) {
            if (side.getAxis() == facing().getAxis() || !level.hasChunkAt(worldPosition.relative(side))) continue;
            var neighbor = level.getBlockEntity(worldPosition.relative(side));
            if (neighbor == null) continue;
//? if >=1.21.1 {
            /*var receiver = level.getCapability(
                    Capabilities.FluidHandler.BLOCK, worldPosition.relative(side), side.getOpposite());
*///?} else {
            var receiver = neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite())
                    .resolve()
                    .orElse(null);
//?}
            if (receiver != null) {
                int offered = condensate.getAmount();
                int accepted = receiver.fill(condensate.copy(), IFluidHandler.FluidAction.EXECUTE);
                condensate.shrink(Math.max(0, Math.min(offered, accepted)));
            }
            if (condensate.isEmpty()) break;
        }
        // The turbine discards condensate that no adjacent tank recovers.
    }

    private IFluidHandler turbineInlet() {
        return turbineInlet(null);
    }

    private IFluidHandler turbineInlet(@Nullable Direction side) {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return side == facing() ? 0 : 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return getTanks() == 0 ? FluidStack.EMPTY : steamTank.getFluid().copy();
            }

            @Override
            public int getTankCapacity(int tank) {
                return getTanks() == 0 ? 0 : (int) steamTank.capacity();
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack fluid) {
                var steam = GTFluids.still("Steam");
                return !fluid.isEmpty() && steam != null && fluid.getFluid().isSame(steam.get());
            }

            @Override
            public int fill(FluidStack fluid, FluidAction action) {
                int filled = 0;
                if (!isRemoved() && !converterStopped
                        && (side == null || side == facing().getOpposite())
                        && isFluidValid(0, fluid)) {
                    filled = steamTank.fill(fluid, action);
                }
                if (action.execute() && level != null) {
                    conversionMeter.input(level.getGameTime(), filled);
                }
                return filled;
            }

            @Override
            public FluidStack drain(FluidStack fluid, FluidAction action) {
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int amount, FluidAction action) {
                return FluidStack.EMPTY;
            }
        };
    }

    // Energy conversion and output

    private void tickSolar(Level level, BlockPos pos) {
        panels().beforeTick();
        var conditions = new SolarPanelEnergy.Conditions(
                level.isDay(),
                level.isRaining(),
                level.isThundering(),
                level.dimension().location().toString().equals("twilightforest:twilight_forest"),
                level.getBiome(pos).value().getModifiedClimateSettings().downfall());
        solarEnergy.tick(
                () -> level.dimensionType().hasSkyLight() && level.canSeeSky(pos.above()),
                conditions,
                energy -> EnergyTransfer.emitEnergyToNetwork(spec.outType(), energy, 1, this));

        var state = getBlockState();
        var activeProperty = SolarPanelBlock.ACTIVE;
        if (state.hasProperty(activeProperty) && state.getValue(activeProperty) != solarEnergy.active()) {
            level.setBlock(pos, state.setValue(activeProperty, solarEnergy.active()), Block.UPDATE_CLIENTS);
        }
        panels().afterTick();
        setChanged();
    }

    private void tickOriginalConverter() {
        panels().beforeTick();
        for (Direction side : Direction.values()) {
            ComponentCoverRuntime.tick(this, side, level.getGameTime());
        }
        if (isTurbine()) tickTurbine();

        long bufferBeforeConversion = bufferedInput;
        var conversion = converterState.tick(bufferedInput, converterStopped, this::emitConvertedEnergy);
        bufferedInput = conversion.energy();
        if (conversion.overloaded()) {
            handleConverterOverload(bufferBeforeConversion, spec.outType());
        }
        if (isRemoved() || level.getBlockEntity(worldPosition) != this) return;
        if (level.getGameTime() % 600 == 5 && !conversion.possible() && overloadCount > 0) {
            overloadCount--;
        }

        var state = getBlockState();
        // Magnet activity levels 1 and 2 use the same overlay.
        var nextState = isMagnet()
                ? state.setValue(MagnetMachineBlock.ACTIVE, conversion.visual() > 0)
                : state.setValue(RotaryConverterBlock.ACTIVITY, conversion.visual());
        if (isOriginalMotor()) {
            nextState = nextState
                    .setValue(OriginalMotorBlock.COUNTER_CLOCKWISE, converterState.counterClockwise())
                    .setValue(OriginalMotorBlock.FAST, conversion.fast());
        }
        if (nextState != state) {
            level.setBlock(worldPosition, nextState, Block.UPDATE_CLIENTS);
        } else if (level.getGameTime() % 20 == 0 && lastSyncedBuffer != bufferedInput) {
            lastSyncedBuffer = bufferedInput;
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
        panels().afterTick();
        setChanged();
    }

    private long emitConvertedEnergy(long packetSize, long offeredPackets) {
        if (isMagnet()) {
            long positivePackets = EnergyTransfer.emitEnergyToSide(
                    spec.outType(), facing(), packetSize, offeredPackets, this);
            long negativePackets = EnergyTransfer.emitEnergyToSide(
                    spec.outType(), facing().getOpposite(), -packetSize, offeredPackets, this);
            long acceptedPackets = positivePackets + negativePackets;
            conversionMeter.output(level.getGameTime(), packetSize, acceptedPackets);
            return acceptedPackets;
        }
        if (isOriginalCooler()) {
            long coldPackets = EnergyTransfer.emitEnergyToSide(
                    spec.outType(), facing(), packetSize, offeredPackets, this);
            long heatPackets = EnergyTransfer.emitEnergyToSide(
                    GregTechTags.Energy.HU, facing().getOpposite(), packetSize, offeredPackets, this);
            conversionMeter.output(level.getGameTime(), packetSize, coldPackets);
            conversionMeter.auxiliaryOutput(level.getGameTime(), packetSize, heatPackets);
            return coldPackets > 0 || heatPackets > 0 ? 1 : 0;
        }

        long acceptedPackets = EnergyTransfer.emitEnergyToNetwork(spec.outType(), packetSize, offeredPackets, this);
        conversionMeter.output(level.getGameTime(), packetSize, acceptedPackets);
        return acceptedPackets;
    }

    private void handleConverterOverload(long packetSize, GregTechTags.Tag energyType) {
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
            emitElectricTransformer();
            return;
        }
        if (isRotationTransformer()) {
            emitRotationTransformer();
            return;
        }

        if (bufferedInput < inputCostPerOutputPacket()) return;
        if (spec.outType() == GregTechTags.Energy.RF) {
            pushForgeEnergy();
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

    private void handleInputOverload(long packetSize) {
        handleConverterOverload(packetSize, spec.inType());
        bufferedInput = 0;
    }

    private void emitElectricTransformer() {
        panels().beforeTick();
        long offeredPackets = inverted ? 1 : spec.inputRate() / spec.outputRate();
        long packetSize = inverted ? bufferedInput : bufferedInput / offeredPackets;
        long maxPacketSize = getEnergySizeOutputMax(spec.outType(), null);
        if (transformerControl.mode() > 0) {
            packetSize = Math.min(packetSize, transformerControl.limit(maxPacketSize));
        }

        boolean canEmit = packetSize >= getEnergySizeOutputMin(spec.outType(), null);
        boolean emitted = false;
        if (canEmit) {
            if (packetSize > maxPacketSize) {
                handleInputOverload(packetSize);
            } else {
                long receivedPackets = EnergyTransfer.emitEnergyToNetwork(
                        spec.outType(), packetSize, offeredPackets, this);
                long acceptedPackets = Math.min(offeredPackets, receivedPackets);
                conversionMeter.output(level.getGameTime(), packetSize, acceptedPackets);
                if (receivedPackets > 0) {
                    bufferedInput -= acceptedPackets * packetSize;
                    emitted = true;
                }
            }
        }

        transformerControl.tick(canEmit, emitted);
        if (level.getGameTime() % 600 == 5 && !canEmit && overloadCount > 0) {
            overloadCount--;
        }
        var activityProperty = ElectricTransformerBlock.ACTIVITY;
        if (getBlockState().hasProperty(activityProperty)
                && getBlockState().getValue(activityProperty) != transformerControl.visual()) {
            level.setBlock(
                    worldPosition,
                    getBlockState().setValue(activityProperty, transformerControl.visual()),
                    Block.UPDATE_ALL);
        }
        panels().afterTick();
    }

    private void emitRotationTransformer() {
        long packetSize = rotationOutputSize();
        long minPacketSize = getEnergySizeOutputMin(spec.outType(), rotationOutputFace());
        long maxPacketSize = getEnergySizeOutputMax(spec.outType(), rotationOutputFace());
        if (packetSize >= minPacketSize && packetSize <= maxPacketSize) {
            long acceptedPackets = EnergyTransfer.emitEnergyToSide(
                    spec.outType(),
                    rotationOutputFace(),
                    negativeRotationInput ? -packetSize : packetSize,
                    inverted ? 1 : 4,
                    this);
            conversionMeter.output(level.getGameTime(), packetSize, acceptedPackets);
        }
        // NBT_WASTE_ENERGY drains up to the input maximum each tick, even when no receiver accepts a packet.
        if (bufferedInput > 0) {
            bufferedInput = Math.max(0, bufferedInput - maximumTransformerInput());
            setChanged();
        }
    }

    /** Push whole RF packets into the Forge Energy storage in front. */
    private void pushForgeEnergy() {
        var neighbor = level.getBlockEntity(worldPosition.relative(facing()));
        if (neighbor == null) return;
//? if >=1.21.1 {
        /*var storage = level.getCapability(
                Capabilities.EnergyStorage.BLOCK, neighbor.getBlockPos(), facing().getOpposite());
*///?} else {
        var storage = neighbor.getCapability(ForgeCapabilities.ENERGY, facing().getOpposite())
                .resolve()
                .orElse(null);
//?}
        if (storage == null || !storage.canReceive()) return;

        long inputCost = inputCostPerOutputPacket();
        int packetSize = (int) effectiveOutputRate();
        for (int packet = 0; packet < 64 && bufferedInput >= inputCost; packet++) {
            if (storage.receiveEnergy(packetSize, true) < packetSize) break;
            storage.receiveEnergy(packetSize, false);
            conversionMeter.output(level.getGameTime(), packetSize, 1);
            bufferedInput -= inputCost;
            setChanged();
        }
    }

    /** Input units consumed per emitted packet (solar generates, so 1:1 on output units). */
    private long inputCostPerOutputPacket() {
        return switch (spec.kind()) {
            case SOLAR, STORAGE -> effectiveOutputRate();
            default -> Math.max(1, effectiveInputRate());
        };
    }

    // IEnergyBlock

    private boolean acceptsEnergyInput() {
        return spec != null && spec.kind() != EnergyNodeSpec.Kind.SOLAR;
    }

    private boolean isOriginalCooler() {
        return isOriginalThermalConverter() && OriginalThermalConverter.cooler(spec);
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
            return (theoretical || !converterStopped)
                    && acceptsOriginalConverterInputFrom(side)
                    && super.isEnergyAcceptingFrom(energyType, side, theoretical);
        }
        if (isElectricTransformer()) {
            if (!theoretical && !transformerControl.accepts()) return false;
            return side == null || (inverted ? side != facing() : side == facing());
        }
        if (isRotationTransformer()) return side == null || side == rotationInputFace();
        if (isSteamConverter()) return !converterStopped && (side == null || side == facing().getOpposite());
        return side == null || side != facing();
    }

    private boolean acceptsOriginalConverterInputFrom(@Nullable Direction side) {
        if (side == null) return true;
        if (isMagnet()) return side != facing() && side != facing().getOpposite();
        if (isOriginalThermalConverter()) {
            return side != facing() && (!OriginalThermalConverter.cooler(spec) || side != facing().getOpposite());
        }
        if (isOriginalMotor() && !isTurbine()) return side != facing();
        return side == facing().getOpposite();
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        if (spec == null) return false;
        if (isOriginalCooler()) {
            if (energyType == spec.outType()) return side == null || side == facing();
            return energyType == GregTechTags.Energy.HU && (side == null || side == facing().getOpposite());
        }
        if (energyType != spec.outType()) return false;
        if (isSolar()) {
            return (side == null || side == facing()) && (theoretical || side == null || !panels().shuttered(side));
        }
        if (isBatteryBox()) return batteryBox.emitsTo(side, theoretical);
        if (isMagnet()) return side == null || side == facing() || side == facing().getOpposite();
        if (isElectricTransformer()) return side == null || (inverted ? side == facing() : side != facing());
        if (isRotationTransformer()) return side == null || side == rotationOutputFace();
        return side == null || side == facing();
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (hasOriginalConverter()) return energyType == spec.inType() ? spec.inputRate() : 0;
        if (!acceptsEnergyInput() || energyType != spec.inType()) return 0;
        return isRotationTransformer() || isElectricTransformer()
                ? recommendedTransformerInput()
                : effectiveInputRate();
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
                    ? OriginalEnergyDeviceTooltipData.batteryInputMinimum(effectiveInputRate())
                    : 0;
        }
        if (!(isRotationTransformer() || isElectricTransformer()) || energyType != spec.inType()) {
            return super.getEnergySizeInputMin(energyType, side);
        }
        if (!inverted) return spec.inputRate() <= 16 ? 1 : spec.inputRate() / 2;
        long minimumOutputSize = Math.max(1, spec.outputRate() / 2);
        return minimumOutputSize <= 8 ? 1 : minimumOutputSize;
    }

    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (hasOriginalConverter()) return energyType == spec.inType() ? spec.inputRate() * 2 : 0;
        if ((isRotationTransformer() || isElectricTransformer()) && energyType == spec.inType()) {
            return maximumTransformerInput();
        }
        return super.getEnergySizeInputMax(energyType, side);
    }

    @Override
    public long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (hasOriginalConverter()) return energyType == spec.outType() ? spec.outputRate() / 2 : 0;
        if (isSolar()) return energyType == spec.outType() ? spec.outputRate() / 8 : 0;
        if (isBatteryBox()) return energyType == spec.outType() ? effectiveOutputRate() : 0;
        if (!(isRotationTransformer() || isElectricTransformer()) || energyType != spec.outType()) {
            return super.getEnergySizeOutputMin(energyType, side);
        }
        return inverted ? spec.inputRate() * 3 / 4 : Math.max(1, spec.outputRate() / 2);
    }

    @Override
    public long getEnergySizeOutputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (hasOriginalConverter()) return energyType == spec.outType() ? spec.outputRate() * 2 : 0;
        if (isSolar()) return energyType == spec.outType() ? spec.outputRate() : 0;
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
        if (isSolar()) return isEnergyEmittingTo(energyType, side, false) ? solarEnergy.energy() : 0;
        if (usesPushOnlyOutput()) return 0;
        if (spec == null || energyType != spec.outType() || packetSize <= 0) return 0;
        long availablePackets = bufferedInput / inputCostPerOutputPacket();
        return Math.min(availablePackets, (availablePackets * effectiveOutputRate()) / packetSize);
    }

    @Override
    public long doInject(GregTechTags.Tag energyType, @Nullable Direction side, long packetSize, long packetCount,
            boolean execute) {
        if (hasOriginalConverter()) {
            return injectOriginalConverter(energyType, side, packetSize, packetCount, execute);
        }
        if (isBatteryBox()) return batteryBox.inject(energyType, side, packetSize, packetCount, execute);
        if (isSteamConverter()) return injectSteamConverter(energyType, side, packetSize, packetCount, execute);

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
            if (isRotationTransformer()) negativeRotationInput = packetSize < 0;
            long acceptedEnergy = acceptedPackets * magnitude;
            bufferedInput += acceptedEnergy;
            if (level != null) conversionMeter.input(level.getGameTime(), acceptedEnergy);
            setChanged();
        }
        return acceptedPackets;
    }

    private long injectOriginalConverter(GregTechTags.Tag energyType, @Nullable Direction side, long packetSize,
            long packetCount, boolean execute) {
        if (!isEnergyAcceptingFrom(energyType, side, false)) return 0;
        var injection = converterState.inject(bufferedInput, packetSize, packetCount, execute);
        if (execute) {
            if (level != null) conversionMeter.input(level.getGameTime(), packetSize, injection.consumed());
            bufferedInput = injection.energy();
            if (injection.overloaded()) handleConverterOverload(Math.abs(packetSize), energyType);
            if (injection.consumed() > 0) setChanged();
        }
        return injection.consumed();
    }

    private long injectSteamConverter(GregTechTags.Tag energyType, @Nullable Direction side, long packetSize,
            long packetCount, boolean execute) {
        if (packetCount <= 0 || packetSize == 0 || packetSize == Long.MIN_VALUE
                || !isEnergyAcceptingFrom(energyType, side, false)) {
            return 0;
        }
        long magnitude = Math.abs(packetSize);
        if (magnitude > spec.inputRate() * 2) {
            if (execute) handleInputOverload(magnitude);
            return packetCount;
        }
        long freeCapacity = Math.max(0, capacity() - bufferedInput);
        long packetsToFill = freeCapacity / magnitude + (freeCapacity % magnitude == 0 ? 0 : 1);
        long acceptedPackets = Math.min(packetCount, packetsToFill);
        if (execute && acceptedPackets > 0) {
            long acceptedEnergy = magnitude * acceptedPackets;
            if (level != null) conversionMeter.input(level.getGameTime(), acceptedEnergy);
            bufferedInput += acceptedEnergy;
            setChanged();
        }
        return acceptedPackets;
    }

    @Override
    public long doExtract(GregTechTags.Tag energyType, @Nullable Direction side, long packetSize, long packetCount,
            boolean execute) {
        if (isSolar()) {
            if (!isEnergyEmittingTo(energyType, side, false)) return 0;
            long extractedPackets = solarEnergy.extract(packetSize, packetCount, execute);
            if (execute && extractedPackets > 0) setChanged();
            return extractedPackets;
        }
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
        if (!hasOriginalConverter() || !OriginalAdjacentEnergyRules.respondsToAdjacent(spec)) return null;
        return enabled -> {
            // Manual and adjacent requests share the stopped state. Stored energy still drains normally.
            if (!isRemoved() && (level == null || !level.isClientSide) && converterStopped == enabled) {
                converterStopped = !enabled;
                setChanged();
            }
            return !converterStopped;
        };
    }

    @Override
    public MachineControl machineControl(Direction side) {
        if (hasOriginalConverter()) return new OriginalConverterControl();
        if (isSolar()) return new SolarControl();
        if (isElectricTransformer()) return transformerControl;
        if (isSteamConverter()) return new SteamConverterControl();
        if (isBatteryBox()) return batteryBox;
        return new BufferedNodeControl();
    }

    private final class OriginalConverterControl implements MachineControl {
        @Override
        public boolean available() {
            return !isRemoved();
        }

        @Override
        public boolean supportsProgress() {
            return false;
        }

        @Override
        public boolean supportsMode() {
            return isOriginalMotor() || isMagnet() || OriginalThermalConverter.modeSelectable(spec);
        }

        @Override
        public int mode() {
            return converterState.mode();
        }

        @Override
        public int setMode(int value) {
            if (supportsMode()) converterState.mode(value);
            setChanged();
            return mode();
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
            return converterState.possible();
        }

        @Override
        public boolean active() {
            return converterState.emitted();
        }

        @Override
        public long progress() {
            return 0;
        }

        @Override
        public long progressMax() {
            return 0;
        }
    }

    private final class SolarControl implements MachineControl {
        @Override
        public boolean supportsProgress() {
            return false;
        }

        @Override
        public boolean enabled() {
            return solarEnergy.enabled();
        }

        @Override
        public boolean setEnabled(boolean enabled) {
            solarEnergy.enabled(enabled);
            setChanged();
            return enabled;
        }

        @Override
        public boolean running() {
            return solarEnergy.active();
        }

        @Override
        public boolean active() {
            return solarEnergy.emitting();
        }

        @Override
        public long progress() {
            return 0;
        }

        @Override
        public long progressMax() {
            return 0;
        }
    }

    private final class SteamConverterControl implements MachineControl {
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
            return !converterStopped && (bufferedInput > 0 || pendingTurbineEnergy > 0);
        }

        @Override
        public boolean active() {
            return running();
        }

        @Override
        public long progress() {
            return bufferedInput;
        }

        @Override
        public long progressMax() {
            return capacity();
        }
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

    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (hasOriginalConverter()) {
                return (int) doEnergyInjection(GregTechTags.Energy.RF, null, 1, maxReceive, !simulate);
            }

            long remainingCapacity = Math.max(0, capacity() - bufferedInput);
            int acceptedEnergy = (int) Math.min(maxReceive, remainingCapacity);
            if (!simulate && acceptedEnergy > 0) {
                bufferedInput += acceptedEnergy;
                if (level != null) conversionMeter.input(level.getGameTime(), acceptedEnergy);
                setChanged();
            }
            return acceptedEnergy;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, bufferedInput);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, capacity());
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return !hasOriginalConverter() || !converterStopped;
        }
    };

    private IEnergyStorage sidedEnergyStorage(Direction side) {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                if (!isEnergyAcceptingFrom(GregTechTags.Energy.RF, side, false)) return 0;
                return energyStorage.receiveEnergy(maxReceive, simulate);
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                return energyStorage.getEnergyStored();
            }

            @Override
            public int getMaxEnergyStored() {
                return energyStorage.getMaxEnergyStored();
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return isEnergyAcceptingFrom(GregTechTags.Energy.RF, side, false);
            }
        };
    }

    private boolean exposesForgeEnergy(@Nullable Direction side) {
        if (spec == null || spec.inType() != GregTechTags.Energy.RF) return false;
        if (side == null) return true;
        if (side == facing()) return false;
        boolean hasRearOutput = isMagnet() || isOriginalCooler();
        return !hasRearOutput || side != facing().getOpposite();
    }

    private boolean exposesSteamInlet(@Nullable Direction side) {
        return steamTank != null && (side == null || side != facing());
    }

    private IFluidHandler coveredSteamInlet(Direction side) {
        return ComponentCoverAccess.fluids(this, side, turbineInlet(side));
    }

//? if >=1.21.1 {
    /*public IEnergyStorage energyCapability(@Nullable Direction side) {
        if (!exposesForgeEnergy(side)) return null;
        if ((isMagnet() || hasOriginalConverter()) && side != null) return sidedEnergyStorage(side);
        return energyStorage;
    }

    public IItemHandler itemCapability(@Nullable Direction side) {
        return isBatteryBox() ? batterySlots.capability(side) : null;
    }

    public IFluidHandler fluidCapability(@Nullable Direction side) {
        if (!exposesSteamInlet(side)) return null;
        return side == null ? turbineInlet() : coveredSteamInlet(side);
    }
*///?} else {
    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ENERGY && exposesForgeEnergy(side)) {
            if ((isMagnet() || hasOriginalConverter()) && side != null) {
                return sidedEnergyCapabilities
                        .computeIfAbsent(side, face -> LazyOptional.of(() -> sidedEnergyStorage(face)))
                        .cast();
            }
            if (unsidedEnergyCapability == null || !unsidedEnergyCapability.isPresent()) {
                unsidedEnergyCapability = LazyOptional.of(() -> energyStorage);
            }
            return unsidedEnergyCapability.cast();
        }

        if (capability == ForgeCapabilities.ITEM_HANDLER && isBatteryBox()) {
            return batterySlots.capability(side).cast();
        }

        if (capability == ForgeCapabilities.FLUID_HANDLER && exposesSteamInlet(side)) {
            if (unsidedSteamCapability == null || !unsidedSteamCapability.isPresent()) {
                unsidedSteamCapability = LazyOptional.of(this::turbineInlet);
            }
            if (side == null) return unsidedSteamCapability.cast();
            return sidedSteamCapabilities
                    .computeIfAbsent(side, face -> LazyOptional.of(() -> coveredSteamInlet(face)))
                    .cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        batterySlots.invalidateCaps();
        sidedSteamCapabilities.values().forEach(LazyOptional::invalidate);
        sidedSteamCapabilities.clear();
        if (unsidedSteamCapability != null) {
            unsidedSteamCapability.invalidate();
            unsidedSteamCapability = null;
        }
        if (unsidedEnergyCapability != null) {
            unsidedEnergyCapability.invalidate();
            unsidedEnergyCapability = null;
        }
        sidedEnergyCapabilities.values().forEach(LazyOptional::invalidate);
        sidedEnergyCapabilities.clear();
    }
//?}

    // Saved state

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        tag.putLong("gt.buffer", isSolar() ? solarEnergy.energy() : bufferedInput);
        if (isSolar()) {
            tag.putBoolean("gt.solar_stopped", !solarEnergy.enabled());
            tag.putBoolean("gt.solar_active", solarEnergy.active());
            tag.putBoolean("gt.solar_emitting", solarEnergy.emitting());
        }
        if (isBatteryBox()) batteryBox.energy.save(tag);
        batterySlots.save(tag, lookup);
        covers.forEach((side, stack) ->
                tag.put("gt.battery_cover_" + side.ordinal(), NbtCompat.saveItem(stack, lookup)));
        if (isElectricTransformer()) transformerControl.save(tag);

        tag.putBoolean("gt.inverted", inverted);
        if (isMagnet()) {
            // Older magnet saves use these keys for the converter's stopped state and mode.
            tag.putBoolean("gt.magnet_stopped", converterStopped);
            tag.putByte("gt.magnet_mode", (byte) converterState.mode());
            tag.putInt("gt.magnet_overloads", overloadCount);
        }
        if (isRotationTransformer()) tag.putBoolean("gt.rotation_negative_input", negativeRotationInput);
        tag.putLong("gt.turbine_pending", pendingTurbineEnergy);
        tag.putLong("gt.steam_remainder", steamRemainder);
        tag.putBoolean("gt.converter_stopped", converterStopped);
        if (hasOriginalConverter()) {
            var snapshot = converterState.snapshot();
            tag.putByte("gt.mode", (byte) snapshot.mode());
            if (isOriginalMotor()) tag.putBoolean("gt.reversed", snapshot.counterClockwise());
            tag.putBoolean("gt.active.energy", snapshot.emitted());
            tag.putBoolean("gt.can.energy", snapshot.possible());
            if (isOriginalMotor()) tag.putBoolean("gt.visual", snapshot.fast());
        }

        if (!legacyRotor.isEmpty()) tag.put("gt.rotor", NbtCompat.saveItem(legacyRotor, lookup));
        if (steamTank != null) {
            CompoundTag tankTag = new CompoundTag();
            NbtCompat.writeTank(steamTank, tankTag, lookup);
            tag.put("gt.steam", tankTag);
        }
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        bufferedInput = tag.getLong("gt.buffer");
        if (isSolar()) {
            solarEnergy.restore(
                    bufferedInput,
                    tag.getBoolean("gt.solar_active"),
                    tag.getBoolean("gt.solar_emitting"),
                    tag.getBoolean("gt.solar_stopped"));
            bufferedInput = 0;
        }
        if (isBatteryBox()) batteryBox.energy.load(tag);
        batterySlots.load(tag, lookup);
        if (isElectricTransformer()) transformerControl.load(tag);

        covers.clear();
        for (Direction side : Direction.values()) {
            String coverKey = "gt.battery_cover_" + side.ordinal();
            if (tag.contains(coverKey)) {
                covers.put(side, NbtCompat.readItem(tag.getCompound(coverKey), lookup));
            }
        }
        panels().loaded();

        inverted = tag.getBoolean("gt.inverted");
        if (isMagnet()) overloadCount = Math.max(0, tag.getInt("gt.magnet_overloads"));
        if (isRotationTransformer()) negativeRotationInput = tag.getBoolean("gt.rotation_negative_input");
        pendingTurbineEnergy = Math.max(0, tag.getLong("gt.turbine_pending"));
        steamRemainder = Math.floorMod(tag.getLong("gt.steam_remainder"), SteamTurbineConversion.STEAM_PER_WATER);

        // Older magnet saves wrote a separate stopped flag and have no shared mode key.
        boolean usesLegacyMagnetKeys = isMagnet() && !tag.contains("gt.mode");
        converterStopped = tag.getBoolean(usesLegacyMagnetKeys ? "gt.magnet_stopped" : "gt.converter_stopped");
        if (hasOriginalConverter()) {
            converterState.restore(new OriginalRotaryConverter.Snapshot(
                    tag.getByte(usesLegacyMagnetKeys ? "gt.magnet_mode" : "gt.mode"),
                    isOriginalMotor() && tag.getBoolean("gt.reversed"),
                    false,
                    tag.getBoolean("gt.can.energy"),
                    tag.getBoolean("gt.active.energy"),
                    isOriginalMotor() && tag.getBoolean("gt.visual")));
        }

        legacyRotor = tag.contains("gt.rotor")
                ? NbtCompat.readItem(tag.getCompound("gt.rotor"), lookup)
                : ItemStack.EMPTY;
        if (steamTank != null && tag.contains("gt.steam")) {
            NbtCompat.readTank(steamTank, tag.getCompound("gt.steam"), lookup);
            // Source restores the rated tank capacity after loading the fluid.
            steamTank.setCapacity(spec.inputRate() * 4);
        }
    }

    @Override
    public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (ItemStack cover : covers.values()) {
            BlockContents.drop(this, cover);
        }
        covers.clear();
        batterySlots.dropContents();
        BlockContents.drop(this, legacyRotor);
        legacyRotor = ItemStack.EMPTY;
        setChanged();
    }
}
