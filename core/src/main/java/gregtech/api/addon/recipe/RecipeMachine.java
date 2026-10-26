/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.recipe;


import gregtech.api.addon.machine.*;





import gregtech.api.addon.*;

import gregtech.api.energy.*;

import gregtech.api.machine.*;

import java.util.List;
import java.util.Objects;

/**
 * One addon recipe machine. Defaults: basic kind, parallel 1,
 * efficiency 10000, constant energy on, cheap overclock unset and off, no light gate.
 * The machine type is the id unchanged. A slash in that id is still invalid.
 */
public final class RecipeMachine implements AddonMachine {
    private final MachineCasing casing;
    private final EnergyInput energy;
    private final EnergyInput auxiliary;
    private final boolean startupEnergy;
    private final MachineIo io;
    private final boolean largeRecipe;
    private final String recipeMap;
    private final int parallel;
    private final int efficiency;
    private final boolean constantEnergy;
    private final boolean cheapOverclocking;
    private final boolean cheapOverclockingSpecified;
    private final int minimumLightAbove;
    private final boolean searchOutputTanks;
    private final boolean exclusiveOutputs;
    private final List<String> structureKeys;

    private RecipeMachine(MachineCasing casing, EnergyInput energy, EnergyInput auxiliary, boolean startupEnergy,
                          MachineIo io, boolean largeRecipe, String recipeMap, int parallel, int efficiency,
                          boolean constantEnergy, boolean cheapOverclocking, boolean cheapOverclockingSpecified,
                          int minimumLightAbove, boolean searchOutputTanks, boolean exclusiveOutputs,
                          List<String> structureKeys) {
        this.casing = casing;
        this.energy = energy;
        this.auxiliary = auxiliary;
        this.startupEnergy = startupEnergy;
        this.io = io;
        this.largeRecipe = largeRecipe;
        this.recipeMap = recipeMap;
        this.parallel = parallel;
        this.efficiency = efficiency;
        this.constantEnergy = constantEnergy;
        this.cheapOverclocking = cheapOverclocking;
        this.cheapOverclockingSpecified = cheapOverclockingSpecified;
        this.minimumLightAbove = minimumLightAbove;
        this.searchOutputTanks = searchOutputTanks;
        this.exclusiveOutputs = exclusiveOutputs;
        this.structureKeys = structureKeys;
    }

    @Override public MachineCasing casing() { return casing; }
    public EnergyInput energy() { return energy; }

    /** Second input. Its buffer is separate from {@link #energy}; null when the machine has none. */
    public EnergyInput auxiliary() { return auxiliary; }

    /** When true, a recipe's special value is charged from the auxiliary buffer before progress starts. */
    public boolean startupEnergy() { return startupEnergy; }

    /** Null when item and fluid faces were left at the six-side default with no automatic face. */
    public MachineIo io() { return io; }

    public boolean basic() { return !largeRecipe; }
    public boolean largeRecipe() { return largeRecipe; }
    public String recipeMap() { return recipeMap; }
    public int parallel() { return parallel; }
    public int efficiency() { return efficiency; }
    public boolean constantEnergy() { return constantEnergy; }
    public boolean cheapOverclocking() { return cheapOverclocking; }
    public boolean cheapOverclockingSpecified() { return cheapOverclockingSpecified; }
    public int minimumLightAbove() { return minimumLightAbove; }
    public boolean searchOutputTanks() { return searchOutputTanks; }
    public boolean exclusiveOutputs() { return exclusiveOutputs; }
    public List<String> structureKeys() { return List.copyOf(structureKeys); }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RecipeMachine that)) return false;
        return largeRecipe == that.largeRecipe && parallel == that.parallel && efficiency == that.efficiency
                && constantEnergy == that.constantEnergy && cheapOverclocking == that.cheapOverclocking
                && cheapOverclockingSpecified == that.cheapOverclockingSpecified
                && minimumLightAbove == that.minimumLightAbove && searchOutputTanks == that.searchOutputTanks
                && exclusiveOutputs == that.exclusiveOutputs && startupEnergy == that.startupEnergy
                && casing.equals(that.casing) && energy.equals(that.energy)
                && Objects.equals(auxiliary, that.auxiliary) && Objects.equals(io, that.io)
                && recipeMap.equals(that.recipeMap) && structureKeys.equals(that.structureKeys);
    }

    @Override public String toString() {
        return "RecipeMachine[casing=" + casing + ", energy=" + energy + ", auxiliary=" + auxiliary
                + ", startupEnergy=" + startupEnergy + ", recipeMap=" + recipeMap + ", parallel=" + parallel
                + ", efficiency=" + efficiency + ", largeRecipe=" + largeRecipe + "]";
    }

    @Override public int hashCode() {
        return Objects.hash(casing, energy, auxiliary, startupEnergy, io, largeRecipe, recipeMap, parallel,
                efficiency, constantEnergy, cheapOverclocking, cheapOverclockingSpecified, minimumLightAbove,
                searchOutputTanks, exclusiveOutputs, structureKeys);
    }

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

        public Builder energyOutput(EnergyOutput value) {
            if (value != null) throw new IllegalArgumentException("Recipe machines cannot declare energy output");
            return this;
        }
        public Builder output(FuelOutput value) {
            if (value != null) throw new IllegalArgumentException("Recipe machines cannot declare fuel output");
            return this;
        }
        public Builder io(MachineIo value) { io = value; return this; }
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
        public Builder structureKeys(List<String> value) { structureKeys = value; return this; }

        public RecipeMachine build() {
            if (casing == null) throw new IllegalArgumentException("Missing casing");
            String id = casing.id();
            String machineType = casing.machineType() == null ? id : casing.machineType();
            if (!AddonNames.matches(AddonNames.MACHINE_TYPE, machineType))
                throw new IllegalArgumentException("Invalid machine type for " + id + ": " + machineType);
            int tier = casing.tierSpecified() ? casing.tier() : 1;
            if (tier < 1)
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
            if (energy.perTick() < 0)
                throw new IllegalArgumentException("Invalid energy per tick for " + id + ": " + energy.perTick());
            if (energy.rangeSpecified() && EnergyType.time(energy.type()))
                throw new IllegalArgumentException("TU machines cannot declare an energy range: " + id);
            if (auxiliary != null) {
                if (auxiliary.dynamic())
                    throw new IllegalArgumentException("Recipe machine '" + id + "' auxiliary energy cannot pick its type");
                if (!auxiliary.anyPacket() && !auxiliary.rangeSpecified())
                    throw new IllegalArgumentException("Recipe machine '" + id
                            + "' auxiliary energy needs a packet range or anyPacket");
                if (auxiliary.perTick() < 1 || auxiliary.perTick() > AddonLimits.MAX_PER_TICK)
                    throw new IllegalArgumentException("Invalid auxiliary energy per tick for " + id);
                if (!auxiliary.capacitySpecified() || auxiliary.capacity() < 1)
                    throw new IllegalArgumentException("Recipe machine '" + id
                            + "' auxiliary energy capacity must be positive");
                if (!auxiliary.facesSpecified() || auxiliary.faces().isEmpty())
                    throw new IllegalArgumentException("Recipe machine '" + id + "' auxiliary energy needs a face");
                if (auxiliary.rangeSpecified()
                        && (auxiliary.perTick() < auxiliary.rangeMin() || auxiliary.perTick() > auxiliary.rangeMax()))
                    throw new IllegalArgumentException("Recipe machine '" + id
                            + "' auxiliary energy per tick must sit inside the packet range");
                if (EnergyType.same(auxiliary.type(), energy.type())) {
                    var mainFaces = energy.facesSpecified() ? energy.faces() : MachineFace.all();
                    for (MachineFace face : auxiliary.faces())
                        if (mainFaces.contains(face))
                            throw new IllegalArgumentException("Recipe machine '" + id
                                    + "' auxiliary energy overlaps an input face");
                }
            } else if (startupEnergy) {
                throw new IllegalArgumentException("Recipe machine '" + id + "' startup energy needs auxiliary energy");
            }
            var faces = energy.facesSpecified() ? energy.faces() : MachineFace.all();
            if (!AddonNames.matches(AddonNames.RECIPE_MAP, recipeMap))
                throw new IllegalArgumentException("Invalid recipe map name: " + recipeMap);
            if (parallel < 1)
                throw new IllegalArgumentException("Invalid parallel for " + id + ": " + parallel);
            if (efficiency < 1 || efficiency > AddonLimits.MAX_EFFICIENCY)
                throw new IllegalArgumentException("Invalid efficiency for " + id + ": " + efficiency);
            if (minimumLightAbove < 0 || minimumLightAbove > 15)
                throw new IllegalArgumentException("Invalid light requirement for " + id + ": " + minimumLightAbove);
            var keys = List.copyOf(structureKeys == null ? List.of() : structureKeys);
            for (String key : keys) {
                if (!AddonNames.matches(AddonNames.RECIPE_MAP, key))
                    throw new IllegalArgumentException("Invalid structure key for " + id + ": " + key);
            }
            if (io != null && io.hasTarget() && !largeRecipe)
                throw new IllegalArgumentException("IO targets require a large recipe machine: " + id);
            return new RecipeMachine(casing.resolve(machineType, true, tier), energy.withFaces(faces),
                    auxiliary == null ? null : auxiliary.withFaces(auxiliary.faces()), startupEnergy, io,
                    largeRecipe, recipeMap, parallel, efficiency, constantEnergy, cheapOverclocking,
                    cheapOverclockingSpecified, minimumLightAbove, searchOutputTanks, exclusiveOutputs, keys);
        }
    }
}
