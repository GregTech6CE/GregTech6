package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.content.book.GTMaterialDictionary;
import com.gregtech.gregtech.content.loot.GTLootTables;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Guards GT6's material dictionaries ({@code MultiItemBooks:59-60} metas 32002/32003, loot variant
 * {@code 32766} "Dusty Material Dictionary").
 *
 * <p>GT6 builds one dictionary per material from the material's own data
 * ({@code UT.Books.addMaterialDictionary}) and lists them all in the {@code gt.matdicts} table
 * ({@code Loader_Loot:359-364}), which the Dusty Material Dictionary rolls. The port generates the
 * pages from its material data ({@link GTMaterialDictionary}) and builds the table from the
 * registered materials, so these tests pin both: a dictionary has readable pages about its material,
 * the table has one row per material, and the loot item really rolls it.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class MaterialDictionaryTests {

    /** Every dictionary has pages, and they talk about its own material. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void dictionariesDescribeTheirMaterial(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (GTMaterial material : GTMaterialDictionary.materials()) {
            ItemStack book = GTMaterialDictionary.bookStack(material);
            if (book.isEmpty()) {
                problems.add(material.getName() + ": no book");
                continue;
            }
            var pages = GTMaterialDictionary.pages(material);
            if (pages.isEmpty()) {
                problems.add(material.getName() + ": no pages");
                continue;
            }
            boolean mentions = false;
            for (String page : pages) {
                if (page.length() >= 256) problems.add(material.getName() + ": page too long");
                if (page.contains(material.getName())) mentions = true;
            }
            if (!mentions) problems.add(material.getName() + ": pages never name the material");
            // The identity page is GT6's first page: name, melting point and state.
            if (!pages.get(0).contains("Melting point") || !pages.get(0).contains("Material: ")) {
                problems.add(material.getName() + ": identity page is incomplete");
            }
            if (GTMaterialDictionary.materialOf(book) != material) {
                problems.add(material.getName() + ": the book does not carry its material");
            }
            checked++;
        }
        helper.assertTrue(problems.isEmpty(), "material dictionaries, problems (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.assertTrue(checked >= 900, "materials with a dictionary: " + checked);
        helper.succeed();
    }

    /** {@code gt.matdicts} holds one row per material, like GT6's loop over the material array. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void matdictsTableHasOneRowPerMaterial(GameTestHelper helper) {
        GTLootTables.load();
        List<GTLootTables.Row> rows = GTLootTables.rows("gt.matdicts");
        helper.assertTrue(!rows.isEmpty(), "gt.matdicts is built from the registered materials");
        helper.assertTrue(rows.size() == GTMaterialDictionary.materials().size(),
                "one row per material: " + rows.size() + " rows, "
                        + GTMaterialDictionary.materials().size() + " materials");
        for (GTLootTables.Row row : rows) {
            helper.assertTrue(row.weight() == 144 && row.min() == 1 && row.max() == 1,
                    "GT6's weight and stack range (Loader_Loot:364), got "
                            + row.weight() + "/" + row.min() + "-" + row.max());
            helper.assertTrue(!row.stack().isEmpty(), "row resolves: " + row.spec());
            break;
        }
        int[] counts = GTLootTables.countRange("gt.matdicts");
        helper.assertTrue(counts[0] == 8 && counts[1] == 24,
                "GT6 sets 8..24 draws for gt.matdicts, got " + counts[0] + ".." + counts[1]);
        helper.assertTrue(GTLootTables.unresolved().stream().noneMatch(s -> s.startsWith("gt.matdicts")),
                "gt.matdicts has unresolved rows: " + GTLootTables.unresolved());
        helper.succeed();
    }

    /** The Dusty Material Dictionary rolls that table, and a roll hands out a dictionary. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void dustyDictionaryRollsDictionaries(GameTestHelper helper) {
        var item = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", "dusty_material_dictionary"));
        helper.assertTrue(item != null, "the dusty material dictionary is registered");
        helper.assertTrue(item instanceof com.gregtech.gregtech.content.loot.LootBagItem bag
                        && bag.tables().length == 1 && bag.tables()[0].equals("gt.matdicts"),
                "it rolls gt.matdicts (GT6 MultiItemBooks:68 + Behavior_Drop_Loot)");
        var random = net.minecraft.util.RandomSource.create(1234L);
        boolean sawDictionary = false;
        for (int i = 0; i < 20 && !sawDictionary; i++) {
            ItemStack stack = GTLootTables.roll("gt.matdicts", random);
            helper.assertTrue(!stack.isEmpty(), "a roll of gt.matdicts gives a book");
            sawDictionary = GTMaterialDictionary.materialOf(stack) != null;
        }
        helper.assertTrue(sawDictionary, "a rolled book is a material dictionary");
        helper.succeed();
    }

    /**
     * Section 71: GT6's three "Processing Data" pages ({@code UT.Books.addMaterialDictionary:900-905}) -
     * what the material becomes when smelted, solidified, burnt, pulverised, crushed, bent, compressed,
     * cut, forged, smashed or worked, in GT6's {@code whole.thousandths} amount format.
     *
     * <p>GT6 keeps all eleven targets on {@code OreDictMaterial}; the port models four of them on
     * {@code GTMaterial} and the other seven in
     * {@link com.gregtech.gregtech.data.generated.MaterialProcessingTargets} (generated from
     * {@code MT.java} by {@code tools/extract_gt6_processing_targets.py}), whose default is GT6's own
     * ({@code OreDictMaterial:287-294}: the material itself, one unit).</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void processingDataPageMatchesGt6(GameTestHelper helper) {
        java.util.regex.Pattern amounts = java.util.regex.Pattern.compile("\\d+\\.\\d{3} ");
        String[][] pages = {
                {"Smelting:", "Solidifying:", "Burning:", "Pulverising:", "Crushing:"},
                {"Bending:", "Compressing:", "Cutting:", "Forging:", "Smashing:"},
                {"Working:"}};
        int checked = 0, withTarget = 0;
        List<String> problems = new ArrayList<>();
        for (GTMaterial material : GTMaterialDictionary.materials()) {
            List<String> processing = GTMaterialDictionary.pages(material).stream()
                    .filter(entry -> entry.startsWith("Processing Data")).toList();
            if (processing.size() != 3) {
                problems.add(material.getName() + ": " + processing.size() + " processing pages, GT6 has 3");
                continue;
            }
            // GT6's order and its header rule of 19 '=' characters, on each of the three pages.
            for (int pageIndex = 0; pageIndex < 3; pageIndex++) {
                String page = processing.get(pageIndex);
                if (!page.contains("===================")) {
                    problems.add(material.getName() + ": no GT6 header rule on page " + (pageIndex + 1));
                }
                int cursor = -1;
                for (String label : pages[pageIndex]) {
                    int at = page.indexOf(label);
                    if (at < 0) problems.add(material.getName() + ": missing " + label);
                    else if (at < cursor) problems.add(material.getName() + ": " + label + " out of GT6's order");
                    else cursor = at;
                }
                // Every label is followed by exactly one GT6 amount line.
                boolean expectValue = false;
                for (String line : page.split("\n")) {
                    if (line.isBlank()) continue;
                    if (line.endsWith(":")) { expectValue = true; continue; }
                    if (!expectValue) continue; // the header and its rule
                    expectValue = false;
                    if (!amounts.matcher(line).lookingAt()) {
                        problems.add(material.getName() + ": GT6 amount format missing in '" + line + "'");
                    } else if (!line.endsWith("nothing")) {
                        withTarget++;
                    }
                }
            }
            checked++;
        }
        helper.assertTrue(problems.isEmpty(), "processing pages, problems (" + problems.size() + "): "
                + problems.subList(0, Math.min(5, problems.size())));
        helper.assertTrue(checked >= 900, "materials with three processing pages: " + checked);
        helper.assertTrue(withTarget > 500, "lines that name a target material: " + withTarget);

        // The table behind the seven extra targets: 38 explicit GT6 rows over 17 materials, and GT6's
        // own seven kind names.
        helper.assertTrue(com.gregtech.gregtech.data.generated.MaterialProcessingTargets.KINDS.length == 7,
                "seven extra targets: " + java.util.Arrays.toString(
                        com.gregtech.gregtech.data.generated.MaterialProcessingTargets.KINDS));
        helper.assertTrue(com.gregtech.gregtech.data.generated.MaterialProcessingTargets.rowCount() == 38,
                "explicit GT6 target rows: "
                        + com.gregtech.gregtech.data.generated.MaterialProcessingTargets.rowCount());

        // Iron's first page carries the header and GT6's wording for a self target.
        GTMaterial iron = GTMaterialRegistry.get("Iron");
        List<String> ironPages = GTMaterialDictionary.pages(iron).stream()
                .filter(entry -> entry.startsWith("Processing Data")).toList();
        helper.assertTrue(ironPages.get(0).contains("Smelting:\n"),
                "iron's first page lists Smelting: " + ironPages.get(0));
        helper.assertTrue(ironPages.get(0).contains("itself") || ironPages.get(0).contains("nothing"),
                "GT6's own wording for a self target: " + ironPages.get(0));

        // GT6's explicit rows: water solidifies into ice and lava into obsidian
        // (MT.java:1879-1880), the magnetic metals bend and compress back into their base metal, and
        // ceramic has all four forming targets disabled (MT.java:1279, setBending(null, 0)...).
        for (String name : new String[]{"Water", "Lava", "IronMagnetic", "Ceramic", "Iron"}) {
            helper.assertTrue(GTMaterialRegistry.get(name).isValid(),
                    "the port registers " + name + " (GT6's processing target table keys it)");
        }
        String water = lineOf(pagesOf("Water").get(0), "Solidifying:");
        helper.assertTrue(water.equals("1.000 Ice"), "GT6 H2O.setSolidifying(Ice, U): " + water);
        String lava = lineOf(pagesOf("Lava").get(0), "Solidifying:");
        helper.assertTrue(lava.equals("1.000 Obsidian"), "GT6 Lava.setSolidifying(Obsidian, U): " + lava);
        String magnetic = lineOf(pagesOf("IronMagnetic").get(1), "Bending:");
        helper.assertTrue(magnetic.equals("1.000 Iron"),
                "GT6 IronMagnetic.setBending(Fe, U) resolves the Fe field: " + magnetic);
        String ceramic = lineOf(pagesOf("Ceramic").get(1), "Bending:");
        helper.assertTrue(ceramic.equals("0.000 nothing"),
                "GT6 Ceramic.setBending(null, 0) disables bending: " + ceramic);
        // A material without an explicit row falls back to GT6's default, the material itself.
        String copper = lineOf(pagesOf("Copper").get(2), "Working:");
        helper.assertTrue(copper.equals("1.000 itself"),
                "GT6's default for the seven targets is the material itself: " + copper);
        helper.succeed();
    }

    /** The three processing pages of one material, by name. */
    private static List<String> pagesOf(String material) {
        return GTMaterialDictionary.pages(GTMaterialRegistry.get(material)).stream()
                .filter(entry -> entry.startsWith("Processing Data")).toList();
    }

    /**
     * The amount line GT6 prints under one label of a processing page. The label line is
     * {@code "Label:\n"}, so the value is the first non-blank line after it - reading {@code lines[0]}
     * would hand back the empty remainder of the label line itself.
     */
    private static String lineOf(String page, String label) {
        int at = page.indexOf(label);
        if (at < 0) return "<missing " + label + ">";
        for (String line : page.substring(at + label.length()).split("\n")) {
            if (!line.isBlank()) return line.trim();
        }
        return "";
    }
}
