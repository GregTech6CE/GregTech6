package gregtech.content.transport.fluid;

import gregtech.engine.machine.TankSpec;
import gregtech.content.material.Materials;

/** Fixed specification of the original logistics barrel (multi-tile 32072). */
public final class LogisticsTankSpec {
    private LogisticsTankSpec() {}

    public static TankSpec spec() {
        return TankSpec.of("logistics_tank", Materials.Tungsten, TankSpec.TankType.LOGISTICS_BARREL,
                1_000_000, true, true, true, true, false, 1f, 10f)
                .withMaxTemperature(100_000);
    }
}
