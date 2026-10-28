package gregtech.compat.jade;

import net.minecraft.network.chat.Component;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.view.FluidView;

/** All GT gauges use Jade's own text, shadow, and border styling. */
final class JadeBars {
    private JadeBars() {}

    static IElement time(long completed, long total, double workPerTick) {
        double ratio = total <= 0 ? 0 : Math.max(0, Math.min(1, (double) completed / total));
        double perSecond = Math.max(1, workPerTick) * 20;
        return progress(ratio, Component.translatable("jade.gt6ce.machine.time",
                JadeText.decimal(Math.max(0, Math.min(completed, total)) / perSecond, 1),
                JadeText.decimal(Math.max(0, total) / perSecond, 1), JadeText.decimal(ratio * 100, 1)), 0xBD4141);
    }

    static IElement progress(double ratio, Component label, int color) {
        var helper = IElementHelper.get();
//? if >=1.21.1 {
        /*var box = BoxStyle.getNestedBox();
*///?} else {
        var box = BoxStyle.DEFAULT;
//?}
        int dark = 0xFF000000 | ((color & 0xFEFEFE) >>> 1);
        return helper.progress((float) Math.max(0, Math.min(1, ratio)), label,
                helper.progressStyle().color(dark, 0xFF000000 | color), box, true);
    }

    static IElement fluid(FluidView view, Component label) {
        var helper = IElementHelper.get();
//? if >=1.21.1 {
        /*var box = BoxStyle.getNestedBox();
*///?} else {
        var box = BoxStyle.DEFAULT;
//?}
        return helper.progress(view.ratio, label, helper.progressStyle().overlay(view.overlay), box, true);
    }
}
