package gregtech.engine.addon;

import gregtech.api.addon.machine.*;
import gregtech.api.addon.recipe.RecipeMachine;
import gregtech.api.multiblock.StructureLayout;
import java.util.*;

/** Normalizes addon declarations before registry or material lookup. */
public final class AddonMachineDeclarations {
    private AddonMachineDeclarations() {}

    public record Resolved(List<AddonMachine> machines, List<StructureLayout> layouts) {
        public Resolved {
            machines = List.copyOf(machines);
            layouts = List.copyOf(layouts);
        }
    }

    /**
     * Registry keys are controller machine types. A reusable template keeps its name when the host
     * registers a bound copy under the controller's machine type.
     */
    public static Resolved resolve(List<AddonMachine> declarations) {
        var layouts = new LinkedHashMap<String, StructureLayout>();
        var machines = new ArrayList<AddonMachine>();
        for (var declaration : declarations) {
            AddonMachine machine = declaration;
            if (declaration instanceof MultiblockMachine multiblock) {
                machine = multiblock.controller();
                var source = multiblock.structure();
                String name = machine.casing().machineType();
                var layout = new StructureLayout(name, source.cells(), source.repeats(), source.constraints());
                var previous = layouts.putIfAbsent(name, layout);
                if (previous != null && !previous.equals(layout))
                    throw new IllegalArgumentException("Conflicting structures for controller type " + name);
            }
            if (!(machine instanceof RecipeMachine
                    || machine instanceof BehaviorMachine
                    || machine instanceof FuelGenerator))
                throw new IllegalArgumentException(
                        "Unsupported machine declaration: " + machine.getClass().getName());
            machines.add(machine);
        }
        for (var machine : machines) {
            boolean required = machine instanceof RecipeMachine recipe && recipe.largeRecipe()
                    || machine instanceof FuelGenerator fuel && fuel.multi();
            if (required && !layouts.containsKey(machine.casing().machineType()))
                throw new IllegalArgumentException(
                        "Multiblock has no structure: " + machine.casing().id());
        }
        return new Resolved(machines, new ArrayList<>(layouts.values()));
    }
}
