package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.generated.GeneratedRecipeResolver;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialFactories;
import gregtech.engine.recipe.Recipe;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** {@code Loader_Recipes_Other.java:202-212}: Dynamite from a wood-family dust and Glyceryl.
 * The source registers one Mixer row per {@code ANY.Wood} member at each dust size; Bark and Paper
 * are registered separately on the same handler and are not wood-group members.
 */
public final class DynamiteMixerRecipes {
    private static final List<Recipe> REGISTERED = new ArrayList<>();

    private DynamiteMixerRecipes() {}

    public static List<Recipe> recipes() {
        return List.copyOf(REGISTERED);
    }

    public static int register() {
        if (!REGISTERED.isEmpty()) throw new IllegalStateException("Dynamite mixing recipes registered twice");
        GTMaterial dynamite = GTMaterialRegistry.get("Dynamite").resolve();
        Set<GTMaterial> sources = new LinkedHashSet<>(MaterialFactories.woods());
        sources.add(GTMaterialRegistry.get("Bark").resolve());
        sources.add(GTMaterialRegistry.get("Paper").resolve());
        for (GTMaterial source : sources) {
            add(MaterialPrefix.dustTiny, source, dynamite, 112, 8);
            add(MaterialPrefix.dustSmall, source, dynamite, 250, 8);
            add(MaterialPrefix.dust, source, dynamite, 1000, 32);
        }
        return REGISTERED.size();
    }

    /** Ratios and durations are the dumped source values: U/9, U/4 and U Glyceryl per dust size. */
    private static void add(MaterialPrefix prefix, GTMaterial source, GTMaterial dynamite, int glyceryl, long ticks) {
        ItemStack input = GTItems.getStack(prefix, source, 1);
        ItemStack output = GTItems.getStack(prefix, dynamite, 2);
        if (input.isEmpty() || output.isEmpty()) return;
        FluidStack fluid = GeneratedRecipeResolver.materialFluid("Glyceryl", glyceryl);
        if (fluid == null || fluid.isEmpty()) throw new IllegalStateException("Missing Glyceryl fluid");
        Recipe recipe = new Recipe(
                new ItemStack[] {input}, new ItemStack[] {output}, null, null,
                new FluidStack[] {fluid}, null, ticks, 16, 0);
        if (MachineRecipeMaps.Mixer.addRecipe(recipe) != recipe)
            throw new IllegalStateException("Conflicting Dynamite mixing recipe for " + source.getName());
        REGISTERED.add(recipe);
    }
}
