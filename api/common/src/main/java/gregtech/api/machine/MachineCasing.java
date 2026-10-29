/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

import gregtech.api.addon.AddonNames;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Block id, material, and the optional machine type and tier shared by the three machine templates.
 * A missing machine type or tier is filled in by the template, and the rules are not the same.
 */
public final class MachineCasing {
    private final String id;
    private final String material;
    private final String machineType;
    private final boolean tierSpecified;
    private final int tier;

    private MachineCasing(String id, String material, String machineType, boolean tierSpecified, int tier) {
        this.id = id;
        this.material = material;
        this.machineType = machineType;
        this.tierSpecified = tierSpecified;
        this.tier = tier;
    }

    public String id() { return id; }

    public String material() { return material; }

    /** Null when the builder did not set one. A built machine has resolved this. */
    public @Nullable String machineType() { return machineType; }

    public boolean tierSpecified() { return tierSpecified; }

    /** Meaningful when {@link #tierSpecified()} is true. Fuel generators reject a specified tier. */
    public int tier() { return tier; }

    public MachineCasing resolve(String machineType, boolean tierSpecified, int tier) {
        requireMachineType(id, machineType);
        return new MachineCasing(id, material, machineType, tierSpecified, tier);
    }

    private static void requireMachineType(String id, String machineType) {
        if (machineType != null && !AddonNames.matches(AddonNames.MACHINE_TYPE, machineType))
            throw new IllegalArgumentException("Invalid machine type for " + id + ": " + machineType);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MachineCasing that)) return false;
        return tierSpecified == that.tierSpecified && tier == that.tier
                && id.equals(that.id) && material.equals(that.material)
                && Objects.equals(machineType, that.machineType);
    }

    @Override public String toString() {
        return "MachineCasing[id=" + id + ", material=" + material + ", machineType=" + machineType
                + ", tier=" + (tierSpecified ? tier : "unset") + "]";
    }

    @Override public int hashCode() {
        return Objects.hash(id, material, machineType, tierSpecified, tier);
    }

    public static Builder builder(String id, String material) { return new Builder(id, material); }

    public static final class Builder {
        private final String id;
        private final String material;
        private String machineType;
        private boolean tierSpecified;
        private int tier;

        private Builder(String id, String material) {
            this.id = id;
            this.material = material;
        }

        public Builder machineType(String value) { machineType = value; return this; }

        public Builder tier(int value) { tierSpecified = true; tier = value; return this; }

        public MachineCasing build() {
            if (!AddonNames.matches(AddonNames.RESOURCE_PATH, id))
                throw new IllegalArgumentException("Invalid machine ID: " + id);
            if (material == null || material.isBlank())
                throw new IllegalArgumentException("Missing casing material for " + id);
            requireMachineType(id, machineType);
            return new MachineCasing(id, material, machineType, tierSpecified, tier);
        }
    }
}
