package com.gregtech.gregtech.api.addon;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** SPDX-License-Identifier: LGPL-3.0-or-later */

/**
 * One addon behavior machine. GregTech builds the block, inventory, tanks, menu and screen.
 * The addon supplies this layout and a {@code BehaviorMachineContributor} for the machine type.
 * Widget positions are pixels on a GUI whose background is {@code gui}. The sheet must place
 * the slot, energy, tank, progress and toggle sprites at the UV constants below; that is the
 * sheet gt6-crops already uses.
 */
public record BehaviorMachineDeclaration(
        String id,
        String material,
        String machineType,
        String energy,
        long energyPerTick,
        int tier,
        String blockEntityId,
        int guiWidth,
        int guiHeight,
        String gui,
        int energyX,
        int energyY,
        List<Face> energyFaces,
        List<ItemSlot> items,
        List<FluidSlot> fluids,
        List<Toggle> toggles,
        Progress progress
) {
    public static final int SLOT = 18;
    public static final int TANK_WIDTH = 18;
    public static final int TANK_HEIGHT = 54;
    public static final int ENERGY_WIDTH = 10;
    public static final int ENERGY_HEIGHT = 54;
    public static final int PROGRESS_WIDTH = 24;
    public static final int PROGRESS_HEIGHT = 17;
    /** Player inventory starts this far above the bottom of {@code guiHeight}. */
    public static final int PLAYER_INVENTORY_GAP = 82;
    public static final int UV_SLOT_U = 176;
    public static final int UV_SLOT_V = 0;
    public static final int UV_ENERGY_U = 194;
    public static final int UV_ENERGY_V = 0;
    public static final int UV_ENERGY_FULL_U = 204;
    public static final int UV_TANK_U = 214;
    public static final int UV_TANK_V = 0;
    public static final int UV_TANK_MARKS_U = 232;
    public static final int UV_ARROW_U = 176;
    public static final int UV_ARROW_V = 54;
    public static final int UV_ARROW_FULL_V = 71;
    public static final int UV_TOGGLE_OFF_U = 176;
    public static final int UV_TOGGLE_ON_U = 194;
    public static final int UV_TOGGLE_V = 88;

    public BehaviorMachineDeclaration {
        if (id == null || !id.matches("[a-z0-9/._-]+"))
            throw new IllegalArgumentException("Invalid machine ID: " + id);
        if (material == null || material.isBlank())
            throw new IllegalArgumentException("Missing casing material for " + id);
        if (machineType == null || !machineType.matches("[a-z0-9_]+"))
            throw new IllegalArgumentException("Invalid machine type for " + id + ": " + machineType);
        if (energy == null || energy.isBlank())
            throw new IllegalArgumentException("Missing energy type for " + id);
        if (energyPerTick < 1 || energyPerTick > 1_000_000_000_000L || tier < 1 || tier > 255)
            throw new IllegalArgumentException("Invalid machine parameters for " + id);
        blockEntityId = blockEntityId == null ? "" : blockEntityId;
        if (!blockEntityId.isEmpty() && !blockEntityId.matches("[a-z0-9_./-]{1,64}"))
            throw new IllegalArgumentException("Invalid block entity id for " + id + ": " + blockEntityId);
        if (guiWidth < 176 || guiWidth > 256 || guiHeight < 166 || guiHeight > 512)
            throw new IllegalArgumentException("Invalid gui size for " + id);
        if (gui == null || !gui.matches("[a-z0-9_.-]+:[a-z0-9_./-]+\\.png"))
            throw new IllegalArgumentException("Invalid gui texture for " + id + ": " + gui);
        Objects.requireNonNull(progress, "progress");
        energyFaces = List.copyOf(energyFaces == null ? List.of() : energyFaces);
        items = List.copyOf(items == null ? List.of() : items);
        fluids = List.copyOf(fluids == null ? List.of() : fluids);
        toggles = List.copyOf(toggles == null ? List.of() : toggles);
        if (energyFaces.isEmpty())
            throw new IllegalArgumentException("Behavior machine '" + id + "' accepts energy on no face");
        if (energyFaces.stream().distinct().count() != energyFaces.size())
            throw new IllegalArgumentException("Behavior machine '" + id + "' repeats an energy face");
        if (items.size() > 54 || fluids.size() > 8 || toggles.size() > 16)
            throw new IllegalArgumentException("Behavior machine '" + id + "' has too many slots");
        for (FluidSlot fluid : fluids) {
            if (fluid.capacity() < 1 || fluid.capacity() > 1_024_000_000)
                throw new IllegalArgumentException("Invalid tank capacity for " + id);
        }
        for (Toggle toggle : toggles) {
            if (toggle.translationKey() == null || !toggle.translationKey().matches("[a-z0-9_.]+"))
                throw new IllegalArgumentException("Invalid toggle key for " + id + ": " + toggle.translationKey());
        }
        var widgets = new ArrayList<Widget>();
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
            if (widget.x() < 0 || widget.y() < 0 || widget.x() + widget.width() > guiWidth || widget.y() + widget.height() > limitY)
                throw new IllegalArgumentException("Behavior machine '" + id + "' places " + widget.name() + " outside the gui");
        }
        for (int i = 0; i < widgets.size(); i++) {
            for (int j = i + 1; j < widgets.size(); j++) {
                if (widgets.get(i).overlaps(widgets.get(j)))
                    throw new IllegalArgumentException("Behavior machine '" + id + "' overlaps "
                            + widgets.get(i).name() + " and " + widgets.get(j).name());
            }
        }
    }

    /** Registry path. An empty {@code blockEntityId} becomes {@code be_<id>}. */
    public String blockEntityPath() {
        return blockEntityId.isEmpty() ? "be_" + id.replace('/', '_') : blockEntityId;
    }

    public int playerInventoryY() { return guiHeight - PLAYER_INVENTORY_GAP; }

    public int energyMask() {
        int mask = 0;
        for (Face face : energyFaces) mask |= face.bit();
        return mask;
    }

    public int initialToggles() {
        int bits = 0;
        for (int i = 0; i < toggles.size(); i++) if (toggles.get(i).initiallyOn()) bits |= 1 << i;
        return bits;
    }

    /** Machine-relative face. Bit indexes match GregTech's machine face masks. */
    public enum Face {
        BOTTOM(0), TOP(1), RIGHT(2), FRONT(3), LEFT(4), BACK(5);
        private final int index;
        Face(int index) { this.index = index; }
        public int index() { return index; }
        public int bit() { return 1 << index; }
    }

    public enum SlotRole { INPUT, OUTPUT, ANY }

    public record ItemSlot(int x, int y, SlotRole role) {}
    public record FluidSlot(int capacity, int x, int y, SlotRole role) {}
    public record Toggle(String translationKey, int x, int y, boolean initiallyOn) {}
    public record Progress(int x, int y) {}

    private record Widget(String name, int x, int y, int width, int height) {
        boolean overlaps(Widget other) {
            return x < other.x + other.width && other.x < x + width
                    && y < other.y + other.height && other.y < y + height;
        }
    }

    public static Builder builder(String id, String material) { return new Builder(id, material); }

    public static final class Builder {
        private final String id;
        private final String material;
        private String machineType;
        private String energy;
        private long energyPerTick;
        private int tier = 1;
        private String blockEntityId = "";
        private int guiWidth = 176;
        private int guiHeight = 166;
        private String gui;
        private int energyX = 8;
        private int energyY = 8;
        private List<Face> energyFaces = List.of(Face.BOTTOM, Face.TOP, Face.LEFT, Face.RIGHT, Face.BACK);
        private final List<ItemSlot> items = new ArrayList<>();
        private final List<FluidSlot> fluids = new ArrayList<>();
        private final List<Toggle> toggles = new ArrayList<>();
        private Progress progress = new Progress(80, 35);

        private Builder(String id, String material) {
            this.id = id;
            this.material = material;
            this.machineType = id == null ? "" : id.replace('/', '_');
        }

        public Builder machineType(String value) { machineType = value; return this; }
        public Builder energy(String shortName, long perTick) { energy = shortName; energyPerTick = perTick; return this; }
        public Builder tier(int value) { tier = value; return this; }
        /** Path only. Tiers of one machine type may share it. Empty uses {@code be_<id>}. */
        public Builder blockEntityId(String value) { blockEntityId = value; return this; }
        public Builder gui(String texture) { gui = texture; return this; }
        public Builder guiSize(int width, int height) { guiWidth = width; guiHeight = height; return this; }
        public Builder energyGauge(int x, int y) { energyX = x; energyY = y; return this; }
        public Builder energyFaces(List<Face> faces) { energyFaces = faces; return this; }
        public Builder item(int x, int y, SlotRole role) { items.add(new ItemSlot(x, y, role)); return this; }
        public Builder fluid(int capacity, int x, int y, SlotRole role) { fluids.add(new FluidSlot(capacity, x, y, role)); return this; }
        public Builder toggle(String translationKey, int x, int y, boolean initiallyOn) {
            toggles.add(new Toggle(translationKey, x, y, initiallyOn));
            return this;
        }
        public Builder progress(int x, int y) { progress = new Progress(x, y); return this; }

        public BehaviorMachineDeclaration build() {
            return new BehaviorMachineDeclaration(id, material, machineType, energy, energyPerTick, tier, blockEntityId,
                    guiWidth, guiHeight, gui, energyX, energyY, energyFaces, items, fluids, toggles, progress);
        }
    }
}
