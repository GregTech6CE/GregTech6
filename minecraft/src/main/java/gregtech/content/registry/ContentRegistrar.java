package gregtech.content.registry;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Loader-neutral view of the item and block registries used by shared registration code.
 * Each platform implements it once; the shared definitions keep deciding what gets registered.
 */
public interface ContentRegistrar {
    <T extends Item> RegisteredItem<T> item(String namespace, String path, Supplier<T> factory);

    <T extends Block> RegisteredBlock<T> block(
            String namespace, String path, Supplier<T> factory, Function<T, ? extends Item> itemFactory);

    /** Queues a block whose item comes from another path (or none at all). */
    <T extends Block> RegisteredBlock<T> blockWithoutItem(String namespace, String path, Supplier<T> factory);

    /** Queues a block and a plain {@link BlockItem} with the same path. */
    default <T extends Block> RegisteredBlock<T> block(String namespace, String path, Supplier<T> factory) {
        return block(namespace, path, factory, block -> new BlockItem(block, new Item.Properties()));
    }
}
