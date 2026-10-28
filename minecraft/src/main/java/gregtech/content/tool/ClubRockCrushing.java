package gregtech.content.tool;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.generated.CompoundMaterials;
import gregtech.content.material.generated.OreMaterials;
import gregtech.content.material.generated.StoneMaterials;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code GT_Tool_Club#convertBlockDrops}: a club works like a hard hammer, but every rocky
 * block it breaks crumbles into {@code rockGt} items of that block's material instead of dropping
 * itself ({@code GT_Tool_Club.java:61-109}).
 *
 * <p>The source compares against a fixed vanilla block list plus every {@code BlockStones} variant
 * flagged {@code JUSTSTONE}, and gives {@code 1 + random(4)} rocks per block. The port keeps the
 * same rows, resolving the vanilla blocks by registry id so both version nodes (and missing
 * modern variants) are handled the same way, and adds the modern vanilla stones the port already
 * binds to GT6 rock materials (granite/diorite/andesite, deepslate, tuff). Materials without a
 * rock form in this port (Obsidian, Blackstone) are left to the hard-hammer fallback rather than
 * dropping nothing.</p>
 */
public final class ClubRockCrushing {

    /** GT6 {@code 1 + RNGSUS.nextInt(4)}. */
    private static final int MAX_ROCKS = 4;

    private static Map<Block, GTMaterial> rocks;

    private ClubRockCrushing() {}

    /**
     * The club's own drop rows. Returns {@code null} when this block is not a rocky block, so the
     * caller can fall through to the hard-hammer conversion the club inherits in GT6.
     */
    @Nullable
    public static List<ItemStack> crush(BlockState state, List<ItemStack> drops, RandomSource random) {
        Block dropBlock = drops.size() == 1 ? Block.byItem(drops.get(0).getItem()) : Blocks.AIR;
        Block crushed = dropBlock == Blocks.AIR ? state.getBlock() : dropBlock;
        ItemStack rocks = rocksFor(crushed, random);
        return rocks.isEmpty() ? null : List.of(rocks);
    }

    private static ItemStack rocksFor(Block block, RandomSource random) {
        // GT_Tool_Club.java:96-99 — vanilla and modded redstone ore yields chipped Cinnabar.
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            return GTItems.getStack(MaterialPrefix.gemChipped, OreMaterials.Cinnabar, count(random));
        }
        // GT_Tool_Club.java:101-104 — BlockStones.JUSTSTONE covers every rock type's masonry.
        if (block instanceof GTStoneBlock stone && stone.variant().isJustStone()) {
            return GTItems.getStack(MaterialPrefix.rockGt, stone.stoneMaterial(), count(random));
        }
        GTMaterial material = rocks().get(block);
        return material == null ? ItemStack.EMPTY : GTItems.getStack(MaterialPrefix.rockGt, material, count(random));
    }

    private static int count(RandomSource random) {
        return 1 + random.nextInt(MAX_ROCKS);
    }

    private static Map<Block, GTMaterial> rocks() {
        Map<Block, GTMaterial> table = rocks;
        if (table != null) return table;
        table = buildRocks();
        rocks = table;
        return table;
    }

    private static Map<Block, GTMaterial> buildRocks() {
        Map<Block, GTMaterial> table = new HashMap<>();
        // GT_Tool_Club.java:65-68 — stone and its masonry metas.
        for (String path : new String[] {
            "stone",
            "smooth_stone",
            "cobblestone",
            "mossy_cobblestone",
            "stone_bricks",
            "mossy_stone_bricks",
            "cracked_stone_bricks",
            "chiseled_stone_bricks",
            "stone_stairs",
            "cobblestone_stairs",
            "mossy_cobblestone_stairs",
            "stone_brick_stairs",
            "mossy_stone_brick_stairs",
            "cobblestone_wall",
            "mossy_cobblestone_wall",
            "stone_brick_wall",
            "mossy_stone_brick_wall",
            "stone_button",
            "stone_pressure_plate"
        }) {
            put(table, path, StoneMaterials.Stone);
        }
        // GT_Tool_Club.java:69-72 — nether brick blocks.
        for (String path : new String[] {
            "nether_bricks",
            "nether_brick_stairs",
            "nether_brick_fence",
            "chiseled_nether_bricks",
            "cracked_nether_bricks"
        }) {
            put(table, path, StoneMaterials.NetherBrick);
        }
        put(table, "netherrack", StoneMaterials.Netherrack);
        put(table, "end_stone", StoneMaterials.Endstone);
        put(table, "obsidian", CompoundMaterials.Obsidian);
        // GT_Tool_Club.java:87-91 — basalt came from mods in 1.7.10; 1.16+ vanilla has it.
        for (String path : new String[] {"basalt", "polished_basalt", "smooth_basalt"}) {
            put(table, path, StoneMaterials.Basalt);
        }
        // GT_Tool_Club.java:92-95 — same for the blackstone family.
        GTMaterial blackstone = GTMaterialRegistry.get("Blackstone");
        for (String path : new String[] {
            "blackstone",
            "polished_blackstone",
            "chiseled_polished_blackstone",
            "cracked_polished_blackstone_bricks",
            "polished_blackstone_bricks"
        }) {
            put(table, path, blackstone);
        }
        // Modern vanilla stones the port already processes as their own GT6 rock material.
        for (String[] rock :
                new String[][] {{"granite", "Granite"}, {"diorite", "Diorite"}, {"andesite", "Andesite"}}) {
            GTMaterial material = GTMaterialRegistry.get(rock[1]);
            put(table, rock[0], material);
            put(table, "polished_" + rock[0], material);
        }
        GTMaterial deepslate = GTMaterialRegistry.get("Deepslate");
        for (String path : new String[] {
            "deepslate", "cobbled_deepslate", "polished_deepslate", "deepslate_bricks",
            "cracked_deepslate_bricks", "deepslate_tiles", "cracked_deepslate_tiles", "chiseled_deepslate"
        }) {
            put(table, path, deepslate);
        }
        GTMaterial tuff = GTMaterialRegistry.get("Tuff");
        for (String path :
                new String[] {"tuff", "polished_tuff", "tuff_bricks", "chiseled_tuff", "chiseled_tuff_bricks"}) {
            put(table, path, tuff);
        }
        return table;
    }

    private static void put(Map<Block, GTMaterial> table, String path, @Nullable GTMaterial material) {
        if (material == null || !material.isValid() || !MaterialPrefix.rockGt.isValidFor(material)) return;
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(path));
        if (block != Blocks.AIR) table.put(block, material);
    }
}
