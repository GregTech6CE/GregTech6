package gregtech.content.blockentity.energy;

import gregtech.content.cover.PanelCoverRuntime;
import gregtech.content.energy.BatteryBoxEnergy;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyTransfer;
import gregtech.engine.energy.IEnergyBlock;
import gregtech.engine.fluid.FluidTankGT;
import gregtech.engine.stack.NbtCompat;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import gregtech.content.blockentity.GTEnergyBlockEntity;
import gregtech.content.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {



/*import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;
//?}

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;

/**
 * GT6 energy-net node: motors/dynamos/transformers (energy conversion), steam
 * turbines (steam fluid → RU), solar panels (sun → EU) and battery boxes /
 * storage cabinets. Each device defines its own input and output faces.
 */
public class EnergyNodeBlockEntity extends GTEnergyBlockEntity implements gregtech.engine.machine.AdjacentEnergyControl.Provider, gregtech.engine.inventory.BlockContents, gregtech.api.platform.machine.MachineControl.Provider, gregtech.content.cover.PanelCoverHost {

    private final EnumMap<Direction, ItemStack> covers = new EnumMap<>(Direction.class);
    private PanelCoverRuntime controlPanels;
    private gregtech.content.energy.ElectricTransformerControl transformerControl;
    private gregtech.content.energy.OriginalRotaryConverter.State rotaryConverter;
    public boolean hasOriginalConverter(){return rotaryConverter != null;}
    public boolean isOriginalRotaryConverter(){return hasOriginalConverter() && gregtech.content.energy.OriginalRotaryConverter.handles(spec);}
    public boolean isOriginalThermalConverter(){return hasOriginalConverter() && gregtech.content.energy.OriginalThermalConverter.handles(spec);}
    public boolean isOriginalMotor(){return hasOriginalConverter() && gregtech.content.energy.OriginalRotaryConverter.motorBehavior(spec);}
    public boolean reverseMotor(){buffer=0;boolean reversed=rotaryConverter.reverse();setChanged();return reversed;}
    public boolean motorCounterClockwise(){return isOriginalMotor() && rotaryConverter.counterClockwise();}
    public boolean hasControlPanels(){return hasOriginalConverter()||isBatteryBox()||isElectricTransformer()||isSolar();}
    @Override public net.minecraft.world.item.ItemStack getCover(Direction side){return covers.getOrDefault(side,net.minecraft.world.item.ItemStack.EMPTY);}
    @Override public PanelCoverRuntime panels() {
        if (controlPanels == null) controlPanels = new PanelCoverRuntime(this);
        return controlPanels;
    }
    @Override public boolean coverSupportsPossible(){return hasControlPanels();}
    @Override public boolean coverPossible(Direction side) {
        if (hasOriginalConverter() || isSolar()) return true;
        if (isBatteryBox()) return batteryBox.energy.buffer() > spec.outputRate();
        return isElectricTransformer() && transformerControl.running();
    }
    /** Mirrors the tool actions in {@code EnergyNodeBlock.use}; unhandled tools fall through to the cover. */
    @Override public boolean consumesTool(ItemStack tool){
        if (gregtech.content.cover.PanelCoverHost.blockConsumesTool(this,tool))return true;
        if (isOriginalMotor()&&(GTToolHelper.isMonkeyWrench(tool)||GTToolHelper.isMagnifyingGlass(tool)))return true;
        if (isInvertible()&&(isRotationTransformer()||isElectricTransformer()
                ?GTToolHelper.isMonkeyWrench(tool):GTToolHelper.isSoftHammer(tool)))return true;
        if (isTurbine()&&GTToolHelper.matchesTool(tool,GTToolType.PLUNGER))return true;
        if (isTurbine()&&hasRotor()&&GTToolHelper.matchesTool(tool,GTToolType.CROWBAR))return true;
        return isBatteryBox()&&GTToolHelper.matchesTool(tool,GTToolType.CROWBAR);
    }
    @Override public boolean attachCover(Direction side,net.minecraft.world.item.ItemStack stack){
        var panel=gregtech.content.cover.PanelCover.of(stack);
        if (!gregtech.content.cover.CoverItems.isCover(stack)||!getCover(side).isEmpty()||!panels().canAttach(side,stack)
                ||!gregtech.content.cover.CoverAdmission.allowCover(this,side,stack))return false;
        covers.put(side,stack.copyWithCount(1));panels().attached(side);return true;
    }
    @Override public net.minecraft.world.item.ItemStack removeCover(Direction side){
        var removed=covers.remove(side);
        if (removed==null)return net.minecraft.world.item.ItemStack.EMPTY;
        var panel=gregtech.content.cover.PanelCover.of(removed);
        if (panel!=null&&panel.selector())machineControl(side).setMode(0);
        return panels().removed(side,removed);
    }

    private EnergyNodeSpec spec;
    private final gregtech.engine.energy.EnergyConversionMeter conversionMeter = new gregtech.engine.energy.EnergyConversionMeter();
    public gregtech.engine.energy.EnergyConversionMeter conversionMeter() { return conversionMeter; }
    public long lastMeasuredInput() { return level == null ? 0 : conversionMeter.input(level.getGameTime()); }
    public long lastMeasuredOutput() { return level == null ? 0 : conversionMeter.output(level.getGameTime()); }
    public long lastMeasuredAuxOutput() { return level == null ? 0 : conversionMeter.auxiliaryOutput(level.getGameTime()); }
    private gregtech.engine.energy.SolarPanelEnergy solarEnergy;
    /** Internal buffer, counted in input units (steam L for turbines). */
    private long buffer;
    /** Transformers: soft-hammer toggled step-up mode (rates swapped). */
    private boolean inverted;
    /** RU rotation direction for the dedicated rotational transformer path. */
    private boolean rotationNegativeInput;
    /** GT6 bipolar magnets: stopped state and 0..15 output current limit. */
    private int magnetOverloads;
    private final BatteryBoxInventory batterySlots = new BatteryBoxInventory(this);
    @Nullable
    private BatteryBoxNode batteryBox;
    /** Retained only to return rotor items installed in older versions of the port. */
    private net.minecraft.world.item.ItemStack rotor = net.minecraft.world.item.ItemStack.EMPTY;
    private long turbinePending, steamRemainder;
    private boolean converterStopped;
    @Nullable
//? if >=1.21.1 {
//?} else {
    private final java.util.Map<Direction,LazyOptional<IFluidHandler>> componentSteamCaps=new java.util.EnumMap<>(Direction.class);
//?}
    private FluidTankGT steamTank;
//? if >=1.21.1 {
//?} else {
    @Nullable
    private LazyOptional<IFluidHandler> steamCap;
//?}

    public EnergyNodeBlockEntity(BlockPos pos, BlockState state) {
        this(gregtech.content.registry.GTEnergyNodes.ENERGY_NODE.get(), pos, state);
    }

    public EnergyNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        if (state.getBlock() instanceof gregtech.content.block.energy.EnergyNodeBlock node) {
            setSpec(node.spec());
        }
    }

    @Override public void onLoad(){
        super.onLoad();
        if (isSolar() && level != null && !level.isClientSide && !isRemoved() && facing() == Direction.UP)
            level.setBlock(worldPosition, getBlockState().setValue(DirectionalBlock.FACING, Direction.DOWN), net.minecraft.world.level.block.Block.UPDATE_ALL);
    }

    public void setSpec(EnergyNodeSpec spec) {
        this.spec = spec;
        rotaryConverter = gregtech.content.energy.OriginalRotaryConverter.handles(spec) || gregtech.content.energy.OriginalThermalConverter.handles(spec) || gregtech.content.energy.MagnetMachineDefinitions.handles(spec) || gregtech.content.energy.OriginalSteamTurbines.handles(spec)
                ? new gregtech.content.energy.OriginalRotaryConverter.State(spec) : null;
        if (isSolar()) {
            solarEnergy = new gregtech.engine.energy.SolarPanelEnergy(spec.outputRate(), worldPosition.hashCode());
        }
        if (isBatteryBox()) {
            batteryBox = new BatteryBoxNode(this, spec, batterySlots);
        }
        if (isElectricTransformer()) {
            transformerControl = new gregtech.content.energy.ElectricTransformerControl(this::setChanged);
        }
        if (spec.kind() == EnergyNodeSpec.Kind.TURBINE) {
            steamTank = new FluidTankGT(spec.inputRate() * 4).setOnChanged(this::setChanged);
        }
    }

    public EnergyNodeSpec spec() { return spec; }
    public long turbineSteamAmount() { return steamTank == null ? 0 : steamTank.getAmount(); }
    public long turbineSteamCapacity() { return steamTank == null ? 0 : steamTank.capacity(); }
    public boolean converterStopped() { return converterStopped; }
    public boolean converterEmitted() { return rotaryConverter != null && rotaryConverter.emitted(); }
    public boolean converterFast() { return rotaryConverter != null && rotaryConverter.fast(); }
    public boolean solarSkyVisible() { return solarEnergy != null && solarEnergy.skyVisible(); }
    public boolean transformerInverted() { return inverted; }
    public long stored() { return isSolar() ? solarEnergy.energy() : isBatteryBox() ? batteryBox.energy.buffer() : buffer; }
    @Nullable
    public BatteryBoxEnergy batteryEnergy() { return batteryBox == null ? null : batteryBox.energy; }

    // ── Transformer step-up/step-down ───────────────────────────────────────

    public boolean isInvertible() {
        return spec != null && spec.kind() == EnergyNodeSpec.Kind.CONVERTER
                && spec.inType() == spec.outType();
    }

    public boolean isElectricTransformer(){return spec!=null&&spec.id().startsWith("transformer_")&&spec.inType()==GregTechTags.Energy.EU;}

    public boolean isRotationTransformer() {
        return spec != null && spec.id().startsWith("rotation_transformer_");
    }

    @Override public boolean isRainProof(Direction side) { return isSolar(); }
    @Override public boolean isThunderProof(Direction side) { return isSolar(); }

    public boolean isSolar() { return spec != null && spec.kind() == EnergyNodeSpec.Kind.SOLAR; }
    public void checkSolarSky() { if (isSolar()) solarEnergy.checkSky(); }

    public boolean isMagnet() {
        return spec != null && spec.kind() == EnergyNodeSpec.Kind.MAGNET;
    }

    public boolean magnetEnabled() { return isMagnet() && !converterStopped; }

    public boolean toggleMagnetEnabled() {
        if (!isMagnet()) return false;
        return machineControl(null).setEnabled(!magnetEnabled());
    }

    public int magnetMode() { return isMagnet() ? rotaryConverter.mode() : 0; }

    /** @return the new mode (true = step-up). */
    public boolean toggleInverted() {
        // GT6 clears the rotational transformer's stored energy when its direction changes.
        // Other converters keep their existing mode-switch behavior.
        if (isRotationTransformer() || isElectricTransformer()) {
            buffer = 0;
            rotationNegativeInput = false;
        }
        inverted = !inverted;
        if (isElectricTransformer())transformerControl.select(inverted);
        setChanged();
        return inverted;
    }

    private long inRate() { return inverted ? spec.outputRate() : spec.inputRate(); }
    long outRate() { return inverted ? spec.inputRate() : spec.outputRate(); }
    private Direction rotationInputFace() { return inverted ? facing().getOpposite() : facing(); }
    private Direction rotationOutputFace() { return inverted ? facing() : facing().getOpposite(); }

    /**
     * GT6's bidirectional RU converter uses the original input rating as the
     * reverse converter's input recommendation, even though it accepts smaller
     * packets. The two modes share the same physical capacitor.
     */
    private long rotationInputRecommended() { return spec.inputRate(); }

    private long rotationInputMaximum() {
        return inverted ? Math.max(spec.inputRate(), spec.outputRate() * 8) : spec.inputRate() * 2;
    }

    private long rotationOutputSize() {
        // TE_Behavior_Energy_Converter.doConversion: floor(stored * outRec / inRec).
        // In reverse, both recommendations are the original input rate.
        return inverted ? buffer : buffer * spec.outputRate() / spec.inputRate();
    }

    // ── Recovery of obsolete port rotor inventories ────

    public boolean isTurbine() {
        return spec != null && spec.kind() == EnergyNodeSpec.Kind.TURBINE;
    }

    public static boolean isRotorItem(net.minecraft.world.item.ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof gregtech.engine.material.MaterialFormItem mat
                && mat.getPrefix() == gregtech.content.data.MaterialPrefix.rotor;
    }

    public boolean hasRotor() { return !rotor.isEmpty(); }

    /** The rotor is part of the turbine crafting recipe in GT6, not a runtime slot. */
    public boolean installRotor(net.minecraft.world.item.ItemStack stack) { return false; }

    public net.minecraft.world.item.ItemStack removeRotor() {
        net.minecraft.world.item.ItemStack out = rotor;
        rotor = net.minecraft.world.item.ItemStack.EMPTY;
        setChanged();
        return out;
    }

    public boolean isBatteryBox() {
        return spec != null && spec.batterySlots() > 0;
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

    private long capacity() {
        if (isBatteryBox()) return batterySlots.installedCapacity();
        return hasOriginalConverter() || isSteamConverter() ? spec.inputRate() * 2 : spec.capacity();
    }

    Direction facing() {
        BlockState st = getBlockState();
        return st.hasProperty(DirectionalBlock.FACING) ? st.getValue(DirectionalBlock.FACING) : Direction.NORTH;
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    private long lastSyncedBuffer = -1;

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyNodeBlockEntity be) {
        if (be.spec == null) return;
        if (be.isBatteryBox()) {
            be.batteryBox.tick(level, pos, state);
            return;
        }
        if (!be.hasOriginalConverter() && !be.isSolar() && !be.isElectricTransformer() && !be.covers.isEmpty()) {
            be.panels().beforeTick();
            for (var side:Direction.values())gregtech.content.cover.ComponentCoverRuntime.tick(be,side,level.getGameTime());
            be.panels().afterTick();
        }
        if (be.isSolar()) {
            be.tickSolar(level, pos);
            return;
        }
        if (be.hasOriginalConverter()) {
            be.tickOriginalConverter();
            return;
        }
        be.emitOutput();
        // charge bar: sync the buffer to clients at most every 20 ticks
        if (level.getGameTime() % 20 == 0 && be.lastSyncedBuffer != be.buffer) {
            be.lastSyncedBuffer = be.buffer;
            level.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    // ── Client sync (charge bar display) ────────────────────────────────────

    /** The charge bar only needs the saved state; nodes never take part in formed-structure sync. */
    @Override
    protected void writeUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        saveEntityAdditional(tag, lookup);
    }

    @Override
    protected void readUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        loadFromTag(tag, lookup);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void handleDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt,
            @Nullable HolderLookup.Provider lookup) {
        if (pkt.getTag() != null) loadFromTag(pkt.getTag(), lookup);
    }

    public long purgeTurbineSteam() {
        if (!isTurbine() || steamTank == null) return 0;
        long removed = steamTank.getAmount(); steamTank.remove(removed); setChanged(); return removed;
    }

    public boolean isSteamConverter() { return isTurbine(); }

    public int converterOverloads() { return magnetOverloads; }

    private void tickTurbine() {
        if (steamTank == null || spec.inputRate() <= 0) return;
        // Older port builds admitted arbitrary fluids. Never turn that saved water into RU.
        if (!steamTank.getFluid().isEmpty() && !turbineInlet().isFluidValid(0, steamTank.getFluid())) {
            steamTank.remove(steamTank.getAmount());
            setChanged();
        }
        var step = gregtech.engine.energy.SteamTurbineConversion.step(buffer, turbinePending,
                steamTank.getAmount(), steamRemainder, spec.inputRate());
        buffer = step.energy(); turbinePending = step.pending(); steamRemainder = step.remainder();
        if (step.consumed() > 0) steamTank.remove(step.consumed());
        emitCondensate(step.condensate());
        setChanged();
    }

    private void emitCondensate(long amount) {
        var water = gregtech.content.registry.GTFluids.still("DistW");
        if (level == null || amount <= 0 || water == null) return;
        var fluid = new FluidStack(water.get(), (int) Math.min(Integer.MAX_VALUE, amount));
        for (Direction side : Direction.values()) {
            if (side.getAxis() == facing().getAxis() || !level.hasChunkAt(worldPosition.relative(side))) continue;
            var neighbor = level.getBlockEntity(worldPosition.relative(side));
            if (neighbor == null) continue;
//? if >=1.21.1 {
            /*var target = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, worldPosition.relative(side), side.getOpposite());
*///?} else {
            var target = neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite()).resolve().orElse(null);
//?}
            if (target != null) fluid.shrink(Math.max(0, Math.min(fluid.getAmount(), target.fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE))));
            if (fluid.isEmpty()) break;
        }
        // Source discards condensate that no adjacent tank recovers.
    }

    private IFluidHandler turbineInlet() { return turbineInlet(null); }
    private IFluidHandler turbineInlet(@Nullable Direction side) { return new IFluidHandler() {
        public int getTanks() { return side == facing() ? 0 : 1; }
        public FluidStack getFluidInTank(int tank) { return getTanks() == 0 ? FluidStack.EMPTY : steamTank.getFluid().copy(); }
        public int getTankCapacity(int tank) { return getTanks() == 0 ? 0 : (int) steamTank.capacity(); }
        public boolean isFluidValid(int tank, FluidStack fluid) {
            var steam = gregtech.content.registry.GTFluids.still("Steam");
            return !fluid.isEmpty() && steam != null && fluid.getFluid().isSame(steam.get());
        }
        public int fill(FluidStack fluid, FluidAction action) {
            int filled = !isRemoved() && !converterStopped && (side == null || side == facing().getOpposite()) && isFluidValid(0, fluid) ? steamTank.fill(fluid, action) : 0;
            if (action.execute() && level != null) conversionMeter.input(level.getGameTime(), filled);
            return filled;
        }
        public FluidStack drain(FluidStack fluid, FluidAction action) { return FluidStack.EMPTY; }
        public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }
    }; }

    private void tickSolar(Level level, BlockPos pos) {
        panels().beforeTick();
        var conditions = new gregtech.engine.energy.SolarPanelEnergy.Conditions(
                level.isDay(), level.isRaining(), level.isThundering(),
                level.dimension().location().toString().equals("twilightforest:twilight_forest"),
                level.getBiome(pos).value().getModifiedClimateSettings().downfall());
        solarEnergy.tick(() -> level.dimensionType().hasSkyLight() && level.canSeeSky(pos.above()),
                conditions, energy -> EnergyTransfer.emitEnergyToNetwork(spec.outType(), energy, 1, this));
        var state = getBlockState();
        var active = gregtech.content.block.energy.SolarPanelBlock.ACTIVE;
        if (state.hasProperty(active) && state.getValue(active) != solarEnergy.active())
            level.setBlock(pos, state.setValue(active, solarEnergy.active()), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        panels().afterTick();
        setChanged();
    }

    private void tickOriginalConverter() {
        panels().beforeTick();
        for (var side : Direction.values())
            gregtech.content.cover.ComponentCoverRuntime.tick(this, side, level.getGameTime());
        if (isTurbine()) tickTurbine();
        long before = buffer;
        var step = rotaryConverter.tick(buffer, converterStopped,
                (size, amount) -> {
                    if (isMagnet()) {
                        long positive = EnergyTransfer.emitEnergyToSide(spec.outType(), facing(), size, amount, this);
                        long negative = EnergyTransfer.emitEnergyToSide(spec.outType(), facing().getOpposite(), -size, amount, this);
                        conversionMeter.output(level.getGameTime(), size, positive + negative);
                        return positive + negative;
                    }
                    if (isOriginalThermalConverter() && gregtech.content.energy.OriginalThermalConverter.cooler(spec)) {
                        long cold = EnergyTransfer.emitEnergyToSide(spec.outType(), facing(), size, amount, this);
                        long heat = EnergyTransfer.emitEnergyToSide(GregTechTags.Energy.HU, facing().getOpposite(), size, amount, this);
                        conversionMeter.output(level.getGameTime(), size, cold);
                        conversionMeter.auxiliaryOutput(level.getGameTime(), size, heat);
                        return cold > 0 || heat > 0 ? 1 : 0;
                    }
                    long sent = EnergyTransfer.emitEnergyToNetwork(spec.outType(), size, amount, this);
                    conversionMeter.output(level.getGameTime(), size, sent);
                    return sent;
                });
        buffer = step.energy();
        if (step.overloaded()) rotaryOverload(before, spec.outType());
        if (isRemoved() || level.getBlockEntity(worldPosition) != this) return;
        if (level.getGameTime() % 600 == 5 && !step.possible() && magnetOverloads > 0) magnetOverloads--;
        var state = getBlockState();
        // Both source trinary states 1 and 2 share the same magnet overlay; keep the existing boolean state identity.
        var next = isMagnet() ? state.setValue(gregtech.content.block.energy.MagnetMachineBlock.ACTIVE, step.visual() > 0)
                : state.setValue(gregtech.content.block.energy.RotaryConverterBlock.ACTIVITY, step.visual());
        if (isOriginalMotor())
            next = next.setValue(gregtech.content.block.energy.OriginalMotorBlock.COUNTER_CLOCKWISE, rotaryConverter.counterClockwise())
                    .setValue(gregtech.content.block.energy.OriginalMotorBlock.FAST, step.fast());
        if (next != state) level.setBlock(worldPosition, next, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        else if (level.getGameTime() % 20 == 0 && lastSyncedBuffer != buffer) {
            lastSyncedBuffer = buffer;
            level.sendBlockUpdated(worldPosition, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
        panels().afterTick();
        setChanged();
    }

    private void rotaryOverload(long size, GregTechTags.Tag type) {
        if (magnetOverloads < 100) { buffer = 0; magnetOverloads++; setChanged(); return; }
        gregtech.engine.energy.MachineOverload.apply(this, size, type);
        setChanged();
    }

    private void emitOutput() {
        if (level == null || converterStopped) return;
        if (isElectricTransformer()){emitElectricTransformer();return;}
        if (isRotationTransformer()) {
            emitRotationTransformer();
            return;
        }
        if (buffer < costPerPacket()) return;
        if (spec.outType() == GregTechTags.Energy.RF) {
            pushForgeEnergy();
            return;
        }
        long packets = buffer / costPerPacket();
        if (isRotationTransformer()) packets = Math.min(packets, inverted ? 1 : 4);
        long emitted = EnergyTransfer.emitEnergyToSide(
                spec.outType(), isRotationTransformer() ? rotationOutputFace() : facing(),
                isRotationTransformer() && rotationNegativeInput ? -outRate() : outRate(), packets, this);
        if (isRotationTransformer()) emitted = Math.min(packets, Math.max(0, emitted));
        if (emitted > 0) {
            conversionMeter.output(level.getGameTime(), outRate(), emitted);
            buffer -= emitted * costPerPacket();
            setChanged();
        }
    }

    private void magnetOverload(long packetSize) {
        rotaryOverload(packetSize, spec.inType());
        buffer = 0;
    }

    private void emitElectricTransformer(){
        panels().beforeTick();
        long packets=inverted?1:spec.inputRate()/spec.outputRate();
        long size=inverted?buffer:buffer/packets;
        long maximum=getEnergySizeOutputMax(spec.outType(),null);
        if (transformerControl.mode()>0)size=Math.min(size,transformerControl.limit(maximum));
        boolean possible=size>=getEnergySizeOutputMin(spec.outType(),null),emitted=false;
        if (possible){
            if (size>maximum)magnetOverload(size);
            else {
                long used=EnergyTransfer.emitEnergyToNetwork(spec.outType(),size,packets,this);
                conversionMeter.output(level.getGameTime(), size, Math.min(packets, used));
                if (used>0){buffer-=Math.min(packets,used)*size;emitted=true;}
            }
        }
        transformerControl.tick(possible,emitted);
        if (level.getGameTime()%600==5&&!possible&&magnetOverloads>0)magnetOverloads--;
        var property=gregtech.content.block.energy.ElectricTransformerBlock.ACTIVITY;
        if (getBlockState().hasProperty(property)&&getBlockState().getValue(property)!=transformerControl.visual())
            level.setBlock(worldPosition,getBlockState().setValue(property,transformerControl.visual()),3);
        panels().afterTick();
    }

    private void emitRotationTransformer() {
        long size = rotationOutputSize();
        long minimum = getEnergySizeOutputMin(spec.outType(), rotationOutputFace());
        long maximum = getEnergySizeOutputMax(spec.outType(), rotationOutputFace());
        if (size >= minimum && size <= maximum) {
            long sent = EnergyTransfer.emitEnergyToSide(spec.outType(), rotationOutputFace(),
                    rotationNegativeInput ? -size : size, inverted ? 1 : 4, this);
            conversionMeter.output(level.getGameTime(), size, sent);
        }
        // Both GT6 conversion modes have NBT_WASTE_ENERGY=T. Their capacitors
        // lose up to the active input maximum every tick, including when no
        // receiver exists or the energy is below the output packet threshold.
        if (buffer > 0) {
            buffer = Math.max(0, buffer - rotationInputMaximum());
            setChanged();
        }
    }

    /** RF output: push whole packets into the Forge Energy storage in front. */
    private void pushForgeEnergy() {
        var be = level.getBlockEntity(worldPosition.relative(facing()));
        if (be == null) return;
//? if >=1.21.1 {
        /*var storage = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,be.getBlockPos(),facing().getOpposite());
*///?} else {
        var storage = be.getCapability(ForgeCapabilities.ENERGY, facing().getOpposite())
                .resolve().orElse(null);
//?}
        if (storage == null || !storage.canReceive()) return;
        long cost = costPerPacket();
        int out = (int) outRate();
        for (int i = 0; i < 64 && buffer >= cost; i++) {
            if (storage.receiveEnergy(out, true) < out) break;
            storage.receiveEnergy(out, false);
            conversionMeter.output(level.getGameTime(), out, 1);
            buffer -= cost;
            setChanged();
        }
    }

    /** Input units consumed per emitted packet (solar generates, so 1:1 on output units). */
    private long costPerPacket() {
        return switch (spec.kind()) {
            case SOLAR, STORAGE -> outRate();
            default -> Math.max(1, inRate());
        };
    }

    // ── IEnergyBlock ─────────────────────────────────────────────────────────

    private boolean acceptsEnergyInput() {
        return spec != null && spec.kind() != EnergyNodeSpec.Kind.SOLAR;
    }

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        if (spec == null) return false;
        return emitting ? energyType == spec.outType() || isOriginalThermalConverter() && gregtech.content.energy.OriginalThermalConverter.cooler(spec) && energyType == GregTechTags.Energy.HU
                : (acceptsEnergyInput() && energyType == spec.inType());
    }

    @Override
    public java.util.Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        if (spec == null) return List.of();
        if (isOriginalThermalConverter() && gregtech.content.energy.OriginalThermalConverter.cooler(spec)) return List.of(spec.inType(), spec.outType(), GregTechTags.Energy.HU);
        return spec.inType() == spec.outType() ? List.of(spec.outType()) : List.of(spec.inType(), spec.outType());
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        if (!acceptsEnergyInput() || energyType != spec.inType()) return false;
        if (hasOriginalConverter()) return (theoretical || !converterStopped)
                && (side == null || (isMagnet() ? side != facing() && side != facing().getOpposite()
                    : isOriginalThermalConverter() ? side != facing() && (!gregtech.content.energy.OriginalThermalConverter.cooler(spec) || side != facing().getOpposite())
                    : isOriginalMotor() && !isTurbine() ? side != facing() : side == facing().getOpposite()))
                && super.isEnergyAcceptingFrom(energyType, side, theoretical);
        if (isElectricTransformer())return (theoretical||transformerControl.accepts())&&(side==null||(inverted?side!=facing():side==facing()));
        if (isRotationTransformer()) return side == null || side == rotationInputFace();
        return isSteamConverter() ? !converterStopped && (side == null || side == facing().getOpposite()) : side == null || side != facing();
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        if (spec == null) return false;
        if (isOriginalThermalConverter() && gregtech.content.energy.OriginalThermalConverter.cooler(spec))
            return side == null ? energyType == spec.outType() || energyType == GregTechTags.Energy.HU
                    : energyType == spec.outType() ? side == facing() : energyType == GregTechTags.Energy.HU && side == facing().getOpposite();
        if (energyType != spec.outType()) return false;
        if (isSolar()) return (side == null || side == facing()) && (theoretical || side == null || !panels().shuttered(side));
        if (isBatteryBox()) return batteryBox.emitsTo(side, theoretical);
        if (isMagnet()) return side == null || side == facing() || side == facing().getOpposite();
        if (isElectricTransformer())return side==null||(inverted?side==facing():side!=facing());
        if (isRotationTransformer()) return side == null || side == rotationOutputFace();
        return side == null || side == facing();
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (hasOriginalConverter()) return energyType == spec.inType() ? spec.inputRate() : 0;
        return acceptsEnergyInput() && energyType == spec.inType()
                ? ((isRotationTransformer() || isElectricTransformer()) ? rotationInputRecommended() : inRate()) : 0;
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (hasOriginalConverter()) return energyType == spec.outType() ? spec.outputRate() : 0;
        return spec != null && energyType == spec.outType() ? outRate() : 0;
    }

    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (hasOriginalConverter())return energyType==spec.inType()?gregtech.content.energy.OriginalRotaryConverter.inputMinimum(spec):0;
        if (isBatteryBox())return energyType==spec.inType()?gregtech.content.energy.OriginalEnergyDeviceTooltipData.batteryInputMinimum(inRate()):0;
        if (!(isRotationTransformer() || isElectricTransformer()) || energyType != spec.inType()) return super.getEnergySizeInputMin(energyType, side);
        if (!inverted) return spec.inputRate() <= 16 ? 1 : spec.inputRate() / 2;
        long originalOutputMin = Math.max(1, spec.outputRate() / 2);
        return originalOutputMin <= 8 ? 1 : originalOutputMin;
    }

    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (hasOriginalConverter())return energyType==spec.inType()?spec.inputRate()*2:0;
        return (isRotationTransformer() || isElectricTransformer()) && energyType == spec.inType()
                ? rotationInputMaximum() : super.getEnergySizeInputMax(energyType, side);
    }

    @Override
    public long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (hasOriginalConverter())return energyType==spec.outType()?spec.outputRate()/2:0;
        if (isSolar()) return energyType == spec.outType() ? spec.outputRate() / 8 : 0;
        if (isBatteryBox()) return energyType==spec.outType()?outRate():0;
        if (!(isRotationTransformer() || isElectricTransformer()) || energyType != spec.outType()) return super.getEnergySizeOutputMin(energyType, side);
        return inverted ? spec.inputRate() * 3 / 4 : Math.max(1, spec.outputRate() / 2);
    }

    @Override public long getEnergySizeOutputMax(GregTechTags.Tag type, @Nullable Direction side) {
        if (hasOriginalConverter()) return type == spec.outType() ? spec.outputRate() * 2 : 0;
        if (isSolar()) return type == spec.outType() ? spec.outputRate() : 0;
        return isBatteryBox() ? (type==spec.outType()?outRate():0) : super.getEnergySizeOutputMax(type,side);
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        if (hasOriginalConverter()) return 0; // Source Root: push-only, discovery is via simulated injection.
        if (isBatteryBox()) return batteryBox.demanded(energyType, side, size);
        long magnitude = gregtech.engine.energy.EnergyPackets.magnitude(size, energyType == GregTechTags.Energy.RU);
        if (!acceptsEnergyInput() || energyType != spec.inType() || magnitude == 0) return 0;
        if ((isMagnet() || isElectricTransformer()) && !isEnergyAcceptingFrom(energyType, side, false)) return 0;
        if ((isRotationTransformer() || isElectricTransformer()) && !isEnergyAcceptingFrom(energyType, side, false)) return 0;
        return Math.max(0, capacity() - buffer) / magnitude;
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        if (isSolar()) return isEnergyEmittingTo(energyType, side, false) ? solarEnergy.energy() : 0;
        // GT6 energy converters emit during their own tick. The inherited
        // TileEntityBase01Root.doExtract() returns zero for pull requests.
        if (hasOriginalConverter() || isSteamConverter() || isRotationTransformer() || isElectricTransformer() || isMagnet() || isBatteryBox()) return 0;
        long magnitude = size;
        if (spec == null || energyType != spec.outType() || magnitude <= 0) return 0;
        long packets = buffer / costPerPacket();
        return Math.min(packets, (packets * outRate()) / magnitude);
    }

    @Override
    public long doInject(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doInject) {
        if (hasOriginalConverter()) {
            if (!isEnergyAcceptingFrom(energyType, side, false)) return 0;
            var step = rotaryConverter.inject(buffer, size, amount, doInject);
            if (doInject) {
                if (level != null) conversionMeter.input(level.getGameTime(), size, step.consumed());
                buffer = step.energy();
                if (step.overloaded()) rotaryOverload(Math.abs(size), energyType);
                if (step.consumed() > 0) setChanged();
            }
            return step.consumed();
        }
        if (isBatteryBox()) return batteryBox.inject(energyType, side, size, amount, doInject);
        if (isSteamConverter()) {
            if (amount <= 0 || size == 0 || size == Long.MIN_VALUE || !isEnergyAcceptingFrom(energyType, side, false)) return 0;
            long magnitude = Math.abs(size);
            if (magnitude > spec.inputRate() * 2) { if (doInject) magnetOverload(magnitude); return amount; }
            long space = Math.max(0, capacity() - buffer);
            long accepted = Math.min(amount, space / magnitude + (space % magnitude == 0 ? 0 : 1));
            if (doInject && accepted > 0) {
                if (level != null) conversionMeter.input(level.getGameTime(), magnitude * accepted);
                buffer += magnitude * accepted; setChanged();
            }
            return accepted;
        }
        long magnitude = gregtech.engine.energy.EnergyPackets.magnitude(size, energyType == GregTechTags.Energy.RU);
        if (!acceptsEnergyInput() || energyType != spec.inType() || amount <= 0 || magnitude == 0) return 0;
        if ((isMagnet() || isElectricTransformer()) && !isEnergyAcceptingFrom(energyType, side, false)) return 0;
        if ((isMagnet() || isElectricTransformer()) && magnitude > getEnergySizeInputMax(energyType, side)) {
            if (doInject) {
                if (level != null) conversionMeter.input(level.getGameTime(), magnitude, amount);
                magnetOverload(magnitude);
            }
            return amount;
        }
        if ((isRotationTransformer() || isElectricTransformer()) && !isEnergyAcceptingFrom(energyType, side, false)) return 0;
        long space = Math.max(0, capacity() - buffer);
        long accepted = gregtech.engine.energy.EnergyPackets.fitting(amount, space, magnitude);
        if (doInject && accepted > 0) {
            if (isRotationTransformer()) rotationNegativeInput = size < 0;
            buffer += accepted * magnitude;
            if (level != null) conversionMeter.input(level.getGameTime(), accepted * magnitude);
            setChanged();
        }
        return accepted;
    }

    @Override
    public long doExtract(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doExtract) {
        if (isSolar()) {
            if (!isEnergyEmittingTo(energyType, side, false)) return 0;
            long extracted = solarEnergy.extract(size, amount, doExtract);
            if (doExtract && extracted > 0) setChanged();
            return extracted;
        }
        if (hasOriginalConverter() || isSteamConverter() || isRotationTransformer() || isElectricTransformer() || isMagnet() || isBatteryBox()) return 0;
        long magnitude = size;
        if (spec == null || energyType != spec.outType() || amount <= 0 || magnitude <= 0) return 0;
        long cost = costPerPacket();
        long availablePackets = buffer / cost;
        long requestedPackets = (amount * magnitude) / Math.max(1, outRate());
        long extracted = Math.min(availablePackets, Math.max(0, requestedPackets));
        long extractedAmount = (extracted * outRate()) / magnitude;
        if (doExtract && extractedAmount > 0) {
            if (level != null) conversionMeter.output(level.getGameTime(), magnitude, extractedAmount);
            buffer -= extracted * cost;
            setChanged();
        }
        return extractedAmount;
    }

    @Override public java.util.Collection<GregTechTags.Tag> getEnergyCapacitorTypes(@Nullable Direction side) {
        return isSolar()?List.of():isBatteryBox()?List.of(spec.inType()):super.getEnergyCapacitorTypes(side);
    }

    @Override
    public long getEnergyStored(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (isSolar()) return 0; // Source solar panels do not implement an energy capacitor.
        if (isBatteryBox()) return batteryBox.stored(energyType);
        return spec != null && (energyType == spec.inType() || energyType == spec.outType()) ? buffer : 0;
    }

    @Override
    public long getEnergyCapacity(GregTechTags.Tag energyType, @Nullable Direction side) {
        if (isSolar()) return 0; // Source solar panels do not implement an energy capacitor.
        if (isBatteryBox()) return batteryBox.capacity(energyType);
        return spec != null ? capacity() : 0;
    }

    @Override public gregtech.engine.machine.AdjacentEnergyControl adjacentEnergyControl() {
        if (!hasOriginalConverter() || !gregtech.content.energy.OriginalAdjacentEnergyRules.respondsToAdjacent(spec)) return null;
        return enabled -> {
            // Source waste-energy converters use the same stopped state for manual and adjacent requests.
            // Stored remainder still converts/drains normally; changing the request must not erase it.
            if (!isRemoved() && (level == null || !level.isClientSide) && converterStopped == enabled) {
                converterStopped = !enabled; setChanged();
            }
            return !converterStopped;
        };
    }
    @Override public gregtech.api.platform.machine.MachineControl machineControl(Direction side) {
        if (hasOriginalConverter()) return new gregtech.api.platform.machine.MachineControl() {
            @Override public boolean available(){return !isRemoved();}
            @Override public boolean supportsProgress(){return false;}
            @Override public boolean supportsMode(){return isOriginalMotor() || isMagnet() || gregtech.content.energy.OriginalThermalConverter.modeSelectable(spec);}
            @Override public int mode(){return rotaryConverter.mode();}
            @Override public int setMode(int value){if (isOriginalMotor() || isMagnet() || gregtech.content.energy.OriginalThermalConverter.modeSelectable(spec))rotaryConverter.mode(value);setChanged();return mode();}
            @Override public boolean enabled(){return !converterStopped;}
            @Override public boolean setEnabled(boolean value){converterStopped=!value;setChanged();return value;}
            @Override public boolean running(){return rotaryConverter.possible();}
            @Override public boolean active(){return rotaryConverter.emitted();}
            @Override public long progress(){return 0;}
            @Override public long progressMax(){return 0;}
        };
        if (isSolar()) return new gregtech.api.platform.machine.MachineControl() {
            @Override public boolean supportsProgress() { return false; }
            @Override public boolean enabled() { return solarEnergy.enabled(); }
            @Override public boolean setEnabled(boolean value) { solarEnergy.enabled(value); setChanged(); return value; }
            @Override public boolean running() { return solarEnergy.active(); }
            @Override public boolean active() { return solarEnergy.emitting(); }
            @Override public long progress() { return 0; }
            @Override public long progressMax() { return 0; }
        };
        if (isElectricTransformer())return transformerControl;
        if (isSteamConverter()) return new gregtech.api.platform.machine.MachineControl() {
            @Override public boolean enabled() { return !converterStopped; }
            @Override public boolean setEnabled(boolean value) { converterStopped = !value; setChanged(); return value; }
            @Override public boolean running() { return !converterStopped && (buffer > 0 || turbinePending > 0); }
            @Override public boolean active() { return running(); }
            @Override public long progress() { return buffer; }
            @Override public long progressMax() { return capacity(); }
        };
        if (isBatteryBox()) return batteryBox;
        return new gregtech.api.platform.machine.MachineControl(){
            public boolean available(){return !isRemoved();}
            public boolean supportsMode(){return false;}
            public boolean enabled(){return !converterStopped;}
            public boolean setEnabled(boolean value){converterStopped=!value;setChanged();return value;}
            public boolean running(){return enabled()&&stored()>0;}
            public boolean active(){return running();}
            public long progress(){return stored();}
            public long progressMax(){return capacity();}
        };
    }

    // ── Forge Energy capability (flux motors accept RF on input faces) ──────

//? if >=1.21.1 {
    /*private net.neoforged.neoforge.energy.IEnergyStorage magnetFeStorage(Direction side) {
        return new net.neoforged.neoforge.energy.IEnergyStorage() {
*///?} else {
    @Nullable
    private LazyOptional<net.minecraftforge.energy.IEnergyStorage> feCap;
    private final java.util.EnumMap<Direction, LazyOptional<net.minecraftforge.energy.IEnergyStorage>> magnetFeCaps =
            new java.util.EnumMap<>(Direction.class);

    private net.minecraftforge.energy.IEnergyStorage magnetFeStorage(Direction side) {
        return new net.minecraftforge.energy.IEnergyStorage() {
//?}
            @Override public int receiveEnergy(int amount, boolean simulate) {
                return isEnergyAcceptingFrom(GregTechTags.Energy.RF, side, false)
                        ? feStorage.receiveEnergy(amount, simulate) : 0;
            }
            @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
            @Override public int getEnergyStored() { return feStorage.getEnergyStored(); }
            @Override public int getMaxEnergyStored() { return feStorage.getMaxEnergyStored(); }
            @Override public boolean canExtract() { return false; }
            @Override public boolean canReceive() {
                return isEnergyAcceptingFrom(GregTechTags.Energy.RF, side, false);
            }
        };
    }

//? if >=1.21.1 {
    /*private final net.neoforged.neoforge.energy.IEnergyStorage feStorage =
            new net.neoforged.neoforge.energy.IEnergyStorage() {
*///?} else {
    private final net.minecraftforge.energy.IEnergyStorage feStorage =
            new net.minecraftforge.energy.IEnergyStorage() {
//?}
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (hasOriginalConverter()) return (int) doEnergyInjection(GregTechTags.Energy.RF, null, 1, maxReceive, !simulate);
            long space = Math.max(0, capacity() - buffer);
            int accepted = (int) Math.min(maxReceive, space);
            if (!simulate && accepted > 0) {
                buffer += accepted;
                if (level != null) conversionMeter.input(level.getGameTime(), accepted);
                setChanged();
            }
            return accepted;
        }

        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, buffer); }
        @Override public int getMaxEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, capacity()); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return !hasOriginalConverter() || !converterStopped; }
    };

//? if >=1.21.1 {
    /*public net.neoforged.neoforge.energy.IEnergyStorage energyCapability(@Nullable Direction side){
        if (spec==null||spec.inType()!=GregTechTags.Energy.RF||side!=null&&(side==facing()||(isMagnet() || isOriginalThermalConverter() && gregtech.content.energy.OriginalThermalConverter.cooler(spec))&&side==facing().getOpposite()))return null;
        return (isMagnet()||hasOriginalConverter())&&side!=null?magnetFeStorage(side):feStorage;
*///?} else {
    // ── Fluid capability (turbines) ─────────────────────────────────────────

    @Override
    public <T> @org.jetbrains.annotations.NotNull LazyOptional<T> getCapability(
            @org.jetbrains.annotations.NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && spec != null && spec.inType() == GregTechTags.Energy.RF
                && (side == null || (side != facing() && ((!isMagnet() && !(isOriginalThermalConverter() && gregtech.content.energy.OriginalThermalConverter.cooler(spec))) || side != facing().getOpposite())))) {
            if ((isMagnet() || hasOriginalConverter()) && side != null) {
                return magnetFeCaps.computeIfAbsent(side,
                        key -> LazyOptional.of(() -> magnetFeStorage(key))).cast();
            }
            if (feCap == null || !feCap.isPresent()) {
                feCap = LazyOptional.of(() -> feStorage);
            }
            return feCap.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER && isBatteryBox()) {
            return batterySlots.capability(side).cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER && steamTank != null
                && (side == null || side != facing())) {
            if (steamCap == null || !steamCap.isPresent()) {
                steamCap = LazyOptional.of(this::turbineInlet);
            }
            return side==null?steamCap.cast():componentSteamCaps.computeIfAbsent(side,face->LazyOptional.of(()->gregtech.content.cover.ComponentCoverAccess.fluids(this,face,turbineInlet(face)))).cast();
        }
        return super.getCapability(cap, side);
//?}
    }
//? if >=1.21.1 {
    /*public IItemHandler itemCapability(@Nullable Direction side) {
        return isBatteryBox() ? batterySlots.capability(side) : null;
    }
    public IFluidHandler fluidCapability(@Nullable Direction side){return steamTank!=null&&(side==null||side!=facing())?side==null?turbineInlet():gregtech.content.cover.ComponentCoverAccess.fluids(this,side,turbineInlet(side)):null;}
*///?} else {

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        batterySlots.invalidateCaps();
        componentSteamCaps.values().forEach(LazyOptional::invalidate);componentSteamCaps.clear();
        if (steamCap != null) { steamCap.invalidate(); steamCap = null; }
        if (feCap != null) { feCap.invalidate(); feCap = null; }
        magnetFeCaps.values().forEach(LazyOptional::invalidate);
        magnetFeCaps.clear();
    }
//?}

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        tag.putLong("gt.buffer", isSolar() ? solarEnergy.energy() : buffer);
        if (isSolar()) {
            tag.putBoolean("gt.solar_stopped", !solarEnergy.enabled());
            tag.putBoolean("gt.solar_active", solarEnergy.active());
            tag.putBoolean("gt.solar_emitting", solarEnergy.emitting());
        }
        if (isBatteryBox()) batteryBox.energy.save(tag);
        batterySlots.save(tag, lookup);
        covers.forEach((side,stack)->tag.put("gt.battery_cover_"+side.ordinal(),NbtCompat.saveItem(stack,lookup)));
        if (isElectricTransformer())transformerControl.save(tag);
        tag.putBoolean("gt.inverted", inverted);
        if (isMagnet()) {
            // Preserve the port's old magnet keys as aliases of the original common state.
            tag.putBoolean("gt.magnet_stopped", converterStopped);
            tag.putByte("gt.magnet_mode", (byte) rotaryConverter.mode());
            tag.putInt("gt.magnet_overloads", magnetOverloads);
        }
        if (isRotationTransformer()) tag.putBoolean("gt.rotation_negative_input", rotationNegativeInput);
        tag.putLong("gt.turbine_pending", turbinePending);
        tag.putLong("gt.steam_remainder", steamRemainder);
        tag.putBoolean("gt.converter_stopped", converterStopped);
        if (hasOriginalConverter()) {
            var rotary = rotaryConverter.snapshot();
            tag.putByte("gt.mode", (byte) rotary.mode());
            if (isOriginalMotor()) tag.putBoolean("gt.reversed", rotary.counterClockwise());
            tag.putBoolean("gt.active.energy", rotary.emitted());
            tag.putBoolean("gt.can.energy", rotary.possible());
            if (isOriginalMotor()) tag.putBoolean("gt.visual", rotary.fast());
        }
        if (!rotor.isEmpty()) tag.put("gt.rotor", NbtCompat.saveItem(rotor, lookup));
        if (steamTank != null) {
            CompoundTag t = new CompoundTag();
            NbtCompat.writeTank(steamTank, t, lookup);
            tag.put("gt.steam", t);
        }
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        buffer = tag.getLong("gt.buffer");
        if (isSolar()) {
            solarEnergy.restore(buffer, tag.getBoolean("gt.solar_active"), tag.getBoolean("gt.solar_emitting"), tag.getBoolean("gt.solar_stopped"));
            buffer = 0;
        }
        if (isBatteryBox()) batteryBox.energy.load(tag);
        batterySlots.load(tag, lookup);
        if (isElectricTransformer())transformerControl.load(tag);
        {
            covers.clear();
            for (var side:Direction.values()){
                String key="gt.battery_cover_"+side.ordinal();
                if (tag.contains(key))covers.put(side,NbtCompat.readItem(tag.getCompound(key),lookup));
            }
            panels().loaded();
        }
        inverted = tag.getBoolean("gt.inverted");
        if (isMagnet()) {
            magnetOverloads = Math.max(0, tag.getInt("gt.magnet_overloads"));
        }
        if (isRotationTransformer()) rotationNegativeInput = tag.getBoolean("gt.rotation_negative_input");
        turbinePending = Math.max(0, tag.getLong("gt.turbine_pending"));
        steamRemainder = Math.floorMod(tag.getLong("gt.steam_remainder"), gregtech.engine.energy.SteamTurbineConversion.STEAM_PER_WATER);
        // Old magnet saves wrote an unrelated false converter flag. Canonical gt.mode distinguishes the new format.
        boolean legacyMagnet = isMagnet() && !tag.contains("gt.mode");
        converterStopped = tag.getBoolean(legacyMagnet ? "gt.magnet_stopped" : "gt.converter_stopped");
        if (hasOriginalConverter())
            rotaryConverter.restore(new gregtech.content.energy.OriginalRotaryConverter.Snapshot(
                    tag.getByte(legacyMagnet ? "gt.magnet_mode" : "gt.mode"), isOriginalMotor() && tag.getBoolean("gt.reversed"), false,
                    tag.getBoolean("gt.can.energy"), tag.getBoolean("gt.active.energy"), isOriginalMotor() && tag.getBoolean("gt.visual")));
        rotor = tag.contains("gt.rotor")
                ? NbtCompat.readItem(tag.getCompound("gt.rotor"), lookup)
                : net.minecraft.world.item.ItemStack.EMPTY;
        if (steamTank != null && tag.contains("gt.steam")) {
            NbtCompat.readTank(steamTank, tag.getCompound("gt.steam"), lookup);
            steamTank.setCapacity(spec.inputRate() * 4); // Source resets rated tank capacity after loading saved contents.
        }
    }
    @Override public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (var cover:covers.values())gregtech.engine.inventory.BlockContents.drop(this,cover);
        if (!covers.isEmpty())covers.clear();
        batterySlots.dropContents();
        gregtech.engine.inventory.BlockContents.drop(this, rotor); rotor = net.minecraft.world.item.ItemStack.EMPTY;
        setChanged();
    }
}
