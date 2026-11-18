package gregtech.engine.machine;

import gregtech.api.machine.BasicMachineParameters;
import gregtech.api.machine.IoTarget;
import gregtech.api.machine.MachineControllerKind;
import gregtech.api.machine.MachineFace;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import gregtech.content.data.BasicMachineOriginalParams;
import gregtech.content.data.GregTechTags;
import gregtech.content.energy.OriginalAdjacentEnergyRules;
import gregtech.content.machine.OriginalBasicMachineRules;
import gregtech.content.multiblock.OriginalLargeRecipeMachineData;
import gregtech.engine.energy.FaceConfig;
import gregtech.engine.multiblock.LargeMachineProcessingRules;
import gregtech.engine.recipe.RecipeMap;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Defines one basic machine variant (GT6 {@code MultiTileEntityBasicMachine}). */
public record BasicMachineSpec(
        String id,
        String namespace,
        String machineName,
        Casing casing,
        RecipeMap recipeMap,
        Energy energy,
        Processing processing,
        Layout layout,
        @Nullable AuxiliaryEnergy auxiliaryEnergy) {

    public record Casing(
            GTMaterial material, float hardness, float blastResistance, List<MaterialComponent> constructionMaterials) {
        public Casing {
            constructionMaterials = List.copyOf(constructionMaterials);
        }

        public static Casing from(BasicMachineParameters parameters) {
            return new Casing(
                    parameters.material(),
                    parameters.hardness(),
                    parameters.blastResistance(),
                    parameters.constructionMaterials());
        }
    }

    /** Effective packet limits, with the declaration flag used by large recipe controllers. */
    public record InputRange(long min, long max, boolean declared) {}

    public record Energy(String type, int tier, long inputPerTick, long outputPerTick, InputRange range) {
        public Energy {
            Objects.requireNonNull(range, "Machine input range");
        }
    }

    public record Processing(
            int parallelLimit,
            /** Parts per 10000. */
            int efficiency,
            /** Constant draw while the machine is not using TU. */
            boolean constantEnergy,
            boolean cheapOverclocking,
            boolean cheapOverclockingDeclared,
            boolean parallelScalesDuration,
            int minimumLightAbove,
            /** After input tanks miss, search and drain the output tanks that matched. */
            boolean searchOutputTanks,
            /** Roll the first item chance once for a recipe with exactly two item outputs. */
            boolean exclusiveOutputs) {}

    public record Layout(
            MachineControllerKind kind,
            FaceConfig faces,
            List<String> structureKeys,
            BasicMachineParameters.IoPorts io,
            List<EnergySource> energySources,
            /** Skips six-face item and fluid auto-input. */
            boolean suppressSideAutoInput,
            /** Built-in recipe multiblocks still configure their own buffers and backpressure. */
            boolean unlimitedOutputTanks) {
        public Layout {
            Objects.requireNonNull(kind, "controller kind");
            Objects.requireNonNull(faces, "Machine face configuration");
            Objects.requireNonNull(io, "Machine IO targets");
            structureKeys = List.copyOf(structureKeys);
            energySources = List.copyOf(energySources);
        }
    }

    /** Optional second input. Startup charging uses the recipe's special value. */
    public record AuxiliaryEnergy(
            GregTechTags.Tag type,
            long inputPerTick,
            long min,
            long max,
            long capacity,
            int faceMask,
            boolean startup) {
        public AuxiliaryEnergy {
            Objects.requireNonNull(type, "Auxiliary energy type");
        }
    }

    public BasicMachineSpec {
        if (id == null || !id.matches("[a-z0-9/._-]+")) throw new IllegalArgumentException("Invalid machine ID: " + id);
        Objects.requireNonNull(casing, "Machine casing");
        if (casing.material() == null || !casing.material().isValid())
            throw new IllegalArgumentException("Invalid casing material for " + id);
        Objects.requireNonNull(machineName, "Machine type");
        Objects.requireNonNull(recipeMap, "Machine recipe map");
        Objects.requireNonNull(energy, "Machine energy");
        Objects.requireNonNull(processing, "Machine processing");
        Objects.requireNonNull(layout, "Machine layout");
        requireEnergy(energy.type());
        if (energy.tier() < 1
                || processing.parallelLimit() < 1
                || energy.inputPerTick() < 0
                || energy.outputPerTick() < 0
                || !Float.isFinite(casing.hardness())
                || !Float.isFinite(casing.blastResistance()))
            throw new IllegalArgumentException("Invalid machine parameters for " + id);
        var range = energy.range();
        if (range.min() < 1 || range.max() < range.min())
            throw new IllegalArgumentException(
                    "Invalid energy range for " + id + ": " + range.min() + ".." + range.max());
        if (namespace == null || !namespace.matches("[a-z][a-z0-9_]{1,63}"))
            throw new IllegalArgumentException("Invalid machine namespace: " + namespace);
        if (processing.efficiency() < 1 || processing.efficiency() > 100_000)
            throw new IllegalArgumentException("Invalid processing efficiency for " + id);
        if (processing.minimumLightAbove() < 0 || processing.minimumLightAbove() > 15)
            throw new IllegalArgumentException(
                    "Invalid light requirement for " + id + ": " + processing.minimumLightAbove());
        if (auxiliaryEnergy != null
                && (!GregTechTags.Energy.ALL.contains(auxiliaryEnergy.type())
                        || auxiliaryEnergy.inputPerTick() < 1
                        || auxiliaryEnergy.min() < 1
                        || auxiliaryEnergy.max() < auxiliaryEnergy.min()
                        || auxiliaryEnergy.capacity() < 1
                        || auxiliaryEnergy.faceMask() == 0))
            throw new IllegalArgumentException("Invalid auxiliary energy for " + id);
        var io = layout.io();
        if (!io.equals(BasicMachineParameters.IoPorts.NONE)
                && layout.kind() != MachineControllerKind.LARGE_RECIPE
                && layout.kind() != MachineControllerKind.MATTER)
            throw new IllegalArgumentException("IO targets require a large recipe machine: " + id);
        // Built-in large machines keep the automatic face for the tooltip and also name a cell.
        if (!"gt6ce".equals(namespace)) {
            var faces = layout.faces();
            rejectAutomaticFace(id, "item output", io.itemOutputTarget(), faces.itemAutoOutput());
            rejectAutomaticFace(id, "fluid output", io.fluidOutputTarget(), faces.fluidAutoOutput());
            rejectAutomaticFace(id, "item input", io.itemInputSource(), faces.itemAutoInput());
            rejectAutomaticFace(id, "fluid input", io.fluidInputSource(), faces.fluidAutoInput());
        }
    }

    private static void rejectAutomaticFace(String id, String channel, @Nullable IoTarget target, int automatic) {
        if (target != null && automatic >= 0)
            throw new IllegalArgumentException(channel + " target conflicts with an automatic face: " + id);
    }

    /** Special built-in controllers take precedence over a layout with the same name. */
    private static MachineControllerKind builtInController(String machineName) {
        return gregtech.content.multiblock.RecipeControllerFamilies.kind(machineName);
    }

    /** Applies the name-based processing and layout rules of the built-in machine catalog. */
    public static BasicMachineSpec builtIn(BasicMachineParameters parameters, RecipeMap recipeMap) {
        String machineName = parameters.machineName();
        var profile = parameters.energy();
        String energyType = profile.type().name();
        int tier = profile.tier();
        var range = new InputRange(profile.energyInMin(), profile.energyInMax(), false);
        var energy = new Energy(energyType, tier, profile.energyIn(), profile.energyOut(), range);
        var processing = new Processing(
                parameters.options().parallelLimit(),
                builtInProcessingEfficiency(machineName, tier),
                builtInConstantEnergy(machineName, tier),
                builtInCheapOverclocking(machineName, tier, energyType),
                false,
                builtInParallelScalesDuration(machineName, tier, energyType),
                0,
                false,
                false);
        boolean originalLargeLayout = originalProcessingName(machineName);
        var io = originalLargeLayout
                ? new BasicMachineParameters.IoPorts(
                        builtInOutputTarget(machineName, true), builtInOutputTarget(machineName, false), null, null)
                : BasicMachineParameters.IoPorts.NONE;
        var layout = new Layout(
                builtInController(machineName),
                FaceConfig.from(parameters.faceConfig()),
                OriginalLargeRecipeMachineData.structureKeys(machineName),
                io,
                builtInEnergySources(machineName),
                originalLargeLayout,
                originalLargeLayout || "fusionreactor".equals(machineName));
        return new BasicMachineSpec(
                parameters.id(),
                "gt6ce",
                machineName,
                Casing.from(parameters),
                recipeMap,
                energy,
                processing,
                layout,
                null);
    }

    /**
     * Efficiency written onto a built-in spec. Single blocks and the compact matter fabricator
     * use the original name table. Large controllers keep {@link LargeMachineProcessingRules}.
     */
    private static int builtInProcessingEfficiency(String machineName, int tier) {
        var kind = builtInController(machineName);
        if (kind == MachineControllerKind.BASIC || kind == MachineControllerKind.COMPACT_MATTER)
            return OriginalBasicMachineRules.efficiency(machineName, tier);
        return LargeMachineProcessingRules.efficiency(machineName);
    }

    /** Constant while the machine is not using TU. The TU exclusion stays in the block entity. */
    private static boolean builtInConstantEnergy(String machineName, int tier) {
        var kind = builtInController(machineName);
        if (kind == MachineControllerKind.MATTER) return false;
        if (kind == MachineControllerKind.BASIC || kind == MachineControllerKind.COMPACT_MATTER)
            return !OriginalBasicMachineRules.noConstantPower(machineName, tier);
        return LargeMachineProcessingRules.constantEnergy(false, machineName);
    }

    /** Cheap overclocking without marking the value as an addon declaration. */
    private static boolean builtInCheapOverclocking(String machineName, int tier, String energyType) {
        var kind = builtInController(machineName);
        if (kind == MachineControllerKind.COMPACT_MATTER) return true;
        if (usesLargeBuiltinProcessing(machineName, kind)) return !"TU".equals(energyType);
        return OriginalBasicMachineRules.cheapOverclocking(machineName, tier);
    }

    /**
     * Parallel-duration flag the block entity uses today. Large controllers, including the cryo
     * tower, follow “not TU”. A single block uses its original row, or true when it has none.
     */
    private static boolean builtInParallelScalesDuration(String machineName, int tier, String energyType) {
        var kind = builtInController(machineName);
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

    private static boolean originalProcessingName(String machineName) {
        return OriginalLargeRecipeMachineData.handles(machineName) || "largemassfab".equals(machineName);
    }

    private static IoTarget builtInOutputTarget(String machineName, boolean items) {
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
                sources.add(new EnergySource(
                        source.right(),
                        source.up(),
                        source.back(),
                        source.outputFace().name()));
        }
        if ("distillationtower".equals(machineName) || "cryodistillationtower".equals(machineName)) {
            for (var source : OriginalAdjacentEnergyRules.TOWER_SOURCES)
                sources.add(new EnergySource(source.right(), source.up(), source.back(), "UP"));
        }
        return List.copyOf(sources);
    }

    private static GregTechTags.Tag requireEnergy(String shortName) {
        return GregTechTags.Energy.ALL.stream()
                .filter(t -> t.getShortName().equalsIgnoreCase(shortName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown machine energy type: " + shortName));
    }

    public GTMaterial material() {
        return casing.material();
    }

    public float hardness() {
        return casing.hardness();
    }

    public float blastResistance() {
        return casing.blastResistance();
    }

    public List<MaterialComponent> constructionMaterials() {
        return casing.constructionMaterials();
    }

    public String energyType() {
        return energy.type();
    }

    public int tier() {
        return energy.tier();
    }

    public long energyIn() {
        return energy.inputPerTick();
    }

    public long energyOut() {
        return energy.outputPerTick();
    }

    public long energyInMin() {
        return energy.range().min();
    }

    public long energyInMax() {
        return energy.range().max();
    }

    public GregTechTags.Tag energyTag() {
        return requireEnergy(energy.type());
    }

    public int parallelLimit() {
        return processing.parallelLimit();
    }

    public int processingEfficiency() {
        return processing.efficiency();
    }

    public boolean constantEnergy() {
        return processing.constantEnergy();
    }

    public boolean cheapOverclocking() {
        return processing.cheapOverclocking();
    }

    public boolean parallelScalesDuration() {
        return processing.parallelScalesDuration();
    }

    public int minimumLightAbove() {
        return processing.minimumLightAbove();
    }

    public boolean searchOutputTanks() {
        return processing.searchOutputTanks();
    }

    public boolean exclusiveOutputs() {
        return processing.exclusiveOutputs();
    }

    public MachineControllerKind controllerKind() {
        return layout.kind();
    }

    public FaceConfig faceConfig() {
        return layout.faces();
    }

    public List<String> structureKeys() {
        return layout.structureKeys();
    }

    public @Nullable IoTarget itemOutputTarget() {
        return layout.io().itemOutputTarget();
    }

    public @Nullable IoTarget fluidOutputTarget() {
        return layout.io().fluidOutputTarget();
    }

    public @Nullable IoTarget itemInputSource() {
        return layout.io().itemInputSource();
    }

    public @Nullable IoTarget fluidInputSource() {
        return layout.io().fluidInputSource();
    }

    public List<EnergySource> energySources() {
        return layout.energySources();
    }

    public boolean suppressSideAutoInput() {
        return layout.suppressSideAutoInput();
    }

    public boolean unlimitedOutputTanks() {
        return layout.unlimitedOutputTanks();
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

    public String textureFolder() {
        return machineName;
    }

    public ResourceLocation modelId(String suffix) {
        return ResourceLocation.fromNamespaceAndPath(namespace, "block/machine/basic/" + machineName + suffix);
    }
}
