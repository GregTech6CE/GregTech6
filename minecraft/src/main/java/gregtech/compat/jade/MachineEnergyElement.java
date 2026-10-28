package gregtech.compat.jade;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

/** The rated input stays at the centre; lower and upper operating ranges each occupy half. */
final class MachineEnergyElement extends Element {
    private final IElement bar;
    private final IElement[] multipliers;
    private final IElement[] powers;
    private final float[] positions;
    private final double marker;
    private final IElement text;
    private final boolean inRange;
    private final int background;

    MachineEnergyElement(
            long min, long rated, long max, double average, String unit, boolean overloaded, boolean insufficient) {
        double multiplier = average < min ? 0 : average / rated;
        boolean overload = overloaded || average > max;
        Component label = overload
                ? Component.translatable("jade.gt6ce.machine.overloaded")
                : average <= 0
                        ? (insufficient
                                ? Component.translatable("jade.gt6ce.machine.insufficient_energy")
                                : Component.translatable(
                                        "jade.gt6ce.machine.no_energy_input", gregtech.client.EnergyText.unit(unit)))
                        : Component.literal(JadeText.decimal(average, 2) + " ")
                                .append(gregtech.client.EnergyText.unit(unit))
                                .append("/t  " + JadeText.decimal(multiplier, 2) + "×");
        double position = average < min
                ? 0
                : average <= rated
                        ? (rated > min ? .5 * (average - min) / (rated - min) : .5)
                        : (max > rated ? .5 + .5 * (average - rated) / (max - rated) : 1);
        marker = Math.max(0, Math.min(1, position));
        inRange = !overload && average > 0 && average >= min && average <= max;
        background = overload ? 0xFFDC3030 : average <= 0 && !insufficient ? 0 : 0xFF66B6DA;
        bar = JadeBars.progress(0, Component.empty(), 0);
        text = IElementHelper.get().text(label);
        positions = max > rated && rated > min ? new float[] {0, .5F, .75F, 1} : new float[] {0, 1};
        double[] rates = positions.length == 4
                ? new double[] {min, rated, rated + (max - rated) / 2.0, max}
                : new double[] {min, max};
        multipliers = new IElement[rates.length];
        powers = new IElement[rates.length];
        for (int i = 0; i < rates.length; i++) {
            multipliers[i] = IElementHelper.get().text(Component.literal(JadeText.decimal(rates[i] / rated, 1) + "×"));
            powers[i] = IElementHelper.get().text(Component.literal(Long.toString((long) rates[i])));
        }
        message(label.getString());
    }

    @Override
    public Vec2 getSize() {
        float topWidth = 0, bottomWidth = 0;
        for (int i = 0; i < positions.length; i++) {
            topWidth += multipliers[i].getCachedSize().x + 16;
            bottomWidth += powers[i].getCachedSize().x + 16;
        }
        return new Vec2(
                Math.max(180, Math.max(text.getCachedSize().x + 10, Math.max(topWidth, bottomWidth))),
                bar.getCachedSize().y + 20);
    }

    @Override
    public void render(GuiGraphics graphics, float x, float y, float right, float bottom) {
        float barTop = y + 10, barBottom = barTop + bar.getCachedSize().y;
        bar.render(graphics, x, barTop, right, barBottom);
        float inset = JadeBars.borderWidth();
        int left = (int) Math.ceil(x + inset), end = (int) Math.floor(right - inset);
        int top = (int) Math.ceil(barTop + inset), lower = (int) Math.floor(barBottom - inset);
        if (inRange && end > left) {
            int markerX = left + (int) Math.round((end - left - 1) * marker);
            shadedFill(graphics, left, top, markerX, lower, 0xFF58C85E);
            shadedFill(graphics, markerX + 1, top, end, lower, 0xFF269C38);
            graphics.fill(markerX, top, markerX + 1, lower, 0xFFFFFFFF);
        } else if (background != 0) {
            shadedFill(graphics, left, top, end, lower, background);
        }
        float textY = barTop + (bar.getCachedSize().y - text.getCachedSize().y) / 2;
        text.render(graphics, x + inset + 1, textY, right - inset, textY + text.getCachedSize().y);
        for (int i = 0; i < positions.length; i++) {
            float anchor = x + (right - x - 1) * positions[i];
            if (i > 0 && i < positions.length - 1)
                graphics.fill((int) anchor, (int) barTop + 1, (int) anchor + 1, (int) barTop + 4, 0xB0000000);
            drawLabel(graphics, multipliers[i], anchor, y, x, right);
            // The intermediate upper-range tick does not need another numeric power label.
            if (positions[i] != .75F) drawLabel(graphics, powers[i], anchor, barBottom + 1, x, right);
        }
    }

    /** Keep the gauge's top and bottom shaded, including both sides of the live marker. */
    private static void shadedFill(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        if (right <= left || bottom <= top) return;
        int edge = 0xFF000000
                | (((color >> 16) & 255) * 2 / 3 << 16)
                | (((color >> 8) & 255) * 2 / 3 << 8)
                | ((color & 255) * 2 / 3);
        int middle = top + (bottom - top) / 2;
        graphics.fillGradient(left, top, right, middle, edge, color);
        graphics.fillGradient(left, middle, right, bottom, color, edge);
    }

    private static void drawLabel(GuiGraphics graphics, IElement text, float anchor, float y, float left, float right) {
        var size = text.getCachedSize();
        float x = Math.max(left, Math.min(right - size.x, anchor - size.x / 2));
        text.render(graphics, x, y, x + size.x, y + size.y);
    }
}
