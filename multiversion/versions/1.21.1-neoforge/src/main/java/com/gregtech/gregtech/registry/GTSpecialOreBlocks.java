package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.*;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Actual original crystal/rock/vanilla ore blocks from GTIconSetBlocks; no decorative substitutes. */
public final class GTSpecialOreBlocks {
    private static final List<DeferredHolder<Block, Block>> ALL = new ArrayList<>();
    private GTSpecialOreBlocks() {}
    public static List<DeferredHolder<Block, Block>> all() { return Collections.unmodifiableList(ALL); }
    public static void registerAll() {
        for (String icon : SpecialOreDefinitions.ORDERED_ICONS) {
            String id = icon.startsWith("ore_") ? "block_" + icon : icon;
            var block = GTBlocks.BLOCKS.register(id, () -> create(icon));
            GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties().stacksTo(64)));
            ALL.add(block);
        }
        // Original plate/WoodTreated exception is this real, placeable treated plank.
        DeferredHolder<Block, Block> treated = GTBlocks.BLOCKS.register("planks_treated", () -> new IconSetBlock(
                BlockBehaviour.Properties.of().strength(2.0F, 3.0F).sound(SoundType.WOOD), "planks_treated"));
        var treatedItem = GTBlocks.BLOCK_ITEMS.register("planks_treated", () -> new BlockItem(treated.get(), new Item.Properties().stacksTo(64)));
        GTItems.bind(MaterialPrefix.plate, com.gregtech.gregtech.content.material.generated.WoodMaterials.WoodTreated, treatedItem);
        ALL.add(treated);
    }

    private static Block create(String icon) {
        var rock = RockOreBlock.spec(icon);
        if (rock != null) return new RockOreBlock(BlockBehaviour.Properties.of()
                .strength(1.5F * rock.hardnessMultiplier(), 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE), icon, rock);
        var vanilla = VanillaOreBlock.spec(icon);
        if (vanilla != null) return new VanillaOreBlock(BlockBehaviour.Properties.of()
                .strength(1.5F * vanilla.hardnessMultiplier(), 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE), icon, vanilla);
        String slug = icon.substring("crystal_ore_".length());
        return new CrystalOreBlock(BlockBehaviour.Properties.of().strength(0.3F, 0.3F)
                .requiresCorrectToolForDrops().sound(SoundType.GLASS), icon,
                Character.toUpperCase(slug.charAt(0)) + slug.substring(1));
    }

    public static void registerCompositions() {
        for (var holder : ALL) {
            Block block = holder.get();
            if (block instanceof DenseOreBlock ore)
                ItemMaterialRegistry.register(block.asItem(), MaterialPrefix.oreDense, ore.material());
            else if (block instanceof VanillaOreBlock ore)
                ItemMaterialRegistry.register(block.asItem(), MaterialPrefix.oreVanillastone, ore.material());
        }
        ItemMaterialRegistry.register(GTItems.getStack(MaterialPrefix.plate,
                com.gregtech.gregtech.content.material.generated.WoodMaterials.WoodTreated).getItem(),
                MaterialPrefix.plate, com.gregtech.gregtech.content.material.generated.WoodMaterials.WoodTreated);
    }
}
