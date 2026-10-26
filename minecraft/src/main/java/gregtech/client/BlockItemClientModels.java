package gregtech.client;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** Shared block / block-item model aliasing for 3D inventory display. */
public final class BlockItemClientModels {
    private BlockItemClientModels() {}

//? if >=1.21.1 {
    /*public static void aliasItemInventory(Map<ModelResourceLocation, BakedModel> models, ResourceLocation itemId,
*///?} else {
    public static void aliasItemInventory(Map<ResourceLocation, BakedModel> models, ResourceLocation itemId,
//?}
                                        BakedModel itemModel) {
        models.put(new ModelResourceLocation(itemId, "inventory"), itemModel);
    }

//? if >=1.21.1 {
    /*public static void alias(Map<ModelResourceLocation, BakedModel> models, ResourceLocation registryId,
*///?} else {
    public static void alias(Map<ResourceLocation, BakedModel> models, ResourceLocation registryId,
//?}
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

//? if >=1.21.1 {
//?} else {
    public static BakedModel asBlockItem(BakedModel shared, int materialColor) {
        return new MaterialBlockBakedModel(shared);
    }
//?}
}
