package com.gregtech.gregtech.api.recipe;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Plans consumption on copies, including repeated ingredients and split stacks/tanks. */
public final class RecipeInputs {
    private RecipeInputs() {}
    public record Remaining(List<ItemStack> items, List<FluidStack> fluids) {}
    public static Remaining consume(Recipe recipe, List<ItemStack> items, List<FluidStack> fluids, int parallel) {
        if (parallel < 1) return null;
        var remainingItems = new ArrayList<ItemStack>();
        items.forEach(s -> remainingItems.add(s.copy()));
        var remainingFluids = new ArrayList<FluidStack>();
        fluids.forEach(s -> remainingFluids.add(s.copy()));
        // Reserve catalysts as well as consumables, then restore only the catalyst portion.
        // This prevents one item satisfying both a consumed input and a retained input.
        int[] retained = new int[remainingItems.size()];
        for (int inputIndex = 0; inputIndex < recipe.mInputs.length; inputIndex++) {
            var required = recipe.mInputs[inputIndex];
            if (required == null || required.isEmpty()) continue;
            boolean catalyst = recipe.isCatalystInput(inputIndex);
            long needed = (long)required.getCount() * (catalyst ? 1 : parallel);
            for (int slot = 0; slot < remainingItems.size(); slot++) {
                var available = remainingItems.get(slot);
                if (!matches(required, available)) continue;
                if (recipe.mExactItemInputs && !ItemStack.isSameItemSameComponents(required, available)) continue;
                if (recipe.mRequiresEmptyContainerInputs && com.gregtech.gregtech.api.material.ItemMaterialRegistry.hasStoredContents(available)) continue;
                if (recipe.mMaterialRecovery && (available.isDamaged() || !com.gregtech.gregtech.api.material.ItemMaterialRegistry.canRecover(available))) continue;
                int take = (int)Math.min(needed, available.getCount());
                available.shrink(take);
                if (catalyst) retained[slot] += take;
                needed -= take;
                if (needed == 0) break;
            }
            if (needed != 0) return null;
        }
        for (int slot = 0; slot < retained.length; slot++) if (retained[slot] > 0) {
            var remainder = items.get(slot).copy();
            remainder.setCount(remainingItems.get(slot).getCount() + retained[slot]);
            remainingItems.set(slot, remainder);
        }
        for (var required : recipe.mFluidInputs) {
            if (required == null || required.isEmpty()) continue;
            long needed = (long)required.getAmount() * parallel;
            for (var available : remainingFluids) {
                if (!FluidStack.isSameFluidSameComponents(available,required)) continue;
                int take = (int)Math.min(needed, available.getAmount());
                available.shrink(take); needed -= take;
                if (needed == 0) break;
            }
            if (needed != 0) return null;
        }
        return new Remaining(remainingItems, remainingFluids);
    }
    public static boolean matches(ItemStack required, ItemStack available) {
        if(available.isEmpty())return false;
        if(!required.is(available.getItem())) return com.gregtech.gregtech.api.material.MaterialEquivalence.matches(required,available);
        // An unfilled vessel in a recipe denotes its empty shell. Never silently
        // destroy stored fluid in recycling/crafting by treating absent NBT as a wildcard.
        if(required.getItem() instanceof com.gregtech.gregtech.api.inventory.ContainerShapeLike
                &&net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(required).isEmpty()
                &&net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(available).isPresent())return false;
        return required.getComponentsPatch().isEmpty() || ItemStack.isSameItemSameComponents(required, available);
    }
}
