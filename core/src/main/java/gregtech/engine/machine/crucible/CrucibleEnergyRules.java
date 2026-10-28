/* Derived from GregTech-6 Team MultiTileEntitySmeltery.doInject; LGPL-3.0-or-later. */
package gregtech.engine.machine.crucible;

import gregtech.api.material.GTValues;
import gregtech.content.data.GregTechTags;
import java.math.BigInteger;
import java.util.List;

/** Original crucible energy types and positive-piston air quantity, shared by both loaders. */
public final class CrucibleEnergyRules {
    private CrucibleEnergyRules() {}

    public static final List<GregTechTags.Tag> INPUTS = List.of(
            GregTechTags.Energy.KU, GregTechTags.Energy.HU,
            GregTechTags.Energy.CU, GregTechTags.Energy.VIS_IGNIS);

    public static boolean accepts(GregTechTags.Tag type) {
        return INPUTS.contains(type);
    }

    /** Pull strokes do not add air; accepted pushing strokes are limited by remaining vessel volume. */
    public static long airAmount(long size, long count, long room) {
        if (size <= 0 || count <= 0 || room <= 0) return 0;
        return BigInteger.valueOf(size)
                .multiply(BigInteger.valueOf(count))
                .multiply(BigInteger.valueOf(GTValues.U / 1000))
                .min(BigInteger.valueOf(room))
                .longValueExact();
    }
}
