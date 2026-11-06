package gregtech.content.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;

/** Removes the vanilla shaped rail rows that the GT track family replaces; part of the publication pass. */
public final class TrackRecipeReplacement {
    private TrackRecipeReplacement() {}

    public static void removeConflicts(RecipeManager manager, RegistryAccess access) {
        var replaced = java.util.Set.of(Items.RAIL, Items.POWERED_RAIL, Items.DETECTOR_RAIL, Items.ACTIVATOR_RAIL);
        var rows = manager.getRecipes();
        var kept = rows.stream()
                .filter(holder -> holder.id().getNamespace().equals("gt6ce")
                        || !(holder.id().getNamespace().equals("minecraft")
                                        ? gregtech.GregTechConfig.Rule.REPLACE_VANILLA_RECIPES
                                        : gregtech.GregTechConfig.Rule.REPLACE_MOD_RECIPES)
                                .enabled()
                        || !(holder.value() instanceof ShapedRecipe)
                        || !replaced.contains(
                                holder.value().getResultItem(access).getItem()))
                .toList();
        if (kept.size() != rows.size()) {
            manager.replaceRecipes(kept);
            com.mojang.logging.LogUtils.getLogger()
                    .info("[gregtech] Replaced {} conflicting shaped rail recipes", rows.size() - kept.size());
        }
    }
}
