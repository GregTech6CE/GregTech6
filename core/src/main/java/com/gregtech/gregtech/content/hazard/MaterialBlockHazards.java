/* Gregorius Techneticies / GregTech-6 Team source rules, LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.hazard;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;

/** PrefixBlock:393-405,538-541: strength follows contents, not the mining charge's fixed cube. */
public final class MaterialBlockHazards {
    private MaterialBlockHazards() {}
    private static boolean storage(BlockMaterialPrefix prefix) {
        return prefix.getName().startsWith("block") || prefix.isCrate() || prefix.getName().startsWith("casingMachine");
    }
    private static boolean dust(BlockMaterialPrefix prefix) {
        return prefix.getName().equals("blockDust") || prefix.getName().equals("crateGtDust")
                || prefix.getName().equals("crateGt64Dust");
    }
    private static float units(BlockMaterialPrefix prefix) {
        return (float) (prefix.getMaterialWeight() > 0 ? prefix.getMaterialWeight() : GTValues.U) / GTValues.U;
    }
    public static float ignitionPower(BlockMaterialPrefix prefix, GTMaterial material) {
        material = material.resolve();
        boolean explosive = material.has(MaterialProperty.EXPLOSIVE);
        return storage(prefix) && material.has(MaterialProperty.FLAMMABLE) && (dust(prefix) || explosive)
                ? units(prefix) * (explosive ? .5F : .33F) : 0;
    }
    public static float chainPower(BlockMaterialPrefix prefix, GTMaterial material) {
        material = material.resolve();
        return storage(prefix) && (material.has(MaterialProperty.EXPLOSIVE)
                || dust(prefix) && material.has(MaterialProperty.FLAMMABLE)) ? units(prefix) * .7F : 0;
    }
}
