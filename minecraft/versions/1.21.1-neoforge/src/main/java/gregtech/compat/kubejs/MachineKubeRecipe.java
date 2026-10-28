package gregtech.compat.kubejs;

import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.component.RecipeValidationContext;
import gregtech.engine.recipe.data.MachineRecipeScript;

/** Checks the map name while the script is loading, before the datapack overlay runs. */
public final class MachineKubeRecipe extends KubeRecipe {
    @Override
    public void validate(RecipeValidationContext cx) {
        try {
            MachineRecipeScript.requireKnownMap(getValue(MachineRecipeSchema.MAP));
        } catch (RuntimeException failure) {
            throw new KubeRuntimeException(failure.getMessage()).source(sourceLine);
        }
    }
}
