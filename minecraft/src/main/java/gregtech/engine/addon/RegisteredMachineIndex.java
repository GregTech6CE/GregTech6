package gregtech.engine.addon;

import gregtech.content.data.MachineRecipeMaps;
import gregtech.engine.machine.BasicMachineSpec;
import gregtech.engine.recipe.RecipeMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Machine type to the recipe map stored on its spec. Unknown built-in names still use the legacy switch. */
public final class RegisteredMachineIndex {
    private static final Map<String, RecipeMap> BY_TYPE = new LinkedHashMap<>();

    private RegisteredMachineIndex() {}

    public static void bind(BasicMachineSpec spec) {
        RecipeMap previous = BY_TYPE.putIfAbsent(spec.machineName(), spec.recipeMap());
        if (previous != null && previous != spec.recipeMap())
            throw new IllegalStateException("Machine type '" + spec.machineName() + "' is bound to two recipe maps");
    }

    public static RecipeMap recipeMap(String machineName) {
        RecipeMap bound = BY_TYPE.get(machineName);
        return bound != null ? bound : MachineRecipeMaps.byMachineName(machineName);
    }
}
