/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from TileEntityBase10EnergyBatBox, TileEntityBase10EnergyConverter,
 * TileEntityBase11Bidirectional and MultiTileEntitySolarPanelElectric. */
package gregtech.content.energy;

import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyNodeSpec.DeviceType;

/** Original rated statistics and tooltip choices, without native item/storage/rendering types. */
public final class OriginalEnergyDeviceTooltipData {
    private OriginalEnergyDeviceTooltipData() {}

    public record Stats(long minimum, long recommended, long maximum) {}

    public record Profile(
            Stats input,
            Stats output,
            String inputFaceKey,
            String outputFaceKey,
            boolean alwaysShowRange,
            int efficiency,
            boolean batteryModes,
            boolean monkeyWrench) {}

    public static boolean handles(EnergyNodeSpec spec) {
        return spec.deviceType() != DeviceType.BUFFERED;
    }

    /** Battery boxes inherit TileEntityBase01Root.getEnergySizeInputMin, including ULV = 4. */
    public static long batteryInputMinimum(long input) {
        return input / 2;
    }

    public static Profile profile(EnergyNodeSpec spec, boolean reversed) {
        if (OriginalSteamTurbines.handles(spec)) {
            return new Profile(
                    new Stats(OriginalRotaryConverter.inputMinimum(spec), spec.inputRate(), spec.inputRate() * 2),
                    new Stats(spec.outputRate() / 2, spec.outputRate(), spec.outputRate() * 2),
                    "gt.lang.face.back",
                    "gt.lang.face.front",
                    true,
                    OriginalSteamTurbines.efficiency(spec),
                    false,
                    true);
        }
        if (MagnetMachineDefinitions.handles(spec)) {
            return new Profile(
                    new Stats(OriginalRotaryConverter.inputMinimum(spec), spec.inputRate(), spec.inputRate() * 2),
                    new Stats(spec.outputRate() / 2, spec.outputRate(), spec.outputRate() * 2),
                    "gt.lang.face.any.but.front.back",
                    "gt.lang.face.front.back",
                    true,
                    MagnetMachineDefinitions.efficiency(spec),
                    false,
                    false);
        }
        if (OriginalThermalConverter.handles(spec)) {
            return new Profile(
                    new Stats(OriginalRotaryConverter.inputMinimum(spec), spec.inputRate(), spec.inputRate() * 2),
                    new Stats(spec.outputRate() / 2, spec.outputRate(), spec.outputRate() * 2),
                    OriginalThermalConverter.cooler(spec)
                            ? "gt.lang.face.any.but.front.back"
                            : "gt.lang.face.any.but.front",
                    "gt.lang.face.front",
                    true,
                    OriginalThermalConverter.efficiency(spec),
                    false,
                    false);
        }
        if (OriginalRotaryConverter.handles(spec)) {
            long input = spec.inputRate(), output = spec.outputRate();
            return new Profile(
                    new Stats(OriginalRotaryConverter.inputMinimum(spec), input, input * 2),
                    new Stats(output / 2, output, output * 2),
                    OriginalRotaryConverter.motor(spec) ? "gt.lang.face.any.but.front" : "gt.lang.face.back",
                    "gt.lang.face.front",
                    true,
                    OriginalRotaryConverter.efficiency(spec),
                    false,
                    OriginalRotaryConverter.motor(spec));
        }
        if (spec.deviceType() == DeviceType.BATTERY_BOX) {
            long input = spec.inputRate(), output = spec.outputRate();
            return new Profile(
                    new Stats(batteryInputMinimum(input), input, input * 2),
                    new Stats(output, output, output),
                    "gt.lang.face.any.but.front",
                    "gt.lang.face.front",
                    false,
                    -1,
                    true,
                    false);
        }
        if (spec.deviceType() == DeviceType.SOLAR) {
            long output = spec.outputRate();
            return new Profile(
                    null, new Stats(output / 8, output, output), null, "gt.lang.face.front", false, -1, false, false);
        }
        if (spec.deviceType() == DeviceType.ELECTRIC_TRANSFORMER
                || spec.deviceType() == DeviceType.ROTATION_TRANSFORMER) {
            long input = spec.inputRate(), output = spec.outputRate();
            Stats in, out;
            if (reversed) {
                long originalOutputMin = output / 2;
                // Source reverse recommendation deliberately remains the original input rating.
                in = new Stats(
                        originalOutputMin <= 8 ? 1 : originalOutputMin,
                        input,
                        Math.max(input, output * 2 * (input / output)));
                out = new Stats(input * 3 / 4, input, input * 2);
            } else {
                in = new Stats(input <= 16 ? 1 : input / 2, input, input * 2);
                out = new Stats(output / 2, output, output * 2);
            }
            // The source's localized tooltip face names do not swap with the actual reversed faces.
            return new Profile(
                    in,
                    out,
                    "gt.lang.face.front",
                    spec.deviceType() == DeviceType.ROTATION_TRANSFORMER
                            ? "gt.lang.face.back"
                            : "gt.lang.face.any.but.front",
                    true,
                    10000,
                    false,
                    true);
        }
        throw new IllegalArgumentException("No source tooltip profile for " + spec.id());
    }
}
