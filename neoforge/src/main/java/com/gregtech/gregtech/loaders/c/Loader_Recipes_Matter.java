package com.gregtech.gregtech.loaders.c;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.loaders.IGTLoader;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

/** GT6 Loader_Recipes_Other: special Ender conversions and elemental separation (131072 QU per nucleon). */
public final class Loader_Recipes_Matter implements IGTLoader {
    private int added;
    public void run() {
        special("Dilithium","dustDiv72",1,144L,"Ender_TE",250);
        special("Dilithium","dustTiny",1,1152L,"Ender_TE",20000);
        special("Dilithium","dustSmall",1,2592L,"Ender_TE",45000);
        special("Dilithium","dust",1,10368L,"Ender_TE",180000);
        special("Dilithium","gem",1,20736L,"Ender_TE",360000);
        special("Dilithium","blockDust",1,93312L,"Ender_TE",1620000);
        special("Dilithium","blockGem",1,186624L,"Ender_TE",3240000);
        special("AncientDebris","dustDiv72",1,144L,"Ender_TE",250);
        special("AncientDebris","dustTiny",1,1152L,"Ender_TE",2000);
        special("AncientDebris","dustSmall",1,2592L,"Ender_TE",4500);
        special("AncientDebris","dust",1,10368L,"Ender_TE",18000);
        special("AncientDebris","ingot",1,10368L,"Ender_TE",18000);
        special("AncientDebris","blockDust",1,93312L,"Ender_TE",162000);
        special("AncientDebris","blockIngot",1,93312L,"Ender_TE",162000);
        special("Dilithium","dustDiv72",0,144L,"Ender",144);
        special("Dilithium","dustTiny",0,1152L,"Ender",11520);
        special("Dilithium","dustSmall",0,2592L,"Ender",25920);
        special("Dilithium","dust",0,10368L,"Ender",103680);
        special("Dilithium","gem",0,20736L,"Ender",207360);
        special("Dilithium","blockDust",0,93312L,"Ender",933120);
        special("Dilithium","blockGem",0,186624L,"Ender",1866240);
        special("AncientDebris","dustDiv72",0,144L,"Ender",144);
        special("AncientDebris","dustTiny",0,1152L,"Ender",1152);
        special("AncientDebris","dustSmall",0,2592L,"Ender",2592);
        special("AncientDebris","dust",0,10368L,"Ender",10368);
        special("AncientDebris","ingot",0,10368L,"Ender",10368);
        special("AncientDebris","blockDust",0,93312L,"Ender",93312);
        special("AncientDebris","blockIngot",0,93312L,"Ender",93312);
        var fluids=RegisteredFluids.all();
        var seen=Collections.newSetFromMap(new IdentityHashMap<GTMaterial,Boolean>());
        for(var raw:GTMaterialRegistry.allMaterials()) {
            var material=raw.resolve();
            if(!seen.add(material)||!material.has(MaterialProperty.ELEMENT)||material.has(MaterialProperty.ANTIMATTER)
                    ||material.getProtons()+material.getNeutrons()<=0)continue;
            for(var prefix:List.of(MaterialPrefix.dust,MaterialPrefix.ingot,MaterialPrefix.plate,MaterialPrefix.plateGem,MaterialPrefix.gem)) {
                var input=GTItems.getStack(prefix,material,1);
                if(!input.isEmpty())add(material,input,FluidStack.EMPTY,1);
            }
            for(var prefix:List.of(BlockMaterialPrefix.blockDust,BlockMaterialPrefix.blockIngot,BlockMaterialPrefix.blockPlate,BlockMaterialPrefix.blockPlateGem,BlockMaterialPrefix.blockGem)) {
                var input=GTBlocks.getStack(prefix,material);
                if(!input.isEmpty())add(material,input,FluidStack.EMPTY,9);
            }
            var fluidNames=new HashSet<String>();
            for(var entry:fluids.entrySet()) {
                var definition=entry.getValue();
                if(definition.materialKey()==null||!GTMaterialRegistry.get(definition.materialKey()).resolve().equals(material))continue;
                var name=definition.registryName();
                if(!fluidNames.add(name)||definition.isHidden())continue;
                var fluid=GTFluids.still(entry.getKey());if(fluid==null||!fluid.isBound())continue;
                boolean molten=name.startsWith("molten.")||name.endsWith(".molten");
                add(material,ItemStack.EMPTY,new FluidStack(fluid.get(),molten?144:1000),1);
            }
        }
        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Matter separation (elemental + special): {} recipes",added);
    }
    private void special(String materialName,String form,int circuit,long duration,String fluidName,int amount) {
        var material=GTMaterialRegistry.get(materialName);ItemStack input;
        input=switch(form) {
            case "blockDust" -> GTBlocks.getStack(BlockMaterialPrefix.blockDust,material);
            case "blockGem" -> GTBlocks.getStack(BlockMaterialPrefix.blockGem,material);
            case "blockIngot" -> GTBlocks.getStack(BlockMaterialPrefix.blockIngot,material);
            default -> {
                var prefix=com.gregtech.gregtech.api.prefix.PrefixRegistry.all().stream().filter(p->p.getName().equals(form)).findFirst().orElse(null);
                yield prefix==null?ItemStack.EMPTY:GTItems.getStack(prefix,material,1);
            }
        };
        var fluid=GTFluids.still(fluidName);
        if(input.isEmpty()||fluid==null||!fluid.isBound())return;
        MachineRecipeMaps.Massfab.addRecipe(new Recipe(new ItemStack[]{new ItemStack(GTTechnological.selectorTag(circuit)),input},null,null,null,null,
                new FluidStack[]{new FluidStack(fluid.get(),amount)},duration,16,0));added++;
    }
    private void add(GTMaterial material,ItemStack input,FluidStack fluid,int units) {
        var outputs=new ArrayList<FluidStack>();
        if(material.getProtons()>0)outputs.add(new FluidStack(GTFluids.still("MatterCharged").get(),Math.toIntExact(material.getProtons()*units)));
        if(material.getNeutrons()>0)outputs.add(new FluidStack(GTFluids.still("MatterNeutral").get(),Math.toIntExact(material.getNeutrons()*units)));
        long duration=Math.multiplyExact(Math.multiplyExact(material.getProtons()+material.getNeutrons(),131072L),units);
        MachineRecipeMaps.Massfab.addRecipe(new Recipe(input.isEmpty()?null:new ItemStack[]{input},null,null,null,
                fluid.isEmpty()?null:new FluidStack[]{fluid},outputs.toArray(FluidStack[]::new),duration,1,0));added++;
    }
}
