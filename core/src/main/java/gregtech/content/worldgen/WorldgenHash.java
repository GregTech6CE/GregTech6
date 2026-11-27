package gregtech.content.worldgen;

/** Stateless coordinate seeds for geology, including the high bits of the world seed. */
public final class WorldgenHash {
    private WorldgenHash() {}

    public static long at(long seed, int x, int z) {
        return mix(seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL));
    }

    public static long at(long seed, int x, int y, int z) {
        return at(seed ^ y * 0x165667B19E3779F9L, x, z);
    }

    /** FNV-1a over the complete registry name, salted by the enclosing seed. */
    static long named(long seed, String name) {
        long value = seed ^ 0xCBF29CE484222325L;
        for (int i = 0; i < name.length(); i++) value = (value ^ name.charAt(i)) * 0x100000001B3L;
        return mix(value);
    }

    public static int roll(long value, int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        return (int) Long.remainderUnsigned(value, bound);
    }

    public static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
