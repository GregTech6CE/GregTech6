package gregtech.mixin;

import com.google.gson.JsonElement;
import gregtech.recipe.ConstructionRecipeMigration;
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
        recipes.values().forEach(ConstructionRecipeMigration::migrate);
    }
}
