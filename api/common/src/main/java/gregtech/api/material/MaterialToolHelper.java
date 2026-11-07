/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.MaterialInfoTool;
import gregtech.api.addon.material.MaterialKey;

/** Tool profile queries for callers that must not depend on four separate legacy getters. */
public final class MaterialToolHelper {
    private MaterialToolHelper() {}

    public static MaterialToolProfile profile(GTMaterial material) {
        if (material == null || !material.isValid()) return new MaterialToolProfile(0, 1F, 0, 0);
        return MaterialInfoTool.frozenTool(new MaterialKey(material.resolve().getName()))
                .orElseGet(() -> new MaterialToolProfile(
                        legacyTypes(material),
                        legacySpeed(material),
                        legacyDurability(material),
                        legacyQuality(material)));
    }

    /** Frozen indexed query by canonical material key. */
    public static MaterialToolProfile profile(MaterialKey material) {
        return MaterialInfoTool.tool(material);
    }

    static int legacyTypes(GTMaterial material) {
        return material.rawToolTypes();
    }

    static int legacyQuality(GTMaterial material) {
        return material.rawToolQuality();
    }

    static float legacySpeed(GTMaterial material) {
        return material.rawToolSpeed();
    }

    static long legacyDurability(GTMaterial material) {
        return material.rawToolDurability();
    }
}
