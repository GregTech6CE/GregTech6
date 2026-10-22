package com.gregtech.gregtech.api.addon;

import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * One addon structure part. With no energy output and no energy input it is a casing.
 * {@code energyOutput} makes it an output part. {@code energyInput} plus {@code condition}
 * makes it a condition input. One part cannot both accept and emit.
 */
public final class LayoutPart {
    private static final java.util.regex.Pattern ENERGY_TYPE =
            java.util.regex.Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,15}");

    private final String name;
    private final String material;
    private final String texture;
    private final float hardness;
    private final EnergyOutput energy;
    private final long energyCapacity;
    private final EnergyInput energyInput;
    private final long conditionPerTick;

    private LayoutPart(String name, String material, String texture, float hardness,
                       EnergyOutput energy, long energyCapacity, EnergyInput energyInput, long conditionPerTick) {
        this.name = name;
        this.material = material;
        this.texture = texture;
        this.hardness = hardness;
        this.energy = energy;
        this.energyCapacity = energyCapacity;
        this.energyInput = energyInput;
        this.conditionPerTick = conditionPerTick;
    }

    public String name() { return name; }
    public String material() { return material; }
    public String texture() { return texture; }
    public float hardness() { return hardness; }

    /** Null when this part does not emit. */
    public EnergyOutput energy() { return energy; }

    /** Zero when this part does not emit. An output part is at least 1. */
    public long energyCapacity() { return energyCapacity; }

    /** Null when this part does not accept energy. */
    public EnergyInput energyInput() { return energyInput; }

    /** Zero when this part is not a condition. An input part is at least 1. */
    public long conditionPerTick() { return conditionPerTick; }

    public boolean condition() { return conditionPerTick > 0; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LayoutPart that)) return false;
        return Float.compare(hardness, that.hardness) == 0 && energyCapacity == that.energyCapacity
                && conditionPerTick == that.conditionPerTick
                && name.equals(that.name) && material.equals(that.material) && texture.equals(that.texture)
                && Objects.equals(energy, that.energy) && Objects.equals(energyInput, that.energyInput);
    }

    @Override public int hashCode() {
        return Objects.hash(name, material, texture, hardness, energy, energyCapacity, energyInput, conditionPerTick);
    }

    public static Builder builder(String name, String material) { return new Builder(name, material); }

    public static final class Builder {
        private final String name;
        private final String material;
        private String texture = "";
        private boolean hardnessSpecified;
        private float hardness;
        private EnergyOutput energy;
        private long energyCapacity;
        private EnergyInput energyInput;
        private long conditionPerTick;

        private Builder(String name, String material) {
            this.name = name;
            this.material = material;
        }

        public Builder texture(String path) { texture = path; return this; }

        public Builder hardness(float value) { hardnessSpecified = true; hardness = value; return this; }

        public Builder energyOutput(EnergyOutput output, long capacity) {
            if (output == null) throw new IllegalArgumentException("Missing energy output");
            energy = output;
            energyCapacity = capacity;
            return this;
        }

        /** Accepts energy into this part. Pair it with {@link #condition(long)}. */
        public Builder energyInput(EnergyInput input) {
            if (input == null) throw new IllegalArgumentException("Missing energy input");
            energyInput = input;
            return this;
        }

        /**
         * Energy removed from the input buffer each tick. The part is satisfied only when the
         * buffer can pay it. Only valid together with {@link #energyInput}.
         */
        public Builder condition(long perTick) {
            conditionPerTick = perTick;
            return this;
        }

        public LayoutPart build() {
            if (name == null || !name.matches("[a-z][a-z0-9_]{0,63}"))
                throw new IllegalArgumentException("Structure part name must be a lower-case registry path: " + name);
            if (material == null || material.isBlank())
                throw new IllegalArgumentException("Structure part '" + name + "' needs a material");
            var path = texture == null ? "" : texture;
            if (!path.isEmpty() && !path.matches("[a-z0-9]+(?:[._-][a-z0-9]+)*(?:/[a-z0-9]+(?:[._-][a-z0-9]+)*)*"))
                throw new IllegalArgumentException(
                        "Structure part '" + name + "' texture must be a path in the addon namespace: " + path);
            if (!hardnessSpecified || Float.isNaN(hardness) || hardness <= 0.0F)
                throw new IllegalArgumentException("Structure part '" + name + "' hardness must be positive");
            if (energy != null && energyInput != null)
                throw new IllegalArgumentException("Structure part '" + name + "' cannot both accept and emit");
            if (conditionPerTick != 0 && energyInput == null)
                throw new IllegalArgumentException("Structure part '" + name + "' condition requires an energy input");
            if (energy != null) {
                if (!ENERGY_TYPE.matcher(energy.type()).matches())
                    throw new IllegalArgumentException("Energy part type is invalid: " + energy.type());
                if (energyCapacity < 1)
                    throw new IllegalArgumentException("Energy part capacity must be positive: " + energyCapacity);
            }
            if (energyInput != null) validateInput(name, energyInput, conditionPerTick);
            return new LayoutPart(name, material, path, hardness, energy, energy == null ? 0 : energyCapacity,
                    energyInput, energyInput == null ? 0 : conditionPerTick);
        }

        private static void validateInput(String name, EnergyInput input, long conditionPerTick) {
            if (!ENERGY_TYPE.matcher(input.type()).matches())
                throw new IllegalArgumentException("Energy part type is invalid: " + input.type());
            if (input.anyPacket())
                throw new IllegalArgumentException("Structure part '" + name + "' cannot ignore packet size");
            if (!input.rangeSpecified())
                throw new IllegalArgumentException("Structure part '" + name + "' energy input needs a packet range");
            if (!input.facesSpecified() || input.faces().isEmpty())
                throw new IllegalArgumentException("Structure part '" + name + "' energy input needs a face");
            if (!input.capacitySpecified() || input.capacity() < 1)
                throw new IllegalArgumentException("Energy part capacity must be positive: " + input.capacity());
            if (input.perTick() < input.rangeMin() || input.perTick() > input.rangeMax())
                throw new IllegalArgumentException("Structure part '" + name
                        + "' energy input per tick must sit inside the packet range");
            if (conditionPerTick < 1)
                throw new IllegalArgumentException("Structure part '" + name + "' energy input requires a condition");
            if (conditionPerTick > input.capacity())
                throw new IllegalArgumentException("Structure part '" + name
                        + "' condition per tick exceeds the energy capacity");
        }
    }
}
