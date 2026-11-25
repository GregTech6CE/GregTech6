package gregtech.content.blockentity.energy;

import gregtech.api.platform.machine.MachineControl;
import gregtech.content.block.energy.SolarPanelBlock;
import gregtech.content.data.GregTechTags;
import gregtech.engine.energy.EnergyTransfer;
import gregtech.engine.energy.SolarPanelEnergy;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;

/** Applies the solar panel's generation rules to its world conditions, covers and energy port. */
final class SolarPanelNode implements MachineControl {
    private final EnergyNodeBlockEntity host;
    private SolarPanelEnergy energy;

    SolarPanelNode(EnergyNodeBlockEntity host, long outputRate) {
        this.host = host;
        configure(outputRate);
    }

    void configure(long outputRate) {
        energy = new SolarPanelEnergy(outputRate, host.getBlockPos().hashCode());
    }

    void onLoad() {
        Level level = host.getLevel();
        if (level != null && !level.isClientSide && !host.isRemoved() && host.facing() == Direction.UP) {
            level.setBlock(
                    host.getBlockPos(),
                    host.getBlockState().setValue(DirectionalBlock.FACING, Direction.DOWN),
                    Block.UPDATE_ALL);
        }
    }

    void tick(Level level, BlockPos pos) {
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

        var state = host.getBlockState();
        var activeProperty = SolarPanelBlock.ACTIVE;
        if (state.hasProperty(activeProperty) && state.getValue(activeProperty) != energy.active()) {
            level.setBlock(pos, state.setValue(activeProperty, energy.active()), Block.UPDATE_CLIENTS);
        }
        host.panels().afterTick();
        host.setChanged();
    }

    long energyAvailable() {
        return energy.energy();
    }

    boolean skyVisible() {
        return energy.skyVisible();
    }

    void checkSky() {
        energy.checkSky();
    }

    boolean emitsTo(@Nullable Direction side, boolean theoretical) {
        return (side == null || side == host.facing())
                && (theoretical || side == null || !host.panels().shuttered(side));
    }

    long minOutput(GregTechTags.Tag type) {
        return type == host.spec().outType() ? host.spec().outputRate() / 8 : 0;
    }

    long maxOutput(GregTechTags.Tag type) {
        return type == host.spec().outType() ? host.spec().outputRate() : 0;
    }

    long offered(GregTechTags.Tag type, @Nullable Direction side) {
        return host.isEnergyEmittingTo(type, side, false) ? energy.energy() : 0;
    }

    long extract(GregTechTags.Tag type, @Nullable Direction side, long packetSize, long packetCount, boolean execute) {
        if (!host.isEnergyEmittingTo(type, side, false)) return 0;
        long extracted = energy.extract(packetSize, packetCount, execute);
        if (execute && extracted > 0) host.setChanged();
        return extracted;
    }

    void save(CompoundTag tag) {
        tag.putLong("gt.buffer", energy.energy());
        tag.putBoolean("gt.solar_stopped", !energy.enabled());
        tag.putBoolean("gt.solar_active", energy.active());
        tag.putBoolean("gt.solar_emitting", energy.emitting());
    }

    void load(CompoundTag tag, long savedBuffer) {
        energy.restore(
                savedBuffer,
                tag.getBoolean("gt.solar_active"),
                tag.getBoolean("gt.solar_emitting"),
                tag.getBoolean("gt.solar_stopped"));
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
