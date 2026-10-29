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
}
