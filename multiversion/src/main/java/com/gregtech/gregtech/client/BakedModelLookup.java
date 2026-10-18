package com.gregtech.gregtech.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

/** Resolve a baked model without treating Minecraft's missing-model sentinel as success. */
public final class BakedModelLookup {
//? if >=1.21.1 {
    /*// Neo 1.21 models have typed keys; never accept the bakery missing-model sentinel.
    public static final ModelResourceLocation MISSING_MODEL = net.minecraft.client.resources.model.ModelBakery.MISSING_MODEL_VARIANT;
*///?} else {
    // Same key as the 1.20.1 bakery; keep lookups independent of its renderer bootstrap.
    public static final ModelResourceLocation MISSING_MODEL = ModelResourceLocation.vanilla("builtin/missing", "missing");
//?}
    private BakedModelLookup() {}

//? if >=1.21.1 {
    /*public static boolean isUsable(Map<ModelResourceLocation, BakedModel> models, BakedModel model) {
*///?} else {
    public static boolean isUsable(Map<ResourceLocation, BakedModel> models, BakedModel model) {
//?}
        return model != null && model != models.get(MISSING_MODEL);
    }

//? if >=1.21.1 {
    /*public static BakedModel find(Map<ModelResourceLocation, BakedModel> models, ResourceLocation id) {
*///?} else {
    public static BakedModel find(Map<ResourceLocation, BakedModel> models, ResourceLocation id) {
//?}
        for (String variant : new String[]{"", "inventory", "normal"}) {
            BakedModel model = models.get(new ModelResourceLocation(id, variant));
            if (isUsable(models, model)) return model;
        }
//? if >=1.21.1 {
        /*BakedModel model = models.get(ModelResourceLocation.standalone(id));
*///?} else {
        BakedModel model = models.get(id);
//?}
        return isUsable(models, model) ? model : null;
    }
}
