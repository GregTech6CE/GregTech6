package gregtech.content.blockentity.energy;

import gregtech.api.platform.machine.MachineControl;
import gregtech.content.data.GregTechTags;
import gregtech.content.data.GregTechTags.Tag;
import gregtech.engine.energy.EnergyBlockDefaults;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.machine.AdjacentEnergyControl;
import gregtech.engine.tool.GTToolHelper;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The active device's energy ports, lifecycle and interactions. */
abstract class EnergyDeviceBehavior {
    static final String BUFFER_KEY = "gt.buffer";

    final EnergyNodeBlockEntity host;

    EnergyDeviceBehavior(EnergyNodeBlockEntity host) {
        this.host = host;
    }

    final boolean isCurrent() {
        return host.isCurrent(this);
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

    void onBound() {}

    void onLoad() {}

    void onHostChanged() {}

    void onNeighborChanged() {}

    boolean weatherproof() {
        return false;
    }

    boolean skyVisible() {
        return false;
    }

    boolean allowsCovers() {
        return true;
    }

    boolean supportsControlPanels() {
        return false;
    }

    boolean coverPossible() {
        return false;
    }

    void inversionChanged() {}

    @Nullable
    EnergyDeviceTool toolAction(ItemStack tool) {
        if (host.isInvertible() && GTToolHelper.isSoftHammer(tool)) return this::invert;
        var turbine = turbine();
        return turbine == null ? null : turbine.toolAction(tool);
    }

    InteractionResult use(Player player, ItemStack tool) {
        var action = toolAction(tool);
        if (action == null) return InteractionResult.PASS;
        boolean client = player.level().isClientSide;
        if (!client) action.use(player, tool);
        return InteractionResult.sidedSuccess(client);
    }

    void invert(Player player, ItemStack tool) {
        showInversionMode(player, host.toggleInverted());
    }

    static void showInversionMode(Player player, boolean up) {
        player.displayClientMessage(
                Component.translatable(
                        up ? "message.gt6ce.transformer.step_up" : "message.gt6ce.transformer.step_down"),
                true);
    }

    @Nullable
    MenuProvider menu() {
        return null;
    }

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

    final boolean exposesRfInput(@Nullable Direction side) {
        var spec = host.spec();
        return spec != null && spec.inType() == GregTechTags.Energy.RF && (side == null || exposesRfInputOn(side));
    }

    boolean exposesRfInputOn(Direction side) {
        return side != host.facing();
    }

    boolean canReceiveRf(@Nullable Direction side) {
        // Generic RF buffers accept directly on every exposed side.
        return true;
    }

    int receiveRf(int amount, boolean simulate) {
        long space = Math.max(0, capacity() - host.bufferedInput);
        int accepted = (int) Math.min(amount, space);
        if (!simulate && accepted > 0) {
            host.bufferedInput += accepted;
            var level = host.getLevel();
            if (level != null) host.conversionMeter().input(level.getGameTime(), accepted);
            host.setChanged();
        }
        return accepted;
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

    long recommendedOutput(Tag type, @Nullable Direction side) {
        var spec = host.spec();
        return spec != null && type == spec.outType() ? outputRate() : 0;
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
