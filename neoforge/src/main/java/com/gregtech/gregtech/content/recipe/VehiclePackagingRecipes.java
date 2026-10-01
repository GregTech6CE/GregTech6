package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.world.item.ItemStack;
import java.util.*;
/** GT6 RM.boxunbox minecarts; modern chest boats use the same empty-shell contract. */
public final class VehiclePackagingRecipes {
    private static final List<Recipe> RECIPES=new ArrayList<>();
    public static List<Recipe> recipes(){return Collections.unmodifiableList(RECIPES);}
    private VehiclePackagingRecipes(){}
    public static int register(){
        for(String content:new String[]{"chest","furnace","hopper","tnt"}) pair("minecart",content+"_minecart",content);
        for(String wood:new String[]{"oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry"}) pair(wood+"_boat",wood+"_chest_boat","chest");
        pair("bamboo_raft","bamboo_chest_raft","chest");
        return RECIPES.size();
    }
    private static void pair(String empty,String full,String content){
        ItemStack shell=OriginalRecipeBatch.item("minecraft:"+empty,1), product=OriginalRecipeBatch.item("minecraft:"+full,1), cargo=OriginalRecipeBatch.item("minecraft:"+content,1);
        var box=new Recipe(new ItemStack[]{cargo.copy(),shell.copy()},new ItemStack[]{product.copy()},null,null,null,null,16,16,0).withEmptyContainerInputs();
        var unbox=new Recipe(new ItemStack[]{product},new ItemStack[]{cargo,shell},null,null,null,null,16,16,0).withEmptyContainerInputs();
        if(MachineRecipeMaps.Boxinator.addRecipe(box)==null||MachineRecipeMaps.Unboxinator.addRecipe(unbox)==null)throw new IllegalStateException("Vehicle packing collision: "+full);
        RECIPES.add(box);RECIPES.add(unbox);
    }
}
