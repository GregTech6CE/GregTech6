package gregtech.platform.forge;

import gregtech.content.registry.ContentRegistrar;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.RegisteredItem;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Forge item adapter over the shared per-namespace deferred registers. */
public record ForgeContentRegistrar() implements ContentRegistrar {
    @Override
    public RegisteredItem<Item> item(String namespace, String path, Supplier<? extends Item> factory) {
        return new RegisteredItem<>(
                ResourceLocation.fromNamespaceAndPath(namespace, path),
                GTItems.items(namespace).register(path, factory));
    }
}
