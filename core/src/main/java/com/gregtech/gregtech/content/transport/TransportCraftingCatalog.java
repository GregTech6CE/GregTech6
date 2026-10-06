/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from MultiTileEntityPipeFluid/Item and Loader_MultiTileEntities. */
package com.gregtech.gregtech.content.transport;

import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.content.transport.fluid.FluidTransportDefinitions;
import java.util.*;

/** Native adapters resolve these inputs without making a second set of recipe rules. */
public final class TransportCraftingCatalog {
    private TransportCraftingCatalog() {}
    public record Input(String kind,String name,GTMaterial material) {}
    public record Row(String path,List<String> pattern,Map<Character,Input> key,String output,int count,boolean unpack,boolean empty) {}
    private static Input form(String prefix,GTMaterial material){return new Input("form",prefix,material);}
    private static Input item(String id){return new Input("item",id,null);}
    private static Input tag(String id){return new Input("tag",id,null);}
    private static Input tool(String id){return item("gregtech:tool_"+id);}
    private static Row shaped(String path,List<String> pattern,Map<Character,Input> key,String output,boolean empty){return new Row(path,pattern,key,"gregtech:"+output,1,false,empty);}
    private static Row plain(String path,String output,GTMaterial material,PipeSpec.PipeSize size){
        var pattern=switch(size){case TINY->List.of("sP ","wzh");case SMALL->List.of(" P ","wzh");case MEDIUM->List.of("PPP","wzh");default->List.of("PPP","wzh","PPP");};
        return shaped("pipe/"+path,pattern,Map.of('P',form(size==PipeSpec.PipeSize.HUGE?"plateDouble":"plateCurved",material),
                's',tool("saw"),'w',tool("wrench"),'z',tool("bending_cylinder"),'h',tool("hammer")),output,false);
    }
    public static List<Row> rows(){
        var rows=new ArrayList<Row>();
        for(var spec:FluidTransportDefinitions.pipes()){
            String suffix=spec.id().substring(("pipe_"+spec.size().name().toLowerCase(Locale.ROOT)+"_").length());
            if(spec.size()==PipeSpec.PipeSize.QUADRUPLE||spec.size()==PipeSpec.PipeSize.NONUPLE){
                boolean quad=spec.size()==PipeSpec.PipeSize.QUADRUPLE;
                String plain="gregtech:pipe_"+(quad?"medium_":"small_")+suffix;
                rows.add(shaped("pipe/fluid/"+spec.id(),quad?List.of("PP","PP"):List.of("PPP","PPP","PPP"),Map.of('P',item(plain)),spec.id(),true));
                rows.add(new Row("pipe/unpack/"+spec.id(),List.of(),Map.of('P',item("gregtech:"+spec.id())),plain,quad?4:9,true,true));
            }else if(!Set.of("wood","treated_wood","plastic","rubber","carbon").contains(suffix)){
                // Original aRecipe=false for these five: ordinary Wood has its separate existing patterns.
                rows.add(plain("fluid/"+spec.id(),spec.id(),spec.material(),spec.size()));
            }
        }
        for(var mat:ItemPipeCatalog.ITEM_PIPE_MATS)for(var size:ItemPipeSpec.ItemPipeSize.values()){
            String id="item_pipe_"+size.name().toLowerCase(Locale.ROOT)+"_"+mat.idSuffix();
            if(size.restrictive()){
                var base=switch(size){case RESTRICTIVE_MEDIUM->"medium";case RESTRICTIVE_LARGE->"large";default->"huge";};
                var pattern=switch(size){case RESTRICTIVE_MEDIUM->List.of(" h ","RPR"," R ");case RESTRICTIVE_LARGE->List.of("hR ","RPR"," R ");default->List.of(" h ","RPR","RRR");};
                rows.add(shaped("pipe/restrictive/"+id,pattern,Map.of('P',item("gregtech:item_pipe_"+base+"_"+mat.idSuffix()),'R',form("ring",com.gregtech.gregtech.data.MaterialGroups.Steel),'h',tool("hammer")),id,true));
            }else rows.add(plain("item/"+id,id,mat.material(),PipeSpec.PipeSize.valueOf(size.name())));
        }
        for(var spec:FluidTransportDefinitions.tanks()){
            var hull=spec.material();
            if(hull==com.gregtech.gregtech.data.generated.GT6Materials.Compounds.Steel)hull=com.gregtech.gregtech.data.MaterialGroups.Steel;
            if(hull==com.gregtech.gregtech.data.generated.GT6Materials.Elements.W)hull=com.gregtech.gregtech.data.MaterialGroups.W;
            if(spec.type()==TankSpec.TankType.METAL_DRUM)
                rows.add(shaped("tank/"+spec.id(),List.of(" h ","PSP","PSP"),Map.of('h',tool("hammer"),'P',form("plateCurved",hull),'S',form("stickLong",hull)),spec.id(),false));
            else if(spec.type()==TankSpec.TankType.WOOD_BARREL)
                rows.add(shaped("tank/"+spec.id(),List.of("rGs","PSP","PSP"),Map.of('r',tool("soft_hammer"),'s',tool("saw"),'G',tag("forge:glue"),'P',com.gregtech.gregtech.content.transport.fluid.CheapWoodBarrelCatalog.entry(spec.id()).isPresent()?tag("gregtech:wooden_planks"):form("plate",spec.material()),'S',form("stickLong",TransportMaterialRules.barrelRod(spec))),spec.id(),false));
            // PlasticCan is an existing extruder mold output, not a made-up shaped recipe.
        }
        rows.addAll(com.gregtech.gregtech.content.energy.OriginalThermalCrafting.rows());
        rows.addAll(com.gregtech.gregtech.content.energy.OriginalMagnetCrafting.rows());
        rows.addAll(com.gregtech.gregtech.content.energy.OriginalSteamTurbines.rows());
        return List.copyOf(rows);
    }
}
