package com.gregtech.gregtech.api.addon;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * One addon recipe machine. {@code constantEnergy} is the non-timed rule: the machine draws a
 * constant amount whenever it is not a TU machine. {@code efficiency} is parts per 10000.
 * {@code energy} is the short name, such as {@code EU}.
 *
 * <p>Face lists use {@link BehaviorMachineDeclaration.Face}. Leaving every face method unset keeps
 * today's six-side item and fluid access, six-side energy input, and no automatic face.
 * {@code cheapOverclockingSpecified} is false until the builder sets it. A basic machine still
 * reads the boolean, whose default is off, and does not consult the built-in name table.
 * {@code minimumLightAbove} of 0 does not check the block above.
 * {@code searchOutputTanks} tries the output tanks only after the input tanks miss, and that job
 * drains the tanks that matched. {@code exclusiveOutputs} rolls the first item chance once and
 * requires every static row to have exactly two item outputs. A large recipe machine may name one
 * cell for item output, fluid output, item input and fluid input. The origin is rejected. Each
 * component is from -16 to 16. Naming a cell leaves that channel's automatic face unset; setting both fails.
 * {@code energyInMin} and {@code energyInMax} stay {@code 0} until {@code energyRange} is set.
 * A TU machine cannot declare a range. The nominal input may sit outside the range.
 *
 * @deprecated Use {@link RecipeMachine}. Removal is 0.2.
 */
@Deprecated(since = "0.0", forRemoval = true)
@AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.MachineDeclaration", removeAt = "0.2")
public record MachineDeclaration(
        String id,
        String material,
        String machineType,
        String energy,
        long energyPerTick,
        int tier,
        int parallel,
        String recipeMap,
        Kind kind,
        int efficiency,
        boolean constantEnergy,
        List<String> structureKeys,
        boolean facesSpecified,
        List<BehaviorMachineDeclaration.Face> itemInputs,
        List<BehaviorMachineDeclaration.Face> itemOutputs,
        List<BehaviorMachineDeclaration.Face> fluidInputs,
        List<BehaviorMachineDeclaration.Face> fluidOutputs,
        List<BehaviorMachineDeclaration.Face> energyInputs,
        BehaviorMachineDeclaration.Face itemAutoInput,
        BehaviorMachineDeclaration.Face itemAutoOutput,
        BehaviorMachineDeclaration.Face fluidAutoInput,
        BehaviorMachineDeclaration.Face fluidAutoOutput,
        boolean cheapOverclocking,
        boolean cheapOverclockingSpecified,
        int minimumLightAbove,
        boolean searchOutputTanks,
        boolean exclusiveOutputs,
        Target itemOutputTarget,
        Target fluidOutputTarget,
        Target itemInputSource,
        Target fluidInputSource,
        long energyInMin,
        long energyInMax
) {
    public MachineDeclaration {
        if (id == null || !id.matches("[a-z0-9/._-]+"))
            throw new IllegalArgumentException("Invalid machine ID: " + id);
        if (material == null || material.isBlank())
            throw new IllegalArgumentException("Missing casing material for " + id);
        if (machineType == null || !machineType.matches("[a-z0-9_]+"))
            throw new IllegalArgumentException("Invalid machine type for " + id + ": " + machineType);
        if (energy == null || energy.isBlank())
            throw new IllegalArgumentException("Missing energy type for " + id);
        if (recipeMap == null || !recipeMap.matches("[a-z0-9_.]+"))
            throw new IllegalArgumentException("Invalid recipe map name: " + recipeMap);
        Objects.requireNonNull(kind, "kind");
        if (kind != Kind.BASIC && kind != Kind.LARGE_RECIPE)
            throw new IllegalArgumentException("Addon machine '" + id + "' cannot use controller kind " + kind);
        if (tier < 1 || parallel < 1 || energyPerTick < 0 || efficiency < 1 || efficiency > 100_000)
            throw new IllegalArgumentException("Invalid machine parameters for " + id);
        if (minimumLightAbove < 0 || minimumLightAbove > 15)
            throw new IllegalArgumentException("Invalid light requirement for " + id + ": " + minimumLightAbove);
        structureKeys = List.copyOf(structureKeys == null ? List.of() : structureKeys);
        for (String key : structureKeys) {
            if (key == null || !key.matches("[a-z0-9_.]+"))
                throw new IllegalArgumentException("Invalid structure key for " + id + ": " + key);
        }
        itemInputs = copyFaces(id, itemInputs);
        itemOutputs = copyFaces(id, itemOutputs);
        fluidInputs = copyFaces(id, fluidInputs);
        fluidOutputs = copyFaces(id, fluidOutputs);
        energyInputs = copyFaces(id, energyInputs);
        if (kind != Kind.LARGE_RECIPE && (itemOutputTarget != null || fluidOutputTarget != null
                || itemInputSource != null || fluidInputSource != null))
            throw new IllegalArgumentException("IO targets require a large recipe machine: " + id);
        rejectAutomaticFace(id, "item output", itemOutputTarget, itemAutoOutput);
        rejectAutomaticFace(id, "fluid output", fluidOutputTarget, fluidAutoOutput);
        rejectAutomaticFace(id, "item input", itemInputSource, itemAutoInput);
        rejectAutomaticFace(id, "fluid input", fluidInputSource, fluidAutoInput);
        if (energyInMin != 0 || energyInMax != 0) {
            if (energyInMin < 1 || energyInMax < energyInMin)
                throw new IllegalArgumentException("Invalid energy range for " + id + ": "
                        + energyInMin + ".." + energyInMax);
            if ("TU".equalsIgnoreCase(energy))
                throw new IllegalArgumentException("TU machines cannot declare an energy range: " + id);
        }
    }

    /** {@code false} when both bounds are {@code 0}, which is the unset range. */
    public boolean energyRangeSpecified() { return energyInMin != 0 || energyInMax != 0; }

    private static void rejectAutomaticFace(String id, String channel, Target target, BehaviorMachineDeclaration.Face automatic) {
        if (target != null && automatic != null)
            throw new IllegalArgumentException(channel + " target conflicts with an automatic face: " + id);
    }

    private static List<BehaviorMachineDeclaration.Face> copyFaces(String id, List<BehaviorMachineDeclaration.Face> faces) {
        var copy = new ArrayList<BehaviorMachineDeclaration.Face>();
        if (faces != null) {
            for (BehaviorMachineDeclaration.Face face : faces) {
                if (face == null) throw new IllegalArgumentException("Missing face for " + id);
                copy.add(face);
            }
        }
        return List.copyOf(copy);
    }

    /**
     * @deprecated Use {@link RecipeMachine#basic()} or {@link RecipeMachine#largeRecipe()}. Removal is 0.2.
     */
    @Deprecated(since = "0.0", forRemoval = true)
    @AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.MachineDeclaration.Kind", removeAt = "0.2")
    public enum Kind { BASIC, LARGE_RECIPE }

    /**
     * One cell, in the same coordinates as {@link MachineLayout.Cell}, plus the face that receives the transfer.
     *
     * @deprecated Use {@link IoTarget}. Removal is 0.2.
     */
    @Deprecated(since = "0.0", forRemoval = true)
    @AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.MachineDeclaration.Target", removeAt = "0.2")
    public record Target(int right, int up, int back, BehaviorMachineDeclaration.Face receiver) {
        public Target {
            if (receiver == null) throw new IllegalArgumentException("Missing receiver");
            if (right == 0 && up == 0 && back == 0)
                throw new IllegalArgumentException("IO target cannot be the origin");
            if (right < -16 || right > 16 || up < -16 || up > 16 || back < -16 || back > 16)
                throw new IllegalArgumentException("IO target is out of range: " + right + "," + up + "," + back);
        }
    }

    public static Builder builder(String id, String material) { return new Builder(id, material); }

    /**
     * @deprecated Use {@link RecipeMachine#builder(MachineCasing)}. Removal is 0.2.
     */
    @Deprecated(since = "0.0", forRemoval = true)
    @AddonApiRemoval(id = "com.gregtech.gregtech.api.addon.MachineDeclaration.Builder", removeAt = "0.2")
    public static final class Builder {
        private final String id;
        private final String material;
        private String machineType;
        private String energy;
        private long energyPerTick;
        private int tier = 1;
        private int parallel = 1;
        private String recipeMap;
        private Kind kind = Kind.BASIC;
        private int efficiency = 10_000;
        private boolean constantEnergy = true;
        private List<String> structureKeys = List.of();
        private boolean facesSpecified;
        private List<BehaviorMachineDeclaration.Face> itemInputs = List.of();
        private List<BehaviorMachineDeclaration.Face> itemOutputs = List.of();
        private List<BehaviorMachineDeclaration.Face> fluidInputs = List.of();
        private List<BehaviorMachineDeclaration.Face> fluidOutputs = List.of();
        private List<BehaviorMachineDeclaration.Face> energyInputs = List.of();
        private BehaviorMachineDeclaration.Face itemAutoInput;
        private BehaviorMachineDeclaration.Face itemAutoOutput;
        private BehaviorMachineDeclaration.Face fluidAutoInput;
        private BehaviorMachineDeclaration.Face fluidAutoOutput;
        private boolean cheapOverclocking;
        private boolean cheapOverclockingSpecified;
        private int minimumLightAbove;
        private boolean searchOutputTanks;
        private boolean exclusiveOutputs;
        private Target itemOutputTarget;
        private Target fluidOutputTarget;
        private Target itemInputSource;
        private Target fluidInputSource;
        private long energyInMin;
        private long energyInMax;

        private Builder(String id, String material) {
            this.id = id;
            this.material = material;
            this.machineType = id;
        }

        public Builder machineType(String value) { machineType = value; return this; }
        public Builder energy(String shortName, long perTick) { energy = shortName; energyPerTick = perTick; return this; }
        public Builder tier(int value) { tier = value; return this; }
        public Builder parallel(int value) { parallel = value; return this; }
        public Builder recipeMap(String value) { recipeMap = value; return this; }
        public Builder kind(Kind value) { kind = value; return this; }
        public Builder basic() { return kind(Kind.BASIC); }
        public Builder largeRecipe() { return kind(Kind.LARGE_RECIPE); }
        public Builder efficiency(int value) { efficiency = value; return this; }
        public Builder constantEnergy(boolean value) { constantEnergy = value; return this; }
        public Builder structureKeys(List<String> value) { structureKeys = value; return this; }
        public Builder itemInputs(BehaviorMachineDeclaration.Face... faces) { facesSpecified = true; itemInputs = List.of(faces); return this; }
        public Builder itemOutputs(BehaviorMachineDeclaration.Face... faces) { facesSpecified = true; itemOutputs = List.of(faces); return this; }
        public Builder fluidInputs(BehaviorMachineDeclaration.Face... faces) { facesSpecified = true; fluidInputs = List.of(faces); return this; }
        public Builder fluidOutputs(BehaviorMachineDeclaration.Face... faces) { facesSpecified = true; fluidOutputs = List.of(faces); return this; }
        public Builder energyInputs(BehaviorMachineDeclaration.Face... faces) { facesSpecified = true; energyInputs = List.of(faces); return this; }
        public Builder itemAutoInput(BehaviorMachineDeclaration.Face face) { facesSpecified = true; itemAutoInput = face; return this; }
        public Builder itemAutoOutput(BehaviorMachineDeclaration.Face face) { facesSpecified = true; itemAutoOutput = face; return this; }
        public Builder fluidAutoInput(BehaviorMachineDeclaration.Face face) { facesSpecified = true; fluidAutoInput = face; return this; }
        public Builder fluidAutoOutput(BehaviorMachineDeclaration.Face face) { facesSpecified = true; fluidAutoOutput = face; return this; }
        public Builder cheapOverclocking(boolean value) { cheapOverclocking = value; cheapOverclockingSpecified = true; return this; }
        public Builder minimumLightAbove(int value) { minimumLightAbove = value; return this; }
        public Builder searchOutputTanks(boolean value) { searchOutputTanks = value; return this; }
        public Builder exclusiveOutputs(boolean value) { exclusiveOutputs = value; return this; }
        public Builder itemOutputTarget(int right, int up, int back, BehaviorMachineDeclaration.Face receiver) {
            itemOutputTarget = new Target(right, up, back, receiver); return this;
        }
        public Builder fluidOutputTarget(int right, int up, int back, BehaviorMachineDeclaration.Face receiver) {
            fluidOutputTarget = new Target(right, up, back, receiver); return this;
        }
        public Builder itemInputSource(int right, int up, int back, BehaviorMachineDeclaration.Face receiver) {
            itemInputSource = new Target(right, up, back, receiver); return this;
        }
        public Builder fluidInputSource(int right, int up, int back, BehaviorMachineDeclaration.Face receiver) {
            fluidInputSource = new Target(right, up, back, receiver); return this;
        }
        /** Accepted packet size. {@code 0, 0} leaves the range unset. {@code 1 <= min <= max}. */
        public Builder energyRange(long min, long max) { energyInMin = min; energyInMax = max; return this; }

        public MachineDeclaration build() {
            return new MachineDeclaration(id, material, machineType, energy, energyPerTick, tier, parallel,
                    recipeMap, kind, efficiency, constantEnergy, structureKeys, facesSpecified,
                    itemInputs, itemOutputs, fluidInputs, fluidOutputs, energyInputs,
                    itemAutoInput, itemAutoOutput, fluidAutoInput, fluidAutoOutput,
                    cheapOverclocking, cheapOverclockingSpecified, minimumLightAbove,
                    searchOutputTanks, exclusiveOutputs,
                    itemOutputTarget, fluidOutputTarget, itemInputSource, fluidInputSource,
                    energyInMin, energyInMax);
        }
    }
}
