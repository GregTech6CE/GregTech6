package com.gregtech.gregtech.platform;

//? if >=1.21.1 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import com.gregtech.gregtech.GregTech;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
//? if >=1.21.1 {
/**///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}
import org.slf4j.Logger;
//? if >=1.21.1 {
/*import org.slf4j.LoggerFactory;

import java.lang.StackWalker.Option;
*///?} else {
//?}

/**
 * Platform shim. NeoForge has a class with the same name and the same methods.
 * Each method keeps the behavior of the Forge call it replaces.
 */

/**
 * Platform shim. Forge has a class with the same name and the same methods.
 * Each method keeps the behavior of the NeoForge call it replaces.
 */
public final class Mc {
    private Mc() {}


/** Names the logger after the calling class, as {@code LogUtils.getLogger()} did. */
    public static Logger logger() {
//? if >=1.21.1 {
/*        Class<?> caller = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames.skip(1).findFirst().orElseThrow().getDeclaringClass());
        return LoggerFactory.getLogger(caller);
*///?} else {
        return GregTech.LOGGER;
//?}
    }

    /** {@link GregTech#id} lowercases the path. The NeoForge shim does not. */
    public static ResourceLocation gregtechId(String path) {
//? if >=1.21.1 {
/*        return ResourceLocation.fromNamespaceAndPath("gt6ce", path);
*///?} else {
        return GregTech.id(path);
//?}
    }

    public static Item item(ResourceLocation id) {
//? if >=1.21.1 {
/*        return BuiltInRegistries.ITEM.get(id);
*///?} else {
        return ForgeRegistries.ITEMS.getValue(id);
//?}
    }

    public static ResourceLocation itemId(Item item) {
//? if >=1.21.1 {
/*        return BuiltInRegistries.ITEM.getKey(item);
*///?} else {
        return ForgeRegistries.ITEMS.getKey(item);
//?}
    }

    public static Block block(ResourceLocation id) {
//? if >=1.21.1 {
/*        return BuiltInRegistries.BLOCK.get(id);
*///?} else {
        return ForgeRegistries.BLOCKS.getValue(id);
//?}
    }

    public static ResourceLocation blockId(Block block) {
//? if >=1.21.1 {
/*        return BuiltInRegistries.BLOCK.getKey(block);
*///?} else {
        return ForgeRegistries.BLOCKS.getKey(block);
//?}
    }

    public static Iterable<Block> blocks() {
//? if >=1.21.1 {
/*        return BuiltInRegistries.BLOCK;
*///?} else {
        return ForgeRegistries.BLOCKS.getValues();
//?}
    }

    public static boolean sameItemData(ItemStack a, ItemStack b) {
//? if >=1.21.1 {
/*        return ItemStack.isSameItemSameComponents(a, b);
*///?} else {
        return ItemStack.isSameItemSameTags(a, b);
//?}
    }

    public static void setCustomName(ItemStack stack, Component name) {
//? if >=1.21.1 {
/*        stack.set(DataComponents.CUSTOM_NAME, name);
*///?} else {
        stack.setHoverName(name);
//?}
    }
}
