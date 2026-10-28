package gregtech.content.recipe;

import gregtech.engine.recipe.SourceRecyclingMath;
import java.util.Objects;

/**
 * Bounded pure decision for the existing one dust-or-ingot1U producer. The amount-aware core is
 * also shared by the separately proven single-component input extension, not a full listener.
 * Source facts, native runtime stats, actual item composition and phase identity are caller evidence.
 * No native METAL/ALLOY/generation flag establishes source MELTING, FURNACE or a fluid phase.
 */
public final class SourceThermalRecyclingRules {
    private SourceThermalRecyclingRules() {}

    public static final int POWER = 16;
    public static final long ENVIRONMENT_TEMPERATURE = 293;
    private static final long U = OriginalMaterialFluidUnits.SOURCE_UNIT;

    public enum Truth {
        YES,
        NO,
        UNKNOWN
    }

    public enum InputProof {
        VERIFIED_SELF_ONE_UNIT,
        ABSENT,
        UNKNOWN
    }

    public enum PhaseProof {
        VERIFIED_SINGLE_BINDING,
        PROVEN_NO_DEFAULT_PHASE,
        UNKNOWN
    }

    public enum Decision {
        SUPPORTED_ROW,
        SUPPORTED_NO_ROW,
        UNSUPPORTED
    }

    /** targetPerUnit is the audited source amount; input and target identity closure is external.
     * targetFurnace and targetSmeltsToCalcite describe the OUTPUT target, not the input.
     */
    public record Facts(
            Truth melting, Truth blacklisted, long targetPerUnit, Truth targetFurnace, Truth targetSmeltsToCalcite) {
        public Facts {
            Objects.requireNonNull(melting);
            Objects.requireNonNull(blacklisted);
            Objects.requireNonNull(targetFurnace);
            Objects.requireNonNull(targetSmeltsToCalcite);
        }
    }
    /** Valid runtime material stats, matching the source algorithm's data flow. null means unknown.
     * Imported float precision/source-stat parity is a separate, explicitly incomplete audit.
     */
    public record Stats(double inputDensity, long targetMelting) {}
    /** A verified phase may be liquid or the source Aerotheum gas exception. Path/accessor,
     * native identity, temperature provenance and external overrides are checked by the adapter.
     * PROVEN_NO_DEFAULT_PHASE is only valid when no unknown external phase override is present.
     */
    public record Phase(
            PhaseProof proof,
            long referenceUnits,
            long referenceMillibuckets,
            long actualFluidTemperature,
            boolean lava) {
        public Phase {
            Objects.requireNonNull(proof);
        }
    }

    public record Row(long targetUnits, int millibuckets, long ticks, boolean melter) {
        public boolean smelter() {
            return true;
        }

        public int power() {
            return POWER;
        }
    }

    public record Plan(Decision decision, Row row) {
        public Plan {
            Objects.requireNonNull(decision);
            if ((decision == Decision.SUPPORTED_ROW) != (row != null))
                throw new IllegalArgumentException("Only a supported row has row data");
        }
    }

    /** Only SUPPORTED_* takes ownership from the old producer. UNSUPPORTED preserves its
     * explicitly deferred compatibility policy; no later correct row can repair a collision.
     * Missing actual input proves no row. Unverified/mixed/non1U input never enters source rules.
     */
    public static Plan plan(InputProof input, Facts facts, Stats stats, Phase phase) {
        if (input == InputProof.ABSENT) return noRow();
        if (input != InputProof.VERIFIED_SELF_ONE_UNIT || facts == null) return unsupported();
        return planVerifiedAmount(U, facts, stats, phase);
    }

    /** Caller has already established the original single component and its actual source units.
     * This is not a simulated 1U input: conversion and mass both consume the real quantity.
     * Keeping the old public entry point above preserves its input proof and exact 1U behavior.
     */
    static Plan planVerifiedAmount(long inputUnits, Facts facts, Stats stats, Phase phase) {
        if (inputUnits <= 0 || facts == null) return unsupported();
        if (facts.targetPerUnit() < 0) return unsupported();
        if (facts.targetPerUnit() == 0 || facts.melting() == Truth.NO || facts.blacklisted() == Truth.YES)
            return noRow();
        if (phase != null && phase.proof() == PhaseProof.PROVEN_NO_DEFAULT_PHASE) return noRow();
        if (facts.melting() != Truth.YES
                || facts.blacklisted() != Truth.NO
                || phase == null
                || phase.proof() != PhaseProof.VERIFIED_SINGLE_BINDING
                || stats == null) return unsupported();
        try {
            var result = SourceRecyclingMath.singleFluid(
                    inputUnits,
                    U,
                    facts.targetPerUnit(),
                    phase.referenceUnits(),
                    phase.referenceMillibuckets(),
                    stats.inputDensity(),
                    stats.targetMelting(),
                    phase.actualFluidTemperature(),
                    ENVIRONMENT_TEMPERATURE,
                    phase.lava());
            if (result.millibuckets() == 0) return noRow();
            // The source checks this OR only after a positive output quantity is established.
            boolean melter = facts.targetFurnace() == Truth.YES || facts.targetSmeltsToCalcite() == Truth.YES;
            if (!melter && (facts.targetFurnace() == Truth.UNKNOWN || facts.targetSmeltsToCalcite() == Truth.UNKNOWN))
                return unsupported();
            return new Plan(
                    Decision.SUPPORTED_ROW,
                    new Row(result.targetUnits(), result.millibuckets(), result.ticks(), melter));
        } catch (IllegalArgumentException | ArithmeticException invalid) {
            // A reviewed numeric precondition failed; do not call it a source-proven NO.
            return unsupported();
        }
    }

    private static Plan noRow() {
        return new Plan(Decision.SUPPORTED_NO_ROW, null);
    }

    private static Plan unsupported() {
        return new Plan(Decision.UNSUPPORTED, null);
    }
}
