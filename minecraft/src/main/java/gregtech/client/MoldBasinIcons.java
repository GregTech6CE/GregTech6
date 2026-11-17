package gregtech.client;

import gregtech.api.material.MaterialTextureSet;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

/** GT6 mold basin hull model (1px walls). Delegates geometry to {@link SmelteryModelHelper}. */
public final class MoldBasinIcons {
    private MoldBasinIcons() {}

    public static ResourceLocation sharedModelLocation(MaterialTextureSet set) {
        return SmelteryModelHelper.modelPath("mold_basin", set);
    }

    public static String sharedModelJson(MaterialTextureSet set) {
        return sharedModelJson(set, MaterialIcons::resourceExists);
    }

    public static String sharedModelJson(MaterialTextureSet set, Predicate<String> resourceExists) {
        return SmelteryModelHelper.hullModelJson(set, SmelteryModelHelper.BASIN_ELEMENTS_TINTED, null, resourceExists);
    }
}
