package gregtech.content.data;

import gregtech.content.book.BookShelfVariants;
import gregtech.content.storage.BottleCrateVariants;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Emits the 59 original bookshelf recipes from {@link BookShelfVariants#all()}.
 *
 * <p>GT6 registers one bookshelf per plank identity and one per metalset material
 * ({@code Loader_MultiTileEntities}). Wooden shelves are eight planks around saw, file
 * and soft hammer; metal shelves are four plates around two screws plus saw, screwdriver and
 * hammer. Neither is mirror-craftable, and the metal rows address the shared {@code forge:plates}
 * and {@code forge:screws} tags so any mod's plate of that material works.</p>
 */
public final class BookshelfRecipePack {
    private static final String TYPE = "gt6ce:tool_shaped";
    private static final List<String> WOOD_PATTERN = List.of("PPP", "sfr", "PPP");
    private static final List<String> METAL_PATTERN = List.of("PTP", "sdh", "PTP");
    private static final String DEFAULT_WOOD = "oak";
    private static final String PATH_PREFIX = "bookshelf_";
    /** The default shelf keeps the unsuffixed id and is built from oak planks. */
    private static final String DEFAULT_PATH = "bookshelf";

    private BookshelfRecipePack() {}

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        for (BookShelfVariants.Variant variant : BookShelfVariants.all()) {
            String resultId = "gt6ce:" + variant.path();
            Map<String, Object> recipe = variant.metal() ? metal(variant, resultId) : wood(variant, resultId);
            GeneratedRecipeJson.put(generated, "bookshelves/" + variant.path(), recipe);
        }
    }

    private static Map<String, Object> wood(BookShelfVariants.Variant variant, String resultId) {
        var key = new LinkedHashMap<String, Object>();
        key.put("P", GeneratedRecipeJson.item(plank(variant)));
        key.put("s", GeneratedRecipeJson.item("gt6ce:tool_saw"));
        key.put("f", GeneratedRecipeJson.item("gt6ce:tool_file"));
        key.put("r", GeneratedRecipeJson.item("gt6ce:tool_soft_hammer"));
        return GeneratedRecipeJson.shaped(TYPE, WOOD_PATTERN, key, resultId, 1, false);
    }

    private static Map<String, Object> metal(BookShelfVariants.Variant variant, String resultId) {
        String material = MaterialPrefix.camelToSnake(variant.materialName());
        var key = new LinkedHashMap<String, Object>();
        key.put("P", GeneratedRecipeJson.tag("forge:plates/" + material));
        key.put("T", GeneratedRecipeJson.tag("forge:screws/" + material));
        key.put("s", GeneratedRecipeJson.item("gt6ce:tool_saw"));
        key.put("d", GeneratedRecipeJson.item("gt6ce:tool_screwdriver"));
        key.put("h", GeneratedRecipeJson.item("gt6ce:tool_hammer"));
        return GeneratedRecipeJson.shaped(TYPE, METAL_PATTERN, key, resultId, 1, false);
    }

    /** The plank item a wooden shelf is built from, taken from the shared plank identity table. */
    private static String plank(BookShelfVariants.Variant variant) {
        String path = variant.path();
        String suffix = path.equals(DEFAULT_PATH) ? DEFAULT_WOOD : path.substring(PATH_PREFIX.length());
        for (BottleCrateVariants.Wood wood : BottleCrateVariants.WOODS)
            if (wood.suffix().equals(suffix)) return wood.plank();
        throw new IllegalStateException("No plank identity for bookshelf " + path);
    }
}
