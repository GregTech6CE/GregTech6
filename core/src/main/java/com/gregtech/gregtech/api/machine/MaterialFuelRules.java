package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.GregTechConstants;

/** Material fuel values extracted from saltnya FurnaceFuelValue; item/tag fallback is platform code. */
public final class MaterialFuelRules {
    private MaterialFuelRules() {}

    public static long heatUnits(long burnValue, int efficiency) {
        return burnValue * GregTechConstants.EU_PER_FURNACE_TICK * efficiency / 10000L;
    }

    public static int ashTinyCount(GTMaterial material, long itemAmount) {
        long scaled = scaleUnits(material.getTargetBurningAmount(), GTValues.U,
                itemAmount > 0 ? itemAmount : MaterialPrefix.dust.getMaterialWeight());
        return scaled <= 0 ? 0 : (int) Math.min(64L, Math.max(1L, scaled / GregTechConstants.U9));
    }

    public static long getBurnValue(GTMaterial material, MaterialPrefix prefix, long amount) {
        GTMaterial mat = material.resolve();
        if (!mat.isValid()) {
            return 0;
        }
        long burnTime = mat.getFurnaceBurnTime();
        if (burnTime <= 0 && prefix == null) {
            return 0;
        }

        long scaled;
        if (prefix == null) {
            scaled = scaleUnits(burnTime, GTValues.U, amount);
        } else if (prefix == MaterialPrefix.oreRaw) {
            scaled = burnTime;
        } else if (isBurnablePrefix(prefix)) {
            scaled = scaleUnits(burnTime, GTValues.U, amount);
            scaled = applyWoodPrefixRules(prefix, mat, scaled);
        } else {
            scaled = scaleUnits(burnTime, GTValues.U, amount);
        }
        return clampFuelValue(scaled);
    }

    private static long applyWoodPrefixRules(MaterialPrefix prefix, GTMaterial material, long burnTime) {
        if (!isWoodFamily(material)) {
            return burnTime;
        }
        if (prefix == MaterialPrefix.stick) {
            return Math.max(GregTechConstants.TICKS_PER_SMELT / 2L, burnTime);
        }
        if (prefix == MaterialPrefix.stickLong) {
            return Math.max(GregTechConstants.TICKS_PER_SMELT, burnTime);
        }
        if (prefix == MaterialPrefix.plate) {
            return Math.max((GregTechConstants.TICKS_PER_SMELT * 27L) / 2, burnTime);
        }
        return burnTime;
    }

    private static boolean isWoodFamily(GTMaterial material) {
        return material.has(MaterialProperty.WOOD) || material.resolve() == com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood.resolve();
    }

    private static boolean isBurnablePrefix(MaterialPrefix prefix) {
        return prefix == MaterialPrefix.stick
                || prefix == MaterialPrefix.stickLong
                || prefix == MaterialPrefix.plate
                || prefix == MaterialPrefix.plateDouble
                || prefix == MaterialPrefix.plateTriple
                || prefix == MaterialPrefix.plateQuadruple
                || prefix == MaterialPrefix.plateQuintuple
                || prefix == MaterialPrefix.plateDense
                || prefix == MaterialPrefix.dust
                || prefix == MaterialPrefix.dustSmall
                || prefix == MaterialPrefix.dustTiny
                || prefix == MaterialPrefix.dustDiv72
                || prefix == MaterialPrefix.gem;
    }

    /** GT6 {@code UT.Code.units}. */
    public static long scaleUnits(long value, long unit, long amount) {
        if (value <= 0 || amount <= 0) {
            return 0;
        }
        return value * amount / unit;
    }

    public static long clampFuelValue(long value) {
        return Math.min(32000L, Math.max(0L, value));
    }
}
