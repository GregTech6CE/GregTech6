package gregtech.tools.worldgen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Checks simulated or saved-world ore distances against explicitly supplied acceptance limits. */
public final class WorldgenDistributionCheck {
    private WorldgenDistributionCheck() {}

    public static void main(String[] args) throws IOException {
        int end = args.length;
        int margin = 0;
        if (end > 0 && args[end - 1].startsWith("--actual-margin=")) {
            margin = Integer.parseInt(args[--end].substring("--actual-margin=".length()));
            if (margin < 0) throw new IllegalArgumentException("Negative actual sample margin");
        }
        if (end < 3)
            throw new IllegalArgumentException(
                    "Expected: report-directory Material=maxP99[,Material=maxP99] sample-directory... [--actual-margin=N]");
        var limits = limits(args[1]);
        var rows = new ArrayList<String>();
        rows.add(
                "sample,actual_margin_chunks,source,dimension,seed,material,limit,full_p99,full_edge_sensitive,status");
        var paths = new HashSet<Path>();
        var seeds = new HashSet<Long>();
        int failures = 0;
        for (int i = 2; i < end; i++) {
            Path sample = Path.of(args[i]).toRealPath();
            if (!paths.add(sample)) throw new IllegalArgumentException("Repeated sample: " + sample);
            var input = sample(sample, margin);
            seeds.add(input.seed());
            for (var limit : limits.entrySet()) {
                var observed = input.measurements().get(limit.getKey());
                // Saved-world exports contain only observed materials, not a complete source catalog.
                if (observed == null && input.source().equals("actual_chunks")) observed = new Measurement(0, 0, 0);
                String status = status(observed, limit.getValue());
                if (!status.equals("PASS")) failures++;
                rows.add(String.format(
                        Locale.ROOT,
                        "\"%s\",%s,%s,%s,%d,%s,%.4f,%s,%s,%s",
                        sample.toString().replace("\"", "\"\""),
                        input.source().equals("actual_chunks") ? margin : "",
                        input.source(),
                        input.dimension(),
                        input.seed(),
                        limit.getKey(),
                        limit.getValue(),
                        observed == null || observed.chunks() == 0 ? "" : observed.p99(),
                        observed == null ? "" : observed.edgeSensitive(),
                        status));
            }
        }
        Path report = Files.createDirectories(Path.of(args[0])).resolve("distribution-check.csv");
        Files.write(report, rows);
        if (failures > 0) throw new IllegalStateException(failures + " checks did not pass; see " + report);
        System.out.printf(
                Locale.ROOT,
                "Passed %d material checks across %d samples and %d distinct seeds.%n",
                rows.size() - 1,
                paths.size(),
                seeds.size());
    }

    private record Measurement(int chunks, double p99, int edgeSensitive) {}

    private record Input(long seed, String source, String dimension, Map<String, Measurement> measurements) {}

    private static Input sample(Path directory, int margin) throws IOException {
        boolean synthetic = Files.exists(directory.resolve("survey.properties"));
        boolean actual = Files.exists(directory.resolve("sample.properties"));
        if (synthetic == actual)
            throw new IllegalArgumentException("Expected exactly one survey or actual sample manifest: " + directory);
        if (actual) {
            var sample = WorldgenComparison.fullDistribution(directory, margin);
            var measurements = new HashMap<String, Measurement>();
            sample.minerals().forEach((name, ore) -> {
                var distance = ore.distances();
                measurements.put(
                        name,
                        new Measurement(
                                ore.chunks(),
                                distance == null ? 0 : distance.p99(),
                                distance == null ? 0 : distance.edgeSensitive()));
            });
            return new Input(sample.seed(), "actual_chunks", sample.dimension(), measurements);
        }
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(directory.resolve("survey.properties"))) {
            properties.load(reader);
        }
        if (!"synthetic_overworld".equals(properties.getProperty("source")))
            throw new IllegalArgumentException("Not a WorldgenSurvey sample: " + directory);
        return new Input(
                Long.parseLong(properties.getProperty("seed")),
                "synthetic_overworld",
                "minecraft:overworld",
                read(directory.resolve("mineral-distribution.csv")));
    }

    private static String status(Measurement observed, double limit) {
        if (observed == null) return "MISSING_MATERIAL";
        if (observed.chunks() == 0) return "NO_ORE";
        // Missing outside targets can only shorten the measured distance; a passing upper bound is safe.
        if (observed.p99() <= limit) return "PASS";
        return observed.edgeSensitive() > 0 ? "EDGE_LIMITED" : "EXCEEDS_LIMIT";
    }

    private static Map<String, Double> limits(String argument) {
        var result = new LinkedHashMap<String, Double>();
        for (String part : argument.split(",", -1)) {
            String[] entry = part.split("=", -1);
            if (entry.length != 2 || !entry[0].matches("[A-Za-z0-9_]+"))
                throw new IllegalArgumentException("Expected Material=maxP99: " + part);
            double limit = nonnegative(entry[1]);
            if (result.putIfAbsent(entry[0], limit) != null)
                throw new IllegalArgumentException("Repeated material: " + entry[0]);
        }
        return result;
    }

    /** Reads the unquoted, numeric/material-only CSV schema emitted by {@link WorldgenSurvey}. */
    private static Map<String, Measurement> read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty()) throw new IllegalArgumentException("Empty survey: " + path);
        var columns = new HashMap<String, Integer>();
        String[] header = lines.get(0).split(",", -1);
        for (int i = 0; i < header.length; i++) {
            if (columns.putIfAbsent(header[i], i) != null)
                throw new IllegalArgumentException("Repeated column: " + header[i]);
        }
        for (String required : List.of("material", "full_chunks", "full_p99", "full_edge_sensitive")) {
            if (!columns.containsKey(required)) throw new IllegalArgumentException("Missing column: " + required);
        }
        var result = new HashMap<String, Measurement>();
        for (String line : lines.subList(1, lines.size())) {
            String[] fields = line.split(",", -1);
            if (fields.length != header.length) throw new IllegalArgumentException("Invalid survey row: " + line);
            String material = fields[columns.get("material")];
            int chunks = Integer.parseInt(fields[columns.get("full_chunks")]);
            String p99 = fields[columns.get("full_p99")];
            String edge = fields[columns.get("full_edge_sensitive")];
            if (chunks < 0 || chunks == 0 && (!p99.isEmpty() || !edge.isEmpty()))
                throw new IllegalArgumentException("Invalid absent-ore statistics: " + material);
            var measurement = new Measurement(
                    chunks, chunks == 0 ? 0 : nonnegative(p99), chunks == 0 ? 0 : Integer.parseInt(edge));
            if (measurement.edgeSensitive() < 0)
                throw new IllegalArgumentException("Negative edge-sensitive count: " + material);
            if (result.putIfAbsent(material, measurement) != null)
                throw new IllegalArgumentException("Repeated material row: " + material);
        }
        return result;
    }

    private static double nonnegative(String value) {
        double number = Double.parseDouble(value);
        if (!Double.isFinite(number) || number < 0)
            throw new IllegalArgumentException("Expected a finite nonnegative distance: " + value);
        return number;
    }
}
