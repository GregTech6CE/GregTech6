package com.gregtech.gregtech.emi;

import com.gregtech.gregtech.api.material.MaterialDisplayBinding;
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
    EmiIngredient ingredient(ItemStack stack) {
        if (stack.hasTag()) return create(stack);
        return plain.computeIfAbsent(stack.getItem(), item -> create(new ItemStack(item)))
                .copy().setAmount(stack.getCount());
    }
    private static EmiIngredient create(ItemStack stack) {
        var aliases = MaterialDisplayBinding.alternatives(stack);
        if (aliases.isEmpty()) return EmiStack.of(stack);
        var alternatives = new ArrayList<EmiStack>(aliases.size() + 1);
        alternatives.add(EmiStack.of(stack.copyWithCount(1)));
        for (var alias : aliases) alternatives.add(EmiStack.of(alias.copyWithCount(1)));
        return new MachineEmiIngredient(alternatives, stack.getCount());
    }
}
