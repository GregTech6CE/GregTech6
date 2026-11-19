package gregtech.content.recipe;

import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.ItemMaterialRuntimeFacts;
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
                    || ItemMaterialRuntimeFacts.compositionDurability(input) == null
                    || !template.matchesItemInput(0, input)) continue;
            var composition = ItemMaterialRegistry.get(input).orElse(null);
            if (composition == null || composition.components().isEmpty()) return null;
            var plan = MaterialRecoveryRules.planShredderDamaged(
                    composition, MachineRecipeMaps.Shredder.itemOutputSlots, staticPrefix);
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
            var resolved = new Recipe(
                            new ItemStack[] {input.copyWithCount(1)},
                            outputs.toArray(ItemStack[]::new),
                            null,
                            null,
                            null,
                            plan.duration(),
                            template.energyPerTick,
                            template.specialValue)
                    .withMaterialRecovery()
                    .withPriority(template.priority)
                    .withItemInputGuard(
                            0,
                            offered -> offered.getItem() == input.getItem()
                                    && ItemMaterialRegistry.canRecover(offered)
                                    && ItemMaterialRegistry.get(offered)
                                            .filter(composition::equals)
                                            .isPresent());
            resolved.recoveryOwnership = template.recoveryOwnership;
            resolved.explicitCatalystsOnly = template.explicitCatalystsOnly;
            resolved.exactItemInputs = template.exactItemInputs;
            resolved.needsEmptyOutput = template.needsEmptyOutput;
            if (!template.source.isEmpty()) resolved.withSource(template.source);
            return resolved;
        }
        return template;
    }

    /**
     * Dynamic fallback for a unified input identity that no longer reaches its original static row.
     */
    static Recipe findDamaged(List<ItemStack> inputs) {
        for (ItemStack input : inputs) {
            if (input.isEmpty() || ItemMaterialRuntimeFacts.compositionDurability(input) == null) continue;
            var composition = ItemMaterialRegistry.get(input).orElse(null);
            if (composition == null || composition.components().isEmpty()) return null;
            MaterialPrefix prefix = composition.prefix();
            if (prefix != null && !RecoveryFormCatalog.shredder(PrefixRegistry.sourceName(prefix.getName()))) {
                return null;
            }
            // This provider is reached only after the static lookup missed; use the dynamic rule.
            boolean staticPrefix = false;
            var plan = MaterialRecoveryRules.planShredderDamaged(
                    composition, MachineRecipeMaps.Shredder.itemOutputSlots, staticPrefix);
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
            return new Recipe(
                            new ItemStack[] {input.copyWithCount(1)},
                            outputs.toArray(ItemStack[]::new),
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
                                    && ItemMaterialRegistry.canRecover(offered)
                                    && ItemMaterialRegistry.get(offered)
                                            .filter(composition::equals)
                                            .isPresent());
        }
        return null;
    }
}
