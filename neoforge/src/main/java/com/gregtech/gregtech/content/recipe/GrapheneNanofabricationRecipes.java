package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.content.recipe.GrapheneNanofabricationRecipeRows.Entry;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Exact carbon quantities, selector modes and times from GT6 Loader_Recipes_Other Nanofab rows. */
public final class GrapheneNanofabricationRecipes {

    private static final Entry[] DEFINITIONS = GrapheneNanofabricationRecipeRows.definitions();
    private static final List<Recipe> RECIPES=new ArrayList<>();
    private GrapheneNanofabricationRecipes() {}
    public static List<Recipe> recipes(){return Collections.unmodifiableList(RECIPES);}
    public static int register() {
        if(!RECIPES.isEmpty())throw new IllegalStateException("Nanofab recipes registered twice");
        var graphene=GTMaterialRegistry.get("Graphene");var carbon=GTMaterialRegistry.get("Carbon");
        var wire=GTWires.allWires().stream().map(r->r.get()).filter(b->b.spec().id().equals("graphene")&&b.spec().size()==1).findFirst().orElseThrow();
        for(var def:DEFINITIONS) {
            var input=GTItems.getStack(def.input(),carbon,def.inputCount());
            var output=def.output()==null?new ItemStack(wire,def.outputCount()):GTItems.getStack(def.output(),graphene,def.outputCount());
            if(input.isEmpty()||output.isEmpty())throw new IllegalStateException("Unresolved graphene recipe: "+def);
            var recipe=new Recipe(new ItemStack[]{new ItemStack(GTTechnological.selectorTag(def.selector())),input},
                    new ItemStack[]{output},null,null,null,null,def.ticks(),16,0);
            if(MachineRecipeMaps.Nanofab.addRecipe(recipe)==null)throw new IllegalStateException("Nanofab recipe collision: "+def);
            RECIPES.add(recipe);
        }
        return RECIPES.size();
    }
}
