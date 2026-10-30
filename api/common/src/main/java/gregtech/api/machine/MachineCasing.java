/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

import gregtech.api.addon.AddonNames;
import org.jspecify.annotations.Nullable;

/**
 * Block id, material, and the optional machine type and tier shared by the three machine templates.
 * A missing machine type or tier is filled in by the template, and the rules are not the same.
 *
 * @param id the block registry path
 * @param material the casing material
 * @param machineType the declared machine type, or null when the template derives it
 * @param tierSpecified true when {@link #tier()} was set
 * @param tier the declared tier; meaningful when {@link #tierSpecified()} is true
 */
public record MachineCasing(String id, String material, @Nullable String machineType, boolean tierSpecified,
                            int tier) {
    public MachineCasing {
        if (!AddonNames.matches(AddonNames.RESOURCE_PATH, id))
            throw new IllegalArgumentException("Invalid machine ID: " + id);
        if (!AddonNames.matches(AddonNames.MATERIAL, material))
            throw new IllegalArgumentException("Invalid casing material for " + id + ": " + material);
        requireMachineType(id, machineType);
    }

    public MachineCasing resolve(String machineType, boolean tierSpecified, int tier) {
        return new MachineCasing(id, material, machineType, tierSpecified, tier);
    }

    private static void requireMachineType(String id, @Nullable String machineType) {
        if (machineType != null && !AddonNames.matches(AddonNames.MACHINE_TYPE, machineType))
            throw new IllegalArgumentException("Invalid machine type for " + id + ": " + machineType);
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
            return new MachineCasing(id, material, machineType, tierSpecified, tier);
        }
    }
}
