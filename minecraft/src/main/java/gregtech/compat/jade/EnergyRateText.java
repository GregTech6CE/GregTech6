package gregtech.compat.jade;

/** Shared inspection notation; equal packet limits do not need a range. */
final class EnergyRateText {
    private EnergyRateText() {}

    static String rate(long rated, long min, long max, String unit) {
        return rated + " " + unit + "/t" + (min == max ? "" : " (" + min + "–" + max + ")");
    }

    static String offered(long packets, long rated, long min, long max, String unit) {
        return net.minecraft.network.chat.Component.translatable("jade.gt6ce.energy.max_packets", packets)
                        .getString() + " " + rate(rated, min, max, unit);
    }

    static void conversion(
            JadeInspection info,
            gregtech.engine.energy.IEnergyBlock energy,
            gregtech.content.data.GregTechTags.Tag input,
            gregtech.content.data.GregTechTags.Tag output,
            long received,
            long transferred,
            int efficiency) {
        info.line(
                "energy_rate",
                JadeInspection.label("input"),
                rate(
                        received,
                        energy.getEnergySizeInputMin(input, null),
                        energy.getEnergySizeInputMax(input, null),
                        input.getShortName()));
        info.line(
                "energy_rate",
                JadeInspection.label("output"),
                rate(
                        transferred,
                        energy.getEnergySizeOutputMin(output, null),
                        energy.getEnergySizeOutputMax(output, null),
                        output.getShortName()));
        efficiency(info, efficiency);
    }

    static void efficiency(JadeInspection info, int efficiency) {
        var row = info.translated("jade.gt6ce.engine.eff", percent(efficiency));
        if (row != null) row.putBoolean("details", true);
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
