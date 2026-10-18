package com.gregtech.gregtech.api.crop;

import java.util.List;

/**
 * Optional crop-analysis boundary for addons on both loaders. A crop block entity can
 * implement this interface without importing Minecraft, either loader, or IC2.
 * Return {@code null} when the holder has no crop. Names may be translation keys or
 * literal names. Scanning promotes discovery to level 4 before scanner payment,
 * matching GregTech 6's original Cropnalyzer behavior.
 */
public interface CropScanSource {
    CropScanData cropScanData();
    void setCropScanLevel(int level);

    record CropScanData(String name, List<String> attributes, String discoveredBy,
            int growth, int gain, int resistance, int fertilizer, int water, int weedEx,
            int nutrients, int humidity, int airQuality, int scanLevel) {
        public CropScanData {
            attributes = List.copyOf(attributes);
        }
    }
}
