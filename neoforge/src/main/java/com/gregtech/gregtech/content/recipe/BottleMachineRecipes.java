package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.item.BottleItem;
import com.gregtech.gregtech.data.generated.GTBottlesGen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** GT6 RecipeMapFluidCanner:50-72, materialized for both runtime machines and recipe viewers.
 * Every existing bottle consumes/returns its actual 250 mB fluid and empty container.
 */
public final class BottleMachineRecipes {
    private BottleMachineRecipes() {}
    private static final java.util.List<String> MISSING = new java.util.ArrayList<>();
    public static java.util.List<String> missing() { return java.util.List.copyOf(MISSING); }
    public static int register() {
        MISSING.clear();
        int count=0;
        for(int i=0;i<GTBottlesGen.ENTRIES.length;i+=3){
            String id=GTBottlesGen.ENTRIES[i];
            var item=com.gregtech.gregtech.content.food.GTFoodItems.itemOrNull(id);
            if(!(item instanceof BottleItem bottle)||bottle.fluid()==null){MISSING.add(id);continue;}
            var liquid=new FluidStack(bottle.fluid(),BottleItem.BOTTLE_AMOUNT);
            var filled=new ItemStack(item);
            var empty=BottleItem.emptyBottle();
            if(MachineRecipeMaps.Canner.addRecipe1(false,16,16,empty,liquid,FluidStack.EMPTY,filled)!=null)count++;
            if(MachineRecipeMaps.Canner.addRecipe1(false,16,16,filled,FluidStack.EMPTY,liquid,empty)!=null)count++;
        }
        com.mojang.logging.LogUtils.getLogger().info("GT6 bottle filling/draining: {} rows, unresolved {}",count,MISSING);
        return count;
    }
}
