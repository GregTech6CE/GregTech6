package com.gregtech.gregtech.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import java.util.Objects;
import java.util.function.Supplier;

/** A typed handle to one real block registration; never an alternate registry ID. */
public record RegisteredBlock<T extends Block>(ResourceLocation getId, Supplier<T> value) implements Supplier<T> {
    public RegisteredBlock { Objects.requireNonNull(getId); Objects.requireNonNull(value); }
    @Override public T get() { return value.get(); }
    public boolean isPresent() { return BuiltInRegistries.BLOCK.containsKey(getId); }
    public boolean isBound() { return isPresent(); }
    public static <T extends Block> RegisteredBlock<T> register(String path, Supplier<T> factory) {
        return register(path, factory, block -> new BlockItem(block, new Item.Properties()));
    }
    public static <T extends Block> RegisteredBlock<T> register(String path, Supplier<T> factory,
                                                               java.util.function.Function<T, ? extends Item> itemFactory) {
        var holder = GTBlocks.BLOCKS.register(path, factory);
        GTBlocks.BLOCK_ITEMS.register(path, () -> itemFactory.apply(holder.get()));
        return new RegisteredBlock<>(id(path), holder);
    }
    public static <T extends Block> RegisteredBlock<T> registerBlock(String path, Supplier<T> factory) {
        return new RegisteredBlock<>(id(path), GTBlocks.BLOCKS.register(path, factory));
    }
    public static <T extends Block> RegisteredBlock<T> wrap(String path, Supplier<T> holder) {
        return new RegisteredBlock<>(id(path), holder);
    }
    @SuppressWarnings("unchecked")
    public static <T extends Block> RegisteredBlock<T> reference(String path) {
        var key = id(path);
        return new RegisteredBlock<>(key, () -> (T) BuiltInRegistries.BLOCK.get(key));
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("gregtech", path); }
}
