package gregtech.engine.logistics;

/** Energy lost by a packet over a long-distance connection. */
public final class LongDistanceRules {
    private LongDistanceRules() {}

    public static long loss(long distance) {
        return Math.max(64, distance / 8);
    }
}
