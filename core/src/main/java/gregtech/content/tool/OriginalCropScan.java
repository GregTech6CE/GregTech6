/*
 * Adapted from Gregorius Techneticies' Behavior_Cropnalyzer (2019), LGPL-3.0-or-later.
 */
package gregtech.content.tool;

import gregtech.api.crop.CropScanSource;
import gregtech.api.crop.CropScanSource.CropScanData;
import gregtech.engine.tool.ScannerEnergyRules;
import java.util.List;

/** Shared crop-analysis contract for the scanner. */
public final class OriginalCropScan {
    private OriginalCropScan() {}

    public record Result(long cost, List<String> lines) {
        public static final Result NONE = new Result(0, List.of());
    }

    public static boolean supports(Object tile) {
        return tile instanceof CropScanSource;
    }

    /** Source scan-level promotion precedes payment, including when the scanner lacks charge. */
    public static Result scan(Object tile, int x, int y, int z, java.util.function.UnaryOperator<String> translate) {
        if (!(tile instanceof CropScanSource source)) return Result.NONE;
        CropScanData data = source.cropScanData();
        if (data == null) return Result.NONE;
        if (data.scanLevel() < 4) source.setCropScanLevel(4);
        return format(data, x, y, z, translate);
    }

    private static Result format(
            CropScanData data, int x, int y, int z, java.util.function.UnaryOperator<String> translate) {
        return new Result(
                data.scanLevel() < 4 ? ScannerEnergyRules.CROP_DISCOVERY_COST : ScannerEnergyRules.CROP_RESCAN_COST,
                List.of(
                        "--- X: " + x + " Y: " + y + " Z: " + z + " ---",
                        "Type -- Name: " + translate.apply(data.name()) + "   Growth: " + data.growth() + "   Gain: "
                                + data.gain() + "   Resistance: " + data.resistance(),
                        "Plant -- Fertilizer: " + data.fertilizer() + "   Water: " + data.water() + "   Weed-Ex: "
                                + data.weedEx(),
                        "Environment -- Nutrients: " + data.nutrients() + "   Humidity: " + data.humidity()
                                + "   Air-Quality: " + data.airQuality(),
                        "Attributes:" + (data.attributes().isEmpty() ? "" : " " + String.join(", ", data.attributes())),
                        "Discovered by: " + data.discoveredBy()));
    }
}
