package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.api.fluid.PortableFluidContainerSpec;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** capcellcon is a 1/9 U placeable vessel, not a second flat material item.
 * The forty variants all inherit EXTRUDER; only waxes, Plastic, Tin, TinAlloy and Aluminium inherit EXTRUDER_SIMPLE. */
public final class CapsuleCellRecipes {
    private CapsuleCellRecipes() {}
    public static int register() {
        int count=0;
        for(var spec:PortableFluidContainerSpec.values()) {
            if(!spec.shapeId().equals("cell"))continue;
            boolean simple=simpleExtrusion(spec);
            for(var prefix:new MaterialPrefix[]{MaterialPrefix.ingot,MaterialPrefix.dust,MaterialPrefix.nugget,MaterialPrefix.dustTiny}) {
                var input=GTItems.getStack(prefix,spec.material(),1);
                if(input.isEmpty())continue;
                boolean tiny=prefix==MaterialPrefix.nugget||prefix==MaterialPrefix.dustTiny;
                var output=new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech","fluid_"+spec.id())),tiny?1:9);
                for(String mold:new String[]{"extruder_shape_capsulecellcontainer","low_heat_extruder_shape_capsulecellcontainer"}) {
                    if(!simple&&mold.startsWith("low_heat_"))continue;
                    if(output.isEmpty()||GTTechnological.get(mold)==null)throw new IllegalStateException("Missing capsule cell or mold: "+spec.id());
                    long ticks=simple?(tiny?8:64):hotExtrusionTicks(spec.material(),prefix.getMaterialWeight());
                    if(MachineRecipeMaps.Extruder.addRecipe(new Recipe(new ItemStack[]{input,new ItemStack(GTTechnological.get(mold))},new ItemStack[]{output},null,null,null,null,ticks,simple?16:96,0))!=null)count++;
                }
            }
        }
        return count;
    }
    public static boolean simpleExtrusion(PortableFluidContainerSpec spec) {
        return spec.id().startsWith("cell_wax")||switch(spec.id()) {
            case "cell_plastic","cell_tin","cell_tin_alloy","cell_aluminium"->true;default->false;
        };
    }
    /** RecipeMapHandlerPrefixForging.getCosts / OreDictMaterial.getWeight, at 96 GU/t. */
    public static long hotExtrusionTicks(com.gregtech.gregtech.api.material.GTMaterial material,long units) {
        double kilograms=material.getDensity()*111.111111*units/com.gregtech.gregtech.api.material.GTValues.U;
        return Math.max(16,1+(long)Math.abs((material.getMeltingPoint()-293d)*kilograms/(75*96)));
    }

    /** RecipeMapShredder: an empty capcellcon recovers 1/9 U; a filled one is never recyclable. */
    public static int registerRecycling() {
        int count=0;
        for(var spec:PortableFluidContainerSpec.values())if(spec.shapeId().equals("cell")) {
            var output=GTItems.getStack(MaterialPrefix.dustTiny,spec.material(),1);
            if(output.isEmpty())throw new IllegalStateException("Missing capsule recycling dust: "+spec.id());
            var input=new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("gregtech","fluid_"+spec.id())));
            boolean brittle=spec.id().startsWith("cell_wax")||spec.id().equals("cell_plastic");
            long work=(brittle?2L:256L)*Math.max(1,spec.material().getToolQuality()+1);
            if(MachineRecipeMaps.Shredder.addRecipe(new Recipe(new ItemStack[]{input},new ItemStack[]{output},null,null,null,null,(work+8)/9,16,0))!=null)count++;
        }
        return count;
    }
}
