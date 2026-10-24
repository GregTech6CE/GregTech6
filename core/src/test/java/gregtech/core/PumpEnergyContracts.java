package gregtech.core;

import gregtech.content.energy.PumpWorkRules;

/** Source final-packet rounding must let every valid speed reach the drain threshold. */
public final class PumpEnergyContracts {
    private static int assertions;
    public static void main(String[] args) {
        for (long packet = 8; packet <= 4096; packet++) {
            for (long sign : new long[]{1, -1}) {
                long size = packet * sign;
                long accepted = PumpWorkRules.accepted(0, size, Long.MAX_VALUE, 4096);
                long stored = accepted * packet;
                require(stored >= PumpWorkRules.START_ENERGY, "all valid speeds reach start threshold");
                require(stored < PumpWorkRules.START_ENERGY + packet, "at most one final packet overshoot");
                require(PumpWorkRules.accepted(stored, size, 1, 4096) == 0, "full buffer rejects");
                require(PumpWorkRules.accepted(8191, size, Long.MAX_VALUE, 4096) == 1,
                        "one final packet at one unit remaining");
                require(PumpWorkRules.accepted(0, size, 1, 4096) == 1, "offer count bounds acceptance");
            }
        }
        long stored = 0;
        for (int tick = 0; tick < 274; tick++) stored += 30 * PumpWorkRules.accepted(stored, 30, 1, 64);
        require(stored == 8220, "30-RU production sequence no longer deadlocks at 8190");
        require(PumpWorkRules.accepted(stored - 2048, 30, Long.MAX_VALUE, 64) * 30
                + stored - 2048 >= 8192, "recharges after draining");
        for (long size : new long[]{Long.MIN_VALUE, 0, 7, -7, 4097, Long.MAX_VALUE})
            require(PumpWorkRules.accepted(0, size, Long.MAX_VALUE, 4096) == 0, "invalid packet rejected");
        require(PumpWorkRules.accepted(0, 32, 0, 64) == 0, "zero offer rejected");
        require(PumpWorkRules.accepted(-1, 32, 1, 64) == 0, "invalid stored value rejected");
        require(PumpWorkRules.accepted(Long.MAX_VALUE, 32, 1, 64) == 0, "saved excess never wraps");
        System.out.println("Pump energy contracts passed: " + assertions + " assertions");
    }
    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
