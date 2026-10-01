package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.material.MaterialTextureSet;
import net.minecraft.resources.ResourceLocation;

/** GT6 crossing model (low cruciform channel). Delegates geometry to {@link SmelteryModelHelper}. */
public final class CrucibleCrossingIcons {
    private CrucibleCrossingIcons() {}

    public static ResourceLocation sharedModelLocation(MaterialTextureSet set) {
        return SmelteryModelHelper.modelPath("crucible_crossing", set);
    }

    public static String sharedModelJson(MaterialTextureSet set) {
        return SmelteryModelHelper.compositeModelJson(set,
                SmelteryModelHelper.CROSSING_ELEMENTS_TINTED,
                SmelteryModelHelper.CROSSING_ELEMENTS_OVERLAY);
    }
}
