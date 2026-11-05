package gregtech.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import snownee.jade.api.ITooltip;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

/** Transfer readings stay on the left; packet limits are optional, muted details on the right. */
final class EnergyRateText {
    private EnergyRateText() {}

    static String amount(double value) {
        return java.math.BigDecimal.valueOf(value)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    static void render(
            ITooltip tooltip, Component label, Component amount, Component unit, long min, long max, boolean details) {
        tooltip.add(label.copy()
                .withStyle(ChatFormatting.GRAY)
                .append(" ")
                .append(amount.copy().withStyle(ChatFormatting.WHITE))
                .append(unit)
                .append(Component.literal("/t").withStyle(ChatFormatting.WHITE)));
        if (details && min != max) {
            var helper = IElementHelper.get();
            tooltip.append(helper.spacer(8, 0));
            // Append after general number formatting so the range digits remain grey too.
            tooltip.append(
                    helper.text(Component.literal("(" + min + "-" + max + ")").withStyle(ChatFormatting.GRAY))
                            .align(IElement.Align.RIGHT));
        }
    }

    static void conversion(
            JadeInspection info,
            gregtech.engine.energy.IEnergyBlock energy,
            gregtech.content.data.GregTechTags.Tag input,
            gregtech.content.data.GregTechTags.Tag output,
            double received,
            double transferred) {
        info.energyRate(
                "average_input",
                received,
                energy.getEnergySizeInputMin(input, null),
                energy.getEnergySizeInputMax(input, null),
                input.getShortName());
        info.energyRate(
                "average_output",
                transferred,
                energy.getEnergySizeOutputMin(output, null),
                energy.getEnergySizeOutputMax(output, null),
                output.getShortName());
    }

    static void efficiency(JadeInspection info, int efficiency) {
        var row = info.translated("jade.gt6ce.engine.eff", percent(efficiency));
        if (row != null) {
            row.putBoolean("details", true);
            row.putBoolean("left", true);
        }
    }

    static String percent(int efficiency) {
        return gregtech.content.machine.OriginalFunctionalTooltipData.efficiencyPercent(efficiency) + "%";
    }

    static int efficiency(
            long input,
            long output,
            gregtech.content.data.GregTechTags.Tag inType,
            gregtech.content.data.GregTechTags.Tag outType) {
        if (input <= 0) return 0;
        long factor = inType == gregtech.content.data.GregTechTags.Energy.RF ? 40000 : 10000;
        long divisor = outType == gregtech.content.data.GregTechTags.Energy.RF ? 4 : 1;
        return java.math.BigInteger.valueOf(output)
                .multiply(java.math.BigInteger.valueOf(factor))
                .divide(java.math.BigInteger.valueOf(input).multiply(java.math.BigInteger.valueOf(divisor)))
                .min(java.math.BigInteger.valueOf(Integer.MAX_VALUE))
                .intValue();
    }
}
