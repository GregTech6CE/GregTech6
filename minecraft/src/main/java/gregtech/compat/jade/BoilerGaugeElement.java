package gregtech.compat.jade;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.Element;

/** Filled pixels share the current pressure color; the remaining scale stays dim. */
final class BoilerGaugeElement extends Element {
    private final double fill;
    private final Component label;
    private final boolean running;

    BoilerGaugeElement(double fill, Component label, boolean running) {
        this.fill = Math.max(0, Math.min(1, fill));
        this.label = label;
        this.running = running;
        message(label.getString());
    }

    @Override
    public Vec2 getSize() {
        return new Vec2(Math.max(20, Minecraft.getInstance().font.width(label) + 10), 14);
    }

    @Override
    public void render(GuiGraphics graphics, float x, float y, float right, float bottom) {
        // Jade supplies the content's right/bottom coordinates, not this element's width/height.
        int left = (int) x, top = (int) y;
        int w = Math.max(3, (int) (right - x)), h = (int) getCachedSize().y;
        graphics.fill(left, top, left + w, top + h, 0xFF999999);
        int inner = w - 2, end = left + 1 + (int) Math.floor(inner * fill);
        int pressureColor = gregtech.content.energy.BoilerGauge.colorAt(fill, running);
        for (int pixel = 0; pixel < inner; pixel++) {
            int start = left + 1 + pixel;
            int color = gregtech.content.energy.BoilerGauge.colorAt((double) pixel / Math.max(1, inner - 1), running);
            int dark = ((color >> 16 & 255) / 4 << 16) | ((color >> 8 & 255) / 4 << 8) | (color & 255) / 4;
            graphics.fill(start, top + 1, start + 1, top + h - 1, 0xFF000000 | (start < end ? pressureColor : dark));
        }
        if (fill > 0 && fill < 1) graphics.fill(end, top + 1, end + 1, top + h - 1, 0xFFFFFFFF);
        graphics.drawString(Minecraft.getInstance().font, label, left + 5, top + (h - 8) / 2, 0xFFFFFFFF, true);
    }
}
