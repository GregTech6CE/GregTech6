/*
 * Adapted from Gregorius Techneticies' Behavior_Cropnalyzer (2019), LGPL-3.0-or-later.
 * The optional IC2 crop API is accessed without a mandatory IC2 dependency.
 */
package com.gregtech.gregtech.content.tool;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/** Only the original ic2.api.crops.ICropTile contract qualifies; vanilla plants do not. */
public final class OriginalCropScan {
    private OriginalCropScan() {}

    public record Result(long cost, List<String> lines) {
        public static final Result NONE = new Result(0, List.of());
    }

    private record CropAccess(Method scanLevel, Method setScanLevel, Method crop,
            Method growth, Method gain, Method resistance, Method fertilizer, Method water,
            Method weedEx, Method nutrients, Method humidity, Method air) {}
    private record CardAccess(Method name, Method attributes, Method discoveredBy) {}

    private static final ClassValue<Optional<CropAccess>> CROPS = new ClassValue<>() {
        @Override protected Optional<CropAccess> computeValue(Class<?> type) {
            try {
                Class<?> api = Class.forName("ic2.api.crops.ICropTile", false, type.getClassLoader());
                if (!api.isAssignableFrom(type)) return Optional.empty();
                return Optional.of(new CropAccess(api.getMethod("getScanLevel"),
                        api.getMethod("setScanLevel", byte.class), api.getMethod("getCrop"),
                        api.getMethod("getGrowth"), api.getMethod("getGain"), api.getMethod("getResistance"),
                        api.getMethod("getNutrientStorage"), api.getMethod("getHydrationStorage"),
                        api.getMethod("getWeedExStorage"), api.getMethod("getNutrients"),
                        api.getMethod("getHumidity"), api.getMethod("getAirQuality")));
            } catch (ReflectiveOperationException | LinkageError e) {
                return Optional.empty();
            }
        }
    };
    private static final ClassValue<Optional<CardAccess>> CARDS = new ClassValue<>() {
        @Override protected Optional<CardAccess> computeValue(Class<?> type) {
            try {
                return Optional.of(new CardAccess(type.getMethod("displayName"),
                        type.getMethod("attributes"), type.getMethod("discoveredBy")));
            } catch (ReflectiveOperationException | LinkageError e) {
                return Optional.empty();
            }
        }
    };

    public static boolean supports(Object tile) {
        return tile != null && CROPS.get(tile.getClass()).isPresent();
    }

    /** Source scan-level promotion precedes payment, including when the scanner lacks charge. */
    public static Result scan(Object tile, int x, int y, int z,
            java.util.function.UnaryOperator<String> translate) {
        if (tile == null) return Result.NONE;
        var access = CROPS.get(tile.getClass());
        if (access.isEmpty()) return Result.NONE;
        try {
            CropAccess a = access.get();
            Object crop = a.crop.invoke(tile);
            if (crop == null) return Result.NONE;
            // Invoke the public API declaration, even if a mod's card subclass is non-public.
            var card = CARDS.get(a.crop.getReturnType());
            if (card.isEmpty()) return Result.NONE;
            CardAccess c = card.get();
            boolean discovery = ((Number) a.scanLevel.invoke(tile)).intValue() < 4;
            if (discovery) a.setScanLevel.invoke(tile, (byte) 4);
            String attributes = "";
            for (String attribute : (String[]) c.attributes.invoke(crop)) attributes += ", " + attribute;
            return new Result(discovery ? ScannerEnergyRules.CROP_DISCOVERY_COST
                    : ScannerEnergyRules.CROP_RESCAN_COST, List.of(
                    "--- X: " + x + " Y: " + y + " Z: " + z + " ---",
                    "Type -- Name: " + translate.apply((String) c.name.invoke(crop))
                            + "   Growth: " + a.growth.invoke(tile) + "   Gain: " + a.gain.invoke(tile)
                            + "   Resistance: " + a.resistance.invoke(tile),
                    "Plant -- Fertilizer: " + a.fertilizer.invoke(tile) + "   Water: " + a.water.invoke(tile)
                            + "   Weed-Ex: " + a.weedEx.invoke(tile),
                    "Environment -- Nutrients: " + a.nutrients.invoke(tile) + "   Humidity: "
                            + a.humidity.invoke(tile) + "   Air-Quality: " + a.air.invoke(tile),
                    "Attributes:" + attributes.replaceFirst(",", ""),
                    "Discovered by: " + c.discoveredBy.invoke(crop)));
        } catch (ReflectiveOperationException | LinkageError e) {
            return Result.NONE;
        }
    }
}
