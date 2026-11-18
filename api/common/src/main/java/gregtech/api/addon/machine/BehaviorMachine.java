/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;

import gregtech.api.addon.AddonLimits;
import gregtech.api.addon.AddonNames;
import gregtech.api.addon.AuxiliaryEnergy;
import gregtech.api.energy.EnergyInput;
import gregtech.api.energy.EnergyOutput;
import gregtech.api.machine.MachineCasing;
import gregtech.api.machine.MachineFace;
import gregtech.api.machine.MachineIo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * One addon behavior machine. GregTech builds the block, inventory, tanks, menu and screen.
 * The machine type is the id unchanged; call {@code machineType(...)} when the id is not a valid
 * machine type (for example it contains a slash). Tier is 1 to 255.
 * Energy input defaults to every face except the front. Capacity 0 keeps the rated-input drain.
 * The main input may declare a packet range or {@code anyPacket()}, and it may turn overload off.
 * It may be omitted when the machine emits. {@code auxiliaryEnergy} is a second buffer the
 * behavior reads by type. It may share a face with the main input when the energy types differ.
 * {@code io} limits item and fluid faces. Leaving it unset keeps every face open.
 * The default GUI geometry (widget sizes and texture atlas coordinates) is exposed by
 * {@code BehaviorMachineGui} in the platform layer; validation here uses the same fixed widget sizes.
 *
 * @param casing the resolved block casing
 * @param energyPorts the main input, auxiliary input and output
 * @param blockEntityId the registry path; empty becomes {@code be_<id>}
 * @param guiLayout the texture, screen geometry and widgets
 * @param automation the item and fluid faces, gas valve and signal output
 * @param runtimeOptions the behavior range, recipe maps and client state keys
 */
public record BehaviorMachine(
        MachineCasing casing,
        EnergyPorts energyPorts,
        String blockEntityId,
        GuiLayout guiLayout,
        Automation automation,
        RuntimeOptions runtimeOptions
) implements AddonMachine {
    private static final Pattern BLOCK_ENTITY_PATH = Pattern.compile("[a-z0-9_./-]{1,64}");
    private static final Pattern STATE_KEY = Pattern.compile("[a-z0-9_.]{1,64}");
    private static final Pattern TRANSLATION_KEY = Pattern.compile("[a-z0-9_.]+");
    // Fixed widget geometry used only to validate a declaration. The public rendering geometry is
    // BehaviorMachineGui in the platform layer; keep these sizes in sync with it.
    private static final int SLOT_SIZE = 18;
    private static final int TANK_WIDTH = 18;
    private static final int TANK_HEIGHT = 54;
    private static final int ENERGY_WIDTH = 10;
    private static final int ENERGY_HEIGHT = 54;
    private static final int PROGRESS_WIDTH = 24;
    private static final int PROGRESS_HEIGHT = 17;
    private static final int PLAYER_INVENTORY_GAP = 82;
    private static final int MIN_GUI_WIDTH = 176;
    private static final int MAX_GUI_WIDTH = 256;
    private static final int MIN_GUI_HEIGHT = 166;
    private static final int MAX_GUI_HEIGHT = 512;
    private static final int MAX_ITEM_SLOTS = 54;
    private static final int MAX_FLUID_SLOTS = 8;
    private static final int MAX_TOGGLES = 16;
    private static final int MAX_CLIENT_STATE_KEYS = 32;
    private static final int DEFAULT_ENERGY_X = 8;
    private static final int DEFAULT_ENERGY_Y = 8;
    private static final int DEFAULT_PROGRESS_X = 80;
    private static final int DEFAULT_PROGRESS_Y = 35;

    public BehaviorMachine {
        if (casing == null) throw new IllegalArgumentException("Missing casing");
        String id = casing.id();
        if (automation == null) throw new IllegalArgumentException("Missing automation for " + id);
        var io = automation.io();
        if (io != null) {
            if (io.hasTarget())
                throw new IllegalArgumentException("Behavior machines cannot declare an IO target");
            if (io.itemAutoInput() != null || io.itemAutoOutput() != null
                    || io.fluidAutoInput() != null || io.fluidAutoOutput() != null)
                throw new IllegalArgumentException("Behavior machines cannot declare an automatic face");
        }
        String machineType = casing.machineType() == null ? id : casing.machineType();
        if (!AddonNames.matches(AddonNames.MACHINE_TYPE, machineType))
            throw new IllegalArgumentException("Invalid machine type for " + id + ": " + machineType
                    + (casing.machineType() == null
                            ? "; call machineType(...) when the id is not a valid machine type" : ""));
        int tier = casing.tierSpecified() ? casing.tier() : 1;
        if (tier < 1 || tier > AddonLimits.MAX_TIER)
            throw new IllegalArgumentException("Invalid tier for " + id + ": " + tier);
        if (energyPorts == null || (energyPorts.input() == null && energyPorts.output() == null))
            throw new IllegalArgumentException("Missing energy type for " + id);
        if (guiLayout == null) throw new IllegalArgumentException("Missing gui layout for " + id);
        if (runtimeOptions == null) throw new IllegalArgumentException("Missing runtime options for " + id);
        if (guiLayout.items() == null || guiLayout.fluids() == null || guiLayout.toggles() == null
                || runtimeOptions.recipeMaps() == null || runtimeOptions.clientState() == null)
            throw new IllegalArgumentException("Missing behavior machine lists for " + id);
        var energy = energyPorts.input();
        var storedEnergy = energy == null ? null : mainInput(id, energy);
        var storedAuxiliary = AuxiliaryEnergy.validate("Behavior", id, storedEnergy, energyPorts.auxiliary());
        if (blockEntityId == null)
            throw new IllegalArgumentException(
                    "Missing block entity id for " + id + "; pass an empty string to derive one");
        if (!blockEntityId.isEmpty() && !AddonNames.matches(BLOCK_ENTITY_PATH, blockEntityId))
            throw new IllegalArgumentException("Invalid block entity id for " + id + ": " + blockEntityId);
        guiLayout.validateGeometry(id);
        runtimeOptions.validateRangeAndRecipeMaps(id);
        guiLayout.validateWidgetCounts(id);
        runtimeOptions.validateClientState(id);
        guiLayout.validateWidgets(id, storedEnergy != null);
        casing = casing.resolve(machineType, true, tier);
        energyPorts = new EnergyPorts(storedEnergy, storedAuxiliary, energyPorts.output());
    }

    public @Nullable EnergyInput energy() { return energyPorts.input(); }
    public @Nullable EnergyInput auxiliary() { return energyPorts.auxiliary(); }
    public @Nullable EnergyOutput output() { return energyPorts.output(); }
    public int guiWidth() { return guiLayout.width(); }
    public int guiHeight() { return guiLayout.height(); }
    public String gui() { return guiLayout.texture(); }
    public int energyX() { return guiLayout.energyX(); }
    public int energyY() { return guiLayout.energyY(); }
    public List<ItemSlot> items() { return guiLayout.items(); }
    public List<FluidSlot> fluids() { return guiLayout.fluids(); }
    public List<Toggle> toggles() { return guiLayout.toggles(); }
    public Progress progress() { return guiLayout.progress(); }
    public int maxRange() { return runtimeOptions.maxRange(); }
    public List<String> recipeMaps() { return runtimeOptions.recipeMaps(); }
    public @Nullable MachineIo io() { return automation.io(); }
    public @Nullable MachineFace gasValve() { return automation.gasValve(); }
    public boolean signalOutput() { return automation.signalOutput(); }
    public List<String> clientState() { return runtimeOptions.clientState(); }

    /** Registry path. An empty block entity id becomes {@code be_<id>}. */
    public String blockEntityPath() {
        return blockEntityId.isEmpty() ? "be_" + casing.id().replace('/', '_') : blockEntityId;
    }

    public int playerInventoryY() { return guiLayout.height() - PLAYER_INVENTORY_GAP; }

    /** Faces that accept the main energy input. Empty when the machine declares no main input. */
    public Set<MachineFace> energyFaces() {
        var energy = energyPorts.input();
        return energy == null ? Set.of() : Set.copyOf(energy.faces());
    }

    /** Faces that accept the auxiliary energy input. Empty when the machine declares none. */
    public Set<MachineFace> auxiliaryFaces() {
        var auxiliary = energyPorts.auxiliary();
        return auxiliary == null ? Set.of() : Set.copyOf(auxiliary.faces());
    }

    public boolean emitsEnergy() { return energyPorts.output() != null; }

    /** Faces that emit the declared output. Empty when the machine does not emit. */
    public Set<MachineFace> outputFaces() {
        var output = energyPorts.output();
        return output == null ? Set.of() : Set.copyOf(output.faces());
    }

    /** Item-insert faces. An unset {@code io} is every face. A named {@code io} leaves an unnamed channel closed. */
    public Set<MachineFace> itemInputFaces() {
        var io = automation.io();
        return faces(io == null ? null : io.itemInputs());
    }
    /** Item-extract faces. Same default as {@link #itemInputFaces()}. */
    public Set<MachineFace> itemOutputFaces() {
        var io = automation.io();
        return faces(io == null ? null : io.itemOutputs());
    }
    /** Fluid-fill faces. Same default as {@link #itemInputFaces()}. */
    public Set<MachineFace> fluidInputFaces() {
        var io = automation.io();
        return faces(io == null ? null : io.fluidInputs());
    }
    /** Fluid-drain faces. Same default as {@link #itemInputFaces()}. */
    public Set<MachineFace> fluidOutputFaces() {
        var io = automation.io();
        return faces(io == null ? null : io.fluidOutputs());
    }

    private static Set<MachineFace> faces(@Nullable List<MachineFace> faces) {
        return faces == null ? Set.copyOf(MachineFace.all()) : Set.copyOf(faces);
    }

    public int initialToggles() {
        var toggles = guiLayout.toggles();
        int bits = 0;
        for (int i = 0; i < toggles.size(); i++) if (toggles.get(i).initiallyOn()) bits |= 1 << i;
        return bits;
    }

    public enum SlotRole { INPUT, OUTPUT, ANY }

    public record ItemSlot(int x, int y, SlotRole role) {}
    public record FluidSlot(int capacity, int x, int y, SlotRole role) {}
    public record Toggle(String translationKey, int x, int y, boolean initiallyOn) {}
    public record Progress(int x, int y) {}

    /** Energy ports as declared by the addon. A null port is absent. */
    public record EnergyPorts(@Nullable EnergyInput input, @Nullable EnergyInput auxiliary,
                              @Nullable EnergyOutput output) {}

    /** Screen geometry and widgets for the host-supplied menu. */
    public record GuiLayout(
            int width, int height, String texture, int energyX, int energyY,
            List<ItemSlot> items, List<FluidSlot> fluids, List<Toggle> toggles, Progress progress
    ) {
        public GuiLayout {
            items = snapshot(items);
            fluids = snapshot(fluids);
            toggles = snapshot(toggles);
        }

        private void validateGeometry(String id) {
            if (width < MIN_GUI_WIDTH || width > MAX_GUI_WIDTH
                    || height < MIN_GUI_HEIGHT || height > MAX_GUI_HEIGHT)
                throw new IllegalArgumentException("Invalid gui size for " + id + ": "
                        + width + "x" + height);
            if (!AddonNames.matches(AddonNames.TEXTURE, texture))
                throw new IllegalArgumentException("Invalid gui texture for " + id + ": " + texture);
            if (progress == null) throw new IllegalArgumentException("Missing progress for " + id);
        }

        private void validateWidgetCounts(String id) {
            if (items.size() > MAX_ITEM_SLOTS)
                throw new IllegalArgumentException("Behavior machine '" + id + "' has " + items.size()
                        + " item slots; at most " + MAX_ITEM_SLOTS);
            if (fluids.size() > MAX_FLUID_SLOTS)
                throw new IllegalArgumentException("Behavior machine '" + id + "' has " + fluids.size()
                        + " fluid slots; at most " + MAX_FLUID_SLOTS);
            if (toggles.size() > MAX_TOGGLES)
                throw new IllegalArgumentException("Behavior machine '" + id + "' has " + toggles.size()
                        + " toggles; at most " + MAX_TOGGLES);
        }

        private void validateWidgets(String id, boolean showEnergy) {
            for (FluidSlot fluid : fluids) {
                if (fluid.capacity() < 1 || fluid.capacity() > AddonLimits.MAX_CAPACITY)
                    throw new IllegalArgumentException("Invalid tank capacity for " + id);
            }
            for (Toggle toggle : toggles) {
                if (!AddonNames.matches(TRANSLATION_KEY, toggle.translationKey()))
                    throw new IllegalArgumentException("Invalid toggle key for " + id + ": " + toggle.translationKey());
            }
            var widgets = new ArrayList<Widget>();
            if (showEnergy)
                widgets.add(new Widget("energy", energyX, energyY, ENERGY_WIDTH, ENERGY_HEIGHT));
            widgets.add(new Widget("progress", progress.x(), progress.y(), PROGRESS_WIDTH, PROGRESS_HEIGHT));
            for (int i = 0; i < items.size(); i++) {
                ItemSlot slot = items.get(i);
                if (slot.role() == null) throw new IllegalArgumentException("Missing item role for " + id);
                widgets.add(new Widget("item " + i, slot.x() - 1, slot.y() - 1, SLOT_SIZE, SLOT_SIZE));
            }
            for (int i = 0; i < fluids.size(); i++) {
                FluidSlot slot = fluids.get(i);
                if (slot.role() == null) throw new IllegalArgumentException("Missing fluid role for " + id);
                widgets.add(new Widget("fluid " + i, slot.x(), slot.y(), TANK_WIDTH, TANK_HEIGHT));
            }
            for (int i = 0; i < toggles.size(); i++) {
                Toggle toggle = toggles.get(i);
                widgets.add(new Widget("toggle " + i, toggle.x(), toggle.y(), SLOT_SIZE, SLOT_SIZE));
            }
            int limitY = height - PLAYER_INVENTORY_GAP;
            for (Widget widget : widgets) {
                if (widget.x() < 0 || widget.y() < 0 || widget.x() + widget.width() > width
                        || widget.y() + widget.height() > limitY)
                    throw new IllegalArgumentException("Behavior machine '" + id + "' places " + widget.name()
                            + " outside the gui");
            }
            for (int i = 0; i < widgets.size(); i++) {
                for (int j = i + 1; j < widgets.size(); j++) {
                    if (widgets.get(i).overlaps(widgets.get(j)))
                        throw new IllegalArgumentException("Behavior machine '" + id + "' overlaps "
                                + widgets.get(i).name() + " and " + widgets.get(j).name());
                }
            }
        }
    }

    /** External automation channels. A null IO declaration leaves every item and fluid face open. */
    public record Automation(@Nullable MachineIo io, @Nullable MachineFace gasValve, boolean signalOutput) {}

    /** World interaction radius, readable recipe maps and synchronized client state keys. */
    public record RuntimeOptions(int maxRange, List<String> recipeMaps, List<String> clientState) {
        public RuntimeOptions {
            recipeMaps = snapshot(recipeMaps);
            clientState = snapshot(clientState);
        }

        private void validateRangeAndRecipeMaps(String id) {
            if (maxRange < 0 || maxRange > BehaviorRangeRules.LIMIT)
                throw new IllegalArgumentException("Behavior machine '" + id + "' range must be 0.."
                        + BehaviorRangeRules.LIMIT);
            var seenMaps = new HashSet<String>();
            for (String name : recipeMaps) {
                if (!AddonNames.matches(AddonNames.RECIPE_MAP, name))
                    throw new IllegalArgumentException("Invalid recipe map for " + id + ": " + name);
                if (!seenMaps.add(name))
                    throw new IllegalArgumentException("Behavior machine '" + id + "' repeats recipe map '" + name + "'");
            }
        }

        private void validateClientState(String id) {
            var seenClientKeys = new HashSet<String>();
            for (String key : clientState) {
                if (!AddonNames.matches(STATE_KEY, key))
                    throw new IllegalArgumentException("Invalid client state key for " + id + ": " + key);
                if (!seenClientKeys.add(key))
                    throw new IllegalArgumentException("Behavior machine '" + id + "' repeats client state '" + key + "'");
            }
            if (clientState.size() > MAX_CLIENT_STATE_KEYS)
                throw new IllegalArgumentException("Behavior machine '" + id + "' has more than "
                        + MAX_CLIENT_STATE_KEYS + " client state keys");
        }
    }

    /** Keep invalid lists and entries intact until validation can report the machine ID. */
    private static <T> List<T> snapshot(List<T> values) {
        return values == null ? null : Collections.unmodifiableList(new ArrayList<>(values));
    }

    public static Builder builder(MachineCasing casing) { return new Builder(casing); }

    public static final class Builder {
        private final MachineCasing casing;
        private EnergyInput energy;
        private EnergyInput auxiliary;
        private EnergyOutput output;
        private MachineIo io;
        private String blockEntityId = "";
        private int guiWidth = MIN_GUI_WIDTH;
        private int guiHeight = MIN_GUI_HEIGHT;
        private String gui;
        private int energyX = DEFAULT_ENERGY_X;
        private int energyY = DEFAULT_ENERGY_Y;
        private final List<ItemSlot> items = new ArrayList<>();
        private final List<FluidSlot> fluids = new ArrayList<>();
        private final List<Toggle> toggles = new ArrayList<>();
        private Progress progress = new Progress(DEFAULT_PROGRESS_X, DEFAULT_PROGRESS_Y);
        private int maxRange;
        private final List<String> recipeMaps = new ArrayList<>();
        private MachineFace gasValve;
        private boolean signalOutput;
        private final List<String> clientState = new ArrayList<>();

        private Builder(MachineCasing casing) { this.casing = casing; }

        public Builder energy(@Nullable EnergyInput value) { energy = value; return this; }

        /** Second input. Its buffer is separate from {@link #energy}. The same face needs a different energy type. */
        public Builder auxiliaryEnergy(EnergyInput value) {
            if (value == null) throw new IllegalArgumentException("Missing auxiliary energy");
            auxiliary = value;
            return this;
        }

        public Builder energyOutput(@Nullable EnergyOutput value) { output = value; return this; }
        public Builder io(@Nullable MachineIo value) { io = value; return this; }
        public Builder blockEntityId(String value) {
            if (value == null) throw new IllegalArgumentException("Missing block entity id; pass an empty string");
            blockEntityId = value;
            return this;
        }
        public Builder gui(String texture) { gui = texture; return this; }
        public Builder guiSize(int width, int height) { guiWidth = width; guiHeight = height; return this; }
        public Builder energyGauge(int x, int y) { energyX = x; energyY = y; return this; }
        public Builder item(int x, int y, SlotRole role) { items.add(new ItemSlot(x, y, role)); return this; }
        public Builder fluid(int capacity, int x, int y, SlotRole role) {
            fluids.add(new FluidSlot(capacity, x, y, role));
            return this;
        }
        public Builder toggle(String translationKey, int x, int y, boolean initiallyOn) {
            toggles.add(new Toggle(translationKey, x, y, initiallyOn));
            return this;
        }
        public Builder progress(int x, int y) { progress = new Progress(x, y); return this; }
        public Builder maxRange(int value) { maxRange = value; return this; }
        public Builder recipeMaps(String... names) {
            if (names != null) for (String name : names) recipeMaps.add(name);
            return this;
        }

        /** One face accepts compressed gas from {@code pushCompressedGas} and can supply it. Pipes stay closed to that path. */
        public Builder gasValve(MachineFace face) {
            if (face == null) throw new IllegalArgumentException("Missing compressed gas valve");
            gasValve = face;
            return this;
        }

        /** Lets an adjacent signal wire read {@code MachineBehavior.gtSignal}. Default false. */
        public Builder signalOutput(boolean value) {
            signalOutput = value;
            return this;
        }

        /**
         * Behavior state keys to sync to the client. A custom screen or renderer reads them through
         * {@code BehaviorMachineClientView}. Keys match {@code [a-z0-9_.]{1,64}}. At most 32 keys.
         */
        public Builder clientState(String... keys) {
            if (keys != null) for (String key : keys) clientState.add(key);
            return this;
        }

        public BehaviorMachine build() {
            var ports = new EnergyPorts(energy, auxiliary, output);
            var layout = new GuiLayout(guiWidth, guiHeight, gui, energyX, energyY, items, fluids, toggles, progress);
            var automation = new Automation(io, gasValve, signalOutput);
            var options = new RuntimeOptions(maxRange, recipeMaps, clientState);
            return new BehaviorMachine(casing, ports, blockEntityId, layout, automation, options);
        }
    }

    private static EnergyInput mainInput(String id, EnergyInput energy) {
        if (energy.perTick() < 1 || energy.perTick() > AddonLimits.MAX_PER_TICK)
            throw new IllegalArgumentException("Invalid energy per tick for " + id + ": " + energy.perTick());
        if (energy.anyPacket()) {
            if (!energy.capacitySpecified() || energy.capacity() < 1)
                throw new IllegalArgumentException("Behavior machine '" + id + "' energy capacity must be positive");
        } else if (energy.rangeSpecified()
                && (energy.perTick() < energy.rangeMin() || energy.perTick() > energy.rangeMax())) {
            throw new IllegalArgumentException("Behavior machine '" + id
                    + "' energy per tick must sit inside the packet range");
        }
        var faces = energy.facesSpecified() ? energy.faces() : MachineFace.exceptFront();
        if (faces.isEmpty())
            throw new IllegalArgumentException("Behavior machine '" + id + "' accepts energy on no face");
        return energy.withFaces(faces);
    }

    private record Widget(String name, int x, int y, int width, int height) {
        boolean overlaps(Widget other) {
            return x < other.x + other.width && other.x < x + width
                    && y < other.y + other.height && other.y < y + height;
        }
    }
}
