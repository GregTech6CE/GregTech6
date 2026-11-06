package gregtech.content.registry;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Loader-neutral view of the item and block registries used by shared registration code.
 * The shared definitions keep deciding what gets registered; {@link Shared} is the only
 * implementation and queues into the per-namespace deferred registers both loaders already own.
 */
public interface ContentRegistrar {
    /** The single adapter both platform entries use. */
    static ContentRegistrar shared() {
        return Shared.INSTANCE;
    }

    /** Queues a plain item that has no block. */
    <T extends Item> RegisteredItem<T> item(String namespace, String path, Supplier<T> factory);

    /**
     * Queues an item for an already-queued block. It lands in the block-item register, whose entries
     * client model setup and block-item colouring read.
     */
    <T extends Item> RegisteredItem<T> blockItem(String namespace, String path, Supplier<T> factory);

    <T extends Block> RegisteredBlock<T> block(
            String namespace, String path, Supplier<T> factory, Function<T, ? extends Item> itemFactory);

    /** Queues a block whose item comes from another path (or none at all). */
    <T extends Block> RegisteredBlock<T> blockWithoutItem(String namespace, String path, Supplier<T> factory);

    /** Queues a block and a plain {@link BlockItem} with the same path. */
    default <T extends Block> RegisteredBlock<T> block(String namespace, String path, Supplier<T> factory) {
        return block(namespace, path, factory, block -> new BlockItem(block, new Item.Properties()));
    }

    /** Queues into {@link GTItems} and {@link GTBlocks}, whose deferred registers are loader-specific. */
    final class Shared implements ContentRegistrar {
        private static final Shared INSTANCE = new Shared();

        private Shared() {}

        @Override
        public <T extends Item> RegisteredItem<T> item(String namespace, String path, Supplier<T> factory) {
            return new RegisteredItem<>(
                    ResourceLocation.fromNamespaceAndPath(namespace, path),
                    GTItems.items(namespace).register(path, factory));
        }

        @Override
        public <T extends Item> RegisteredItem<T> blockItem(String namespace, String path, Supplier<T> factory) {
            return new RegisteredItem<>(
                    ResourceLocation.fromNamespaceAndPath(namespace, path),
                    GTBlocks.blockItems(namespace).register(path, factory));
        }

        @Override
        public <T extends Block> RegisteredBlock<T> block(
                String namespace, String path, Supplier<T> factory, Function<T, ? extends Item> itemFactory) {
            var block = GTBlocks.blocks(namespace).register(path, factory);
            GTBlocks.blockItems(namespace).register(path, () -> itemFactory.apply(block.get()));
            return new RegisteredBlock<>(ResourceLocation.fromNamespaceAndPath(namespace, path), block);
        }

        @Override
        public <T extends Block> RegisteredBlock<T> blockWithoutItem(
                String namespace, String path, Supplier<T> factory) {
            return new RegisteredBlock<>(
                    ResourceLocation.fromNamespaceAndPath(namespace, path),
                    GTBlocks.blocks(namespace).register(path, factory));
        }
    }
}
