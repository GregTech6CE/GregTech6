package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.world.item.*;
import java.util.List;
/** GT6 Loader_Recipes_Other 708–758 and MT/ANY gem family declarations. */
public final class ImplosionRecipes {
    private record Gem(String input,String output) {}
    private static final List<Gem> GEMS=List.of(
            new Gem("Diamond","DiamondIndustrial"),
            new Gem("Blue Diamond","DiamondIndustrial"),
            new Gem("Green Diamond","DiamondIndustrial"),
            new Gem("Purple Diamond","DiamondIndustrial"),
            new Gem("Red Diamond","DiamondIndustrial"),
            new Gem("Yellow Diamond","DiamondIndustrial"),
            new Gem("Pink Diamond","DiamondIndustrial"),
            new Gem("DiamondIndustrial","DiamondIndustrial"),
            new Gem("Mana Diamond","DiamondIndustrial"),
            new Gem("Elven Dragonstone","DiamondIndustrial"),
            new Gem("Gravitite","DiamondIndustrial"),
            new Gem("Emerald","Emerald"),
            new Gem("Aquamarine","Emerald"),
            new Gem("Morganite","Emerald"),
            new Gem("Heliodor","Emerald"),
            new Gem("Goshenite","Emerald"),
            new Gem("Bixbite","Emerald"),
            new Gem("Maxixe","Emerald"),
            new Gem("Sapphire","Sapphire"),
            new Gem("Ruby","Sapphire"),
            new Gem("Blue Sapphire","Sapphire"),
            new Gem("Green Sapphire","Sapphire"),
            new Gem("Purple Sapphire","Sapphire"),
            new Gem("Yellow Sapphire","Sapphire"),
            new Gem("Orange Sapphire","Sapphire"),
            new Gem("Almandine","Almandine"),
            new Gem("Grossular","Grossular"),
            new Gem("Pyrope","Pyrope"),
            new Gem("Spessartine","Spessartine"),
            new Gem("Andradite","Andradite"),
            new Gem("Uvarovite","Uvarovite"),
            new Gem("Red Jasper","Red Jasper"),
            new Gem("Ocean Jasper","Ocean Jasper"),
            new Gem("Rainforest Jasper","Rainforest Jasper"),
            new Gem("Blue Jasper","Blue Jasper"),
            new Gem("Green Jasper","Green Jasper"),
            new Gem("Yellow Jasper","Yellow Jasper"),
            new Gem("Tiger Eye","Tiger Eye"),
            new Gem("Cat's Eye","Cat's Eye"),
            new Gem("Dragon Eye","Dragon Eye"),
            new Gem("Hawk's Eye","Hawk's Eye"),
            new Gem("Black Eye","Black Eye"),
            new Gem("Tiger Iron","Tiger Iron"),
            new Gem("Green Aventurine","Green Aventurine"),
            new Gem("Brown Aventurine","Brown Aventurine"),
            new Gem("Yellow Aventurine","Yellow Aventurine"),
            new Gem("Black Aventurine","Black Aventurine"),
            new Gem("Blue Aventurine","Blue Aventurine"),
            new Gem("Red Aventurine","Red Aventurine"),
            new Gem("Diamantine","DiamondIndustrial"),
            new Gem("Emeradic","Emerald"),
            new Gem("Amethyst","Amethyst"),
            new Gem("AmethystEnder","AmethystEnder"));
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
