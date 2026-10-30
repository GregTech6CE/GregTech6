package gregtech.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Logical pixel dimensions stay constant when a resource pack supplies a higher-resolution PNG. */
public record GuiSprite(ResourceLocation texture, int width, int height) {
    public void draw(GuiGraphics graphics, int x, int y) {
        region(graphics, x, y, 0, 0, width, height);
    }

    public void region(GuiGraphics graphics, int x, int y, int u, int v, int w, int h) {
        graphics.blit(texture, x, y, u, v, w, h, width, height);
    }

    /** Four-pixel corners and borders, with a stretchable center. */
    public void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        int[] sourceX = {0, 4, width - 4}, sourceY = {0, 4, height - 4};
        int[] sourceW = {4, width - 8, 4}, sourceH = {4, height - 8, 4};
        int[] destX = {x, x + 4, x + w - 4}, destY = {y, y + 4, y + h - 4};
        int[] destW = {4, w - 8, 4}, destH = {4, h - 8, 4};
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 3; column++) {
                graphics.blit(
                        texture,
                        destX[column],
                        destY[row],
                        destW[column],
                        destH[row],
                        sourceX[column],
                        sourceY[row],
                        sourceW[column],
                        sourceH[row],
                        width,
                        height);
            }
    }
}
