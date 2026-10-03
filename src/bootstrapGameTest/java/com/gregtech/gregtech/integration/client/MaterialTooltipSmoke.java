package com.gregtech.gregtech.integration.client;

import com.gregtech.gregtech.block.MaterialBlockItem;
import com.gregtech.gregtech.item.MaterialItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

/** Uses the native tooltip entry point, including item callbacks and the event bus. */
final class MaterialTooltipSmoke {
    static void check() {
        int materials = 0, blocks = 0;
        for (var item : BuiltInRegistries.ITEM) {
            if (item instanceof MaterialItem material && material.getMaterial().resolve().getId() > 0) {
                check(new ItemStack(item));
                materials++;
            } else if (item instanceof MaterialBlockItem block && block.material() != null
                    && block.material().resolve().getId() > 0) {
                check(new ItemStack(item));
                blocks++;
            }
        }
        for (var item : new Item[]{Items.IRON_INGOT, Items.COPPER_INGOT, Items.GOLD_INGOT})
            check(new ItemStack(item));
        if (materials == 0 || blocks == 0) throw new IllegalStateException("No material tooltip specimens");
        com.mojang.logging.LogUtils.getLogger().info(
                "MATERIAL_TOOLTIP_SMOKE_SUCCESS {} material items, {} material/stone blocks, 3 foreign items; normal and advanced",
                materials, blocks);
    }

    private static void check(ItemStack stack) {
        require(stack, stack.getTooltipLines(null, TooltipFlag.NORMAL), "tooltip.gregtech.f3h_hint");
        require(stack, stack.getTooltipLines(null, TooltipFlag.ADVANCED), "tooltip.gregtech.contained_materials");
    }

    private static void require(ItemStack stack, List<Component> lines, String key) {
        long count = lines.stream().filter(line -> line.getContents() instanceof TranslatableContents c
                && c.getKey().equals(key)).count();
        if (count != 1) throw new IllegalStateException("Expected one " + key + " for " + stack + ", got " + count);
    }
}
