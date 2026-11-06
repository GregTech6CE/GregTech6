package gregtech.content.registry;

import java.util.function.Supplier;
import net.minecraft.world.item.Item;

/**
 * Loader-neutral view of the item registries used by shared registration code.
 * Each platform implements it once; the shared definitions keep deciding what gets registered.
 */
public interface ContentRegistrar {
    <T extends Item> RegisteredItem<T> item(String namespace, String path, Supplier<T> factory);
}
