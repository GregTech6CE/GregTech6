package com.gregtech.gregtech.client;

import net.minecraft.client.color.item.ItemColor;

/** Adapts the port's RGB callbacks to the opaque ARGB expected by NeoForge item rendering. */
public final class ItemColorARGB {
    private ItemColorARGB() {}

    public static ItemColor opaque(ItemColor rgb) {
        return (stack, layer) -> 0xFF000000 | (rgb.getColor(stack, layer) & 0xFFFFFF);
    }
}
