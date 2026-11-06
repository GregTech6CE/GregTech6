package gregtech.platform.neoforge.registry;

import gregtech.content.registry.ContentRegistrar;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.RegisteredBlock;
import gregtech.content.registry.RegisteredItem;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** NeoForge adapter over the shared per-namespace deferred registers. */
public record NeoContentRegistrar() implements ContentRegistrar {
    @Override
    public <T extends Item> RegisteredItem<T> item(String namespace, String path, Supplier<T> factory) {
        return new RegisteredItem<>(
                ResourceLocation.fromNamespaceAndPath(namespace, path),
                GTItems.items(namespace).register(path, factory));
    }

    @Override
    public <T extends Block> RegisteredBlock<T> block(
            String namespace, String path, Supplier<T> factory, Function<T, ? extends Item> itemFactory) {
        var block = GTBlocks.blocks(namespace).register(path, factory);
        GTBlocks.blockItems(namespace).register(path, () -> itemFactory.apply(block.get()));
        return new RegisteredBlock<>(ResourceLocation.fromNamespaceAndPath(namespace, path), block);
    }

    @Override
    public <T extends Block> RegisteredBlock<T> blockWithoutItem(String namespace, String path, Supplier<T> factory) {
        return new RegisteredBlock<>(
                ResourceLocation.fromNamespaceAndPath(namespace, path),
                GTBlocks.blocks(namespace).register(path, factory));
    }
}
