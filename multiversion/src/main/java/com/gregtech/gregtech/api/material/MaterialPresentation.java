package com.gregtech.gregtech.api.material;

import net.minecraft.network.chat.Component;

/** Minecraft display boundary for the single shared material model. */
public final class MaterialPresentation {
    private MaterialPresentation() {}

    public static Component name(GTMaterial material) {
        return Component.translatableWithFallback(material.getTranslationKey(), material.getDisplayNameFallback());
    }

    /** Whole original translations win; source rules only supply the missing-name fallback. */
    public static Component formName(String prefix, GTMaterial material, Component legacyFallback) {
        String key = com.gregtech.gregtech.api.prefix.PrefixRegistry.sourceTranslationKey(prefix, material.getName());
        if (net.minecraft.locale.Language.getInstance().has(key)) return Component.translatable(key);
        // Preserve translated prefix/material fallbacks where the original whole-name key is absent.
        // language.code is supplied by Minecraft's active language resource, including on servers.
        if (!net.minecraft.locale.Language.getInstance().getOrDefault("language.code").startsWith("en_"))
            return legacyFallback;
        String generated = OriginalMaterialNameRules.name(prefix, material, m -> name(m).getString());
        return Component.translatableWithFallback(key, "%s",
                generated == null ? legacyFallback : Component.literal(generated));
    }
}
