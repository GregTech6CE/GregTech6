package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Loader_Recipes_Crops + RM.crop: original yields for crops available in this port. */
public final class CropProcessingRecipes {
    private int registered;
    public static int register() { var recipes=new CropProcessingRecipes(); recipes.addAll(); return recipes.registered; }
    private void addAll() {
        crop("minecraft:apple", "Juice_Apple", 100, 7000, "fruit_remains");
        crop("gregtech:apple", "Juice_Apple", 100, 7000, "fruit_remains");
        crop("minecraft:melon_slice", "Juice_Melon", 250, 6000, "fruit_remains");
        crop("gregtech:blackberry", "Juice_Blackberry", 100, 4000, "fruit_remains");
        crop("gregtech:blueberry", "Juice_Blueberry", 100, 4000, "fruit_remains");
        crop("gregtech:raspberry", "Juice_Raspberry", 100, 4000, "fruit_remains");
        crop("gregtech:cranberry", "Juice_Cranberry", 100, 4000, "fruit_remains");
        crop("gregtech:gooseberry", "Juice_Gooseberry", 100, 5000, "fruit_remains");
        crop("gregtech:strawberry", "Juice_Strawberry", 100, 4000, "fruit_remains");
        crop("gregtech:lemon", "Juice_Lemon", 125, 7000, "fruit_remains");
        crop("gregtech:pomegranate", "Juice_Pomegranate", 100, 9000, "fruit_remains");
        crop("gregtech:banana", "Juice_Banana", 100, 8000, "fruit_remains");
        crop("gregtech:ananas", "Juice_Ananas", 200, 9000, "fruit_remains");
        crop("minecraft:beetroot", "Juice_Beet", 200, 7000, "vegetable_remains");
        crop("minecraft:carrot", "Juice_Carrot", 100, 7000, "vegetable_remains");
        crop("minecraft:pumpkin", "Juice_Pumpkin", 1000, 9000, "vegetable_remains");
        crop("gregtech:tomato", "Juice_Tomato", 100, 5000, "vegetable_remains");
        crop("gregtech:cucumber", "Juice_Cucumber", 150, 5000, "vegetable_remains");
        crop("gregtech:pickle", "Juice_Cucumber", 150, 5000, "vegetable_remains");
        crop("gregtech:onion", "Juice_Onion", 100, 7000, "vegetable_remains");
    }
    private void crop(String inputId,String fluidName,int amount,long chance,String residueId) {
        var input=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(inputId));
        var residue=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech",residueId));
        var fluid=GTFluids.still(fluidName);
        if(input==null || residue==null || fluid==null || !fluid.isPresent()) throw new IllegalStateException("Unresolved crop recipe: "+inputId+" / "+fluidName);
        add(MachineRecipeMaps.Squeezer,new ItemStack(input),new ItemStack(residue),new FluidStack(fluid.get(),amount),chance-1000);
        int pressed=amount-(amount<100?amount/3:1+amount/250)*25;
        add(MachineRecipeMaps.Juicer,new ItemStack(input),new ItemStack(residue),new FluidStack(fluid.get(),pressed),chance);
        add(MachineRecipeMaps.Shredder,new ItemStack(input),new ItemStack(residue),FluidStack.EMPTY,chance);
        add(MachineRecipeMaps.Mortar,new ItemStack(input),new ItemStack(residue),FluidStack.EMPTY,chance/2);
    }
    private void add(RecipeMap map,ItemStack input,ItemStack output,FluidStack fluid,long chance) {
        if(map.addRecipe1(false,16,16,chance,input,FluidStack.EMPTY,fluid,output)!=null) registered++;
    }
}
