package gregtech.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

/** Fractional texture clipping and a one-tick transition between authoritative progress samples. */
final class MachineProgressRenderer {
    private float from, target;
    private long changedAt = -1;

    float sample(float progress) {
        long now = Util.getMillis();
        progress = Math.max(0, Math.min(1, progress));
        // A new recipe or reset must never interpolate backwards through the completed batch.
        if (changedAt < 0 || progress < target || progress == 0 || progress == 1) {
            from = target = progress;
            changedAt = now;
        } else if (progress != target) {
            from = interpolated(now);
            target = progress;
            changedAt = now;
        }
        return interpolated(now);
    }

    private float interpolated(long now) {
        float fraction = Math.max(0, Math.min(1, (now - changedAt) / 50.0F));
        return from + (target - from) * fraction;
    }

    void draw(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v,
              int width, int height, float progress) {
        drawFraction(graphics, texture, x, y, u, v, width, height, 256, 256, sample(progress));
    }

    static void drawFraction(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v,
                             int width, int height, int textureWidth, int textureHeight, float progress) {
        float filled = width * Math.max(0, Math.min(1, progress));
        if (texture == null || filled <= 0) return;
        // Integer GuiGraphics.blit/scissor bounds snap to GUI pixels. Clip geometry and UVs together instead.
        graphics.flush();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        var pose = graphics.pose().last().pose();
        float right = x + filled, bottom = y + height;
        float u0 = u / (float) textureWidth, u1 = (u + filled) / textureWidth;
        float v0 = v / (float) textureHeight, v1 = (v + height) / (float) textureHeight;
//? if >=1.21.1 {
        /*var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(pose, x, y, 0).setUv(u0, v0);
        buffer.addVertex(pose, x, bottom, 0).setUv(u0, v1);
        buffer.addVertex(pose, right, bottom, 0).setUv(u1, v1);
        buffer.addVertex(pose, right, y, 0).setUv(u1, v0);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
*///?} else {
        var buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(pose, x, y, 0).uv(u0, v0).endVertex();
        buffer.vertex(pose, x, bottom, 0).uv(u0, v1).endVertex();
        buffer.vertex(pose, right, bottom, 0).uv(u1, v1).endVertex();
        buffer.vertex(pose, right, y, 0).uv(u1, v0).endVertex();
        BufferUploader.drawWithShader(buffer.end());
//?}
        RenderSystem.disableBlend();
    }
}
