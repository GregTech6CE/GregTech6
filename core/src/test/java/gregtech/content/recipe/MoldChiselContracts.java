package gregtech.content.recipe;

import gregtech.engine.machine.crucible.MoldShapes;
import java.util.HashSet;

/** Viewer examples must be recognizable in the world and cover each result exactly once. */
public final class MoldChiselContracts {
    public static void main(String[] args) {
        var patterns = MoldChiselPatterns.all();
        var expected = new HashSet<String>();
        for (var recipe : MoldShapes.recipes().values()) expected.add(MoldShapes.sourcePrefixName(recipe));
        expected.add("nugget");
        var actual = new HashSet<String>();
        for (var pattern : patterns) {
            require(actual.add(pattern.id()), "Duplicate page: " + pattern.id());
            require(pattern.recipe().equals(MoldShapes.recipe(pattern.shape())), "Unrecognized example");
            int rebuilt = 0;
            for (int row = 0; row < 5; row++) for (int col = 0; col < 5; col++)
                if (pattern.carved(col, row)) rebuilt |= 1 << (row * 5 + col);
            require(rebuilt == pattern.shape(), "Grid orientation/bit loss");
            require(!pattern.carved(5, 0) && !pattern.carved(-1, 0), "Out of bounds cell");
        }
        require(actual.equals(expected), "Missing mold outputs: " + expected);
        require(patterns.equals(MoldChiselPatterns.all()), "Unstable order");
        for (String id : new String[]{"stickLong", "gearGt", "gearGtSmall"})
            require(actual.contains(id), "Missing requested example: " + id);
        System.out.println("Mold chiseling contracts passed: " + patterns.size() + " unique casting forms.");
    }

    private static void require(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
    }
}
