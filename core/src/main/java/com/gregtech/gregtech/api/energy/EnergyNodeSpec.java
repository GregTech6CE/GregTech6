package com.gregtech.gregtech.api.energy;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.GregTechTags;

/**
 * One GT6 energy-net node device (motor, dynamo, transformer, steam turbine,
 * solar panel, battery box, energy storage cabinet).
 *
 * <p>Model: input faces are every side except the front (FACING); each emitted
 * packet of {@code outputRate} {@code outType} units costs {@code inputRate}
 * {@code inType} units from the internal buffer (GT6 conversion losses are the
 * rate difference). Turbines take steam as fluid ({@code inputRate} = L per
 * packet); solar panels generate {@code outputRate} per bright daytime tick.</p>
 */
public record EnergyNodeSpec(
        String id,
        GTMaterial material,
        Kind kind,
        String textureFolder,
        GregTechTags.Tag inType,
        GregTechTags.Tag outType,
        long inputRate,
        long outputRate,
        long capacity,
        String displayEn,
        String displayZh,
        int batterySlots
) {
    public EnergyNodeSpec {
        if (id == null || !id.matches("[a-z0-9/._-]+") || material == null || !material.isValid())
            throw new IllegalArgumentException("Invalid energy device identity/material: " + id);
        java.util.Objects.requireNonNull(kind, "Device kind");
        java.util.Objects.requireNonNull(textureFolder, "Device texture folder");
        java.util.Objects.requireNonNull(displayEn, "English name");
        java.util.Objects.requireNonNull(displayZh, "Chinese name");
        if (!GregTechTags.Energy.ALL.contains(inType) || !GregTechTags.Energy.ALL.contains(outType))
            throw new IllegalArgumentException("Unknown energy type for " + id);
        if (batterySlots != 0 && batterySlots != 4 && batterySlots != 16)
            throw new IllegalArgumentException("Battery boxes require 4 or 16 slots: " + id);
        if (inputRate < 0 || outputRate < 0 || capacity < 0)
            throw new IllegalArgumentException("Negative energy device rate/capacity: " + id);
    }

    public static Builder builder(String id, GTMaterial casing) { return new Builder(id, casing); }

    public static final class Builder {
        private final String id;
        private final GTMaterial casing;
        private Kind kind = Kind.CONVERTER;
        private String texture;
        private GregTechTags.Tag inputType;
        private GregTechTags.Tag outputType;
        private long input;
        private long output;
        private long capacity;
        private int batterySlots;
        private String english;
        private String chinese;
        private Builder(String id, GTMaterial casing) { this.id = id; this.casing = casing; }
        public Builder kind(Kind value) { kind = value; return this; }
        public Builder texture(String value) { texture = value; return this; }
        public Builder input(GregTechTags.Tag type, long rate) { inputType = type; input = rate; return this; }
        public Builder output(GregTechTags.Tag type, long rate) { outputType = type; output = rate; return this; }
        public Builder capacity(long value) { capacity = value; return this; }
        public Builder batterySlots(int count) { batterySlots=count; return this; }
        public Builder names(String english, String chinese) { this.english = english; this.chinese = chinese; return this; }
        public EnergyNodeSpec build() {
            return new EnergyNodeSpec(id, casing, kind, texture, inputType, outputType, input, output, capacity, english, chinese, batterySlots);
        }
    }
    public enum Kind { CONVERTER, MAGNET, TURBINE, SOLAR, STORAGE }
}
