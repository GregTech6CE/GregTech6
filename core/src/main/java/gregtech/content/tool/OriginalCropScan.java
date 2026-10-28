/*
 * Adapted from Gregorius Techneticies' Behavior_Cropnalyzer (2019), LGPL-3.0-or-later.
 * The optional IC2 crop API is accessed without a mandatory IC2 dependency.
 */
package gregtech.content.tool;

import gregtech.api.crop.CropScanSource;
import gregtech.api.crop.CropScanSource.CropScanData;
import gregtech.engine.tool.ScannerEnergyRules;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/** Shared addon contract, with the original optional IC2 contract as a fallback. */
public final class OriginalCropScan {
    private OriginalCropScan() {}

    public record Result(long cost, List<String> lines) {
        public static final Result NONE = new Result(0, List.of());
    }

    private record CropAccess(
            Method scanLevel,
            Method setScanLevel,
            Method crop,
            Method growth,
            Method gain,
            Method resistance,
            Method fertilizer,
            Method water,
            Method weedEx,
            Method nutrients,
            Method humidity,
            Method air) {}

    private record CardAccess(Method name, Method attributes, Method discoveredBy) {}

    private static final ClassValue<Optional<CropAccess>> CROPS = new ClassValue<>() {
        @Override
        protected Optional<CropAccess> computeValue(Class<?> type) {
            try {
                Class<?> api = Class.forName("ic2.api.crops.ICropTile", false, type.getClassLoader());
                if (!api.isAssignableFrom(type)) return Optional.empty();
                return Optional.of(new CropAccess(
                        api.getMethod("getScanLevel"),
                        api.getMethod("setScanLevel", byte.class),
                        api.getMethod("getCrop"),
                        api.getMethod("getGrowth"),
                        api.getMethod("getGain"),
                        api.getMethod("getResistance"),
                        api.getMethod("getNutrientStorage"),
                        api.getMethod("getHydrationStorage"),
                        api.getMethod("getWeedExStorage"),
                        api.getMethod("getNutrients"),
                        api.getMethod("getHumidity"),
                        api.getMethod("getAirQuality")));
            } catch (ReflectiveOperationException | LinkageError e) {
                return Optional.empty();
            }
        }
    };
    private static final ClassValue<Optional<CardAccess>> CARDS = new ClassValue<>() {
        @Override
        protected Optional<CardAccess> computeValue(Class<?> type) {
            try {
                return Optional.of(new CardAccess(
                        type.getMethod("displayName"), type.getMethod("attributes"), type.getMethod("discoveredBy")));
            } catch (ReflectiveOperationException | LinkageError e) {
                return Optional.empty();
            }
        }
    };

    public static boolean supports(Object tile) {
        return tile instanceof CropScanSource
                || tile != null && CROPS.get(tile.getClass()).isPresent();
    }

    /** Source scan-level promotion precedes payment, including when the scanner lacks charge. */
    public static Result scan(Object tile, int x, int y, int z, java.util.function.UnaryOperator<String> translate) {
        if (tile == null) return Result.NONE;
        if (tile instanceof CropScanSource source) {
            CropScanData data = source.cropScanData();
            if (data == null) return Result.NONE;
            if (data.scanLevel() < 4) source.setCropScanLevel(4);
            return format(data, x, y, z, translate);
        }
        var access = CROPS.get(tile.getClass());
        if (access.isEmpty()) return Result.NONE;
        try {
            CropAccess cropAccess = access.get();
            Object crop = cropAccess.crop.invoke(tile);
            if (crop == null) return Result.NONE;
            // Invoke the public API declaration, even if a mod's card subclass is non-public.
            var card = CARDS.get(cropAccess.crop.getReturnType());
            if (card.isEmpty()) return Result.NONE;
            CardAccess cardAccess = card.get();
            boolean discovery = ((Number) cropAccess.scanLevel.invoke(tile)).intValue() < 4;
            if (discovery) cropAccess.setScanLevel.invoke(tile, (byte) 4);
            return format(
                    new CropScanData(
                            (String) cardAccess.name.invoke(crop),
                            List.of((String[]) cardAccess.attributes.invoke(crop)),
                            (String) cardAccess.discoveredBy.invoke(crop),
                            number(cropAccess.growth, tile),
                            number(cropAccess.gain, tile),
                            number(cropAccess.resistance, tile),
                            number(cropAccess.fertilizer, tile),
                            number(cropAccess.water, tile),
                            number(cropAccess.weedEx, tile),
                            number(cropAccess.nutrients, tile),
                            number(cropAccess.humidity, tile),
                            number(cropAccess.air, tile),
                            discovery ? 0 : 4),
                    x,
                    y,
                    z,
                    translate);
        } catch (ReflectiveOperationException | LinkageError e) {
            return Result.NONE;
        }
    }

    private static int number(Method method, Object tile) throws ReflectiveOperationException {
        return ((Number) method.invoke(tile)).intValue();
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
