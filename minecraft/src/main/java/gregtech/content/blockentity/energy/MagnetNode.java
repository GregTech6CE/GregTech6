package gregtech.content.blockentity.energy;

import gregtech.content.block.energy.MagnetMachineBlock;
import gregtech.content.data.GregTechTags;
import gregtech.content.energy.OriginalRotaryConverter;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyTransfer;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** A bipolar magnet emits opposite signs from its two poles and accepts input through its sides. */
final class MagnetNode extends OriginalConverterNode {
    private static final String STOPPED_KEY = "gt.magnet_stopped";
    private static final String LEGACY_MODE_KEY = "gt.magnet_mode";
    private static final String OVERLOADS_KEY = "gt.magnet_overloads";

    MagnetNode(EnergyNodeBlockEntity host, EnergyNodeSpec spec) {
        super(host, spec, LATERAL);
    }

    @Override
    boolean exposesRfInputOn(Direction side) {
        return LATERAL.test(host.facing(), side);
    }

    @Override
    void saveState(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveState(tag, lookup);
        saveLegacyState(host, tag, mode());
    }

    @Override
    void loadState(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadState(tag, lookup);
        loadLegacyState(host, tag);
    }

    @Override
    byte readSavedMode(CompoundTag tag) {
        return tag.contains(MODE_KEY) ? super.readSavedMode(tag) : tag.getByte(LEGACY_MODE_KEY);
    }

    static void saveLegacyState(EnergyNodeBlockEntity host, CompoundTag tag, int mode) {
        tag.putBoolean(STOPPED_KEY, host.converterStopped);
        tag.putByte(LEGACY_MODE_KEY, (byte) mode);
        tag.putInt(OVERLOADS_KEY, host.converterOverloads());
    }

    static void loadLegacyState(EnergyNodeBlockEntity host, CompoundTag tag) {
        // Original magnets use the shared mode key to signal the shared stopped flag is present.
        if (!tag.contains(MODE_KEY)) host.converterStopped = tag.getBoolean(STOPPED_KEY);
        host.restoreOverloads(tag.getInt(OVERLOADS_KEY));
    }

    @Override
    boolean emitsTo(GregTechTags.Tag type, @Nullable Direction side, boolean theoretical) {
        return side == null || side.getAxis() == host.facing().getAxis();
    }

    @Override
    long emit(long packetSize, long offeredPackets) {
        long positivePackets =
                EnergyTransfer.emitEnergyToSide(host.spec().outType(), host.facing(), packetSize, offeredPackets, host);
        long negativePackets = EnergyTransfer.emitEnergyToSide(
                host.spec().outType(), host.facing().getOpposite(), -packetSize, offeredPackets, host);
        long acceptedPackets = positivePackets + negativePackets;
        host.conversionMeter().output(host.getLevel().getGameTime(), packetSize, acceptedPackets);
        return acceptedPackets;
    }

    @Override
    BlockState visualState(BlockState blockState, OriginalRotaryConverter.Tick conversion) {
        // Magnet activity levels 1 and 2 share the same overlay.
        return blockState.setValue(MagnetMachineBlock.ACTIVE, conversion.visual() > 0);
    }
}
