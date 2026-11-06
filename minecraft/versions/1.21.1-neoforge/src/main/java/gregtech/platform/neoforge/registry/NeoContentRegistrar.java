package gregtech.platform.neoforge.registry;

import gregtech.content.registry.ContentRegistrar;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.RegisteredItem;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** NeoForge item adapter over the shared per-namespace deferred registers. */
public record NeoContentRegistrar() implements ContentRegistrar {
    @Override
    public <T extends Item> RegisteredItem<T> item(String namespace, String path, Supplier<T> factory) {
        return new RegisteredItem<>(
                ResourceLocation.fromNamespaceAndPath(namespace, path),
                GTItems.items(namespace).register(path, factory));
    }
}
