package gregtech.compat.kubejs;

import dev.latvian.mods.kubejs.recipe.RecipeExceptionJS;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import gregtech.engine.recipe.data.MachineRecipeScript;

/** Writes ingredient JSON in the shape the machine-recipe parser accepts. */
public final class MachineRecipeJS extends RecipeJS {
    @Override
    public void afterLoaded() {
        try {
            MachineRecipeScript.requireKnownMap(getValue(MachineRecipeSchema.MAP));
        } catch (RuntimeException failure) {
            throw new RecipeExceptionJS(failure.getMessage());
        }
    }
}
