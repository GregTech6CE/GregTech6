package com.gregtech.gregtech.content.recipe;

import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

/** GT6 Loader_Recipes_Vanilla smash/sawing rows, with named 1.20 wood/color variants. */
public final class VanillaBlockProcessingRecipes extends OriginalRecipeBatch {
    public static final VanillaBlockProcessingRecipes INSTANCE = new VanillaBlockProcessingRecipes();
    public static int register() {
        INSTANCE.smashing();
        INSTANCE.cutting();
        INSTANCE.furniture();
        INSTANCE.washing();
        return INSTANCE.entries().size();
    }
    private static ItemStack mc(String path, int count) { return item("minecraft:" + path, count); }
    private void smash(String input, ItemStack output) {
        add("hammer/"+input, MachineRecipeMaps.Hammer,16,16,items(mc(input,1)),items(output),null,null);
        add("crusher/"+input, MachineRecipeMaps.Crusher,32,16,items(mc(input,1)),items(output),null,null);
    }
    private void smashing() {
        smash("brown_mushroom_block",mc("brown_mushroom",1));
        smash("red_mushroom_block",mc("red_mushroom",1));
        for(String quartz : new String[]{"quartz_block","chiseled_quartz_block","quartz_pillar"}) smash(quartz,mc("quartz",4));
        // Modern stonecutting produces one stair from one four-quartz block.
        smash("quartz_stairs",mc("quartz",4));
        smash("quartz_slab",mc("quartz",2));
        smash("stone_bricks",mc("cracked_stone_bricks",1));
        for(String block : new String[]{"mossy_stone_bricks","cracked_stone_bricks","chiseled_stone_bricks","stone"}) smash(block,mc("cobblestone",1));
        smash("cobblestone",mc("gravel",1));
        for(String block : new String[]{"sandstone","chiseled_sandstone","cut_sandstone"}) smash(block,mc("sand",1));
        smash("ice",mat(MaterialPrefix.dust,"Ice",1));
        smash("packed_ice",mat(MaterialPrefix.dust,"Ice",2));
        smash("terracotta",mat(MaterialPrefix.dust,"Clay",2));
        smash("glass",mat(MaterialPrefix.dust,"Glass",9));
        smash("glass_pane",mat(MaterialPrefix.dust,"Glass",1));
        smash("glowstone",mc("glowstone_dust",4));
        for(var color : DyeColor.values()) {
            String c=color.getName();
            smash(c+"_terracotta",mat(MaterialPrefix.dust,"Clay",2));
            smash(c+"_stained_glass",mat(MaterialPrefix.dust,"Glass",9));
            smash(c+"_stained_glass_pane",mat(MaterialPrefix.dust,"Glass",1));
        }
    }
    private void saw(String id, int count, int ticks, int coolant, boolean food, ItemStack... outputs) {
        for(var variant : SawingCoolants.variants(food)) {
            add("saw/"+id+"/"+variant.field(),MachineRecipeMaps.Cutter,ticks*variant.multiplier(),16,
                    items(mc(id,count)),outputs,fluids(fluid(variant.field(),coolant*variant.multiplier())),null);
            // A block carrying stored items must be emptied before dismantling.
            entries().get(entries().size()-1).recipe().withEmptyContainerInputs();
        }
    }
    private void cutting() {
        saw("glass",1,32,50,false,mc("glass_pane",9));
        for(var color : DyeColor.values()) {
            String c=color.getName();
            saw(c+"_stained_glass",1,32,50,false,mc(c+"_stained_glass_pane",9));
            saw(c+"_wool",2,32,50,false,mc(c+"_carpet",3));
            saw(c+"_bed",1,48,100,false,mc("oak_planks",3),mc(c+"_wool",3));
        }
        for(String[] pair : new String[][]{{"stone","smooth_stone_slab"},{"sandstone","sandstone_slab"},
                {"cobblestone","cobblestone_slab"},{"bricks","brick_slab"},{"stone_bricks","stone_brick_slab"},
                {"nether_bricks","nether_brick_slab"},{"quartz_block","quartz_slab"},
                {"chiseled_quartz_block","quartz_slab"},{"quartz_pillar","quartz_slab"}})
            saw(pair[0],1,16,100,false,mc(pair[1],2));
        saw("smooth_stone",1,16,100,false,mat(MaterialPrefix.plate,"Stone",8),mat(MaterialPrefix.dust,"Stone",1));
        saw("smooth_stone_slab",1,16,50,false,mat(MaterialPrefix.plate,"Stone",4),mat(MaterialPrefix.dustSmall,"Stone",2));
        saw("melon",1,16,50,true,mc("melon_slice",8),mc("melon_seeds",1));
    }
    private void furniture() {
        for(String wood : new String[]{"oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","crimson","warped"}) {
            saw(wood+"_button",1,16,100,false,mc(wood+"_planks",1));
            saw(wood+"_pressure_plate",1,32,100,false,mc(wood+"_planks",2));
            saw(wood+"_door",1,32,100,false,mc(wood+"_planks",2));
            saw(wood+"_trapdoor",1,48,100,false,mc(wood+"_planks",3));
            saw(wood+"_fence_gate",1,32,100,false,mc(wood+"_planks",2),mat(MaterialPrefix.dust,"Wood",2));
            saw(wood+"_sign",1,32,100,false,mc(wood+"_planks",2),mat(MaterialPrefix.dustDiv72,"Wood",12));
            if(!wood.equals("crimson")&&!wood.equals("warped")) saw(wood+"_boat",1,80,100,false,mc(wood+"_planks",5));
        }
        saw("crafting_table",1,64,100,false,mc("oak_planks",4));
        saw("bookshelf",1,96,100,false,mc("oak_planks",6),mc("book",3));
        saw("chest",1,128,100,false,mc("oak_planks",8));
        saw("trapped_chest",1,128,100,false,mc("oak_planks",8),mc("tripwire_hook",1));
        saw("note_block",1,128,100,false,mc("oak_planks",8),mc("redstone",1));
        saw("jukebox",1,128,100,false,mc("oak_planks",8),mc("diamond",1));
        saw("painting",1,64,100,false,mc("stick",8));
        saw("item_frame",1,64,100,false,mc("stick",8));
        saw("ladder",1,40,100,false,mc("stick",2),mat(MaterialPrefix.dustDiv72,"Wood",12));
    }
    private void washing() {
        for(String[] pair : new String[][]{{"mossy_cobblestone","cobblestone"},{"mossy_stone_bricks","stone_bricks"}})
            for(String water : new String[]{"Water","DistW","SpDew","MnWtr"})
                add("wash/"+pair[0]+"/"+water,MachineRecipeMaps.PressureWasher,64,16,
                        items(mc(pair[0],1)),items(mc(pair[1],1)),fluids(fluid(water,200)),null);
    }
}
