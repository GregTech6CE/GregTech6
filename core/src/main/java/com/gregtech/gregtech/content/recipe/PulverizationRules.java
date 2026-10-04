/* Gregorius Techneticies / GregTech-6 Team source rule, LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTValues;
import java.math.BigInteger;

/** OM.pulverize:370-371 uses UT.Code.units(input, U, targetPerUnit, false). */
public final class PulverizationRules {
    private PulverizationRules() {}
    public static long amount(long input, long targetPerUnit) {
        if (input <= 0 || targetPerUnit <= 0) return 0;
        if (targetPerUnit == GTValues.U) return input;
        return BigInteger.valueOf(input).multiply(BigInteger.valueOf(targetPerUnit))
                .divide(BigInteger.valueOf(GTValues.U)).min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }
}
