package com.gregtech.gregtech.platform;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.StackWalker.Option;

/**
 * Platform shim. Forge has a class with the same name and the same methods.
 * Each method keeps the behavior of the NeoForge call it replaces.
 */
public final class Mc {
    private Mc() {}

    /** Names the logger after the calling class, as {@code LogUtils.getLogger()} did. */
    public static Logger logger() {
        Class<?> caller = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames.skip(1).findFirst().orElseThrow().getDeclaringClass());
        return LoggerFactory.getLogger(caller);
    }

    public static ResourceLocation gregtechId(String path) {
        return ResourceLocation.fromNamespaceAndPath("gregtech", path);
    }

    public static Item item(ResourceLocation id) {
        return BuiltInRegistries.ITEM.get(id);
    }

    public static Iterable<Block> blocks() {
        return BuiltInRegistries.BLOCK;
    }
}
