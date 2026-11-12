package gregtech.content.recipe;

import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.engine.recipe.Recipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** GT6's damaged-item Shredder lookup: the recipe is rebuilt from the remaining composition. */
final class ShredderDamageScaling {
    private ShredderDamageScaling() {}

    static Recipe resolve(List<ItemStack> inputs, Recipe template, boolean staticPrefix) {
        for (ItemStack input : inputs) {
            if (input.isEmpty()
                    || !input.isDamageableItem()
                    || !input.isDamaged()
                    || !template.matchesItemInput(0, input)) continue;
            var composition = ItemMaterialRegistry.get(input).orElse(null);
            if (composition == null || composition.components().isEmpty()) return null;
            var plan = MaterialRecoveryRules.planShredderDamaged(
                    composition, MachineRecipeMaps.Shredder.mOutputItemsCount, staticPrefix);
            if (!plan.accepted()) return null;
            var outputs = new ArrayList<ItemStack>();
            for (var output : plan.outputs()) {
                for (var pile : output.dust().piles()) {
                    var stack = GTItems.getStack(pile.prefix(), output.material(), pile.count());
                    if (stack.isEmpty()) return null;
                    outputs.add(stack);
                }
            }
            if (outputs.isEmpty()) return null;
            int damage = input.getDamageValue();
            var resolved = new Recipe(
                            new ItemStack[] {input.copyWithCount(1)},
                            outputs.toArray(ItemStack[]::new),
                            template.mSpecialItems,
                            null,
                            null,
                            null,
                            plan.duration(),
                            template.mEUt,
                            template.mSpecialValue)
                    .withMaterialRecovery()
                    .withPriority(template.mPriority)
                    .withItemInputGuard(
                            0,
                            offered -> offered.getItem() == input.getItem()
                                    && offered.getDamageValue() == damage
                                    && ItemMaterialRegistry.canRecover(offered));
            resolved.mRecoveryOwnership = template.mRecoveryOwnership;
            resolved.mExplicitCatalystsOnly = template.mExplicitCatalystsOnly;
            resolved.mExactItemInputs = template.mExactItemInputs;
            resolved.mNeedsEmptyOutput = template.mNeedsEmptyOutput;
            if (!template.mSource.isEmpty()) resolved.withSource(template.mSource);
            return resolved;
        }
        return template;
    }

    /**
     * Dynamic fallback for a unified input identity that no longer reaches its original static row.
     */
    static Recipe findDamaged(List<ItemStack> inputs) {
        for (ItemStack input : inputs) {
            if (input.isEmpty() || !input.isDamageableItem() || !input.isDamaged()) continue;
            var composition = ItemMaterialRegistry.get(input).orElse(null);
            if (composition == null || composition.components().isEmpty()) return null;
            MaterialPrefix prefix = composition.prefix();
            if (prefix != null && !RecoveryFormCatalog.shredder(PrefixRegistry.sourceName(prefix.getName()))) {
                return null;
            }
            boolean staticPrefix = prefix != null;
            var plan = MaterialRecoveryRules.planShredderDamaged(
                    composition, MachineRecipeMaps.Shredder.mOutputItemsCount, staticPrefix);
            if (!plan.accepted()) return null;
            var outputs = new ArrayList<ItemStack>();
            for (var output : plan.outputs()) {
                for (var pile : output.dust().piles()) {
                    var stack = GTItems.getStack(pile.prefix(), output.material(), pile.count());
                    if (stack.isEmpty()) return null;
                    outputs.add(stack);
                }
            }
            if (outputs.isEmpty()) return null;
            int damage = input.getDamageValue();
            return new Recipe(
                            new ItemStack[] {input.copyWithCount(1)},
                            outputs.toArray(ItemStack[]::new),
                            null,
                            null,
                            null,
                            null,
                            plan.duration(),
                            16,
                            0)
                    .withMaterialRecovery()
                    .withRecoveryOwnership("shredder-damaged", Recipe.RecoveryOwnership.Scope.GENERIC)
                    .withSource("recovery:damaged-shredder")
                    .withItemInputGuard(
                            0,
                            offered -> offered.getItem() == input.getItem()
                                    && offered.getDamageValue() == damage
                                    && ItemMaterialRegistry.canRecover(offered));
        }
        return null;
    }
}
