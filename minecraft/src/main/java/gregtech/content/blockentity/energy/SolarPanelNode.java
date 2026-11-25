package gregtech.content.blockentity.energy;

import gregtech.api.platform.machine.MachineControl;
import gregtech.content.block.energy.SolarPanelBlock;
import gregtech.content.data.GregTechTags;
import gregtech.engine.energy.EnergyTransfer;
import gregtech.engine.energy.SolarPanelEnergy;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Applies the solar panel's generation rules to its world conditions, covers and energy port. */
final class SolarPanelNode extends EnergyDeviceBehavior implements MachineControl {
    private final SolarPanelEnergy energy;

    SolarPanelNode(EnergyNodeBlockEntity host, long outputRate) {
        super(host);
        energy = new SolarPanelEnergy(outputRate, host.getBlockPos().hashCode());
    }

    @Override
    void onLoad() {
        Level level = host.getLevel();
        if (level != null && !level.isClientSide && !host.isRemoved() && host.facing() == Direction.UP) {
            level.setBlock(
                    host.getBlockPos(),
                    host.getBlockState().setValue(DirectionalBlock.FACING, Direction.DOWN),
                    Block.UPDATE_ALL);
        }
    }

    @Override
    void tick(Level level, BlockPos pos, BlockState state) {
        host.panels().beforeTick();
        var conditions = new SolarPanelEnergy.Conditions(
                level.isDay(),
                level.isRaining(),
                level.isThundering(),
                level.dimension().location().toString().equals("twilightforest:twilight_forest"),
                level.getBiome(pos).value().getModifiedClimateSettings().downfall());
        energy.tick(
                () -> level.dimensionType().hasSkyLight() && level.canSeeSky(pos.above()),
                conditions,
                packetSize -> EnergyTransfer.emitEnergyToNetwork(host.spec().outType(), packetSize, 1, host));

        var currentState = host.getBlockState();
        var activeProperty = SolarPanelBlock.ACTIVE;
        if (currentState.hasProperty(activeProperty) && currentState.getValue(activeProperty) != energy.active()) {
            level.setBlock(pos, currentState.setValue(activeProperty, energy.active()), Block.UPDATE_CLIENTS);
        }
        host.panels().afterTick();
        host.setChanged();
    }

    @Override
    long stored() {
        return energy.energy();
    }

    @Override
    Collection<GregTechTags.Tag> capacitorTypes(@Nullable Direction side) {
        return List.of();
    }

    @Override
    long energyCapacity(GregTechTags.Tag type) {
        return 0;
    }

    @Override
    long energyStored(GregTechTags.Tag type) {
        return 0;
    }

    boolean skyVisible() {
        return energy.skyVisible();
    }

    void checkSky() {
        energy.checkSky();
    }

    @Override
    boolean acceptsInputType(GregTechTags.Tag type) {
        return false;
    }

    @Override
    boolean emitsTo(GregTechTags.Tag type, @Nullable Direction side, boolean theoretical) {
        return (side == null || side == host.facing())
                && (theoretical || side == null || !host.panels().shuttered(side));
    }

    @Override
    long minOutput(GregTechTags.Tag type, @Nullable Direction side) {
        return type == host.spec().outType() ? host.spec().outputRate() / 8 : 0;
    }

    @Override
    long maxOutput(GregTechTags.Tag type, @Nullable Direction side) {
        return type == host.spec().outType() ? host.spec().outputRate() : 0;
    }

    @Override
    long offered(GregTechTags.Tag type, @Nullable Direction side, long packetSize) {
        return host.isEnergyEmittingTo(type, side, false) ? energy.energy() : 0;
    }

    @Override
    long extract(GregTechTags.Tag type, @Nullable Direction side, long packetSize, long packetCount, boolean execute) {
        if (!host.isEnergyEmittingTo(type, side, false)) return 0;
        long extracted = energy.extract(packetSize, packetCount, execute);
        if (execute && extracted > 0) host.setChanged();
        return extracted;
    }

    @Override
    void saveState(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        tag.putLong(BUFFER_KEY, energy.energy());
        tag.putBoolean("gt.solar_stopped", !energy.enabled());
        tag.putBoolean("gt.solar_active", energy.active());
        tag.putBoolean("gt.solar_emitting", energy.emitting());
    }

    @Override
    void loadState(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        energy.restore(
                tag.getLong(BUFFER_KEY),
                tag.getBoolean("gt.solar_active"),
                tag.getBoolean("gt.solar_emitting"),
                tag.getBoolean("gt.solar_stopped"));
        host.bufferedInput = 0;
    }

    @Override
    MachineControl control() {
        return this;
    }

    @Override
    public boolean supportsProgress() {
        return false;
    }

    @Override
    public boolean enabled() {
        return energy.enabled();
    }

    @Override
    public boolean setEnabled(boolean enabled) {
        energy.enabled(enabled);
        host.setChanged();
        return enabled;
    }

    @Override
    public boolean running() {
        return energy.active();
    }

    @Override
    public boolean active() {
        return energy.emitting();
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
