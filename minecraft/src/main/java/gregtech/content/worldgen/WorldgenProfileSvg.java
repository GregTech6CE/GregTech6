package gregtech.content.worldgen;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.core.Direction.Axis;

/** Writes one SVG rectangle for each contiguous height range in the profile. */
final class WorldgenProfileSvg {
    private static final int LEFT = 56;
    private static final int TOP = 56;
    private static final int GAP = 16;
    private static final String MARKER_COLOR = "#51ead1";
    private static final Pattern INVALID_XML = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\uFFFE\\uFFFF]");

    private final Writer output;
    private final WorldgenProfile.Section section;
    private final int columnWidth;
    private final int storedX;
    private final int plotWidth;

    WorldgenProfileSvg(Writer output, WorldgenProfile.Section section, boolean planned) throws IOException {
        this.output = output;
        this.section = section;
        columnWidth = Math.max(1, 168 / section.width());
        plotWidth = section.width() * columnWidth;
        storedX = planned ? LEFT + plotWidth + GAP : LEFT;
        int imageWidth = Math.max(360, storedX + plotWidth + GAP);
        int imageHeight = TOP + section.height() + 38;
        output.write(String.format(Locale.ROOT, """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" width="%d" height="%d">
                <title>GT6CE world profile</title>
                <rect width="100%%" height="100%%" fill="#101720"/>
                <g fill="#e1eaf2" font-family="sans-serif" font-size="12">
                """, imageWidth, imageHeight, imageWidth, imageHeight));
        boolean alongX = section.axis() == Axis.X;
        int first = alongX ? section.x() : section.z();
        int last = first + section.width() - 1;
        String range = alongX
                ? "X %d to %d | Z %d".formatted(first, last, section.z())
                : "Z %d to %d | X %d".formatted(first, last, section.x());
        text(LEFT, 22, range);
        if (planned) text(LEFT, TOP - 8, "Planned strata");
        text(storedX, TOP - 8, "Stored blocks");
        output.write("</g>\n");
    }

    void layer(int col, int from, int to, String color, String title) throws IOException {
        rectangle(LEFT, col, from, to, color, title);
    }

    void block(int col, int from, int to, String color, String title, boolean marker) throws IOException {
        rectangle(storedX, col, from, to, marker ? MARKER_COLOR : color, title);
    }

    private void rectangle(int panelX, int column, int from, int to, String color, String title) throws IOException {
        output.write(String.format(
                Locale.ROOT,
                """
                <rect x="%d" y="%d" width="%d" height="%d" fill="%s" shape-rendering="crispEdges"><title>%s</title></rect>
                """,
                panelX + column * columnWidth,
                row(to),
                columnWidth,
                to - from + 1,
                color,
                xml(title)));
    }

    void finish() throws IOException {
        output.write("<g fill=\"#e1eaf2\" font-family=\"sans-serif\" font-size=\"11\">\n");
        int step = Math.max(32, ((section.height() + 255) / 256) * 32);
        for (int y = Math.floorDiv(section.maxY(), step) * step; y >= section.minY(); y -= step) {
            int row = row(y);
            output.write(String.format(Locale.ROOT, """
                    <path d="M%d %d H%d" fill="none" stroke="#ffffff" stroke-opacity="0.24"/>
                    """, LEFT - 6, row, storedX + plotWidth));
            text(8, row + 4, "Y " + y);
        }
        int legendY = TOP + section.height() + 22;
        output.write(String.format(Locale.ROOT, """
                <rect x="%d" y="%d" width="10" height="10" fill="%s"/>
                """, LEFT, legendY - 9, MARKER_COLOR));
        text(LEFT + 16, legendY, "Ground marker");
        output.write("</g>\n</svg>\n");
    }

    private void text(int x, int y, String value) throws IOException {
        output.write(String.format(Locale.ROOT, "<text x=\"%d\" y=\"%d\">%s</text>%n", x, y, xml(value)));
    }

    private int row(int y) {
        return TOP + section.maxY() - y;
    }

    private static String xml(String value) {
        return INVALID_XML
                .matcher(value)
                .replaceAll("")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
