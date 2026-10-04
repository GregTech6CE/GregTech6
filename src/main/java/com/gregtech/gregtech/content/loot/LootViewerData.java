package com.gregtech.gregtech.content.loot;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;

/** Real resolved GT loot rows and the same shared mob rules, independent of viewer APIs. */
public final class LootViewerData {
    private LootViewerData() {}
    public record Row(String id, Component source, List<ItemStack> inputs, ItemStack output,
                      int min, int max, double chance, List<Component> notes) {}
    private static Component text(String key, Object... args) { return Component.translatable("gregtech.loot." + key, args); }
    private static ItemStack item(String id) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
    public static List<Row> lootTables() {
        var result = new ArrayList<Row>();
        GTLootTables.load();
        for (String table : GTLootTables.tableNames()) {
            var inputs = new ArrayList<ItemStack>();
            for (String entry : LootChestCatalog.ENTRIES) {
                String[] parts = entry.split("\\|");
                if (parts[1].equals(table)) {
                    var chest = item("gregtech:loot_chest_" + parts[0]);
                    if (!chest.isEmpty()) inputs.add(chest);
                }
            }
            // A bag may roll a table twice; this page is explicitly one weighted draw.
            for (var nativeItem : BuiltInRegistries.ITEM) if (nativeItem instanceof LootBagItem bag
                    && Arrays.asList(bag.tables()).contains(table)) inputs.add(new ItemStack(nativeItem));
            var rows = GTLootTables.rows(table);
            long total = rows.stream().mapToLong(GTLootTables.Row::weight).sum();
            int[] rolls = GTLootTables.countRange(table);
            int index = 0;
            for (var row : rows) {
                double chance = total == 0 ? 0 : (double)row.weight() / total;
                List<Component> notes = new ArrayList<>();
                notes.add(text("table", table));
                notes.add(text("weighted", row.weight(), total));
                notes.add(text("one_draw"));
                notes.add(text("chest_draws", rolls[0], rolls[1]));
                if (table.startsWith("van.")) notes.add(text("vanilla_pool"));
                for (var source : inputs) if (source.getItem() instanceof LootBagItem bag) {
                    long count = Arrays.stream(bag.tables()).filter(table::equals).count();
                    notes.add(text("bag_draws", source.getHoverName(), count));
                }
                result.add(new Row("table/" + table.toLowerCase(Locale.ROOT).replace('.', '/') + "/" + index++,
                    inputs.isEmpty() ? text("table", table) : inputs.get(0).getHoverName(), List.copyOf(inputs),
                    row.stack().copy(), row.min(), row.max(), chance, List.copyOf(notes)));
            }
        }
        return List.copyOf(result);
    }
    private static final Map<MobDropRules.Kind, String> ENTITIES = Map.ofEntries(
        Map.entry(MobDropRules.Kind.ZOMBIE, "zombie"), Map.entry(MobDropRules.Kind.ZOMBIE_VILLAGER, "zombie_villager"),
        Map.entry(MobDropRules.Kind.PIG_ZOMBIE, "zombified_piglin"), Map.entry(MobDropRules.Kind.SPIDER, "spider"),
        Map.entry(MobDropRules.Kind.SKELETON, "skeleton"), Map.entry(MobDropRules.Kind.WITCH, "witch"),
        Map.entry(MobDropRules.Kind.HOGLIN, "hoglin"), Map.entry(MobDropRules.Kind.ZOGLIN, "zoglin"),
        Map.entry(MobDropRules.Kind.WOLF, "wolf"), Map.entry(MobDropRules.Kind.HORSE, "horse"),
        Map.entry(MobDropRules.Kind.DONKEY, "donkey"), Map.entry(MobDropRules.Kind.MULE, "mule"),
        Map.entry(MobDropRules.Kind.ZOMBIE_HORSE, "zombie_horse"), Map.entry(MobDropRules.Kind.SKELETON_HORSE, "skeleton_horse"),
        Map.entry(MobDropRules.Kind.COW, "cow"), Map.entry(MobDropRules.Kind.MOOSHROOM, "mooshroom"),
        Map.entry(MobDropRules.Kind.PIG, "pig"), Map.entry(MobDropRules.Kind.RABBIT, "rabbit"));
    private static List<ItemStack> eggs(MobDropRules.Kind kind) {
        String type = ENTITIES.get(kind);
        if (type == null) return List.of(new ItemStack(Items.NAME_TAG));
        var result = new ArrayList<ItemStack>();
        for (String name : switch (kind) {
            case ZOMBIE -> List.of(type, "husk", "drowned");
            case SPIDER -> List.of(type, "cave_spider");
            case SKELETON -> List.of(type, "stray", "wither_skeleton");
            default -> List.of(type);
        }) { var egg = item("minecraft:" + name + "_spawn_egg"); if (!egg.isEmpty()) result.add(egg); }
        return List.copyOf(result);
    }
    private static void mob(List<Row> rows, MobDropRules.Kind kind, String spec, int min, int max, double chance, String... conditions) {
        var output = GTLootTables.stackOf(spec);
        if (output.isEmpty()) throw new IllegalStateException("Missing original mob drop: " + spec);
        var notes = new ArrayList<Component>();
        notes.add(text("looting_zero"));
        for (String condition : conditions) notes.add(text(condition));
        rows.add(new Row("mob/" + kind.name().toLowerCase(Locale.ROOT) + "/" + rows.size(),
            kind == MobDropRules.Kind.OTHER ? text("any_mob") : Component.translatable("entity.minecraft." + ENTITIES.get(kind)),
            eggs(kind), output, min, max, chance, List.copyOf(notes)));
    }
    public static List<Row> mobDrops() {
        var rows = new ArrayList<Row>();
        for (var fixed : MobDropRules.FIXED) {
            mob(rows, fixed.kind(), fixed.spec(), 1, 1, 1.0 / fixed.denominator(),
                fixed.playerOnly() ? "player_kill" : "any_kill", "not_baby");
            if (fixed.kind() == MobDropRules.Kind.ZOMBIE) mob(rows, MobDropRules.Kind.ZOMBIE_VILLAGER,
                fixed.spec(), 1, 1, 1.0 / fixed.denominator(), "player_kill", "not_baby");
        }
        for (var part : MobDropRules.PARTS) {
            mob(rows, part.kind(), "tech:" + part.item() + ":1", 0, part.attempts(),
                (double)MobDropRules.partChance(0, part.denominator()) / part.denominator(), "per_attempt", "looting_parts", "not_baby");
        }
        for (var kind : List.of(MobDropRules.Kind.ZOMBIE, MobDropRules.Kind.ZOMBIE_VILLAGER)) {
            mob(rows, kind, "i:rockGt:Stone:1", 1, 1, .25, "player_kill");
            mob(rows, kind, "v:flint:1", 1, 1, .25, "player_kill");
            for (String raisin : MobDropRules.RAISINS) mob(rows, kind, "tech:" + raisin + ":1", 1, 1,
                1.0 / MobDropRules.rareBound(0) / MobDropRules.RAISINS.size(), "player_kill", "rare_looting");
        }
        mob(rows, MobDropRules.Kind.PIG_ZOMBIE, "i:rockGt:Netherrack:1", 1, 1, .25, "player_kill");
        mob(rows, MobDropRules.Kind.PIG_ZOMBIE, "v:flint:1", 1, 1, .25, "player_kill");
        for (String book : List.of("dusty_guide_book", "dusty_material_dictionary"))
            mob(rows, MobDropRules.Kind.ZOMBIE_VILLAGER, "tech:" + book + ":1", 1, 3, 1, "player_kill");
        mob(rows, MobDropRules.Kind.SPIDER, "v:spider_eye:1", 1, 1, .25, "non_player_kill");
        for (String cookie : MobDropRules.COOKIES) mob(rows, MobDropRules.Kind.SPIDER, "tech:" + cookie + ":1", 1, 1,
            1.0 / MobDropRules.rareBound(0) / MobDropRules.COOKIES.size(), "player_kill", "rare_looting");
        for (String spec : new LinkedHashSet<>(MobDropRules.SKELETON_RARE)) mob(rows, MobDropRules.Kind.SKELETON, spec, 1, 1,
            (double)Collections.frequency(MobDropRules.SKELETON_RARE, spec) / MobDropRules.SKELETON_RARE.size() / MobDropRules.rareBound(0), "player_kill", "rare_looting");
        mob(rows, MobDropRules.Kind.WITCH, "tech:loot_bottle:1", 1, 1, 1, "player_kill", "looting_quantity");
        mob(rows, MobDropRules.Kind.WITCH, "tech:loot_bottle:1", 1, 1, 1.0 / MobDropRules.rareBound(0), "non_player_kill", "rare_looting");
        mob(rows, MobDropRules.Kind.RABBIT, "v:carrot:1", 1, 1, .11, "rabbit_looting", "not_baby");
        for (var kind : List.of(MobDropRules.Kind.HORSE, MobDropRules.Kind.DONKEY, MobDropRules.Kind.MULE, MobDropRules.Kind.WOLF)) {
            String animal = kind.name().toLowerCase(Locale.ROOT);
            String raw = kind == MobDropRules.Kind.WOLF ? "dogmeat" : animal + "_meat";
            for (boolean cooked : new boolean[]{false, true}) mob(rows, kind, "tech:" + (cooked ? "grilled_" : "") + raw + ":1",
                kind == MobDropRules.Kind.WOLF ? 0 : 1, kind == MobDropRules.Kind.WOLF ? 2 : 5, -1,
                cooked ? "burning" : "not_burning", "looting_quantity", "not_baby", kind == MobDropRules.Kind.WOLF ? "uniform_meat" : "horse_stats");
        }
        for (var kind : List.of(MobDropRules.Kind.ZOMBIE_HORSE, MobDropRules.Kind.SKELETON_HORSE))
            mob(rows, kind, kind == MobDropRules.Kind.ZOMBIE_HORSE ? "v:rotten_flesh:1" : "v:bone:1", 1, 5, -1,
                "horse_stats", "looting_quantity", "not_baby");
        for (var kind : List.of(MobDropRules.Kind.PIG, MobDropRules.Kind.HOGLIN, MobDropRules.Kind.COW, MobDropRules.Kind.MOOSHROOM, MobDropRules.Kind.HORSE)) {
            for (boolean cooked : new boolean[]{false, true}) {
                var family = kind == MobDropRules.Kind.PIG || kind == MobDropRules.Kind.HOGLIN ? MobDropRules.Meat.PORK
                        : kind == MobDropRules.Kind.HORSE ? MobDropRules.Meat.HORSE : MobDropRules.Meat.BEEF;
                for (int rotation = 0; rotation < (family == MobDropRules.Meat.HORSE ? 1 : 2); rotation++) {
                    var drop = MobDropRules.meatReplacement(family, 1, cooked, rotation, ignored -> 0);
                    mob(rows, kind, drop.spec(), drop.count(), 64,
                        -1, "existing_meat", "rotation", cooked ? "burning_or_cooked" : "not_burning");
                }
            }
        }
        mob(rows, MobDropRules.Kind.OTHER, "tech:scrap_meat:1", 1, 1, 1.0 / 3, "per_meat_stack");
        mob(rows, MobDropRules.Kind.OTHER, "i:arrowGtWood:Empty:1", 1, 64, .75, "existing_arrow", "headless_looting");
        for (String prefix : List.of("plate", "ingot", "chunkGt", "nugget"))
            mob(rows, MobDropRules.Kind.OTHER, "i:" + prefix + ":Lead:1", 1, 64, 1, "existing_iron", "eligible_mob", "same_count");
        mob(rows, MobDropRules.Kind.OTHER, "v:stick:1", 1, 1, 1, "wood_tool", "non_player_kill");
        mob(rows, MobDropRules.Kind.OTHER, "v:stick:1", 2, 2, 1, "stone_tool", "non_player_kill");
        mob(rows, MobDropRules.Kind.OTHER, "v:name_tag:1", 1, 1, 1, "persistent_name");
        return List.copyOf(rows);
    }
}
