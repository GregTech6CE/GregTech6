package gregtech.content.blockentity.sensor;

import gregtech.api.addon.platform.AddonSensor;
import gregtech.api.addon.platform.SensorContext;
import gregtech.internal.energy.IEnergyBlock;
import gregtech.content.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {

/*import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
//?}

/**
 * Current sensor panel: measures the selected input every ten game ticks and
 * emits the selected mode's redstone result. Original non-TPS sensors sampled
 * every server post tick; measurement coverage and scaling still differ.
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
        return configuredInput != null && gregtech.internal.sensor.SensorPanelRules.validInputSide(front.ordinal(), configuredInput.ordinal())
                ? configuredInput : front.getOpposite();
    }
    public boolean setInputSide(Direction side) {
        if (!gregtech.internal.sensor.SensorPanelRules.validInputSide(
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
    @javax.annotation.Nullable private final AddonSensor addon;
    private long value;
    private int signal; // 0..15

    /** The last averaged sample and its maximum, so a click re-derives the display at once. */
    private long measuredValue;
    private long measuredMaximum;
    private boolean hasMeasurement;
    private gregtech.internal.sensor.SensorControl.Mode mode = gregtech.internal.sensor.SensorControl.Mode.DISPLAY;
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
        return gregtech.internal.sensor.SensorPanelRules.tpsFromElapsed(elapsedMillis,tickRate);
    }

    private final gregtech.internal.sensor.SensorAverage average = new gregtech.internal.sensor.SensorAverage();
    public gregtech.internal.sensor.SixCellDisplay.Glyph[] displayCells() {
        String unit = addon != null ? addon.unit() : gregtech.internal.sensor.SensorPanelRules.unit(kind.name());
        int color = addon != null ? addon.color() : gregtech.internal.sensor.SensorPanelRules.color(kind.name());
        return gregtech.internal.sensor.SixCellDisplay.sensor(value,mode,hexadecimal,unit,color);
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
        if (reset) { mode = gregtech.internal.sensor.SensorControl.Mode.DISPLAY; threshold = 0; hexadecimal = false; average.resize(1); }
        else if (screwdriver) {
            if (x >= 2 && x <= 14 && y >= 2 && y <= 4) hexadecimal = !hexadecimal;
            else {
                int change = gregtech.internal.sensor.SensorControl.thresholdChange(x,y,hexadecimal);
                if (change != 0) average.resize(average.size()+change);
                else mode = gregtech.internal.sensor.SensorControl.Mode.values()[(mode.ordinal()+1)%8];
            }
        } else {
            if (!gregtech.internal.sensor.SensorControl.hasThreshold(mode)) return false;
            int change = gregtech.internal.sensor.SensorControl.thresholdChange(x,y,hexadecimal);
            if (change == 0) return false;
            threshold = Math.max(0, Math.min(hexadecimal ? 65535 : 9999, threshold + change));
        }
        if (hasMeasurement) {
            var reading = gregtech.internal.sensor.SensorControl.evaluate(
                    mode, measuredValue, measuredMaximum, threshold);
            value = reading.displayed();
            signal = reading.signal();
        } else {
            signal = 0;
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        return true;
    }

    public SensorBlockEntity(BlockPos pos, BlockState state) {
        super(gregtech.content.registry.GTBlockEntities.SENSOR.get(), pos, state);
        if (state.getBlock() instanceof gregtech.content.block.sensor.SensorBlock sensor) {
            this.kind = sensor.kind() == null ? Kind.FLUID : sensor.kind();
            this.addon = sensor.addon();
        } else {
            this.kind = Kind.FLUID;
            this.addon = null;
        }
    }

    public Kind kind() { return kind; }

    /** The addon kind id, or the built-in enum name. */
    public String kindName() { return addon != null ? addon.id() : kind.name(); }
    public long value() { return value; }
    public int signal() { return signal; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SensorBlockEntity be) {
        if (level.getGameTime() % 10 != 0) return;
        measure(level, pos, state, be);
    }

    /**
     * The measurement itself, without the port's ten-tick throttle. Direct calls
     * from GameTests exercise measurement but do not validate the scheduled sampling
     * rate, post-tick ordering or capture of short-lived values.
     */
    public static void measure(Level level, BlockPos pos, BlockState state, SensorBlockEntity be) {
        Direction facing = be.inputSide().getOpposite();
        BlockEntity target = level.getBlockEntity(pos.relative(be.inputSide()));
        // MultiTileEntitySensorTE unwraps a bound structural part before every measurement.
        if (target instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity part)
            target = part.sensorTarget();

        // GT6 MultiTileEntityTPSmeter:42-49 - the TPS meter measures the server, not the block in
        // front of it, so it is sampled on its own 20-tick beat and needs no target at all.
        if (be.kind == Kind.TPS && level.getGameTime() % TPS_TICK_RATE == 0) be.sampleTps();

        long newValue = 0;
        long maximum = 0;
        if (be.addon != null) {
            SensorContext context = new SensorContext(level, pos, target, facing);
            newValue = be.addon.read(context);
            maximum = be.addon.maximum(context);
        } else if (be.kind == Kind.TPS) {
            newValue = be.tpsValue;
            maximum = TPS_MAX;
        } else if (target != null) {
            switch (be.kind) {
                case FLUID -> {
//? if >=1.21.1 {
                    /*IFluidHandler fluids = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,target.getBlockPos(),facing);
*///?} else {
                    IFluidHandler fluids = target.getCapability(ForgeCapabilities.FLUID_HANDLER, facing)
                            .resolve().orElse(null);
//?}
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
//? if >=1.21.1 {
                    /*IItemHandler items = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,target.getBlockPos(),facing);
*///?} else {
                    IItemHandler items = target.getCapability(ForgeCapabilities.ITEM_HANDLER, facing)
                            .resolve().orElse(null);
//?}
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
                    if (target instanceof gregtech.content.blockentity.energy.ElectricWireBlockEntity wire) {
                        newValue = wire.lastWattage();
                        maximum = gregtech.internal.sensor.SensorPanelRules.transferMaximum(wire.voltage(), wire.amperage());
                    }
                }
                case PROGRESS -> {
                    var progress = gregtech.internal.sensor.ProgressSources.find(target, facing);
                    if (progress != null) {
                        newValue = progress.value();
                        maximum = progress.maximum();
                    } else if (target instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner) {
                        newValue = ((gregtech.mixin.BaseSpawnerAccessor)spawner.getSpawner()).gregtech$spawnDelay();
                        maximum = Integer.MAX_VALUE; // Original sensor binds Long.MAX_VALUE to a signed int.
                    }
                    newValue = Math.max(Integer.MIN_VALUE,Math.min(Integer.MAX_VALUE,newValue));
                    maximum = Math.max(Integer.MIN_VALUE,Math.min(Integer.MAX_VALUE,maximum));
                }
                case THERMOMETER -> {
                    if (target instanceof gregtech.api.sensor.TemperatureSource source) {
                        newValue = source.temperatureK(facing.ordinal());
                        maximum = source.maximumK(facing.ordinal());
                    }
//? if >=1.21.1 {
                    /*else if (target instanceof gregtech.platform.neoforge.smeltery.SmeltingCrucibleEntity crucible) {
*///?} else {
                    else if (target instanceof gregtech.content.blockentity.machine.SmeltingCrucibleBlockEntity crucible) {
//?}
                        newValue = crucible.getTemperature();
                        maximum = crucible.getMeltDownLimitK();
                    }
                }
                case TACHOMETER -> {
                    if (target instanceof gregtech.content.blockentity.energy.AxleBlockEntity axle) {
                        newValue = axle.transferredLast();
                        maximum = gregtech.internal.sensor.SensorPanelRules.transferMaximum(axle.maxSpeed(), axle.maxPower());
                    }
                    else if (target instanceof gregtech.content.blockentity.energy.GearboxBlockEntity gearbox
                            && target.getBlockState().getBlock() instanceof gregtech.content.block.energy.GearboxBlock block) {
                        newValue = gearbox.transferredLast();
                        // GT6's custom-gearbox meter used rated speed * 16 instead.
                        maximum = gregtech.internal.sensor.SensorPanelRules.transferMaximum(
                                block.spec().maxSpeed(), block.spec().maxPower());
                    }
                }
                case WEIGHTOMETRIC, WEIGHTOMETRIC_LIGHT, WEIGHTOMETRIC_MEDIUM,
                     WEIGHTOMETRIC_SUPER_HEAVY -> {
                    double kilograms = 0;
                    if (target instanceof gregtech.api.sensor.WeightSensorSource source) {
                        // The host owns the physical contents; a crucible's cache is not its contents.
                        kilograms = source.weightKilograms(facing.ordinal());
                    } else {
//? if >=1.21.1 {
                        /*IItemHandler items = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,target.getBlockPos(),facing);
*///?} else {
                        IItemHandler items = target.getCapability(ForgeCapabilities.ITEM_HANDLER, facing)
                                .resolve().orElse(null);
//?}
                        if (items != null) {
                            for (int i = 0; i < items.getSlots(); i++)
                                kilograms += gregtech.internal.sensor.PhysicalItemMass.kilograms(items.getStackInSlot(i));
                        }
                    }
                    newValue = switch (be.kind) {
                        case WEIGHTOMETRIC_LIGHT -> gregtech.internal.sensor.SensorMeasurements.grams(kilograms);
                        case WEIGHTOMETRIC_MEDIUM -> gregtech.internal.sensor.SensorMeasurements.kilograms(kilograms);
                        case WEIGHTOMETRIC_SUPER_HEAVY -> gregtech.internal.sensor.SensorMeasurements.kilotonnes(kilograms);
                        default -> gregtech.internal.sensor.SensorMeasurements.tonnes(kilograms);
                    };
                }
                case TPS -> {
                    // Handled before the target lookup: the TPS meter measures the whole server.
                }
                case BUCKETOMETER, KILOBUCKETOMETER -> {
//? if >=1.21.1 {
                    /*IFluidHandler fluids = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,target.getBlockPos(),facing);
*///?} else {
                    IFluidHandler fluids = target.getCapability(ForgeCapabilities.FLUID_HANDLER, facing)
                            .resolve().orElse(null);
//?}
                    if (fluids != null) {
                        long amount = 0, capacity = 0;
                        for (int i = 0; i < fluids.getTanks(); i++) {
                            amount += fluids.getFluidInTank(i).getAmount();
                            capacity += fluids.getTankCapacity(i);
                        }
                        newValue = switch (be.kind) {
                            case BUCKETOMETER -> gregtech.internal.sensor.SensorMeasurements.cubicMetres(amount);
                            case KILOBUCKETOMETER -> gregtech.internal.sensor.SensorMeasurements.cubicDecametres(amount);
                            default -> amount;
                        };
                        maximum = switch (be.kind) {
                            case BUCKETOMETER -> gregtech.internal.sensor.SensorMeasurements.cubicMetres(capacity);
                            case KILOBUCKETOMETER -> gregtech.internal.sensor.SensorMeasurements.cubicDecametres(capacity);
                            default -> capacity;
                        };
                    }
                }
                case GIBBLOMETER, KILOGIBBLOMETER -> {
                    if (target instanceof gregtech.api.sensor.CompressionSensorSource compressed) {
                        long amount = compressed.gibblValue(facing.ordinal()), capacity = compressed.gibblMaximum(facing.ordinal());
                        newValue = be.kind == Kind.GIBBLOMETER
                                ? gregtech.internal.sensor.SensorMeasurements.gibbl(amount)
                                : gregtech.internal.sensor.SensorMeasurements.kiloGibbl(amount);
                        maximum = be.kind == Kind.GIBBLOMETER
                                ? gregtech.internal.sensor.SensorMeasurements.gibbl(capacity)
                                : gregtech.internal.sensor.SensorMeasurements.kiloGibbl(capacity);
                    }
                }
                case STACKOMETER -> {
                    // GT6 counts occupied inventory slots, regardless of item stack size.
//? if >=1.21.1 {
                    /*IItemHandler items = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,target.getBlockPos(),facing);
*///?} else {
                    IItemHandler items = target.getCapability(ForgeCapabilities.ITEM_HANDLER, facing)
                            .resolve().orElse(null);
//?}
                    if (items != null) {
                        int stacks = 0;
                        for (int i = 0; i < items.getSlots(); i++)
                            if (!items.getStackInSlot(i).isEmpty()) stacks++;
                        newValue = stacks;
                        maximum = items.getSlots();
                    }
                }
                case LUMINOMETER, PLAYERCOUNTER, CHRONOMETER -> { /* sampled without a block entity below */ }
                case GEIGER -> {
                    if (target instanceof gregtech.content.blockentity.energy.ReactorCoreBlockEntity reactor) {
                        newValue = reactor.neutronTotal();
                        // The original sums installed rods' neutron maxima, including coolant adjustments.
                        maximum = Integer.MAX_VALUE;
                    }
                }
                case LASEROMETER -> {
                    // GT6 MultiTileEntityLaserometer reads only an adjacent laser fiber's
                    // mTransferredLast, not a converter's stored LU or the current tick's traffic.
                    if (target instanceof gregtech.content.blockentity.energy.LaserFiberBlockEntity fiber) {
                        newValue = fiber.transferredLast();
                        maximum = 65535;
                    }
                }
            }
        }

        if (be.addon == null) {
            // Bulk storage exposes legal stacks to automation, but the meter must read its full count.
            if (be.kind == Kind.ITEM && target instanceof gregtech.content.blockentity.inventory.MassStorageBlockEntity storage) {
                newValue = storage.stored(); maximum = storage.baseCapacity();
            }
            switch (be.kind) {
                case WEIGHTOMETRIC, WEIGHTOMETRIC_LIGHT, WEIGHTOMETRIC_MEDIUM,
                     WEIGHTOMETRIC_SUPER_HEAVY -> maximum = gregtech.internal.sensor.SensorMeasurements.MAX_COUNT;
                case LUMINOMETER -> {
                    newValue = level.getMaxLocalRawBrightness(pos.relative(facing.getOpposite())); maximum = 15;
                }
                case PLAYERCOUNTER -> {
                    if (level.getServer() != null) {
                        newValue = level.getServer().getPlayerCount(); maximum = level.getServer().getMaxPlayers();
                    }
                }
                case CHRONOMETER -> { newValue = gregtech.internal.sensor.SensorMeasurements.timeOfDayMinutes(level.getDayTime()); maximum = 1440; }
                default -> {}
            }
        }
        // sample clamps values to int; most maximum values remain long here, unlike
        // the original base sensor which binds both operands before evaluating modes.
        newValue = be.average.sample(newValue);
        be.measuredValue = newValue;
        be.measuredMaximum = maximum;
        be.hasMeasurement = true;
        be.setChanged();
        var reading = gregtech.internal.sensor.SensorControl.evaluate(be.mode, newValue, maximum, be.threshold);
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
//? if >=1.21.1 {
    /*protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
*///?} else {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
//?}
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
        if (hasMeasurement) {
            tag.putLong("gt.measured_value", measuredValue);
            tag.putLong("gt.measured_max", measuredMaximum);
        }
    }

    @Override
//? if >=1.21.1 {
    /*protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
*///?} else {
    public void load(CompoundTag tag) {
        super.load(tag);
//?}
        int input = tag.contains("gt.sensor_input") ? tag.getByte("gt.sensor_input") : -1;
        configuredInput = gregtech.internal.sensor.SensorPanelRules.validInputSide(
                getBlockState().getValue(DirectionalBlock.FACING).ordinal(), input) ? Direction.values()[input] : null;
        value = tag.getLong("gt.value");
        signal = Math.max(0, Math.min(15, tag.getInt("gt.signal")));
        mode = gregtech.internal.sensor.SensorControl.Mode.values()[Math.max(0,Math.min(7,tag.getInt("gt.control_mode")))];
        threshold = Math.max(0,Math.min(65535,tag.getInt("gt.threshold")));
        hexadecimal = tag.getBoolean("gt.hex");
        hasMeasurement = tag.contains("gt.measured_value");
        if (hasMeasurement) {
            measuredValue = tag.getLong("gt.measured_value");
            measuredMaximum = tag.getLong("gt.measured_max");
        }
        average.restore(tag.getIntArray("gt.average"),tag.getInt("gt.average_index"));
    }

    @Override
//? if >=1.21.1 {
    /*public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
*///?} else {
    public CompoundTag getUpdateTag() {
//?}
        CompoundTag tag = new CompoundTag();
        saveDisplay(tag);
        return tag;
    }

    @Override
//? if >=1.21.1 {
    /*public void handleUpdateTag(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        loadAdditional(tag,lookup);
*///?} else {
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
//?}
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
//? if >=1.21.1 {
    /*public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt,net.minecraft.core.HolderLookup.Provider lookup) {
        if (pkt.getTag() != null) loadAdditional(pkt.getTag(),lookup);
*///?} else {
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) load(pkt.getTag());
//?}
    }
}
