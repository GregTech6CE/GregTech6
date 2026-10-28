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

    MachineEnergyElement(long min, long rated, long max, double average, String unit) {
        double multiplier = average < min ? 0 : average / rated;
        Component label = Component.literal(JadeText.decimal(average, 2) + " ")
                .append(gregtech.client.EnergyText.unit(unit))
                .append("/t  " + JadeText.decimal(multiplier, 2) + "×");
        double fill = average < min
                ? 0
                : average <= rated
                        ? (rated > min ? .5 * (average - min) / (rated - min) : .5)
                        : (max > rated ? .5 + .5 * (average - rated) / (max - rated) : 1);
        bar = JadeBars.progress(fill, label, average < min ? 0x777777 : 0x29B52A);
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
                Math.max(180, Math.max(bar.getCachedSize().x, Math.max(topWidth, bottomWidth))),
                bar.getCachedSize().y + 20);
    }

    @Override
    public void render(GuiGraphics graphics, float x, float y, float right, float bottom) {
        float barTop = y + 10, barBottom = barTop + bar.getCachedSize().y;
        bar.render(graphics, x, barTop, right, barBottom);
        for (int i = 0; i < positions.length; i++) {
            float anchor = x + (right - x - 1) * positions[i];
            if (i > 0 && i < positions.length - 1)
                graphics.fill((int) anchor, (int) barTop + 1, (int) anchor + 1, (int) barTop + 4, 0xB0000000);
            drawLabel(graphics, multipliers[i], anchor, y, x, right);
            // The intermediate upper-range tick does not need another numeric power label.
            if (positions[i] != .75F) drawLabel(graphics, powers[i], anchor, barBottom + 1, x, right);
        }
    }

    private static void drawLabel(GuiGraphics graphics, IElement text, float anchor, float y, float left, float right) {
        var size = text.getCachedSize();
        float x = Math.max(left, Math.min(right - size.x, anchor - size.x / 2));
        text.render(graphics, x, y, x + size.x, y + size.y);
    }
}
