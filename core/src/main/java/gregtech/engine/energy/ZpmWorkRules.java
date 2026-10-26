package gregtech.engine.energy;

/** Pure storage, buffer-refill and light calculations; native items own the saved charge. */
public final class ZpmWorkRules {
    private ZpmWorkRules() {}

    public static final long CAPACITY = 2_000_000_000_000L, PACKET = 131072;
    public static final String KEY = "gt.zpm.energy";

    public static long dungeonCharge(boolean active) {
        return active ? CAPACITY : 0;
    }

    public static long charge(long value) {
        return Math.max(0, Math.min(CAPACITY, value));
    }

    public static long savedBuffer(long value) {
        return Math.max(0, Math.min(PACKET * 320, value));
    }

    public static int light(long energy) {
        return (int) (charge(energy) * 15 / CAPACITY);
    }

    /** Amount drawn from the non-rechargeable artifact into the discharger's working buffer. */
    public static long refillAmount(long buffer, long charge) {
        if (buffer >= PACKET * 80) return 0;
        long packets = buffer < PACKET * 40 ? 40 : 20;
        return Math.min(packets, charge / PACKET) * PACKET;
    }
}
