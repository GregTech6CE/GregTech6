package gregtech.mixin;

import gregtech.content.recipe.CraftingMaterialForms;
import java.util.List;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Polymorph caches the matching rows of a cropped 1x1 CraftingInput without the cell the item
 * came from, so a position-sensitive form conversion keeps the first cell's output after the
 * item moves. Optional integration: applied only while Polymorph is installed.
 */
@Pseudo
@Mixin(targets = "com.illusivesoulworks.polymorph.common.capability.RecipeCache", remap = false)
public abstract class PolymorphFormConversionCacheMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true, require = 0)
    private void gregtech$matchPositionedForm(
            Level level,
            RecipeType<?> type,
            RecipeInput input,
            CallbackInfoReturnable<List<RecipeHolder<?>>> callback) {
        if (!(input instanceof CraftingInput crafting) || crafting.ingredientCount() != 1) return;
        if (!CraftingMaterialForms.positionSensitive(crafting.getItem(0))) return;
        callback.setReturnValue(uncached(level, type, crafting));
    }

    /** The same lookup Polymorph performs on a cache miss, minus the entry it would insert. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<RecipeHolder<?>> uncached(Level level, RecipeType<?> type, CraftingInput input) {
        return (List) level.getRecipeManager().getRecipesFor((RecipeType) type, input, level);
    }
}
