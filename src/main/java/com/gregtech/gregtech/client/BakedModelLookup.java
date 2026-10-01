package com.gregtech.gregtech.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

/** Resolve a baked model without treating Minecraft's missing-model sentinel as success. */
public final class BakedModelLookup {
    // Same key as the 1.20.1 bakery; keep lookups independent of its renderer bootstrap.
    public static final ModelResourceLocation MISSING_MODEL = ModelResourceLocation.vanilla("builtin/missing", "missing");
    private BakedModelLookup() {}

    public static boolean isUsable(Map<ResourceLocation, BakedModel> models, BakedModel model) {
        return model != null && model != models.get(MISSING_MODEL);
    }

    public static BakedModel find(Map<ResourceLocation, BakedModel> models, ResourceLocation id) {
        for (String variant : new String[]{"", "inventory", "normal"}) {
            BakedModel model = models.get(new ModelResourceLocation(id, variant));
            if (isUsable(models, model)) return model;
        }
        BakedModel model = models.get(id);
        return isUsable(models, model) ? model : null;
    }
}
