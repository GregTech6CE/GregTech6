/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.machine;

import gregtech.api.addon.AddonLimits;
import gregtech.api.addon.AddonNames;
import gregtech.api.energy.EnergyType;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Defines one basic machine variant (GT6 {@code MultiTileEntityBasicMachine}).
 * Host-side machine parameter model; addons declare machines through
 * {@link gregtech.api.addon.machine.BehaviorMachine} or {@link gregtech.api.addon.recipe.RecipeMachine}.
 *
 * @param energy energy type, tier, nominal transfer and accepted input range
 * @param options parallelism, overclocking, light and tank settings
 * @param io automatically pushed or pulled item and fluid targets
 */
public record BasicMachineParameters(
        String id,
        GTMaterial material,
        String machineName,
        EnergyProfile energy,
        float hardness,
        float blastResistance,
        MachineFaceMasks faceConfig,
        List<MaterialComponent> constructionMaterials,
        Options options,
        IoPorts io
) {
    public BasicMachineParameters {
        if (!AddonNames.matches(AddonNames.RESOURCE_PATH, id))
            throw new IllegalArgumentException("Invalid machine ID: " + id);
        if (material == null || !material.isValid())
            throw new IllegalArgumentException("Invalid casing material for " + id);
        if (machineName == null) throw new IllegalArgumentException("Missing machine type for " + id);
        if (faceConfig == null) throw new IllegalArgumentException("Missing machine face configuration for " + id);
        if (energy == null) throw new IllegalArgumentException("Missing machine energy profile for " + id);
        if (options == null) throw new IllegalArgumentException("Missing machine options for " + id);
        if (io == null) throw new IllegalArgumentException("Missing machine IO targets for " + id);
        if (!Float.isFinite(hardness) || !Float.isFinite(blastResistance))
            throw new IllegalArgumentException("Invalid machine parameters for " + id);
        if (constructionMaterials == null)
            throw new IllegalArgumentException("Missing construction materials for " + id);
        constructionMaterials = List.copyOf(constructionMaterials);
        rejectAutomaticFace(id, "item output", io.itemOutputTarget(), faceConfig.itemAutoOutput());
        rejectAutomaticFace(id, "fluid output", io.fluidOutputTarget(), faceConfig.fluidAutoOutput());
        rejectAutomaticFace(id, "item input", io.itemInputSource(), faceConfig.itemAutoInput());
        rejectAutomaticFace(id, "fluid input", io.fluidInputSource(), faceConfig.fluidAutoInput());
    }

    private static void rejectAutomaticFace(String id, String channel, @Nullable IoTarget target,
                                            @Nullable MachineFace automatic) {
        if (target != null && automatic != null)
            throw new IllegalArgumentException(channel + " target conflicts with an automatic face: " + id);
    }

    /**
     * GT6 {@code MultiTileEntityBasicMachine#readFromNBT2}: without {@code NBT_INPUT_MIN} /
     * {@code NBT_INPUT_MAX} the accepted range is half to double the nominal input. That is the
     * default for every machine GT6 does not give an explicit range.
     */
    public static long defaultEnergyInMin(EnergyType energyType, long energyIn) {
        return EnergyProfile.defaultEnergyInMin(energyType, energyIn);
    }

    /** @see #defaultEnergyInMin(EnergyType, long) */
    public static long defaultEnergyInMax(EnergyType energyType, long energyIn) {
        return EnergyProfile.defaultEnergyInMax(energyType, energyIn);
    }

    /** Energy type, tier and transfer of one machine, plus its accepted input range. */
    public record EnergyProfile(
            EnergyType type,
            int tier,
            long energyIn,
            long energyOut,
            EnergyRange range
    ) {
        private static final long TIME_MIN_INPUT = 1;
        private static final long TIME_MAX_INPUT = 16;
        private static final PacketBounds LARGE_RECIPE_GENERAL = new PacketBounds(512, 4096);
        private static final PacketBounds LARGE_RECIPE_TIME = new PacketBounds(TIME_MIN_INPUT, TIME_MAX_INPUT);
        /** HU keeps the time minimum but the general maximum, matching the host contract. */
        private static final PacketBounds LARGE_RECIPE_HU = new PacketBounds(TIME_MIN_INPUT, 4096);

        public EnergyProfile {
            if (type == null) throw new IllegalArgumentException("Missing machine energy type");
            type = EnergyType.known(type);
            if (tier < 1) throw new IllegalArgumentException("Invalid tier for " + type + ": " + tier);
            if (energyIn < 0 || energyOut < 0)
                throw new IllegalArgumentException("Invalid energy for " + type + ": " + energyIn + "/" + energyOut);
            if (range == null) throw new IllegalArgumentException("Missing energy range for " + type);
            if (range instanceof EnergyRange.Defaulted) {
                long min = defaultEnergyInMin(type, energyIn);
                long max = defaultEnergyInMax(type, energyIn);
                if (min < 1 || max < min)
                    throw new IllegalArgumentException("Invalid default energy range for " + type
                            + ": " + min + ".." + max);
            }
        }

        static long defaultEnergyInMin(EnergyType energyType, long energyIn) {
            return EnergyType.known(energyType).time() ? TIME_MIN_INPUT : Math.max(1, energyIn / 2);
        }

        static long defaultEnergyInMax(EnergyType energyType, long energyIn) {
            return EnergyType.known(energyType).time() ? TIME_MAX_INPUT : energyIn * 2;
        }

        /** The stored minimum: declared bounds, the derived default, or 0 when omitted. */
        public long energyInMin() {
            if (range instanceof EnergyRange.Declared declared) return declared.min();
            return range instanceof EnergyRange.Omitted ? 0 : defaultEnergyInMin(type, energyIn);
        }

        /** The stored maximum: declared bounds, the derived default, or 0 when omitted. */
        public long energyInMax() {
            if (range instanceof EnergyRange.Declared declared) return declared.max();
            return range instanceof EnergyRange.Omitted ? 0 : defaultEnergyInMax(type, energyIn);
        }

        /**
         * The range a controller actually accepts. An explicit declaration wins. Otherwise a single
         * block uses half to double; a large recipe machine uses its fixed packet range.
         */
        public long acceptedEnergyInMin(boolean largeRecipe) {
            if (range instanceof EnergyRange.Declared declared) return declared.min();
            return largeRecipe ? largeRecipeBounds().min() : defaultEnergyInMin(type, energyIn);
        }

        /**
         * @see #acceptedEnergyInMin(boolean)
         * HU's large-recipe row keeps the time minimum and the general maximum.
         */
        public long acceptedEnergyInMax(boolean largeRecipe) {
            if (range instanceof EnergyRange.Declared declared) return declared.max();
            return largeRecipe ? largeRecipeBounds().max() : defaultEnergyInMax(type, energyIn);
        }

        private PacketBounds largeRecipeBounds() {
            if (type.equals(EnergyType.HU)) return LARGE_RECIPE_HU;
            return type.time() ? LARGE_RECIPE_TIME : LARGE_RECIPE_GENERAL;
        }

        private record PacketBounds(long min, long max) {}
    }

    /** Non-energy machine settings. */
    public record Options(
            int parallelLimit,
            CheapOverclocking cheapOverclocking,
            int minimumLightAbove,
            boolean searchOutputTanks,
            boolean exclusiveOutputs
    ) {
        /** Whether a machine declared cheap overclocking, and which way. */
        public enum CheapOverclocking { UNSPECIFIED, ENABLED, DISABLED }

        public Options {
            if (parallelLimit < 1) throw new IllegalArgumentException("Invalid parallel limit: " + parallelLimit);
            if (cheapOverclocking == null)
                throw new IllegalArgumentException("Missing cheap overclocking setting");
            if (minimumLightAbove < 0 || minimumLightAbove > AddonLimits.MAX_LIGHT_LEVEL)
                throw new IllegalArgumentException("Invalid light requirement: " + minimumLightAbove);
        }
    }

    /**
     * Automatic item and fluid targets. This is not {@link MachineIo}: that type also carries face
     * lists and automatic-face defaults, while a basic machine keeps those in {@link MachineFaceMasks}.
     */
    public record IoPorts(
            @Nullable IoTarget itemOutputTarget,
            @Nullable IoTarget fluidOutputTarget,
            @Nullable IoTarget itemInputSource,
            @Nullable IoTarget fluidInputSource
    ) {
        public static final IoPorts NONE = new IoPorts(null, null, null, null);
    }

    public static Builder builder(String id, GTMaterial casing) { return new Builder(id, casing); }

    public static final class Builder {
        private final String id;
        private final GTMaterial material;
        private String machineType;
        private EnergyType energy;
        private long energyIn;
        private long energyOut;
        private int tier = 1;
        private int parallel = 1;
        private float hardness = 5;
        private float resistance = 5;
        private MachineFaceMasks faces = MachineFaceMasks.ALL_SIDES;
        private List<MaterialComponent> construction;
        private @Nullable EnergyRange range;
        private Options.CheapOverclocking cheapOverclocking = Options.CheapOverclocking.UNSPECIFIED;
        private int minimumLightAbove;
        private boolean searchOutputTanks;
        private boolean exclusiveOutputs;
        private @Nullable IoTarget itemOutputTarget;
        private @Nullable IoTarget fluidOutputTarget;
        private @Nullable IoTarget itemInputSource;
        private @Nullable IoTarget fluidInputSource;

        private Builder(String id, GTMaterial casing) {
            this.id = id; material = casing; machineType = id;
            construction = List.of(); // No guessed material amount for definitions without source data.
        }
        public Builder machineType(String value) { machineType = value; return this; }
        public Builder energy(EnergyType type, long inputPerTick) { energy = type; energyIn = inputPerTick; return this; }
        public Builder energyOutput(long outputPerTick) { energyOut = outputPerTick; return this; }
        /** GT6 {@code NBT_INPUT_MIN} / {@code NBT_INPUT_MAX}: the accepted input range. */
        public Builder energyRange(long inputMin, long inputMax) {
            range = EnergyRange.declared(inputMin, inputMax); return this;
        }
        /** Keep both bounds at 0 so a large recipe machine is recorded as undeclared. A declared range still wins. */
        public Builder omitEnergyRange() {
            if (!(range instanceof EnergyRange.Declared)) range = EnergyRange.omitted();
            return this;
        }
        public Builder tier(int value) { tier = value; return this; }
        public Builder parallel(int value) { parallel = value; return this; }
        public Builder strength(float hardness, float resistance) { this.hardness = hardness; this.resistance = resistance; return this; }
        public Builder faces(MachineFaceMasks value) { faces = value; return this; }
        public Builder constructionMaterials(List<MaterialComponent> value) { construction = List.copyOf(value); return this; }
        public Builder cheapOverclocking(boolean value) {
            cheapOverclocking = value ? Options.CheapOverclocking.ENABLED : Options.CheapOverclocking.DISABLED;
            return this;
        }
        public Builder minimumLightAbove(int value) { minimumLightAbove = value; return this; }
        public Builder searchOutputTanks(boolean value) { searchOutputTanks = value; return this; }
        public Builder exclusiveOutputs(boolean value) { exclusiveOutputs = value; return this; }
        public Builder itemOutputTarget(@Nullable IoTarget value) { itemOutputTarget = value; return this; }
        public Builder fluidOutputTarget(@Nullable IoTarget value) { fluidOutputTarget = value; return this; }
        public Builder itemInputSource(@Nullable IoTarget value) { itemInputSource = value; return this; }
        public Builder fluidInputSource(@Nullable IoTarget value) { fluidInputSource = value; return this; }
        public BasicMachineParameters build() {
            if (energy == null) throw new IllegalArgumentException("Choose the machine energy type explicitly");
            EnergyProfile profile = new EnergyProfile(energy, tier, energyIn, energyOut,
                    range == null ? EnergyRange.defaulted() : range);
            Options machineOptions = new Options(parallel, cheapOverclocking, minimumLightAbove,
                    searchOutputTanks, exclusiveOutputs);
            IoPorts ports = new IoPorts(itemOutputTarget, fluidOutputTarget, itemInputSource, fluidInputSource);
            return new BasicMachineParameters(id, material, machineType, profile, hardness, resistance,
                    faces, construction, machineOptions, ports);
        }
    }
    public String textureFolder() { return machineName; }
}
