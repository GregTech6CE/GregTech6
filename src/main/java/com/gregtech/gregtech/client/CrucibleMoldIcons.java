package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.material.MaterialTextureSet;
import net.minecraft.resources.ResourceLocation;

/** GT6 mold hull model (hollow shell). Delegates geometry to {@link SmelteryModelHelper}. */
public final class CrucibleMoldIcons {
    private CrucibleMoldIcons() {}

    public static ResourceLocation sharedModelLocation(MaterialTextureSet set) {
        return SmelteryModelHelper.modelPath("crucible_mold", set);
    }

    public static String sharedModelJson(MaterialTextureSet set) {
        return SmelteryModelHelper.compositeModelJson(set,
                SmelteryModelHelper.MOLD_ELEMENTS_TINTED,
                SmelteryModelHelper.overlayOnly(SmelteryModelHelper.MOLD_ELEMENTS_TINTED));
    }
}
