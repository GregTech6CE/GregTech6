package gregtech.content.recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool-shaped recipes that cut the extruder, food and slicer molds out of each other.
 *
 * <p>Both extruder families publish one shared 31-shape table: every shape is cut with the wire
 * cutter out of another mold of the same family, so the family prefix alone carries the dependency
 * chain from the empty mold down to the tool heads. Only the empty mold is filed from a double
 * plate, and that plate is the single row that differs between the two families. Food molds repeat
 * the same slot template with the hammer; the slicer shapes are the only rows with their own
 * patterns.
 */
public final class ExtruderShapeRecipeRules {
    private ExtruderShapeRecipeRules() {}

    /** Recipe type shared by every row. */
    public static final String TYPE = "gt6ce:tool_shaped";
    /** Recipe folder shared by all four families. */
    public static final String DIRECTORY = "extruder_shapes";

    /** One ingredient of a shaped recipe: {@code item:<id>} or {@code tag:<id>}. */
    public record Key(char symbol, String spec) {}

    /** One published recipe; {@code path} is relative to {@link #DIRECTORY} and also names the result. */
    public record Recipe(String path, List<String> pattern, List<Key> key) {}

    /** Wire cutter or hammer position around the center mold cell. */
    public enum Slot {
        TOP_LEFT(0, 0),
        TOP_CENTER(0, 1),
        TOP_RIGHT(0, 2),
        MIDDLE_RIGHT(1, 2),
        BOTTOM_RIGHT(2, 2),
        BOTTOM_CENTER(2, 1);

        private final int row;
        private final int column;

        Slot(int row, int column) {
            this.row = row;
            this.column = column;
        }

        /** Three rows with the base mold at the center and {@code symbol} at this slot. */
        public List<String> pattern(char symbol) {
            var rows = new char[][] {{' ', ' ', ' '}, {' ', 'P', ' '}, {' ', ' ', ' '}};
            rows[row][column] = symbol;
            return List.of(new String(rows[0]), new String(rows[1]), new String(rows[2]));
        }
    }

    /** One mold family: the recipe prefix, the plate its empty mold is filed from and its cutting tool. */
    public record Family(String prefix, String emptyPlate, Key tool) {}

    /** A shape cut out of {@code base} (a mold name without the family prefix) at {@code slot}. */
    public record Shape(String name, String base, Slot slot) {}

    /** A slicer plate; {@code hollow} adds the stainless ring that the hollow variants need. */
    public record SlicerShape(String name, List<String> pattern, boolean hollow) {}

    private static final Key CUTTER = new Key('x', "item:gt6ce:tool_wire_cutter");
    private static final Key HAMMER = new Key('h', "item:gt6ce:tool_hammer");
    private static final Key FILE = new Key('f', "item:gt6ce:tool_file");
    private static final Key SAW = new Key('s', "item:gt6ce:tool_saw");
    private static final Key TINY_STAINLESS_PLATE = new Key('B', "item:gt6ce:plate_tiny_stainlesssteel");
    private static final Key SLICER_EMPTY = new Key('O', "item:gt6ce:slicer_shape_empty");
    private static final Key STAINLESS_ROD = new Key('R', "tag:forge:rods/stainless_steel");
    private static final Key STAINLESS_RING = new Key('R', "tag:forge:rings/stainless_steel");

    /** The empty mold itself: hammer and file above the plate, wire cutter beside it. */
    private static final List<String> EMPTY_PATTERN = List.of("hf", "xP");

    /** The empty slicer shape is hammered around a stainless rod frame instead of a plate. */
    private static final List<String> SLICER_EMPTY_PATTERN = List.of(" R ", "RhR", " R ");

    public static final List<Family> EXTRUDER_FAMILIES = List.of(
            new Family("extruder_shape_", "gt6ce:plate_double_tungstencarbide", CUTTER),
            new Family("low_heat_extruder_shape_", "gt6ce:plate_double_steel", CUTTER));

    public static final Family FOODMOLD = new Family("foodmold_shape_", "gt6ce:plate_double_stainlesssteel", HAMMER);

    /** The 31 shapes below the empty mold, grouped by the mold they are cut from. */
    public static final List<Shape> EXTRUDER_SHAPES = List.of(
            // Cut from the empty mold.
            new Shape("curvedplate", "empty", Slot.TOP_RIGHT),
            new Shape("foil", "empty", Slot.BOTTOM_RIGHT),
            new Shape("ingot", "empty", Slot.TOP_LEFT),
            new Shape("ring", "empty", Slot.BOTTOM_CENTER),
            new Shape("rod", "empty", Slot.MIDDLE_RIGHT),
            new Shape("tinyplate", "empty", Slot.TOP_CENTER),
            // Cut from the tiny plate.
            new Shape("axehead", "tinyplate", Slot.TOP_LEFT),
            new Shape("filehead", "tinyplate", Slot.TOP_RIGHT),
            new Shape("sawblade", "tinyplate", Slot.BOTTOM_RIGHT),
            new Shape("shovelhead", "tinyplate", Slot.TOP_CENTER),
            new Shape("swordblade", "tinyplate", Slot.MIDDLE_RIGHT),
            // Cut from the ingot mold.
            new Shape("block", "ingot", Slot.TOP_LEFT),
            new Shape("hammerhead", "ingot", Slot.TOP_RIGHT),
            new Shape("hoehead", "ingot", Slot.MIDDLE_RIGHT),
            new Shape("pickaxehead", "ingot", Slot.TOP_CENTER),
            // Cut from the ring mold.
            new Shape("bottle", "ring", Slot.TOP_RIGHT),
            new Shape("capsulecellcontainer", "ring", Slot.BOTTOM_RIGHT),
            new Shape("cell", "ring", Slot.MIDDLE_RIGHT),
            new Shape("gear", "ring", Slot.TOP_LEFT),
            new Shape("smallgear", "ring", Slot.TOP_CENTER),
            // Cut from the rod mold.
            new Shape("bolt", "rod", Slot.TOP_LEFT),
            new Shape("finewire", "rod", Slot.MIDDLE_RIGHT),
            new Shape("longrod", "rod", Slot.TOP_RIGHT),
            new Shape("wire", "rod", Slot.TOP_CENTER),
            // Cut from the foil.
            new Shape("casing", "foil", Slot.TOP_CENTER),
            new Shape("plate", "foil", Slot.TOP_LEFT),
            // Cut from the curved plate.
            new Shape("hugepipe", "curvedplate", Slot.BOTTOM_RIGHT),
            new Shape("largepipe", "curvedplate", Slot.MIDDLE_RIGHT),
            new Shape("mediumpipe", "curvedplate", Slot.TOP_RIGHT),
            new Shape("smallpipe", "curvedplate", Slot.TOP_CENTER),
            new Shape("tinypipe", "curvedplate", Slot.TOP_LEFT));

    /** The five food molds hammered out of the empty food mold. */
    public static final List<Shape> FOODMOLD_SHAPES = List.of(
            new Shape("bun", "empty", Slot.TOP_LEFT),
            new Shape("bread", "empty", Slot.TOP_CENTER),
            new Shape("baguette", "empty", Slot.TOP_RIGHT),
            new Shape("cylinder", "empty", Slot.MIDDLE_RIGHT),
            new Shape("toast", "empty", Slot.BOTTOM_RIGHT));

    /** The seven slicer plates cut from the empty slicer shape. */
    public static final List<SlicerShape> SLICER_SHAPES = List.of(
            new SlicerShape("eights", List.of("B B", "s f", "BOB"), false),
            new SlicerShape("eights_hollow", List.of("B B", "sRf", "BOB"), true),
            new SlicerShape("flat", List.of("B f", "BO ", "B s"), false),
            new SlicerShape("grid", List.of(" Bf", "BOB", " Bs"), false),
            new SlicerShape("quaters", List.of("fB ", "B s", " O "), false),
            new SlicerShape("quaters_hollow", List.of("fB ", "BRs", " O "), true),
            new SlicerShape("split", List.of(" Of", "BBB", "  s"), false));

    /** All 78 rows: both extruder families, the food molds and the slicer shapes. */
    public static List<Recipe> recipes() {
        var recipes = new ArrayList<Recipe>(78);
        for (var family : EXTRUDER_FAMILIES) addFamily(recipes, family, EXTRUDER_SHAPES);
        addFamily(recipes, FOODMOLD, FOODMOLD_SHAPES);
        recipes.add(slicerEmpty());
        for (var shape : SLICER_SHAPES) recipes.add(slicer(shape));
        return List.copyOf(recipes);
    }

    private static void addFamily(List<Recipe> recipes, Family family, List<Shape> shapes) {
        recipes.add(emptyMold(family));
        for (var shape : shapes) recipes.add(cut(family, shape));
    }

    private static Recipe emptyMold(Family family) {
        String name = family.prefix() + "empty";
        return new Recipe(
                name, EMPTY_PATTERN, List.of(new Key('P', "item:" + family.emptyPlate()), HAMMER, FILE, CUTTER));
    }

    private static Recipe cut(Family family, Shape shape) {
        String name = family.prefix() + shape.name();
        var base = new Key('P', "item:gt6ce:" + family.prefix() + shape.base());
        return new Recipe(name, shape.slot().pattern(family.tool().symbol()), List.of(base, family.tool()));
    }

    private static Recipe slicerEmpty() {
        return new Recipe("slicer_shape_empty", SLICER_EMPTY_PATTERN, List.of(STAINLESS_ROD, HAMMER));
    }

    private static Recipe slicer(SlicerShape shape) {
        var keys = new ArrayList<Key>(5);
        keys.add(TINY_STAINLESS_PLATE);
        keys.add(SLICER_EMPTY);
        keys.add(FILE);
        keys.add(SAW);
        if (shape.hollow()) keys.add(STAINLESS_RING);
        return new Recipe("slicer_shape_" + shape.name(), shape.pattern(), List.copyOf(keys));
    }
}
