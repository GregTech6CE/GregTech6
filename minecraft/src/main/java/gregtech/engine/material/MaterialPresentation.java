package gregtech.engine.material;

import gregtech.api.material.*;
import net.minecraft.network.chat.Component;

/** Minecraft display boundary for the single shared material model. */
public final class MaterialPresentation {
    private MaterialPresentation() {}

    public static Component name(GTMaterial material) {
        return Component.translatableWithFallback(material.getTranslationKey(), material.getDisplayNameFallback());
    }

    /** Resolve plain host-rock names before legacy whole-name translations in either language. */
    public static Component formName(String prefix, GTMaterial material, Component legacyFallback) {
        // Server-produced Jade components must not depend on the server's active language.
        if ("rockGt".equals(prefix) && material.resolve().has(MaterialProperty.STONE))
            return Component.translatable("item.gt6ce.rock_gt_stone", name(material));
        String key = gregtech.engine.prefix.PrefixRegistry.sourceTranslationKey(prefix, material.getName());
        if (net.minecraft.locale.Language.getInstance().has(key)) return Component.translatable(key);
        // Preserve translated prefix/material fallbacks where the original whole-name key is absent.
        // language.code is supplied by Minecraft's active language resource, including on servers.
        if (!net.minecraft.locale.Language.getInstance()
                .getOrDefault("language.code")
                .startsWith("en_")) return legacyFallback;
        String generated = OriginalMaterialNameRules.name(prefix, material, m -> name(m).getString());
        return Component.translatableWithFallback(
                key, "%s", generated == null ? legacyFallback : Component.literal(generated));
    }
}
