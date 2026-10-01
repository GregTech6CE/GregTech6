package com.gregtech.gregtech.api.fluid;

import net.minecraft.resources.ResourceLocation;
import java.util.function.Predicate;

/** Shared, testable fallback policy; callers supply the current resource-pack view. */
public final class FluidTexturePolicy {
    public enum Kind {
        LIQUID("fluid/molten"), GAS("fluid/gas"), MOLTEN("metallic/molten"), PLASMA("plasma/gas");
        private final String texture;
        Kind(String texture) { this.texture = texture; }
        public ResourceLocation standard() { return ResourceLocation.fromNamespaceAndPath("gregtech", "block/material_icons/" + texture); }
    }
    private FluidTexturePolicy() {}
    public static Kind kind(com.gregtech.gregtech.data.RegisteredFluids.FluidEntry entry) {
        String name = entry.registryName();
        if ((entry.flags() & com.gregtech.gregtech.data.RegisteredFluids.FluidFlags.PLASMA) != 0 || name.startsWith("plasma.")) return Kind.PLASMA;
        if (name.startsWith("gas.") || entry.gas()) return Kind.GAS;
        if (name.startsWith("liquid.")) return Kind.LIQUID;
        if (entry.textureMode() == com.gregtech.gregtech.data.RegisteredFluids.FluidTextureMode.GENERIC_MOLTEN || name.startsWith("molten.")) return Kind.MOLTEN;
        return entry.gas() ? Kind.GAS : Kind.LIQUID;
    }
    public static ResourceLocation select(ResourceLocation preferred, boolean molten, boolean gas,
                                          Predicate<ResourceLocation> exists) {
        return select(preferred, molten ? Kind.MOLTEN : gas ? Kind.GAS : Kind.LIQUID, exists);
    }
    public static ResourceLocation select(ResourceLocation preferred, Kind kind, Predicate<ResourceLocation> exists) {
        if (exists.test(preferred)) return preferred;
        ResourceLocation standard = kind.standard();
        if (exists.test(standard)) return standard;
        return ResourceLocation.withDefaultNamespace("block/water_still");
    }
}
