package gregtech.content.data;

import gregtech.content.book.ColoredBookRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Emits colored book binding, recoloring and copying recipes from {@link ColoredBookRules}. */
public final class BookRecipePack {
    private static final String GROUP_BINDING = "gt6ce.books.binding";
    private static final String GROUP_COPY = "gt6ce.books.copy";
    private static final String CATEGORY = "misc";

    private BookRecipePack() {}

    public static void contribute(Map<ResourceLocation, byte[]> generated) {
        for (var variant : ColoredBookRules.VARIANTS) {
            String bookId = "gt6ce:" + variant.path();
            if (!variant.hidden()) {
                emitDyed(generated, variant, bookId);
            }
            for (int count = 1; count <= 8; count++) {
                List<Object> ingredients = new ArrayList<>(count + 1);
                ingredients.add(GeneratedRecipeJson.item(bookId));
                for (int i = 0; i < count; i++) ingredients.add(GeneratedRecipeJson.item("minecraft:writable_book"));
                GeneratedRecipeJson.put(
                        generated,
                        "books/copy/" + variant.path() + "/" + count,
                        GeneratedRecipeJson.shapeless(
                                "gt6ce:book_binding", ingredients, bookId, count, GROUP_COPY, CATEGORY, null));
            }
        }
    }

    private static void emitDyed(
            Map<ResourceLocation, byte[]> generated, ColoredBookRules.Variant variant, String bookId) {
        var leather = GeneratedRecipeJson.tag("gt6ce:book_binding/leather");
        var dye = GeneratedRecipeJson.tag("gt6ce:book_binding/dyes/" + variant.color());
        var paper = GeneratedRecipeJson.tag("gt6ce:book_binding/paper");

        List<Object> empty = new ArrayList<>();
        int paperCount = variant.large() ? 6 : 3;
        for (int i = 0; i < paperCount; i++) empty.add(paper);
        empty.add(leather);
        empty.add(dye);
        GeneratedRecipeJson.put(
                generated,
                "books/empty/" + variant.path(),
                GeneratedRecipeJson.shapeless(
                        "minecraft:crafting_shapeless", empty, bookId, 1, GROUP_BINDING, CATEGORY, null));

        String pagesItem = variant.large() ? "gt6ce:many_printed_pages" : "gt6ce:printed_pages";
        GeneratedRecipeJson.put(
                generated,
                "books/printed/" + variant.path(),
                GeneratedRecipeJson.shapeless(
                        "gt6ce:book_binding",
                        List.of(GeneratedRecipeJson.item(pagesItem), leather, dye),
                        bookId,
                        1,
                        GROUP_BINDING,
                        CATEGORY,
                        null));

        String recolorTag = variant.large() ? "gt6ce:written_books_large" : "gt6ce:written_books_small";
        GeneratedRecipeJson.put(
                generated,
                "books/recolor/" + variant.path(),
                GeneratedRecipeJson.shapeless(
                        "gt6ce:book_binding",
                        List.of(GeneratedRecipeJson.tag(recolorTag), dye),
                        bookId,
                        1,
                        GROUP_BINDING,
                        CATEGORY,
                        null));
    }
}
