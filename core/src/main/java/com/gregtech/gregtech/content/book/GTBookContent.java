package com.gregtech.gregtech.content.book;

import com.gregtech.gregtech.api.machine.crucible.CrucibleReactions;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialMass;
import com.gregtech.gregtech.api.material.MaterialProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GT6's written books ({@code Loader_Books}, 18 of them carry literal text).
 *
 * <p>GT6 writes them through {@code UT.Books.createWrittenBook(mapping, title, author, stack, pages)},
 * which fills the vanilla written-book NBT — {@code title}, {@code author} and a {@code pages} list,
 * with {@code ¶} turned into a line break and pages of 256 characters or more dropped. The port
 * uses source cover identities with each platform's native written-book data, and
 * only has to carry the text over (generated into {@link GTBooksGen} by
 * {@code tools/transpile_gt6_books.py}).</p>
 *
 * <p>{@code Manual_Elements} and {@code Manual_Alloys} are assembled from the registered elements
 * and playable crucible reactions, as GT6 does at the end of {@code Loader_Books}. The other 18
 * books use literal pages from {@link GTBooksGen}.</p>
 */
public final class GTBookContent {

    /** Page separator inside a generated row (pages may contain any printable character). */
    private static final String PAGE_SEPARATOR = "\u0001";

    private static final Map<String, Book> BOOKS = new LinkedHashMap<>();

    /** GT6's full Blaze title has 35 characters, but vanilla 1.20.1 rejects titles over 32. */
    private static final String BLAZE_VANILLA_TITLE = "Hunting Guide: Blazes and Ghasts";

    private record Book(String name, String title, String author, List<String> pages) {}

    private GTBookContent() {}

    private static void load() {
        if (!BOOKS.isEmpty()) return;
        for (String row : GTBooksGen.ROWS) {
            String[] fields = row.split("\\|", 4);
            if (fields.length != 4) continue;
            List<String> pages = new ArrayList<>();
            Collections.addAll(pages, fields[3].split(PAGE_SEPARATOR, -1));
            BOOKS.put(fields[0], new Book(fields[0], fields[1], fields[2], List.copyOf(pages)));
        }
        GTMaterialRegistry.init();
        Book alloys = alloyBook();
        Book elements = elementBook();
        BOOKS.put(alloys.name(), alloys);
        BOOKS.put(elements.name(), elements);
    }

    /** GT6 Loader_Books:570-582: three instructions, then one page per crucible alloy recipe. */
    private static Book alloyBook() {
        List<String> pages = new ArrayList<>();
        pages.add("This Book Contains Information about every Alloy, which can be created by using a Smelting Crucible.\n===================\nIn order to make an Alloy you need to reach the Melting Point of the Alloy itself.");
        pages.add("And you need to reach the Melting Point of all but one of its Components. You can ofcourse also melt all of the Components, but you are free to 'not melt' one of the Components.");
        pages.add("In case you have to supply Air for the Crucible (for Steel), you need to point an Engine into it, which will act as a Fan. For Multiblock Crucibles the Engine has to be at the Bottom Row.");
        List<CrucibleReactions.Reaction> reactions = new ArrayList<>(CrucibleReactions.allRecipes());
        reactions.sort(Comparator.comparingInt(reaction -> reaction.output().getId()));
        int recipeCount = 0;
        for (CrucibleReactions.Reaction reaction : reactions) {
            GTMaterial alloy = reaction.output();
            if (!alloy.has(MaterialProperty.ALLOY)) continue;
            recipeCount++;
            StringBuilder page = new StringBuilder("Alloy:\n").append(materialName(alloy))
                    .append("\n===================\nMelting: ").append(alloy.getMeltingPoint())
                    .append(" K\nBoiling: ").append(alloy.getBoilingPoint())
                    .append(" K\n===================\nComponents per ").append(reaction.yield()).append('\n');
            for (CrucibleReactions.Part part : reaction.parts()) {
                page.append(part.ratio()).append(' ').append(materialName(part.material())).append('\n');
            }
            // GT6's UT.Books.createWrittenBook discards pages with 256+ source characters.
            if (page.length() < 256) pages.add(page.toString());
        }
        // The original title uses tBook.size()-2: three instructions make this recipeCount+1.
        return new Book("Manual_Alloys", "Book of Alloys, Smeltery Edition (" + (recipeCount + 1) + ")",
                "GMWI (Gregorius Metal Working Industries)", List.copyOf(pages));
    }

    /** GT6 Loader_Books:586-596; visible non-antimatter elements in numeric material-ID order. */
    private static Book elementBook() {
        List<GTMaterial> materials = new ArrayList<>(GTMaterialRegistry.allMaterials());
        materials.sort(Comparator.comparingInt(GTMaterial::getId));
        List<String> pages = new ArrayList<>();
        int count = 0;
        for (GTMaterial material : materials) {
            if (!material.isValid() || !material.has(MaterialProperty.ELEMENT)
                    || material.has(MaterialProperty.HIDDEN) || material.has(MaterialProperty.ANTIMATTER)) continue;
            count++;
            // MT.element/diatomicgas/etc call OreDictMaterial.setStats, whose heat(melt, boil)
            // sets mPlasmaPoint = mBoilingPoint * 100. The explicit three-argument exception is
            // hidden Magic, which is excluded by the original book's filter.
            long plasma = (long) material.getBoilingPoint() * 100L;
            String page = materialName(material) + '\n' + material.getProtons() + '/' + material.getNeutrons()
                    + "\n===================\nID: " + material.getId()
                    + "\nMelting: " + material.getMeltingPoint() + " K\nBoiling: " + material.getBoilingPoint()
                    + " K\nPlasma: " + plasma + " K\n===================\nDensity:\n"
                    + (material.getDensity() == 0 ? "???" : material.getDensity()) + " g/cm3\n"
                    + MaterialMass.kilograms(material, com.gregtech.gregtech.api.material.GTValues.U)
                    + " kg/unit\n===================\n";
            if (page.length() < 256) pages.add(page);
        }
        return new Book("Manual_Elements", "Book of Periods (" + count + ")",
                "GCC (Gregorius Chemical Consortium)", List.copyOf(pages));
    }

    private static String materialName(GTMaterial material) {
        // The generated element holder currently uses chemical symbols as local-name fallbacks.
        // GT6's getLocal() resolves the full translated material name, not that symbol.
        return material.has(MaterialProperty.ELEMENT) ? material.getName() : material.getDisplayNameFallback();
    }

    /** All mappings, in generation order. */
    public static List<String> names() {
        load();
        return List.copyOf(BOOKS.keySet());
    }

    public static String titleOf(String mapping) {
        load();
        Book book = BOOKS.get(mapping);
        return book == null ? null : book.title();
    }

    public static String authorOf(String mapping) {
        load();
        Book book = BOOKS.get(mapping);
        return book == null ? null : book.author();
    }

    public static List<String> pagesOf(String mapping) {
        load();
        Book book = BOOKS.get(mapping);
        return book == null ? List.of() : book.pages();
    }

    public static String vanillaTitleOf(String mapping){
        load();Book book=BOOKS.get(mapping);if(book==null)return null;
        String title=book.name().equals("Manual_Hunting_Blaze")?BLAZE_VANILLA_TITLE:book.name().equals("Manual_Alloys")?"Book of Alloys "+book.title().substring(book.title().lastIndexOf('(')):book.title();
        if(title.length()>32)throw new IllegalStateException("Book title exceeds vanilla's 32-character limit: "+mapping);
        return title;
    }
}
