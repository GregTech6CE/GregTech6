package com.gregtech.gregtech.loader;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Vanilla item  -> GT material mapping from GT6 {@code LoaderUnificationTargets}.
 */
public final class VanillaUnificationLoader {
    private static final long BLOCK = GTValues.U * 9;
    private static final long ORE = GTValues.U * 2;

    private VanillaUnificationLoader() {}

    public static void register() {
        // gems
        gem("EnderPearl", Items.ENDER_PEARL);
        gem("EnderEye", Items.ENDER_EYE);
        gem("Diamond", Items.DIAMOND);
        gem("Emerald", Items.EMERALD);
        gem("Coal", Items.COAL);
        gem("Charcoal", Items.CHARCOAL);
        gem("NetherQuartz", Items.QUARTZ);
        gem("NetherStar", Items.NETHER_STAR);
        gem("Lapis", Items.LAPIS_LAZULI);

        // ingots / nuggets
        ingot("Fe", Items.IRON_INGOT);
        ingot("Au", Items.GOLD_INGOT);
        ingot("Cu", Items.COPPER_INGOT);
        ingot("Brick", Items.BRICK);
        ingot("NetherBrick", Items.NETHER_BRICK);
        nugget("Au", Items.GOLD_NUGGET);

        // dusts / rods / misc
        dust("Redstone", Items.REDSTONE);
        dust("Glowstone", Items.GLOWSTONE_DUST);
        dust("Gunpowder", Items.GUNPOWDER);
        dust("Sugar", Items.SUGAR);
        dust("Bone", Items.BONE_MEAL);
        dustTiny("Blaze", Items.BLAZE_POWDER);
        stick("Blaze", Items.BLAZE_ROD);
        stick("Wood", Items.STICK);
        plate("Paper", Items.PAPER);
        plate("Glass", Blocks.GLASS_PANE.asItem());
        item("Flint", Items.FLINT, GTValues.U);

        // blocks (no dedicated prefix yet  - use GT6 block weights)
        block("Sand", Blocks.SAND.asItem());
        block("RedSand", Blocks.RED_SAND.asItem());
        block("Glass", Blocks.GLASS.asItem());
        block("Netherrack", Blocks.NETHERRACK.asItem());
        block("Endstone", Blocks.END_STONE.asItem());
        block("Obsidian", Blocks.OBSIDIAN.asItem());
        block("SoulSand", Blocks.SOUL_SAND.asItem());
        block("Stone", Blocks.GRAVEL.asItem());
        block("Sand", Blocks.SANDSTONE.asItem());
        block("Fe", Blocks.IRON_BLOCK.asItem());
        block("Au", Blocks.GOLD_BLOCK.asItem());
        block("Cu", Blocks.COPPER_BLOCK.asItem());
        block("Diamond", Blocks.DIAMOND_BLOCK.asItem());
        block("Emerald", Blocks.EMERALD_BLOCK.asItem());
        block("Lapis", Blocks.LAPIS_BLOCK.asItem());
        block("Coal", Blocks.COAL_BLOCK.asItem());
        block("Redstone", Blocks.REDSTONE_BLOCK.asItem());

        // ores
        ore("Coal", Blocks.COAL_ORE.asItem());
        ore("Fe", Blocks.IRON_ORE.asItem());
        ore("Au", Blocks.GOLD_ORE.asItem());
        ore("Cu", Blocks.COPPER_ORE.asItem());
        ore("Lapis", Blocks.LAPIS_ORE.asItem());
        ore("Redstone", Blocks.REDSTONE_ORE.asItem());
        ore("Diamond", Blocks.DIAMOND_ORE.asItem());
        ore("Emerald", Blocks.EMERALD_ORE.asItem());
        ore("NetherQuartz", Blocks.NETHER_QUARTZ_ORE.asItem());
        ore("Coal", Blocks.DEEPSLATE_COAL_ORE.asItem());
        ore("Fe", Blocks.DEEPSLATE_IRON_ORE.asItem());
        ore("Au", Blocks.DEEPSLATE_GOLD_ORE.asItem());
        ore("Cu", Blocks.DEEPSLATE_COPPER_ORE.asItem());
        ore("Lapis", Blocks.DEEPSLATE_LAPIS_ORE.asItem());
        ore("Redstone", Blocks.DEEPSLATE_REDSTONE_ORE.asItem());
        ore("Diamond", Blocks.DEEPSLATE_DIAMOND_ORE.asItem());
        ore("Emerald", Blocks.DEEPSLATE_EMERALD_ORE.asItem());

        rawOre("Cu", Items.RAW_COPPER);
        rawOre("Cu", Blocks.RAW_COPPER_BLOCK.asItem());
        rawOre("Fe", Items.RAW_IRON);
        rawOre("Fe", Blocks.RAW_IRON_BLOCK.asItem());
        rawOre("Au", Items.RAW_GOLD);
        rawOre("Au", Blocks.RAW_GOLD_BLOCK.asItem());

        // 1.16+ additions
        nugget("Fe", Items.IRON_NUGGET);
        gem("Amethyst", Items.AMETHYST_SHARD);
        block("Amethyst", Blocks.AMETHYST_BLOCK.asItem());
        ingot("Netherite", Items.NETHERITE_INGOT);
        block("Netherite", Blocks.NETHERITE_BLOCK.asItem());
        item("AncientDebris", Items.NETHERITE_SCRAP, GTValues.U);
        ore("AncientDebris", Blocks.ANCIENT_DEBRIS.asItem());

        // clay / glowstone / quartz
        item("Clay", Items.CLAY_BALL, GTValues.U);
        block("Clay", Blocks.CLAY.asItem());
        item("Glowstone", Blocks.GLOWSTONE.asItem(), GTValues.U * 4);
        item("NetherQuartz", Blocks.QUARTZ_BLOCK.asItem(), GTValues.U * 4);
        item("Bone", Items.BONE, GTValues.U * 2);
        item("Bone", Blocks.BONE_BLOCK.asItem(), GTValues.U * 6);

        // stone family (1 unit of the matching rock material per block)
        item("Stone", Blocks.STONE.asItem(), GTValues.U);
        item("Stone", Blocks.COBBLESTONE.asItem(), GTValues.U);
        item("Stone", Blocks.MOSSY_COBBLESTONE.asItem(), GTValues.U);
        item("Granite", Blocks.GRANITE.asItem(), GTValues.U);
        item("Diorite", Blocks.DIORITE.asItem(), GTValues.U);
        item("Andesite", Blocks.ANDESITE.asItem(), GTValues.U);
        item("Deepslate", Blocks.DEEPSLATE.asItem(), GTValues.U);
        item("Deepslate", Blocks.COBBLED_DEEPSLATE.asItem(), GTValues.U);
        item("Basalt", Blocks.BASALT.asItem(), GTValues.U);
        item("Blackstone", Blocks.BLACKSTONE.asItem(), GTValues.U);
        item("Calcite", Blocks.CALCITE.asItem(), GTValues.U);
        item("Tuff", Blocks.TUFF.asItem(), GTValues.U);
        item("Dripstone", Blocks.DRIPSTONE_BLOCK.asItem(), GTValues.U);

        item("Empty", Items.GLASS_BOTTLE, GTValues.U);
    }

    private static void gem(String material, Item item) {
        bind(MaterialPrefix.gem, material, item, MaterialPrefix.gem.getMaterialWeight());
    }

    private static void ingot(String material, Item item) {
        bind(MaterialPrefix.ingot, material, item, MaterialPrefix.ingot.getMaterialWeight());
    }

    private static void nugget(String material, Item item) {
        bind(MaterialPrefix.nugget, material, item, MaterialPrefix.nugget.getMaterialWeight());
    }

    private static void dust(String material, Item item) {
        bind(MaterialPrefix.dust, material, item, MaterialPrefix.dust.getMaterialWeight());
    }

    private static void dustTiny(String material, Item item) {
        bind(MaterialPrefix.dustTiny, material, item, MaterialPrefix.dustTiny.getMaterialWeight());
    }

    private static void stick(String material, Item item) {
        bind(MaterialPrefix.stick, material, item, MaterialPrefix.stick.getMaterialWeight());
    }

    private static void plate(String material, Item item) {
        bind(MaterialPrefix.plate, material, item, MaterialPrefix.plate.getMaterialWeight());
    }

    private static void block(String material, Item item) {
        bind(null, material, item, BLOCK);
    }

    private static void ore(String material, Item item) {
        bind(null, material, item, ORE);
    }

    private static void rawOre(String material, Item item) {
        bind(MaterialPrefix.oreRaw, material, item, MaterialPrefix.oreRaw.getMaterialWeight());
    }

    private static void item(String material, Item item, long amount) {
        bind(null, material, item, amount);
    }

    private static void bind(MaterialPrefix prefix, String materialName, Item item, long amount) {
        GTMaterial material = GTMaterialRegistry.get(materialName);
        if (material.isValid()) {
            ItemMaterialRegistry.register(item, prefix, material, amount);
        }
    }
}
