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
 * requires every static row to have exactly two item outputs.
 */
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
        boolean exclusiveOutputs
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

    public enum Kind { BASIC, LARGE_RECIPE }

    public static Builder builder(String id, String material) { return new Builder(id, material); }

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

        public MachineDeclaration build() {
            return new MachineDeclaration(id, material, machineType, energy, energyPerTick, tier, parallel,
                    recipeMap, kind, efficiency, constantEnergy, structureKeys, facesSpecified,
                    itemInputs, itemOutputs, fluidInputs, fluidOutputs, energyInputs,
                    itemAutoInput, itemAutoOutput, fluidAutoInput, fluidAutoOutput,
                    cheapOverclocking, cheapOverclockingSpecified, minimumLightAbove,
                    searchOutputTanks, exclusiveOutputs);
        }
    }
}
