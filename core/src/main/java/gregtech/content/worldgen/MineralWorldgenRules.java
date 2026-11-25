package gregtech.content.worldgen;

import java.util.Random;

/** Shared mineral and bedrock geometry helpers; platform sinks own block identity and placement. */
public final class MineralWorldgenRules {
    public static final int HEIGHT_ANCHOR_Y = 40;
    public static final int LEGACY_ORE_FLOOR = -48;

    private MineralWorldgenRules() {}

    public static int overworldHeight(int oldY, int floor) {
        if (oldY >= HEIGHT_ANCHOR_Y) return oldY;
        if (oldY <= 0) return floor + oldY;
        double fraction = oldY / (double) HEIGHT_ANCHOR_Y;
        return (int) Math.round(floor + (HEIGHT_ANCHOR_Y - floor) * Math.pow(fraction, 1.5));
    }

    public static int stratumOreMinY(GTStoneLayersGen.OreDef ore) {
        return overworldHeight(ore.minY(), -64);
    }

    public static int stratumOreMaxY(GTStoneLayersGen.OreDef ore) {
        return overworldHeight(ore.maxY(), -64);
    }

    /** Zero-based weighted draw. A roll beyond all real weights represents an empty cell. */
    public static GTOreVeins.OreVein selectVein(java.util.List<GTOreVeins.OreVein> veins, int roll) {
        if (roll < 0) throw new IllegalArgumentException("Negative ore vein roll");
        for (var vein : veins) {
            if (roll < vein.weight()) return vein;
            roll -= vein.weight();
        }
        return null;
    }

    public record Point(int x, int z) {}

    public static Point coltanCentre(long seed) {
        Random r = new Random(seed + 5);
        return new Point((int) (r.nextGaussian() * 1500), (int) (r.nextGaussian() * 1500));
    }

    public static int scatterAttempts(int amount, java.util.function.IntUnaryOperator random) {
        return Math.max(1, amount / 2 + random.applyAsInt(1 + amount) / 2);
    }

    public static long distanceSquared(int x, int z, int minX, int minZ) {
        long dx = (long) x - minX;
        long dz = (long) z - minZ;
        return dx * dx + dz * dz;
    }

    public static boolean bedrockHit(long seed, int chunkX, int chunkZ, String name, int divisor, int densityPercent) {
        if (divisor <= 0 || densityPercent <= 0) return false;
        long roll = WorldgenHash.at(seed ^ WorldgenHash.mix(name.hashCode()), chunkX, chunkZ);
        return Long.remainderUnsigned(roll, (long) divisor * 100) < densityPercent;
    }

    public static double bedrockChancePercent(int divisor, int densityPercent) {
        if (divisor <= 0 || densityPercent <= 0) return 0;
        return Math.min(100.0, (double) densityPercent / divisor);
    }

    public static long bedrockSeed(long worldSeed, String dimension) {
        return WorldgenHash.mix(worldSeed ^ dimension.hashCode() ^ 0xBED7E5L);
    }

    public record Tier(int fromL, int toL, int radius) {}

    public static final java.util.List<Tier> PYLON_TIERS =
            java.util.List.of(new Tier(8, 10, 0), new Tier(5, 7, 1), new Tier(2, 4, 2), new Tier(0, 1, 3));
    private static final int[] MUFFIN_INNER = {5, 4, 2, 1, 0, 2, 5}, MUFFIN_OUTER = {11, 12, 14, 15, 16, 14, 11};

    public interface VeinSink {
        boolean isBedrockFloor(int x, int y, int z);

        void prepareDrillPad(int x, int floor, int z);

        void prepareStone(int x, int y, int z);

        boolean bedrock(int x, int y, int z, boolean small);

        boolean ore(int x, int y, int z, boolean small);
    }

    public static boolean bedrockVein(
            int minX,
            int minZ,
            int floor,
            int tailLimit,
            boolean prepare,
            java.util.function.IntUnaryOperator random,
            VeinSink sink) {
        if (!sink.isBedrockFloor(minX + 8, floor, minZ + 8)) return false;
        boolean core = placeBedrockCore(sink, minX + 6 + random.applyAsInt(4), floor, minZ + 6 + random.applyAsInt(4));
        for (int x = 5; x < 11 && !core; x++) {
            for (int z = 5; z < 11 && !core; z++) core = placeBedrockCore(sink, minX + x, floor, minZ + z);
        }
        if (!core) return false;
        for (int x = 5; x < 11; x++)
            for (int z = 5; z < 11; z++)
                switch (random.applyAsInt(6)) {
                    case 0 -> sink.bedrock(minX + x, floor, minZ + z, false);
                    case 1, 2 -> sink.bedrock(minX + x, floor, minZ + z, true);
                    default -> {}
                }
        for (int y = 1; y < MUFFIN_INNER.length; y++)
            for (int x = MUFFIN_INNER[y]; x < MUFFIN_OUTER[y]; x++)
                for (int z = MUFFIN_INNER[y]; z < MUFFIN_OUTER[y]; z++) {
                    if (prepare) sink.prepareStone(minX + x, floor + y, minZ + z);
                    switch (random.applyAsInt(6)) {
                        case 0 -> sink.ore(minX + x, floor + y, minZ + z, false);
                        case 1, 2 -> sink.ore(minX + x, floor + y, minZ + z, true);
                        default -> {}
                    }
                }
        for (int i = 5 + random.applyAsInt(3); i > 0; i--) {
            int x = 5 + random.applyAsInt(6), z = 5 + random.applyAsInt(6);
            for (int y = floor + MUFFIN_INNER.length; y < tailLimit; y++) {
                switch (random.applyAsInt(7)) {
                    case 0 -> x++;
                    case 1 -> x--;
                    case 2 -> z++;
                    case 3 -> z--;
                    default -> {}
                }
                if (x <= 0 || x >= 15 || z <= 0 || z >= 15) {
                    sink.ore(minX + x, y, minZ + z, true);
                    break;
                }
                if (random.applyAsInt(3) != 0) sink.ore(minX + x, y, minZ + z, true);
            }
        }
        return true;
    }

    private static boolean placeBedrockCore(VeinSink sink, int x, int floor, int z) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) if (!sink.isBedrockFloor(x + dx, floor, z + dz)) return false;
        if (!sink.bedrock(x, floor, z, false)) return false;
        sink.prepareDrillPad(x, floor, z);
        return true;
    }
}
