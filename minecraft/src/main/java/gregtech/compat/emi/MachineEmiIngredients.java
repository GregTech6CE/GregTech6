package gregtech.compat.emi;

import gregtech.engine.material.MaterialDisplayBinding;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

/** One cache per EMI reload; mutable amounts/chances are copied, alias lists are shared. */
final class MachineEmiIngredients {
    private final Map<Item, EmiIngredient> plain = new IdentityHashMap<>();
    EmiIngredient ingredient(gregtech.engine.recipe.Recipe recipe, int index) {
        var choices = recipe.viewerInputAlternatives(index);
        if (choices.isEmpty() && (recipe.mExactItemInputs || recipe.hasItemInputGuard(index))) return EmiStack.of(recipe.mInputs[index]);
        if (choices.isEmpty()) return ingredient(recipe.mInputs[index]);
        var expanded = new ArrayList<EmiStack>();
        for (var stack : choices) {
            if (!recipe.mExactItemInputs && !recipe.hasItemInputGuard(index)) {
                var preferred = gregtech.engine.material.MaterialUnification.canonical(stack);
                if (preferred.getItem() != stack.getItem()) expanded.add(EmiStack.of(preferred.copyWithCount(1)));
            }
            expanded.add(EmiStack.of(stack.copyWithCount(1)));
        }
        return new MachineEmiIngredient(expanded, recipe.mInputs[index].getCount());
    }
    EmiIngredient ingredient(ItemStack stack) {
//? if >=1.21.1 {
        /*if (!stack.getComponentsPatch().isEmpty()) return create(stack);
*///?} else {
        if (stack.hasTag()) return create(stack);
//?}
        return plain.computeIfAbsent(stack.getItem(), item -> create(new ItemStack(item)))
                .copy().setAmount(stack.getCount());
    }
    private static EmiIngredient create(ItemStack stack) {
        var aliases = MaterialDisplayBinding.alternatives(stack);
        var preferred = gregtech.engine.material.MaterialUnification.canonical(stack);
        boolean unified = preferred.getItem() != stack.getItem();
        if (aliases.isEmpty() && !unified) return EmiStack.of(stack);
        var alternatives = new ArrayList<EmiStack>(aliases.size() + 1);
        if (unified) alternatives.add(EmiStack.of(preferred.copyWithCount(1)));
        alternatives.add(EmiStack.of(stack.copyWithCount(1)));
        for (var alias : aliases) alternatives.add(EmiStack.of(alias.copyWithCount(1)));
        return new MachineEmiIngredient(alternatives, stack.getCount());
    }
}
