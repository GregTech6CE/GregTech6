package gregtech.content.recipe;

import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.catalog.SourceLensMaterialFacts.Color;
import gregtech.content.registry.GTItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Native lens holders selected from material-owned lens roles. */
public final class SourceLensRecipeItems {
    private SourceLensRecipeItems() {}

    public static List<ItemStack> lenses(Color color) {
        var result = new ArrayList<ItemStack>();
        for (var material : SourceLensRoles.materials(color)) {
            var lens = GTItems.getStack(MaterialPrefix.lens, material, 1);
            if (!lens.isEmpty()) result.add(lens);
        }
        return List.copyOf(result);
    }
}
