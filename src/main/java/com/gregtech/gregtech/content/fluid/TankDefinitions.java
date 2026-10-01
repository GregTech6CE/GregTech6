package com.gregtech.gregtech.content.fluid;
import com.gregtech.gregtech.content.transport.fluid.FluidTransportDefinitions;
import com.gregtech.gregtech.registry.GTTanks;
/** Platform registration over the shared original ordinary tank catalog. */
public final class TankDefinitions {
    private TankDefinitions() {}
    public static void register() {
        for(var s:FluidTransportDefinitions.tanks()) {
            var block=GTTanks.register(s.id(),s.material(),s.type(),s.capacity(),s.gasProof(),s.acidProof(),
                    s.plasmaProof(),s.magicProof(),s.simpleOnly(),s.hardness(),s.blastResistance());
            if(s.id().equals("wood_barrel"))GTTanks.WOOD_BARREL=block;
            if(s.id().equals("drum_bronze"))GTTanks.DRUM_BRONZE=block;
            if(s.id().equals("drum_steel"))GTTanks.DRUM_STEEL=block;
        }
        GTTanks.LOGISTICS_TANK=GTTanks.registerLogisticsTank();
    }
}
