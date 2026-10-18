package com.gregtech.gregtech.integration.client;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTElectricItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Check the final baked layers and native color callbacks, then render the same stacks. */
final class LayeredItemSmoke {
    static void check(Minecraft client, List<ItemStack> gallery) {
        int materials = 0, wires = 0, electric = 0;
        for (var item : BuiltInRegistries.ITEM) {
            if (!(item instanceof MaterialItem material)) continue;
            var stack = new ItemStack(item);
            var quads = quads(client, stack);
            requireOpaque(client, stack, quads);
            materials++;
            if (material.getPrefix() == MaterialPrefix.wireFine) {
                requireLayer(stack, quads, 0);
                requireLayer(stack, quads, 1);
                if (quads.stream().noneMatch(q -> q.getSprite().contents().name().getPath().endsWith("wirefine_overlay")))
                    throw new IllegalStateException("Fine wire lacks its inner ring " + stack);
                wires++;
                if (material.getMaterial().getName().equals("Copper") || material.getMaterial().getName().equals("Gold"))
                    gallery.add(stack);
            }
        }
        for (var holder : GTElectricItems.tools()) {
            var stack = holder.get().assembled(GTMaterialRegistry.get("Iron"), 100000);
            var quads = quads(client, stack);
            for (int layer = 0; layer < 4; layer++) requireLayer(stack, quads, layer);
            requireOpaque(client, stack, quads);
            gallery.add(stack);
            electric++;
        }
        if (materials == 0 || wires == 0 || electric != 4) throw new IllegalStateException("Incomplete layered item specimens");
        com.mojang.logging.LogUtils.getLogger().info(
                "LAYERED_ITEM_SMOKE_SUCCESS {} material items, {} fine wires with inner rings, {} electric tools with four opaque layers",
                materials, wires, electric);
    }

    static List<BakedQuad> quads(Minecraft client, ItemStack stack) {
        var result = new ArrayList<BakedQuad>();
        var model = client.getItemRenderer().getModel(stack, null, null, 0);
        for (var pass : model.getRenderPasses(stack, false)) {
            result.addAll(pass.getQuads(null, null, RandomSource.create(42)));
            for (var side : Direction.values()) result.addAll(pass.getQuads(null, side, RandomSource.create(42)));
        }
        if (result.isEmpty()) throw new IllegalStateException("Invisible item " + stack);
        return result;
    }

    static void requireOpaque(Minecraft client, ItemStack stack, List<BakedQuad> quads) {
        for (var quad : quads) {
            if (quad.isTinted() && (client.getItemColors().getColor(stack, quad.getTintIndex()) >>> 24) != 255)
                throw new IllegalStateException("Transparent item layer " + quad.getTintIndex() + ": " + stack);
            if (quad.getSprite().contents().name().getPath().equals("missingno"))
                throw new IllegalStateException("Missing layer sprite " + stack);
        }
    }

    private static void requireLayer(ItemStack stack, List<BakedQuad> quads, int layer) {
        if (quads.stream().noneMatch(q -> q.getTintIndex() == layer))
            throw new IllegalStateException("Missing baked layer " + layer + ": " + stack);
    }
}
