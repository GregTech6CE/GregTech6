package gregtech.api.machine;

import gregtech.api.addon.AddonApiRemoval;
import gregtech.api.machine.IoTarget;
import gregtech.api.machine.MachineFaceMasks;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import gregtech.api.energy.EnergyType;


import java.util.List;
import java.util.Objects;

/**
 * Defines one basic machine variant (GT6 {@code MultiTileEntityBasicMachine}).
 * Host-side machine parameter model; addons declare machines through
 * {@link gregtech.api.addon.machine.BehaviorMachine} or {@link gregtech.api.addon.recipe.RecipeMachine}.
 */
public record BasicMachineParameters(
        String id,
        GTMaterial material,
        String machineName,
        String energyType,
        int tier,
        long energyIn,
        long energyOut,
        float hardness,
        float blastResistance,
        MachineFaceMasks faceConfig,
        List<MaterialComponent> constructionMaterials,
        int parallelLimit,
        /** Minimum energy per tick accepted, from GT6 {@code NBT_INPUT_MIN}. */
        long energyInMin,
        /** Maximum energy per tick accepted, from GT6 {@code NBT_INPUT_MAX}. */
        long energyInMax,
        boolean cheapOverclocking,
        boolean cheapOverclockingSpecified,
        int minimumLightAbove,
        boolean searchOutputTanks,
        boolean exclusiveOutputs,
        IoTarget itemOutputTarget,
        IoTarget fluidOutputTarget,
        IoTarget itemInputSource,
        IoTarget fluidInputSource,
        /** True only when an addon called {@code energyRange}. Built-in rows stay false. */
        boolean energyRangeSpecified
) {
    public BasicMachineParameters {
        if (id == null || !id.matches("[a-z0-9/._-]+"))
            throw new IllegalArgumentException("Invalid machine ID: " + id);
        if (material == null || !material.isValid())
            throw new IllegalArgumentException("Invalid casing material for " + id);
        Objects.requireNonNull(machineName, "Machine type");
        Objects.requireNonNull(faceConfig, "Machine face configuration");
        EnergyType.requireKnown(energyType);
        if (tier < 1 || parallelLimit < 1 || energyIn < 0 || energyOut < 0
                || !Float.isFinite(hardness) || !Float.isFinite(blastResistance))
            throw new IllegalArgumentException("Invalid machine parameters for " + id);
        if (energyRangeSpecified || energyInMin != 0 || energyInMax != 0) {
            if (energyInMin < 1 || energyInMax < energyInMin)
                throw new IllegalArgumentException("Invalid energy range for " + id + ": "
                        + energyInMin + ".." + energyInMax);
        }
        if (minimumLightAbove < 0 || minimumLightAbove > 15)
            throw new IllegalArgumentException("Invalid light requirement for " + id + ": " + minimumLightAbove);
        constructionMaterials = List.copyOf(constructionMaterials);
        rejectAutomaticFace(id, "item output", itemOutputTarget, faceConfig.itemAutoOutput());
        rejectAutomaticFace(id, "fluid output", fluidOutputTarget, faceConfig.fluidAutoOutput());
        rejectAutomaticFace(id, "item input", itemInputSource, faceConfig.itemAutoInput());
        rejectAutomaticFace(id, "fluid input", fluidInputSource, faceConfig.fluidAutoInput());
    }

    private static void rejectAutomaticFace(String id, String channel, IoTarget target, int automatic) {
        if (target != null && automatic >= 0)
            throw new IllegalArgumentException(channel + " target conflicts with an automatic face: " + id);
    }

    /** Built-in and older callers keep cheap overclocking on the name table and do not require light. */
    public BasicMachineParameters(String id, GTMaterial material, String machineName, String energyType,
                                  int tier, long energyIn, long energyOut, float hardness, float blastResistance,
                                  MachineFaceMasks faceConfig, List<MaterialComponent> constructionMaterials,
                                  int parallelLimit, long energyInMin, long energyInMax) {
        this(id, material, machineName, energyType, tier, energyIn, energyOut, hardness, blastResistance,
                faceConfig, constructionMaterials, parallelLimit, energyInMin, energyInMax, false, false, 0, false, false,
                null, null, null, null, false);
    }

    /**
     * GT6 {@code MultiTileEntityBasicMachine#readFromNBT2}: without {@code NBT_INPUT_MIN} /
     * {@code NBT_INPUT_MAX} the accepted range is half to double the nominal input. That is the
     * default for every machine GT6 does not give an explicit range.
     */
    public static long defaultEnergyInMin(String energyType, long energyIn) {
        return EnergyType.time(EnergyType.requireKnown(energyType)) ? 1 : Math.max(1, energyIn / 2);
    }

    /** @see #defaultEnergyInMin(String, long) */
    public static long defaultEnergyInMax(String energyType, long energyIn) {
        return EnergyType.time(EnergyType.requireKnown(energyType)) ? 16 : energyIn * 2;
    }

    /**
     * @deprecated Host machine content; the host keeps this table with its own machine definitions.
     * Replacement is the host-side copy. Removal is 0.5.
     */
    @Deprecated(since = "0.3", forRemoval = true)
    @AddonApiRemoval(id = "gregtech.api.machine.BasicMachineParameters#legacyParallelLimit", removeAt = "0.5")
    public static int legacyParallelLimit(String machineName) {
        return switch (machineName) {
            case "largemixer", "largefermenter" -> 256;
            case "largecoagulator", "largebath", "largeoven", "largesluice",
                 "largecrusher", "largeshredder", "largesqueezer", "largemassfab" -> 64;
            case "largecentrifuge", "largeelectrolyzer", "largeautoclave" -> 16;
            default -> 1;
        };
    }

    /** Fixed packet range of a large recipe machine unless the energy is time-based. */
    private static final long LARGE_RECIPE_MIN_INPUT = 512;
    private static final long LARGE_RECIPE_MAX_INPUT = 4096;
    /** TU uses packets of 1..16 units. HU shares the 1-unit minimum but not the 16-unit maximum. */
    private static final long LARGE_RECIPE_TIME_MIN_INPUT = 1;
    private static final long LARGE_RECIPE_TIME_MAX_INPUT = 16;

    /**
     * The range a controller actually accepts. An explicit declaration wins. Otherwise a single
     * block uses half to double; a large recipe machine uses its fixed packet range.
     */
    public long acceptedEnergyInMin(boolean largeRecipe) {
        if (energyRangeSpecified) return energyInMin;
        if (largeRecipe) return EnergyType.time(energyType) || EnergyType.same(energyType, EnergyType.HU)
                ? LARGE_RECIPE_TIME_MIN_INPUT : LARGE_RECIPE_MIN_INPUT;
        return energyInMin > 0 ? energyInMin : defaultEnergyInMin(energyType, energyIn);
    }

    /**
     * @see #acceptedEnergyInMin(boolean)
     * HU uses the time minimum but the general maximum, matching the current host contract.
     */
    public long acceptedEnergyInMax(boolean largeRecipe) {
        if (energyRangeSpecified) return energyInMax;
        if (largeRecipe) return EnergyType.time(energyType)
                ? LARGE_RECIPE_TIME_MAX_INPUT : LARGE_RECIPE_MAX_INPUT;
        return energyInMax > 0 ? energyInMax : defaultEnergyInMax(energyType, energyIn);
    }

    public static Builder builder(String id, GTMaterial casing) { return new Builder(id, casing); }

    public static final class Builder {
        private final String id;
        private final GTMaterial material;
        private String machineType;
        private String energy;
        private long energyIn;
        private long energyOut;
        private int tier = 1;
        private int parallel = 1;
        private float hardness = 5;
        private float resistance = 5;
        private MachineFaceMasks faces = MachineFaceMasks.ALL_SIDES;
        private List<MaterialComponent> construction;
        private long energyInMin;
        private long energyInMax;
        private boolean cheapOverclocking;
        private boolean cheapOverclockingSpecified;
        private int minimumLightAbove;
        private boolean searchOutputTanks;
        private boolean exclusiveOutputs;
        private IoTarget itemOutputTarget;
        private IoTarget fluidOutputTarget;
        private IoTarget itemInputSource;
        private IoTarget fluidInputSource;
        private boolean energyRangeSpecified;
        private boolean energyRangeOmitted;

        private Builder(String id, GTMaterial casing) {
            this.id = id; material = casing; machineType = id;
            construction = List.of(); // No guessed material amount for definitions without source data.
        }
        public Builder machineType(String value) { machineType = value; return this; }
        public Builder energy(String type, long inputPerTick) { energy = type; energyIn = inputPerTick; return this; }
        public Builder energyOutput(long outputPerTick) { energyOut = outputPerTick; return this; }
        /** GT6 {@code NBT_INPUT_MIN} / {@code NBT_INPUT_MAX}: the accepted input range. */
        public Builder energyRange(long inputMin, long inputMax) {
            energyInMin = inputMin; energyInMax = inputMax; energyRangeSpecified = true; return this;
        }
        /** Keep both bounds at 0 so a large recipe machine is recorded as undeclared. */
        public Builder omitEnergyRange() { energyRangeOmitted = true; return this; }
        public Builder tier(int value) { tier = value; return this; }
        public Builder parallel(int value) { parallel = value; return this; }
        public Builder strength(float hardness, float resistance) { this.hardness = hardness; this.resistance = resistance; return this; }
        public Builder faces(MachineFaceMasks value) { faces = value; return this; }
        public Builder constructionMaterials(List<MaterialComponent> value) { construction = List.copyOf(value); return this; }
        public Builder cheapOverclocking(boolean value) { cheapOverclocking = value; cheapOverclockingSpecified = true; return this; }
        public Builder minimumLightAbove(int value) { minimumLightAbove = value; return this; }
        public Builder searchOutputTanks(boolean value) { searchOutputTanks = value; return this; }
        public Builder exclusiveOutputs(boolean value) { exclusiveOutputs = value; return this; }
        public Builder itemOutputTarget(IoTarget value) { itemOutputTarget = value; return this; }
        public Builder fluidOutputTarget(IoTarget value) { fluidOutputTarget = value; return this; }
        public Builder itemInputSource(IoTarget value) { itemInputSource = value; return this; }
        public Builder fluidInputSource(IoTarget value) { fluidInputSource = value; return this; }
        public BasicMachineParameters build() {
            Objects.requireNonNull(energy, "Choose the machine energy type explicitly");
            energy = EnergyType.requireKnown(energy);
            long min = energyRangeSpecified ? energyInMin
                    : energyRangeOmitted ? 0 : defaultEnergyInMin(energy, energyIn);
            long max = energyRangeSpecified ? energyInMax
                    : energyRangeOmitted ? 0 : defaultEnergyInMax(energy, energyIn);
            return new BasicMachineParameters(id, material, machineType, energy, tier, energyIn, energyOut,
                    hardness, resistance, faces, construction, parallel, min, max,
                    cheapOverclocking, cheapOverclockingSpecified, minimumLightAbove,
                    searchOutputTanks, exclusiveOutputs,
                    itemOutputTarget, fluidOutputTarget, itemInputSource, fluidInputSource,
                    energyRangeSpecified);
        }
    }
    public String textureFolder() { return machineName; }
}
