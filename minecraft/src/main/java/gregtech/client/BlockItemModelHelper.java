package gregtech.client;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/** Block-item display transforms (inventory / first-person / third-person). */
public final class BlockItemModelHelper {
    private static volatile ItemTransforms BLOCK_ITEM_TRANSFORMS = ItemTransforms.NO_TRANSFORMS;

    private BlockItemModelHelper() {}

    public static void captureFromVanillaBlockItem(BakedModelMap models) {
        // Every bake (including F3+T / resource-pack changes) owns new transforms.
        BLOCK_ITEM_TRANSFORMS = ItemTransforms.NO_TRANSFORMS;
        for (ResourceLocation id : new ResourceLocation[] {
            ResourceLocation.fromNamespaceAndPath("minecraft", "iron_block"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "stone"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "dirt")
        }) {
            BakedModel model = models.get(id, "inventory");
            if (models.isUsable(model) && model.isGui3d() && model.getTransforms() != ItemTransforms.NO_TRANSFORMS) {
                BLOCK_ITEM_TRANSFORMS = model.getTransforms();
                return;
            }
        }
    }

    public static ItemTransforms blockItemTransforms() {
        return BLOCK_ITEM_TRANSFORMS;
    }
}
