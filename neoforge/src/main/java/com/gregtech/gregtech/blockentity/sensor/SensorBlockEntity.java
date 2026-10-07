package com.gregtech.gregtech.blockentity.sensor;

import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * GT6-style sensor panel: measures the block behind it (opposite the display
 * face) every half second, shows the value on the front and emits an analog
 * redstone signal selected by the original eight modes and threshold buttons.
 */
public class SensorBlockEntity extends BlockEntity {

    public enum Kind {
        FLUID, ITEM, ENERGY, PROGRESS,
        THERMOMETER, TACHOMETER, WEIGHTOMETRIC, BUCKETOMETER,
        KILOBUCKETOMETER, GIBBLOMETER, STACKOMETER, LUMINOMETER,
        PLAYERCOUNTER, CHRONOMETER, GEIGER, LASEROMETER,
        // §108: GT6 has four weight-o-meters that differ only in their scale (gramm / kilogramme /
        // tons / kilotons - Loader_MultiTileEntities:1988-1991) and a TPS meter (:1992). The port's
        // WEIGHTOMETRIC is GT6's Heavy one (tonnes, maximum 65535), so these complete the set.
        WEIGHTOMETRIC_LIGHT, WEIGHTOMETRIC_MEDIUM, WEIGHTOMETRIC_SUPER_HEAVY, TPS, KILOGIBBLOMETER
    }

    /** GT6 {@code MultiTileEntityTPSmeter:62} {@code getTickRate()}: one sample every 20 ticks. */
    public static final int TPS_TICK_RATE = 20;

    /** GT6 {@code MultiTileEntityTPSmeter:46}: the value before the first measurement. */
    public static final long TPS_DEFAULT = 2000L;

    /** GT6 {@code MultiTileEntityTPSmeter:57} {@code getCurrentMax()}: 20.00 TPS on the display. */
    public static final long TPS_MAX = 2000L;

    private Direction configuredInput;
    public Direction inputSide() {
        var front = getBlockState().getValue(DirectionalBlock.FACING);
        return configuredInput != null && com.gregtech.gregtech.api.sensor.SensorPanelRules.validInputSide(front.ordinal(), configuredInput.ordinal())
                ? configuredInput : front.getOpposite();
    }
    public boolean setInputSide(Direction side) {
        if (!com.gregtech.gregtech.api.sensor.SensorPanelRules.validInputSide(
                getBlockState().getValue(DirectionalBlock.FACING).ordinal(), side.ordinal())) return false;
        configuredInput = side;
        syncInputSide();
        return true;
    }
    public void resetInputSide() { configuredInput = null; syncInputSide(); }
    private void syncInputSide() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    private final Kind kind;
    private long value;
    private int signal; // 0..15
    private com.gregtech.gregtech.api.sensor.SensorControl.Mode mode = com.gregtech.gregtech.api.sensor.SensorControl.Mode.DISPLAY;
    private int threshold;
    private boolean hexadecimal;

    /** GT6 {@code MultiTileEntityTPSmeter:39}: the last wall-clock sample and the value it produced. */
    private long tpsSampleTime;
    private long tpsValue = TPS_DEFAULT;

    /** GT6 {@code MultiTileEntityTPSmeter:42-49}: sample the wall clock every {@code getTickRate()} ticks. */
    public void sampleTps() {
        long now = System.currentTimeMillis();
        if (tpsSampleTime != 0L) tpsValue = tpsFromElapsed(now - tpsSampleTime, TPS_TICK_RATE);
        tpsSampleTime = now;
    }

    public long tpsValue() { return tpsValue; }

    /**
     * The arithmetic of {@code MultiTileEntityTPSmeter:46}:
     * {@code (mTime - tTime > 0 ? (getTickRate() * 100000) / (mTime - tTime) : 2000)} - i.e. hundredths
     * of a tick per second, so 20 TPS reads as {@code 2000} and 10 TPS as {@code 1000}. Split out so a
     * test can drive it without a clock.
     */
    public static long tpsFromElapsed(long elapsedMillis, int tickRate) {
        return com.gregtech.gregtech.api.sensor.SensorPanelRules.tpsFromElapsed(elapsedMillis,tickRate);
    }

    private final com.gregtech.gregtech.api.sensor.SensorAverage average = new com.gregtech.gregtech.api.sensor.SensorAverage();
    public com.gregtech.gregtech.api.sensor.SixCellDisplay.Glyph[] displayCells() {
        return com.gregtech.gregtech.api.sensor.SixCellDisplay.sensor(value,mode,hexadecimal,com.gregtech.gregtech.api.sensor.SensorPanelRules.unit(kind.name()),com.gregtech.gregtech.api.sensor.SensorPanelRules.color(kind.name()));
    }
    public String displayText() {
        String number = hexadecimal ? Long.toHexString(value).toUpperCase(java.util.Locale.ROOT) : Long.toString(value);
        return switch (mode) {
            case GREATER -> ">" + number; case EQUAL -> "=" + number; case SMALLER -> "<" + number;
            case SCALE -> "/" + number; case PERCENT -> number + "%";
            case FULL -> "=100%"; case NOT_FULL -> "<100%"; default -> number;
        };
    }
    public boolean control(double x, double y, boolean screwdriver, boolean reset) {
        if (reset) { mode = com.gregtech.gregtech.api.sensor.SensorControl.Mode.DISPLAY; threshold = 0; hexadecimal = false; average.resize(1); }
        else if (screwdriver) {
            if (x >= 2 && x <= 14 && y >= 2 && y <= 4) hexadecimal = !hexadecimal;
            else {
                int change = com.gregtech.gregtech.api.sensor.SensorControl.thresholdChange(x,y,hexadecimal);
                if (change != 0) average.resize(average.size()+change);
                else mode = com.gregtech.gregtech.api.sensor.SensorControl.Mode.values()[(mode.ordinal()+1)%8];
            }
        } else {
            if (!com.gregtech.gregtech.api.sensor.SensorControl.hasThreshold(mode)) return false;
            int change = com.gregtech.gregtech.api.sensor.SensorControl.thresholdChange(x,y,hexadecimal);
            if (change == 0) return false;
            threshold = Math.max(0, Math.min(hexadecimal ? 65535 : 9999, threshold + change));
        }
        signal = 0;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        return true;
    }

    public SensorBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.SENSOR.get(), pos, state);
        this.kind = state.getBlock() instanceof com.gregtech.gregtech.block.sensor.SensorBlock sensor
                ? sensor.kind() : Kind.FLUID;
    }

    public Kind kind() { return kind; }
    public long value() { return value; }
    public int signal() { return signal; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SensorBlockEntity be) {
        if (level.getGameTime() % 10 != 0) return;
        measure(level, pos, state, be);
    }

    /**
     * The measurement itself, without the ten-tick throttle. GT6's sensors only measure on their own
     * timer, but a GameTest runs inside a single game tick, so the throttle is split out here and the
     * test drives {@code measure} directly (the same split {@code CoverAttachmentBehaviors} uses).
     */
    public static void measure(Level level, BlockPos pos, BlockState state, SensorBlockEntity be) {
        Direction facing = be.inputSide().getOpposite();
        BlockEntity target = level.getBlockEntity(pos.relative(be.inputSide()));

        // GT6 MultiTileEntityTPSmeter:42-49 - the TPS meter measures the server, not the block in
        // front of it, so it is sampled on its own 20-tick beat and needs no target at all.
        if (be.kind == Kind.TPS && level.getGameTime() % TPS_TICK_RATE == 0) be.sampleTps();

        long newValue = 0;
        long maximum = 0;
        if (be.kind == Kind.TPS) {
            newValue = be.tpsValue;
            maximum = TPS_MAX;
        } else if (target != null) {
            switch (be.kind) {
                case FLUID -> {
                    IFluidHandler fluids = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,target.getBlockPos(),facing);
                    if (fluids != null) {
                        long amount = 0, capacity = 0;
                        for (int i = 0; i < fluids.getTanks(); i++) {
                            amount += fluids.getFluidInTank(i).getAmount();
                            capacity += fluids.getTankCapacity(i);
                        }
                        newValue = amount;
                        maximum = capacity;
                    }
                }
                case ITEM -> {
                    IItemHandler items = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,target.getBlockPos(),facing);
                    if (items != null) {
                        long count = 0, capacity = 0;
                        for (int i = 0; i < items.getSlots(); i++) {
                            count += items.getStackInSlot(i).getCount();
                            capacity += items.getSlotLimit(i);
                        }
                        newValue = count;
                        maximum = capacity;
                    }
                }
                case ENERGY -> {
                    if (target instanceof com.gregtech.gregtech.blockentity.energy.ElectricWireBlockEntity wire) {
                        newValue = wire.lastWattage();
                        maximum = com.gregtech.gregtech.api.sensor.SensorPanelRules.transferMaximum(wire.voltage(), wire.amperage());
                    }
                }
                case PROGRESS -> {
                    var machine=com.gregtech.gregtech.api.machine.MachineControl.find(target,facing);
                    if (machine!=null&&machine.available()&&machine.supportsProgress()) {
                        long total=machine.progressMax();
                        newValue = total<=0?0:(long)Math.min(100,100.0*machine.progress()/total);
                        maximum = 100;
                    }
                }
                case THERMOMETER -> {
                    if (target instanceof com.gregtech.gregtech.platform.neoforge.smeltery.SmeltingCrucibleEntity crucible) {
                        newValue = crucible.getTemperature();
                        maximum = crucible.getMeltDownLimitK();
                    }
                }
                case TACHOMETER -> {
                    if(target instanceof com.gregtech.gregtech.blockentity.energy.AxleBlockEntity axle){newValue=axle.transferredLast();maximum=com.gregtech.gregtech.api.sensor.SensorPanelRules.transferMaximum(axle.maxSpeed(),axle.maxPower());}
                    else if(target instanceof com.gregtech.gregtech.blockentity.energy.GearboxBlockEntity gearbox&&target.getBlockState().getBlock() instanceof com.gregtech.gregtech.block.energy.GearboxBlock block){newValue=gearbox.transferredLast();maximum=com.gregtech.gregtech.api.sensor.SensorPanelRules.transferMaximum(block.spec().maxSpeed(),block.spec().maxPower());}
                }
                case WEIGHTOMETRIC, WEIGHTOMETRIC_LIGHT, WEIGHTOMETRIC_MEDIUM,
                     WEIGHTOMETRIC_SUPER_HEAVY -> {
                    IItemHandler items = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,target.getBlockPos(),facing);
                    if (items != null) {
                        double kilograms = 0;
                        for (int i = 0; i < items.getSlots(); i++) {
                            var stack = items.getStackInSlot(i);
                            for (var material : com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput.parse(stack))
                                kilograms += material.weightKg() * stack.getCount();
                        }
                        // The four GT6 weight-o-meters are the same measurement with a different
                        // scale (SensorMeasurements): gramm / kilogramme / tons / kilotons.
                        newValue = switch (be.kind) {
                            case WEIGHTOMETRIC_LIGHT ->
                                    com.gregtech.gregtech.api.sensor.SensorMeasurements.grams(kilograms);
                            case WEIGHTOMETRIC_MEDIUM ->
                                    com.gregtech.gregtech.api.sensor.SensorMeasurements.kilograms(kilograms);
                            case WEIGHTOMETRIC_SUPER_HEAVY ->
                                    com.gregtech.gregtech.api.sensor.SensorMeasurements.kilotonnes(kilograms);
                            default -> com.gregtech.gregtech.api.sensor.SensorMeasurements.tonnes(kilograms);
                        };
                        maximum = com.gregtech.gregtech.api.sensor.SensorMeasurements.MAX_COUNT;
                    }
                }
                case TPS -> {
                    // Handled before the target lookup: the TPS meter measures the whole server.
                }
                case BUCKETOMETER, KILOBUCKETOMETER -> {
                    IFluidHandler fluids = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,target.getBlockPos(),facing);
                    if (fluids != null) {
                        long amount = 0, capacity = 0;
                        for (int i = 0; i < fluids.getTanks(); i++) {
                            amount += fluids.getFluidInTank(i).getAmount();
                            capacity += fluids.getTankCapacity(i);
                        }
                        newValue = switch (be.kind) {
                            case BUCKETOMETER -> com.gregtech.gregtech.api.sensor.SensorMeasurements.cubicMetres(amount);
                            case KILOBUCKETOMETER -> com.gregtech.gregtech.api.sensor.SensorMeasurements.cubicDecametres(amount);
                            default -> amount;
                        };
                        maximum = switch (be.kind) {
                            case BUCKETOMETER -> com.gregtech.gregtech.api.sensor.SensorMeasurements.cubicMetres(capacity);
                            case KILOBUCKETOMETER -> com.gregtech.gregtech.api.sensor.SensorMeasurements.cubicDecametres(capacity);
                            default -> capacity;
                        };
                    }
                }
                case GIBBLOMETER, KILOGIBBLOMETER -> {
                    if (target instanceof com.gregtech.gregtech.api.sensor.CompressionSensorSource compressed) {
                        long amount = compressed.gibblValue(facing.ordinal()), capacity = compressed.gibblMaximum(facing.ordinal());
                        newValue = be.kind == Kind.GIBBLOMETER
                                ? com.gregtech.gregtech.api.sensor.SensorMeasurements.gibbl(amount)
                                : com.gregtech.gregtech.api.sensor.SensorMeasurements.kiloGibbl(amount);
                        maximum = be.kind == Kind.GIBBLOMETER
                                ? com.gregtech.gregtech.api.sensor.SensorMeasurements.gibbl(capacity)
                                : com.gregtech.gregtech.api.sensor.SensorMeasurements.kiloGibbl(capacity);
                    }
                }
                case STACKOMETER -> {
                    IItemHandler items = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,target.getBlockPos(),facing);
                    if (items != null) {
                        int stacks = 0, maxStacks = 0;
                        for (int i = 0; i < items.getSlots(); i++) {
                            stacks += items.getStackInSlot(i).getCount();
                            maxStacks += items.getSlotLimit(i);
                        }
                        newValue = stacks / 64;
                        maximum = maxStacks / 64;
                    }
                }
                case LUMINOMETER, PLAYERCOUNTER, CHRONOMETER -> { /* sampled without a block entity below */ }
                case GEIGER -> {
                    if(target instanceof com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity reactor){newValue=reactor.neutronTotal();maximum=Integer.MAX_VALUE;}
                }
                case LASEROMETER -> {
                    // GT6 MultiTileEntityLaserometer reads only an adjacent laser fiber's
                    // mTransferredLast, not a converter's stored LU or the current tick's traffic.
                    if (target instanceof com.gregtech.gregtech.blockentity.energy.LaserFiberBlockEntity fiber) {
                        newValue = fiber.transferredLast();
                        maximum = 65535;
                    }
                }
            }
        }

        // Bulk storage exposes legal stacks to automation, but the meter must read its full count.
        if (be.kind == Kind.ITEM && target instanceof com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity storage) {
            newValue = storage.stored(); maximum = storage.CAPACITY;
        }
        switch (be.kind) {
            case LUMINOMETER -> {
                newValue = level.getMaxLocalRawBrightness(pos.relative(facing.getOpposite())); maximum = 15;
            }
            case PLAYERCOUNTER -> {
                if (level.getServer() != null) {
                    newValue = level.getServer().getPlayerCount(); maximum = level.getServer().getMaxPlayers();
                }
            }
            case CHRONOMETER -> { newValue = com.gregtech.gregtech.api.sensor.SensorMeasurements.timeOfDayMinutes(level.getDayTime()); maximum = 1440; }
            default -> {}
        }
        newValue = be.average.sample(newValue);
        be.setChanged();
        var reading = com.gregtech.gregtech.api.sensor.SensorControl.evaluate(be.mode, newValue, maximum, be.threshold);
        newValue = reading.displayed();
        int newSignal = reading.signal();
        if (newValue != be.value || newSignal != be.signal) {
            be.value = newValue;
            be.signal = newSignal;
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        saveDisplay(tag);
        tag.putIntArray("gt.average", average.values());
        tag.putInt("gt.average_index", average.index());
    }
    private void saveDisplay(CompoundTag tag) {
        tag.putByte("gt.sensor_input", (byte) inputSide().ordinal());
        tag.putLong("gt.value", value);
        tag.putInt("gt.signal", signal);
        tag.putInt("gt.control_mode", mode.ordinal());
        tag.putInt("gt.threshold", threshold);
        tag.putBoolean("gt.hex", hexadecimal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        int input = tag.contains("gt.sensor_input") ? tag.getByte("gt.sensor_input") : -1;
        configuredInput = com.gregtech.gregtech.api.sensor.SensorPanelRules.validInputSide(
                getBlockState().getValue(DirectionalBlock.FACING).ordinal(), input) ? Direction.values()[input] : null;
        value = tag.getLong("gt.value");
        signal = Math.max(0, Math.min(15, tag.getInt("gt.signal")));
        mode = com.gregtech.gregtech.api.sensor.SensorControl.Mode.values()[Math.max(0,Math.min(7,tag.getInt("gt.control_mode")))];
        threshold = Math.max(0,Math.min(65535,tag.getInt("gt.threshold")));
        hexadecimal = tag.getBoolean("gt.hex");
        average.restore(tag.getIntArray("gt.average"),tag.getInt("gt.average_index"));
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        saveDisplay(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        loadAdditional(tag,lookup);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt,net.minecraft.core.HolderLookup.Provider lookup) {
        if (pkt.getTag() != null) loadAdditional(pkt.getTag(),lookup);
    }
}
