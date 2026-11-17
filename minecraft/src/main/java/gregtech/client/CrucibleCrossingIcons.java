package gregtech.client;

import gregtech.api.material.MaterialTextureSet;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

/** GT6 crossing model (low cruciform channel). Delegates geometry to {@link SmelteryModelHelper}. */
public final class CrucibleCrossingIcons {
    private CrucibleCrossingIcons() {}

    public static ResourceLocation sharedModelLocation(MaterialTextureSet set) {
        return SmelteryModelHelper.modelPath("crucible_crossing", set);
    }

    public static String sharedModelJson(MaterialTextureSet set) {
        return sharedModelJson(set, MaterialIcons::resourceExists);
    }

    public static String sharedModelJson(MaterialTextureSet set, Predicate<String> resourceExists) {
        return SmelteryModelHelper.hullModelJson(
                set, SmelteryModelHelper.CROSSING_ELEMENTS_TINTED, null, resourceExists);
    }
}
