package com.gregtech.gregtech.content.loot;

import com.gregtech.gregtech.GregTech;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetNbtFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GT6 puts GT loot into the world's chests and replaces those chests with GT loot chests
 * ({@code Loader_Loot:45-56} registers a {@code ChestGenHooksChestReplacer} per vanilla category,
 * {@code ChestGenHooksChestReplacer:122} places a GT chest carrying
 * {@code gt.dungeonloot = <vanilla category>}), and the GT chest rolls <em>that vanilla category</em>
 * ({@code MultiTileEntityChest:262} → {@code ST.generateLoot} → {@code ChestGenHooks}). So the rows a
 * player finds in a dungeon, mineshaft, stronghold, pyramid or village chest are the ones GT6 added
 * to the vanilla tables — {@code addLoot(ChestGenHooks.<CONST>, …)} rows, 91 of them transpiled into
 * {@link com.gregtech.gregtech.loaders.c.GTLootGen} as {@code van.<CONST>} by
 * {@code tools/transpile_gt6_loot.py}.</p>
 *
 * <p>The port has no GT chest block, so vanilla chests stay vanilla and this loader adds one GT pool
 * with a single roll to the ten 1.20.1 tables that correspond to GT6's categories (GT6's own roll
 * count for a category is vanilla's, which the 1.20.1 tables keep in their own pools). The pools are
 * added on {@link LootTableLoadEvent}, so vanilla chest contents stay untouched.</p>
 *
 * <p>GT's own tables ({@code gt.misc}, {@code gt.gems}, {@code gt.flawless}, {@code gt.seeds},
 * {@code gt.saplings}, {@code gt.bottles}, {@code gt.books}) are <em>not</em> injected here: GT6
 * reaches them through the loot bags and the Dusty Guide Book ({@link LootBagItem},
 * {@link GTLootTables}), which is what the {@code tech:bagged_sapling}, {@code tech:gem_pouch},
 * {@code tech:loot_pouch}, {@code tech:seed_pouch} and {@code tech:dusty_guide_book} rows below hand
 * out.</p>
 */
@Mod.EventBusSubscriber(modid = GregTech.MOD_ID)
public final class LootTableInjection {

    /**
     * GT6's vanilla chest categories mapped to the 1.20.1 loot tables (the constants are
     * {@code ChestGenHooks.*} from 1.7.10, the targets are the 1.20.1 chest tables).
     */
    private static final Map<String, ResourceLocation> VANILLA_TABLES = Map.ofEntries(
            Map.entry("DUNGEON_CHEST", table("chests/simple_dungeon")),
            Map.entry("MINESHAFT_CORRIDOR", table("chests/abandoned_mineshaft")),
            Map.entry("STRONGHOLD_LIBRARY", table("chests/stronghold_library")),
            Map.entry("STRONGHOLD_CROSSING", table("chests/stronghold_crossing")),
            Map.entry("STRONGHOLD_CORRIDOR", table("chests/stronghold_corridor")),
            Map.entry("PYRAMID_DESERT_CHEST", table("chests/desert_pyramid")),
            Map.entry("PYRAMID_JUNGLE_CHEST", table("chests/jungle_temple")),
            Map.entry("PYRAMID_JUNGLE_DISPENSER", table("chests/jungle_temple_dispenser")),
            Map.entry("VILLAGE_BLACKSMITH", table("chests/village/village_weaponsmith")),
            Map.entry("BONUS_CHEST", table("chests/spawn_bonus_chest")));

    /** The 1.20.1 chest table -> the GT6 tables feeding it (built once, tests read it). */
    private static final Map<ResourceLocation, List<String>> CATEGORIES = new LinkedHashMap<>();

    private static final List<String> ENTRIES = new ArrayList<>();

    private LootTableInjection() {}

    private static ResourceLocation table(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    /** The GT6 table names feeding one chest table, e.g. {@code chests/simple_dungeon -> [van.DUNGEON_CHEST]}. */
    public static List<String> categoriesOf(ResourceLocation chest) {
        return CATEGORIES.getOrDefault(chest, List.of());
    }

    /** Rows the last load added, for tests and reports ({@code <chest>|<item>|<row>}). */
    public static List<String> entries() { return List.copyOf(ENTRIES); }

    /** The chest tables this loader feeds (tests read it). */
    public static List<ResourceLocation> chests() { return List.copyOf(VANILLA_TABLES.values()); }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        String category = categoryOf(event.getName());
        if (category == null) return;
        GTLootTables.load();
        CATEGORIES.put(event.getName(), List.of(category));

        LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1));
        int rows = 0;
        for (GTLootTables.Row row : GTLootTables.rows(category)) {
            var entry = LootItem.lootTableItem(row.stack().getItem())
                    .setWeight(row.weight())
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(row.min(), row.max())));
            // Books carry their text in NBT (title/author/pages), so hand the tag to the loot item.
            CompoundTag tag = row.stack().getTag();
            if (tag != null) entry.apply(SetNbtFunction.setTag(tag));
            pool.add(entry);
            rows++;
            if (ENTRIES.size() < 400) {
                ENTRIES.add(event.getName().getPath() + "|" + row.stack().getItem() + "|" + row.spec());
            }
        }
        if (rows > 0) event.getTable().addPool(pool.build());
        GregTech.LOGGER.info("GT loot: {} rows added to {} ({})", rows, event.getName(), category);
    }

    /** The GT6 category a 1.20.1 chest table belongs to, or null when GT6 has no loot for it. */
    private static String categoryOf(ResourceLocation chest) {
        for (Map.Entry<String, ResourceLocation> entry : VANILLA_TABLES.entrySet()) {
            if (entry.getValue().equals(chest)) return "van." + entry.getKey();
        }
        return null;
    }

    /** The item id a spec resolves to, or null (used by the loot tests). */
    public static String itemIdOf(String spec) {
        return GTLootTables.itemIdOf(spec);
    }

    /** The stack a spec resolves to (the loot bags roll the {@code gt.*} tables through it). */
    public static ItemStack stackOf(String spec) {
        return GTLootTables.stackOf(spec);
    }
}
