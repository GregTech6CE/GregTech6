package gregtech.content.worldgen;

/** Stateless coordinate seeds for geology, including the high bits of the world seed. */
public final class WorldgenHash {
    private WorldgenHash() {}

    static long at(long seed, int x, int z) {
        return mix(seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL));
    }

    public static long at(long seed, int x, int y, int z) {
        return at(seed ^ y * 0x165667B19E3779F9L, x, z);
    }

    static int roll(long value, int bound) {
        return (int) Long.remainderUnsigned(value, bound);
    }

    static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
