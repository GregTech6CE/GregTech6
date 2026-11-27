package gregtech.tools.worldgen;

import gregtech.content.worldgen.GTCellNoise;
import gregtech.content.worldgen.StoneLayerField;
import gregtech.tools.worldgen.WorldgenSampler.Layer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Rock field at 9b64bc17d. The table retains the position and ore eligibility of every original row. */
final class LegacyStrata {
    private final GTCellNoise field;
    private final List<Layer> layers;
    private final Map<Long, Integer> topByChunk;

    LegacyStrata(long seed, Path heights) throws IOException {
        field = new GTCellNoise(seed);
        var input = Objects.requireNonNull(getClass().getResourceAsStream("/worldgen/strata-9b64bc17d.csv"));
        try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            layers = reader.lines()
                    .skip(1)
                    .map(row -> {
                        var values = row.split(",");
                        return new Layer(values[0], Boolean.parseBoolean(values[1]), Boolean.parseBoolean(values[2]));
                    })
                    .toList();
        }
        topByChunk = heights == null ? null : readHeights(heights);
    }

    List<Layer> layers() {
        return layers;
    }

    private static Map<Long, Integer> readHeights(Path file) throws IOException {
        var result = new HashMap<Long, Integer>();
        try (var reader = Files.newBufferedReader(file)) {
            if (!"chunk_x,chunk_z,highest_section_y".equals(reader.readLine()))
                throw new IOException("Invalid height map " + file);
            for (String line; (line = reader.readLine()) != null; ) {
                var cols = line.split(",");
                if (cols.length != 3) throw new IOException("Invalid height row " + line);
                int sectionY = Integer.parseInt(cols[2]);
                int top = StoneLayerField.maxScanY(-64, 320, sectionY + 4);
                result.put(chunkKey(Integer.parseInt(cols[0]), Integer.parseInt(cols[1])), top);
            }
        }
        return result;
    }

    private static long chunkKey(int x, int z) {
        return Integer.toUnsignedLong(x) | ((long) z << 32);
    }

    int index(int x, int y, int z) {
        int top = WorldgenSampler.TOP;
        if (topByChunk != null) {
            var height = topByChunk.get(chunkKey(x >> 4, z >> 4));
            if (height == null)
                throw new IllegalArgumentException("Missing height sample for chunk " + (x >> 4) + "," + (z >> 4));
            top = height;
        }
        if (y < -47 || y > top) return -1;
        int index = field.get(x, y, z, layers.size());
        return y < -7 && layers.get(index).noDeep() ? -1 : index;
    }
}
