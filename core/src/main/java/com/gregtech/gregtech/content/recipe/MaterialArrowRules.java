package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.MaterialPrefix;

/** Gregorius Techneticies' GT6 Loader_Items:167-168 and EntityArrow_Material:108,178. */
public final class MaterialArrowRules {
    private MaterialArrowRules() {}
    public static final int LIFETIME_TICKS = 3000;

    public static boolean isArrow(MaterialPrefix prefix, GTMaterial material) {
        return material != null && material.isValid()
                && (prefix == MaterialPrefix.arrowGtWood || prefix == MaterialPrefix.arrowGtPlastic);
    }

    public static float speedMultiplier(MaterialPrefix prefix) {
        return prefix == MaterialPrefix.arrowGtPlastic ? 1.5F : 1.0F;
    }

    public static double baseDamage(GTMaterial material) {
        return 2.0D + Math.max(0, material.getToolQuality() - 1);
    }
}
