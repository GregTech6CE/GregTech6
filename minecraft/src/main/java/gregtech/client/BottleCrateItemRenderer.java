package gregtech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gregtech.content.blockentity.inventory.BottleCrateBlockEntity;
import gregtech.content.registry.GTStorageContainers;
import gregtech.engine.stack.BlockEntityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
*///?} else {
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
//?}
public final class BottleCrateItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static BottleCrateItemRenderer instance;

    private BottleCrateItemRenderer(Minecraft client) {
        super(client.getBlockEntityRenderDispatcher(), client.getEntityModels());
    }

    public static BottleCrateItemRenderer instance() {
        if (instance == null) instance = new BottleCrateItemRenderer(Minecraft.getInstance());
        return instance;
    }

    static void models(ModelEvent.ModifyBakingResult event) {
        var models = BakedModelMap.of(event.getModels());
        for (var crate : GTStorageContainers.BOTTLE_CRATES) {
            var id = BuiltInRegistries.ITEM.getKey(crate.get().asItem());
            var model = models.get(id, "inventory");
            if (model != null && !model.isCustomRenderer()) models.put(id, "inventory", new CrateModel(model));
        }
    }

    @Override
    public void renderByItem(
            ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light,
            int overlay) {
        var client = Minecraft.getInstance();
        var renderer = client.getItemRenderer();
        if (!(renderer.getItemModelShaper().getItemModel(stack) instanceof CrateModel model)) return;

        // ItemRenderer has already applied the display transform. Cancel the second centering translation.
        pose.pushPose();
        pose.translate(.5, .5, .5);
        renderer.render(stack, ItemDisplayContext.NONE, false, pose, buffers, light, overlay, model.shell());
        pose.popPose();

        if (!BlockEntityData.has(stack)) return;
        var lookup = client.level != null ? client.level.registryAccess()
                : client.getConnection() != null ? client.getConnection().registryAccess()
                : RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var bottles = BottleCrateBlockEntity.readPackedBottles(stack, lookup);
        BottleCrateRenderer.renderBottles(bottles::getStackInSlot, pose, buffers, light);
    }

    private static final class CrateModel extends BakedModelWrapper<BakedModel> {
        private CrateModel(BakedModel shell) {
            super(shell);
        }

        private BakedModel shell() {
            return originalModel;
        }

        @Override
        public boolean isCustomRenderer() {
            return true;
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
            originalModel.applyTransform(context, pose, leftHand);
            return this;
        }
    }
}
