package gregtech.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

/** Filled geometry avoids the disconnected bars produced by a rotated pixel-chevron head. */
public final class MaterialProcessArrows {
    private MaterialProcessArrows() {}

    public static void head(GuiGraphics graphics, int color) {
        var vertices = graphics.bufferSource().getBuffer(RenderType.gui());
        var matrix = graphics.pose().last().pose();
        float[][] points = {{0, 0}, {-4, -2.2f}, {-4, 2.2f}, {-4, 2.2f}};
        for (float[] point : points) {
//? if >=1.21.1 {
            /*vertices.addVertex(matrix, point[0], point[1], 0)
                    .setColor((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >>> 24) & 255);
*///?} else {
            vertices.vertex(matrix, point[0], point[1], 0)
                    .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >>> 24) & 255).endVertex();
//?}
        }
    }
}
