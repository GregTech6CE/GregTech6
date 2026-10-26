package gregtech.content.recipe;

import gregtech.engine.machine.crucible.MoldShapes;
import java.util.LinkedHashMap;
import java.util.List;

/** One example per actual casting result; rotations, mirrors and materials do not add pages. */
public final class MoldChiselPatterns {
    public record Pattern(String id, int shape, MoldShapes.Recipe recipe) {
        public boolean carved(int column, int row) {
            return column >= 0 && column < 5 && row >= 0 && row < 5
                    && (shape & (1 << (row * 5 + column))) != 0;
        }
    }

    private MoldChiselPatterns() {}

    public static List<Pattern> all() {
        var patterns = new LinkedHashMap<String, Pattern>();
        // Prefer the source's existing centered clay-mold examples to arbitrary rotated bitmasks.
        for (var raw : ClayMoldCatalog.RAW) if (raw.shape() != 0) add(patterns, raw.shape());
        // Include any additional shapes in the live recognition table, in deterministic order.
        MoldShapes.recipes().keySet().stream().sorted().forEach(shape -> add(patterns, shape));
        return List.copyOf(patterns.values());
    }

    private static void add(LinkedHashMap<String, Pattern> patterns, int shape) {
        var recipe = MoldShapes.recipe(shape);
        if (recipe == null) return;
        String id = MoldShapes.sourcePrefixName(recipe);
        patterns.putIfAbsent(id, new Pattern(id, shape, recipe));
    }
}
