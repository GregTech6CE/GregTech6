package com.gregtech.gregtech.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

/** Resolve a baked model without treating Minecraft's missing-model sentinel as success. */
public final class BakedModelLookup {
    // Neo 1.21 models have typed keys; never accept the bakery missing-model sentinel.
    public static final ModelResourceLocation MISSING_MODEL = net.minecraft.client.resources.model.ModelBakery.MISSING_MODEL_VARIANT;
    private BakedModelLookup() {}

    public static boolean isUsable(Map<ModelResourceLocation, BakedModel> models, BakedModel model) {
        return model != null && model != models.get(MISSING_MODEL);
    }

    public static BakedModel find(Map<ModelResourceLocation, BakedModel> models, ResourceLocation id) {
        for (String variant : new String[]{"", "inventory", "normal"}) {
            BakedModel model = models.get(new ModelResourceLocation(id, variant));
            if (isUsable(models, model)) return model;
        }
        BakedModel model = models.get(ModelResourceLocation.standalone(id));
        return isUsable(models, model) ? model : null;
    }
}
