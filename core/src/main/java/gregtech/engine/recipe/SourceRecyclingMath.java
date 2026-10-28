/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

// Bounded arithmetic adaptation; original UT.Code.units and RecyclingProcessing source provenance in
// thermal-source-draft.json.
package gregtech.engine.recipe;
/** Source RecyclingProcessing arithmetic for one material portion.
 * Eligibility, source evidence, phase identity and actual item composition stay outside this helper.
 * Source: Loader_OreProcessing240-291; OM324; OreDictMaterial1391; UT.Code.units1677.
 */
public final class SourceRecyclingMath {
    private SourceRecyclingMath() {}

    public record Result(long targetUnits, int millibuckets, long ticks) {}

    /** Existing non-lava, integer mB/U entry point; keep reviewed callers behavior unchanged. */
    public static Result singleLiquid(
            long inputUnits,
            long sourceUnit,
            long targetPerUnit,
            long liquidMbPerUnit,
            double inputDensity,
            long targetMelting,
            long liquidTemperature,
            long environmentTemperature) {
        if (inputUnits < 0
                || sourceUnit <= 0
                || targetPerUnit < 0
                || liquidMbPerUnit < 0
                || !Double.isFinite(inputDensity)
                || inputDensity < 0) throw new IllegalArgumentException("Unreviewed thermal arithmetic inputs");
        long target = unitsDown(inputUnits, sourceUnit, targetPerUnit);
        long mb = unitsDown(target, sourceUnit, liquidMbPerUnit);
        // Do not use the INPUT melting point, fluid quantity as mass, or integerized density.
        double weight = (inputDensity * 111.111111 * inputUnits) / sourceUnit;
        long ticks = (long)
                Math.max(16, (weight * (Math.max(targetMelting, liquidTemperature) - environmentTemperature)) / 1600);
        return new Result(target, Math.toIntExact(mb), ticks);
    }

    /** Two distinct downward conversions; a binding such as Lava1000/(9U) stays rational.
     * The reference amounts belong to the OUTPUT target's verified phase, not the input's phase.
     * The native adapter supplies the actual fluid temperature, not its catalog default.
     */
    public static Result singleFluid(
            long inputUnits,
            long sourceUnit,
            long targetPerUnit,
            long referenceFluidUnits,
            long referenceMillibuckets,
            double inputDensity,
            long targetMelting,
            long actualFluidTemperature,
            long environmentTemperature,
            boolean lava) {
        if (inputUnits < 0
                || sourceUnit <= 0
                || targetPerUnit < 0
                || referenceFluidUnits <= 0
                || referenceMillibuckets < 0
                || !Double.isFinite(inputDensity)
                || inputDensity < 0
                || targetMelting < 0
                || actualFluidTemperature < 0
                || environmentTemperature < 0)
            throw new IllegalArgumentException("Unreviewed thermal arithmetic inputs");
        long target = unitsDown(inputUnits, sourceUnit, targetPerUnit);
        int mb = gregtech.content.recipe.OriginalMaterialFluidUnits.millibuckets(
                target, referenceFluidUnits, referenceMillibuckets, false);
        if (mb < 0) throw new ArithmeticException("Fluid amount exceeds native int capacity");
        // Source uses input mass, not output mass, and keeps this exact double expression/order.
        double weight = (inputDensity * 111.111111 * inputUnits) / sourceUnit;
        long delta = Math.subtractExact(Math.max(targetMelting, actualFluidTemperature), environmentTemperature);
        double thermal = (weight * delta) / 1600;
        // Original EU_PER_LAVA=80; ceil(mB*80/16), not the ordinary16-tick minimum.
        long lavaEnergy = Math.multiplyExact((long) mb, 80L);
        long base = lava ? lavaEnergy / 16 + (lavaEnergy % 16 == 0 ? 0 : 1) : 16;
        double duration = Math.max(base, thermal);
        if (!Double.isFinite(weight) || !Double.isFinite(thermal) || !Double.isFinite(duration) || duration >= 0x1.0p63)
            throw new ArithmeticException("Thermal duration exceeds supported long range");
        // The final positive long cast is the only duration rounding, AFTER max(base,thermal).
        return new Result(target, mb, (long) duration);
    }

    private static long unitsDown(long amount, long original, long target) {
        if (target == 0) return 0;
        if (original == target || original == 0) return amount;
        if (original % target == 0) {
            original /= target;
            target = 1;
        } else if (target % original == 0) {
            target /= original;
            original = 1;
        }
        // Retain the existing bounded helper's checked product; overflow is unsupported, not saturated.
        return Math.max(0, Math.multiplyExact(amount, target) / original);
    }
}
