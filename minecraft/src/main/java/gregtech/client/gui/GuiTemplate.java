package gregtech.client.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/** Presentation only: the Menu remains authoritative for slots, inventory and interaction. */
public record GuiTemplate(
        int width,
        int height,
        GuiSprite background,
        List<Element> elements,
        Progress progress,
        boolean brand,
        boolean menuSlots) {
    public record Element(GuiSprite sprite, int x, int y, int columns, int rows, int stepX, int stepY) {
        void draw(GuiGraphics graphics, int left, int top) {
            for (int row = 0; row < rows; row++)
                for (int column = 0; column < columns; column++)
                    sprite.draw(graphics, left + x + column * stepX, top + y + row * stepY);
        }
    }

    /** Both states occupy equal halves of a horizontal sprite, idle on the left. */
    public record Progress(GuiSprite sprite, int x, int y) {
        public int width() {
            return sprite.width() / 2;
        }

        public int height() {
            return sprite.height();
        }

        public void draw(GuiGraphics graphics, int left, int top, float fraction) {
            sprite.region(graphics, left, top, 0, 0, width(), height());
            MachineProgressRenderer.drawFraction(
                    graphics,
                    sprite.texture(),
                    left,
                    top,
                    width(),
                    0,
                    width(),
                    height(),
                    sprite.width(),
                    sprite.height(),
                    fraction);
        }
    }

    public void draw(GuiGraphics graphics, int x, int y) {
        draw(graphics, x, y, List.of());
    }

    public void draw(GuiGraphics graphics, int x, int y, List<net.minecraft.world.inventory.Slot> slots) {
        background.panel(graphics, x, y, width, height);
        if (menuSlots)
            for (var slot : slots)
                GuiTemplates.sprite(slot instanceof SlotFluid ? "slot/fluid" : "slot/item")
                        .draw(graphics, x + slot.x - 1, y + slot.y - 1);
        for (var element : elements) element.draw(graphics, x, y);
        if (progress != null) progress.draw(graphics, x + progress.x(), y + progress.y(), 0);
    }
}
