package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.content.recipe.ImplosionRecipeRows.Gem;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.world.item.*;
import java.util.List;
/** GT6 Loader_Recipes_Other 708–758 and MT/ANY gem family declarations. */
public final class ImplosionRecipes {

    private static final List<Gem> GEMS = ImplosionRecipeRows.GEMS;
    public static int register() {
        int count=0;
        MaterialPrefix[] outputs={MaterialPrefix.plateGem,MaterialPrefix.gem,MaterialPrefix.gemFlawless,MaterialPrefix.gemExquisite};
        int[] dust={1,1,2,4},explosive={1,1,4,8};
        for(var family:GEMS) {
            var input=GTMaterialRegistry.get(family.input());var output=GTMaterialRegistry.get(family.output());
            if(!input.isValid()||!output.isValid()) continue;
            for(int mode=0;mode<4;mode++) for(var base:List.of(new ItemStack(Items.TNT,8),new ItemStack(GTToolBlocks.DYNAMITE.get(),2))) {
                var raw=GTItems.getStack(MaterialPrefix.dust,input,dust[mode]);var result=GTItems.getStack(outputs[mode],output);
                if(raw.isEmpty()||result.isEmpty()) continue;
                var explosiveStack=base.copyWithCount(base.getCount()*explosive[mode]);
                var recipe=new Recipe(new ItemStack[]{raw,explosiveStack,new ItemStack(GTTechnological.selectorTag(mode))},new ItemStack[]{result},null,null,null,null,256,0,0);
                if(MachineRecipeMaps.ImplosionCompressor.addRecipe(recipe,true,false,false)!=null) count++;
            }
        }
        return count;
    }
}
