package gregtech.compat.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import gregtech.content.recipe.FormConversionDisplay;
import gregtech.content.recipe.ToolShapelessRecipe;
import net.minecraft.resources.ResourceLocation;
import java.util.HashSet;
import java.util.List;

/** Preserve source selector positions in both the grid picture and EMI's standard crafting transfer. */
final class FormConversionEmiRecipe extends EmiCraftingRecipe {
    private FormConversionEmiRecipe(List<EmiIngredient> inputs, EmiStack output, ResourceLocation id) {
        super(inputs, output, id, false);
    }
    // Re-indexing a 3-wide grid into 2x2 changes the source leading-slot modulo.
    @Override public boolean canFit(int width, int height) { return width == 3 && height == 3; }
    static void register(EmiRegistry registry) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return;
        var replaced = new HashSet<ResourceLocation>();
        for (var entry : registry.getRecipeManager().getRecipes()) {
//? if >=1.21.1 {
            /*var recipe = entry.value();
            var id = entry.id();
*///?} else {
            var recipe = entry;
            var id = entry.getId();
//?}
            if (!(recipe instanceof ToolShapelessRecipe conversion)) continue;
            var layout = FormConversionDisplay.layout(conversion);
            if (layout.isEmpty()) continue;
            replaced.add(id);
            registry.addRecipe(new FormConversionEmiRecipe(layout.get().stream().map(EmiIngredient::of).toList(),
                    EmiStack.of(conversion.getResultItem(level.registryAccess())), id));
        }
        // Some EMI versions apply removals after registration; never remove our replacements.
        registry.removeRecipes(recipe -> replaced.contains(recipe.getId()) && !(recipe instanceof FormConversionEmiRecipe));
    }
}
