package gregtech.client;

import gregtech.engine.fluid.FluidTexturePolicy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** Resource-pack-aware texture choice used by both world fluids and inventory previews. */
public final class FluidAppearance {
    private record Key(ResourceLocation preferred, FluidTexturePolicy.Kind kind) {}

    private static final Map<Key, ResourceLocation> CACHE = new ConcurrentHashMap<>();
    private static final Map<
                    gregtech.content.data.RegisteredFluids.FluidEntry, gregtech.engine.fluid.FluidVisualPolicy.Visual>
            VISUALS = new ConcurrentHashMap<>();
    private static ResourceManager resources;

    private FluidAppearance() {}

    public static synchronized void clearCache() {
        CACHE.clear();
        VISUALS.clear();
        resources = null;
    }

    public static synchronized gregtech.engine.fluid.FluidVisualPolicy.Visual appearance(
            gregtech.content.data.RegisteredFluids.FluidEntry entry) {
        ResourceManager current = Minecraft.getInstance().getResourceManager();
        if (current != resources) {
            CACHE.clear();
            VISUALS.clear();
            resources = current;
        }
        return VISUALS.computeIfAbsent(
                entry,
                value -> gregtech.engine.fluid.FluidVisualPolicy.select(
                        value,
                        id -> current.getResource(ResourceLocation.fromNamespaceAndPath(
                                        id.getNamespace(), "textures/" + id.getPath() + ".png"))
                                .isPresent()));
    }

    public static synchronized ResourceLocation texture(ResourceLocation preferred, FluidTexturePolicy.Kind kind) {
        ResourceManager current = Minecraft.getInstance().getResourceManager();
        if (current != resources) {
            CACHE.clear();
            VISUALS.clear();
            resources = current;
        }
        return CACHE.computeIfAbsent(
                new Key(preferred, kind),
                key -> FluidTexturePolicy.select(
                        key.preferred(),
                        key.kind(),
                        id -> current.getResource(ResourceLocation.fromNamespaceAndPath(
                                        id.getNamespace(), "textures/" + id.getPath() + ".png"))
                                .isPresent()));
    }
}
