package gregtech.engine.kubejs;

import gregtech.api.addon.machine.AddonMachine;
import gregtech.api.addon.machine.LayoutPart;
import gregtech.api.addon.recipe.RecipeTable;
import gregtech.api.multiblock.StructureLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Machines, layouts and parts collected from one KubeJS startup event. */
public final class KubeJsMachineDeclarations {
    public static final String ADDON_ID = "kubejs";
    private static final KubeJsMachineDeclarations EMPTY = new KubeJsMachineDeclarations(true);

    private final List<RecipeTable> tables = new ArrayList<>();
    private final List<AddonMachine> machines = new ArrayList<>();
    private final List<StructureLayout> layouts = new ArrayList<>();
    private final List<LayoutPart> parts = new ArrayList<>();
    private boolean sealed;

    public KubeJsMachineDeclarations() {
        this(false);
    }

    private KubeJsMachineDeclarations(boolean sealed) {
        this.sealed = sealed;
    }

    public static KubeJsMachineDeclarations empty() {
        return EMPTY;
    }

    public void addTable(RecipeTable table) {
        open();
        tables.add(Objects.requireNonNull(table, "table"));
    }

    public void addMachine(AddonMachine machine) {
        open();
        machines.add(Objects.requireNonNull(machine, "machine"));
    }

    public void addLayout(StructureLayout layout) {
        open();
        layouts.add(Objects.requireNonNull(layout, "layout"));
    }

    public void addPart(LayoutPart part) {
        open();
        parts.add(Objects.requireNonNull(part, "part"));
    }

    public void seal() {
        sealed = true;
    }

    public boolean sealed() {
        return sealed;
    }

    public List<RecipeTable> tables() {
        return List.copyOf(tables);
    }

    public List<AddonMachine> machines() {
        return List.copyOf(machines);
    }

    public List<StructureLayout> layouts() {
        return List.copyOf(layouts);
    }

    public List<LayoutPart> parts() {
        return List.copyOf(parts);
    }

    private void open() {
        if (sealed) throw new IllegalStateException("KubeJS machine declarations are closed");
    }
}
