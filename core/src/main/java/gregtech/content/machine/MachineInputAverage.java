package gregtech.content.machine;

import java.util.Arrays;

/** Accepted input over the last twenty complete world ticks, independent of machine tick order. */
public final class MachineInputAverage {
    public static final int WINDOW = 20;
    private final long[] ticks = new long[WINDOW + 1];
    private final double[] units = new double[WINDOW + 1];

    public MachineInputAverage() {
        Arrays.fill(ticks, Long.MIN_VALUE);
    }

    public void record(long tick, long amount) {
        if (amount <= 0) return;
        int slot = Math.floorMod(tick, ticks.length);
        if (ticks[slot] != tick) {
            ticks[slot] = tick;
            units[slot] = 0;
        }
        units[slot] += amount;
    }

    public double average(long tick) {
        double total = 0;
        for (int slot = 0; slot < ticks.length; slot++) {
            if (ticks[slot] != Long.MIN_VALUE && ticks[slot] < tick
                    && ticks[slot] >= tick - WINDOW) total += units[slot];
        }
        return total / WINDOW;
    }
}
