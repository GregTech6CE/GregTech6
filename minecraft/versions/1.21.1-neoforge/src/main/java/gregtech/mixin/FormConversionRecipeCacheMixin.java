package gregtech.mixin;

import gregtech.content.recipe.CraftingMaterialForms;
import gregtech.content.recipe.OriginalFormConversions;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeCache;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** RecipeCache stores cropped stacks directly and does not consult CraftingInput.equals. */
@Mixin(RecipeCache.class)
public abstract class FormConversionRecipeCacheMixin {
    @Unique
    private static final Set<String> gregtech$positionedForms = OriginalFormConversions.FIXED.stream()
            .filter(OriginalFormConversions.Conversion::single)
            .map(OriginalFormConversions.Conversion::input)
            .collect(Collectors.toUnmodifiableSet());

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void gregtech$matchPositionedForm(
            Level level, CraftingInput input, CallbackInfoReturnable<Optional<RecipeHolder<CraftingRecipe>>> callback) {
        if (input.ingredientCount() != 1) return;
        var form = CraftingMaterialForms.form(input.getItem(0));
        if (form != null && gregtech$positionedForms.contains(form.prefix())) {
            // A cached hit (including an empty result) is invalid after moving this item.
            callback.setReturnValue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level));
        }
    }
}
