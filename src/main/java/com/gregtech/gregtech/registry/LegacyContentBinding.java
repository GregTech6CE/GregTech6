package com.gregtech.gregtech.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Native identity boundary; only explicitly audited aliases are interchangeable. */
public final class LegacyContentBinding {
    private LegacyContentBinding() {}
    public static boolean legacy(Item item) {
        var id = BuiltInRegistries.ITEM.getKey(item);
        return id.getNamespace().equals("gregtech") && LegacyContentAliases.legacy(id.getPath());
    }
    public static Item canonicalItem(Item item) {
        var id = BuiltInRegistries.ITEM.getKey(item);
        if (!id.getNamespace().equals("gregtech") || !LegacyContentAliases.legacy(id.getPath())) return item;
        var target = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", LegacyContentAliases.canonical(id.getPath()));
        return BuiltInRegistries.ITEM.containsKey(target) ? BuiltInRegistries.ITEM.get(target) : item;
    }
    public static boolean samePart(Block actual, Block expected) {
        if (actual == expected) return true;
        var a = BuiltInRegistries.BLOCK.getKey(actual);
        var b = BuiltInRegistries.BLOCK.getKey(expected);
        return a.getNamespace().equals("gregtech") && b.getNamespace().equals("gregtech")
                && LegacyContentAliases.canonical(a.getPath()).equals(LegacyContentAliases.canonical(b.getPath()));
    }
}
