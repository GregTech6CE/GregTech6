package com.gregtech.gregtech.loaders.c;

import com.gregtech.gregtech.content.tool.CraftingToolDefinitions;
import com.gregtech.gregtech.data.MaterialGroups;
import com.gregtech.gregtech.data.MaterialPrefix;

/** Original embedded RM rows in MultiItemRandomTools:345-361,369,378,389,503-512. */
final class GTTechnologyRecipes {
    private static final String[] NONE = new String[0];
    private GTTechnologyRecipes() {}
    private static void row(String map, long ticks, String[] in, String[] fluids, String... out) {
        GeneratedRecipeSink.register(map,16,ticks,null,in,fluids,NONE,out);
    }
    static void load() {
        for (var tool : CraftingToolDefinitions.ALL)
            row("Boxinator",16L*tool.count(),new String[]{"i:"+tool.prefix()+":"+tool.material()+":1",
                "tech:"+tool.tip()+":0"},NONE,"tech:"+tool.token()+":"+tool.count());
        row("Canner",16,new String[]{"tech:lighter_empty:1"},new String[]{"f:Butane:100"},"tech:lighter_full:1");
        row("Canner",64,new String[]{"tech:shiny_lighter_empty:1"},new String[]{"f:Butane:1000"},"tech:shiny_lighter_full:1");
        row("Canner",16,new String[]{"tech:plastic_lighter_empty:1"},new String[]{"f:Butane:100"},"tech:plastic_lighter_full:1");
        row("Boxinator",64,new String[]{"tech:match:64","i:plateDouble:Paper:1"},NONE,"tech:match_box_full:1");
        row("Unboxinator",32,new String[]{"tech:match_box_full:1"},NONE,"tech:match:64","i:scrapGt:Paper:16");
        for (var wood : MaterialGroups.Wood.getReRegistrations()) {
            if (!MaterialPrefix.bolt.isValidFor(wood.resolve())) continue;
            var phosphorus = new java.util.LinkedHashSet<>(MaterialGroups.Phosphorus.getReRegistrations());
            phosphorus.add(com.gregtech.gregtech.data.generated.GT6Materials.Elements.P);
            for (var material : phosphorus) {
                if (material == null || !MaterialPrefix.dust.isValidFor(material.resolve())) continue;
                String name = material.resolve().getName(), stick = wood.resolve().getName();
                row("Assembler",16,new String[]{"i:bolt:"+stick+":1","i:dustSmall:"+name+":1"},NONE,"tech:match:1");
                row("Assembler",64,new String[]{"i:bolt:"+stick+":4","i:dust:"+name+":1"},NONE,"tech:match:4");
            }
        }
    }
}
