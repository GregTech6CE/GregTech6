package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.content.loot.GTLootTables;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6's configurable loot chests — one block per loot table.
 *
 * <p>In GT6 the world chests are replaced with a GT chest carrying {@code gt.dungeonloot = <table>}
 * ({@code ChestGenHooksChestReplacer:122}) and the same "Loot Chest" item variants let a player place
 * a chest that rolls any table of {@code ST.LOOT_TABLES} ({@code ST.java:1019},
 * {@code MultiTileEntityChest:275}). The port registers one block per table
 * ({@code loot_chest_<suffix>}) instead of one block with NBT, which keeps the table on the block and
 * keeps a consumed-loot marker on dropped chest items to prevent rerolls after replacement.
 * The labels are GT6's own ({@code LH.java} {@code loot.*} entries).</p>
 *
 * <p>{@code gt.matdicts} is not offered: its rows are the material dictionary books, which the port
 * builds from material data at runtime and therefore does not register (see §21).</p>
 */
public final class GTLootChests {

    /**
     * {@code suffix|table} — GT6's {@code ST.LOOT_TABLES} order, minus {@code gt.matdicts}. The table is
     * the key {@code GTLootTables} uses: GT's own tables by name, the vanilla categories as
     * {@code van.<ChestGenHooks constant>} (that is how the loot transpiler emits the rows GT6 added to
     * the vanilla chest tables, §21).
     */
    private static final java.util.List<String> TABLES = com.gregtech.gregtech.content.loot.LootChestCatalog.ENTRIES;

    public static final List<DeferredHolder<net.minecraft.world.level.block.Block,com.gregtech.gregtech.block.inventory.LootChestBlock>> LOOT_CHESTS =
            new ArrayList<>();

    private GTLootChests() {}

    public static void registerAll() {
        for (String entry : TABLES) {
            String suffix = entry.substring(0, entry.indexOf('|'));
            String table = entry.substring(entry.indexOf('|') + 1);
            var chest = GTBlocks.BLOCKS.register("loot_chest_" + suffix, () ->
                    new com.gregtech.gregtech.block.inventory.LootChestBlock(
                            com.gregtech.gregtech.content.material.Materials.Steel, table,
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(4.0f, 6.0f)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()));
            GTBlocks.BLOCK_ITEMS.register("loot_chest_" + suffix, () ->
                    new com.gregtech.gregtech.block.inventory.MetalChestBlockItem(chest.get(),
                            new Item.Properties()));
            LOOT_CHESTS.add(chest);
        }
    }

    /** The table names a loot chest can roll, in registration order (tests read it). */
    public static List<String> tables() {
        List<String> out = new ArrayList<>();
        for (String entry : TABLES) out.add(entry.substring(entry.indexOf('|') + 1));
        return List.copyOf(out);
    }

    /** Every table the port can actually roll something from (the rest would place an empty chest). */
    public static List<String> usableTables() {
        List<String> out = new ArrayList<>();
        for (String table : tables()) {
            if (!GTLootTables.rows(table).isEmpty()) out.add(table);
        }
        return List.copyOf(out);
    }
}
