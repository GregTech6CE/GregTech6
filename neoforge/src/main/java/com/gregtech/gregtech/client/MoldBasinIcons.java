package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.material.MaterialTextureSet;
import net.minecraft.resources.ResourceLocation;

/** GT6 mold basin hull model (1px walls). Delegates geometry to {@link SmelteryModelHelper}. */
public final class MoldBasinIcons {
    private MoldBasinIcons() {}

    public static ResourceLocation sharedModelLocation(MaterialTextureSet set) {
        return SmelteryModelHelper.modelPath("mold_basin", set);
    }

    public static String sharedModelJson(MaterialTextureSet set) {
        return SmelteryModelHelper.compositeModelJson(set,
                SmelteryModelHelper.BASIN_ELEMENTS_TINTED,
                SmelteryModelHelper.overlayOnly(SmelteryModelHelper.BASIN_ELEMENTS_TINTED));
    }
}
