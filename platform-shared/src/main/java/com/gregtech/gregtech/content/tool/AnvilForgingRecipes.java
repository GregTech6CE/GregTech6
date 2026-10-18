package com.gregtech.gregtech.content.tool;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import static com.gregtech.gregtech.data.MaterialPrefix.*;

/** Loader_Recipes_Handlers 175–199, restricted by the original self-forging material declarations. */
public final class AnvilForgingRecipes {
    public static int register() {
        int count=0;
        for(var material:GTMaterialRegistry.sortedMaterials()) {
            if(material.resolve()!=material || !AnvilMaterialCatalog.SELF_FORGING.contains(material.getName())) continue;
            if(!material.isValid() || !material.has(MaterialProperty.SMITHABLE) || material.has(MaterialProperty.FLAMMABLE) || material.has(MaterialProperty.ANTIMATTER)) continue;
            for(var rule:AnvilForgingCatalog.RULES) {
                var first=GTItems.getStack(rule.first(),material);
                var second=rule.second()==null?ItemStack.EMPTY:GTItems.getStack(rule.second(),material);
                var out=GTItems.getStack(rule.output(),material,rule.outputCount());
                var scrap=rule.scrap()==null?ItemStack.EMPTY:GTItems.getStack(rule.scrap(),material,rule.scrapCount());
                if(first.isEmpty()||out.isEmpty()||(rule.second()!=null&&second.isEmpty())||(rule.scrap()!=null&&scrap.isEmpty())) continue;
                long units=rule.first().getMaterialWeight()+(rule.second()==null?0:rule.second().getMaterialWeight());
                var recipe=new Recipe(new ItemStack[]{first,second},scrap.isEmpty()?new ItemStack[]{out}:new ItemStack[]{out,scrap},null,
                        scrap.isEmpty()?null:new long[]{10000,9000},null,null,AnvilRecipeDefinitions.duration(units,material.getToolQuality()),64,0);
                if(MachineRecipeMaps.Anvil.addRecipe(recipe,true,false,material.has(MaterialProperty.HIDDEN))!=null) count++;
            }
        }
        return count;
    }
}
