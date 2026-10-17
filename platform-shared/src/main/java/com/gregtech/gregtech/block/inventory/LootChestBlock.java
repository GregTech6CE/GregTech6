package com.gregtech.gregtech.block.inventory;

import com.gregtech.gregtech.api.material.GTMaterial;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6's configurable loot chest — the one world chest that is <em>not</em> vanilla
 * ({@code MultiTileEntityChest}, registrations {@code Loader_Loot:45-56} and
 * {@code Loader_MultiTileEntities} "Loot Chest" variants).
 *
 * <p>A GT6 chest carries {@code gt.dungeonloot = <table>} in its NBT
 * ({@code ChestGenHooksChestReplacer:122}, {@code MultiTileEntityChest:101}) and rolls that table when
 * it is first opened ({@code MultiTileEntityChest:262} → {@code ST.generateLoot}). The port registers
 * one variant per table instead of one block with NBT, so the table lives on the block
 * ({@link #lootTable()}) and the chest block entity fills itself from {@code GTLootTables}.</p>
 */
public class LootChestBlock extends MetalChestBlock {

    private final String lootTable;

    public LootChestBlock(GTMaterial material, String lootTable, Properties properties) {
        super(material, Shell.LOOT, properties);
        this.lootTable = lootTable;
    }

    /** The GT6 loot table this chest rolls (e.g. {@code gt.books}, {@code dungeonChest}). */
    public String lootTable() {
        return lootTable;
    }

    /** GT6's loot chests are trapped chests ({@code NBT_TRAPPED}, {@code ChestGenHooksChestReplacer:122}). */
    public static boolean isLootChest(BlockState state) {
        return state.getBlock() instanceof LootChestBlock;
    }
}
