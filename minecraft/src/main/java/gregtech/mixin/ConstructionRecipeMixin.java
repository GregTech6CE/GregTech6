package gregtech.mixin;

import com.google.gson.JsonElement;
import gregtech.content.recipe.ConstructionRecipeMigration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

@Mixin(RecipeManager.class)
public abstract class ConstructionRecipeMixin {
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"), require = 1)
    private void gt6ce$constructionRecipes(Map<ResourceLocation, JsonElement> recipes, ResourceManager resources,
                                         ProfilerFiller profiler, CallbackInfo callback) {
        gregtech.content.recipe.ConfiguredRecipeRules.apply(recipes, resources);
        recipes.values().forEach(ConstructionRecipeMigration::migrate);
    }

    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "replaceRecipes", at = @At("HEAD"), argsOnly = true, require = 1)
    private Iterable<?> gt6ce$disabledRuntimeRecipes(Iterable<?> recipes) {
        var kept = new java.util.ArrayList<Object>();
        for (Object entry : recipes) {
//? if >=1.21.1 {
            /*var id = ((net.minecraft.world.item.crafting.RecipeHolder<?>) entry).id();
*///?} else {
            var id = ((net.minecraft.world.item.crafting.Recipe<?>) entry).getId();
//?}
            if (gregtech.GregTechConfig.recipeEnabled(id.toString())) kept.add(entry);
        }
        return kept;
    }
}
