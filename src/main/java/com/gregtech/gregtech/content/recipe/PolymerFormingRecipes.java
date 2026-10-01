package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.content.recipe.PolymerFormingRecipeRows.Form;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** Original latex coagulation and low-heat EXTRUDER_SIMPLE routes for Rubber and Plastic. */
public final class PolymerFormingRecipes {

    private static final Form[] FORMS = PolymerFormingRecipeRows.forms();
    private static final List<Recipe> RECIPES=new ArrayList<>();
    private PolymerFormingRecipes() {}
    public static List<Recipe> recipes(){return Collections.unmodifiableList(RECIPES);}
    public static int register() {
        if(!RECIPES.isEmpty())throw new IllegalStateException("Polymer recipes registered twice");
        var latex=GTFluids.still("Latex");
        if(latex==null||!latex.isPresent())throw new IllegalStateException("Missing Latex fluid");
        add(MachineRecipeMaps.Coagulator,new Recipe(null,new ItemStack[]{mat(MaterialPrefix.nugget,"Rubber",1)},null,null,
                new FluidStack[]{new FluidStack(latex.get(),16)},null,256,0,0));
        for(String material:new String[]{"Rubber","Plastic"})for(String mold:new String[]{"extruder_shape_","low_heat_extruder_shape_"})
            for(MaterialPrefix input:new MaterialPrefix[]{MaterialPrefix.dust,MaterialPrefix.ingot})for(var form:FORMS) {
                if(input==form.prefix())continue;
                var shape=GTTechnological.get(mold+form.shape());
                if(shape==null)throw new IllegalStateException("Missing mold "+mold+form.shape());
                add(MachineRecipeMaps.Extruder,new Recipe(new ItemStack[]{mat(input,material,form.units()),new ItemStack(shape)},
                        new ItemStack[]{mat(form.prefix(),material,form.count())},null,null,null,null,64L*form.units(),16,0));
            }
        return RECIPES.size();
    }
    private static ItemStack mat(MaterialPrefix prefix,String material,int amount) {
        var stack=GTItems.getStack(prefix,GTMaterialRegistry.get(material),amount);
        if(stack.isEmpty())throw new IllegalStateException("Missing polymer form "+prefix.getName()+"/"+material);
        return stack;
    }
    private static void add(RecipeMap map,Recipe recipe) {
        if(map.addRecipe(recipe)!=null){RECIPES.add(recipe);return;}
        var in=recipe.mInputs.length>0?recipe.mInputs[0]:ItemStack.EMPTY;
        var mold=recipe.mInputs.length>1?recipe.mInputs[1]:ItemStack.EMPTY;
        var out=recipe.mOutputs.length>0?recipe.mOutputs[0]:ItemStack.EMPTY;
        var collision=map.findCollision(recipe);
        throw new IllegalStateException("Polymer recipe conflict in "+map.mNameInternal+": "+in+" + "+mold+" -> "+out
                +" collides with "+(collision==null?"nothing (validation failed)"
                :Arrays.toString(collision.mInputs)+" -> "+Arrays.toString(collision.mOutputs)));
    }
}
