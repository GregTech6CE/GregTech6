/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;

import gregtech.api.addon.AddonNames;
import gregtech.api.energy.EnergyInput;
import gregtech.api.energy.EnergyOutput;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * One addon structure part. With no energy and no function it is a casing.
 * {@code energyOutput} makes it an output part. {@code energyInput} plus {@code condition}
 * makes it a condition input. A part may declare both: the input is then internal only,
 * it names no faces and no condition, and the controller fills the output buffer.
 * Slots, tanks, a menu, tick, use and named fields combine with either energy role.
 *
 * @param name the lower-case registry path
 * @param material the part material
 * @param texture the texture path, or empty
 * @param hardness block hardness
 * @param energy the energy output, or null
 * @param energyCapacity output buffer capacity, or 0 when the part does not emit
 * @param energyInput the energy input, or null
 * @param conditionPerTick energy removed each tick, or 0 when the part is not a condition
 * @param slots item slots, or null
 * @param tanks fluid tanks, or null
 * @param gui the gui texture path, or null
 * @param ticks true when the placed part receives a server tick
 * @param handlesUse true when a player click is offered to the part behavior
 * @param fields named values GregTech stores for this part
 */
public record LayoutPart(String name, String material, String texture, float hardness,
                         @Nullable EnergyOutput energy, long energyCapacity, @Nullable EnergyInput energyInput,
                         long conditionPerTick, @Nullable PartSlots slots, @Nullable PartTanks tanks,
                         @Nullable String gui, boolean ticks, boolean handlesUse, List<String> fields) {
    public static final int MAX_FIELDS = 16;
    private static final Pattern FIELD = Pattern.compile("[a-z0-9_.]{1,64}");

    public LayoutPart {
        if (!AddonNames.matches(AddonNames.LOWER_PATH, name))
            throw new IllegalArgumentException("Structure part name must be a lower-case registry path: " + name);
        if (!AddonNames.matches(AddonNames.MATERIAL, material))
            throw new IllegalArgumentException(
                    "Structure part '" + name + "' has an invalid material name: " + material);
        if (texture == null)
            throw new IllegalArgumentException(
                    "Structure part '" + name + "' texture is missing; pass an empty string when there is none");
        if (!texture.isEmpty() && !AddonNames.matches(AddonNames.SEGMENT_PATH, texture))
            throw new IllegalArgumentException(
                    "Structure part '" + name + "' texture must be a path in the addon namespace: " + texture);
        if (Float.isNaN(hardness) || hardness <= 0.0F)
            throw new IllegalArgumentException("Structure part '" + name + "' hardness must be positive");
        if (energy != null && energyInput != null
                && (conditionPerTick != 0 || (energyInput.facesSpecified() && !energyInput.faces().isEmpty())))
            throw new IllegalArgumentException("Structure part '" + name
                    + "' cannot both accept external energy and emit");
        if (conditionPerTick != 0 && energyInput == null)
            throw new IllegalArgumentException("Structure part '" + name + "' condition requires an energy input");
        if (energy == null) energyCapacity = 0;
        if (energyInput == null) conditionPerTick = 0;
        if (energy != null) {
            if (energy.type().wildcard())
                throw new IllegalArgumentException("Energy part type is invalid: " + energy.type());
            if (energyCapacity < 1)
                throw new IllegalArgumentException("Energy part capacity must be positive: " + energyCapacity);
        }
        if (energyInput != null) {
            if (energy != null) validateInternalInput(name, energyInput);
            else validateInput(name, energyInput, conditionPerTick);
        }
        if (fields == null) throw new IllegalArgumentException("Missing part fields");
        fields = List.copyOf(fields);
        var seen = new HashSet<String>();
        for (String key : fields) {
            requireField(key);
            if (!seen.add(key))
                throw new IllegalArgumentException("Repeated part field: " + key);
        }
        if (fields.size() > MAX_FIELDS)
            throw new IllegalArgumentException("Too many part fields");
    }

    public boolean condition() { return conditionPerTick > 0; }

    /** Slots, tanks, a menu, a tick, a click handler, or a named field. A casing and an energy-only part are false. */
    public boolean functional() {
        return slots != null || tanks != null || gui != null || ticks || handlesUse || !fields.isEmpty();
    }

    public static Builder builder(String name, String material) { return new Builder(name, material); }

    public static final class Builder {
        private final String name;
        private final String material;
        private String texture = "";
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

        public Builder hardness(float value) { hardness = value; return this; }

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
            requireField(key);
            if (fields.contains(key))
                throw new IllegalArgumentException("Repeated part field: " + key);
            if (fields.size() >= MAX_FIELDS)
                throw new IllegalArgumentException("Too many part fields");
            fields.add(key);
            return this;
        }

        public LayoutPart build() {
            return new LayoutPart(name, material, texture, hardness, energy, energyCapacity, energyInput,
                    conditionPerTick, slots, tanks, gui, ticks, handlesUse, fields);
        }
    }

    private static void requireField(String key) {
        if (key == null || !FIELD.matcher(key).matches())
            throw new IllegalArgumentException("Invalid part field: " + key);
    }

    /** A two-way part's input is only the controller-to-part channel. */
    private static void validateInternalInput(String name, EnergyInput input) {
        if (input.type().wildcard())
            throw new IllegalArgumentException("Energy part type is invalid: " + input.type());
        if (input.facesSpecified() && !input.faces().isEmpty())
            throw new IllegalArgumentException("Structure part '" + name
                    + "' cannot both accept external faces and emit");
        if (input.capacitySpecified() && input.capacity() < 0)
            throw new IllegalArgumentException("Energy part capacity must not be negative: " + input.capacity());
    }

    private static void validateInput(String name, EnergyInput input, long conditionPerTick) {
        if (input.type().wildcard())
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
