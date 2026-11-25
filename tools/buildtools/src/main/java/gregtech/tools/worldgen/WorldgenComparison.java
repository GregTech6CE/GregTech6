package gregtech.tools.worldgen;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

/** Compares matching samples of generated chunks. All distance units in the output are blocks. */
public final class WorldgenComparison {
    private WorldgenComparison() {}

    public static void main(String[] args) throws IOException {
        int end = args.length;
        String mineral = "";
        if (end > 0 && args[end - 1].startsWith("--mineral=")) {
            mineral = args[--end].substring("--mineral=".length()).strip();
            if (mineral.isEmpty()) throw new IllegalArgumentException("--mineral requires a material name or *");
        }
        if (end < 4 || end % 2 != 0)
            throw new IllegalArgumentException(
                    "Expected: output margin baseline candidate [baseline candidate ...] [--mineral=material]");
        int margin = Integer.parseInt(args[1]);
        var comparisons = new ArrayList<Comparison>();
        for (int i = 2; i < end; i += 2) {
            var baseline = read(Path.of(args[i]), mineral);
            var candidate = read(Path.of(args[i + 1]), mineral);
            var region = baseline.region();
            if (!region.equals(candidate.region()))
                throw new IllegalArgumentException("Samples cover different seeds, dimensions, or chunk coordinates");
            validateMargin(region.side(), margin);
            comparisons.add(new Comparison(baseline, candidate));
        }
        Path output = Files.createDirectories(Path.of(args[0]));
        try (var runs = Files.newBufferedWriter(output.resolve("runs.csv"));
                var minerals = Files.newBufferedWriter(output.resolve("minerals.csv"));
                var rocks = Files.newBufferedWriter(output.resolve("rocks.csv"))) {
            runs.write("seed,dimension,build,revision,origin_x,origin_z,side,sampled_chunks,distance_origins,"
                    + "generation_ms,ms_per_chunk\n");
            minerals.write("seed,dimension,build,material,normal,poor,bedrock,raw_pebbles,ore_per_chunk,"
                    + "mineable_chunks,full_ore_chunks,raw_pebble_chunks,mineable_chunk_percent,"
                    + "mineable_mean,mineable_p95,mineable_p99,mineable_max,mineable_edge_sensitive,"
                    + "full_mean,full_p95,full_p99,full_max,full_edge_sensitive\n");
            rocks.write("seed,dimension,build,block,volume,rock_share_percent\n");
            for (int i = 0; i < comparisons.size(); i++) {
                var comparison = comparisons.get(i);
                var baseline = comparison.baseline();
                var candidate = comparison.candidate();
                var names = new TreeSet<>(baseline.minerals().keySet());
                names.addAll(candidate.minerals().keySet());
                write(runs, minerals, rocks, baseline, "baseline", names, margin);
                write(runs, minerals, rocks, candidate, "candidate", names, margin);
                if (!mineral.isEmpty())
                    WorldgenHeatmap.write(output.resolve("heatmap-" + (i + 1) + ".svg"), mineral, baseline, candidate);
            }
        }
    }

    record Region(long seed, String dimension, int originX, int originZ, int side) {
        private int chunkIndex(int x, int z) {
            long dx = (long) x - originX;
            long dz = (long) z - originZ;
            if (dx < 0 || dx >= side || dz < 0 || dz >= side)
                throw new IllegalArgumentException("Chunk outside sample: " + x + "," + z);
            return (int) (dx * side + dz);
        }

        private int chunks() {
            return Math.multiplyExact(side, side);
        }
    }

    record Sample(
            Region region,
            String revision,
            Double millis,
            Map<String, Mineral> minerals,
            Map<String, Long> rocks,
            WorldgenHeatmap heatmap) {}

    private record Comparison(Sample baseline, Sample candidate) {}

    record FullOre(int chunks, Distances distances) {}

    record FullDistribution(long seed, String dimension, Map<String, FullOre> minerals) {}

    /** Uses the same block-distance calculation as the baseline/candidate comparison. */
    static FullDistribution fullDistribution(Path directory, int margin) throws IOException {
        var sample = read(directory, "");
        validateMargin(sample.region().side(), margin);
        var minerals = new TreeMap<String, FullOre>();
        sample.minerals()
                .forEach((name, ore) -> minerals.put(
                        name,
                        new FullOre(
                                ore.full.cardinality(),
                                distances(ore.full, sample.region().side(), margin))));
        return new FullDistribution(sample.region().seed(), sample.region().dimension(), Map.copyOf(minerals));
    }

    private static void validateMargin(int side, int margin) {
        if (margin < 0 || margin > (side - 1) / 2)
            throw new IllegalArgumentException("Margin leaves no origins in a sample");
    }

    private static final class Mineral {
        private long normal;
        private long poor;
        private long bedrock;
        private long pebbles;
        private final BitSet mineable = new BitSet();
        private final BitSet full = new BitSet();
        private final BitSet clues = new BitSet();

        private void add(int chunk, long normal, long poor, long bedrock, long pebbles) {
            this.normal += normal;
            this.poor += poor;
            this.bedrock += bedrock;
            this.pebbles += pebbles;
            if (normal > 0 || poor > 0) mineable.set(chunk);
            if (normal > 0) full.set(chunk);
            if (pebbles > 0) clues.set(chunk);
        }
    }

    private static Sample read(Path directory, String mineral) throws IOException {
        var properties = new Properties();
        try (var input = Files.newBufferedReader(directory.resolve("sample.properties"))) {
            properties.load(input);
        }
        var region = new Region(
                Long.parseLong(properties.getProperty("seed")),
                Objects.requireNonNull(properties.getProperty("dimension"), "Missing sample dimension"),
                Integer.parseInt(properties.getProperty("origin_x")),
                Integer.parseInt(properties.getProperty("origin_z")),
                Integer.parseInt(properties.getProperty("side")));
        String timing = properties.getProperty("generation_ms");
        Double millis = timing == null ? null : Double.parseDouble(timing);
        if (region.side() < 1 || region.chunks() < 1)
            throw new IllegalArgumentException("Invalid sample scope in " + directory);
        if (millis != null && (!Double.isFinite(millis) || millis < 0))
            throw new IllegalArgumentException("Invalid generation time in " + directory);
        var heatmap = mineral.isEmpty() ? null : new WorldgenHeatmap(region.chunks());
        var minerals = new TreeMap<String, Mineral>();
        try (var input = Files.newBufferedReader(directory.resolve("ores.csv"))) {
            if (!"chunk_x,chunk_z,material,normal,poor,bedrock,raw_pebbles".equals(input.readLine()))
                throw new IllegalArgumentException("Unexpected ore sample header in " + directory);
            for (String row; (row = input.readLine()) != null; ) {
                var column = row.split(",", -1);
                if (column.length != 7 || column[2].isBlank())
                    throw new IllegalArgumentException("Invalid ore row in " + directory + ": " + row);
                int chunk = region.chunkIndex(Integer.parseInt(column[0]), Integer.parseInt(column[1]));
                long normal = count(column[3]), poor = count(column[4]), bedrock = count(column[5]);
                long pebbles = count(column[6]);
                minerals.computeIfAbsent(column[2], key -> new Mineral()).add(chunk, normal, poor, bedrock, pebbles);
                if (heatmap != null && ("*".equals(mineral) || mineral.equalsIgnoreCase(column[2])))
                    heatmap.add(chunk, normal, poor, bedrock, pebbles);
            }
        }
        var rocks = new TreeMap<String, Long>();
        try (var input = Files.newBufferedReader(directory.resolve("rocks.csv"))) {
            if (!"block,volume".equals(input.readLine()))
                throw new IllegalArgumentException("Unexpected rock sample header in " + directory);
            for (String row; (row = input.readLine()) != null; ) {
                var column = row.split(",", -1);
                if (column.length != 2 || column[0].isBlank())
                    throw new IllegalArgumentException("Invalid rock row in " + directory + ": " + row);
                rocks.merge(column[0], count(column[1]), Long::sum);
            }
        }
        return new Sample(
                region,
                properties.getProperty("revision", directory.getFileName().toString()),
                millis,
                minerals,
                rocks,
                heatmap);
    }

    private static long count(String field) {
        long value = Long.parseLong(field);
        if (value < 0) throw new IllegalArgumentException("Negative sample count: " + value);
        return value;
    }

    private static void write(
            BufferedWriter runs,
            BufferedWriter minerals,
            BufferedWriter rocks,
            Sample sample,
            String label,
            Iterable<String> names,
            int margin)
            throws IOException {
        var region = sample.region();
        String prefix = "%d,%s,%s,".formatted(region.seed(), region.dimension(), label);
        int interior = region.side() - 2 * margin;
        int chunks = region.chunks();
        runs.write(prefix
                + String.format(
                        Locale.ROOT,
                        "\"%s\",%d,%d,%d,%d,%d,",
                        sample.revision().replace("\"", "\"\""),
                        region.originX(),
                        region.originZ(),
                        region.side(),
                        chunks,
                        interior * interior));
        Double millis = sample.millis();
        runs.write(millis == null ? "," : String.format(Locale.ROOT, "%.3f,%.3f", millis, millis / chunks));
        runs.newLine();
        var empty = new Mineral();
        for (String name : names) {
            var ore = sample.minerals().getOrDefault(name, empty);
            minerals.write(prefix
                    + String.format(
                            Locale.ROOT,
                            "%s,%d,%d,%d,%d,%.4f,%d,%d,%d,%.2f",
                            name,
                            ore.normal,
                            ore.poor,
                            ore.bedrock,
                            ore.pebbles,
                            (ore.normal + ore.poor + ore.bedrock) / (double) chunks,
                            ore.mineable.cardinality(),
                            ore.full.cardinality(),
                            ore.clues.cardinality(),
                            100.0 * ore.mineable.cardinality() / chunks));
            minerals.write(distanceFields(distances(ore.mineable, region.side(), margin)));
            minerals.write(distanceFields(distances(ore.full, region.side(), margin)));
            minerals.newLine();
        }
        long rockTotal =
                sample.rocks().values().stream().mapToLong(Long::longValue).sum();
        for (var rock : sample.rocks().entrySet()) {
            double share = rockTotal == 0 ? 0 : 100.0 * rock.getValue() / rockTotal;
            rocks.write(prefix + String.format(Locale.ROOT, "%s,%d,%.4f%n", rock.getKey(), rock.getValue(), share));
        }
    }

    record Distances(double mean, double p95, double p99, double max, int edgeSensitive) {}

    static Distances distances(BitSet present, int side, int margin) {
        if (present.isEmpty()) return null;
        // A row only needs its nearest target along Z. This keeps dense samples from
        // comparing every origin with almost every chunk.
        int[] occupiedRows =
                present.stream().map(index -> index / side).distinct().toArray();
        int[] rowDistances = new int[side * side];
        for (int row : occupiedRows) {
            int start = row * side;
            int last = -side;
            for (int z = 0; z < side; z++) {
                if (present.get(start + z)) last = z;
                rowDistances[start + z] = z - last;
            }
            last = side * 2;
            for (int z = side - 1; z >= 0; z--) {
                if (present.get(start + z)) last = z;
                rowDistances[start + z] = Math.min(rowDistances[start + z], last - z);
            }
        }
        int interior = side - 2 * margin;
        double[] distances = new double[interior * interior];
        int count = 0;
        int edgeSensitive = 0;
        double sum = 0;
        for (int x = margin; x < side - margin; x++) {
            for (int z = margin; z < side - margin; z++) {
                long nearest = Long.MAX_VALUE;
                if (present.get(x * side + z)) {
                    nearest = 0;
                } else {
                    for (int row : occupiedRows) {
                        long dx = x - row;
                        long dz = rowDistances[row * side + z];
                        nearest = Math.min(nearest, dx * dx + dz * dz);
                    }
                }
                long outside = Math.min(Math.min(x + 1, side - x), Math.min(z + 1, side - z));
                if (nearest > outside * outside) edgeSensitive++;
                double blocks = 16 * Math.sqrt(nearest);
                distances[count++] = blocks;
                sum += blocks;
            }
        }
        Arrays.sort(distances);
        return new Distances(
                sum / count,
                percentile(distances, 0.95),
                percentile(distances, 0.99),
                distances[count - 1],
                edgeSensitive);
    }

    private static double percentile(double[] ascending, double fraction) {
        return ascending[(int) Math.ceil(fraction * ascending.length) - 1];
    }

    static String distanceFields(Distances distances) {
        if (distances == null) return ",,,,,";
        return String.format(
                Locale.ROOT,
                ",%.4f,%.4f,%.4f,%.4f,%d",
                distances.mean(),
                distances.p95(),
                distances.p99(),
                distances.max(),
                distances.edgeSensitive());
    }
}
