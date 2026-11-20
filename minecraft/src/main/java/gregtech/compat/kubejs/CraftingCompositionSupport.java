package gregtech.compat.kubejs;

import dev.latvian.mods.kubejs.recipe.special.KubeJSCraftingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/** Loaded only for recipes with a registered KubeJS serializer. */
public final class CraftingCompositionSupport {
    private CraftingCompositionSupport() {}

    public static boolean hasFixedInputsAndOutput(CraftingRecipe recipe) {
        if (!(recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe)
                || !(recipe instanceof KubeJSCraftingRecipe scripted)
                || !scripted.kjs$getIngredientActions().isEmpty()) return false;
//? if >=1.21.1 {
        /*return scripted.kjs$getModifyResult().isEmpty();
*///?} else {
        return scripted.kjs$getModifyResult() == null;
//?}
    }
}
