/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.material;

import gregtech.api.addon.material.MaterialInfoTool;
import gregtech.api.addon.material.MaterialKey;
import java.util.Optional;

/** Tool profile queries for callers that must not depend on four separate legacy getters. */
public final class MaterialToolHelper {
    private MaterialToolHelper() {}

    @Deprecated(since = "1.7.0", forRemoval = false)
    public static MaterialToolProfile profile(GTMaterial material) {
        if (material == null || !material.isValid()) return new MaterialToolProfile(0, 1F, 0, 0);
        if (material.resolve().getName().indexOf('/') >= 0) return new MaterialToolProfile(0, 1F, 0, 0);
        return MaterialInfoTool.currentTool(new MaterialKey(material.resolve().getName()))
                .orElseGet(() -> new MaterialToolProfile(0, 1F, 0, 0));
    }

    /** Frozen indexed query by canonical material key. */
    public static Optional<MaterialToolProfile> profile(MaterialKey material) {
        return MaterialInfoTool.tool(material);
    }

    static int legacyTypes(GTMaterial material) {
        return profile(material).types();
    }

    static int legacyQuality(GTMaterial material) {
        return profile(material).quality();
    }

    static float legacySpeed(GTMaterial material) {
        return profile(material).speed();
    }

    static long legacyDurability(GTMaterial material) {
        return profile(material).durability();
    }
}
