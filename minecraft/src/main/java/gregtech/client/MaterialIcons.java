package gregtech.client;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialTextureSet;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.MaterialIconDefinitions;
import gregtech.platform.Mc;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
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
     * Selects the material sprite probe for {@code manager}: once the {@code gt6ce} namespace is
     * mounted, queries only the resource manager so pack {@code filter} rules are honoured; before
     * the first reload (or when {@code manager} is absent), falls back to the built-in classpath.
     */
    static Predicate<String> selectResourceProbe(@Nullable ResourceManager manager) {
        if (manager != null) {
            return MaterialIconDefinitions.selectMaterialProbe(
                    manager.getNamespaces(),
                    rel -> manager.getResource(Mc.gregtechId(rel)).isPresent());
        }
        return MaterialIconDefinitions::classpathResourceExists;
    }

    /** Whether the client can load a pack-relative material resource. */
    public static boolean resourceExists(String relativePath) {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager manager = minecraft == null ? null : minecraft.getResourceManager();
        return selectResourceProbe(manager).test(relativePath);
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
