package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Welding recipes from GT6 Loader_MultiTileEntities, metalwall / metalwalldense registrations. */
public final class StructuralPartRecipes {
    private StructuralPartRecipes() {}
    public static int register() {
        int count=0;
        count+=wall(GTMultiblocks.TANK_WALL.get(),"StainlessSteel",false);
        count+=wall(GTMultiblocks.TANK_WALL_DENSE.get(),"StainlessSteel",true);
        count+=wall(GTMultiblocks.IMPLOSION_COMPRESSOR_WALL.get(),"TungstenSteel",true);
        // The original 18004 Tungsten Wall has the four-plate welder route. The older
        // lightning_rod_wall alias is crafted from it; registering both from the same
        // plates and selector makes the recipe map silently discard the original wall.
        count+=wall(LargeMachineParts.block(18008),"SteelGalvanized",false);
        count+=wall(LargeMachineParts.block(18031),"Lead",true);
        count+=wall(LargeMachineParts.block(18003),"TungstenSteel",false);
        count+=wall(LargeMachineParts.block(18004),"Tungsten",false);
        count+=wall(LargeMachineParts.block(18005),"Adamantium",false);
        count+=wall(LargeMachineParts.block(18006),"Titanium",false);
        count+=wall(LargeMachineParts.block(18007),"Invar",false);
        count+=wall(LargeMachineParts.block(18009),"Steel",false);
        count+=wall(LargeMachineParts.block(18026),"Titanium",true);
        count+=wall(LargeMachineParts.block(18024),"Tungsten",true);
        count+=wall(LargeMachineParts.block(18025),"Adamantium",true);
        count+=wall(LargeMachineParts.block(18027),"Invar",true);
        // MT.Ad has neither FURNACE nor SOFT. Original handler: 9 plates, 16 GU/t,
        // 256 ticks per material unit per (quality + 1), not the easy-workable 144 ticks.
        var adamantium=GTMaterialRegistry.get("Adamantium");
        var plates=GTItems.getStack(MaterialPrefix.plate,adamantium,9);
        var dense=GTItems.getStack(MaterialPrefix.plateDense,adamantium,1);
        if(!plates.isEmpty()&&!dense.isEmpty()) MachineRecipeMaps.Compressor.addRecipe1(false,16,
                9L*256*(adamantium.getToolQuality()+1),plates,dense);
        return count;
    }
    private static int wall(Block result,String material,boolean dense) {
        ItemStack plate=GTItems.getStack(dense?MaterialPrefix.plateDense:MaterialPrefix.plate,GTMaterialRegistry.get(material),4);
        if(plate.isEmpty())return 0;
        return MachineRecipeMaps.Welder.addRecipe(new Recipe(new ItemStack[]{plate,new ItemStack(GTTechnological.selectorTag(10))},
                new ItemStack[]{new ItemStack(result)},null,null,null,null,dense?512:256,dense?64:16,0),true,false,false)!=null?1:0;
    }
}
