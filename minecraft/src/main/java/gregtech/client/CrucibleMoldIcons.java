package gregtech.client;

import gregtech.api.material.MaterialTextureSet;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

/** GT6 mold hull model (hollow shell). Delegates geometry to {@link SmelteryModelHelper}. */
public final class CrucibleMoldIcons {
    private CrucibleMoldIcons() {}

    public static ResourceLocation sharedModelLocation(MaterialTextureSet set) {
        return SmelteryModelHelper.modelPath("crucible_mold", set);
    }

    public static String sharedModelJson(MaterialTextureSet set) {
        return sharedModelJson(set, MaterialIcons::resourceExists);
    }

    public static String sharedModelJson(MaterialTextureSet set, Predicate<String> resourceExists) {
        return SmelteryModelHelper.hullModelJson(set, SmelteryModelHelper.MOLD_ELEMENTS_TINTED, null, resourceExists);
    }
}
