/* Gregorius Techneticies / GregTech-6 Team source rules, LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.generated.MaterialWorkability;
import java.math.BigInteger;
import java.util.List;

/** Loader_Recipes_Furnace:151-160,190-201; all source FURNACE targets solidify with an identity U ratio. */
public final class FurnaceSmeltingRules {
    private FurnaceSmeltingRules() {}
    public record ExternalRow(MaterialPrefix input, long fixedAmount) {
        public long amount() { return fixedAmount < 0 ? input.getMaterialWeight() : fixedAmount; }
    }
    public static final List<ExternalRow> EXTERNAL = List.of(
            new ExternalRow(MaterialPrefix.rawOreChunk, -1),
            new ExternalRow(MaterialPrefix.chunk, GTValues.U * 2),
            new ExternalRow(MaterialPrefix.rubble, GTValues.U * 2),
            new ExternalRow(MaterialPrefix.pebbles, GTValues.U * 3),
            new ExternalRow(MaterialPrefix.cluster, GTValues.U * 3),
            new ExternalRow(MaterialPrefix.cleanGravel, -1),
            new ExternalRow(MaterialPrefix.dirtyGravel, -1),
            new ExternalRow(MaterialPrefix.crystalline, -1),
            new ExternalRow(MaterialPrefix.reduced, -1));
    public static boolean allows(GTMaterial material) {
        return material.isValid() && !material.has(MaterialProperty.HIDDEN) && MaterialWorkability.isFurnace(material);
    }
    /** Target solidifying amount is U, not the source material's smelting amount again. */
    public static long amount(long formAmount, long smeltingPerUnit) {
        return PulverizationRules.amount(formAmount, smeltingPerUnit);
    }
    public static long experience(long targetAmount, int toolQuality) {
        if (targetAmount <= 0 || toolQuality < 0) return 0;
        return BigInteger.valueOf(targetAmount).multiply(BigInteger.valueOf(toolQuality + 1L))
                .add(BigInteger.valueOf(GTValues.U - 1)).divide(BigInteger.valueOf(GTValues.U))
                .min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }
}
