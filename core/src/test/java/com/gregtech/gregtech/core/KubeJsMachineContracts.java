package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.addon.AddonMachine;
import com.gregtech.gregtech.api.addon.AddonMachines;
import com.gregtech.gregtech.api.addon.CellMatch;
import com.gregtech.gregtech.api.addon.EnergyInput;
import com.gregtech.gregtech.api.addon.EnergyOutput;
import com.gregtech.gregtech.api.addon.LayoutPart;
import com.gregtech.gregtech.api.addon.MachineCasing;
import com.gregtech.gregtech.api.addon.MachineFace;
import com.gregtech.gregtech.api.addon.RecipeMachine;
import com.gregtech.gregtech.api.addon.RecipeTable;
import com.gregtech.gregtech.api.addon.StructureLayout;
import com.gregtech.gregtech.api.addon.StructureRole;
import com.gregtech.gregtech.api.kubejs.KubeJsMachineDeclarations;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.content.multiblock.SharedLargeMachineLayouts;
import com.gregtech.gregtech.data.MaterialGroups;
import com.gregtech.gregtech.data.ModReferences;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Script-shaped machine declarations. Each scenario is its own JVM because install freezes the queue. */
public final class KubeJsMachineContracts {
    private static int assertions;

    private KubeJsMachineContracts() {}

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("Expected one scenario");
        Locale.setDefault(Locale.ROOT);
        switch (args[0]) {
            case "empty" -> empty();
            case "duplicate" -> duplicate();
            case "reserved" -> reserved();
            case "press" -> press();
            default -> throw new IllegalArgumentException("Unknown scenario: " + args[0]);
        }
        System.out.println(args[0] + " passed " + assertions);
    }

    private static void empty() {
        var declarations = KubeJsMachineDeclarations.empty();
        expectThrows(IllegalStateException.class, () -> declarations.addTable(table()), "closed");
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.acceptContributor("kubejs", List.of(), List.of(), List.of(), List.of());
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        check(AddonMachines.machines().stream().noneMatch(machine -> "kubejs".equals(machine.addonId())),
                "empty kubejs contributor installs no machine");
    }

    private static void duplicate() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.acceptContributor("kubejs", List.of(table()), List.of(pressMachine(), pressMachine()),
                List.of(layout()), List.of(casing(), emitter()));
        expectThrows(IllegalStateException.class, () -> AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames()),
                "already used");
    }

    private static void reserved() {
        prepare();
        GTMaterialRegistry.init();
        var machine = RecipeMachine.builder(MachineCasing.builder("script_press", "Steel").machineType("centrifuge").build())
                .energy(EnergyInput.builder("EU", 32).build())
                .recipeMap("kubejs.recipe.press").basic().build();
        AddonMachines.acceptContributor("kubejs", List.of(table()), List.of(machine), List.of(), List.of());
        expectThrows(IllegalStateException.class, () -> AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames()),
                "built-in machine name");
    }

    private static void press() {
        prepare();
        GTMaterialRegistry.init();
        AddonMachines.acceptContributor("kubejs", List.of(table()), List.of(pressMachine()),
                List.of(layout()), List.of(casing(), emitter()));
        AddonMachines.install(Set.of("centrifuge"), AddonMachines.builtInRecipeMapNames());
        check(AddonMachines.machines().stream().anyMatch(machine -> "kubejs".equals(machine.addonId())
                && "largepress".equals(machine.definition().parameters().machineName())), "script press is installed");
        var cells = SharedLargeMachineLayouts.cells("largepress");
        check(cells != null && cells.size() == 3, "script layout is installed");
        check(SharedLargeMachineLayouts.replaceable("largepress"), "script layout can be replaced");
        check(!SharedLargeMachineLayouts.replaceable("largecrusher"), "built-in layout stays fixed");
    }

    private static RecipeTable table() {
        return RecipeTable.machine("kubejs.recipe.press").title("KubeJS Press")
                .gui("gt6ce:textures/gui/machines/press")
                .inputItems(1).outputItems(1).minimalInputItems(1).minimalInputs(1).needsOutputs(true).build();
    }

    private static AddonMachine pressMachine() {
        return RecipeMachine.builder(MachineCasing.builder("largepress", "Steel").build())
                .energy(EnergyInput.builder("EU", 128).build())
                .recipeMap("kubejs.recipe.press").largeRecipe().build();
    }

    private static StructureLayout layout() {
        return StructureLayout.builder("largepress")
                .cell(1, 0, 0, CellMatch.part("press_casing"), StructureRole.CASING)
                .cell(0, 0, 1, CellMatch.part("tank_wall"), StructureRole.ENERGY_INPUT)
                .cell(-1, 0, 0, CellMatch.part("press_emitter"), StructureRole.ENERGY_OUTPUT)
                .build();
    }

    private static LayoutPart casing() {
        return LayoutPart.builder("press_casing", "Steel").hardness(6.0F).build();
    }

    private static LayoutPart emitter() {
        return LayoutPart.builder("press_emitter", "Steel").hardness(6.0F)
                .energyOutput(EnergyOutput.builder("EU", 32, 1).faces(MachineFace.FRONT).build(), 100_000L)
                .build();
    }

    private static void prepare() {
        ModData.bindPresence(id -> true);
        GTMaterialRegistry.setLogSink((warning, message) -> {});
        PrefixRegistry.ensurePrefixesLoaded();
        ModReferences.UNKNOWN.getClass();
        MaterialGroups.Glowstone.getClass();
    }

    private static void expectThrows(Class<? extends Throwable> expected, Runnable action, String... parts) {
        assertions++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (!expected.isInstance(failure)) throw new AssertionError(String.join(" ", parts), failure);
            String text = (failure.getMessage() == null ? "" : failure.getMessage()) + " "
                    + (failure.getCause() == null || failure.getCause().getMessage() == null ? "" : failure.getCause().getMessage());
            for (String part : parts) {
                if (!text.contains(part)) throw new AssertionError(String.join(" ", parts) + " but was: " + text, failure);
            }
            return;
        }
        throw new AssertionError(String.join(" ", parts));
    }

    private static void check(boolean valid, String message) {
        assertions++;
        if (!valid) throw new AssertionError(message);
    }
}
