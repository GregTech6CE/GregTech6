package gregtech.client;

import gregtech.api.addon.platform.PartClientView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** Dispatches an addon part renderer. Parts without one are not drawn. */
public final class FunctionalPartRenderer implements BlockEntityRenderer<BlockEntity> {
    public FunctionalPartRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(BlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffer,
                                 int light, int overlay) {
        if (!(entity instanceof PartClientView view)) return;
        BlockEntityRenderer<BlockEntity> renderer = LayoutPartClients.renderer(view);
        if (renderer != null) renderer.render(entity, partialTick, pose, buffer, light, overlay);
    }

    @Override public boolean shouldRender(BlockEntity entity, Vec3 camera) {
        return entity instanceof PartClientView view && LayoutPartClients.renderer(view) != null;
    }
}
