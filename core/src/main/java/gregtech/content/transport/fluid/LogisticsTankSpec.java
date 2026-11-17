package gregtech.content.transport.fluid;

import gregtech.content.material.Materials;
import gregtech.engine.fluid.FluidHazard;
import gregtech.engine.machine.TankSpec;
import java.util.EnumSet;

/** Fixed specification of the original logistics barrel (multi-tile 32072). */
public final class LogisticsTankSpec {
    private LogisticsTankSpec() {}

    public static TankSpec spec() {
        return TankSpec.of(
                        "logistics_tank",
                        Materials.Tungsten,
                        TankSpec.TankType.LOGISTICS_BARREL,
                        1_000_000,
                        EnumSet.allOf(FluidHazard.class),
                        false,
                        1f,
                        10f)
                .withMaxTemperature(100_000);
    }
}
