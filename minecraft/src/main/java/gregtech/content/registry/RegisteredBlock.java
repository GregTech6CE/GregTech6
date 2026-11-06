package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/** A typed handle to one real block registration; never an alternate registry ID. */
public record RegisteredBlock<T extends Block>(ResourceLocation getId, Supplier<T> value) implements Supplier<T> {
    public RegisteredBlock {
        Objects.requireNonNull(getId);
        Objects.requireNonNull(value);
    }

    @Override
    public T get() {
        return value.get();
    }

    public boolean isPresent() {
        return BuiltInRegistries.BLOCK.containsKey(getId);
    }

    public boolean isBound() {
        return isPresent();
    }

    public static <T extends Block> RegisteredBlock<T> wrap(String path, Supplier<T> holder) {
        return new RegisteredBlock<>(id(path), holder);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Block> RegisteredBlock<T> reference(String path) {
        var key = id(path);
        return new RegisteredBlock<>(key, () -> (T) BuiltInRegistries.BLOCK.get(key));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(REGISTRY_NAMESPACE, path);
    }
}
