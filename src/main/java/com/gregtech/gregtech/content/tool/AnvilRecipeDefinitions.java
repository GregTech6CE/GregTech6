package com.gregtech.gregtech.content.tool;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import static com.gregtech.gregtech.data.MaterialPrefix.*;

/** Original GT6 anvil handlers whose material conditions are already represented by this port. */
public final class AnvilRecipeDefinitions {
    public record Rule(RecipeMap map,MaterialPrefix input,int amount,MaterialPrefix second,int secondAmount,
                       MaterialPrefix output,int outputAmount,MaterialPrefix byproduct,int byproductAmount,boolean smithable) {}
    public static final List<Rule> RULES=List.of(
            split(gemLegendary,gemExquisite,2),split(gemExquisite,gemFlawless,2),split(gemFlawless,gem,2),
            split(gem,gemFlawed,2),split(gemFlawed,gemChipped,2),split(gemChipped,dustSmall,1),
            new Rule(MachineRecipeMaps.Anvil,ring,2,ring,2,chain,1,null,0,false),
            bend(MachineRecipeMaps.AnvilBendBig,plate,plateCurved,null,0),
            bend(MachineRecipeMaps.AnvilBendBig,stick,springSmall,scrapGt,2),
            bend(MachineRecipeMaps.AnvilBendBig,stickLong,spring,null,0),
            bend(MachineRecipeMaps.AnvilBendSmall,stick,ring,scrapGt,2));
    private AnvilRecipeDefinitions() {}
    private static Rule split(MaterialPrefix in,MaterialPrefix out,int amount) {
        return new Rule(MachineRecipeMaps.Anvil,in,1,null,0,out,amount,null,0,false);
    }
    private static Rule bend(RecipeMap map,MaterialPrefix in,MaterialPrefix out,MaterialPrefix by,int amount) {
        return new Rule(map,in,1,null,0,out,1,by,amount,true);
    }
    /** Original prefix-handler cost: ceiling(material units * 64 * (quality+1) / U). */
    public static long duration(long units,int quality){return AnvilRecipeCost.duration(units,quality);}
    public static int register() {
        int count=0;
        for(var material:GTMaterialRegistry.sortedMaterials()) {
            if(!material.isValid()||material.resolve()!=material||material.has(MaterialProperty.ANTIMATTER)) continue;
            for(var rule:RULES) {
                if(rule.smithable()&&!material.has(MaterialProperty.SMITHABLE)) continue;
                var in=GTItems.getStack(rule.input(),material,rule.amount());
                var out=GTItems.getStack(rule.output(),material,rule.outputAmount());
                if(in.isEmpty()||out.isEmpty()) continue;
                var second=rule.second()==null?ItemStack.EMPTY:GTItems.getStack(rule.second(),material,rule.secondAmount());
                var by=rule.byproduct()==null?ItemStack.EMPTY:GTItems.getStack(rule.byproduct(),material,rule.byproductAmount());
                if((rule.second()!=null&&second.isEmpty())||(rule.byproduct()!=null&&by.isEmpty())) continue;
                long inputUnits=rule.input().getMaterialWeight()*rule.amount()+(rule.second()==null?0:rule.second().getMaterialWeight()*rule.secondAmount());
                long outputUnits=rule.output().getMaterialWeight()*rule.outputAmount()+(rule.byproduct()==null?0:rule.byproduct().getMaterialWeight()*rule.byproductAmount());
                var recipe=new Recipe(new ItemStack[]{in,second},by.isEmpty()?new ItemStack[]{out}:new ItemStack[]{out,by},
                        null,by.isEmpty()?new long[]{10000}:new long[]{10000,9000},null,null,
                        duration(Math.max(inputUnits,outputUnits),material.getToolQuality()),64,0);
                recipe.mHidden=material.has(MaterialProperty.HIDDEN);
                if(rule.map().addRecipe(recipe,true,false,recipe.mHidden)!=null) count++;
            }
        }
        return count + AnvilForgingRecipes.register();
    }
}
