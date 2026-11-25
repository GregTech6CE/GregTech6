package gregtech.content.blockentity.energy;

import gregtech.api.platform.machine.MachineControl;
import gregtech.content.data.GregTechTags.Tag;
import gregtech.engine.energy.EnergyBlockDefaults;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.machine.AdjacentEnergyControl;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The energy ports, server tick and controls of one placed device. */
abstract class EnergyDeviceBehavior {
    static final String BUFFER_KEY = "gt.buffer";

    final EnergyNodeBlockEntity host;

    EnergyDeviceBehavior(EnergyNodeBlockEntity host) {
        this.host = host;
    }

    static EnergyDeviceBehavior create(EnergyNodeBlockEntity host, EnergyNodeSpec spec) {
        return switch (spec.deviceType()) {
            case MOTOR, DYNAMO, HEATER -> new OriginalConverterNode(host, spec);
            case COOLER -> new CoolerNode(host, spec);
            case MAGNET -> new MagnetNode(host, spec);
            case STEAM_TURBINE -> new OriginalConverterNode.SteamConverter(host, spec);
            case ELECTRIC_TRANSFORMER -> new TransformerNode.Electric(host);
            case ROTATION_TRANSFORMER -> new TransformerNode.Rotation(host);
            case BATTERY_BOX -> new BatteryBoxNode(host, spec);
            case SOLAR -> new SolarPanelNode(host, spec.outputRate());
            case BUFFERED ->
                switch (spec.kind()) {
                    case TURBINE -> new BufferedEnergyNode.Turbine(host, spec.inputRate());
                    case MAGNET -> new BufferedEnergyNode.Magnet(host);
                    default -> new BufferedEnergyNode.Pullable(host);
                };
        };
    }

    void onLoad() {}

    final void save(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        saveState(tag, lookup);
        var turbine = turbine();
        // The existing format includes both conversion counters even on devices without a turbine.
        if (turbine == null) SteamTurbineNode.saveEmptyConversion(tag);
        else turbine.save(tag, lookup);
    }

    final void load(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        loadState(tag, lookup);
        var turbine = turbine();
        if (turbine != null) turbine.load(tag, lookup);
    }

    void saveState(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        tag.putLong(BUFFER_KEY, host.bufferedInput);
    }

    void loadState(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        host.bufferedInput = tag.getLong(BUFFER_KEY);
    }

    void dropContents() {
        var turbine = turbine();
        if (turbine != null) turbine.dropContents();
    }

    void detach() {
        var turbine = turbine();
        if (turbine != null) turbine.detach();
    }

    @Nullable
    SteamTurbineNode turbine() {
        return null;
    }

    abstract void tick(Level level, BlockPos pos, BlockState state);

    /** Cover callbacks can change which device will run the conversion tick. */
    void tickCoversBeforeDevice(Level level) {}

    abstract MachineControl control();

    @Nullable
    AdjacentEnergyControl adjacentEnergyControl() {
        return null;
    }

    boolean acceptsInputType(Tag type) {
        return host.spec() != null && type == host.spec().inType();
    }

    boolean acceptsOutputType(Tag type) {
        return host.spec() != null && type == host.spec().outType();
    }

    Collection<Tag> types() {
        var spec = host.spec();
        if (spec == null) return List.of();
        return spec.inType() == spec.outType() ? List.of(spec.outType()) : List.of(spec.inType(), spec.outType());
    }

    boolean acceptsFrom(@Nullable Direction side, boolean theoretical) {
        return side == null || side != host.facing();
    }

    boolean emitsTo(Tag type, @Nullable Direction side, boolean theoretical) {
        return side == null || side == host.facing();
    }

    long inputRate() {
        return host.effectiveInputRate();
    }

    long outputRate() {
        return host.effectiveOutputRate();
    }

    long minInput(Tag type, @Nullable Direction side) {
        return EnergyBlockDefaults.getEnergySizeInputMin(host, type, side);
    }

    long maxInput(Tag type, @Nullable Direction side) {
        return EnergyBlockDefaults.getEnergySizeInputMax(host, type, side);
    }

    long minOutput(Tag type, @Nullable Direction side) {
        return EnergyBlockDefaults.getEnergySizeOutputMin(host, type, side);
    }

    long maxOutput(Tag type, @Nullable Direction side) {
        return EnergyBlockDefaults.getEnergySizeOutputMax(host, type, side);
    }

    long demanded(Tag type, @Nullable Direction side, long size) {
        return 0;
    }

    long offered(Tag type, @Nullable Direction side, long size) {
        return 0;
    }

    long inject(Tag type, @Nullable Direction side, long size, long amount, boolean execute) {
        return 0;
    }

    long extract(Tag type, @Nullable Direction side, long size, long amount, boolean execute) {
        return 0;
    }

    long stored() {
        return host.bufferedInput;
    }

    long capacity() {
        return host.spec() == null ? 0 : host.spec().capacity();
    }

    long energyStored(Tag type) {
        var spec = host.spec();
        return spec != null && (type == spec.inType() || type == spec.outType()) ? host.bufferedInput : 0;
    }

    long energyCapacity(Tag type) {
        return capacity();
    }

    Collection<Tag> capacitorTypes(@Nullable Direction side) {
        return host.defaultCapacitorTypes(side);
    }
}
