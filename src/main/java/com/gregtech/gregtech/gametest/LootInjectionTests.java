package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.book.GTBooks;
import com.gregtech.gregtech.content.loot.GTLootTables;
import com.gregtech.gregtech.content.loot.LootBagItem;
import com.gregtech.gregtech.content.loot.LootTableInjection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6's loot: the rows it adds to the vanilla chest tables ({@code addLoot(ChestGenHooks.X, …)},
 * which is what a player finds in a world chest — see {@link LootTableInjection}), and its own
 * tables ({@code gt.*}), which the loot bags and the Dusty Guide Book hand out
 * ({@link LootBagItem}, {@code Behavior_Drop_Loot}).
 *
 * <p>These tests check that the transpiled rows resolve and that the loot really reaches the loaded
 * chest tables — a silently empty pool would look like "GT loot just never spawns".</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class LootInjectionTests {

    /**
     * Chest rows GT6 loots that the port cannot place — pinned, so a new gap cannot slip in unnoticed.
     *
     * <p>Empty since §22: the three rows that used to be here asked for Trinium ({@code MT.Ke}) and
     * Naquadah ({@code MT.Nq}) arrow parts. GT6 generates them (both materials carry
     * {@code G_INGOT_MACHINE_ORES}, whose flags include {@code PROJECTILES}, and that flag is what
     * {@code OP.toolHeadArrow}, {@code OP.java:254}, and {@code OP.arrowGtWood}, {@code OP.java:281},
     * need); they were missing because {@code MaterialForms} is keyed by GT6's field name while the
     * lookup used the port's display name, and because the port's arrow-head condition asked for tool
     * quality instead. Both are fixed, so this list must stay empty.</p>
     */
    private static final List<String> EXPECTED_UNRESOLVED = List.of();

    /** Every chest row GT6 wrote must resolve to a port item, or be one of the pinned gaps. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void vanillaChestRowsResolve(GameTestHelper h) {
        GTLootTables.load();
        int rows = 0;
        List<String> tables = new ArrayList<>(GTLootTables.tableNames());
        tables.sort(String::compareTo);
        for (String table : tables) {
            if (!table.startsWith("van.")) continue;
            for (GTLootTables.Row row : GTLootTables.rows(table)) {
                rows++;
                h.assertTrue(GTLootTables.itemIdOf(row.spec()) != null,
                        table + " row resolves: " + row.spec());
            }
        }
        h.assertTrue(rows >= 91, "chest rows transpiled from GT6: " + rows);
        h.assertTrue(tables.size() >= 10, "GT6 chest categories carried over: " + tables);

        List<String> unresolved = GTLootTables.unresolved().stream()
                .filter(spec -> spec.startsWith("van.")).sorted().toList();
        List<String> expected = new ArrayList<>(EXPECTED_UNRESOLVED);
        expected.sort(String::compareTo);
        h.assertTrue(unresolved.equals(expected), "unresolved chest rows: " + unresolved
                + " (expected exactly " + expected + ")");
        h.succeed();
    }

    /** The ten 1.20.1 chest tables GT6 feeds really roll GT loot once their table is loaded. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void chestTablesRollGtLoot(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var origin = net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(4, 3, 4)));
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, origin)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        int checked = 0;
        for (ResourceLocation chest : LootTableInjection.chests()) {
            var table = server.getLootData().getLootTable(chest);
            h.assertTrue(table != null, chest + " is a loaded loot table");
            List<String> category = LootTableInjection.categoriesOf(chest);
            h.assertTrue(!category.isEmpty(), chest + " was fed by GT6's chest rows");
            int gtItems = 0;
            for (int roll = 0; roll < 12; roll++) {
                for (ItemStack stack : table.getRandomItems(params)) {
                    ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    if (key != null && key.getNamespace().equals("gregtech")) gtItems++;
                }
            }
            h.assertTrue(gtItems > 0, "GT loot in " + chest + " (" + category + "), rows: "
                    + GTLootTables.rows(category.get(0)).size());
            checked++;
        }
        h.assertTrue(checked >= 10, "chest tables checked: " + checked);
        h.succeed();
    }

    /** The four GT6 loot bags exist and roll the tables GT6 gave them ({@code Behavior_Drop_Loot}). */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void lootBagsRollTheirTables(GameTestHelper h) {
        String[][] expected = {
                {"bagged_sapling", "gt.saplings"},
                {"seed_pouch", "gt.seeds"},
                {"gem_pouch", "gt.flawless", "gt.gems", "gt.gems"},
                {"loot_pouch", "gt.misc"},
                {"loot_bottle", "gt.bottles"},
        };
        for (String[] bag : expected) {
            Item item = ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath("gregtech", bag[0]));
            h.assertTrue(item != null, "registered loot bag gregtech:" + bag[0]);
            h.assertTrue(item instanceof LootBagItem, "gregtech:" + bag[0] + " is a LootBagItem");
            String[] tables = ((LootBagItem) item).tables();
            h.assertTrue(tables.length == bag.length - 1, "gregtech:" + bag[0] + " table count: "
                    + tables.length);
            for (int i = 0; i < tables.length; i++) {
                h.assertTrue(tables[i].equals(bag[i + 1]), "gregtech:" + bag[0] + " table " + i
                        + ": " + tables[i] + " (GT6: " + bag[i + 1] + ")");
            }
        }
        // Every gt.* table a bag rolls must yield a port item, or the bag would drop nothing.
        RandomSource random = RandomSource.create(4711L);
        for (String table : List.of("gt.saplings", "gt.seeds", "gt.gems", "gt.flawless", "gt.misc",
                "gt.bottles")) {
            int rolls = 0;
            for (int i = 0; i < 40; i++) {
                ItemStack stack = GTLootTables.roll(table, random);
                h.assertTrue(!stack.isEmpty(), table + " roll returned an item");
                h.assertTrue(stack.getCount() >= 1, table + " roll has a positive count");
                rolls++;
            }
            h.assertTrue(rolls == 40, table + " rolled: " + rolls);
        }
        h.succeed();
    }

    /** The configurable GT6 loot chests fill themselves from their table on the first tick. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void lootChestsRollTheirTables(GameTestHelper h) {
        GTLootTables.load();
        List<String> tables = com.gregtech.gregtech.registry.GTLootChests.usableTables();
        h.assertTrue(tables.size() >= 15, "loot chest tables with rules: " + tables);
        h.assertTrue(tables.contains("gt.books") && tables.contains("gt.bottles")
                        && tables.contains("van.DUNGEON_CHEST"),
                "the GT tables and the vanilla categories are offered: " + tables);

        // Place one chest per table and let it generate its loot through the block entity.
        int index = 0;
        for (String table : tables) {
            String suffix = null;
            for (var entry : com.gregtech.gregtech.registry.GTLootChests.LOOT_CHESTS) {
                if (entry.get().lootTable().equals(table)) suffix = entry.getId().getPath();
            }
            h.assertTrue(suffix != null, "a chest block carries the table " + table);
            var block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                    ResourceLocation.fromNamespaceAndPath("gregtech", suffix));
            h.assertTrue(block != null, "registered loot chest block " + suffix);
            var pos = new net.minecraft.core.BlockPos(1 + (index % 4), 2, 1 + (index / 4));
            h.setBlock(pos, block.defaultBlockState()
                    .setValue(com.gregtech.gregtech.block.inventory.MetalChestBlock.FACING,
                            net.minecraft.core.Direction.NORTH));
            var be = h.getBlockEntity(pos);
            h.assertTrue(be instanceof com.gregtech.gregtech.blockentity.inventory.MetalChestBlockEntity,
                    suffix + " has a chest block entity");
            var chest = (com.gregtech.gregtech.blockentity.inventory.MetalChestBlockEntity) be;
            // The block ticker may already have filled the chest (that is the point of the ticker), so
            // the assertion is on the outcome, not on who called it.
            int placed = chest.generateLootIfNeeded();
            int looted = 0;
            for (int slot = 0; slot < chest.inventory().getSlots(); slot++) {
                if (!chest.inventory().getStackInSlot(slot).isEmpty()) looted++;
            }
            h.assertTrue(chest.lootGenerated(), suffix + " generated its loot");
            h.assertTrue(looted > 0, suffix + " (" + table + ") holds loot: " + looted + " stacks"
                    + " [placed by this call=" + placed + ", rows=" + GTLootTables.rows(table).size()
                    + ", count=" + java.util.Arrays.toString(GTLootTables.countRange(table)) + "]");
            h.assertTrue(chest.generateLootIfNeeded() == 0, suffix + " rolls only once");
            h.assertTrue(chest.generateLootIfNeeded() == 0, suffix + " rolls only once");
            index++;
        }
        h.succeed();
    }

    /** The Dusty Guide Book opens {@code gt.books}, so the manuals are reachable from chest loot. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void guideBookRollsGt6Manuals(GameTestHelper h) {
        Item guide = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath("gregtech", "dusty_guide_book"));
        h.assertTrue(guide instanceof LootBagItem, "gregtech:dusty_guide_book is a LootBagItem");
        String[] tables = ((LootBagItem) guide).tables();
        h.assertTrue(tables.length == 1 && tables[0].equals("gt.books"),
                "the guide book rolls gt.books: " + java.util.Arrays.toString(tables));

        h.assertTrue(GTBooks.names().size() == 20, "GT6 literal and material-generated books: " + GTBooks.names().size());
        for (String name : GTBooks.names()) {
            h.assertTrue(GTBooks.titleOf(name) != null && !GTBooks.titleOf(name).isEmpty(),
                    name + " has a title");
            h.assertTrue(GTBooks.authorOf(name) != null, name + " has an author");
            h.assertTrue(!GTBooks.pagesOf(name).isEmpty(), name + " has pages");
            for (String page : GTBooks.pagesOf(name)) {
                h.assertTrue(page.length() < 256, name + " page length " + page.length()
                        + " (GT6 drops pages of 256 characters or more)");
            }
            ItemStack book = GTBooks.bookStack(name);
            h.assertTrue(book.is(Items.WRITTEN_BOOK) && WrittenBookItem.makeSureTagIsValid(book.getTag()),
                    name + " opens as a valid Minecraft 1.20.1 written book");
        }
        h.assertTrue(GTBooks.pagesOf("Manual_Tools").size() == 43
                        && GTBooks.pagesOf("Manual_Tools").get(42).startsWith("Pocket Multitool"),
                "GT6 Tool Index contains all 43 pages, including the final page");
        h.assertTrue(GTBooks.pagesOf("Manual_Smeltery").size() == 42
                        && GTBooks.pagesOf("Manual_Smeltery").get(41).startsWith("Builder's Wand"),
                "GT6 Smelting Crucible Manual contains all 42 pages, including the final mold shape");
        h.assertTrue(GTBooks.titleOf("Manual_Hunting_Blaze").equals("Hunting Guide for Blazes and Ghasts")
                        && GTBooks.bookStack("Manual_Hunting_Blaze").getTag().getString("title").length() <= 32,
                "the original Blaze manual title stays indexed while its vanilla NBT remains valid");

        List<String> alloyPages = GTBooks.pagesOf("Manual_Alloys");
        h.assertTrue(alloyPages.size() > 3 && alloyPages.get(0).startsWith("This Book Contains Information"),
                "GT6 Alloy book has the three instructions and registered crucible recipes");
        h.assertTrue(alloyPages.stream().anyMatch(page -> page.startsWith("Alloy:\nBronze\n")
                        && page.contains("Components per 4") && page.contains("3 Copper")
                        && page.contains("1 Tin")),
                "Bronze recipe lists the actual crucible ratio");
        h.assertTrue(GTBooks.titleOf("Manual_Alloys").startsWith("Book of Alloys, Smeltery Edition (")
                        && GTBooks.bookStack("Manual_Alloys").getTag().getString("title").length() <= 32,
                "GT6 Alloy title is indexed and its 1.20.1 title opens");
        List<String> elementPages = GTBooks.pagesOf("Manual_Elements");
        h.assertTrue(elementPages.size() > 100
                        && elementPages.stream().anyMatch(page -> page.startsWith("Hydrogen\n1/0\n")
                        && page.contains("Plasma: 2000 K"))
                        && elementPages.stream().anyMatch(page -> page.startsWith("Iron\n26/30\n")
                        && page.contains("Plasma: 313400 K"))
                        && elementPages.stream().anyMatch(page -> page.startsWith("Curium\n96/153\n"))
                        && elementPages.stream().anyMatch(page -> page.startsWith("Oganesson\n118/176\n")),
                "periodic manual includes gas, metal and recovered superheavy elements with GT6 atomic/thermal values");
        h.assertTrue(elementPages.stream().noneMatch(page -> page.startsWith("Magic\n")),
                "GT6's hidden Magic element is excluded from the periodic manual");

        // GT6 has 17 gt.books rows, including the two material-generated manuals.
        var bookRows = GTLootTables.rows("gt.books");
        h.assertTrue(bookRows.size() == 17, "gt.books rows resolved: " + bookRows.size() + " of 17");
        h.assertTrue(bookRows.stream().anyMatch(row -> row.spec().equals("book:Manual_Smeltery"))
                        && bookRows.stream().anyMatch(row -> row.spec().equals("book:Manual_Tools"))
                        && bookRows.stream().anyMatch(row -> row.spec().equals("book:Manual_Elements"))
                        && bookRows.stream().anyMatch(row -> row.spec().equals("book:Manual_Alloys")),
                "all GT6 manual mappings resolve in gt.books");
        RandomSource random = RandomSource.create(99L);
        int written = 0;
        for (int i = 0; i < 60; i++) {
            ItemStack stack = GTLootTables.roll("gt.books", random);
            h.assertTrue(!stack.isEmpty(), "gt.books roll returned a book");
            h.assertTrue(stack.is(Items.WRITTEN_BOOK), "gt.books roll is a written book");
            h.assertTrue(stack.getTag() != null && stack.getTag().contains("title"),
                    "the rolled book carries its title in NBT");
            h.assertTrue(!stack.getTag().getList("pages", 8).isEmpty(), "the rolled book carries pages");
            written++;
        }
        h.assertTrue(written == 60, "books rolled: " + written);
        h.succeed();
    }

    /** Writes the loot report (per-table rows, unresolved specs, injected chest rows). */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void lootReportIsWritten(GameTestHelper h) {
        GTLootTables.load();
        var json = new java.util.TreeMap<String, Object>();
        var perTable = new java.util.TreeMap<String, Integer>();
        for (String table : GTLootTables.tableNames()) perTable.put(table, GTLootTables.rows(table).size());
        json.put("tables", perTable);
        json.put("unresolved", GTLootTables.unresolved());
        json.put("chestTables", LootTableInjection.chests().stream().map(Object::toString).sorted().toList());
        json.put("injectedRows", LootTableInjection.entries().size());
        json.put("baggedItems", java.util.Map.of(
                "bagged_sapling", "gt.saplings",
                "seed_pouch", "gt.seeds",
                "gem_pouch", "gt.flawless, gt.gems, gt.gems",
                "loot_pouch", "gt.misc",
                "dusty_guide_book", "gt.books"));
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("../../docs/gt6-loot-report.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (java.io.IOException e) {
            h.fail("cannot write docs/gt6-loot-report.json: " + e);
            return;
        }
        h.assertTrue(perTable.containsKey("gt.books"), "the report lists the gt.books table");
        h.assertTrue(BuiltInRegistries.ITEM.containsKey(
                ResourceLocation.fromNamespaceAndPath("gregtech", "loot_pouch")), "loot pouch is registered");
        h.succeed();
    }
}
