package gregtech.content.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.crafting.Ingredient;

/** A valid 3x3 example of the source's modulo-slot placement. No client dependencies. */
public final class FormConversionDisplay {
    private FormConversionDisplay() {}

    public static Optional<List<Ingredient>> layout(ToolShapelessRecipe recipe) {
        var selector = recipe.formSelector();
        if (selector.variants() <= 1
                || selector.offset() >= 9
                || recipe.getIngredients().size() != 1) return Optional.empty();
        var grid = new ArrayList<>(Collections.nCopies(9, Ingredient.EMPTY));
        grid.set(selector.offset(), recipe.getIngredients().get(0));
        return Optional.of(List.copyOf(grid));
    }
}
