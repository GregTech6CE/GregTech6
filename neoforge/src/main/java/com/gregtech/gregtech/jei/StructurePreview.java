package com.gregtech.gregtech.jei;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import java.util.Map;

/** Renders actual baked block models without creating a client world or ticking block entities. */
public final class StructurePreview {
    private StructurePreview() {}

    public static void draw(GuiGraphics graphics, Map<BlockPos, ItemStack> blocks, PreviewCamera camera, Integer layer) {
        draw(graphics, blocks, camera, layer, 0, 20, 176, 128);
    }

    public static void draw(GuiGraphics graphics, Map<BlockPos, ItemStack> blocks, PreviewCamera camera, Integer layer,
                            int left, int top, int right, int bottom) {
        if (blocks.isEmpty()) return;
        int minX = blocks.keySet().stream().mapToInt(BlockPos::getX).min().orElse(0);
        int maxX = blocks.keySet().stream().mapToInt(BlockPos::getX).max().orElse(0);
        int minY = blocks.keySet().stream().mapToInt(BlockPos::getY).min().orElse(0);
        int maxY = blocks.keySet().stream().mapToInt(BlockPos::getY).max().orElse(0);
        int minZ = blocks.keySet().stream().mapToInt(BlockPos::getZ).min().orElse(0);
        int maxZ = blocks.keySet().stream().mapToInt(BlockPos::getZ).max().orElse(0);
        float width = (maxX - minX + maxZ - minZ + 2) * 0.7072f;
        float height = (maxY - minY + 1) * 0.8661f + width * 0.5f;
        float scale = Math.min((right - left - 38f) / width, (bottom - top - 17f) / height) * (float)camera.scale();
        var client = Minecraft.getInstance();
        graphics.flush();
        var viewport = PreviewViewport.screenBounds(graphics.pose().last().pose(), left, top, right, bottom);
        graphics.enableScissor(viewport.left(), viewport.top(), viewport.right(), viewport.bottom());
        var pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate((left + right) / 2f + camera.x(), (top + bottom) / 2f + 3 + camera.y(), 150);
            PreviewTransforms.scaleForGui(pose, scale);
            pose.mulPose(Axis.XP.rotationDegrees((float)camera.pitch()));
            pose.mulPose(Axis.YP.rotationDegrees((float)camera.yaw()));
            pose.translate(-(minX + maxX + 1) / 2f, -(minY + maxY + 1) / 2f, -(minZ + maxZ + 1) / 2f);
            RenderSystem.setShaderLights(new org.joml.Vector3f(0,1,0), new org.joml.Vector3f(0,1,0));
            RenderSystem.enableDepthTest();
            var buffers = client.renderBuffers().bufferSource();
            var lighting = new java.util.HashMap<net.minecraft.client.renderer.RenderType, PreviewLighting>();
            net.minecraft.client.renderer.MultiBufferSource previewBuffers = type ->
                    lighting.computeIfAbsent(type, key -> new PreviewLighting(buffers.getBuffer(key)));
            for (var entry : blocks.entrySet()) {
                if (layer != null && entry.getKey().getY() != layer) continue;
                if (!(entry.getValue().getItem() instanceof BlockItem item)) continue;
                var pos = entry.getKey();
                pose.pushPose();
                pose.translate(pos.getX(), pos.getY(), pos.getZ());
                client.getBlockRenderer().renderSingleBlock(item.getBlock().defaultBlockState(), pose, previewBuffers,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            buffers.endBatch();
        } finally {
            pose.popPose();
            Lighting.setupFor3DItems();
            RenderSystem.disableDepthTest();
            graphics.disableScissor();
        }
    }
}
