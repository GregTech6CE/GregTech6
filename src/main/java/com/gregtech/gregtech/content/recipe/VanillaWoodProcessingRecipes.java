package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.world.item.ItemStack;

/** Loader_Recipes_Woods / WoodEntry / BeamEntry defaults. Stripped logs are modern beams. */
public final class VanillaWoodProcessingRecipes extends OriginalRecipeBatch {
    public static final VanillaWoodProcessingRecipes INSTANCE = new VanillaWoodProcessingRecipes();
    public static int register() {
        for(String wood:new String[]{"oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry"}) INSTANCE.wood(wood);
        TreatedWoodRecipes.register();
        return INSTANCE.entries().size();
    }
    private static ItemStack mc(String id,int count) { return item("minecraft:"+id,count); }
    private void saw(String input,int ticks,int coolant,ItemStack... outputs) {
        for(var v:SawingCoolants.variants(false))
            add("saw/"+input+"/"+v.field(),MachineRecipeMaps.Cutter,ticks*v.multiplier(),16,items(mc(input,1)),outputs,
                    fluids(fluid(v.field(),coolant*v.multiplier())),null);
    }
    private void wood(String wood) {
        for(String suffix:new String[]{"log","wood"}) {
            String raw=wood+"_"+suffix, stripped="stripped_"+raw;
            saw(raw,128,4,mc(wood+"_planks",6),mat(MaterialPrefix.dust,"Bark",1));
            saw(stripped,128,4,mc(wood+"_planks",7),mat(MaterialPrefix.dust,"Wood",1));
            for(String water:new String[]{"Water","DistW","SpDew","MnWtr"})
                add("debark/"+raw+"/"+water,MachineRecipeMaps.PressureWasher,64,16,items(mc(raw,1)),
                        items(mc(stripped,1),mat(MaterialPrefix.dust,"Bark",1)),fluids(fluid(water,200)),null);
            for(String input:new String[]{raw,stripped}) {
                add("lathe/"+input,MachineRecipeMaps.Lathe,80,16,items(mc(input,1)),
                        items(mat(MaterialPrefix.stickLong,"Wood",4),mat(MaterialPrefix.dust,"Wood",1)),null,null);
                add("coke/"+input,MachineRecipeMaps.CokeOven,3600,0,items(mc(input,1)),items(mc("charcoal",1)),
                        null,fluids(fluid("Oil_Creosote",input.equals(raw)?250:200)));
            }
        }
        saw(wood+"_planks",72,3,mc(wood+"_slab",2));
        saw(wood+"_stairs",72,3,mc(wood+"_slab",1),mat(MaterialPrefix.dustSmall,"Wood",1));
        add("lathe/"+wood+"_planks",MachineRecipeMaps.Lathe,16,16,items(mc(wood+"_planks",1)),items(mc("stick",2)),null,null);
    }
}
