package gregtech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gregtech.content.block.misc.ExtenderBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;

public final class ExtenderCoverRenderer implements BlockEntityRenderer<ExtenderBlockEntity> {
    public ExtenderCoverRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            ExtenderBlockEntity blockEntity,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        for (var side : Direction.values()) PanelCoverRenderer.renderFace(blockEntity, side, pose, buffers, light);
    }
}
