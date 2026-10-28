/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.engine.recipe;

import java.math.BigInteger;

/**
 * Internal: not part of the addon contract; may change without notice.
 * Recipe work costs and bounded progress arithmetic; machine adapters own job transitions.
 */
public final class MachineWorkCost {
    public record Cost(long minimumPower, long totalWork) {}

    private MachineWorkCost() {}

    /**
     * Bound completed work without overflowing at the largest representable recipe costs.
     * This discards excess supplied work; it does not implement the original machine's
     * carry-over into the next job.
     */
    public static long advance(long completed, long total, long supplied) {
        long limit = Math.max(0, total);
        long progress = Math.min(limit, Math.max(0, completed));
        return progress + Math.min(limit - progress, Math.max(0, supplied));
    }

    public static Cost calculate(
            long recipePower,
            long duration,
            int parallel,
            boolean scaleDuration,
            int efficiency,
            long minimumInput,
            long maximumInput,
            boolean cheapOverclocking) {
        return calculate(
                recipePower,
                duration,
                parallel,
                scaleDuration,
                efficiency,
                minimumInput,
                maximumInput,
                cheapOverclocking,
                false);
    }

    /**
     * GT6 {@code MultiTileEntityBasicMachine.java:766-773}: ordinary parallel batches scale
     * power; TU keeps recipe time.
     * Returns null when these inputs have no representable cost. That is a refused calculation,
     * not a missing lookup: items and fluids use an empty stack, materials use {@code invalid()}.
     */
    public static Cost calculate(
            long recipePower,
            long duration,
            int parallel,
            boolean scaleDuration,
            int efficiency,
            long minimumInput,
            long maximumInput,
            boolean cheapOverclocking,
            boolean timeEnergy) {
        if (recipePower < 0 || recipePower > maximumInput || duration < 1 || parallel < 1 || efficiency < 1)
            return null;

        long power = Math.max(1, recipePower);
        if (!scaleDuration && !timeEnergy && recipePower > 0) {
            if (power > maximumInput / parallel) return null;
            power *= parallel;
        }

        BigInteger work = BigInteger.valueOf(power)
                .multiply(BigInteger.valueOf(duration))
                .multiply(BigInteger.valueOf(scaleDuration ? parallel : 1))
                .multiply(BigInteger.valueOf(10000))
                .add(BigInteger.valueOf(efficiency - 1))
                .divide(BigInteger.valueOf(efficiency));

        if (!cheapOverclocking)
            while (power < minimumInput && power <= maximumInput / 4) {
                power *= 4;
                work = work.multiply(BigInteger.TWO);
            }

        if (work.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) return null;
        return new Cost(power, Math.max(1, work.longValue()));
    }
}
