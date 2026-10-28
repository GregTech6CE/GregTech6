/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.engine.recipe;

/**
 * Internal: not part of the addon contract; may change without notice.
 *
 * <p>Parallel-batch bounds and the output-space simulation, on primitive keys. The Minecraft
 * adapter maps item/fluid stacks to keys; the sharing rules (merge into a matching slot, one pass
 * for empty slots) stay here so they can be tested without a game runtime.</p>
 */
public final class ParallelFit {
    /** Matches no output; such a slot or tank is never used. */
    public static final int NO_MATCH = -2;
    /** Empty slot or tank; used by the first pass only. */
    public static final int EMPTY = -1;

    private ParallelFit() {}

    /** Power-scaled batches are capped at nominal input; duration-scaled and TU batches are not. */
    public static int powerCap(
            int limit, long nominalInput, long recipePower, boolean scalesDuration, boolean timeEnergy) {
        if (scalesDuration || timeEnergy || recipePower <= 0) return limit;
        return (int) Math.min(limit, Math.max(1, nominalInput / recipePower));
    }

    /** Keeps a fluid output's scaled amount representable as an {@code int}. */
    public static int amountCap(int limit, long amount) {
        return amount <= 0 ? limit : (int) Math.min(limit, Integer.MAX_VALUE / amount);
    }

    /**
     * Whether {@code parallel} runs of the outputs fit into the given slots.
     *
     * <p>Each output first merges into slots already keyed to it, then uses empty slots; the empty
     * slots become keyed to that output, so a later pass fills them further up to capacity. Both
     * arrays are mutated as the simulation proceeds; callers pass copies.</p>
     *
     * @param slotKeys          per-slot output key, {@link #EMPTY} or {@link #NO_MATCH}
     * @param slotCounts        per-slot current stack size
     * @param capacityPerOutput per-output stack limit
     * @param outputCounts      per-output batch size for one run
     */
    public static boolean itemsFit(
            int[] slotKeys, int[] slotCounts, int[] capacityPerOutput, long[] outputCounts, int parallel) {
        for (int output = 0; output < outputCounts.length; output++) {
            long remaining = outputCounts[output] * parallel;
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (int slot = 0; slot < slotKeys.length && remaining > 0; slot++) {
                    int key = slotKeys[slot];
                    boolean empty = key == EMPTY;
                    if (empty ? pass != 0 : key != output) continue;
                    int space = capacityPerOutput[output] - (empty ? 0 : slotCounts[slot]);
                    int take = (int) Math.min(Math.max(0, space), remaining);
                    if (take <= 0) continue;
                    slotKeys[slot] = output;
                    slotCounts[slot] = (empty ? 0 : slotCounts[slot]) + take;
                    remaining -= take;
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    /**
     * Whether {@code parallel} runs of the fluid outputs fit into the given tanks.
     *
     * @param tankKeys      per-tank output key, {@link #EMPTY} or {@link #NO_MATCH}
     * @param free          per-tank free capacity, reduced as the simulation proceeds
     * @param outputAmounts per-output batch size for one run
     */
    public static boolean fluidsFit(int[] tankKeys, long[] free, long[] outputAmounts, int parallel) {
        for (int output = 0; output < outputAmounts.length; output++) {
            long remaining = outputAmounts[output] * parallel;
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (int tank = 0; tank < tankKeys.length && remaining > 0; tank++) {
                    boolean empty = tankKeys[tank] == EMPTY;
                    if (empty ? pass != 0 : tankKeys[tank] != output) continue;
                    if (free[tank] <= 0) continue;
                    long take = Math.min(free[tank], remaining);
                    free[tank] -= take;
                    if (empty && take > 0) tankKeys[tank] = output;
                    remaining -= take;
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }
}
