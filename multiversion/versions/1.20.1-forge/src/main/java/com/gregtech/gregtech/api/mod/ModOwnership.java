package com.gregtech.gregtech.api.mod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

/** Forge registry boundary for ModData; metadata and string ownership remain in core. */
public final class ModOwnership {
    private ModOwnership() {}

    public static boolean owns(ModData mod, Item item) {
        if (!mod.isLoaded() || item == null) return false;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key != null && mod.owns(key.toString());
    }

    public static boolean owns(ModData mod, Block block) {
        if (!mod.isLoaded() || block == null) return false;
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        return key != null && mod.owns(key.toString());
    }
}
