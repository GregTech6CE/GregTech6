package gregtech.compat.jade;

import gregtech.content.energy.BoilerGauge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

/** Pressure colours share Jade's border and text, with a shaded top and bottom. */
final class BoilerGaugeElement extends Element {
    private final double fill;
    private final boolean running;
    private final double warmupEnd;
    private final IElement frame;
    private final IElement text;

    BoilerGaugeElement(double fill, Component label, boolean running) {
        this(fill, label, running, .5);
    }

    BoilerGaugeElement(double fill, Component label, boolean running, double warmupEnd) {
        this.fill = Math.max(0, Math.min(1, fill));
        this.running = running;
        this.warmupEnd = warmupEnd;
        frame = JadeBars.progress(0, Component.empty(), 0);
        text = IElementHelper.get().text(label);
        message(label.getString());
    }

    @Override
    public Vec2 getSize() {
        return new Vec2(Math.max(frame.getCachedSize().x, text.getCachedSize().x + 10), frame.getCachedSize().y);
    }

    @Override
    public void render(GuiGraphics graphics, float x, float y, float right, float bottom) {
        float height = getCachedSize().y;
        frame.render(graphics, x, y, right, y + height);
        float inset = JadeBars.borderWidth();
        float width = Math.max(0, right - x - 2 * inset), innerHeight = Math.max(0, height - 2 * inset);
        var display = IDisplayHelper.get();
        int pressureColor = BoilerGauge.colorAt(fill, running, warmupEnd);
        for (int pixel = 0; pixel < Math.ceil(width); pixel++) {
            int color = pixel < width * fill
                    ? pressureColor
                    : shade(BoilerGauge.colorAt(pixel / Math.max(1, width - 1), running, warmupEnd), .25);
            int edge = 0xFF000000 | shade(color, .65);
            int center = 0xFF000000 | color;
            float columnWidth = Math.min(1, width - pixel);
            display.drawGradientRect(
                    graphics, x + inset + pixel, y + inset, columnWidth, innerHeight / 2, edge, center);
            display.drawGradientRect(
                    graphics,
                    x + inset + pixel,
                    y + inset + innerHeight / 2,
                    columnWidth,
                    innerHeight / 2,
                    center,
                    edge);
        }
        if (fill > 0 && fill < 1) {
            int marker = (int) (x + inset + width * fill);
            graphics.fill(marker, (int) (y + inset), marker + 1, (int) (y + height - inset), 0xFFFFFFFF);
        }
        float textY = y + (height - text.getCachedSize().y) / 2;
        text.render(graphics, x + inset + 1, textY, right - inset, textY + text.getCachedSize().y);
    }

    private static int shade(int color, double factor) {
        return ((int) ((color >> 16 & 255) * factor) << 16)
                | ((int) ((color >> 8 & 255) * factor) << 8)
                | (int) ((color & 255) * factor);
    }
}
