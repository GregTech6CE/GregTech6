package com.gregtech.gregtech.client;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialTextureSet;
import net.minecraft.resources.ResourceLocation;

/**
 * Crucible fill textures (GT6 {@code MultiTileEntitySmeltery} pass 5).
 * <p>
 * Solid content: fixed {@code OP.blockRaw} gray — {@code block/material_icons/metallic/blockraw.png}
 * (GT6 {@code BlockTextureDefault.get(Materials.Invalid, OP.blockRaw, CA_GRAY_64)}), tinted like vanadium raw ore.
 * Molten content: {@link MaterialFluidVisuals} → dedicated {@code block/fluids/*} or generic {@code molten} icon.
 */
public final class CrucibleContentIcons {
    /** Fixed gray solid fill — always metallic {@code blockraw}. */
    public static final ResourceLocation SOLID_GRAY_BASE =
            ResourceLocation.fromNamespaceAndPath("gregtech","block/material_icons/metallic/blockraw");
    public static final ResourceLocation SOLID_GRAY_OVERLAY =
            ResourceLocation.fromNamespaceAndPath("gregtech","block/material_icons/metallic/blockraw_overlay");

    private CrucibleContentIcons() {}

    /** Vanadium raw ore gray ({@code Materials.Vanadium} / GT6 {@code CA_GRAY_64}-like). */
    public static int solidGrayTint() {
        return Materials.Vanadium.resolve().getColor() | 0xFF000000;
    }

    public static MaterialTextureSet resolveSet(GTMaterial material) {
        return MaterialIcons.resolveTextureSet(material.resolve());
    }

    public static MaterialFluidVisuals.Appearance moltenAppearance(GTMaterial material) {
        return MaterialFluidVisuals.forMaterial(material);
    }

    /** @deprecated use {@link #moltenAppearance(GTMaterial)} */
    @Deprecated
    public static ResourceLocation moltenBase(GTMaterial material) {
        return moltenAppearance(material).baseTexture();
    }

    /** @deprecated use {@link #moltenAppearance(GTMaterial)} */
    @Deprecated
    public static ResourceLocation moltenOverlay(GTMaterial material) {
        return moltenAppearance(material).overlayTexture();
    }

    public static ResourceLocation blockIcon(MaterialTextureSet set, String prefix, boolean overlay) {
        String file = prefix + (overlay ? "_overlay" : "");
        return ResourceLocation.fromNamespaceAndPath("gregtech","block/material_icons/" + set.folder() + "/" + file);
    }
}
