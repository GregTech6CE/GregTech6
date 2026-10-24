package gregtech.internal.machine;

import gregtech.api.addon.IoTarget;
import gregtech.api.addon.MachineFace;
import gregtech.internal.energy.FaceConfig;
import gregtech.internal.material.GTMaterial;
import gregtech.internal.material.MaterialChemistry.WeightedMaterial;
import gregtech.internal.recipe.RecipeMap;
import gregtech.content.energy.OriginalAdjacentEnergyRules;
import gregtech.content.machine.OriginalBasicMachineRules;
import gregtech.content.multiblock.LargeMachineProcessingRules;
import gregtech.content.multiblock.OriginalLargeRecipeMachineData;
import gregtech.data.BasicMachineOriginalParams;
import gregtech.data.GregTechTags;
import gregtech.data.MachineRecipeMaps;
import net.minecraft.resources.ResourceLocation;

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
        long energyInMax,
        String namespace,
        MachineControllerKind controllerKind,
        /** Parts per 10000. Built-in values come from {@link #builtInProcessingEfficiency}. */
        int processingEfficiency,
        /** Constant draw while the machine is not using TU. */
        boolean constantEnergy,
        List<String> structureKeys,
        boolean cheapOverclocking,
        /** When false, built-in and undeclared large machines keep their existing rule. */
        boolean cheapOverclockingDeclared,
        int minimumLightAbove,
        /** After the input tanks miss, search the output tanks and drain the tanks that matched. */
        boolean searchOutputTanks,
        /** Roll the first item chance once. The row must have exactly two item outputs. */
        boolean exclusiveOutputs,
        IoTarget itemOutputTarget,
        IoTarget fluidOutputTarget,
        IoTarget itemInputSource,
        IoTarget fluidInputSource,
        /** An addon called {@code energyRange}. Built-in specs stay false and keep their own limits. */
        boolean energyRangeDeclared,
        /** Live parallel-duration rule. Not a declaration field. */
        boolean parallelScalesDuration,
        /** Neighbors this controller toggles. Empty leaves the six adjacent faces alone. */
        List<EnergySource> energySources,
        /** Skips six-face item and fluid auto-input. Not inferred from an output cell. */
        boolean suppressSideAutoInput,
        /**
         * Output tanks have no finite capacity. This matches {@code sourceUnlimitedOutputs}:
         * the thirteen original controllers and the fusion reactor. Distillation towers stay false
         * and keep their own tank cap.
         */
        boolean unlimitedOutputTanks,
        /** Second input. Null when the machine has none. */
        String auxiliaryEnergyType,
        long auxiliaryEnergyIn,
        long auxiliaryEnergyMin,
        long auxiliaryEnergyMax,
        long auxiliaryEnergyCapacity,
        int auxiliaryFaceMask,
        /** Charge a recipe's special value from the auxiliary buffer before progress starts. */
        boolean startupEnergy
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
        if (namespace == null || !namespace.matches("[a-z][a-z0-9_]{1,63}"))
            throw new IllegalArgumentException("Invalid machine namespace: " + namespace);
        Objects.requireNonNull(controllerKind, "controller kind");
        if (processingEfficiency < 1 || processingEfficiency > 100_000)
            throw new IllegalArgumentException("Invalid processing efficiency for " + id);
        if (minimumLightAbove < 0 || minimumLightAbove > 15)
            throw new IllegalArgumentException("Invalid light requirement for " + id + ": " + minimumLightAbove);
        if (auxiliaryEnergyType != null) {
            requireEnergy(auxiliaryEnergyType);
            if (auxiliaryEnergyIn < 1 || auxiliaryEnergyMin < 1 || auxiliaryEnergyMax < auxiliaryEnergyMin
                    || auxiliaryEnergyCapacity < 1 || auxiliaryFaceMask == 0)
                throw new IllegalArgumentException("Invalid auxiliary energy for " + id);
        } else if (startupEnergy || auxiliaryEnergyIn != 0 || auxiliaryEnergyMin != 0 || auxiliaryEnergyMax != 0
                || auxiliaryEnergyCapacity != 0 || auxiliaryFaceMask != 0) {
            throw new IllegalArgumentException("Auxiliary energy fields need a type: " + id);
        }
        constructionMaterials = List.copyOf(constructionMaterials);
        structureKeys = List.copyOf(structureKeys == null ? List.of() : structureKeys);
        energySources = List.copyOf(energySources == null ? List.of() : energySources);
        if ((itemOutputTarget != null || fluidOutputTarget != null || itemInputSource != null || fluidInputSource != null)
                && controllerKind != MachineControllerKind.LARGE_RECIPE
                && controllerKind != MachineControllerKind.MATTER)
            throw new IllegalArgumentException("IO targets require a large recipe machine: " + id);
        // Built-in large machines keep the automatic face for the tooltip and also name a cell.
        if (!"gt6ce".equals(namespace)) {
            rejectAutomaticFace(id, "item output", itemOutputTarget, faceConfig.itemAutoOutput());
            rejectAutomaticFace(id, "fluid output", fluidOutputTarget, faceConfig.fluidAutoOutput());
            rejectAutomaticFace(id, "item input", itemInputSource, faceConfig.itemAutoInput());
            rejectAutomaticFace(id, "fluid input", fluidInputSource, faceConfig.fluidAutoInput());
        }
    }

    private static void rejectAutomaticFace(String id, String channel, IoTarget target, int automatic) {
        if (target != null && automatic >= 0)
            throw new IllegalArgumentException(channel + " target conflicts with an automatic face: " + id);
    }

    /** Built-in machines stay in {@code gregtech} and keep the historical name-based rules. */
    public BasicMachineSpec(String id, GTMaterial material, String machineName, String energyType,
                            int tier, long energyIn, long energyOut, float hardness, float blastResistance,
                            FaceConfig faceConfig, List<WeightedMaterial> constructionMaterials,
                            RecipeMap recipeMap, int parallelLimit, long energyInMin, long energyInMax) {
        this(id, material, machineName, energyType, tier, energyIn, energyOut, hardness, blastResistance,
                faceConfig, constructionMaterials, recipeMap, parallelLimit, energyInMin, energyInMax,
                "gt6ce", MachineControllerKind.builtIn(machineName),
                builtInProcessingEfficiency(machineName, tier),
                builtInConstantEnergy(machineName, tier),
                OriginalLargeRecipeMachineData.structureKeys(machineName),
                builtInCheapOverclocking(machineName, tier, energyType), false, 0, false, false,
                builtInOutputTarget(machineName, true), builtInOutputTarget(machineName, false),
                null, null, false,
                builtInParallelScalesDuration(machineName, tier, energyType),
                builtInEnergySources(machineName),
                builtInSuppressSideAutoInput(machineName),
                builtInUnlimitedOutputTanks(machineName),
                null, 0, 0, 0, 0, 0, false);
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

    /**
     * Efficiency written onto a built-in spec. Single blocks and the compact matter fabricator
     * use the original name table. Large controllers keep {@link LargeMachineProcessingRules}.
     */
    public static int builtInProcessingEfficiency(String machineName, int tier) {
        var kind = MachineControllerKind.builtIn(machineName);
        if (kind == MachineControllerKind.BASIC || kind == MachineControllerKind.COMPACT_MATTER)
            return OriginalBasicMachineRules.efficiency(machineName, tier);
        return LargeMachineProcessingRules.efficiency(machineName);
    }

    /** Constant while the machine is not using TU. The TU exclusion stays in the block entity. */
    public static boolean builtInConstantEnergy(String machineName, int tier) {
        var kind = MachineControllerKind.builtIn(machineName);
        if (kind == MachineControllerKind.MATTER) return false;
        if (kind == MachineControllerKind.BASIC || kind == MachineControllerKind.COMPACT_MATTER)
            return !OriginalBasicMachineRules.noConstantPower(machineName, tier);
        return LargeMachineProcessingRules.constantEnergy(false, machineName);
    }

    /** Cheap overclocking without marking the value as an addon declaration. */
    public static boolean builtInCheapOverclocking(String machineName, int tier, String energyType) {
        var kind = MachineControllerKind.builtIn(machineName);
        if (kind == MachineControllerKind.COMPACT_MATTER) return true;
        if (usesLargeBuiltinProcessing(machineName, kind)) return !"TU".equals(energyType);
        return OriginalBasicMachineRules.cheapOverclocking(machineName, tier);
    }

    /**
     * Parallel-duration flag the block entity uses today. Large controllers, including the cryo
     * tower, follow “not TU”. A single block uses its original row, or true when it has none.
     */
    public static boolean builtInParallelScalesDuration(String machineName, int tier, String energyType) {
        var kind = MachineControllerKind.builtIn(machineName);
        if (usesLargeBuiltinProcessing(machineName, kind)) return !"TU".equals(energyType);
        var row = BasicMachineOriginalParams.find(machineName, tier);
        return row == null || row.parallelDuration();
    }

    /**
     * Controllers whose processing is the large-machine rule, including the ones whose kind is
     * still {@link MachineControllerKind#BASIC} because they do not share a layout table.
     */
    private static boolean usesLargeBuiltinProcessing(String machineName, MachineControllerKind kind) {
        return kind == MachineControllerKind.LARGE_RECIPE
                || kind == MachineControllerKind.DISTILLATION
                || kind == MachineControllerKind.MATTER
                || "cryodistillationtower".equals(machineName)
                || "cokeoven".equals(machineName)
                || "fusionreactor".equals(machineName);
    }

    private static long[] builtInImplicitEnergyRange(String machineName, String energyType) {
        return switch (machineName) {
            case "distillationtower", "cryodistillationtower" -> new long[] {1, 1024};
            case "largemassfab" -> new long[] {1, LargeMachineProcessingRules.MATTER_INPUT_MAX};
            default -> {
                boolean timed = "TU".equals(energyType);
                yield new long[] {
                        LargeMachineProcessingRules.inputMinimum(timed, "HU".equals(energyType)),
                        LargeMachineProcessingRules.inputMaximum(timed)
                };
            }
        };
    }

    private static boolean originalProcessingName(String machineName) {
        return OriginalLargeRecipeMachineData.handles(machineName) || "largemassfab".equals(machineName);
    }

    private static IoTarget builtInOutputTarget(String machineName, boolean items) {
        if (!originalProcessingName(machineName)) return null;
        var output = OriginalLargeRecipeMachineData.output(machineName, items);
        return IoTarget.at(output.right(), output.up(), output.back(), receiver(output.receiverFace()));
    }

    private static MachineFace receiver(OriginalLargeRecipeMachineData.Face face) {
        return switch (face) {
            case UP -> MachineFace.TOP;
            case DOWN -> MachineFace.BOTTOM;
            case LEFT -> MachineFace.LEFT;
            case RIGHT -> MachineFace.RIGHT;
            case FRONT -> MachineFace.FRONT;
        };
    }

    private static List<EnergySource> builtInEnergySources(String machineName) {
        var sources = new java.util.ArrayList<EnergySource>();
        if (originalProcessingName(machineName)) {
            for (var source : OriginalLargeRecipeMachineData.sources(machineName))
                sources.add(new EnergySource(source.right(), source.up(), source.back(), source.outputFace().name()));
        }
        if ("distillationtower".equals(machineName) || "cryodistillationtower".equals(machineName)) {
            for (var source : OriginalAdjacentEnergyRules.TOWER_SOURCES)
                sources.add(new EnergySource(source.right(), source.up(), source.back(), "UP"));
        }
        return List.copyOf(sources);
    }

    private static boolean builtInSuppressSideAutoInput(String machineName) {
        return originalProcessingName(machineName);
    }

    private static boolean builtInUnlimitedOutputTanks(String machineName) {
        return originalProcessingName(machineName) || "fusionreactor".equals(machineName);
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
        private String namespace = "gt6ce";
        private MachineControllerKind kind;
        private int efficiency;
        private boolean efficiencySet;
        private boolean constantEnergy = true;
        private boolean constantEnergySet;
        private List<String> structureKeys;
        private boolean cheapOverclocking;
        private boolean cheapOverclockingDeclared;
        private int minimumLightAbove;
        private boolean searchOutputTanks;
        private boolean exclusiveOutputs;
        private IoTarget itemOutputTarget;
        private IoTarget fluidOutputTarget;
        private IoTarget itemInputSource;
        private IoTarget fluidInputSource;
        private boolean energyRangeDeclared;
        private GregTechTags.Tag auxiliaryEnergy;
        private long auxiliaryEnergyIn;
        private long auxiliaryEnergyMin;
        private long auxiliaryEnergyMax;
        private long auxiliaryEnergyCapacity;
        private int auxiliaryFaceMask;
        private boolean startupEnergy;

        private Builder(String id, GTMaterial casing) {
            this.id = id; material = casing; machineType = id;
            construction = List.of(); // No guessed material amount for definitions without source data.
        }
        public Builder machineType(String value) { machineType = value; return this; }
        public Builder recipes(RecipeMap value) { recipes = value; return this; }
        public Builder energy(GregTechTags.Tag type, long inputPerTick) { energy = type; energyIn = inputPerTick; return this; }
        public Builder auxiliaryEnergy(GregTechTags.Tag type, long perTick, long min, long max, long capacity, int faceMask) {
            auxiliaryEnergy = type; auxiliaryEnergyIn = perTick;
            auxiliaryEnergyMin = min; auxiliaryEnergyMax = max;
            auxiliaryEnergyCapacity = capacity; auxiliaryFaceMask = faceMask;
            return this;
        }
        public Builder startupEnergy(boolean value) { startupEnergy = value; return this; }
        public Builder energyOutput(long outputPerTick) { energyOut = outputPerTick; return this; }
        /** GT6 {@code NBT_INPUT_MIN} / {@code NBT_INPUT_MAX}: the accepted input range. */
        public Builder energyRange(long inputMin, long inputMax) {
            energyInMin = inputMin; energyInMax = inputMax; energyRangeDeclared = inputMin > 0 || inputMax > 0; return this;
        }
        /** Accepted range for a machine that did not declare one. Processing reads these numbers; the declared flag stays false. */
        public Builder implicitEnergyRange(long inputMin, long inputMax) {
            energyInMin = inputMin; energyInMax = inputMax; energyRangeDeclared = false; return this;
        }
        public Builder tier(int value) { tier = value; return this; }
        public Builder parallel(int value) { parallel = value; return this; }
        public Builder strength(float hardness, float resistance) { this.hardness = hardness; this.resistance = resistance; return this; }
        public Builder faces(FaceConfig value) { faces = value; return this; }
        public Builder constructionMaterials(List<WeightedMaterial> value) { construction = List.copyOf(value); return this; }
        public Builder namespace(String value) { namespace = value; return this; }
        public Builder controllerKind(MachineControllerKind value) { kind = value; return this; }
        public Builder processingEfficiency(int value) { efficiency = value; efficiencySet = true; return this; }
        public Builder constantEnergy(boolean value) { constantEnergy = value; constantEnergySet = true; return this; }
        public Builder structureKeys(List<String> value) { structureKeys = List.copyOf(value); return this; }
        public Builder cheapOverclocking(boolean value) { cheapOverclocking = value; cheapOverclockingDeclared = true; return this; }
        public Builder minimumLightAbove(int value) { minimumLightAbove = value; return this; }
        public Builder searchOutputTanks(boolean value) { searchOutputTanks = value; return this; }
        public Builder exclusiveOutputs(boolean value) { exclusiveOutputs = value; return this; }
        public Builder itemOutputTarget(IoTarget value) { itemOutputTarget = value; return this; }
        public Builder fluidOutputTarget(IoTarget value) { fluidOutputTarget = value; return this; }
        public Builder itemInputSource(IoTarget value) { itemInputSource = value; return this; }
        public Builder fluidInputSource(IoTarget value) { fluidInputSource = value; return this; }
        public BasicMachineSpec build() {
            Objects.requireNonNull(energy, "Choose the machine energy type explicitly");
            var resolvedKind = kind != null ? kind : MachineControllerKind.builtIn(machineType);
            boolean gt6ce = "gt6ce".equals(namespace);
            String energyName = energy.getShortName();
            long resolvedMin;
            long resolvedMax;
            if (energyInMin > 0) {
                resolvedMin = energyInMin;
                resolvedMax = energyInMax;
            } else if (gt6ce && usesLargeBuiltinProcessing(machineType, resolvedKind)) {
                long[] range = builtInImplicitEnergyRange(machineType, energyName);
                resolvedMin = range[0];
                resolvedMax = range[1];
            } else {
                resolvedMin = defaultEnergyInMin(energyName, energyIn);
                resolvedMax = defaultEnergyInMax(energyName, energyIn);
            }
            boolean resolvedCheap = cheapOverclockingDeclared ? cheapOverclocking
                    : gt6ce ? builtInCheapOverclocking(machineType, tier, energyName)
                    : resolvedKind == MachineControllerKind.LARGE_RECIPE && !"TU".equals(energyName);
            boolean resolvedConstant = constantEnergySet ? constantEnergy
                    : gt6ce ? builtInConstantEnergy(machineType, tier)
                    : LargeMachineProcessingRules.constantEnergy(false, machineType);
            boolean resolvedParallel = gt6ce
                    ? builtInParallelScalesDuration(machineType, tier, energyName)
                    : resolvedKind == MachineControllerKind.LARGE_RECIPE ? !"TU".equals(energyName) : true;
            var resolvedItemOut = itemOutputTarget;
            var resolvedFluidOut = fluidOutputTarget;
            if (gt6ce && resolvedItemOut == null && resolvedFluidOut == null
                    && itemInputSource == null && fluidInputSource == null) {
                resolvedItemOut = builtInOutputTarget(machineType, true);
                resolvedFluidOut = builtInOutputTarget(machineType, false);
            }
            return new BasicMachineSpec(id, material, machineType, energyName, tier, energyIn, energyOut,
                    hardness, resistance, faces, construction, recipes, parallel,
                    resolvedMin, resolvedMax,
                    namespace, resolvedKind,
                    efficiencySet ? efficiency : builtInProcessingEfficiency(machineType, tier),
                    resolvedConstant,
                    structureKeys != null ? structureKeys : OriginalLargeRecipeMachineData.structureKeys(machineType),
                    resolvedCheap, cheapOverclockingDeclared, minimumLightAbove,
                    searchOutputTanks, exclusiveOutputs,
                    resolvedItemOut, resolvedFluidOut, itemInputSource, fluidInputSource,
                    energyRangeDeclared, resolvedParallel,
                    gt6ce ? builtInEnergySources(machineType) : List.of(),
                    gt6ce && builtInSuppressSideAutoInput(machineType),
                    gt6ce && builtInUnlimitedOutputTanks(machineType),
                    auxiliaryEnergy == null ? null : auxiliaryEnergy.getShortName(),
                    auxiliaryEnergyIn, auxiliaryEnergyMin, auxiliaryEnergyMax, auxiliaryEnergyCapacity,
                    auxiliaryFaceMask, startupEnergy);
        }
    }

    /** One neighbor toggled with the controller. The face token matches the original source table. */
    public record EnergySource(int right, int up, int back, String face) {
        public EnergySource {
            if (face == null) throw new IllegalArgumentException("Missing energy source face");
            switch (face) {
                case "UP", "DOWN", "LEFT", "RIGHT", "FRONT" -> {}
                default -> throw new IllegalArgumentException("Unknown energy source face: " + face);
            }
        }
    }
    public String textureFolder() { return machineName; }
    public ResourceLocation modelId(String suffix) {
        return ResourceLocation.fromNamespaceAndPath(namespace, "block/machine/basic/" + machineName + suffix);
    }
}
