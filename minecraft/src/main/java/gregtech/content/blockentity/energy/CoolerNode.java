package gregtech.content.blockentity.energy;

import gregtech.content.data.GregTechTags;
import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyTransfer;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;

/**
 * A cooler emits cooling at the front and heat at the rear. GT6's two-output converter
 * advertises both energy types, but its packet-size queries describe only the primary output.
 */
final class CoolerNode extends OriginalConverterNode {
    CoolerNode(EnergyNodeBlockEntity host, EnergyNodeSpec spec) {
        super(host, spec, LATERAL);
    }

    @Override
    boolean acceptsOutputType(GregTechTags.Tag type) {
        return type == host.spec().outType() || type == GregTechTags.Energy.HU;
    }

    @Override
    Collection<GregTechTags.Tag> types() {
        return List.of(host.spec().inType(), host.spec().outType(), GregTechTags.Energy.HU);
    }

    @Override
    boolean emitsTo(GregTechTags.Tag type, @Nullable Direction side, boolean theoretical) {
        return side == null
                || side
                        == (type == host.spec().outType()
                                ? host.facing()
                                : host.facing().getOpposite());
    }

    @Override
    long emit(long packetSize, long offeredPackets) {
        long coldPackets =
                EnergyTransfer.emitEnergyToSide(host.spec().outType(), host.facing(), packetSize, offeredPackets, host);
        long heatPackets = EnergyTransfer.emitEnergyToSide(
                GregTechTags.Energy.HU, host.facing().getOpposite(), packetSize, offeredPackets, host);
        host.conversionMeter().output(host.getLevel().getGameTime(), packetSize, coldPackets);
        host.conversionMeter().auxiliaryOutput(host.getLevel().getGameTime(), packetSize, heatPackets);
        return coldPackets > 0 || heatPackets > 0 ? 1 : 0;
    }
}
