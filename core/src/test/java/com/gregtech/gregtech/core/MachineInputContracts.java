package com.gregtech.gregtech.core;

import com.gregtech.gregtech.content.machine.MachineInputAverage;

/** Complete-tick telemetry must not depend on which adjacent block ticked first. */
public final class MachineInputContracts {
    public static void main(String[] args) {
        var steady = new MachineInputAverage();
        var split = new MachineInputAverage();
        for (int tick = 0; tick < 200; tick++) {
            steady.record(tick, 32);
            for (int source = 0; source < 4; source++) split.record(tick, 8);
            double expected = Math.min(tick, 20) * 32.0 / 20;
            require(steady.average(tick) == expected, "current partial tick excluded");
            require(split.average(tick) == expected, "four sources conserve the same received energy");
            if (tick >= 20) require(steady.average(tick) / 32 == 1, "rated input is exactly one times");
        }
        for (int tick = 200; tick <= 220; tick++) {
            require(steady.average(tick) == Math.max(0, 220 - tick) * 32.0 / 20, "twenty-tick idle decay");
        }
        var empty = new MachineInputAverage();
        empty.record(0, 0); empty.record(0, -100);
        require(empty.average(1) == 0, "invalid or simulated energy must not appear");
        empty.record(1_000_000, Long.MAX_VALUE);
        require(Double.isFinite(empty.average(1_000_001)), "large packet does not wrap negative");
        require(empty.average(1_000_021) == 0, "old bucket expires after time jump");
        System.out.println("Machine input contracts passed: 604 assertions");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
