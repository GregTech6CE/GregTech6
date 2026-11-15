package gregtech.client;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialTextureSet;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.MaterialIconDefinitions;
import gregtech.platform.Mc;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves GregTech material icon paths under {@code textures/item/material_icons/{set}/{prefix}.png}.
 */

/** Neo resource identifiers over the shared original icon definitions. */
public final class MaterialIcons {
    private MaterialIcons() {}

    public static MaterialTextureSet resolveTextureSet(GTMaterial material) {
        return MaterialIconDefinitions.resolveTextureSet(material);
    }

    /**
     * Whether the client can load a pack-relative resource. The resource manager sees resource-pack
     * overrides once a reload has run; before the first reload, and while the shared model pack is
     * being built, the built-in classpath copy is the only safe source.
     */
    public static boolean resourceExists(String relativePath) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!SharedInventoryModelPack.isBuilding()
                && minecraft != null
                && minecraft.getResourceManager() != null
                && minecraft
                        .getResourceManager()
                        .getResource(Mc.gregtechId(relativePath))
                        .isPresent()) {
            return true;
        }
        return MaterialIconDefinitions.classpathResourceExists(relativePath);
    }

    /** Sprite location for this set, following the inheritance chain to the set that ships it. */
    public static ResourceLocation resolveTexture(MaterialTextureSet set, MaterialPrefix prefix, boolean overlay) {
        MaterialTextureSet owner = resolveItemTextureOwner(set, prefix, overlay);
        return texture(owner == null ? set : owner, prefix, overlay);
    }

    @Nullable
    public static MaterialTextureSet resolveItemTextureOwner(
            MaterialTextureSet set, MaterialPrefix prefix, boolean overlay) {
        return MaterialIconDefinitions.resolveItemTextureOwner(set, prefix, overlay, MaterialIcons::resourceExists);
    }

    public static ResourceLocation texture(MaterialTextureSet set, MaterialPrefix prefix, boolean overlay) {
        return Mc.gregtechId(MaterialIconDefinitions.texturePath(set, prefix, overlay));
    }

    /**
     * Baked model id for {@code models/item/material/{set}/{prefix}.json}.
     * Path is relative to the {@code models/} root, so {@code item/} is required.
     */
    public static ResourceLocation sharedModelLocation(MaterialTextureSet set, MaterialPrefix prefix) {
        return Mc.gregtechId(MaterialIconDefinitions.modelPath(set, prefix));
    }

    public static String sharedModelId(GTMaterial material, MaterialPrefix prefix) {
        return MaterialIconDefinitions.modelPath(resolveTextureSet(material), prefix);
    }
}
