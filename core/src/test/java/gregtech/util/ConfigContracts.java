package gregtech.util;

import gregtech.content.worldgen.dungeon.DungeonLayoutRules;
import java.util.Arrays;
import java.util.Random;

public final class ConfigContracts {
    public static void main(String[] args) {
        String input = "# Header\n[general]\n# First\nenabled = true\n# Second\nvalues = [\n  \"one\",\n  \"two\"\n]\ncount = 3\n";
        String spaced = ConfigSpacing.format(input);
        require(spaced.contains("enabled = true\n\n# Second"), "setting spacing");
        require(spaced.contains("]\n\ncount = 3"), "array spacing");
        require(spaced.contains("values = [\n  \"one\",\n  \"two\"\n]"), "array preservation");
        require(ConfigSpacing.format(spaced).equals(spaced), "format is idempotent");
        String crlf = input.replace("\n", "\r\n");
        require(ConfigSpacing.format(crlf).equals(spaced.replace("\n", "\r\n")), "Windows newlines");
        for (int seed = 0; seed < 40; seed++) {
            require(Arrays.deepEquals(DungeonLayoutRules.layout(new Random(seed)::nextInt),
                    DungeonLayoutRules.layout(new Random(seed)::nextInt, DungeonLayoutRules.DEFAULTS)), "default layout unchanged");
        }
        for (int max : new int[]{3, 4, 7, 12, 16}) {
            var settings = new DungeonLayoutRules.Settings(3, max, -40, 20, 6);
            for (int region = -30; region <= 30; region++) {
                int anchor = DungeonLayoutRules.anchorOfRegion(region, settings);
                require(DungeonLayoutRules.isAnchor(anchor, settings), "locate anchor");
                require(DungeonLayoutRules.regionOf(anchor, settings) == region, "locate region");
                require(DungeonLayoutRules.nearestAnchor(anchor, settings) == anchor, "feature anchor");
            }
            for (int seed = 0; seed < 40; seed++) {
                byte[][] cells = DungeonLayoutRules.layout(new Random(seed)::nextInt, settings);
                require(cells.length >= 5 && cells.length <= max + 2, "configured size");
                int barracks = 0, entrance = 0;
                for (byte[] row : cells) for (byte cell : row) {
                    if (cell == DungeonLayoutRules.BARRACKS) barracks++;
                    if (cell == DungeonLayoutRules.ENTRANCE) entrance++;
                }
                require(barracks == 1 && entrance == 1, "important rooms survive");
            }
        }
        System.out.println("Config contracts passed: spacing, preservation, configured dungeon lattice and layouts");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
