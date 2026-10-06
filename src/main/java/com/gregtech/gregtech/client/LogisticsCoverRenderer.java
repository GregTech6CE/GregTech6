package com.gregtech.gregtech.client;

import com.gregtech.gregtech.content.cover.CoverFaceCoordinates;
import com.gregtech.gregtech.content.logistics.LogisticsCoverHost;
import com.gregtech.gregtech.content.logistics.LogisticsCoverType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/** GT6 logistics covers on wire, core and bound wall faces, including CPU meter frames 0..10. */
public final class LogisticsCoverRenderer<T extends BlockEntity & LogisticsCoverHost>
        implements BlockEntityRenderer<T> {
    public LogisticsCoverRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                                 int packedLight, int packedOverlay) {
        renderFaces(be,be,pose,buffers,packedLight);
    }
    public static void renderFaces(BlockEntity be,LogisticsCoverHost host,PoseStack pose,MultiBufferSource buffers,int packedLight){
        for (Direction side : Direction.values()) {
            var stack = host.logisticsCovers().get(side);
            if (stack.isEmpty()) continue;
            var type = LogisticsCoverType.of(stack);
            if (type == null) continue;
            int light = be.getLevel() == null ? packedLight
                    : LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(side));
            var vc = buffers.getBuffer(RenderType.cutout());
            double out = 0;
            var base = ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gregtech", "block/machines/covers/logistics/base"));
            CoverSurfaceRenderer.draw(pose,vc,side,base,light,0);
            if (type.role() == LogisticsCoverType.Role.DISPLAY) {
                String name = switch (type) {
                    case CPU_LOGIC -> "cpu_logic";
                    case CPU_CONTROL -> "cpu_control";
                    case CPU_STORAGE -> "cpu_storage";
                    case CPU_CONVERSION -> "cpu_conversion";
                    default -> throw new IllegalStateException("Unknown CPU display " + type);
                };
                String prefix = "block/machines/covers/logistics/display/" + name + "/";
                drawSurface(pose, vc, side, prefix + "underlay", out + .0006, light);
                drawSurface(pose, vc, side, prefix + host.logisticsCovers().displayVisual(side),
                        out + .0008, light);
            } else {
                var itemModel = Minecraft.getInstance().getItemRenderer().getModel(stack, be.getLevel(), null, 0);
                var icon = itemModel.getParticleIcon(net.minecraftforge.client.model.data.ModelData.EMPTY);
                // Bus items use their original flat icon as the outward-facing plate.
                drawSurface(pose, vc, side, icon, out + .0006, light);
            }
        }
    }

    private static void drawSurface(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer vc,
                                    Direction side, String texture, double distance, int light) {
        drawSurface(pose, vc, side,
                ArmRenderHelper.getSprite(ResourceLocation.fromNamespaceAndPath("gregtech", texture)),
                distance, light);
    }

    private static void drawSurface(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer vc,
                                    Direction side, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
                                    double distance, int light) {
        for (int corner = 0; corner < 4; corner++) {
            double u = corner >= 2 ? 1 : 0;
            double v = corner == 1 || corner == 2 ? 1 : 0;
            var pos = CoverFaceCoordinates.to(side, u, v, distance);
            vc.vertex(pose.last().pose(), (float) pos.x, (float) pos.y, (float) pos.z)
                    .color(1f, 1f, 1f, 1f).uv(sprite.getU((float) (u * 16)),
                            sprite.getV((float) (v * 16)))
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(pose.last().normal(), side.getStepX(), side.getStepY(), side.getStepZ())
                    .endVertex();
        }
    }
}
