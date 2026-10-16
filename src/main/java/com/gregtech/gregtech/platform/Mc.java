package com.gregtech.gregtech.platform;

import com.gregtech.gregtech.GregTech;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * Platform shim. NeoForge has a class with the same name and the same methods.
 * Each method keeps the behavior of the Forge call it replaces.
 */
public final class Mc {
    private Mc() {}

    public static Logger logger() {
        return GregTech.LOGGER;
    }

    /** {@link GregTech#id} lowercases the path. The NeoForge shim does not. */
    public static ResourceLocation gregtechId(String path) {
        return GregTech.id(path);
    }

    public static Item item(ResourceLocation id) {
        return ForgeRegistries.ITEMS.getValue(id);
    }

    public static ResourceLocation itemId(Item item) {
        return ForgeRegistries.ITEMS.getKey(item);
    }

    public static Block block(ResourceLocation id) {
        return ForgeRegistries.BLOCKS.getValue(id);
    }

    public static ResourceLocation blockId(Block block) {
        return ForgeRegistries.BLOCKS.getKey(block);
    }

    public static Iterable<Block> blocks() {
        return ForgeRegistries.BLOCKS.getValues();
    }

    public static boolean sameItemData(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameTags(a, b);
    }

    public static void setCustomName(ItemStack stack, Component name) {
        stack.setHoverName(name);
    }
}
