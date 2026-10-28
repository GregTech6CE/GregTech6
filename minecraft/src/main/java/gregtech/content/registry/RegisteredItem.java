package gregtech.content.registry;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Common typed access to a single item registration. */
public record RegisteredItem<T extends Item>(ResourceLocation getId, Supplier<T> value) implements Supplier<T> {
    @Override
    public T get() {
        return value.get();
    }

    public boolean isPresent() {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(getId);
    }

    public boolean isBound() {
        return isPresent();
    }

    public static <T extends Item> RegisteredItem<T> register(String path, Supplier<T> factory) {
        return new RegisteredItem<>(
                ResourceLocation.fromNamespaceAndPath("gt6ce", path), GTItems.ITEMS.register(path, factory));
    }
}
