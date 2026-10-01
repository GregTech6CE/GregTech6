package com.gregtech.gregtech.content.tool;

import net.minecraft.world.item.ItemStack;

/** Empty recipe entries represent GT6's explicit empty-workpiece marker. */
public final class AnvilWorkInputs {
    private AnvilWorkInputs() {}
    public static ItemStack[] consume(ItemStack[] work,ItemStack[] inputs) {
        int empty=0;
        for(var stack:work) if(stack.isEmpty()) empty++;
        var result=java.util.Arrays.stream(work).map(ItemStack::copy).toArray(ItemStack[]::new);
        for(var needed:inputs) {
            if(needed==null||needed.isEmpty()) {if(--empty<0) return null;continue;}
            int remaining=needed.getCount();
            for(var available:result) {
                if(!com.gregtech.gregtech.api.recipe.RecipeInputs.matches(needed,available)) continue;
                int take=Math.min(remaining,available.getCount());available.shrink(take);remaining-=take;
                if(remaining==0) break;
            }
            if(remaining>0) return null;
        }
        return result;
    }
}
