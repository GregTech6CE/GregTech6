package gregtech.content.blockentity.energy;

import gregtech.content.block.energy.MagnetMachineBlock;
import gregtech.content.data.GregTechTags;
import gregtech.content.energy.OriginalRotaryConverter;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyTransfer;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** A bipolar magnet emits opposite signs from its two poles and accepts input through its sides. */
final class MagnetNode extends OriginalConverterNode {
    MagnetNode(EnergyNodeBlockEntity host, EnergyNodeSpec spec) {
        super(host, spec, LATERAL);
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
