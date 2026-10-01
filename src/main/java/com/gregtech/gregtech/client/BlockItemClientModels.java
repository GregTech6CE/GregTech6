package com.gregtech.gregtech.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** Shared block / block-item model aliasing for 3D inventory display. */
public final class BlockItemClientModels {
    private BlockItemClientModels() {}

    public static void aliasItemInventory(Map<ResourceLocation, BakedModel> models, ResourceLocation itemId,
                                        BakedModel itemModel) {
        models.put(new ModelResourceLocation(itemId, "inventory"), itemModel);
    }

    public static void alias(Map<ResourceLocation, BakedModel> models, ResourceLocation registryId,
                             ResourceLocation blockModelId, BakedModel blockModel, BakedModel itemModel) {
        models.put(new ModelResourceLocation(blockModelId, ""), blockModel);
        models.put(new ModelResourceLocation(registryId, ""), blockModel);
        models.put(new ModelResourceLocation(registryId, "inventory"), itemModel);
        models.put(new ModelResourceLocation(registryId, "waterlogged=false"), blockModel);
        models.put(new ModelResourceLocation(registryId, "waterlogged=true"), blockModel);
        models.put(new ModelResourceLocation(blockModelId, "waterlogged=false"), blockModel);
        models.put(new ModelResourceLocation(blockModelId, "waterlogged=true"), blockModel);
    }

    public static BakedModel asBlockItem(BakedModel shared) {
        return new BlockItemDisplayBakedModel(shared);
    }

    public static BakedModel asBlockItem(BakedModel shared, int materialColor) {
        return new MaterialBlockBakedModel(shared);
    }
}
