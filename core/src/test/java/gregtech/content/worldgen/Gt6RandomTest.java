package gregtech.content.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** Locks the GT6 {@code WD.random} warm-up, multipliers and dimension xor. */
class Gt6RandomTest {
    private static final long SEED = 0x123456789ABCDEFL;
    private static final long CHUNK_X = 123;
    private static final long CHUNK_Z = -456;

    @Test
    void matchesTheOriginalWarmUpAndMultipliers() {
        Random random = Gt6Random.random(SEED, CHUNK_X, CHUNK_Z);
        int[] expected = {387, 560, 879, 497, 800, 196, 284, 338};
        for (int value : expected) {
            assertEquals(value, random.nextInt(1000));
        }
    }

    @Test
    void dimensionIdChangesTheStream() {
        Random overworld = Gt6Random.forChunk(SEED, 0, CHUNK_X, CHUNK_Z);
        Random end = Gt6Random.forChunk(SEED, 1, CHUNK_X, CHUNK_Z);
        assertNotEquals(overworld.nextInt(1000), end.nextInt(1000));
    }

    @Test
    void sameCoordinatesAreDeterministic() {
        Random a = Gt6Random.forChunk(SEED, 0, CHUNK_X, CHUNK_Z);
        Random b = Gt6Random.forChunk(SEED, 0, CHUNK_X, CHUNK_Z);
        for (int i = 0; i < 16; i++) {
            assertEquals(a.nextInt(), b.nextInt());
        }
    }

    @Test
    void blockAndChunkVariantsAgree() {
        Random block = Gt6Random.forBlock(SEED, 0, CHUNK_X << 4, CHUNK_Z << 4);
        Random chunk = Gt6Random.forChunk(SEED, 0, CHUNK_X, CHUNK_Z);
        for (int i = 0; i < 16; i++) {
            assertEquals(block.nextInt(), chunk.nextInt());
        }
    }
}
