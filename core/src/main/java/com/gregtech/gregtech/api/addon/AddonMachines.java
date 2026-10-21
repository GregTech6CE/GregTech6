package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.energy.MachineFaceMasks;
import com.gregtech.gregtech.api.machine.BasicMachineParameters;
import com.gregtech.gregtech.api.machine.MachineControllerKind;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.multiblock.StructureGrid;
import com.gregtech.gregtech.api.recipe.RecipeMapSpec;
import com.gregtech.gregtech.content.multiblock.SharedLargeMachineLayouts;
import com.gregtech.gregtech.content.multiblock.SharedLargeMachineParts;
import com.gregtech.gregtech.data.FuelRecipeMapDefinitions;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.MachineRecipeMapDefinitions;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/** Queued addon recipe maps, machines and layouts. GregTech installs this once, after materials exist. */
public final class AddonMachines {
    private static final TreeMap<String, Submission> QUEUE = new TreeMap<>();
    private static boolean collected;
    private static boolean installed;
    private static List<AcceptedRecipeMap> recipeMaps = List.of();
    private static List<AcceptedRecipeMap> fuelMaps = List.of();
    private static List<AcceptedMachine> machines = List.of();
    private static List<AcceptedBehavior> behaviorMachines = List.of();
    private static List<AcceptedFuel> fuelGenerators = List.of();
    private static List<AcceptedCompressedGas> compressedGasTanks = List.of();
    private static List<StructurePart> structureParts = List.of();
    private static java.util.Map<String, StructurePart> partsByName = java.util.Map.of();
    /** Block ids and tags named by layouts. Checked after the block registry exists. */
    private static final List<PlainBlock> PLAIN_BLOCKS = new ArrayList<>();
    public record PlainBlock(String addonId, String layout, String name) {}

    private AddonMachines() {}

    /** Runtime-only structure part. The numeric id is assigned at startup and is not saved. */
    public record StructurePart(String addonId, int id, LayoutPart declaration) {}

    public record AcceptedRecipeMap(String addonId, RecipeMapSpec spec) {}
    public record AcceptedMachine(String addonId, AddonMachineDefinition definition) {}
    public record AcceptedBehavior(String addonId, BehaviorMachine declaration,
                                   long energyInMin, long energyInMax, String blockEntityPath) {}
    public record AcceptedFuel(String addonId, FuelGenerator declaration) {}

    /** A compressed gas drum whose temperature ceiling is the material melting point. */
    public record AcceptedCompressedGas(String addonId, CompressedGasTankDeclaration declaration, int meltingPoint) {}

    private record Submission(String addonId, List<RecipeMapSpec> recipeMaps,
                              List<AddonMachineDefinition> machines, List<StructureLayout> layouts,
                              List<BehaviorMachine> behavior, List<RecipeMapSpec> fuelMaps,
                              List<FuelGenerator> generators, List<LayoutPart> parts,
                              List<FluidPipeDeclaration> fluidPipes, List<ItemPipeDeclaration> itemPipes,
                              List<CompressedGasTankDeclaration> tanks) {}
    /** Pipe rows queued before install, in addon-id order. */
    public record QueuedPipes(String addonId, List<FluidPipeDeclaration> fluidPipes, List<ItemPipeDeclaration> itemPipes) {}
    private record Signature(String recipeMapName, MachineControllerKind kind, int efficiency,
                             boolean constantEnergy, List<String> structureKeys, MachineFaceMasks faces,
                             boolean cheapOverclocking, boolean cheapOverclockingSpecified, int minimumLightAbove,
                             boolean searchOutputTanks, boolean exclusiveOutputs,
                             IoTarget itemOutputTarget, IoTarget fluidOutputTarget,
                             IoTarget itemInputSource, IoTarget fluidInputSource,
                             boolean energyRangeSpecified, long energyInMin, long energyInMax) {}
    private record FuelSignature(String fuelMap, int efficiency, String energy, long outputPerTick, int tankCapacity,
                                 FuelOutput output, boolean multi) {}
    private record BehaviorSignature(String energy, String energyOut, List<BehaviorMachine.SlotRole> items,
                                     List<BehaviorMachine.SlotRole> fluids, List<String> toggleKeys) {}
    private record PreparedLayout(String addonId, String machineName, List<SharedLargeMachineLayouts.Cell> cells) {}

    public static void collectContributors() {
        collectContributors(GregTechMachineContributor.class.getClassLoader());
    }

    /** A second call does not load services again. */
    public static synchronized void collectContributors(ClassLoader loader) {
        if (collected) return;
        if (installed) throw new IllegalStateException("Addon machine contributors are closed");
        collected = true;
        Objects.requireNonNull(loader, "loader");
        for (GregTechMachineContributor contributor : ServiceLoader.load(GregTechMachineContributor.class, loader)) {
            try {
                accept(contributor.addonId(), contributor.recipeMaps(), contributor.machines(), contributor.layouts(), List.of());
            } catch (RuntimeException | Error failure) {
                throw new IllegalStateException("Addon machine contributor failed: " + safeId(contributor)
                        + " (" + contributor.getClass().getName() + ")", failure);
            }
        }
        for (AddonMachineContributor contributor : ServiceLoader.load(AddonMachineContributor.class, loader)) {
            try {
                var recipes = new ArrayList<RecipeMapSpec>();
                for (RecipeMapDeclaration declaration : contributor.recipeMaps())
                    recipes.add(spec(recipeTable(declaration)));
                var fuels = new ArrayList<RecipeMapSpec>();
                for (FuelMapDeclaration declaration : contributor.fuelMaps())
                    fuels.add(spec(fuelTable(declaration)));
                for (RecipeTable table : contributor.recipeTables())
                    (table.fuel() ? fuels : recipes).add(spec(table));
                var machines = new ArrayList<AddonMachineDefinition>();
                for (MachineDeclaration declaration : contributor.machines())
                    machines.add(definition(recipeMachine(declaration)));
                var behavior = new ArrayList<BehaviorMachine>();
                for (BehaviorMachineDeclaration declaration : contributor.behaviorMachines())
                    behavior.add(behaviorMachine(declaration));
                var generators = new ArrayList<FuelGenerator>();
                for (FuelGeneratorDeclaration declaration : contributor.fuelGenerators())
                    generators.add(fuelGenerator(declaration));
                var layouts = new ArrayList<StructureLayout>();
                for (MachineLayout layout : contributor.layouts()) layouts.add(structureLayout(layout));
                layouts.addAll(contributor.structureLayouts());
                var parts = new ArrayList<LayoutPart>();
                for (StructurePartDeclaration declaration : contributor.parts())
                    parts.add(layoutPart(declaration));
                parts.addAll(contributor.layoutParts());
                for (AddonMachine machine : contributor.addonMachines()) {
                    if (machine instanceof RecipeMachine recipe) machines.add(definition(recipe));
                    else if (machine instanceof BehaviorMachine behaviorMachine) behavior.add(behaviorMachine);
                    else generators.add((FuelGenerator) machine);
                }
                enqueue(contributor.addonId(), recipes, machines, layouts, behavior, fuels, generators, parts,
                        contributor.fluidPipes(), contributor.itemPipes(), contributor.compressedGasTanks());
            } catch (RuntimeException | Error failure) {
                throw new IllegalStateException("Addon machine contributor failed: " + safeMachineId(contributor)
                        + " (" + contributor.getClass().getName() + ")", failure);
            }
        }
    }

    private static boolean hasIoTarget(BasicMachineParameters parameters) {
        return parameters.itemOutputTarget() != null || parameters.fluidOutputTarget() != null
                || parameters.itemInputSource() != null || parameters.fluidInputSource() != null;
    }

    private static final int ALL_MASK = 0b111111;
    private static final List<BehaviorMachineDeclaration.Face> BEHAVIOR_FACES = List.of(
            BehaviorMachineDeclaration.Face.BOTTOM, BehaviorMachineDeclaration.Face.TOP,
            BehaviorMachineDeclaration.Face.LEFT, BehaviorMachineDeclaration.Face.RIGHT,
            BehaviorMachineDeclaration.Face.BACK);

    /** Old recipe declaration, with repeated faces removed. The resulting mask is unchanged. */
    public static RecipeMachine recipeMachine(MachineDeclaration declaration) {
        Objects.requireNonNull(declaration, "declaration");
        var casing = MachineCasing.builder(declaration.id(), declaration.material())
                .machineType(declaration.machineType()).tier(declaration.tier());
        var energy = EnergyInput.builder(declaration.energy(), declaration.energyPerTick());
        if (declaration.energyRangeSpecified())
            energy.range(declaration.energyInMin(), declaration.energyInMax());
        if (declaration.facesSpecified()) energy.faces(distinctFaces(declaration.energyInputs()));
        var builder = RecipeMachine.builder(casing.build()).energy(energy.build())
                .recipeMap(declaration.recipeMap()).parallel(declaration.parallel())
                .efficiency(declaration.efficiency()).constantEnergy(declaration.constantEnergy())
                .minimumLightAbove(declaration.minimumLightAbove())
                .searchOutputTanks(declaration.searchOutputTanks())
                .exclusiveOutputs(declaration.exclusiveOutputs())
                .structureKeys(declaration.structureKeys());
        if (declaration.kind() == MachineDeclaration.Kind.LARGE_RECIPE) builder.largeRecipe();
        if (declaration.cheapOverclockingSpecified())
            builder.cheapOverclocking(declaration.cheapOverclocking());
        if (declaration.facesSpecified() || hasTarget(declaration)) builder.io(io(declaration));
        return builder.build();
    }

    /** Old behavior declaration. The default five input faces stay unspecified. */
    public static BehaviorMachine behaviorMachine(BehaviorMachineDeclaration declaration) {
        Objects.requireNonNull(declaration, "declaration");
        var casing = MachineCasing.builder(declaration.id(), declaration.material())
                .machineType(declaration.machineType()).tier(declaration.tier());
        var energy = EnergyInput.builder(declaration.energy(), declaration.energyPerTick());
        if (!declaration.energyFaces().equals(BEHAVIOR_FACES))
            energy.faces(distinctFaces(declaration.energyFaces()));
        if (declaration.energyCapacity() != 0) energy.capacity(declaration.energyCapacity());
        var builder = BehaviorMachine.builder(casing.build()).energy(energy.build())
                .gui(declaration.gui()).guiSize(declaration.guiWidth(), declaration.guiHeight())
                .energyGauge(declaration.energyX(), declaration.energyY())
                .progress(declaration.progress().x(), declaration.progress().y())
                .maxRange(declaration.maxRange());
        if (!declaration.blockEntityId().isEmpty()) builder.blockEntityId(declaration.blockEntityId());
        if (declaration.emitsEnergy()) {
            builder.energyOutput(EnergyOutput.builder(declaration.energyOut(),
                            declaration.energyOutSize(), declaration.energyOutAmount())
                    .faces(faceArray(declaration.energyOutputFaces())).build());
        }
        for (BehaviorMachineDeclaration.ItemSlot item : declaration.items())
            builder.item(item.x(), item.y(), BehaviorMachine.SlotRole.valueOf(item.role().name()));
        for (BehaviorMachineDeclaration.FluidSlot fluid : declaration.fluids())
            builder.fluid(fluid.capacity(), fluid.x(), fluid.y(), BehaviorMachine.SlotRole.valueOf(fluid.role().name()));
        for (BehaviorMachineDeclaration.Toggle toggle : declaration.toggles())
            builder.toggle(toggle.translationKey(), toggle.x(), toggle.y(), toggle.initiallyOn());
        if (!declaration.recipeMaps().isEmpty())
            builder.recipeMaps(declaration.recipeMaps().toArray(String[]::new));
        return builder.build();
    }

    /** Old fuel declaration. A single block and a front output stay the defaults. */
    public static FuelGenerator fuelGenerator(FuelGeneratorDeclaration declaration) {
        Objects.requireNonNull(declaration, "declaration");
        var casing = MachineCasing.builder(declaration.id(), declaration.material())
                .machineType(declaration.machineType());
        var output = FuelOutput.builder(declaration.energy(), declaration.outputPerTick());
        switch (declaration.output().side()) {
            case FRONT -> output.front();
            case BACK -> output.back();
            case BEHIND -> output.behind(declaration.output().distance());
        }
        var builder = FuelGenerator.builder(casing.build()).output(output.build())
                .fuelMap(declaration.fuelMap()).efficiency(declaration.efficiency())
                .tank(declaration.tankCapacity());
        if (declaration.kind() == FuelGeneratorDeclaration.Kind.MULTI) builder.multi();
        return builder.build();
    }

    public static StructureLayout structureLayout(MachineLayout layout) {
        Objects.requireNonNull(layout, "layout");
        var builder = StructureLayout.builder(layout.machineName());
        for (MachineLayout.Cell cell : layout.cells())
            builder.cell(cell.right(), cell.up(), cell.back(), CellMatch.parse(cell.part()), cell.role());
        return builder.build();
    }

    public static LayoutPart layoutPart(StructurePartDeclaration declaration) {
        Objects.requireNonNull(declaration, "declaration");
        var builder = LayoutPart.builder(declaration.name(), declaration.material())
                .texture(declaration.texture()).hardness(declaration.hardness());
        StructurePartEnergy energy = declaration.energy();
        if (energy != null) {
            builder.energyOutput(EnergyOutput.builder(energy.type(), energy.maxSize(), energy.maxAmount())
                    .faces(faceArray(energy.outputFaces())).build(), energy.capacity());
        }
        return builder.build();
    }

    public static RecipeTable recipeTable(RecipeMapDeclaration declaration) {
        return table(RecipeTable.machine(declaration.name()), declaration.title(), declaration.gui(),
                declaration.inputItems(), declaration.outputItems(), declaration.minimalInputItems(),
                declaration.inputFluids(), declaration.outputFluids(), declaration.minimalInputFluids(),
                declaration.minimalInputs(), declaration.needsOutputs(), declaration.combinePower(),
                declaration.bucketIn(), declaration.bucketOut());
    }

    public static RecipeTable fuelTable(FuelMapDeclaration declaration) {
        return table(RecipeTable.fuel(declaration.name()), declaration.title(), declaration.gui(),
                declaration.inputItems(), declaration.outputItems(), declaration.minimalInputItems(),
                declaration.inputFluids(), declaration.outputFluids(), declaration.minimalInputFluids(),
                declaration.minimalInputs(), declaration.needsOutputs(), declaration.combinePower(),
                declaration.bucketIn(), declaration.bucketOut());
    }

    /** The one path from a recipe machine to the installed definition. */
    public static AddonMachineDefinition definition(RecipeMachine machine) {
        Objects.requireNonNull(machine, "machine");
        var casing = machine.casing();
        var input = machine.energy();
        var parameters = BasicMachineParameters.builder(casing.id(), GTMaterialRegistry.get(casing.material()))
                .machineType(casing.machineType())
                .energy(energy(input.type()), input.perTick())
                .tier(casing.tier())
                .parallel(machine.parallel())
                .faces(faces(machine))
                .minimumLightAbove(machine.minimumLightAbove())
                .searchOutputTanks(machine.searchOutputTanks())
                .exclusiveOutputs(machine.exclusiveOutputs());
        MachineIo io = machine.io();
        if (io != null) {
            parameters.itemOutputTarget(io.itemOutputTarget()).fluidOutputTarget(io.fluidOutputTarget())
                    .itemInputSource(io.itemInputSource()).fluidInputSource(io.fluidInputSource());
        }
        if (input.rangeSpecified()) parameters.energyRange(input.rangeMin(), input.rangeMax());
        else if (machine.largeRecipe()) parameters.omitEnergyRange();
        if (machine.cheapOverclockingSpecified()) parameters.cheapOverclocking(machine.cheapOverclocking());
        return new AddonMachineDefinition(parameters.build(), machine.recipeMap(),
                machine.largeRecipe() ? MachineControllerKind.LARGE_RECIPE : MachineControllerKind.BASIC,
                machine.efficiency(), machine.constantEnergy(), machine.structureKeys());
    }

    /** The one path from a recipe or fuel table to the installed spec. */
    public static RecipeMapSpec spec(RecipeTable table) {
        Objects.requireNonNull(table, "table");
        return new RecipeMapSpec(null, table.name(), table.title(), table.gui(),
                table.inputItems(), table.outputItems(), table.minimalInputItems(),
                table.inputFluids(), table.outputFluids(), table.minimalInputFluids(),
                table.minimalInputs(), table.needsOutputs(), table.combinePower(),
                table.bucketIn(), table.bucketOut());
    }

    private static RecipeTable table(RecipeTable.Builder builder, String title, String gui,
                                     int inputItems, int outputItems, int minimalInputItems,
                                     int inputFluids, int outputFluids, int minimalInputFluids, int minimalInputs,
                                     boolean needsOutputs, boolean combinePower, boolean bucketIn, boolean bucketOut) {
        return builder.title(title).gui(gui).inputItems(inputItems).outputItems(outputItems)
                .minimalInputItems(minimalInputItems).inputFluids(inputFluids).outputFluids(outputFluids)
                .minimalInputFluids(minimalInputFluids).minimalInputs(minimalInputs)
                .needsOutputs(needsOutputs).combinePower(combinePower).bucketIn(bucketIn).bucketOut(bucketOut)
                .build();
    }

    private static boolean hasTarget(MachineDeclaration declaration) {
        return declaration.itemOutputTarget() != null || declaration.fluidOutputTarget() != null
                || declaration.itemInputSource() != null || declaration.fluidInputSource() != null;
    }

    /**
     * Targets live on the io object. An old declaration can name targets without calling a face
     * method, and that still means six-side channels. Naming the six faces keeps that mask.
     */
    private static MachineIo io(MachineDeclaration declaration) {
        var io = MachineIo.builder();
        if (declaration.facesSpecified()) {
            io.itemInputs(faceArray(declaration.itemInputs()))
                    .itemOutputs(faceArray(declaration.itemOutputs()))
                    .fluidInputs(faceArray(declaration.fluidInputs()))
                    .fluidOutputs(faceArray(declaration.fluidOutputs()));
        } else {
            var all = MachineFace.all().toArray(MachineFace[]::new);
            io.itemInputs(all).itemOutputs(all).fluidInputs(all).fluidOutputs(all);
        }
        if (declaration.itemAutoInput() != null) io.itemAutoInput(face(declaration.itemAutoInput()));
        if (declaration.itemAutoOutput() != null) io.itemAutoOutput(face(declaration.itemAutoOutput()));
        if (declaration.fluidAutoInput() != null) io.fluidAutoInput(face(declaration.fluidAutoInput()));
        if (declaration.fluidAutoOutput() != null) io.fluidAutoOutput(face(declaration.fluidAutoOutput()));
        MachineDeclaration.Target itemOut = declaration.itemOutputTarget();
        MachineDeclaration.Target fluidOut = declaration.fluidOutputTarget();
        MachineDeclaration.Target itemIn = declaration.itemInputSource();
        MachineDeclaration.Target fluidIn = declaration.fluidInputSource();
        if (itemOut != null) io.itemOutputTarget(itemOut.right(), itemOut.up(), itemOut.back(), face(itemOut.receiver()));
        if (fluidOut != null) io.fluidOutputTarget(fluidOut.right(), fluidOut.up(), fluidOut.back(), face(fluidOut.receiver()));
        if (itemIn != null) io.itemInputSource(itemIn.right(), itemIn.up(), itemIn.back(), face(itemIn.receiver()));
        if (fluidIn != null) io.fluidInputSource(fluidIn.right(), fluidIn.up(), fluidIn.back(), face(fluidIn.receiver()));
        return io.build();
    }

    private static StructureLayout structureLayout(AddonMachineLayout layout) {
        var builder = StructureLayout.builder(layout.machineName());
        for (AddonMachineLayout.Cell cell : layout.cells())
            builder.cell(cell.right(), cell.up(), cell.back(), CellMatch.parse(cell.part()),
                    StructureRole.valueOf(cell.role().name()));
        return builder.build();
    }

    private static MachineFaceMasks faces(RecipeMachine machine) {
        MachineIo io = machine.io();
        EnergyInput energy = machine.energy();
        if (io == null && !energy.facesSpecified()) return MachineFaceMasks.ALL_SIDES;
        return new MachineFaceMasks(
                io == null ? ALL_MASK : mask(io.itemInputs()),
                io == null ? ALL_MASK : mask(io.itemOutputs()),
                io == null ? ALL_MASK : mask(io.fluidInputs()),
                io == null ? ALL_MASK : mask(io.fluidOutputs()),
                energy.facesSpecified() ? mask(energy.faces()) : ALL_MASK, 0,
                io == null ? MachineFaceMasks.AUTO_NONE : auto(io.itemAutoInput()),
                io == null ? MachineFaceMasks.AUTO_NONE : auto(io.itemAutoOutput()),
                io == null ? MachineFaceMasks.AUTO_NONE : auto(io.fluidAutoInput()),
                io == null ? MachineFaceMasks.AUTO_NONE : auto(io.fluidAutoOutput()));
    }

    private static MachineFace face(BehaviorMachineDeclaration.Face face) {
        return MachineFace.values()[face.ordinal()];
    }

    /** First occurrence wins. A repeated face does not change the mask. */
    private static List<MachineFace> distinctFaces(List<BehaviorMachineDeclaration.Face> faces) {
        var seen = new LinkedHashSet<MachineFace>();
        for (BehaviorMachineDeclaration.Face face : faces) seen.add(face(face));
        return List.copyOf(seen);
    }

    private static MachineFace[] faceArray(List<BehaviorMachineDeclaration.Face> faces) {
        return distinctFaces(faces).toArray(MachineFace[]::new);
    }

    private static int mask(List<MachineFace> faces) {
        int mask = 0;
        for (MachineFace face : faces) mask |= face.bit();
        return mask;
    }

    private static int auto(MachineFace face) {
        return face == null ? MachineFaceMasks.AUTO_NONE : face.index();
    }

    private static GregTechTags.Tag energy(String shortName) {
        return GregTechTags.Energy.ALL.stream().filter(tag -> tag.getShortName().equalsIgnoreCase(shortName))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown machine energy type: " + shortName));
    }

    private static String safeMachineId(AddonMachineContributor contributor) {
        try {
            String id = contributor.addonId();
            return id == null ? contributor.getClass().getName() : id;
        } catch (RuntimeException | Error ignored) {
            return contributor.getClass().getName();
        }
    }

    /** Direct queue used by tests and by {@link #collectContributors(ClassLoader)}. */
    public static synchronized void accept(String addonId, List<RecipeMapSpec> recipeMaps,
                                           List<AddonMachineDefinition> machines, List<AddonMachineLayout> layouts) {
        accept(addonId, recipeMaps, machines, layouts, List.of());
    }

    /** Direct queue used by tests and by {@link #collectContributors(ClassLoader)}. */
    public static synchronized void accept(String addonId, List<RecipeMapSpec> recipeMaps,
                                           List<AddonMachineDefinition> machines, List<AddonMachineLayout> layouts,
                                           List<BehaviorMachineDeclaration> behavior) {
        accept(addonId, recipeMaps, machines, layouts, behavior, List.of(), List.of());
    }

    /** Direct queue used by tests and by {@link #collectContributors(ClassLoader)}. */
    public static synchronized void accept(String addonId, List<RecipeMapSpec> recipeMaps,
                                           List<AddonMachineDefinition> machines, List<AddonMachineLayout> layouts,
                                           List<BehaviorMachineDeclaration> behavior, List<RecipeMapSpec> fuelMaps,
                                           List<FuelGeneratorDeclaration> generators) {
        accept(addonId, recipeMaps, machines, layouts, behavior, fuelMaps, generators, List.of());
    }

    /** Direct queue used by tests and by {@link #collectContributors(ClassLoader)}. */
    public static synchronized void accept(String addonId, List<RecipeMapSpec> recipeMaps,
                                           List<AddonMachineDefinition> machines, List<AddonMachineLayout> layouts,
                                           List<BehaviorMachineDeclaration> behavior, List<RecipeMapSpec> fuelMaps,
                                           List<FuelGeneratorDeclaration> generators,
                                           List<StructurePartDeclaration> parts) {
        accept(addonId, recipeMaps, machines, layouts, behavior, fuelMaps, generators, parts, List.of(), List.of());
    }

    /** Direct queue used by tests and by {@link #collectContributors(ClassLoader)}. */
    public static synchronized void accept(String addonId, List<RecipeMapSpec> recipeMaps,
                                           List<AddonMachineDefinition> machines, List<AddonMachineLayout> layouts,
                                           List<BehaviorMachineDeclaration> behavior, List<RecipeMapSpec> fuelMaps,
                                           List<FuelGeneratorDeclaration> generators,
                                           List<StructurePartDeclaration> parts,
                                           List<FluidPipeDeclaration> fluidPipes, List<ItemPipeDeclaration> itemPipes) {
        accept(addonId, recipeMaps, machines, layouts, behavior, fuelMaps, generators, parts,
                fluidPipes, itemPipes, List.of());
    }

    /** Direct queue used by tests and by {@link #collectContributors(ClassLoader)}. */
    public static synchronized void accept(String addonId, List<RecipeMapSpec> recipeMaps,
                                           List<AddonMachineDefinition> machines, List<AddonMachineLayout> layouts,
                                           List<BehaviorMachineDeclaration> behavior, List<RecipeMapSpec> fuelMaps,
                                           List<FuelGeneratorDeclaration> generators,
                                           List<StructurePartDeclaration> parts,
                                           List<FluidPipeDeclaration> fluidPipes, List<ItemPipeDeclaration> itemPipes,
                                           List<CompressedGasTankDeclaration> tanks) {
        var convertedLayouts = new ArrayList<StructureLayout>();
        for (AddonMachineLayout layout : Objects.requireNonNull(layouts, "layouts")) {
            for (AddonMachineLayout.Cell cell : layout.cells()) {
                if (MachineLayout.plainBlock(cell.part()) && cell.role() != StructureGrid.Role.CASING)
                    throw new IllegalStateException("Addon '" + addonId + "' layout '" + layout.machineName()
                            + "' plain block '" + cell.part() + "' requires the casing role");
            }
            convertedLayouts.add(structureLayout(layout));
        }
        var convertedBehavior = new ArrayList<BehaviorMachine>();
        for (BehaviorMachineDeclaration declaration : Objects.requireNonNull(behavior, "behaviorMachines"))
            convertedBehavior.add(behaviorMachine(declaration));
        var convertedGenerators = new ArrayList<FuelGenerator>();
        for (FuelGeneratorDeclaration declaration : Objects.requireNonNull(generators, "fuelGenerators"))
            convertedGenerators.add(fuelGenerator(declaration));
        var convertedParts = new ArrayList<LayoutPart>();
        for (StructurePartDeclaration declaration : Objects.requireNonNull(parts, "parts"))
            convertedParts.add(layoutPart(declaration));
        enqueue(addonId, recipeMaps, machines, convertedLayouts, convertedBehavior, fuelMaps, convertedGenerators,
                convertedParts, fluidPipes, itemPipes, tanks);
    }

    private static synchronized void enqueue(String addonId, List<RecipeMapSpec> recipeMaps,
                                             List<AddonMachineDefinition> machines, List<StructureLayout> layouts,
                                             List<BehaviorMachine> behavior, List<RecipeMapSpec> fuelMaps,
                                             List<FuelGenerator> generators, List<LayoutPart> parts,
                                             List<FluidPipeDeclaration> fluidPipes, List<ItemPipeDeclaration> itemPipes,
                                             List<CompressedGasTankDeclaration> tanks) {
        if (installed) throw new IllegalStateException("Addon machine queue is closed: " + addonId);
        if (addonId == null || !addonId.matches("[a-z][a-z0-9_]{1,63}"))
            throw new IllegalArgumentException("Invalid addon mod ID: " + addonId);
        if (addonId.equals("gt6ce") || addonId.equals("gregtech6") || addonId.equals("minecraft"))
            throw new IllegalArgumentException("Addon mod ID is reserved: " + addonId);
        if (QUEUE.containsKey(addonId))
            throw new IllegalStateException("Addon '" + addonId + "' already submitted machines");
        QUEUE.put(addonId, new Submission(addonId,
                List.copyOf(Objects.requireNonNull(recipeMaps, "recipeMaps")),
                List.copyOf(Objects.requireNonNull(machines, "machines")),
                List.copyOf(Objects.requireNonNull(layouts, "layouts")),
                List.copyOf(Objects.requireNonNull(behavior, "behaviorMachines")),
                List.copyOf(Objects.requireNonNull(fuelMaps, "fuelMaps")),
                List.copyOf(Objects.requireNonNull(generators, "fuelGenerators")),
                List.copyOf(Objects.requireNonNull(parts, "parts")),
                List.copyOf(Objects.requireNonNull(fluidPipes, "fluidPipes")),
                List.copyOf(Objects.requireNonNull(itemPipes, "itemPipes")),
                List.copyOf(Objects.requireNonNull(tanks, "compressedGasTanks"))));
    }

    /** Queued pipe rows, in addon-id order. Empty until a contributor submits pipes. */
    public static synchronized List<QueuedPipes> queuedPipes() {
        var rows = new ArrayList<QueuedPipes>();
        for (Submission submission : QUEUE.values())
            rows.add(new QueuedPipes(submission.addonId, submission.fluidPipes, submission.itemPipes));
        return List.copyOf(rows);
    }

    /** Names of the built-in machine recipe maps. Fuel maps are not machine maps. */
    public static Set<String> builtInRecipeMapNames() {
        var names = new HashSet<String>();
        for (Field field : MachineRecipeMapDefinitions.class.getFields()) {
            if (field.getType() != RecipeMapSpec.class) continue;
            try {
                names.add(((RecipeMapSpec) field.get(null)).mNameInternal);
            } catch (IllegalAccessException failure) {
                throw new IllegalStateException(failure);
            }
        }
        return Set.copyOf(names);
    }

    /** Names of the built-in fuel maps. Machine maps are not fuel maps. */
    public static Set<String> builtInFuelMapNames() {
        var names = new HashSet<String>();
        for (Field field : FuelRecipeMapDefinitions.class.getFields()) {
            if (field.getType() != RecipeMapSpec.class) continue;
            try {
                names.add(((RecipeMapSpec) field.get(null)).mNameInternal);
            } catch (IllegalAccessException failure) {
                throw new IllegalStateException(failure);
            }
        }
        return Set.copyOf(names);
    }

    /**
     * Validate the queue, append layouts, and freeze. {@code reservedMachineNames} is the built-in
     * machine-type set. {@code knownRecipeMaps} is the built-in recipe-map set.
     */
    public static synchronized void install(Set<String> reservedMachineNames, Set<String> knownRecipeMaps) {
        if (installed) return;
        PLAIN_BLOCKS.clear();
        collectContributors();
        AddonPipes.validate();
        Objects.requireNonNull(reservedMachineNames, "reservedMachineNames");
        Objects.requireNonNull(knownRecipeMaps, "knownRecipeMaps");
        var mapOwner = new LinkedHashMap<String, String>();
        for (String builtIn : knownRecipeMaps) mapOwner.put(builtIn, "");
        var acceptedMaps = new ArrayList<AcceptedRecipeMap>();
        for (Submission submission : QUEUE.values()) {
            for (RecipeMapSpec spec : submission.recipeMaps) {
                String name = spec.mNameInternal;
                if (name == null || !name.matches("[a-z0-9_.]+"))
                    throw new IllegalStateException("Addon '" + submission.addonId
                            + "' recipe map has an invalid name: " + name);
                if (knownRecipeMaps.contains(name))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' recipe map '" + name
                            + "' uses the same name as a built-in recipe map");
                String previous = mapOwner.putIfAbsent(name, submission.addonId);
                if (previous != null)
                    throw new IllegalStateException("Addon '" + submission.addonId + "' recipe map '" + name
                            + "' uses the same name as addon '" + previous + "'");
                acceptedMaps.add(new AcceptedRecipeMap(submission.addonId, spec));
            }
        }
        var fuelOwner = new LinkedHashMap<String, String>();
        for (String builtIn : builtInFuelMapNames()) fuelOwner.put(builtIn, "");
        var acceptedFuelMaps = new ArrayList<AcceptedRecipeMap>();
        for (Submission submission : QUEUE.values()) {
            for (RecipeMapSpec spec : submission.fuelMaps) {
                String name = spec.mNameInternal;
                if (name == null || !name.matches("[a-z0-9_.]+"))
                    throw new IllegalStateException("Addon '" + submission.addonId
                            + "' fuel map has an invalid name: " + name);
                if (mapOwner.containsKey(name) || fuelOwner.containsKey(name))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' fuel map '" + name
                            + "' uses a recipe or fuel map name that already exists");
                fuelOwner.put(name, submission.addonId);
                acceptedFuelMaps.add(new AcceptedRecipeMap(submission.addonId, spec));
            }
        }
        var builtParts = new ArrayList<StructurePart>();
        var partNames = new LinkedHashMap<String, StructurePart>();
        int nextPartId = 1_000_000;
        for (Submission submission : QUEUE.values()) {
            for (LayoutPart declaration : submission.parts) {
                String name = declaration.name();
                if (SharedLargeMachineParts.builtinId(name) >= 0)
                    throw new IllegalStateException("Addon '" + submission.addonId + "' structure part '" + name
                            + "' uses a built-in part name");
                StructurePart previous = partNames.get(name);
                if (previous != null)
                    throw new IllegalStateException("Addon '" + submission.addonId + "' structure part '" + name
                            + "' uses the same name as addon '" + previous.addonId() + "'");
                var material = GTMaterialRegistry.get(declaration.material());
                if (material == null || !material.isValid())
                    throw new IllegalStateException("Addon '" + submission.addonId + "' structure part '" + name
                            + "' uses unknown material '" + declaration.material() + "'");
                if (declaration.energy() != null) {
                    try {
                        energy(declaration.energy().type());
                    } catch (IllegalArgumentException failure) {
                        throw new IllegalStateException("Addon '" + submission.addonId + "' structure part '" + name
                                + "' uses unknown energy type '" + declaration.energy().type() + "'", failure);
                    }
                }
                if (nextPartId < 0)
                    throw new IllegalStateException("Addon structure part ids overflowed");
                var part = new StructurePart(submission.addonId, nextPartId, declaration);
                nextPartId++;
                partNames.put(name, part);
                builtParts.add(part);
            }
        }
        structureParts = List.copyOf(builtParts);
        partsByName = java.util.Map.copyOf(partNames);
        var preparedLayouts = new ArrayList<PreparedLayout>();
        var layoutNames = new HashSet<String>();
        for (Submission submission : QUEUE.values()) {
            for (StructureLayout layout : submission.layouts) {
                if (!layoutNames.add(layout.name()))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' layout '"
                            + layout.name() + "' uses the same name as another layout");
                if (reservedMachineNames.contains(layout.name()))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' layout '"
                            + layout.name() + "' uses a built-in machine name");
                preparedLayouts.add(new PreparedLayout(submission.addonId, layout.name(),
                        cells(submission.addonId, layout)));
            }
        }
        var machineOwner = new LinkedHashMap<String, String>();
        var machineSignature = new LinkedHashMap<String, Signature>();
        var idOwner = new LinkedHashMap<String, String>();
        var largeNames = new HashSet<String>();
        var acceptedMachines = new ArrayList<AcceptedMachine>();
        for (Submission submission : QUEUE.values()) {
            for (AddonMachineDefinition definition : submission.machines) {
                var parameters = definition.parameters();
                String machineName = parameters.machineName();
                if (!machineName.matches("[a-z0-9_]+"))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + parameters.id()
                            + "' has an invalid machine name: " + machineName);
                String previousId = idOwner.putIfAbsent(submission.addonId + ":" + parameters.id(), submission.addonId);
                if (previousId != null)
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine id '"
                            + parameters.id() + "' is already used");
                if (reservedMachineNames.contains(machineName))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' uses a built-in machine name");
                if (definition.kind() != MachineControllerKind.LARGE_RECIPE && hasIoTarget(parameters))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' IO targets require a large recipe machine");
                String previousOwner = machineOwner.putIfAbsent(machineName, submission.addonId);
                if (previousOwner != null && !previousOwner.equals(submission.addonId))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' uses the same machine name as addon '" + previousOwner + "'");
                if (parameters.energyTag() == GregTechTags.Energy.TU && parameters.energyRangeSpecified())
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' cannot declare an energy range for TU");
                var signature = new Signature(definition.recipeMapName(), definition.kind(), definition.efficiency(),
                        definition.constantEnergy(), definition.structureKeys(), parameters.faceConfig(),
                        parameters.cheapOverclocking(), parameters.cheapOverclockingSpecified(),
                        parameters.minimumLightAbove(), parameters.searchOutputTanks(), parameters.exclusiveOutputs(),
                        parameters.itemOutputTarget(), parameters.fluidOutputTarget(),
                        parameters.itemInputSource(), parameters.fluidInputSource(),
                        parameters.energyRangeSpecified(),
                        parameters.energyRangeSpecified() ? parameters.energyInMin() : 0,
                        parameters.energyRangeSpecified() ? parameters.energyInMax() : 0);
                Signature existing = machineSignature.putIfAbsent(machineName, signature);
                if (existing != null && !existing.equals(signature))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' disagrees with an earlier definition of that machine");
                if (!mapOwner.containsKey(definition.recipeMapName()))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' uses unknown recipe map '" + definition.recipeMapName() + "'");
                if (definition.kind() == MachineControllerKind.LARGE_RECIPE) largeNames.add(machineName);
                acceptedMachines.add(new AcceptedMachine(submission.addonId, definition));
            }
        }
        var acceptedGenerators = generators(machineOwner, machineSignature, idOwner, reservedMachineNames,
                fuelOwner, acceptedFuelMaps, largeNames);
        var behaviorNames = new HashSet<String>();
        for (Submission submission : QUEUE.values())
            for (BehaviorMachine declaration : submission.behavior)
                behaviorNames.add(declaration.casing().machineType());
        for (PreparedLayout layout : preparedLayouts) {
            if (!largeNames.contains(layout.machineName) && !behaviorNames.contains(layout.machineName))
                throw new IllegalStateException("Addon '" + layout.addonId + "' layout '"
                        + layout.machineName + "' has no controller");
        }
        for (AcceptedMachine machine : acceptedMachines) {
            String name = machine.definition().parameters().machineName();
            if (machine.definition().kind() == MachineControllerKind.LARGE_RECIPE && !layoutNames.contains(name))
                throw new IllegalStateException("Addon '" + machine.addonId() + "' machine '" + name + "' has no structure");
        }
        for (AcceptedFuel generator : acceptedGenerators) {
            String name = generator.declaration().casing().machineType();
            if (generator.declaration().multi() && !layoutNames.contains(name))
                throw new IllegalStateException("Addon '" + generator.addonId() + "' machine '" + name + "' has no structure");
        }
        var generatorNames = new HashSet<String>();
        for (AcceptedFuel generator : acceptedGenerators)
            generatorNames.add(generator.declaration().casing().machineType());
        var acceptedBehavior = behavior(machineOwner, machineSignature, idOwner, reservedMachineNames, generatorNames,
                mapOwner, fuelOwner);
        for (PreparedLayout layout : preparedLayouts)
            SharedLargeMachineLayouts.add(layout.machineName, layout.cells);
        SharedLargeMachineLayouts.freeze();
        recipeMaps = List.copyOf(acceptedMaps);
        fuelMaps = List.copyOf(acceptedFuelMaps);
        machines = List.copyOf(acceptedMachines);
        behaviorMachines = List.copyOf(acceptedBehavior);
        fuelGenerators = List.copyOf(acceptedGenerators);
        compressedGasTanks = gasTanks();
        installed = true;
    }

    public static List<PlainBlock> plainBlocks() { return List.copyOf(PLAIN_BLOCKS); }

    /** Unknown block ids fail here. Tags are left for the structure check, when their contents exist. */
    public static void requireKnownBlocks(Predicate<String> known) {
        for (PlainBlock block : PLAIN_BLOCKS) {
            if (block.name().startsWith("#")) continue;
            if (!known.test(block.name()))
                throw new IllegalStateException("Addon '" + block.addonId() + "' layout '" + block.layout()
                        + "' uses unknown block '" + block.name() + "'");
        }
    }

    public static List<AcceptedRecipeMap> recipeMaps() { return recipeMaps; }
    public static List<AcceptedRecipeMap> fuelMaps() { return fuelMaps; }
    public static List<AcceptedMachine> machines() { return machines; }
    public static List<AcceptedBehavior> behaviorMachines() { return behaviorMachines; }
    public static List<AcceptedFuel> fuelGenerators() { return fuelGenerators; }
    public static List<AcceptedCompressedGas> compressedGasTanks() { return compressedGasTanks; }
    public static List<StructurePart> structureParts() { return structureParts; }

    /** Runtime id of an addon structure part, or -1. */
    public static int partId(String name) {
        StructurePart part = partsByName.get(name);
        return part == null ? -1 : part.id();
    }

    public static boolean installed() { return installed; }

    private static List<AcceptedCompressedGas> gasTanks() {
        var accepted = new ArrayList<AcceptedCompressedGas>();
        for (Submission submission : QUEUE.values()) {
            var seen = new HashSet<String>();
            for (CompressedGasTankDeclaration declaration : submission.tanks) {
                if (!seen.add(declaration.id()))
                    throw new IllegalStateException("Addon '" + submission.addonId
                            + "' uses compressed gas tank '" + declaration.id() + "' twice");
                var material = GTMaterialRegistry.get(declaration.material());
                if (material == null || !material.isValid())
                    throw new IllegalStateException("Addon '" + submission.addonId + "' compressed gas tank '"
                            + declaration.id() + "' uses unknown material '" + declaration.material() + "'");
                accepted.add(new AcceptedCompressedGas(submission.addonId, declaration, material.getMeltingPoint()));
            }
        }
        return List.copyOf(accepted);
    }

    private static List<AcceptedFuel> generators(java.util.Map<String, String> machineOwner,
                                                  java.util.Map<String, Signature> machineSignature,
                                                  java.util.Map<String, String> idOwner,
                                                  Set<String> reservedMachineNames,
                                                  java.util.Map<String, String> fuelOwner,
                                                  List<AcceptedRecipeMap> acceptedFuelMaps,
                                                  Set<String> largeNames) {
        var signatures = new LinkedHashMap<String, FuelSignature>();
        var accepted = new ArrayList<AcceptedFuel>();
        for (Submission submission : QUEUE.values()) {
            for (FuelGenerator declaration : submission.generators) {
                String id = declaration.casing().id();
                String materialName = declaration.casing().material();
                var material = GTMaterialRegistry.get(materialName);
                if (material == null || !material.isValid())
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '"
                            + id + "' uses unknown material '" + materialName + "'");
                try {
                    energy(declaration.output().type());
                } catch (IllegalArgumentException failure) {
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '"
                            + id + "' uses unknown energy type '" + declaration.output().type() + "'", failure);
                }
                if (!fuelOwner.containsKey(declaration.fuelMap()))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '"
                            + declaration.casing().machineType() + "' uses unknown fuel map '" + declaration.fuelMap() + "'");
                if (fuelInputs(declaration.fuelMap(), acceptedFuelMaps) < 1)
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '"
                            + declaration.casing().machineType() + "' uses fuel map '" + declaration.fuelMap()
                            + "' which has no fluid input");
                String machineName = declaration.casing().machineType();
                String previousId = idOwner.putIfAbsent(submission.addonId + ":" + id, submission.addonId);
                if (previousId != null)
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine id '"
                            + id + "' is already used");
                if (reservedMachineNames.contains(machineName))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' uses a built-in machine name");
                String previousOwner = machineOwner.putIfAbsent(machineName, submission.addonId);
                if (previousOwner != null && !previousOwner.equals(submission.addonId))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' uses the same machine name as addon '" + previousOwner + "'");
                if (machineSignature.containsKey(machineName))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' disagrees with an earlier definition of that machine");
                var signature = new FuelSignature(declaration.fuelMap(), declaration.efficiency(),
                        declaration.output().type(), declaration.output().perTick(), declaration.tankCapacity(),
                        declaration.output(), declaration.multi());
                FuelSignature existing = signatures.putIfAbsent(machineName, signature);
                if (existing != null && !existing.equals(signature))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' disagrees with an earlier definition of that machine");
                if (declaration.multi()) largeNames.add(machineName);
                accepted.add(new AcceptedFuel(submission.addonId, declaration));
            }
        }
        return accepted;
    }

    private static int fuelInputs(String name, List<AcceptedRecipeMap> acceptedFuelMaps) {
        for (AcceptedRecipeMap map : acceptedFuelMaps)
            if (name.equals(map.spec().mNameInternal)) return map.spec().mInputFluidCount;
        for (Field field : FuelRecipeMapDefinitions.class.getFields()) {
            if (field.getType() != RecipeMapSpec.class) continue;
            try {
                RecipeMapSpec spec = (RecipeMapSpec) field.get(null);
                if (name.equals(spec.mNameInternal)) return spec.mInputFluidCount;
            } catch (IllegalAccessException failure) {
                throw new IllegalStateException(failure);
            }
        }
        return -1;
    }

    private static List<AcceptedBehavior> behavior(java.util.Map<String, String> machineOwner,
                                                   java.util.Map<String, Signature> machineSignature,
                                                   java.util.Map<String, String> idOwner,
                                                   Set<String> reservedMachineNames,
                                                   Set<String> generatorNames,
                                                   java.util.Map<String, String> mapOwner,
                                                   java.util.Map<String, String> fuelOwner) {
        var signatures = new LinkedHashMap<String, BehaviorSignature>();
        var accepted = new ArrayList<AcceptedBehavior>();
        var byPath = new LinkedHashMap<String, AcceptedBehavior>();
        for (Submission submission : QUEUE.values()) {
            for (BehaviorMachine declaration : submission.behavior) {
                String id = declaration.casing().id();
                String materialName = declaration.casing().material();
                var material = GTMaterialRegistry.get(materialName);
                if (material == null || !material.isValid())
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '"
                            + id + "' uses unknown material '" + materialName + "'");
                try {
                    energy(declaration.energy().type());
                } catch (IllegalArgumentException failure) {
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '"
                            + id + "' uses unknown energy type '" + declaration.energy().type() + "'", failure);
                }
                if (declaration.emitsEnergy()) {
                    try {
                        energy(declaration.output().type());
                    } catch (IllegalArgumentException failure) {
                        throw new IllegalStateException("Addon '" + submission.addonId + "' machine '"
                                + id + "' uses unknown energy output '" + declaration.output().type() + "'", failure);
                    }
                }
                for (String name : declaration.recipeMaps()) {
                    String recipeOwner = mapOwner.get(name);
                    String fuel = fuelOwner.get(name);
                    boolean ownRecipe = recipeOwner != null && (recipeOwner.isEmpty() || recipeOwner.equals(submission.addonId));
                    boolean ownFuel = fuel != null && (fuel.isEmpty() || fuel.equals(submission.addonId));
                    if (!ownRecipe && !ownFuel)
                        throw new IllegalStateException("Addon '" + submission.addonId + "' machine '"
                                + id + "' uses unknown recipe map '" + name + "'");
                }
                String machineName = declaration.casing().machineType();
                String previousId = idOwner.putIfAbsent(submission.addonId + ":" + id, submission.addonId);
                if (previousId != null)
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine id '"
                            + id + "' is already used");
                if (reservedMachineNames.contains(machineName))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' uses a built-in machine name");
                String previousOwner = machineOwner.putIfAbsent(machineName, submission.addonId);
                if (previousOwner != null && !previousOwner.equals(submission.addonId))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' uses the same machine name as addon '" + previousOwner + "'");
                if (machineSignature.containsKey(machineName) || generatorNames.contains(machineName))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' disagrees with an earlier definition of that machine");
                var signature = new BehaviorSignature(declaration.energy().type(),
                        declaration.output() == null ? "" : declaration.output().type(),
                        declaration.items().stream().map(BehaviorMachine.ItemSlot::role).toList(),
                        declaration.fluids().stream().map(BehaviorMachine.FluidSlot::role).toList(),
                        declaration.toggles().stream().map(BehaviorMachine.Toggle::translationKey).toList());
                BehaviorSignature existing = signatures.putIfAbsent(machineName, signature);
                if (existing != null && !existing.equals(signature))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' disagrees with an earlier definition of that machine");
                if (!BehaviorMachineFactories.ready())
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' has no behavior factory");
                if (!BehaviorMachineFactories.types(submission.addonId).contains(machineName))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' has no behavior factory");
                long min = BasicMachineParameters.defaultEnergyInMin(declaration.energy().type(), declaration.energy().perTick());
                long max = BasicMachineParameters.defaultEnergyInMax(declaration.energy().type(), declaration.energy().perTick());
                var row = new AcceptedBehavior(submission.addonId, declaration, min, max, declaration.blockEntityPath());
                AcceptedBehavior shared = byPath.putIfAbsent(submission.addonId + ":" + row.blockEntityPath(), row);
                if (shared != null && (!shared.declaration().casing().machineType().equals(machineName)
                        || !signatures.get(shared.declaration().casing().machineType()).equals(signature)))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' block entity '"
                            + row.blockEntityPath() + "' is shared by machines that disagree");
                accepted.add(row);
            }
        }
        return accepted;
    }

    private static List<SharedLargeMachineLayouts.Cell> cells(String addonId, StructureLayout layout) {
        var cells = new ArrayList<SharedLargeMachineLayouts.Cell>();
        for (LayoutCell cell : layout.cells()) {
            var role = StructureGrid.Role.valueOf(cell.role().name());
            CellMatch match = cell.match();
            if (match instanceof CellMatch.Block block) {
                PLAIN_BLOCKS.add(new PlainBlock(addonId, layout.name(), block.id()));
                cells.add(new SharedLargeMachineLayouts.Cell(cell.right(), cell.up(), cell.back(), 0,
                        role, block.id()));
            } else if (match instanceof CellMatch.Tag tag) {
                String name = "#" + tag.id();
                PLAIN_BLOCKS.add(new PlainBlock(addonId, layout.name(), name));
                cells.add(new SharedLargeMachineLayouts.Cell(cell.right(), cell.up(), cell.back(), 0,
                        role, name));
            } else {
                String name = match instanceof CellMatch.Air ? "air" : ((CellMatch.Part) match).name();
                int part = SharedLargeMachineParts.idForName(name);
                if (part < 0)
                    throw new IllegalStateException("Addon '" + addonId + "' layout '" + layout.name()
                            + "' uses unknown part '" + name + "'");
                cells.add(new SharedLargeMachineLayouts.Cell(cell.right(), cell.up(), cell.back(), part, role));
            }
        }
        return List.copyOf(cells);
    }

    private static String safeId(GregTechMachineContributor contributor) {
        try {
            String id = contributor.addonId();
            return id == null ? contributor.getClass().getName() : id;
        } catch (RuntimeException | Error ignored) {
            return contributor.getClass().getName();
        }
    }
}
