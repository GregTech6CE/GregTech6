package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.fluid.FluidTexturePolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/** Resource-pack-aware texture choice used by both world fluids and inventory previews. */
public final class FluidAppearance {
    private record Key(ResourceLocation preferred, FluidTexturePolicy.Kind kind) {}
    private static final Map<Key, ResourceLocation> CACHE = new ConcurrentHashMap<>();
    private static final Map<com.gregtech.gregtech.data.RegisteredFluids.FluidEntry, com.gregtech.gregtech.api.fluid.FluidVisualPolicy.Visual> VISUALS = new ConcurrentHashMap<>();
    private static ResourceManager resources;
    private FluidAppearance() {}
    public static synchronized void clearCache() { CACHE.clear(); VISUALS.clear(); resources = null; }
    public static synchronized com.gregtech.gregtech.api.fluid.FluidVisualPolicy.Visual appearance(com.gregtech.gregtech.data.RegisteredFluids.FluidEntry entry) {
        ResourceManager current = Minecraft.getInstance().getResourceManager();
        if(current != resources){CACHE.clear();VISUALS.clear();resources=current;}
        return VISUALS.computeIfAbsent(entry, value -> com.gregtech.gregtech.api.fluid.FluidVisualPolicy.select(value,
                id -> current.getResource(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"textures/"+id.getPath()+".png")).isPresent()));
    }
    public static synchronized ResourceLocation texture(ResourceLocation preferred, FluidTexturePolicy.Kind kind) {
        ResourceManager current = Minecraft.getInstance().getResourceManager();
        if (current != resources) { CACHE.clear(); VISUALS.clear(); resources = current; }
        return CACHE.computeIfAbsent(new Key(preferred, kind), key -> FluidTexturePolicy.select(
                key.preferred(), key.kind(), id -> current.getResource(
                        ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/" + id.getPath() + ".png")).isPresent()));
    }
}
