package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;

/** Loader_Recipes_Crops + RM.crop: original yields for crops available in this port. */
public final class CropProcessingRecipes {
    private int registered;
    public static int register() { var recipes=new CropProcessingRecipes(); recipes.addAll(); return recipes.registered; }
    private void addAll() {
        for(var row:CropProcessingCatalog.ROWS)crop(row.input(),row.fluid(),row.amount(),row.chance(),row.residue());
    }
    private void crop(String inputId,String fluidName,int amount,long chance,String residueId) {
        var input=BuiltInRegistries.ITEM.get(ResourceLocation.parse(inputId));
        var residue=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",residueId));
        var fluid=GTFluids.still(fluidName);
        if(input==null || input==net.minecraft.world.item.Items.AIR || residue==null || residue==net.minecraft.world.item.Items.AIR || fluid==null || !fluid.isBound()) throw new IllegalStateException("Unresolved crop recipe: "+inputId+" / "+fluidName);
        add(MachineRecipeMaps.Squeezer,new ItemStack(input),new ItemStack(residue),new FluidStack(fluid.get(),amount),chance-1000);
        int pressed=CropProcessingCatalog.pressed(amount);
        add(MachineRecipeMaps.Juicer,new ItemStack(input),new ItemStack(residue),new FluidStack(fluid.get(),pressed),chance);
        add(MachineRecipeMaps.Shredder,new ItemStack(input),new ItemStack(residue),FluidStack.EMPTY,chance);
        add(MachineRecipeMaps.Mortar,new ItemStack(input),new ItemStack(residue),FluidStack.EMPTY,chance/2);
    }
    private void add(RecipeMap map,ItemStack input,ItemStack output,FluidStack fluid,long chance) {
        if(map.addRecipe1(false,16,16,chance,input,FluidStack.EMPTY,fluid,output)!=null) registered++;
    }
}
