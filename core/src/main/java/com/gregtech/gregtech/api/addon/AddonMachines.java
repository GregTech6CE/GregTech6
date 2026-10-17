package com.gregtech.gregtech.api.addon;

import com.gregtech.gregtech.api.machine.MachineControllerKind;
import com.gregtech.gregtech.api.recipe.RecipeMapSpec;
import com.gregtech.gregtech.content.multiblock.SharedLargeMachineLayouts;
import com.gregtech.gregtech.content.multiblock.SharedLargeMachineParts;
import com.gregtech.gregtech.data.MachineRecipeMapDefinitions;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;

/** Queued addon recipe maps, machines and layouts. GregTech installs this once, after materials exist. */
public final class AddonMachines {
    private static final TreeMap<String, Submission> QUEUE = new TreeMap<>();
    private static boolean collected;
    private static boolean installed;
    private static List<AcceptedRecipeMap> recipeMaps = List.of();
    private static List<AcceptedMachine> machines = List.of();

    private AddonMachines() {}

    public record AcceptedRecipeMap(String addonId, RecipeMapSpec spec) {}
    public record AcceptedMachine(String addonId, AddonMachineDefinition definition) {}

    private record Submission(String addonId, List<RecipeMapSpec> recipeMaps,
                              List<AddonMachineDefinition> machines, List<AddonMachineLayout> layouts) {}
    private record Signature(String recipeMapName, MachineControllerKind kind, int efficiency,
                             boolean constantEnergy, List<String> structureKeys) {}
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
                accept(contributor.addonId(), contributor.recipeMaps(), contributor.machines(), contributor.layouts());
            } catch (RuntimeException | Error failure) {
                throw new IllegalStateException("Addon machine contributor failed: " + safeId(contributor)
                        + " (" + contributor.getClass().getName() + ")", failure);
            }
        }
    }

    /** Direct queue used by tests and by {@link #collectContributors(ClassLoader)}. */
    public static synchronized void accept(String addonId, List<RecipeMapSpec> recipeMaps,
                                           List<AddonMachineDefinition> machines, List<AddonMachineLayout> layouts) {
        if (installed) throw new IllegalStateException("Addon machine queue is closed: " + addonId);
        if (addonId == null || !addonId.matches("[a-z][a-z0-9_]{1,63}"))
            throw new IllegalArgumentException("Invalid addon mod ID: " + addonId);
        if (addonId.equals("gregtech") || addonId.equals("gregtech6") || addonId.equals("minecraft"))
            throw new IllegalArgumentException("Addon mod ID is reserved: " + addonId);
        if (QUEUE.containsKey(addonId))
            throw new IllegalStateException("Addon '" + addonId + "' already submitted machines");
        QUEUE.put(addonId, new Submission(addonId,
                List.copyOf(Objects.requireNonNull(recipeMaps, "recipeMaps")),
                List.copyOf(Objects.requireNonNull(machines, "machines")),
                List.copyOf(Objects.requireNonNull(layouts, "layouts"))));
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

    /**
     * Validate the queue, append layouts, and freeze. {@code reservedMachineNames} is the built-in
     * machine-type set. {@code knownRecipeMaps} is the built-in recipe-map set.
     */
    public static synchronized void install(Set<String> reservedMachineNames, Set<String> knownRecipeMaps) {
        if (installed) return;
        collectContributors();
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
        var preparedLayouts = new ArrayList<PreparedLayout>();
        var layoutNames = new HashSet<String>();
        for (Submission submission : QUEUE.values()) {
            for (AddonMachineLayout layout : submission.layouts) {
                if (!layoutNames.add(layout.machineName()))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' layout '"
                            + layout.machineName() + "' uses the same name as another layout");
                if (reservedMachineNames.contains(layout.machineName()))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' layout '"
                            + layout.machineName() + "' uses a built-in machine name");
                preparedLayouts.add(new PreparedLayout(submission.addonId, layout.machineName(),
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
                String previousOwner = machineOwner.putIfAbsent(machineName, submission.addonId);
                if (previousOwner != null && !previousOwner.equals(submission.addonId))
                    throw new IllegalStateException("Addon '" + submission.addonId + "' machine '" + machineName
                            + "' uses the same machine name as addon '" + previousOwner + "'");
                var signature = new Signature(definition.recipeMapName(), definition.kind(), definition.efficiency(),
                        definition.constantEnergy(), definition.structureKeys());
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
        for (PreparedLayout layout : preparedLayouts) {
            if (!largeNames.contains(layout.machineName))
                throw new IllegalStateException("Addon '" + layout.addonId + "' layout '"
                        + layout.machineName + "' has no controller");
        }
        for (AcceptedMachine machine : acceptedMachines) {
            String name = machine.definition().parameters().machineName();
            if (machine.definition().kind() == MachineControllerKind.LARGE_RECIPE && !layoutNames.contains(name))
                throw new IllegalStateException("Addon '" + machine.addonId() + "' machine '" + name + "' has no structure");
        }
        for (PreparedLayout layout : preparedLayouts)
            SharedLargeMachineLayouts.add(layout.machineName, layout.cells);
        SharedLargeMachineLayouts.freeze();
        recipeMaps = List.copyOf(acceptedMaps);
        machines = List.copyOf(acceptedMachines);
        installed = true;
    }

    public static List<AcceptedRecipeMap> recipeMaps() { return recipeMaps; }
    public static List<AcceptedMachine> machines() { return machines; }
    public static boolean installed() { return installed; }

    private static List<SharedLargeMachineLayouts.Cell> cells(String addonId, AddonMachineLayout layout) {
        var cells = new ArrayList<SharedLargeMachineLayouts.Cell>();
        var occupied = new HashSet<String>();
        for (AddonMachineLayout.Cell cell : layout.cells()) {
            if (cell.right() == 0 && cell.up() == 0 && cell.back() == 0)
                throw new IllegalStateException("Addon '" + addonId + "' layout '" + layout.machineName()
                        + "' places a part on the controller");
            String position = cell.right() + "," + cell.up() + "," + cell.back();
            if (!occupied.add(position))
                throw new IllegalStateException("Addon '" + addonId + "' layout '" + layout.machineName()
                        + "' repeats position " + position);
            int part = SharedLargeMachineParts.idForName(cell.part());
            if (part < 0)
                throw new IllegalStateException("Addon '" + addonId + "' layout '" + layout.machineName()
                        + "' uses unknown part '" + cell.part() + "'");
            cells.add(new SharedLargeMachineLayouts.Cell(cell.right(), cell.up(), cell.back(), part, cell.role()));
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
