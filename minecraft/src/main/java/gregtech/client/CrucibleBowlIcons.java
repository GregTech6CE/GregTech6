package gregtech.client;

import gregtech.api.material.MaterialTextureSet;
import net.minecraft.resources.ResourceLocation;

/** GT6 crucible bowl hull model (2px walls). Delegates geometry to {@link SmelteryModelHelper}. */
public final class CrucibleBowlIcons {
    private CrucibleBowlIcons() {}

    public static ResourceLocation sharedModelLocation(MaterialTextureSet set) {
        return SmelteryModelHelper.modelPath("crucible_bowl", set);
    }

    public static String sharedModelJson(MaterialTextureSet set) {
        return SmelteryModelHelper.hullModelJson(set, SmelteryModelHelper.BOWL_ELEMENTS_TINTED, null);
    }
}
