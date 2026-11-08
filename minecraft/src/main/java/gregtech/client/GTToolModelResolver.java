package gregtech.client;

import com.mojang.logging.LogUtils;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialTextureSet;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.prefix.PrefixRegistry;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/** Bake-time cache for tool layer models (iconsets + shared material icons). */
public final class GTToolModelResolver {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile Map<ResourceLocation, BakedModel> iconsetModels = Map.of();
    private static volatile Map<ResourceLocation, BakedModel> materialModels = Map.of();

    private GTToolModelResolver() {}

    public static void rebuild(BakedModelMap models) {
        Map<ResourceLocation, BakedModel> iconsets = new HashMap<>();
        Map<ResourceLocation, BakedModel> materials = new HashMap<>();

        for (ResourceLocation icon : ToolIconSets.allRegisteredIcons()) {
            cacheIconset(iconsets, models, GTToolModel.iconsetModelId(icon));
            cacheIconset(iconsets, models, GTToolModel.iconsetOverlayModelId(icon));
        }

        PrefixRegistry.ensurePrefixesLoaded();
        for (MaterialTextureSet set : MaterialTextureSet.modeled()) {
            for (MaterialPrefix prefix : PrefixRegistry.all()) {
                cacheIconset(materials, models, MaterialIcons.sharedModelLocation(set, prefix));
            }
        }

        iconsetModels = Map.copyOf(iconsets);
        materialModels = Map.copyOf(materials);
        LOGGER.info(
                "[gregtech] Tool model cache: {} iconset layers, {} material icons",
                iconsetModels.size(),
                materialModels.size());
    }

    private static void cacheIconset(Map<ResourceLocation, BakedModel> out, BakedModelMap models, ResourceLocation id) {
        BakedModel model = models.find(id);
        if (model != null) {
            out.put(id, model);
        }
    }

    @Nullable
    public static BakedModel iconsetModel(ResourceLocation modelId) {
        return iconsetModels.get(modelId);
    }

    @Nullable
    public static BakedModel materialModel(GTMaterial material, MaterialPrefix prefix) {
        MaterialTextureSet primary = MaterialIcons.resolveTextureSet(material);
        BakedModel model = materialModels.get(MaterialIcons.sharedModelLocation(primary, prefix));
        if (model != null) {
            return model;
        }
        for (MaterialTextureSet fallback : MaterialTextureSet.modeled()) {
            if (fallback == primary) {
                continue;
            }
            model = materialModels.get(MaterialIcons.sharedModelLocation(fallback, prefix));
            if (model != null) {
                return model;
            }
        }
        return null;
    }
}
