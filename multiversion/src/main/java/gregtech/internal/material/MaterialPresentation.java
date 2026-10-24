package gregtech.internal.material;

import net.minecraft.network.chat.Component;

/** Minecraft display boundary for the single shared material model. */
public final class MaterialPresentation {
    private MaterialPresentation() {}

    public static Component name(GTMaterial material) {
        return Component.translatableWithFallback(material.getTranslationKey(), material.getDisplayNameFallback());
    }

    /** Original names, with the requested plain-stone Chinese display before the source fallback. */
    public static Component formName(String prefix, GTMaterial material, Component legacyFallback) {
        // The patch calls every rock "bearing", including ordinary STONE materials.
        // Follow GT6 LanguageHandler's STONE branch for the requested Chinese display:
        // retain the exact material translation and derive its rock suffix from the patch.
        if (net.minecraft.locale.Language.getInstance().getOrDefault("language.code").startsWith("zh_")
                && "rockGt".equals(prefix) && material.resolve().has(MaterialProperty.STONE)
                && "%s Rock".equals(OriginalMaterialNameRules.name(prefix, material, ignored -> "%s", false)))
            return Component.translatable("item.gt6ce.rock_gt_stone", name(material));
        String key = gregtech.internal.prefix.PrefixRegistry.sourceTranslationKey(prefix, material.getName());
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
