/* Copyright GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * MultiTileEntitySmeltery and MultiTileEntityCrucible gas, fire and explosion rules. */
package com.gregtech.gregtech.api.machine.crucible;

import com.gregtech.gregtech.api.material.GTValues;
import java.math.BigInteger;
import java.util.function.IntUnaryOperator;

/** Source parameters and integer arithmetic; world effects belong to the platform. */
public final class CrucibleHazards {
    private CrucibleHazards() {}
    public record Profile(int range, float temperatureMultiplier, long capacity, int explosionScale,
                          boolean largeVessel) {
        public int displayedFireRange() { return range + 1; }
    }
    public record FireTarget(int x, int y, int z, boolean checkFlammability) {}
    public static final Profile SMALL = new Profile(3, 2F, 16L * GTValues.U, 6, false);
    public static final Profile LARGE = new Profile(5, 4F, 432L * GTValues.U, 8, true);

    /** GT6 only ignites from a vapor whose own boiling point is at least2000K. */
    public static int vaporFireAttempts(long boilingPoint, long amount) {
        if (boilingPoint < 2000 || amount <= 0) return 0;
        return Math.max(1, bindInt(BigInteger.valueOf(amount).multiply(BigInteger.valueOf(9))
                .divide(BigInteger.valueOf(GTValues.U))));
    }
    public static int meltdownFireAttempts(long temperature) {
        return temperature <= 0 ? 0 : bindInt(BigInteger.valueOf(temperature / 25));
    }
    private static int bindInt(BigInteger value) {
        return value.min(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
    }
    /** UT.Code.scale has a positive first step, rather than a simple fractional capacity. */
    public static int explosionPower(Profile profile, long amount) {
        if (amount <= 0) return 0;
        if (amount >= profile.capacity()) return profile.explosionScale();
        return 1 + BigInteger.valueOf(amount).multiply(BigInteger.valueOf(profile.explosionScale() - 1))
                .divide(BigInteger.valueOf(profile.capacity())).intValue();
    }
    public static FireTarget fireTarget(Profile profile, IntUnaryOperator nextInt) {
        int r = profile.range();
        return new FireTarget(-r + nextInt.applyAsInt(2 * r + 1), -1 + nextInt.applyAsInt(r + 2),
                -r + nextInt.applyAsInt(2 * r + 1), nextInt.applyAsInt(3) != 0);
    }
}
