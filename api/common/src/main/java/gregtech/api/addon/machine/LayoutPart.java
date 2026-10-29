/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;




import gregtech.api.addon.*;

import gregtech.api.energy.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * One addon structure part. With no energy and no function it is a casing.
 * {@code energyOutput} makes it an output part. {@code energyInput} plus {@code condition}
 * makes it a condition input. A part may declare both: the input is then internal only,
 * it names no faces and no condition, and the controller fills the output buffer.
 * Slots, tanks, a menu, tick, use and named fields combine with either energy role.
 */
public final class LayoutPart {
    public static final int MAX_FIELDS = 16;
    private static final Pattern ENERGY_TYPE = EnergyType.NAME;
    private static final Pattern FIELD = Pattern.compile("[a-z0-9_.]{1,64}");

    private final String name;
    private final String material;
    private final String texture;
    private final float hardness;
    private final EnergyOutput energy;
    private final long energyCapacity;
    private final EnergyInput energyInput;
    private final long conditionPerTick;
    private final PartSlots slots;
    private final PartTanks tanks;
    private final String gui;
    private final boolean ticks;
    private final boolean handlesUse;
    private final List<String> fields;

    private LayoutPart(String name, String material, String texture, float hardness,
                       EnergyOutput energy, long energyCapacity, EnergyInput energyInput, long conditionPerTick,
                       PartSlots slots, PartTanks tanks, String gui, boolean ticks, boolean handlesUse,
                       List<String> fields) {
        this.name = name;
        this.material = material;
        this.texture = texture;
        this.hardness = hardness;
        this.energy = energy;
        this.energyCapacity = energyCapacity;
        this.energyInput = energyInput;
        this.conditionPerTick = conditionPerTick;
        this.slots = slots;
        this.tanks = tanks;
        this.gui = gui;
        this.ticks = ticks;
        this.handlesUse = handlesUse;
        this.fields = fields;
    }

    public String name() { return name; }
    public String material() { return material; }
    public String texture() { return texture; }
    public float hardness() { return hardness; }

    /** Null when this part does not emit. */
    public @Nullable EnergyOutput energy() { return energy; }

    /** Zero when this part does not emit. An output part is at least 1. */
    public long energyCapacity() { return energyCapacity; }

    /** Null when this part does not accept energy. */
    public @Nullable EnergyInput energyInput() { return energyInput; }

    /** Zero when this part is not a condition. An input part is at least 1. */
    public long conditionPerTick() { return conditionPerTick; }

    public boolean condition() { return conditionPerTick > 0; }

    /** Null when this part has no item slots. */
    public @Nullable PartSlots slots() { return slots; }

    /** Null when this part has no fluid tanks. */
    public @Nullable PartTanks tanks() { return tanks; }

    /** Null when a right click does not open this part's menu. A path in {@code namespace:path.png} form. */
    public @Nullable String gui() { return gui; }

    /** The placed part receives a server tick. */
    public boolean ticks() { return ticks; }

    /** A player click is offered to the part behavior. */
    public boolean handlesUse() { return handlesUse; }

    /** Named values GregTech stores for this part. Empty when the part has none. */
    public List<String> fields() { return fields; }

    /** Slots, tanks, a menu, a tick, a click handler, or a named field. A casing and an energy-only part are false. */
    public boolean functional() {
        return slots != null || tanks != null || gui != null || ticks || handlesUse || !fields.isEmpty();
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LayoutPart that)) return false;
        return Float.compare(hardness, that.hardness) == 0 && energyCapacity == that.energyCapacity
                && conditionPerTick == that.conditionPerTick && ticks == that.ticks && handlesUse == that.handlesUse
                && name.equals(that.name) && material.equals(that.material) && texture.equals(that.texture)
                && Objects.equals(energy, that.energy) && Objects.equals(energyInput, that.energyInput)
                && Objects.equals(slots, that.slots) && Objects.equals(tanks, that.tanks)
                && Objects.equals(gui, that.gui) && fields.equals(that.fields);
    }

    @Override public String toString() {
        return "LayoutPart[name=" + name + ", material=" + material + ", texture=" + texture
                + ", hardness=" + hardness + ", energy=" + energy + ", energyInput=" + energyInput
                + ", energyCapacity=" + energyCapacity + ", conditionPerTick=" + conditionPerTick
                + ", slots=" + slots + ", tanks=" + tanks + ", gui=" + gui
                + ", ticks=" + ticks + ", handlesUse=" + handlesUse + ", fields=" + fields + "]";
    }

    @Override public int hashCode() {
        return Objects.hash(name, material, texture, hardness, energy, energyCapacity, energyInput, conditionPerTick,
                slots, tanks, gui, ticks, handlesUse, fields);
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
        private PartSlots slots;
        private PartTanks tanks;
        private String gui;
        private boolean ticks;
        private boolean handlesUse;
        private final List<String> fields = new ArrayList<>();

        private Builder(String name, String material) {
            this.name = name;
            this.material = material;
        }

        public Builder texture(String path) { texture = path; return this; }

        public Builder hardness(float value) { hardnessSpecified = true; hardness = value; return this; }

        public Builder energyOutput(EnergyOutput output, long capacity) {
            if (output == null) throw new IllegalArgumentException("Missing energy output");
            if (output.remote()) throw new IllegalArgumentException("A structure part cannot use a remote output");
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

        /** This part keeps its own item slots. Pipes use the faces on {@code slots}. */
        public Builder slots(PartSlots value) {
            if (value == null) throw new IllegalArgumentException("Missing part slots");
            slots = value;
            return this;
        }

        /** This part keeps its own fluid tanks. Pipes use the faces on {@code tanks}. */
        public Builder tanks(PartTanks value) {
            if (value == null) throw new IllegalArgumentException("Missing part tanks");
            tanks = value;
            return this;
        }

        /**
         * Right click opens a menu of this part's slots, tanks and fields.
         * {@code texture} is {@code namespace:path.png}. {@code onUse} returning true consumes the click first.
         */
        public Builder gui(String texture) {
            if (!AddonNames.matches(AddonNames.TEXTURE, texture))
                throw new IllegalArgumentException("Invalid part gui texture: " + texture);
            gui = texture;
            return this;
        }

        /** Server tick for the part behavior. */
        public Builder tick() { ticks = true; return this; }

        /** Player use is offered to the part behavior, with the clicked face and position. */
        public Builder use() { handlesUse = true; return this; }

        /**
         * One value GregTech saves for this part. The key matches {@code [a-z0-9_.]{1,64}}.
         * At most {@link #MAX_FIELDS} keys. GregTech does not invent a meaning for the key.
         */
        public Builder field(String key) {
            if (key == null || !FIELD.matcher(key).matches())
                throw new IllegalArgumentException("Invalid part field: " + key);
            if (fields.contains(key))
                throw new IllegalArgumentException("Repeated part field: " + key);
            if (fields.size() >= MAX_FIELDS)
                throw new IllegalArgumentException("Too many part fields");
            fields.add(key);
            return this;
        }

        public LayoutPart build() {
            if (!AddonNames.matches(AddonNames.LOWER_PATH, name))
                throw new IllegalArgumentException("Structure part name must be a lower-case registry path: " + name);
            if (material == null || material.isBlank())
                throw new IllegalArgumentException("Structure part '" + name + "' needs a material");
            var path = texture == null ? "" : texture;
            if (!path.isEmpty() && !AddonNames.matches(AddonNames.SEGMENT_PATH, path))
                throw new IllegalArgumentException(
                        "Structure part '" + name + "' texture must be a path in the addon namespace: " + path);
            if (!hardnessSpecified || Float.isNaN(hardness) || hardness <= 0.0F)
                throw new IllegalArgumentException("Structure part '" + name + "' hardness must be positive");
            if (energy != null && energyInput != null
                    && (conditionPerTick != 0 || (energyInput.facesSpecified() && !energyInput.faces().isEmpty())))
                throw new IllegalArgumentException("Structure part '" + name
                        + "' cannot both accept external energy and emit");
            if (conditionPerTick != 0 && energyInput == null)
                throw new IllegalArgumentException("Structure part '" + name + "' condition requires an energy input");
            if (energy != null) {
                if (!ENERGY_TYPE.matcher(energy.type()).matches())
                    throw new IllegalArgumentException("Energy part type is invalid: " + energy.type());
                if (energyCapacity < 1)
                    throw new IllegalArgumentException("Energy part capacity must be positive: " + energyCapacity);
            }
            if (energyInput != null) {
                if (energy != null) validateInternalInput(name, energyInput);
                else validateInput(name, energyInput, conditionPerTick);
            }
            return new LayoutPart(name, material, path, hardness, energy, energy == null ? 0 : energyCapacity,
                    energyInput, energyInput == null ? 0 : conditionPerTick,
                    slots, tanks, gui, ticks, handlesUse, List.copyOf(fields));
        }

        /** A two-way part's input is only the controller-to-part channel. */
        private static void validateInternalInput(String name, EnergyInput input) {
            if (!ENERGY_TYPE.matcher(input.type()).matches())
                throw new IllegalArgumentException("Energy part type is invalid: " + input.type());
            if (input.facesSpecified() && !input.faces().isEmpty())
                throw new IllegalArgumentException("Structure part '" + name
                        + "' cannot both accept external faces and emit");
            if (input.capacitySpecified() && input.capacity() < 0)
                throw new IllegalArgumentException("Energy part capacity must not be negative: " + input.capacity());
        }

        private static void validateInput(String name, EnergyInput input, long conditionPerTick) {
            if (!ENERGY_TYPE.matcher(input.type()).matches())
                throw new IllegalArgumentException("Energy part type is invalid: " + input.type());
            if (input.anyPacket())
                throw new IllegalArgumentException("Structure part '" + name + "' cannot ignore packet size");
            if (!input.overload())
                throw new IllegalArgumentException("Structure part '" + name + "' cannot disable overload");
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
