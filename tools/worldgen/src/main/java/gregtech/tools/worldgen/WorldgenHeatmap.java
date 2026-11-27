package gregtech.tools.worldgen;

import gregtech.tools.worldgen.WorldgenComparison.Region;
import gregtech.tools.worldgen.WorldgenComparison.Sample;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;

/** Draws matched chunk counts with the same color scale for each pair of maps. */
final class WorldgenHeatmap {
    private static final int MARGIN = 20;
    private static final int GAP = 24;
    private static final int FIRST_MAP_Y = 104;
    private static final String EMPTY_COLOR = "#292a32";
    private static final HexFormat HEX = HexFormat.of();

    private enum Form {
        FULL("Full ore"),
        POOR("Small ore"),
        BEDROCK("Bedrock ore"),
        CLUE("Surface raw ore clues");

        private final String label;

        Form(String label) {
            this.label = label;
        }
    }

    private final long[][] counts;

    WorldgenHeatmap(int chunks) {
        counts = new long[Form.values().length][chunks];
    }

    void add(int chunk, long full, long poor, long bedrock, long pebbles) {
        add(Form.FULL, chunk, full);
        add(Form.POOR, chunk, poor);
        add(Form.BEDROCK, chunk, bedrock);
        add(Form.CLUE, chunk, pebbles);
    }

    private void add(Form form, int chunk, long amount) {
        var values = counts[form.ordinal()];
        values[chunk] = Math.addExact(values[chunk], amount);
    }

    void writeSingle(Path output, Region region, String mineral, int issues) throws IOException {
        int cell = Math.max(1, 288 / region.side()), panel = region.side() * cell;
        int width = 2 * panel + 3 * MARGIN, height = 2 * panel + 130;
        try (var svg = Files.newBufferedWriter(output)) {
            svg.write("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + width + "\" height=\"" + height + "\">");
            rect(svg, 0, 0, width, height, "#17191e");
            svg.write("<g font-family=\"sans-serif\" fill=\"#e8ebf1\">");
            String material = "*".equals(mineral) ? "All minerals" : mineral;
            text(svg, MARGIN, 25, 18, material + " · " + region.dimension());
            text(
                    svg,
                    MARGIN,
                    47,
                    12,
                    "Origin chunk (" + region.originX() + ", " + region.originZ() + ") · "
                            + (issues == 0
                                    ? "Complete sample"
                                    : "Incomplete: " + issues + " skipped entries (see skipped.csv)"));
            for (var form : Form.values()) {
                int x = MARGIN + (form.ordinal() % 2) * (panel + MARGIN);
                int y = 83 + (form.ordinal() / 2) * (panel + 33);
                var values = counts[form.ordinal()];
                long max = Arrays.stream(values).max().orElse(0);
                text(svg, x, y - 5, 12, form.label + " · max " + max);
                drawPanel(svg, region, x, y, cell, values, max);
            }
            svg.write("</g></svg>\n");
        }
    }

    static void write(Path output, String material, Sample baseline, Sample candidate) throws IOException {
        var region = baseline.region();
        int cell = Math.max(1, 288 / region.side());
        int panel = region.side() * cell;
        int candidateX = MARGIN + panel + GAP;
        int width = candidateX + panel + MARGIN;
        int rowHeight = panel + 45;
        int height = FIRST_MAP_Y + rowHeight * Form.values().length;
        String name = material.equals("*") ? "All minerals" : material;
        String title = name + " · Mineral counts by chunk";
        try (var svg = Files.newBufferedWriter(output)) {
            svg.write("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">\n"
                    .formatted(width, height, width, height));
            svg.write("<title>" + escape(title) + "</title>\n");
            svg.write("<defs><linearGradient id=\"density\">\n");
            for (int i = 0; i <= 4; i++) {
                svg.write("<stop offset=\"%d%%\" stop-color=\"%s\"/>\n".formatted(25 * i, color(i / 4.0)));
            }
            svg.write("</linearGradient></defs>\n");
            rect(svg, 0, 0, width, height, "#17191e");
            svg.write("<g font-family=\"sans-serif\" fill=\"#e8ebf1\">\n");
            text(svg, MARGIN, 28, 18, title);
            text(
                    svg,
                    MARGIN,
                    49,
                    12,
                    "Seed %d · %s · Origin chunk (%d, %d) · %d × %d"
                            .formatted(
                                    region.seed(),
                                    region.dimension(),
                                    region.originX(),
                                    region.originZ(),
                                    region.side(),
                                    region.side()));
            text(svg, MARGIN, 68, 12, "X increases →    Z increases ↓    Dark cells are empty");
            text(svg, MARGIN, 88, 13, "Baseline " + baseline.revision());
            text(svg, candidateX, 88, 13, "Candidate " + candidate.revision());
            for (var form : Form.values()) {
                int y = FIRST_MAP_Y + form.ordinal() * rowHeight;
                long[] before = baseline.heatmap().counts[form.ordinal()];
                long[] after = candidate.heatmap().counts[form.ordinal()];
                long max = Math.max(
                        Arrays.stream(before).max().orElse(0),
                        Arrays.stream(after).max().orElse(0));
                drawPanel(svg, region, MARGIN, y, cell, before, max);
                drawPanel(svg, region, candidateX, y, cell, after, max);
                text(svg, MARGIN, y + panel + 20, 13, form.label);
                if (max > 1) {
                    rect(svg, candidateX, y + panel + 10, 72, 10, "url(#density)");
                    text(svg, candidateX + 82, y + panel + 20, 12, "1–" + max + " per chunk · log scale");
                } else {
                    text(svg, candidateX, y + panel + 20, 12, max == 0 ? "No matches" : "1 per chunk");
                }
            }
            svg.write("</g></svg>\n");
        }
    }

    private static void drawPanel(BufferedWriter svg, Region region, int x, int y, int cell, long[] values, long max)
            throws IOException {
        int side = region.side();
        rect(svg, x, y, cell * side, cell * side, EMPTY_COLOR);
        double logMax = Math.log(max);
        for (int index = 0; index < values.length; index++) {
            long count = values[index];
            if (count == 0) continue;
            int dx = index / side;
            int dz = index % side;
            svg.write("<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" fill=\"%s\">"
                    .formatted(
                            x + dx * cell, y + dz * cell, cell, cell, color(max == 1 ? 1 : Math.log(count) / logMax)));
            svg.write("<title>Chunk (%d, %d) · %d</title></rect>\n"
                    .formatted((long) region.originX() + dx, (long) region.originZ() + dz, count));
        }
    }

    private static void rect(BufferedWriter svg, int x, int y, int width, int height, String color) throws IOException {
        svg.write("<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" fill=\"%s\"/>\n"
                .formatted(x, y, width, height, color));
    }

    private static void text(BufferedWriter svg, int x, int y, int size, String label) throws IOException {
        svg.write("<text x=\"%d\" y=\"%d\" font-size=\"%d\">%s</text>\n".formatted(x, y, size, escape(label)));
    }

    private static String color(double fraction) {
        int rgb = Color.HSBtoRGB((float) (0.61 - 0.57 * fraction), 0.9f, 0.86f);
        return "#" + HEX.toHexDigits(rgb, 6);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
