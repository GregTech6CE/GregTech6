package gregtech.client;

import gregtech.content.logistics.LogisticsCoverHost;
import gregtech.content.logistics.LogisticsCoverType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/** GT6 logistics covers on wire, core and bound wall faces, including CPU meter frames 0..10. */
public final class LogisticsCoverRenderer<T extends BlockEntity & LogisticsCoverHost>
        implements BlockEntityRenderer<T> {

    public LogisticsCoverRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack pose, MultiBufferSource buffers,
                                 int packedLight, int packedOverlay) {
        renderFaces(blockEntity, blockEntity, pose, buffers, packedLight);
    }

    public static void renderFaces(BlockEntity blockEntity, LogisticsCoverHost host, PoseStack pose,
                                   MultiBufferSource buffers, int packedLight) {
        for (Direction side : Direction.values()) {
            var stack = host.logisticsCovers().get(side);
            if (stack.isEmpty()) continue;
            var type = LogisticsCoverType.of(stack);
            if (type == null) continue;
            int light = CoverSurfaceRenderer.light(blockEntity, side, packedLight);
            var vertices = buffers.getBuffer(RenderType.cutout());
            double surfaceOffset = 0;
            var base = ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath(
                    "gt6ce", "block/machines/covers/logistics/base"));
            CoverSurfaceRenderer.attachment(blockEntity, side, pose, vertices, base, light, -1);
            CoverSurfaceRenderer.draw(blockEntity, pose, vertices, side, base, light, 0);
            if (type.role() == LogisticsCoverType.Role.DISPLAY) {
                String name = switch (type) {
                    case CPU_LOGIC -> "cpu_logic";
                    case CPU_CONTROL -> "cpu_control";
                    case CPU_STORAGE -> "cpu_storage";
                    case CPU_CONVERSION -> "cpu_conversion";
                    default -> throw new IllegalStateException("Unknown CPU display " + type);
                };
                String prefix = "block/machines/covers/logistics/display/" + name + "/";
                drawSurface(blockEntity, pose, vertices, side, prefix + "underlay", surfaceOffset + .0006, light);
                drawSurface(blockEntity, pose, vertices, side, prefix + host.logisticsCovers().displayVisual(side),
                        surfaceOffset + .0008, light);
            } else {
                var itemModel = Minecraft.getInstance().getItemRenderer().getModel(stack, blockEntity.getLevel(), null, 0);
//? if >=1.21.1 {
                /*var icon = itemModel.getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData.EMPTY);
*///?} else {
                var icon = itemModel.getParticleIcon(net.minecraftforge.client.model.data.ModelData.EMPTY);
//?}
                // Bus items use their original flat icon as the outward-facing plate.
                drawSurface(blockEntity, pose, vertices, side, icon, surfaceOffset + .0006, light);
            }
        }
    }

    private static void drawSurface(BlockEntity owner, PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer vertices,
            Direction side, String texture, double distance, int light) {
        drawSurface(owner, pose, vertices, side,
                ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gt6ce", texture)),
                distance, light);
    }

    private static void drawSurface(BlockEntity owner, PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer vertices,
            Direction side, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
            double distance, int light) {
        CoverSurfaceRenderer.drawAt(owner, pose, vertices, side, sprite, light,
                CoverSurfaceRenderer.distance(0) + distance, -1);
    }
}
