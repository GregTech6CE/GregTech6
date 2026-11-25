package gregtech.content.blockentity.energy;

import gregtech.api.platform.machine.MachineControl;
import gregtech.content.block.energy.OriginalMotorBlock;
import gregtech.content.block.energy.RotaryConverterBlock;
import gregtech.content.data.GregTechTags;
import gregtech.content.energy.OriginalRotaryConverter;
import gregtech.content.energy.OriginalThermalConverter;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyTransfer;
import gregtech.engine.machine.AdjacentEnergyControl;
import gregtech.engine.tool.GTToolHelper;
import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** World-facing behavior for devices that share the original GT6 conversion cycle. */
class OriginalConverterNode extends EnergyDeviceBehavior implements MachineControl {
    static final String MODE_KEY = "gt.mode";
    private static final BiPredicate<Direction, Direction> REAR = (front, side) -> side == front.getOpposite();
    private static final BiPredicate<Direction, Direction> EXCEPT_FRONT = (front, side) -> side != front;
    static final BiPredicate<Direction, Direction> LATERAL = (front, side) -> side.getAxis() != front.getAxis();

    private final BiPredicate<Direction, Direction> inputFaces;
    private final OriginalRotaryConverter.State state;

    OriginalConverterNode(
            EnergyNodeBlockEntity host, EnergyNodeSpec spec, BiPredicate<Direction, Direction> inputFaces) {
        super(host);
        this.inputFaces = inputFaces;
        state = new OriginalRotaryConverter.State(spec);
    }

    OriginalConverterNode(EnergyNodeBlockEntity host, EnergyNodeSpec spec) {
        this(host, spec, spec.deviceType() == EnergyNodeSpec.DeviceType.DYNAMO ? REAR : EXCEPT_FRONT);
    }

    @Override
    boolean supportsControlPanels() {
        return true;
    }

    @Override
    boolean coverPossible() {
        return true;
    }

    @Override
    @Nullable
    EnergyDeviceTool toolAction(ItemStack tool) {
        if (host.isOriginalMotor()) {
            if (GTToolHelper.isMonkeyWrench(tool)) return this::reverseMotor;
            if (GTToolHelper.isMagnifyingGlass(tool)) return this::inspectMotor;
        }
        return super.toolAction(tool);
    }

    private void reverseMotor(Player player, ItemStack tool) {
        reverseMotor();
        inspectMotor(player, tool);
    }

    private void inspectMotor(Player player, ItemStack tool) {
        GTToolHelper.damageForUse(tool, 1, player);
        String direction = host.motorCounterClockwise() ? "Counterclockwise" : "Clockwise";
        player.displayClientMessage(Component.literal(direction), true);
    }

    boolean reverseMotor() {
        host.bufferedInput = 0;
        boolean counterClockwise = state.reverse();
        host.setChanged();
        return counterClockwise;
    }

    boolean counterClockwise() {
        return host.isOriginalMotor() && state.counterClockwise();
    }

    boolean emitted() {
        return state.emitted();
    }

    boolean fast() {
        return state.fast();
    }

    @Override
    boolean acceptsFrom(@Nullable Direction side, boolean theoretical) {
        return (theoretical || !host.converterStopped)
                && (side == null || inputFaces.test(host.facing(), side))
                && host.hasEnergySurface(side);
    }

    @Override
    boolean canReceiveRf(@Nullable Direction side) {
        return acceptsInputType(GregTechTags.Energy.RF) && acceptsFrom(side, false);
    }

    @Override
    int receiveRf(int amount, boolean simulate) {
        return (int) host.doEnergyInjection(GregTechTags.Energy.RF, null, 1, amount, !simulate);
    }

    @Override
    long capacity() {
        return OriginalRotaryConverter.capacity(host.spec());
    }

    @Override
    long inputRate() {
        return host.spec().inputRate();
    }

    @Override
    long outputRate() {
        return host.spec().outputRate();
    }

    @Override
    long minInput(GregTechTags.Tag type, @Nullable Direction side) {
        return type == host.spec().inType() ? OriginalRotaryConverter.inputMinimum(host.spec()) : 0;
    }

    @Override
    long maxInput(GregTechTags.Tag type, @Nullable Direction side) {
        return type == host.spec().inType() ? inputRate() * 2 : 0;
    }

    @Override
    long minOutput(GregTechTags.Tag type, @Nullable Direction side) {
        return type == host.spec().outType() ? outputRate() / 2 : 0;
    }

    @Override
    long maxOutput(GregTechTags.Tag type, @Nullable Direction side) {
        return type == host.spec().outType() ? outputRate() * 2 : 0;
    }

    @Override
    long inject(
            GregTechTags.Tag energyType, @Nullable Direction side, long packetSize, long packetCount, boolean execute) {
        if (!host.isEnergyAcceptingFrom(energyType, side, false)) return 0;
        var injection = state.inject(host.bufferedInput, packetSize, packetCount, execute);
        if (execute) {
            if (host.getLevel() != null) {
                host.conversionMeter().input(host.getLevel().getGameTime(), packetSize, injection.consumed());
            }
            host.bufferedInput = injection.energy();
            if (injection.overloaded()) host.handleConverterOverload(Math.abs(packetSize), energyType);
            if (injection.consumed() > 0) host.setChanged();
        }
        return injection.consumed();
    }

    @Override
    void tickCoversBeforeDevice(Level level) {
        host.panels().beforeTick();
        host.tickCoverComponents(level);
    }

    @Override
    void tick(Level level, BlockPos pos, BlockState state) {
        convertBufferedInput();
    }

    final void convertBufferedInput() {
        long bufferBeforeConversion = host.bufferedInput;
        var conversion = state.tick(host.bufferedInput, host.converterStopped, this::emit);
        host.bufferedInput = conversion.energy();
        if (conversion.overloaded()) {
            host.handleConverterOverload(bufferBeforeConversion, host.spec().outType());
        }
        var level = host.getLevel();
        var pos = host.getBlockPos();
        if (host.isRemoved() || level.getBlockEntity(pos) != host) return;
        host.coolDownOverload(conversion.possible());

        var converter = host.originalConverter();
        if (converter == null) return;
        var blockState = host.getBlockState();
        var nextState = converter.visualState(blockState, conversion);
        if (nextState != blockState) {
            level.setBlock(pos, nextState, Block.UPDATE_CLIENTS);
        } else {
            host.syncBufferedInput(level, pos, blockState);
        }
        host.panels().afterTick();
        host.setChanged();
    }

    BlockState visualState(BlockState blockState, OriginalRotaryConverter.Tick conversion) {
        var nextState = blockState.setValue(RotaryConverterBlock.ACTIVITY, conversion.visual());
        if (host.isOriginalMotor()) {
            nextState = nextState
                    .setValue(OriginalMotorBlock.COUNTER_CLOCKWISE, state.counterClockwise())
                    .setValue(OriginalMotorBlock.FAST, conversion.fast());
        }
        return nextState;
    }

    long emit(long packetSize, long offeredPackets) {
        long acceptedPackets =
                EnergyTransfer.emitEnergyToNetwork(host.spec().outType(), packetSize, offeredPackets, host);
        host.conversionMeter().output(host.getLevel().getGameTime(), packetSize, acceptedPackets);
        return acceptedPackets;
    }

    @Override
    MachineControl control() {
        return this;
    }

    @Override
    AdjacentEnergyControl adjacentEnergyControl() {
        return enabled -> {
            // Manual and adjacent requests share the stopped state. Stored energy still drains normally.
            if (available()
                    && (host.getLevel() == null || !host.getLevel().isClientSide)
                    && host.converterStopped == enabled) {
                host.converterStopped = !enabled;
                host.setChanged();
            }
            return !host.converterStopped;
        };
    }

    @Override
    public boolean available() {
        // A retained converter control can still address a retuned converter.
        return !host.isRemoved() && host.originalConverter() != null;
    }

    @Override
    public boolean supportsProgress() {
        return false;
    }

    @Override
    public boolean supportsMode() {
        return host.isOriginalMotor() || host.isMagnet() || OriginalThermalConverter.modeSelectable(host.spec());
    }

    // Covers may retain their control across a spec change.
    @Override
    public int mode() {
        var current = host.originalConverter();
        return current == null ? 0 : current.state.mode();
    }

    @Override
    public int setMode(int value) {
        if (!available()) return 0;
        var current = host.originalConverter();
        if (current != null && supportsMode()) current.state.mode(value);
        host.setChanged();
        return mode();
    }

    @Override
    public boolean enabled() {
        return !host.converterStopped;
    }

    @Override
    public boolean setEnabled(boolean enabled) {
        if (!available()) return false;
        host.converterStopped = !enabled;
        host.setChanged();
        return enabled;
    }

    @Override
    public boolean running() {
        var current = host.originalConverter();
        return current != null && current.state.possible();
    }

    @Override
    public boolean active() {
        var current = host.originalConverter();
        return current != null && current.state.emitted();
    }

    @Override
    public long progress() {
        return 0;
    }

    @Override
    public long progressMax() {
        return 0;
    }

    @Override
    void saveState(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveState(tag, lookup);
        var snapshot = state.snapshot();
        tag.putByte(MODE_KEY, (byte) snapshot.mode());
        if (host.isOriginalMotor()) tag.putBoolean("gt.reversed", snapshot.counterClockwise());
        tag.putBoolean("gt.active.energy", snapshot.emitted());
        tag.putBoolean("gt.can.energy", snapshot.possible());
        if (host.isOriginalMotor()) tag.putBoolean("gt.visual", snapshot.fast());
    }

    byte readSavedMode(CompoundTag tag) {
        return tag.getByte(MODE_KEY);
    }

    @Override
    void loadState(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadState(tag, lookup);
        state.restore(new OriginalRotaryConverter.Snapshot(
                readSavedMode(tag),
                host.isOriginalMotor() && tag.getBoolean("gt.reversed"),
                false,
                tag.getBoolean("gt.can.energy"),
                tag.getBoolean("gt.active.energy"),
                host.isOriginalMotor() && tag.getBoolean("gt.visual")));
    }

    static final class SteamConverter extends OriginalConverterNode {
        private final SteamTurbineNode turbine;

        SteamConverter(EnergyNodeBlockEntity host, EnergyNodeSpec spec) {
            super(host, spec, REAR);
            turbine = new SteamTurbineNode(host, spec.inputRate());
        }

        @Override
        SteamTurbineNode turbine() {
            return turbine;
        }

        @Override
        void tick(Level level, BlockPos pos, BlockState state) {
            turbine.tick();
            var converter = host.originalConverter();
            if (converter != null) converter.convertBufferedInput();
        }
    }
}
