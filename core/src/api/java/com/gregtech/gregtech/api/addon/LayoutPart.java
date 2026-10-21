package com.gregtech.gregtech.api.addon;

import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * One addon structure part. With no energy output it is a casing. {@code energyOutput}
 * makes it an energy part. Input energy and functional parts are later cards.
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

    private LayoutPart(String name, String material, String texture, float hardness,
                       EnergyOutput energy, long energyCapacity) {
        this.name = name;
        this.material = material;
        this.texture = texture;
        this.hardness = hardness;
        this.energy = energy;
        this.energyCapacity = energyCapacity;
    }

    public String name() { return name; }
    public String material() { return material; }
    public String texture() { return texture; }
    public float hardness() { return hardness; }

    /** Null when this part is only a casing. */
    public EnergyOutput energy() { return energy; }

    /** Zero when this part is only a casing. An energy part is at least 1. */
    public long energyCapacity() { return energyCapacity; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LayoutPart that)) return false;
        return Float.compare(hardness, that.hardness) == 0 && energyCapacity == that.energyCapacity
                && name.equals(that.name) && material.equals(that.material) && texture.equals(that.texture)
                && Objects.equals(energy, that.energy);
    }

    @Override public int hashCode() {
        return Objects.hash(name, material, texture, hardness, energy, energyCapacity);
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
            if (energy != null) {
                if (!ENERGY_TYPE.matcher(energy.type()).matches())
                    throw new IllegalArgumentException("Energy part type is invalid: " + energy.type());
                if (energyCapacity < 1)
                    throw new IllegalArgumentException("Energy part capacity must be positive: " + energyCapacity);
            }
            return new LayoutPart(name, material, path, hardness, energy, energy == null ? 0 : energyCapacity);
        }
    }
}
