package gregtech.content.recipe;

import gregtech.content.data.MaterialPrefix;
import java.util.HashSet;
import java.util.List;

/** An original wood recipe shared by both platform registration paths. */
public record WoodCraftingRow(String path, Source source, Shape shape, ItemSpec output, int count) {
    public WoodCraftingRow {
        if (count < 1) throw new IllegalArgumentException("Invalid wood output count for " + path);
    }

    public record Source(String file, int line) {
        public static Source woods(int line) {
            return new Source("Loader_Recipes_Woods.java", line);
        }

        @Override
        public String toString() {
            return file + ":" + line;
        }
    }

    public sealed interface IngredientSpec permits ItemSpec, Tag {}

    public sealed interface ItemSpec extends IngredientSpec permits Item, MaterialForm, Tool, WoodBlock {}

    public record Item(String id) implements ItemSpec {}

    public record Tag(String id) implements IngredientSpec {}

    public record MaterialForm(MaterialPrefix prefix, String material) implements ItemSpec {}

    public enum Tool implements ItemSpec {
        SAW,
        FILE
    }

    public enum WoodForm {
        LOG,
        PLANKS,
        STRIPPED
    }

    public record WoodBlock(WoodForm form, String species) implements ItemSpec {}

    public sealed interface Shape permits Shaped, Shapeless {}

    public record Key(char symbol, IngredientSpec ingredient) {}

    public record Shaped(List<String> pattern, List<Key> keys, boolean allowMirror) implements Shape {
        public Shaped {
            pattern = List.copyOf(pattern);
            keys = List.copyOf(keys);
            if (pattern.isEmpty() || pattern.get(0).isEmpty())
                throw new IllegalArgumentException("Missing wood crafting pattern");
            int width = pattern.get(0).length();
            var symbols = new HashSet<Character>();
            for (var key : keys) {
                if (key.symbol() == ' ' || !symbols.add(key.symbol()))
                    throw new IllegalArgumentException("Invalid wood crafting key " + key.symbol());
            }
            for (String row : pattern) {
                if (row.length() != width)
                    throw new IllegalArgumentException("Uneven wood crafting pattern " + pattern);
                for (int x = 0; x < width; x++) {
                    char symbol = row.charAt(x);
                    if (symbol != ' ' && !symbols.contains(symbol))
                        throw new IllegalArgumentException("Missing wood crafting key " + symbol);
                }
            }
        }
    }

    public record Shapeless(List<IngredientSpec> ingredients) implements Shape {
        public Shapeless {
            ingredients = List.copyOf(ingredients);
            if (ingredients.isEmpty()) throw new IllegalArgumentException("Missing shapeless wood ingredients");
        }
    }

    static WoodCraftingRow shaped(
            String path, Source source, ItemSpec output, int count, List<String> pattern, Key... keys) {
        return new WoodCraftingRow(path, source, new Shaped(pattern, List.of(keys), false), output, count);
    }

    static WoodCraftingRow shapeless(
            String path, Source source, ItemSpec output, int count, IngredientSpec ingredient) {
        return new WoodCraftingRow(path, source, new Shapeless(List.of(ingredient)), output, count);
    }
}
