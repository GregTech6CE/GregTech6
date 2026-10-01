package com.gregtech.gregtech.api.material;

import net.minecraft.network.chat.Component;

/** Minecraft display boundary for the single shared material model. */
public final class MaterialPresentation {
    private MaterialPresentation() {}

    public static Component name(GTMaterial material) {
        return Component.translatable(material.getTranslationKey(), material.getDisplayNameFallback());
    }
}
