package com.gregtech.gregtech.api.addon;

import java.util.List;
import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * One addon recipe machine. {@code constantEnergy} is the non-timed rule: the machine draws a
 * constant amount whenever it is not a TU machine. {@code efficiency} is parts per 10000.
 * {@code energy} is the short name, such as {@code EU}.
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
        List<String> structureKeys
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
        structureKeys = List.copyOf(structureKeys == null ? List.of() : structureKeys);
        for (String key : structureKeys) {
            if (key == null || !key.matches("[a-z0-9_.]+"))
                throw new IllegalArgumentException("Invalid structure key for " + id + ": " + key);
        }
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

        public MachineDeclaration build() {
            return new MachineDeclaration(id, material, machineType, energy, energyPerTick, tier, parallel,
                    recipeMap, kind, efficiency, constantEnergy, structureKeys);
        }
    }
}
