package com.gregtech.gregtech.data;

import net.minecraft.resources.ResourceLocation;

/** Minecraft texture boundary; inherited definitions are the single shared fluid catalog. */
public class RegisteredFluids extends FluidCatalog {
    protected RegisteredFluids() {}
    public static ResourceLocation fluidTexture(String registryName) {
        return ResourceLocation.fromNamespaceAndPath("gregtech", "block/fluids/" + sanitizeTextureId(registryName));
    }
}
