package gregtech.content.fluid;

import gregtech.content.registry.GTTanks;
import gregtech.content.transport.fluid.FluidTransportDefinitions;

/** Platform registration over the shared original ordinary tank catalog. */
public final class TankDefinitions {
    private TankDefinitions() {}

    public static void register(gregtech.content.registry.ContentRegistrar registrar) {
        for (var s : FluidTransportDefinitions.tanks()) {
            var block = GTTanks.register(registrar, s);
            if (s.id().equals("wood_barrel")) GTTanks.WOOD_BARREL = block;
            if (s.id().equals("drum_bronze")) GTTanks.DRUM_BRONZE = block;
            if (s.id().equals("drum_steel")) GTTanks.DRUM_STEEL = block;
        }
        GTTanks.LOGISTICS_TANK = GTTanks.registerLogisticsTank(registrar);
    }
}
