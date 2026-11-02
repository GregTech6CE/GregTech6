package gregtech.engine.multiblock;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/** A block factory explicitly bound to its controller type and structure; no runtime type inference. */
public record ControllerDeclaration<B extends Block>(
        ResourceLocation id, Supplier<B> block, ControllerBinding<?> binding) {
    public ControllerDeclaration {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(block, "block");
    }

    public static <B extends Block> ControllerDeclaration<B> multiblock(
            String namespace, String path, Supplier<B> block, ControllerBinding<?> binding) {
        return new ControllerDeclaration<>(
                ResourceLocation.fromNamespaceAndPath(namespace, path),
                block,
                Objects.requireNonNull(binding, "Multiblock structure binding"));
    }

    /** Single-block machines are declared explicitly and may not silently acquire a structure. */
    public static <B extends Block> ControllerDeclaration<B> single(String namespace, String path, Supplier<B> block) {
        return new ControllerDeclaration<>(ResourceLocation.fromNamespaceAndPath(namespace, path), block, null);
    }
}
