package com.gregtech.gregtech.api.machine.crucible;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.data.MaterialPrefix;

/** Original material item/raw ore payloads, after platform stack recovery checks. */
public final class CrucibleInputRules {
    private CrucibleInputRules() {}

    /** Display the same per-item input conversion used by the actual crucible, including raw ores. */
    public record SmeltingPreview(GTMaterial material, long amount, long temperatureK) {}
    public static SmeltingPreview smeltingPreview(GTMaterial material, MaterialPrefix prefix) {
        CrucibleMaterialStack input = materialItem(material.resolve(), prefix);
        GTMaterial source = input.material.resolve();
        GTMaterial target = source.getTargetSmeltingMaterial();
        if (!source.isValid() || !source.has(com.gregtech.gregtech.api.material.MaterialProperty.MELTING)
                || target == null || !target.isValid() || input.amount <= 0
                || source.getTargetSmeltingAmount() <= 0 || source.getMeltingPoint() <= 0) return null;
        java.math.BigInteger amount = java.math.BigInteger.valueOf(input.amount)
                .multiply(java.math.BigInteger.valueOf(source.getTargetSmeltingAmount()))
                .divide(java.math.BigInteger.valueOf(GTValues.U));
        if (amount.signum() <= 0 || amount.compareTo(java.math.BigInteger.valueOf(Long.MAX_VALUE)) > 0) return null;
        return new SmeltingPreview(target.resolve(), amount.longValue(), source.getMeltingPoint());
    }

    public static CrucibleMaterialStack materialItem(GTMaterial material, MaterialPrefix prefix) {
        return prefix == MaterialPrefix.oreRaw ? ore(material.resolve(), 1)
                : CrucibleMaterialStack.of(material.resolve(), prefix == null ? GTValues.U : prefix.getMaterialWeight());
    }
    public static CrucibleMaterialStack ore(GTMaterial material, long count) {
        return CrucibleMaterialStack.of(material.getTargetCrushingMaterial(),
                Math.multiplyExact(material.getTargetCrushingAmount(), Math.multiplyExact(count, (long) material.getOreMultiplier())));
    }
}
