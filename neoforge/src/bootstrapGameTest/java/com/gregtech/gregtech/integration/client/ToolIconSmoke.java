package com.gregtech.gregtech.integration.client;

import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.item.GTToolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import java.util.List;

/** Exercise the final inventory renderer, including recipe stacks without material components. */
final class ToolIconSmoke {
    static void check(Minecraft client, List<ItemStack> gallery) {
        int checked = 0;
        for (GTToolType type : GTToolType.values()) {
            ItemStack tool = GTToolItem.create(type, GTMaterialRegistry.get("Iron"), GTMaterialRegistry.get("Wood"));
            check(client, tool, type.requiresHeadAssembly() && type.handleIcon() == null);
            ItemStack placeholder = new ItemStack(tool.getItem());
            check(client, placeholder, false);
            if ((client.getItemColors().getColor(placeholder, 0) >>> 24) != 255
                    || (client.getItemColors().getColor(tool, 2) >>> 24) != 255)
                throw new IllegalStateException("Transparent tool tint " + type);
            if (type == GTToolType.AXE || type == GTToolType.WRENCH || type == GTToolType.SWORD) {
                gallery.add(tool);
                gallery.add(placeholder);
            }
            checked += 2;
        }
        com.mojang.logging.LogUtils.getLogger().info("TOOL_ICON_SMOKE_SUCCESS {} real and recipe tool models", checked);
    }

    private static void check(Minecraft client, ItemStack stack, boolean woodHandle) {
        var model = client.getItemRenderer().getModel(stack, null, null, 0);
        var quads = new java.util.ArrayList<net.minecraft.client.renderer.block.model.BakedQuad>();
        for (var pass : model.getRenderPasses(stack, false)) {
            quads.addAll(pass.getQuads(null, null, RandomSource.create(42)));
            for (var side : net.minecraft.core.Direction.values())
                quads.addAll(pass.getQuads(null, side, RandomSource.create(42)));
        }
        if (quads.isEmpty()) throw new IllegalStateException("Invisible tool " + stack);
        if (woodHandle && quads.stream().noneMatch(q -> q.getSprite().contents().name().getPath().equals("item/material_icons/wood/stick")))
            throw new IllegalStateException("Missing wood handle " + stack + ": " + quads.stream().map(q -> q.getSprite().contents().name()).distinct().toList());
    }
}
