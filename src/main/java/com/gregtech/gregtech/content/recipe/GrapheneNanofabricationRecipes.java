package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Exact carbon quantities, selector modes and times from GT6 Loader_Recipes_Other Nanofab rows. */
public final class GrapheneNanofabricationRecipes {
    private record Entry(int ticks,int selector,MaterialPrefix input,int inputCount,MaterialPrefix output,int outputCount) {}
    private static final Entry[] DEFINITIONS={
        new Entry(64,0,MaterialPrefix.dustDiv72,18,MaterialPrefix.foil,1),
        new Entry(256,0,MaterialPrefix.dustTiny,9,MaterialPrefix.foil,4),
        new Entry(64,0,MaterialPrefix.dustSmall,1,MaterialPrefix.foil,1),
        new Entry(256,0,MaterialPrefix.dust,1,MaterialPrefix.foil,4),
        new Entry(256,1,MaterialPrefix.dustTiny,9,MaterialPrefix.plate,1),
        new Entry(256,1,MaterialPrefix.dustSmall,4,MaterialPrefix.plate,1),
        new Entry(256,1,MaterialPrefix.dust,1,MaterialPrefix.plate,1),
        new Entry(256,2,MaterialPrefix.dustTiny,9,MaterialPrefix.plateCurved,1),
        new Entry(256,2,MaterialPrefix.dustSmall,4,MaterialPrefix.plateCurved,1),
        new Entry(256,2,MaterialPrefix.dust,1,MaterialPrefix.plateCurved,1),
        new Entry(128,3,MaterialPrefix.dustDiv72,36,MaterialPrefix.itemCasing,1),
        new Entry(256,3,MaterialPrefix.dustTiny,9,MaterialPrefix.itemCasing,2),
        new Entry(128,3,MaterialPrefix.dustSmall,2,MaterialPrefix.itemCasing,1),
        new Entry(256,3,MaterialPrefix.dust,1,MaterialPrefix.itemCasing,2),
        new Entry(32,4,MaterialPrefix.dustDiv72,9,MaterialPrefix.wireFine,1),
        new Entry(256,4,MaterialPrefix.dustTiny,9,MaterialPrefix.wireFine,8),
        new Entry(64,4,MaterialPrefix.dustSmall,1,MaterialPrefix.wireFine,2),
        new Entry(256,4,MaterialPrefix.dust,1,MaterialPrefix.wireFine,8),
        new Entry(128,5,MaterialPrefix.dustDiv72,36,null,1),
        new Entry(256,5,MaterialPrefix.dustTiny,9,null,2),
        new Entry(128,5,MaterialPrefix.dustSmall,2,null,1),
        new Entry(256,5,MaterialPrefix.dust,1,null,2),
        new Entry(32,6,MaterialPrefix.dustDiv72,9,MaterialPrefix.bolt,1),
        new Entry(256,6,MaterialPrefix.dustTiny,9,MaterialPrefix.bolt,8),
        new Entry(64,6,MaterialPrefix.dustSmall,1,MaterialPrefix.bolt,2),
        new Entry(256,6,MaterialPrefix.dust,1,MaterialPrefix.bolt,8),
        new Entry(128,7,MaterialPrefix.dustDiv72,36,MaterialPrefix.stick,1),
        new Entry(256,7,MaterialPrefix.dustTiny,9,MaterialPrefix.stick,2),
        new Entry(128,7,MaterialPrefix.dustSmall,2,MaterialPrefix.stick,1),
        new Entry(256,7,MaterialPrefix.dust,1,MaterialPrefix.stick,2),
        new Entry(256,8,MaterialPrefix.dustTiny,9,MaterialPrefix.stickLong,1),
        new Entry(256,8,MaterialPrefix.dustSmall,4,MaterialPrefix.stickLong,1),
        new Entry(256,8,MaterialPrefix.dust,1,MaterialPrefix.stickLong,1),
        new Entry(64,9,MaterialPrefix.dustDiv72,18,MaterialPrefix.ring,1),
        new Entry(256,9,MaterialPrefix.dustTiny,9,MaterialPrefix.ring,4),
        new Entry(64,9,MaterialPrefix.dustSmall,1,MaterialPrefix.ring,1),
        new Entry(256,9,MaterialPrefix.dust,1,MaterialPrefix.ring,4),
        new Entry(256,10,MaterialPrefix.dustTiny,9,MaterialPrefix.gearGtSmall,1),
        new Entry(256,10,MaterialPrefix.dustSmall,4,MaterialPrefix.gearGtSmall,1),
        new Entry(256,10,MaterialPrefix.dust,1,MaterialPrefix.gearGtSmall,1),
        new Entry(1024,11,MaterialPrefix.dustTiny,36,MaterialPrefix.gearGt,1),
        new Entry(1024,11,MaterialPrefix.dustSmall,16,MaterialPrefix.gearGt,1),
        new Entry(1024,11,MaterialPrefix.dust,4,MaterialPrefix.gearGt,1),
        new Entry(1088,12,MaterialPrefix.dustSmall,17,MaterialPrefix.rotor,1),
        new Entry(4352,12,MaterialPrefix.dust,17,MaterialPrefix.rotor,4),
    };
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
