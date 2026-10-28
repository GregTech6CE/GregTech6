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
        double seconds = Math.max(0, total) / perSecond;
        String unit = seconds >= 3600 ? "h" : seconds >= 60 ? "min" : seconds >= 1 ? "s" : "tick";
        double divisor = switch (unit) {
            case "h" -> 3600;
            case "min" -> 60;
            case "tick" -> .05;
            default -> 1;
        };
        int decimals = unit.equals("tick") ? 0 : 1;
        return progress(ratio, Component.translatable("jade.gt6ce.machine.time",
                Component.translatable("jade.gt6ce.time." + unit,
                        JadeText.decimal(Math.max(0, Math.min(completed, total)) / perSecond / divisor, decimals)),
                Component.translatable("jade.gt6ce.time." + unit, JadeText.decimal(seconds / divisor, decimals)),
                JadeText.decimal(ratio * 100, 1)), 0xBD4141);
    }

    static IElement progress(double ratio, Component label, int color) {
        var helper = IElementHelper.get();
//? if >=1.21.1 {
        /*var box = BoxStyle.getNestedBox();
*///?} else {
        var box = BoxStyle.DEFAULT;
//?}
        return helper.progress((float) Math.max(0, Math.min(1, ratio)), label,
                helper.progressStyle().color(0xFF000000 | color).textColor(0xFFFFFF), box, true);
    }

    static float borderWidth() {
//? if >=1.21.1 {
        /*return BoxStyle.getNestedBox().borderWidth();
*///?} else {
        return BoxStyle.DEFAULT.borderWidth();
//?}
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
