package gregtech.engine.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.api.material.GTValues;
import org.junit.jupiter.api.Test;

final class MoltenCastingChargeTest {
    @Test
    void lavaChargesRoundUpWithoutCreatingMaterial() {
        var ratio = MoltenMaterialUnits.LAVA;
        assertEquals(112, ratio.castingMillibuckets(GTValues.U));
        assertEquals(13, ratio.castingMillibuckets(GTValues.U9));
        assertEquals(1000, ratio.castingMillibuckets(9 * GTValues.U));
        for (long units : new long[] {GTValues.U9, GTValues.U4, GTValues.U, 9 * GTValues.U}) {
            int charge = Math.toIntExact(ratio.castingMillibuckets(units));
            assertTrue(ratio.toUnits(charge) >= units);
            assertTrue(ratio.toUnits(charge - 1) < units);
        }
    }

    @Test
    void metalChargesRemainExactAndOverflowCannotBecomeFree() {
        assertEquals(144, MoltenMaterialUnits.STANDARD.castingMillibuckets(GTValues.U));
        assertEquals(16, MoltenMaterialUnits.STANDARD.castingMillibuckets(GTValues.U9));
        assertEquals(0, MoltenMaterialUnits.LAVA.castingMillibuckets(0));
        assertTrue(MoltenMaterialUnits.LAVA.castingMillibuckets(Long.MAX_VALUE) > Integer.MAX_VALUE);
    }
}
