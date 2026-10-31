package gregtech.mixin;

import gregtech.engine.material.MaterialIngredientCandidates;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Deduplicate after tag expansion, retaining the original lazy ingredient and reload semantics. */
@Mixin(Ingredient.class)
public abstract class MaterialIngredientCandidatesMixin {
    @Unique
    private ItemStack[] gt6ce$lastCandidates;

    @Unique
    private long gt6ce$compositionRevision = -1;

    @Unique
    private ItemStack[] gt6ce$uniqueCandidates;

    @Inject(method = "getItems", at = @At("RETURN"), cancellable = true, require = 1)
    private void gt6ce$materialCandidates(CallbackInfoReturnable<ItemStack[]> result) {
        ItemStack[] candidates = result.getReturnValue();
        long revision = gregtech.engine.material.ItemMaterialRegistry.compositionRevision();
        if (candidates != gt6ce$lastCandidates || revision != gt6ce$compositionRevision) {
            gt6ce$compositionRevision = revision;
            gt6ce$lastCandidates = candidates;
            gt6ce$uniqueCandidates = MaterialIngredientCandidates.unique(candidates);
        }
        if (candidates != gt6ce$uniqueCandidates) result.setReturnValue(gt6ce$uniqueCandidates);
    }
}
