/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from TileEntityBase10EnergyBatBox, TileEntityBase10EnergyConverter,
 * TileEntityBase11Bidirectional and MultiTileEntitySolarPanelElectric. */
package com.gregtech.gregtech.content.energy;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;

/** Original rated statistics and tooltip choices, without native item/storage/rendering types. */
public final class OriginalEnergyDeviceTooltipData {
    private OriginalEnergyDeviceTooltipData() {}

    public record Stats(long minimum, long recommended, long maximum) {}
    public record Profile(Stats input, Stats output, String inputFaceKey, String outputFaceKey,
                          boolean alwaysShowRange, int efficiency, boolean batteryModes,
                          boolean monkeyWrench) {}

    public static boolean handles(EnergyNodeSpec spec) {
        return spec.batterySlots() > 0 || spec.kind() == EnergyNodeSpec.Kind.SOLAR
                || spec.id().startsWith("transformer_") || spec.id().startsWith("rotation_transformer_");
    }

    /** Battery boxes inherit TileEntityBase01Root.getEnergySizeInputMin, including ULV = 4. */
    public static long batteryInputMinimum(long input) { return input / 2; }

    public static Profile profile(EnergyNodeSpec spec, boolean reversed) {
        if (spec.batterySlots() > 0) {
            long input = spec.inputRate(), output = spec.outputRate();
            return new Profile(new Stats(batteryInputMinimum(input), input, input * 2),
                    new Stats(output, output, output), "gt.lang.face.any.but.front", "gt.lang.face.front",
                    false, -1, true, false);
        }
        if (spec.kind() == EnergyNodeSpec.Kind.SOLAR) {
            long output = spec.outputRate();
            return new Profile(null, new Stats(output / 8, output, output), null, "gt.lang.face.front",
                    false, -1, false, false);
        }
        if (spec.id().startsWith("transformer_") || spec.id().startsWith("rotation_transformer_")) {
            long input = spec.inputRate(), output = spec.outputRate();
            Stats in, out;
            if (reversed) {
                long originalOutputMin = output / 2;
                // Source reverse recommendation deliberately remains the original input rating.
                in = new Stats(originalOutputMin <= 8 ? 1 : originalOutputMin, input,
                        Math.max(input, output * 2 * (input / output)));
                out = new Stats(input * 3 / 4, input, input * 2);
            } else {
                in = new Stats(input <= 16 ? 1 : input / 2, input, input * 2);
                out = new Stats(output / 2, output, output * 2);
            }
            // The source's localized tooltip face names do not swap with the actual reversed faces.
            return new Profile(in, out, "gt.lang.face.front",
                    spec.id().startsWith("rotation_transformer_") ? "gt.lang.face.back" : "gt.lang.face.any.but.front",
                    true, 10000, false, true);
        }
        throw new IllegalArgumentException("No source tooltip profile for " + spec.id());
    }
}
