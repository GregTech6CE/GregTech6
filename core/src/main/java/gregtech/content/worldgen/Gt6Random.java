package gregtech.content.worldgen;

import java.util.Random;

/**
 * GT6 {@code WD.random}: a warmed-up {@link Random} seeded by the world seed, the dimension and
 * the chunk coordinates.
 *
 * <p>The original discards 50 draws before and after re-seeding. The two multipliers are written
 * as {@code nextLong() >> 2 + 1L}; Java parses that as {@code nextLong() >> 3}, not as
 * {@code (nextLong() >> 2) + 1}. The port keeps the original expression's result.</p>
 */
public final class Gt6Random {
    private Gt6Random() {}

    /** GT6 {@code WD.random(World, blockX, blockZ)}: block coordinates, dimension-xored seed. */
    public static Random forBlock(long worldSeed, int dimensionId, long blockX, long blockZ) {
        return random(worldSeed ^ dimensionId, blockX >> 4, blockZ >> 4);
    }

    /** GT6 {@code WD.random(World, chunkX, chunkZ)} with chunk coordinates already extracted. */
    public static Random forChunk(long worldSeed, int dimensionId, long chunkX, long chunkZ) {
        return random(worldSeed ^ dimensionId, chunkX, chunkZ);
    }

    /** GT6 {@code WD.random(long seed, long chunkX, long chunkZ)}. */
    public static Random random(long seed, long chunkX, long chunkZ) {
        Random random = new Random(seed);
        for (int i = 0; i < 50; i++) random.nextInt(0x00ffffff);
        random = new Random(seed ^ ((random.nextLong() >> 3) * chunkX + (random.nextLong() >> 3) * chunkZ));
        for (int i = 0; i < 50; i++) random.nextInt(0x00ffffff);
        return random;
    }
}
