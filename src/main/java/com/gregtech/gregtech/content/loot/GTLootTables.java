package com.gregtech.gregtech.content.loot;

import com.gregtech.gregtech.content.book.GTBooks;
import com.gregtech.gregtech.content.book.GTMaterialDictionary;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.loaders.c.GTGeneratedChem;
import com.gregtech.gregtech.loaders.c.GTLootGen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GT6's loot tables, resolved from the rows {@code tools/transpile_gt6_loot.py} generated into
 * {@link GTLootGen} ({@code table|weight|min|max|spec}).
 *
 * <p>GT6 keeps its loot in vanilla 1.7.10 {@code ChestGenHooks} tables: {@code addLoot} adds a
 * weighted row ({@code ConfigsGT.WORLDGEN}, {@code Loader_Loot:565-574}) and a table is rolled with
 * {@code ChestGenHooks.getOneItem(table, rand)} / {@code ST.generateLoot} — one weighted row, its
 * stack size uniform between the row's min and max ({@code Loader_Loot.addLoot}, which passes
 * {@code min}/{@code max} to {@code WeightedRandomChestContent}). {@link #roll} reproduces exactly
 * that pick.</p>
 *
 * <p>The two kinds of table in {@link GTLootGen} are used differently:</p>
 *
 * <ul>
 *   <li>{@code van.<ChestGenHooks constant>} — the rows GT6 adds to the vanilla chest tables. Those
 *       are the ones a player finds in world chests ({@link LootTableInjection} feeds them to the
 *       1.20.1 chest loot tables).</li>
 *   <li>{@code gt.<name>} — GT's own tables ({@code gt.misc}, {@code gt.gems}, {@code gt.flawless},
 *       {@code gt.seeds}, {@code gt.saplings}, {@code gt.bottles}, {@code gt.books}), which GT6
 *       hands out through the loot bags and the Dusty Guide Book
 *       ({@code gregapi/item/multiitem/behaviors/Behavior_Drop_Loot}, {@link LootBagItem}), not
 *       through world chests.</li>
 * </ul>
 */
public final class GTLootTables {

    /** One transpiled row: GT6's weight/chance, its stack-size range and the port stack it resolves to. */
    public record Row(String table, int weight, int min, int max, String spec, ItemStack stack) {}

    private static final Map<String, List<Row>> TABLES = new LinkedHashMap<>();
    private static final Map<String, int[]> COUNTS = new LinkedHashMap<>();
    private static final List<String> UNRESOLVED = new ArrayList<>();
    private static boolean loaded;

    private GTLootTables() {}

    /** Parses and resolves every generated row (idempotent; needs the item registries to be frozen). */
    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        for (String row : GTLootGen.tableCounts()) {
            String[] parts = row.split("\\|");
            if (parts.length == 3) {
                COUNTS.put(parts[0], new int[]{Integer.parseInt(parts[1]), Integer.parseInt(parts[2])});
            }
        }
        for (String row : GTLootGen.rows()) {
            String[] parts = row.split("\\|");
            if (parts.length != 5) continue;
            ItemStack stack = stackOf(parts[4]);
            if (stack.isEmpty()) {
                UNRESOLVED.add(parts[0] + " " + parts[4]);
                continue;
            }
            Row entry = new Row(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]), parts[4], stack);
            TABLES.computeIfAbsent(entry.table(), key -> new ArrayList<>()).add(entry);
        }
        materialDictionaries();
    }

    /**
     * GT6's {@code gt.matdicts} table ({@code Loader_Loot:359-364}): one row per material that has a
     * dictionary book, weight 144, exactly like the original loops over
     * {@code OreDictMaterial.MATERIAL_ARRAY}. The table is built here rather than transpiled because
     * its rows depend on the registered materials, just as in GT6.
     */
    private static void materialDictionaries() {
        List<Row> rows = new ArrayList<>();
        for (GTMaterial material : GTMaterialDictionary.materials()) {
            ItemStack book = GTMaterialDictionary.bookStack(material);
            if (book.isEmpty()) continue;
            rows.add(new Row("gt.matdicts", 144, 1, 1,
                    GTMaterialDictionary.MAPPING_PREFIX + material.getName(), book));
        }
        if (rows.isEmpty()) return;
        TABLES.put("gt.matdicts", List.copyOf(rows));
        COUNTS.putIfAbsent("gt.matdicts", new int[]{8, 24});
    }

    /** The rows of one GT6 table, in generation order; empty when the table is unknown. */
    public static List<Row> rows(String table) {
        load();
        return TABLES.getOrDefault(table, List.of());
    }

    /** Every table that has at least one resolvable row (tests and reports read it). */
    public static Set<String> tableNames() {
        load();
        return Collections.unmodifiableSet(TABLES.keySet());
    }

    /** Rows whose spec did not resolve, as {@code <table> <spec>} (reported by the loot tests). */
    public static List<String> unresolved() {
        load();
        return List.copyOf(UNRESOLVED);
    }

    /** The port stack for one spec: books are written books, the rest is a chem spec. */
    public static ItemStack stackOf(String spec) {
        if (spec.startsWith("book:")) return GTBooks.bookStack(spec.substring("book:".length()));
        if (spec.startsWith(GTMaterialDictionary.MAPPING_PREFIX)) {
            return GTMaterialDictionary.bookStack(spec);
        }
        ItemStack stack = GTGeneratedChem.resolveSpec(spec);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    /** The item id a spec resolves to, or null (used by the loot tests and reports). */
    public static String itemIdOf(String spec) {
        ItemStack stack = stackOf(spec);
        if (stack.isEmpty()) return null;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key == null ? null : key.toString();
    }

    /**
     * One roll of a GT6 table: a weighted pick, sized uniformly between the row's min and max — the
     * same content {@code ChestGenHooks.getOneItem} produces for GT6's {@code addLoot} rows.
     *
     * @return the rolled stack, or an empty stack when the table has no resolvable rows
     */
    public static ItemStack roll(String table, RandomSource random) {
        List<Row> rows = rows(table);
        if (rows.isEmpty()) return ItemStack.EMPTY;
        int index=LootRollRules.pick(rows.stream().mapToInt(Row::weight).toArray(),random::nextInt);
        if(index>=0){var row=rows.get(index);int count=LootRollRules.count(row.min(),row.max(),random::nextInt);ItemStack stack=row.stack().copy();stack.setCount(Math.min(count,stack.getMaxStackSize()));return stack;}

        return ItemStack.EMPTY;
    }

    /**
     * How many items a roll of a table hands out, as {@code [min, max]} — GT6's
     * {@code ChestGenHooks.getInfo("gt.books").setMin(8).setMax(24)} ({@code Loader_Loot:337-339}); the
     * configurable loot chest draws that many stacks when it is first opened.
     *
     * @return the range, or {@code {8, 24}} for a table GT6 does not declare bounds for
     */
    public static int[] countRange(String table) {
        load();
        int[] range = COUNTS.get(table);
        return range == null ? new int[]{8, 24} : range.clone();
    }

    /** Every table GT6 gave roll bounds to (tests and the loot chest read it). */
    public static Set<String> countedTables() {
        load();
        return Collections.unmodifiableSet(COUNTS.keySet());
    }

    /**
     * Fills a container the way GT6's configurable loot chest does when it is first opened
     * ({@code MultiTileEntityChest:262 generateDungeonLoot} → {@code ST.generateLoot}): as many
     * {@link #roll} draws as the table's own count range, placed into the first free slots.
     *
     * @return how many stacks were placed
     */
    public static int fillInto(String table, net.minecraftforge.items.ItemStackHandler inventory,
                               RandomSource random) {
        int[] range = countRange(table);
        int rolls = LootRollRules.count(range[0],range[1],random::nextInt);
        int placed = 0;
        for (int draw = 0; draw < rolls; draw++) {
            ItemStack stack = roll(table, random);
            if (stack.isEmpty()) continue;
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                if (!inventory.getStackInSlot(slot).isEmpty()) continue;
                inventory.insertItem(slot, stack, false);
                placed++;
                break;
            }
        }
        return placed;
    }
}
