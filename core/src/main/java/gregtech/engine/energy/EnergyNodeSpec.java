package gregtech.engine.energy;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.GregTechTags;
import java.util.Objects;

/**
 * One GT6 energy-net node device (motor, dynamo, transformer, steam turbine,
 * solar panel, battery box, energy storage cabinet).
 *
 * <p>This record describes rated values, not a universal conversion algorithm.
 * Device rules determine packet limits, buffer consumption, accepted faces and
 * environmental conditions. A converter may waste buffered input even when its
 * output is refused. Steam turbines split a fluid batch over two ticks before
 * conversion; their input rate is not a fixed fluid cost per emitted packet.
 * Native block entities own fluid tanks, directional access and saved state.</p>
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
        int batterySlots,
        DeviceType deviceType) {
    /** Retains the original constructor for specifications created before device types were explicit. */
    public EnergyNodeSpec(
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
            int batterySlots) {
        this(
                id,
                material,
                kind,
                textureFolder,
                inType,
                outType,
                inputRate,
                outputRate,
                capacity,
                displayEn,
                batterySlots,
                null);
    }

    public EnergyNodeSpec {
        if (id == null || !id.matches("[a-z0-9/._-]+") || material == null || !material.isValid())
            throw new IllegalArgumentException("Invalid energy device identity/material: " + id);
        Objects.requireNonNull(kind, "Device kind");
        Objects.requireNonNull(textureFolder, "Device texture folder");
        Objects.requireNonNull(displayEn, "English name");
        if (!GregTechTags.Energy.ALL.contains(inType) || !GregTechTags.Energy.ALL.contains(outType))
            throw new IllegalArgumentException("Unknown energy type for " + id);
        if (batterySlots != 0 && batterySlots != 4 && batterySlots != 16)
            throw new IllegalArgumentException("Battery boxes require 4 or 16 slots: " + id);
        if (inputRate < 0 || outputRate < 0 || capacity < 0)
            throw new IllegalArgumentException("Negative energy device rate/capacity: " + id);
        if (deviceType == null) {
            deviceType = DeviceType.fromLegacySpec(id, kind, inType, batterySlots);
        } else {
            if (kind != deviceType.kind && !(deviceType == DeviceType.BUFFERED && kind == Kind.STORAGE))
                throw new IllegalArgumentException("Incompatible device type and kind: " + id);
            if ((deviceType == DeviceType.BATTERY_BOX) != (batterySlots > 0))
                throw new IllegalArgumentException("Only battery boxes and chargers have battery slots: " + id);
        }
    }

    public static Builder builder(String id, GTMaterial casing) {
        return new Builder(id, casing);
    }

    public static final class Builder {
        private final String id;
        private final GTMaterial casing;
        private Kind kind;
        private DeviceType deviceType;
        private String texture;
        private GregTechTags.Tag inputType;
        private GregTechTags.Tag outputType;
        private long input;
        private long output;
        private long capacity;
        private int batterySlots;
        private String english;

        private Builder(String id, GTMaterial casing) {
            this.id = id;
            this.casing = casing;
        }

        public Builder kind(Kind value) {
            kind = Objects.requireNonNull(value);
            return this;
        }

        public Builder deviceType(DeviceType value) {
            deviceType = Objects.requireNonNull(value);
            return this;
        }

        public Builder texture(String value) {
            texture = value;
            return this;
        }

        public Builder input(GregTechTags.Tag type, long rate) {
            inputType = type;
            input = rate;
            return this;
        }

        public Builder output(GregTechTags.Tag type, long rate) {
            outputType = type;
            output = rate;
            return this;
        }

        public Builder capacity(long value) {
            capacity = value;
            return this;
        }

        public Builder batterySlots(int count) {
            batterySlots = count;
            return this;
        }

        public Builder name(String english) {
            this.english = english;
            return this;
        }

        public EnergyNodeSpec build() {
            Kind defaultKind = deviceType == null ? Kind.CONVERTER : deviceType.kind;
            return new EnergyNodeSpec(
                    id,
                    casing,
                    kind == null ? defaultKind : kind,
                    texture,
                    inputType,
                    outputType,
                    input,
                    output,
                    capacity,
                    english,
                    batterySlots,
                    deviceType);
        }
    }

    public enum Kind {
        CONVERTER,
        MAGNET,
        TURBINE,
        SOLAR,
        STORAGE
    }

    /** The rule set shared by a device family across materials and voltage tiers. */
    public enum DeviceType {
        BUFFERED(Kind.CONVERTER),
        MOTOR(Kind.CONVERTER),
        DYNAMO(Kind.CONVERTER),
        HEATER(Kind.CONVERTER),
        COOLER(Kind.CONVERTER),
        MAGNET(Kind.MAGNET),
        STEAM_TURBINE(Kind.TURBINE),
        SOLAR(Kind.SOLAR),
        BATTERY_BOX(Kind.STORAGE),
        ELECTRIC_TRANSFORMER(Kind.CONVERTER),
        ROTATION_TRANSFORMER(Kind.CONVERTER);

        private final Kind kind;

        DeviceType(Kind kind) {
            this.kind = kind;
        }

        public boolean usesOriginalConverter() {
            return switch (this) {
                case MOTOR, DYNAMO, HEATER, COOLER, MAGNET, STEAM_TURBINE -> true;
                default -> false;
            };
        }

        /** Resolve legacy naming conventions once, when binding an old-style specification. */
        private static DeviceType fromLegacySpec(String id, Kind kind, GregTechTags.Tag inputType, int slots) {
            if (kind == Kind.MAGNET && (id.startsWith("electromagnet_") || id.startsWith("flux_magnet_")))
                return MAGNET;
            if (kind == Kind.TURBINE && id.startsWith("steam_turbine_")) return STEAM_TURBINE;
            if (id.startsWith("electric_motor_") || id.startsWith("flux_motor_")) return MOTOR;
            if (id.startsWith("electric_dynamo_") || id.startsWith("flux_dynamo_")) return DYNAMO;
            if (id.startsWith("electric_heater_") || id.startsWith("flux_heater_")) return HEATER;
            if (id.startsWith("electric_cooler_") || id.startsWith("flux_cooler_")) return COOLER;
            if (slots > 0) return BATTERY_BOX;
            if (kind == Kind.SOLAR) return SOLAR;
            if (id.startsWith("transformer_") && inputType == GregTechTags.Energy.EU) return ELECTRIC_TRANSFORMER;
            if (id.startsWith("rotation_transformer_")) return ROTATION_TRANSFORMER;
            return BUFFERED;
        }
    }
}
