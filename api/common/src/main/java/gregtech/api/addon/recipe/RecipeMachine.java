/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;

import gregtech.api.addon.AddonLimits;
import gregtech.api.addon.AddonNames;
import gregtech.api.addon.AuxiliaryEnergy;
import gregtech.api.addon.machine.AddonMachine;
import gregtech.api.energy.EnergyInput;
import gregtech.api.machine.MachineCasing;
import gregtech.api.machine.MachineFace;
import gregtech.api.machine.MachineIo;
import java.util.HashSet;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One addon recipe machine. Defaults: basic kind, parallel 1,
 * efficiency 10000, constant energy on, cheap overclock unset and off, no light gate.
 * The machine type is the id unchanged; call {@code machineType(...)} when the id is not a valid
 * machine type (for example it contains a slash).
 *
 * @param casing the resolved block casing
 * @param energy the main energy input, with the six-side default already applied
 * @param auxiliary the second input, or null when the machine has none
 * @param startupEnergy true when a recipe's special value is charged from the auxiliary buffer
 * @param io item and fluid faces, or null when they stay at the six-side default
 * @param largeRecipe true for a large recipe machine
 * @param recipeMap the recipe map name
 * @param parallel recipes processed in parallel
 * @param efficiency processing efficiency in parts per 10,000
 * @param constantEnergy true when every parallel runs at the same energy
 * @param cheapOverclocking true when the machine uses cheap overclocking
 * @param cheapOverclockingSpecified true when {@link #cheapOverclocking()} was set explicitly
 * @param minimumLightAbove lowest light level the machine needs, or 0
 * @param searchOutputTanks true when the machine searches output tanks for room
 * @param exclusiveOutputs true when an output slot may hold only one recipe's result
 * @param structureKeys structure keys the multiblock controller binds
 */
public record RecipeMachine(MachineCasing casing, EnergyInput energy, @Nullable EnergyInput auxiliary,
                            boolean startupEnergy, @Nullable MachineIo io, boolean largeRecipe, String recipeMap,
                            int parallel, int efficiency, boolean constantEnergy, boolean cheapOverclocking,
                            boolean cheapOverclockingSpecified, int minimumLightAbove, boolean searchOutputTanks,
                            boolean exclusiveOutputs, List<String> structureKeys) implements AddonMachine {
    public RecipeMachine {
        if (casing == null) throw new IllegalArgumentException("Missing casing");
        String id = casing.id();
        String machineType = casing.machineType() == null ? id : casing.machineType();
        if (!AddonNames.matches(AddonNames.MACHINE_TYPE, machineType))
            throw new IllegalArgumentException("Invalid machine type for " + id + ": " + machineType
                    + (casing.machineType() == null
                            ? "; call machineType(...) when the id is not a valid machine type" : ""));
        int tier = casing.tierSpecified() ? casing.tier() : 1;
        if (tier < 1 || tier > AddonLimits.MAX_TIER)
            throw new IllegalArgumentException("Invalid tier for " + id + ": " + tier);
        if (energy == null) throw new IllegalArgumentException("Missing energy type for " + id);
        if (energy.dynamic())
            throw new IllegalArgumentException("Recipe machine '" + id + "' cannot pick its energy type");
        if (energy.capacitySpecified())
            throw new IllegalArgumentException("Recipe machine '" + id + "' cannot declare energy capacity");
        if (energy.anyPacket())
            throw new IllegalArgumentException("Recipe machine '" + id + "' cannot ignore packet size");
        if (!energy.overload())
            throw new IllegalArgumentException("Recipe machine '" + id + "' cannot disable overload");
        if (energy.perTick() < 1 || energy.perTick() > AddonLimits.MAX_PER_TICK)
            throw new IllegalArgumentException("Invalid energy per tick for " + id + ": " + energy.perTick());
        if (energy.rangeSpecified()
                && (energy.perTick() < energy.rangeMin() || energy.perTick() > energy.rangeMax()))
            throw new IllegalArgumentException("Recipe machine '" + id
                    + "' energy per tick must sit inside the packet range");
        if (energy.rangeSpecified() && energy.type().time())
            throw new IllegalArgumentException("TU machines cannot declare an energy range: " + id);
        var mainFaces = energy.facesSpecified() ? energy.faces() : MachineFace.all();
        var storedEnergy = energy.withFaces(mainFaces);
        var storedAuxiliary = AuxiliaryEnergy.validate("Recipe", id, storedEnergy, auxiliary);
        if (storedAuxiliary == null && startupEnergy)
            throw new IllegalArgumentException("Recipe machine '" + id + "' startup energy needs auxiliary energy");
        if (!AddonNames.matches(AddonNames.RECIPE_MAP, recipeMap))
            throw new IllegalArgumentException("Invalid recipe map name: " + recipeMap);
        if (parallel < 1 || parallel > AddonLimits.MAX_PARALLEL)
            throw new IllegalArgumentException("Invalid parallel for " + id + ": " + parallel);
        if (efficiency < 1 || efficiency > AddonLimits.MAX_EFFICIENCY)
            throw new IllegalArgumentException("Invalid efficiency for " + id + ": " + efficiency);
        if (minimumLightAbove < 0 || minimumLightAbove > AddonLimits.MAX_LIGHT_LEVEL)
            throw new IllegalArgumentException("Invalid light requirement for " + id + ": " + minimumLightAbove);
        if (structureKeys == null)
            throw new IllegalArgumentException(
                    "Structure keys must not be null for " + id + "; pass an empty list");
        var keys = List.copyOf(structureKeys);
        var seenKeys = new HashSet<String>();
        for (String key : keys) {
            if (!AddonNames.matches(AddonNames.RECIPE_MAP, key))
                throw new IllegalArgumentException("Invalid structure key for " + id + ": " + key);
            if (!seenKeys.add(key))
                throw new IllegalArgumentException("Recipe machine '" + id + "' repeats structure key '" + key + "'");
        }
        if (io != null && io.hasTarget() && !largeRecipe)
            throw new IllegalArgumentException("IO targets require a large recipe machine: " + id);
        casing = casing.resolve(machineType, true, tier);
        energy = storedEnergy;
        auxiliary = storedAuxiliary;
        structureKeys = keys;
    }

    public boolean basic() { return !largeRecipe; }

    public static Builder builder(MachineCasing casing) { return new Builder(casing); }

    public static final class Builder {
        private final MachineCasing casing;
        private EnergyInput energy;
        private EnergyInput auxiliary;
        private boolean startupEnergy;
        private MachineIo io;
        private boolean largeRecipe;
        private String recipeMap;
        private int parallel = 1;
        private int efficiency = AddonLimits.DEFAULT_EFFICIENCY;
        private boolean constantEnergy = true;
        private boolean cheapOverclocking;
        private boolean cheapOverclockingSpecified;
        private int minimumLightAbove;
        private boolean searchOutputTanks;
        private boolean exclusiveOutputs;
        private List<String> structureKeys = List.of();

        private Builder(MachineCasing casing) { this.casing = casing; }

        public Builder energy(EnergyInput value) {
            if (value == null) throw new IllegalArgumentException("Missing energy");
            energy = value;
            return this;
        }

        /** Second input buffer. It needs faces, a positive capacity and a range or {@code anyPacket}. */
        public Builder auxiliaryEnergy(EnergyInput value) {
            if (value == null) throw new IllegalArgumentException("Missing auxiliary energy");
            auxiliary = value;
            return this;
        }

        /** Charge each recipe's {@code special} from the auxiliary buffer before progress starts. */
        public Builder startupEnergy(boolean value) { startupEnergy = value; return this; }

        public Builder io(@Nullable MachineIo value) { io = value; return this; }
        public Builder basic() { largeRecipe = false; return this; }
        public Builder largeRecipe() { largeRecipe = true; return this; }
        public Builder recipeMap(String value) { recipeMap = value; return this; }
        public Builder parallel(int value) { parallel = value; return this; }
        public Builder efficiency(int value) { efficiency = value; return this; }
        public Builder constantEnergy(boolean value) { constantEnergy = value; return this; }
        public Builder cheapOverclocking(boolean value) {
            cheapOverclocking = value;
            cheapOverclockingSpecified = true;
            return this;
        }
        public Builder minimumLightAbove(int value) { minimumLightAbove = value; return this; }
        public Builder searchOutputTanks(boolean value) { searchOutputTanks = value; return this; }
        public Builder exclusiveOutputs(boolean value) { exclusiveOutputs = value; return this; }
        public Builder structureKeys(List<String> value) {
            if (value == null)
                throw new IllegalArgumentException("Structure keys must not be null; pass an empty list");
            structureKeys = value;
            return this;
        }

        public RecipeMachine build() {
            return new RecipeMachine(casing, energy, auxiliary, startupEnergy, io, largeRecipe, recipeMap,
                    parallel, efficiency, constantEnergy, cheapOverclocking, cheapOverclockingSpecified,
                    minimumLightAbove, searchOutputTanks, exclusiveOutputs, structureKeys);
        }
    }
}
