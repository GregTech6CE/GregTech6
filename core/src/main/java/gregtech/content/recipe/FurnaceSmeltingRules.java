/* Gregorius Techneticies / GregTech-6 Team source rules, LGPL-3.0-or-later. */
package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.generated.MaterialWorkability;
import gregtech.engine.material.*;
import java.math.BigInteger;
import java.util.List;

/** Loader_Recipes_Furnace:151-160,190-201; source FURNACE is input permission.
 * All reviewed FURNACE-positive smelting targets solidify with an identity U ratio.
 * The source's Water/Lava solidification exceptions are not targets of those positive inputs. */
public final class FurnaceSmeltingRules {
    private FurnaceSmeltingRules() {}

    public record ExternalRow(MaterialPrefix input, long fixedAmount, boolean experience) {
        public ExternalRow(MaterialPrefix input, long fixedAmount) {
            this(input, fixedAmount, true);
        }

        public long amount() {
            return fixedAmount < 0 ? input.getMaterialWeight() : fixedAmount;
        }
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
            new ExternalRow(MaterialPrefix.reduced, -1),
            // Source :142-143 inherits the prefix weight and deliberately grants no experience.
            new ExternalRow(MaterialPrefix.dustPure, -1, false),
            new ExternalRow(MaterialPrefix.dustRefined, -1, false),
            new ExternalRow(MaterialPrefix.dustImpure, -1, false));

    public static boolean allows(GTMaterial material) {
        if (material == null || !material.isValid()) return false;
        var decision = OreSourcePolicy.furnaceDecision(material);
        return decision == SourceRecyclingFacts.Decision.UNKNOWN
                ? legacyAllows(material)
                : reviewedAllows(decision, material.resolve().has(MaterialProperty.HIDDEN));
    }
    /** Exact prior listener policy for callers whose item provenance is outside this native correction. */
    public static boolean legacyAllows(GTMaterial material) {
        return material != null
                && material.isValid()
                && !material.has(MaterialProperty.HIDDEN)
                && MaterialWorkability.isFurnace(material);
    }
    /** Shared routing guard for known source decisions. UNKNOWN callers choose their own old fallback;
     * the source FURNACE fact itself is not rewritten to pretend HIDDEN means source UNUSED. */
    public static boolean reviewedAllows(SourceRecyclingFacts.Decision decision, boolean hidden) {
        return decision == SourceRecyclingFacts.Decision.YES && !hidden;
    }
    /** Target solidifying amount is U, not the source material's smelting amount again. */
    public static long amount(long formAmount, long smeltingPerUnit) {
        return PulverizationRules.amount(formAmount, smeltingPerUnit);
    }

    public static long experience(long targetAmount, int toolQuality) {
        if (targetAmount <= 0 || toolQuality < 0) return 0;
        return BigInteger.valueOf(targetAmount)
                .multiply(BigInteger.valueOf(toolQuality + 1L))
                .add(BigInteger.valueOf(GTValues.U - 1))
                .divide(BigInteger.valueOf(GTValues.U))
                .min(BigInteger.valueOf(Long.MAX_VALUE))
                .longValue();
    }
}
