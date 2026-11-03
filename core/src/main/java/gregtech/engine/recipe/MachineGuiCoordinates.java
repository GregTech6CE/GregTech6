package gregtech.engine.recipe;

import java.util.Locale;

/** GT6's 176-wide machine panel; shared by the real container and JEI. */
public final class MachineGuiCoordinates {
    public record Point(int x, int y) {}

    private static final int COLUMN_STEP = 18;

    /**
     * First column of each input item row, indexed by the number of item slots. GT6 right-aligns
     * short input rows and hand-writes the last row of the seven to eleven slot cases, so the
     * table is kept explicit instead of being derived.
     */
    private static final int[][] INPUT_ROW_STARTS = {
        {},
        {2},
        {1},
        {0},
        {1, 1},
        {0, 1},
        {0, 0},
        {0, 0, 0},
        {0, 0, 0},
        {0, 0, 0},
        {0, 0, 0, 2},
        {0, 0, 0, 1},
        {0, 0, 0, 0}
    };

    /** Output rows stay left-aligned up to six slots; the seven to twelve slot cases match the input table. */
    private static final int[][] OUTPUT_ROW_STARTS = {
        {},
        {0},
        {0},
        {0},
        {0, 0},
        {0, 0},
        {0, 0},
        {0, 0, 0},
        {0, 0, 0},
        {0, 0, 0},
        {0, 0, 0, 2},
        {0, 0, 0, 1},
        {0, 0, 0, 0}
    };

    /** Slots per item row; four, five, seven, eight, ten and eleven slots leave a short last row. */
    private static final int[][] ITEM_ROW_SIZES = {
        {},
        {1},
        {2},
        {3},
        {2, 2},
        {3, 2},
        {3, 3},
        {3, 3, 1},
        {3, 3, 2},
        {3, 3, 3},
        {3, 3, 3, 1},
        {3, 3, 3, 2},
        {3, 3, 3, 3}
    };

    private MachineGuiCoordinates() {}

    public static Point item(boolean input, int index, int count, int totalFluids) {
        int[][] startsTable = input ? INPUT_ROW_STARTS : OUTPUT_ROW_STARTS;
        int[] starts = count < startsTable.length ? startsTable[count] : startsTable[12];
        int[] sizes = count < ITEM_ROW_SIZES.length ? ITEM_ROW_SIZES[count] : ITEM_ROW_SIZES[12];
        int row = 0;
        int column = index;
        while (row < sizes.length && column >= sizes[row]) {
            column -= sizes[row];
            row++;
        }
        if (row >= sizes.length) {
            row = index / 3;
            column = index % 3;
        }
        int start = starts[Math.min(row, starts.length - 1)];
        return new Point((input ? 17 : 107) + (start + column) * COLUMN_STEP, itemRowY(count, row, totalFluids));
    }

    private static int itemRowY(int count, int row, int totalFluids) {
        if (count <= 3) return totalFluids > 6 ? 7 : 25;
        if (count <= 6) return (totalFluids > 3 ? 7 : 16) + row * COLUMN_STEP;
        return 7 + row * COLUMN_STEP;
    }

    public static Point fluid(boolean input, int index) {
        return new Point(input ? 53 - index % 3 * 18 : 107 + index % 3 * 18, 63 - index / 3 * 18);
    }

    public static String texture(String guiPath) {
        String path = guiPath == null
                ? "default"
                : guiPath.substring(guiPath.lastIndexOf('/') + 1)
                        .replace(".png", "")
                        .toLowerCase(Locale.ROOT);
        // These two legacy port maps have no corresponding original GT6 GUI.
        if (path.equals("assembler") || path.equals("chisel")) path = "default";
        return "textures/gui/recipes/" + path + ".png";
    }
}
