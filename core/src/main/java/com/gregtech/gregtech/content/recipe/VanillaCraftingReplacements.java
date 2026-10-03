package com.gregtech.gregtech.content.recipe;

import java.util.*;

/** Default enabled vanilla rows from Loader_Recipes_Vanilla, Loader_Recipes_Replace and the wood dictionary.
 * Modern wood variants follow the same original wood rules. Optional-mod rows are owned by their compatibility loaders.
 */
public final class VanillaCraftingReplacements {
    public record Row(String id, String result, int count, String pattern, Map<Character,String> keys) {}
    private VanillaCraftingReplacements() {}
    private static Map<Character,String> keys(String... entries) {
        var keys=new LinkedHashMap<Character,String>();
        for(String entry:entries) keys.put(entry.charAt(0),entry.substring(2));
        return Collections.unmodifiableMap(keys);
    }
    private static void shaped(List<Row> rows,String id,int count,String pattern,String... entries) {
        rows.add(new Row("minecraft:"+id,"minecraft:"+id,count,pattern,keys(entries)));
    }
    public static List<Row> rows() {
        var rows=new ArrayList<Row>();
        // Loader_Recipes_Vanilla:469,478 removes the vanilla arrow shortcut and uses four shafts.
        rows.add(new Row("minecraft:arrow","minecraft:arrow",4,null,keys("F=item:minecraft:flint",
                "A=item:gregtech:arrow_gt_wood_empty","B=item:gregtech:arrow_gt_wood_empty",
                "C=item:gregtech:arrow_gt_wood_empty","D=item:gregtech:arrow_gt_wood_empty")));
        shaped(rows,"furnace",1,"XXX/XFX/XXX","X=tag:minecraft:stone_crafting_materials","F=firestarter");
        shaped(rows,"paper",1,"XXX","X=item:minecraft:sugar_cane");
        rows.add(new Row("minecraft:book","minecraft:book",1,null,keys("L=item:minecraft:leather","A=item:minecraft:paper","B=item:minecraft:paper","C=item:minecraft:paper")));
        shaped(rows,"glass_bottle",3,"G G/ G ","G=item:minecraft:glass");
        shaped(rows,"enchanting_table",1," B /DOD/OOO","B=item:minecraft:book","D=item:minecraft:diamond","O=item:minecraft:obsidian");
        shaped(rows,"ender_chest",1,"OOO/OEO/OOO","O=item:minecraft:obsidian","E=item:minecraft:ender_eye");
        shaped(rows,"bucket",1,"XhX/ Y ","X=material:plateCurved:Iron","Y=material:plate:Iron","h=tool:hard_hammer");
        shaped(rows,"anvil",1,"BBB/ Ih/III","B=item:minecraft:iron_block","I=item:minecraft:iron_ingot","h=tool:hard_hammer");
        shaped(rows,"iron_door",1,"XX /XXh/XX ","X=material:plate:Iron","h=tool:hard_hammer");
        shaped(rows,"cauldron",1,"X X/XhX/XXX","X=material:plate:Iron","h=tool:hard_hammer");
        shaped(rows,"hopper",1,"XwX/XCX/ X ","X=material:plate:Iron","w=tool:wrench","C=tag:minecraft:wooden_chests");
        shaped(rows,"iron_bars",8," w /XXX/XXX","X=material:stick:Iron","w=tool:wrench");
        shaped(rows,"heavy_weighted_pressure_plate",1,"XXh","X=material:plate:Iron","h=tool:hard_hammer");
        shaped(rows,"light_weighted_pressure_plate",1,"XXh","X=material:plate:Gold","h=tool:hard_hammer");
        shaped(rows,"compass",1,"sSR/CIC/dPh","P=material:plate:Iron","R=item:minecraft:redstone","C=material:plateCurved:Iron","I=material:stick:Iron","S=material:stick:IronMagnetic","s=tool:saw","d=tool:screwdriver","h=tool:hard_hammer");
        shaped(rows,"clock",1,"sGr/CQC/dPR","P=material:plate:Gold","R=item:minecraft:redstone","C=material:plateCurved:Gold","Q=item:minecraft:quartz","G=material:gearGtSmall:Gold","s=tool:saw","d=tool:screwdriver","r=tool:soft_hammer");
        shaped(rows,"stone_button",2,"S/S","S=item:minecraft:stone");
        shaped(rows,"stone_pressure_plate",1,"SS","S=item:minecraft:stone");
        shaped(rows,"shears",1,"hP/Pf","P=material:plate:Iron","h=tool:hard_hammer","f=tool:file");
        shaped(rows,"bone_meal",1,"h/X","h=tool:hard_hammer","X=item:minecraft:bone");
        rows.add(new Row("gregtech:vanilla/bone_white_dust","material:dust:White",1,null,keys("X=item:minecraft:bone")));
        shaped(rows,"quartz_stairs",1,"  X/ XX/XXX","X=item:minecraft:quartz");
        shaped(rows,"quartz_slab",1,"XX","X=item:minecraft:quartz");
        for(String prefix:List.of("stick","stickLong")) {
            var ingredients=new LinkedHashMap<Character,String>();
            ingredients.put('I',"material:"+prefix+":Iron");
            for(int i=0;i<(prefix.equals("stick")?4:8);i++) ingredients.put((char)('a'+i),"item:minecraft:redstone");
            rows.add(new Row("gregtech:vanilla/magnetic_"+prefix.toLowerCase(Locale.ROOT),"material:"+prefix+":IronMagnetic",1,null,ingredients));
        }
        for(String material:new String[]{"Iron","Gold"}) {
            String name=material.equals("Iron")?"iron":"golden";
            String p="P=material:plate:"+material,c="C=material:plateCurved:"+material;
            shaped(rows,name+"_helmet",1,"PPP/ChC",p,c,"h=tool:hard_hammer");
            shaped(rows,name+"_chestplate",1,"PhP/CPC/CPC",p,c,"h=tool:hard_hammer");
            shaped(rows,name+"_leggings",1,"PCP/ChC/C C",p,c,"h=tool:hard_hammer");
            shaped(rows,name+"_boots",1,"P P/ChC",p,c,"h=tool:hard_hammer");
            String ingot="I=item:minecraft:"+(material.equals("Iron")?"iron":"gold")+"_ingot";
            for(String kind:new String[]{"sword","pickaxe","shovel","axe","hoe"}) {
                String pattern=switch(kind){case "sword"->" P /fPh/ R ";case "pickaxe"->"PII/fRh/ R ";case "shovel"->"fPh/ R / R ";case "axe"->"PIh/PR /fR ";default->"PIh/fR / R ";};
                shaped(rows,name+"_"+kind,1,pattern,p,ingot,"R=item:minecraft:stick","f=tool:file","h=tool:hard_hammer");
            }
        }
        for(String wood:new String[]{"oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","crimson","warped"}) {
            String tag="tag:minecraft:"+wood+"_"+(wood.equals("crimson")||wood.equals("warped")?"stems":"logs");
            rows.add(new Row("minecraft:"+wood+"_planks","minecraft:"+wood+"_planks",2,null,keys("L="+tag)));
            rows.add(new Row("gregtech:vanilla/"+wood+"_planks_saw","minecraft:"+wood+"_planks",4,"s/L",keys("s=tool:saw","L="+tag)));
            shaped(rows,wood+"_pressure_plate",1,"PP","P=item:minecraft:"+wood+"_planks");
        }
        shaped(rows,"stick",2,"P/P","P=tag:minecraft:planks");
        rows.add(new Row("gregtech:vanilla/stick_saw","minecraft:stick",2,"s/P",keys("s=tool:saw","P=tag:minecraft:planks")));
        shaped(rows,"bowl",1,"k/X","k=tool:knife","X=tag:minecraft:planks");
        return List.copyOf(rows);
    }
}
