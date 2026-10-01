package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/** Bake-time cache for tool layer models (iconsets + shared material icons). */
public final class GTToolModelResolver {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String[] VARIANTS = {"", "inventory", "normal"};

    private static Map<ResourceLocation, BakedModel> iconsetModels = Map.of();
    private static Map<ResourceLocation, BakedModel> materialModels = Map.of();
    @Nullable
    private static BakedModel missingModel;

    private GTToolModelResolver() {}

    public static void rebuild(Map<ResourceLocation, BakedModel> models, @Nullable BakedModel missing) {
        missingModel = missing;
        Map<ResourceLocation, BakedModel> iconsets = new HashMap<>();
        Map<ResourceLocation, BakedModel> materials = new HashMap<>();

        for (ResourceLocation icon : ToolIconSets.allRegisteredIcons()) {
            cacheIconset(iconsets, models, GTToolModel.iconsetModelId(icon));
            cacheIconset(iconsets, models, GTToolModel.iconsetOverlayModelId(icon));
        }

        PrefixRegistry.ensurePrefixesLoaded();
        for (MaterialTextureSet set : MaterialTextureSet.MODELED) {
            for (MaterialPrefix prefix : PrefixRegistry.all()) {
                cacheIconset(materials, models, MaterialIcons.sharedModelLocation(set, prefix));
            }
        }

        iconsetModels = Map.copyOf(iconsets);
        materialModels = Map.copyOf(materials);
        LOGGER.info("[gregtech] Tool model cache: {} iconset layers, {} material icons",
                iconsetModels.size(), materialModels.size());
    }

    private static void cacheIconset(Map<ResourceLocation, BakedModel> out,
                                     Map<ResourceLocation, BakedModel> models,
                                     ResourceLocation id) {
        BakedModel model = lookup(models, id);
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
        for (MaterialTextureSet fallback : MaterialTextureSet.MODELED) {
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

    @Nullable
    private static BakedModel lookup(Map<ResourceLocation, BakedModel> models, ResourceLocation id) {
        for (String variant : VARIANTS) {
            BakedModel model = models.get(new ModelResourceLocation(id, variant));
            if (isValid(model)) {
                return model;
            }
        }
        return isValid(models.get(id)) ? models.get(id) : null;
    }

    private static boolean isValid(@Nullable BakedModel model) {
        return model != null && model != missingModel;
    }
}
