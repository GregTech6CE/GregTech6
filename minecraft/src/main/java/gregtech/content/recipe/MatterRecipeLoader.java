package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.engine.material.*;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import gregtech.content.data.*;
import gregtech.content.registry.*;
import gregtech.bootstrap.InitializationStep;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import java.util.*;

/** GT6 Loader_Recipes_Other: special Ender conversions and elemental separation (131072 QU per nucleon). */
public final class MatterRecipeLoader implements InitializationStep {
    private int added;
    public void run() {
        for (var conversion : MatterConversionCatalog.SPECIAL_CONVERSIONS) {
            special(conversion.material(), conversion.form(), conversion.circuit(), conversion.duration(),
                    conversion.outputFluid(), conversion.outputAmount());
        }
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
//? if >=1.21.1 {
                /*var fluid=GTFluids.still(entry.getKey());if(fluid==null||!fluid.isBound())continue;
*///?} else {
                var fluid=GTFluids.still(entry.getKey());if(fluid==null||!fluid.isPresent())continue;
//?}
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
                var prefix=gregtech.engine.prefix.PrefixRegistry.all().stream().filter(p->p.getName().equals(form)).findFirst().orElse(null);
                yield prefix==null?ItemStack.EMPTY:GTItems.getStack(prefix,material,1);
            }
        };
        var fluid=GTFluids.still(fluidName);
//? if >=1.21.1 {
        /*if(input.isEmpty()||fluid==null||!fluid.isBound())return;
*///?} else {
        if(input.isEmpty()||fluid==null||!fluid.isPresent())return;
//?}
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
