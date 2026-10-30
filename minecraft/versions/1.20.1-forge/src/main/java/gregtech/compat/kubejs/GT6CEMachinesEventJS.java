package gregtech.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import gregtech.api.addon.machine.AddonMachine;
import gregtech.api.addon.machine.FuelGenerator;
import gregtech.api.addon.machine.FuelOutput;
import gregtech.api.addon.machine.LayoutPart;
import gregtech.api.addon.recipe.RecipeMachine;
import gregtech.api.addon.recipe.RecipeTable;
import gregtech.api.energy.EnergyInput;
import gregtech.api.energy.EnergyOutput;
import gregtech.api.energy.EnergyType;
import gregtech.api.machine.MachineCasing;
import gregtech.api.machine.MachineFace;
import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.LayoutCell;
import gregtech.api.multiblock.StructureLayout;
import gregtech.api.multiblock.StructureRole;
import gregtech.engine.kubejs.KubeJsMachineDeclarations;
import java.util.Locale;

/** Builders available inside {@code GT6CEStartupEvents.machines}. */
public final class GT6CEMachinesEventJS extends EventJS {
    private final KubeJsMachineDeclarations declarations;

    public GT6CEMachinesEventJS(KubeJsMachineDeclarations declarations) {
        this.declarations = declarations;
    }

    public RecipeTable.Builder recipeTable(String name) {
        return RecipeTable.machine(name);
    }

    public RecipeTable.Builder fuelTable(String name) {
        return RecipeTable.fuel(name);
    }

    public void addTable(RecipeTable table) {
        declarations.addTable(table);
    }

    public MachineCasing.Builder casing(String id, String material) {
        return MachineCasing.builder(id, material);
    }

    public RecipeMachine.Builder recipeMachine(String id, String material) {
        return RecipeMachine.builder(MachineCasing.builder(id, material).build());
    }

    public FuelGenerator.Builder fuelMachine(String id, String material) {
        return FuelGenerator.builder(MachineCasing.builder(id, material).build());
    }

    public void addMachine(AddonMachine machine) {
        declarations.addMachine(machine);
    }

    public StructureLayout.Builder layout(String name) {
        return StructureLayout.builder(name);
    }

    public void addLayout(StructureLayout layout) {
        declarations.addLayout(layout);
    }

    public LayoutPart.Builder part(String name, String material) {
        return LayoutPart.builder(name, material);
    }

    public void addPart(LayoutPart part) {
        declarations.addPart(part);
    }

    public EnergyInput.Builder energy(String type, long perTick) {
        return EnergyInput.builder(EnergyType.of(type), perTick);
    }

    public EnergyOutput.Builder energyOutput(String type, long maxSize, long maxAmount) {
        return EnergyOutput.builder(EnergyType.of(type), maxSize, maxAmount);
    }

    public FuelOutput.Builder fuelOutput(String type, long perTick) {
        return FuelOutput.builder(EnergyType.of(type), perTick);
    }

    public MachineFace face(String name) {
        return MachineFace.valueOf(name.trim().toUpperCase(Locale.ROOT));
    }

    public StructureRole role(String name) {
        return StructureRole.valueOf(name.trim().toUpperCase(Locale.ROOT));
    }

    public CellMatch match(String text) {
        return CellMatch.parse(text);
    }

    public LayoutCell cell(int right, int up, int back, Object match, Object role) {
        return LayoutCell.at(right, up, back, match(match), role(role));
    }

    private static CellMatch match(Object value) {
        if (value instanceof CellMatch parsed) return parsed;
        if (value instanceof java.util.List<?> list) {
            if (list.isEmpty()) throw new IllegalArgumentException("Missing structure part");
            var members = new java.util.ArrayList<CellMatch>();
            for (Object element : list) members.add(match(element));
            return members.size() == 1 ? members.get(0) : CellMatch.oneOf(members);
        }
        return CellMatch.parse(String.valueOf(value));
    }

    private static StructureRole role(Object value) {
        if (value instanceof StructureRole parsed) return parsed;
        return StructureRole.valueOf(String.valueOf(value).trim().toUpperCase(Locale.ROOT));
    }
}
