package com.gregtech.gregtech.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.block.model.ItemTransforms;

import java.util.Map;

/** Block-item display transforms (inventory / first-person / third-person). */
public final class BlockItemModelHelper {
    private static ItemTransforms BLOCK_ITEM_TRANSFORMS = ItemTransforms.NO_TRANSFORMS;

    private BlockItemModelHelper() {}

    public static void captureFromVanillaBlockItem(Map<ModelResourceLocation, BakedModel> models) {
        // Every bake (including F3+T / resource-pack changes) owns new transforms.
        BLOCK_ITEM_TRANSFORMS = ItemTransforms.NO_TRANSFORMS;
        for (ResourceLocation id : new ResourceLocation[]{
                ResourceLocation.fromNamespaceAndPath("minecraft", "iron_block"),
                ResourceLocation.fromNamespaceAndPath("minecraft", "stone"),
                ResourceLocation.fromNamespaceAndPath("minecraft", "dirt")
        }) {
            BakedModel model = models.get(new ModelResourceLocation(id, "inventory"));
            if (model!=null && model!=models.get(net.minecraft.client.resources.model.ModelBakery.MISSING_MODEL_VARIANT) && model.isGui3d()
                    && model.getTransforms() != ItemTransforms.NO_TRANSFORMS) {
                BLOCK_ITEM_TRANSFORMS = model.getTransforms();
                return;
            }
        }
        BakedModel cube = models.get(new ModelResourceLocation(
                ResourceLocation.fromNamespaceAndPath("minecraft", "block/cube_all"), ""));
        if (cube!=null && cube!=models.get(net.minecraft.client.resources.model.ModelBakery.MISSING_MODEL_VARIANT) && cube.getTransforms() != ItemTransforms.NO_TRANSFORMS) {
            BLOCK_ITEM_TRANSFORMS = cube.getTransforms();
        }
    }

    public static ItemTransforms blockItemTransforms() {
        return BLOCK_ITEM_TRANSFORMS;
    }
}
