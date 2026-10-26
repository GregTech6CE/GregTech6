/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.machine;




import gregtech.api.addon.*;

import gregtech.api.energy.*;

import gregtech.api.machine.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * One addon behavior machine. GregTech builds the block, inventory, tanks, menu and screen.
 * The machine type replaces {@code /} in the id with {@code _}. Tier is 1 to 255.
 * Energy input defaults to every face except the front. Capacity 0 keeps the rated-input drain.
 * The main input may declare a packet range or {@code anyPacket()}, and it may turn overload off.
 * It may be omitted when the machine emits. {@code auxiliaryEnergy} is a second buffer the
 * behavior reads by type. It may share a face with the main input when the energy types differ.
 * {@code io} limits item and fluid faces. Leaving it unset keeps every face open.
 */
public final class BehaviorMachine implements AddonMachine {
    /** Every relative face. An unset {@code io} uses this mask. */
    private static final int EVERY_FACE = (1 << 6) - 1;
    private static final Pattern BLOCK_ENTITY_PATH = Pattern.compile("[a-z0-9_./-]{1,64}");
    private static final Pattern STATE_KEY = Pattern.compile("[a-z0-9_.]{1,64}");
    private static final Pattern TRANSLATION_KEY = Pattern.compile("[a-z0-9_.]+");
    public static final int SLOT = AddonLimits.live(18);
    public static final int TANK_WIDTH = AddonLimits.live(18);
    public static final int TANK_HEIGHT = AddonLimits.live(54);
    public static final int ENERGY_WIDTH = AddonLimits.live(10);
    public static final int ENERGY_HEIGHT = AddonLimits.live(54);
    public static final int PROGRESS_WIDTH = AddonLimits.live(24);
    public static final int PROGRESS_HEIGHT = AddonLimits.live(17);
    /** Player inventory starts this far above the bottom of the gui height. */
    public static final int PLAYER_INVENTORY_GAP = AddonLimits.live(82);
    public static final int UV_SLOT_U = AddonLimits.live(176);
    public static final int UV_SLOT_V = AddonLimits.live(0);
    public static final int UV_ENERGY_U = AddonLimits.live(194);
    public static final int UV_ENERGY_V = AddonLimits.live(0);
    public static final int UV_ENERGY_FULL_U = AddonLimits.live(204);
    public static final int UV_TANK_U = AddonLimits.live(214);
    public static final int UV_TANK_V = AddonLimits.live(0);
    public static final int UV_TANK_MARKS_U = AddonLimits.live(232);
    public static final int UV_ARROW_U = AddonLimits.live(176);
    public static final int UV_ARROW_V = AddonLimits.live(54);
    public static final int UV_ARROW_FULL_V = AddonLimits.live(71);
    public static final int UV_TOGGLE_OFF_U = AddonLimits.live(176);
    public static final int UV_TOGGLE_ON_U = AddonLimits.live(194);
    public static final int UV_TOGGLE_V = AddonLimits.live(88);

    private final MachineCasing casing;
    private final EnergyInput energy;
    private final EnergyInput auxiliary;
    private final EnergyOutput output;
    private final String blockEntityId;
    private final int guiWidth;
    private final int guiHeight;
    private final String gui;
    private final int energyX;
    private final int energyY;
    private final List<ItemSlot> items;
    private final List<FluidSlot> fluids;
    private final List<Toggle> toggles;
    private final Progress progress;
    private final int maxRange;
    private final List<String> recipeMaps;
    private final MachineIo io;
    private final MachineFace gasValve;
    private final boolean signalOutput;
    private final List<String> clientState;

    private BehaviorMachine(MachineCasing casing, EnergyInput energy, EnergyInput auxiliary, EnergyOutput output,
                            String blockEntityId, int guiWidth, int guiHeight, String gui, int energyX, int energyY,
                            List<ItemSlot> items, List<FluidSlot> fluids, List<Toggle> toggles, Progress progress,
                            int maxRange, List<String> recipeMaps, MachineIo io, MachineFace gasValve,
                            boolean signalOutput, List<String> clientState) {
        this.casing = casing;
        this.energy = energy;
        this.auxiliary = auxiliary;
        this.output = output;
        this.blockEntityId = blockEntityId;
        this.guiWidth = guiWidth;
        this.guiHeight = guiHeight;
        this.gui = gui;
        this.energyX = energyX;
        this.energyY = energyY;
        this.items = items;
        this.fluids = fluids;
        this.toggles = toggles;
        this.progress = progress;
        this.maxRange = maxRange;
        this.recipeMaps = recipeMaps;
        this.io = io;
        this.gasValve = gasValve;
        this.signalOutput = signalOutput;
        this.clientState = clientState;
    }

    @Override public MachineCasing casing() { return casing; }
    /** Null when the machine declares no main input. */
    public EnergyInput energy() { return energy; }
    /** Null when the machine has no second energy input. */
    public EnergyInput auxiliary() { return auxiliary; }
    /** Null when the machine does not emit energy. */
    public EnergyOutput output() { return output; }
    public String blockEntityId() { return blockEntityId; }
    public int guiWidth() { return guiWidth; }
    public int guiHeight() { return guiHeight; }
    public String gui() { return gui; }
    public int energyX() { return energyX; }
    public int energyY() { return energyY; }
    public List<ItemSlot> items() { return List.copyOf(items); }
    public List<FluidSlot> fluids() { return List.copyOf(fluids); }
    public List<Toggle> toggles() { return List.copyOf(toggles); }
    public Progress progress() { return progress; }
    public int maxRange() { return maxRange; }
    public List<String> recipeMaps() { return List.copyOf(recipeMaps); }
    /** Null when every item and fluid face stays open and no automatic face was declared. */
    public MachineIo io() { return io; }

    /**
     * The face that accepts and supplies compressed gas, relative to the horizontal facing.
     * Null when this machine is not a compressed-gas valve. Pipes still use {@link #io()}.
     */
    public MachineFace gasValve() { return gasValve; }

    /** True when this machine may be read as a GT signal source by an adjacent signal wire. */
    public boolean signalOutput() { return signalOutput; }

    /** Behavior state keys GregTech syncs to the client for a custom screen or renderer. */
    public List<String> clientState() { return List.copyOf(clientState); }

    /** Registry path. An empty block entity id becomes {@code be_<id>}. */
    public String blockEntityPath() {
        return blockEntityId.isEmpty() ? "be_" + casing.id().replace('/', '_') : blockEntityId;
    }

    public int playerInventoryY() { return guiHeight - PLAYER_INVENTORY_GAP; }

    public int energyMask() { return energy == null ? 0 : MachineFace.mask(energy.faces()); }

    public int auxiliaryMask() { return auxiliary == null ? 0 : MachineFace.mask(auxiliary.faces()); }

    public boolean emitsEnergy() { return output != null; }

    public int energyOutMask() { return output == null ? 0 : MachineFace.mask(output.faces()); }

    /** Item-insert faces. An unset {@code io} is every face. A named {@code io} leaves an unnamed channel closed. */
    public int itemInputMask() { return faceMask(io == null ? null : io.itemInputs()); }
    /** Item-extract faces. Same default as {@link #itemInputMask()}. */
    public int itemOutputMask() { return faceMask(io == null ? null : io.itemOutputs()); }
    /** Fluid-fill faces. Same default as {@link #itemInputMask()}. */
    public int fluidInputMask() { return faceMask(io == null ? null : io.fluidInputs()); }
    /** Fluid-drain faces. Same default as {@link #itemInputMask()}. */
    public int fluidOutputMask() { return faceMask(io == null ? null : io.fluidOutputs()); }

    private static int faceMask(List<MachineFace> faces) {
        return faces == null ? EVERY_FACE : MachineFace.mask(faces);
    }

    public int initialToggles() {
        int bits = 0;
        for (int i = 0; i < toggles.size(); i++) if (toggles.get(i).initiallyOn()) bits |= 1 << i;
        return bits;
    }

    public enum SlotRole { INPUT, OUTPUT, ANY }

    public record ItemSlot(int x, int y, SlotRole role) {}
    public record FluidSlot(int capacity, int x, int y, SlotRole role) {}
    public record Toggle(String translationKey, int x, int y, boolean initiallyOn) {}
    public record Progress(int x, int y) {}

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BehaviorMachine that)) return false;
        return guiWidth == that.guiWidth && guiHeight == that.guiHeight && energyX == that.energyX
                && energyY == that.energyY && maxRange == that.maxRange && casing.equals(that.casing)
                && Objects.equals(energy, that.energy) && Objects.equals(auxiliary, that.auxiliary)
                && Objects.equals(output, that.output)
                && blockEntityId.equals(that.blockEntityId) && gui.equals(that.gui)
                && items.equals(that.items) && fluids.equals(that.fluids) && toggles.equals(that.toggles)
                && progress.equals(that.progress) && recipeMaps.equals(that.recipeMaps)
                && Objects.equals(io, that.io) && gasValve == that.gasValve
                && signalOutput == that.signalOutput && clientState.equals(that.clientState);
    }

    @Override public String toString() {
        return "BehaviorMachine[casing=" + casing + ", energy=" + energy + ", auxiliary=" + auxiliary
                + ", output=" + output + ", gui=" + gui + ", maxRange=" + maxRange
                + ", recipeMaps=" + recipeMaps + "]";
    }

    @Override public int hashCode() {
        return Objects.hash(casing, energy, auxiliary, output, blockEntityId, guiWidth, guiHeight, gui, energyX, energyY,
                items, fluids, toggles, progress, maxRange, recipeMaps, io, gasValve, signalOutput, clientState);
    }

    public static Builder builder(MachineCasing casing) { return new Builder(casing); }

    public static final class Builder {
        private final MachineCasing casing;
        private EnergyInput energy;
        private EnergyInput auxiliary;
        private EnergyOutput output;
        private MachineIo io;
        private String blockEntityId = "";
        private int guiWidth = 176;
        private int guiHeight = 166;
        private String gui;
        private int energyX = 8;
        private int energyY = 8;
        private final List<ItemSlot> items = new ArrayList<>();
        private final List<FluidSlot> fluids = new ArrayList<>();
        private final List<Toggle> toggles = new ArrayList<>();
        private Progress progress = new Progress(80, 35);
        private int maxRange;
        private final List<String> recipeMaps = new ArrayList<>();
        private MachineFace gasValve;
        private boolean signalOutput;
        private final List<String> clientState = new ArrayList<>();

        private Builder(MachineCasing casing) { this.casing = casing; }

        public Builder energy(EnergyInput value) { energy = value; return this; }

        /** Second input. Its buffer is separate from {@link #energy}. The same face needs a different energy type. */
        public Builder auxiliaryEnergy(EnergyInput value) {
            if (value == null) throw new IllegalArgumentException("Missing auxiliary energy");
            auxiliary = value;
            return this;
        }

        public Builder energyOutput(EnergyOutput value) { output = value; return this; }
        public Builder output(FuelOutput value) {
            if (value != null) throw new IllegalArgumentException("Behavior machines cannot declare fuel output");
            return this;
        }
        public Builder io(MachineIo value) { io = value; return this; }
        public Builder blockEntityId(String value) { blockEntityId = value; return this; }
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
            if (casing == null) throw new IllegalArgumentException("Missing casing");
            if (io != null) {
                if (io.hasTarget())
                    throw new IllegalArgumentException("Behavior machines cannot declare an IO target");
                if (io.itemAutoInput() != null || io.itemAutoOutput() != null
                        || io.fluidAutoInput() != null || io.fluidAutoOutput() != null)
                    throw new IllegalArgumentException("Behavior machines cannot declare an automatic face");
            }
            String id = casing.id();
            String machineType = casing.machineType() == null ? id.replace('/', '_') : casing.machineType();
            if (!AddonNames.matches(AddonNames.MACHINE_TYPE, machineType))
                throw new IllegalArgumentException("Invalid machine type for " + id + ": " + machineType);
            int tier = casing.tierSpecified() ? casing.tier() : 1;
            if (tier < 1 || tier > 255)
                throw new IllegalArgumentException("Invalid tier for " + id + ": " + tier);
            if (energy == null && output == null)
                throw new IllegalArgumentException("Missing energy type for " + id);
            var storedEnergy = energy == null ? null : mainInput(id, energy);
            var storedAuxiliary = auxiliaryInput(id, auxiliary, storedEnergy);
            var blockEntity = blockEntityId == null ? "" : blockEntityId;
            if (!blockEntity.isEmpty() && !AddonNames.matches(BLOCK_ENTITY_PATH, blockEntity))
                throw new IllegalArgumentException("Invalid block entity id for " + id + ": " + blockEntity);
            if (guiWidth < 176 || guiWidth > 256 || guiHeight < 166 || guiHeight > 512)
                throw new IllegalArgumentException("Invalid gui size for " + id + ": "
                        + guiWidth + "x" + guiHeight);
            if (!AddonNames.matches(AddonNames.TEXTURE, gui))
                throw new IllegalArgumentException("Invalid gui texture for " + id + ": " + gui);
            Objects.requireNonNull(progress, "progress");
            if (maxRange < 0 || maxRange > BehaviorRangeRules.LIMIT)
                throw new IllegalArgumentException("Behavior machine '" + id + "' range must be 0.."
                        + BehaviorRangeRules.LIMIT);
            var seenMaps = new HashSet<String>();
            var mapNames = new ArrayList<String>();
            for (String name : recipeMaps) {
                if (!AddonNames.matches(AddonNames.RECIPE_MAP, name))
                    throw new IllegalArgumentException("Invalid recipe map for " + id + ": " + name);
                if (!seenMaps.add(name))
                    throw new IllegalArgumentException("Behavior machine '" + id + "' repeats recipe map '" + name + "'");
                mapNames.add(name);
            }
            if (items.size() > 54)
                throw new IllegalArgumentException("Behavior machine '" + id + "' has " + items.size()
                        + " item slots; at most 54");
            if (fluids.size() > 8)
                throw new IllegalArgumentException("Behavior machine '" + id + "' has " + fluids.size()
                        + " fluid slots; at most 8");
            if (toggles.size() > 16)
                throw new IllegalArgumentException("Behavior machine '" + id + "' has " + toggles.size()
                        + " toggles; at most 16");
            var seenClientKeys = new HashSet<String>();
            var clientKeys = new ArrayList<String>();
            for (String key : clientState) {
                if (!AddonNames.matches(STATE_KEY, key))
                    throw new IllegalArgumentException("Invalid client state key for " + id + ": " + key);
                if (!seenClientKeys.add(key))
                    throw new IllegalArgumentException("Behavior machine '" + id + "' repeats client state '" + key + "'");
                clientKeys.add(key);
            }
            if (clientKeys.size() > 32)
                throw new IllegalArgumentException("Behavior machine '" + id + "' has more than 32 client state keys");
            for (FluidSlot fluid : fluids) {
                if (fluid.capacity() < 1 || fluid.capacity() > AddonLimits.MAX_CAPACITY)
                    throw new IllegalArgumentException("Invalid tank capacity for " + id);
            }
            for (Toggle toggle : toggles) {
                if (!AddonNames.matches(TRANSLATION_KEY, toggle.translationKey()))
                    throw new IllegalArgumentException("Invalid toggle key for " + id + ": " + toggle.translationKey());
            }
            var widgets = new ArrayList<Widget>();
            if (storedEnergy != null)
                widgets.add(new Widget("energy", energyX, energyY, ENERGY_WIDTH, ENERGY_HEIGHT));
            widgets.add(new Widget("progress", progress.x(), progress.y(), PROGRESS_WIDTH, PROGRESS_HEIGHT));
            for (int i = 0; i < items.size(); i++) {
                ItemSlot slot = items.get(i);
                Objects.requireNonNull(slot.role(), "item role");
                widgets.add(new Widget("item " + i, slot.x() - 1, slot.y() - 1, SLOT, SLOT));
            }
            for (int i = 0; i < fluids.size(); i++) {
                FluidSlot slot = fluids.get(i);
                Objects.requireNonNull(slot.role(), "fluid role");
                widgets.add(new Widget("fluid " + i, slot.x(), slot.y(), TANK_WIDTH, TANK_HEIGHT));
            }
            for (int i = 0; i < toggles.size(); i++) {
                Toggle toggle = toggles.get(i);
                widgets.add(new Widget("toggle " + i, toggle.x(), toggle.y(), SLOT, SLOT));
            }
            int limitY = guiHeight - PLAYER_INVENTORY_GAP;
            for (Widget widget : widgets) {
                if (widget.x() < 0 || widget.y() < 0 || widget.x() + widget.width() > guiWidth
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
            return new BehaviorMachine(casing.resolve(machineType, true, tier), storedEnergy, storedAuxiliary, output,
                    blockEntity,
                    guiWidth, guiHeight, gui, energyX, energyY, List.copyOf(items), List.copyOf(fluids),
                    List.copyOf(toggles), progress, maxRange, List.copyOf(mapNames), io, gasValve, signalOutput,
                    List.copyOf(clientKeys));
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
        return energy.withFaces(faces).collapseDefaultCapacity();
    }

    private static EnergyInput auxiliaryInput(String id, EnergyInput auxiliary, EnergyInput main) {
        if (auxiliary == null) return null;
        if (auxiliary.dynamic())
            throw new IllegalArgumentException("Behavior machine '" + id + "' auxiliary energy cannot pick its type");
        if (!auxiliary.anyPacket() && !auxiliary.rangeSpecified())
            throw new IllegalArgumentException("Behavior machine '" + id
                    + "' auxiliary energy needs a packet range or anyPacket");
        if (auxiliary.perTick() < 1 || auxiliary.perTick() > AddonLimits.MAX_PER_TICK)
            throw new IllegalArgumentException("Invalid auxiliary energy per tick for " + id + ": " + auxiliary.perTick());
        if (!auxiliary.capacitySpecified() || auxiliary.capacity() < 1)
            throw new IllegalArgumentException("Behavior machine '" + id + "' auxiliary energy capacity must be positive");
        var auxFaces = auxiliary.facesSpecified() ? auxiliary.faces() : List.<MachineFace>of();
        if (!auxiliary.facesSpecified() || auxFaces.isEmpty())
            throw new IllegalArgumentException("Behavior machine '" + id + "' auxiliary energy needs a face");
        boolean sameType = main != null && EnergyType.same(auxiliary.type(), main.type());
        var mainFaces = main == null ? List.<MachineFace>of() : main.faces();
        for (MachineFace face : auxFaces) {
            if (sameType && mainFaces.contains(face))
                throw new IllegalArgumentException("Behavior machine '" + id
                        + "' auxiliary energy overlaps an input face");
        }
        if (auxiliary.rangeSpecified()
                && (auxiliary.perTick() < auxiliary.rangeMin() || auxiliary.perTick() > auxiliary.rangeMax()))
            throw new IllegalArgumentException("Behavior machine '" + id
                    + "' auxiliary energy per tick must sit inside the packet range");
        return auxiliary.withFaces(auxFaces);
    }

    private record Widget(String name, int x, int y, int width, int height) {
        boolean overlaps(Widget other) {
            return x < other.x + other.width && other.x < x + width
                    && y < other.y + other.height && other.y < y + height;
        }
    }
}
