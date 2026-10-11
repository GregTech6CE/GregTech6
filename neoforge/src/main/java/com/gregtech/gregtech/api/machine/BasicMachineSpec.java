package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialChemistry.WeightedMaterial;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.MachineRecipeMaps;

import java.util.List;
import java.util.Objects;

/** Defines one basic machine variant (GT6 {@code MultiTileEntityBasicMachine}). */
public record BasicMachineSpec(
        String id,
        GTMaterial material,
        String machineName,
        String energyType,
        int tier,
        long energyIn,
        long energyOut,
        float hardness,
        float blastResistance,
        FaceConfig faceConfig,
        List<WeightedMaterial> constructionMaterials,
        RecipeMap recipeMap,
        int parallelLimit,
        /** Minimum energy per tick accepted, from GT6 {@code NBT_INPUT_MIN}. */
        long energyInMin,
        /** Maximum energy per tick accepted, from GT6 {@code NBT_INPUT_MAX}. */
        long energyInMax
) {
    public BasicMachineSpec {
        if (id == null || !id.matches("[a-z0-9/._-]+"))
            throw new IllegalArgumentException("Invalid machine ID: " + id);
        if (material == null || !material.isValid())
            throw new IllegalArgumentException("Invalid casing material for " + id);
        Objects.requireNonNull(machineName, "Machine type");
        Objects.requireNonNull(faceConfig, "Machine face configuration");
        Objects.requireNonNull(recipeMap, "Machine recipe map");
        requireEnergy(energyType);
        if (tier < 1 || parallelLimit < 1 || energyIn < 0 || energyOut < 0
                || !Float.isFinite(hardness) || !Float.isFinite(blastResistance))
            throw new IllegalArgumentException("Invalid machine parameters for " + id);
        if (energyInMin < 1 || energyInMax < energyInMin)
            throw new IllegalArgumentException("Invalid energy range for " + id + ": "
                    + energyInMin + ".." + energyInMax);
        constructionMaterials = List.copyOf(constructionMaterials);
    }

    /**
     * GT6 {@code MultiTileEntityBasicMachine#readFromNBT2}: without {@code NBT_INPUT_MIN} /
     * {@code NBT_INPUT_MAX} the accepted range is half to double the nominal input. That is the
     * default for every machine GT6 does not give an explicit range.
     */
    public static long defaultEnergyInMin(String energyType, long energyIn) {
        return requireEnergy(energyType) == GregTechTags.Energy.TU ? 1 : Math.max(1, energyIn / 2);
    }

    /** @see #defaultEnergyInMin(String, long) */
    public static long defaultEnergyInMax(String energyType, long energyIn) {
        return requireEnergy(energyType) == GregTechTags.Energy.TU ? 16 : energyIn * 2;
    }

    /** Compatibility for imported definitions. New content supplies recipeMap and parallelLimit explicitly. */
    public BasicMachineSpec(String id, GTMaterial material, String machineName, String energyType,
                            int tier, long energyIn, long energyOut, float hardness, float blastResistance,
                            FaceConfig faceConfig, List<WeightedMaterial> constructionMaterials) {
        this(id, material, machineName, energyType, tier, energyIn, energyOut, hardness, blastResistance,
                faceConfig, constructionMaterials, MachineRecipeMaps.byMachineName(machineName),
                legacyParallelLimit(machineName),
                defaultEnergyInMin(energyType, energyIn), defaultEnergyInMax(energyType, energyIn));
    }

    public static int legacyParallelLimit(String machineName) {
        return switch (machineName) {
            case "largemixer", "largefermenter" -> 256;
            case "largecoagulator", "largebath", "largeoven", "largesluice",
                 "largecrusher", "largeshredder", "largesqueezer", "largemassfab" -> 64;
            case "largecentrifuge", "largeelectrolyzer", "largeautoclave" -> 16;
            default -> 1;
        };
    }

    private static GregTechTags.Tag requireEnergy(String shortName) {
        return GregTechTags.Energy.ALL.stream().filter(t -> t.getShortName().equalsIgnoreCase(shortName))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown machine energy type: " + shortName));
    }

    public GregTechTags.Tag energyTag() { return requireEnergy(energyType); }

    public static Builder builder(String id, GTMaterial casing) { return new Builder(id, casing); }

    public static final class Builder {
        private final String id;
        private final GTMaterial material;
        private String machineType;
        private GregTechTags.Tag energy;
        private long energyIn;
        private long energyOut;
        private int tier = 1;
        private int parallel = 1;
        private float hardness = 5;
        private float resistance = 5;
        private FaceConfig faces = FaceConfig.ALL_SIDES;
        private RecipeMap recipes;
        private List<WeightedMaterial> construction;
        private long energyInMin;
        private long energyInMax;

        private Builder(String id, GTMaterial casing) {
            this.id = id; material = casing; machineType = id;
            construction = List.of(); // No guessed material amount for definitions without source data.
        }
        public Builder machineType(String value) { machineType = value; return this; }
        public Builder recipes(RecipeMap value) { recipes = value; return this; }
        public Builder energy(GregTechTags.Tag type, long inputPerTick) { energy = type; energyIn = inputPerTick; return this; }
        public Builder energyOutput(long outputPerTick) { energyOut = outputPerTick; return this; }
        /** GT6 {@code NBT_INPUT_MIN} / {@code NBT_INPUT_MAX}: the accepted input range. */
        public Builder energyRange(long inputMin, long inputMax) { energyInMin = inputMin; energyInMax = inputMax; return this; }
        public Builder tier(int value) { tier = value; return this; }
        public Builder parallel(int value) { parallel = value; return this; }
        public Builder strength(float hardness, float resistance) { this.hardness = hardness; this.resistance = resistance; return this; }
        public Builder faces(FaceConfig value) { faces = value; return this; }
        public Builder constructionMaterials(List<WeightedMaterial> value) { construction = List.copyOf(value); return this; }
        public BasicMachineSpec build() {
            Objects.requireNonNull(energy, "Choose the machine energy type explicitly");
            return new BasicMachineSpec(id, material, machineType, energy.getShortName(), tier, energyIn, energyOut,
                    hardness, resistance, faces, construction, recipes, parallel,
                    energyInMin > 0 ? energyInMin : defaultEnergyInMin(energy.getShortName(), energyIn),
                    energyInMax > 0 ? energyInMax : defaultEnergyInMax(energy.getShortName(), energyIn));
        }
    }
    public String textureFolder() { return machineName; }
}
