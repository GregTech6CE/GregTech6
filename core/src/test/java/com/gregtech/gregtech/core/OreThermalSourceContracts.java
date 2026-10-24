package com.gregtech.gregtech.core;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.content.recipe.ReviewedOreGeneratedFluids;
import com.gregtech.gregtech.data.FluidCatalog;
public final class OreThermalSourceContracts {
 static void require(boolean condition,String evidence){if(!condition)throw new AssertionError(evidence);System.out.println(evidence);}
 static FluidCatalog.FluidEntry exact(){return new FluidCatalog.FluidEntry("mercury",FluidCatalog.FluidTextureMode.DEDICATED,"Mercury",0,true,false,false,0,0,0,300,FluidCatalog.FluidFlags.LIQUID);}
 public static void main(String[] args){String mode=args[0];if(!java.util.Set.of("positive","same-field","alias-field").contains(mode))throw new IllegalArgumentException(mode);com.gregtech.gregtech.api.mod.ModData.bindPresence(id->id.equals("minecraft")||id.equals("gt6ce"));GTMaterialRegistry.init();
  FluidCatalog.FluidEntry external=null;
  if(!mode.equals("positive"))external=FluidCatalog.registerDefinition(mode.equals("same-field")?"GenLiquid_Mercury":"ExternalMercury",exact());
  com.gregtech.gregtech.content.fluid.FluidDefinitions.prepare();
  var entry=FluidCatalog.get("GenLiquid_Mercury");
  require(entry!=null&&entry.temperature()==300,"actual native Mercury300");
  if(mode.equals("positive")){
   long u=648648000L;long[] inputs={u,u/4,u/9,10*(u/8),11*(u/8),10*(u/72),11*(u/72),u/72,9*u};int[] outputs={48,12,5,60,66,6,7,0,432};
   for(int i=0;i<inputs.length;i++){
    var result=com.gregtech.gregtech.content.recipe.SourceRecyclingMath.singleLiquid(inputs[i],u,u/3,144,7.8003,234,300,293);
    require(result.millibuckets()==outputs[i]&&result.ticks()==(i==8?34:16),"source floor/work row "+i);
   }
   require(ReviewedOreGeneratedFluids.mercury(entry),"factory Mercury is reviewed");
   require(!FluidCatalog.isBuiltInDefinition(entry),"generated Mercury stays outside static builtin directory");
   require(!FluidCatalog.isGeneratedGasLiquidDefinition("GenLiquid_Mercury",exact()),"equal copied definition is not factory identity");
   require(FluidCatalog.isGeneratedGasLiquidDefinition("GenGas_Mercury",entry),"generated alias resolves actual factory identity");
   require(!FluidCatalog.isGeneratedGasLiquidDefinition("Petrotheum",FluidCatalog.get("Petrotheum")),"static Petrotheum not claimed generated");
   FluidCatalog.registerGeneratedGasLiquidFluids(GTMaterialRegistry.allMaterials());
   require(ReviewedOreGeneratedFluids.mercury(FluidCatalog.get("GenLiquid_Mercury")),"repeated factory preserves provenance");
   var owned=new java.util.LinkedHashMap<String,FluidCatalog.FluidEntry>();
   for(var e:FluidCatalog.all().entrySet())if(e.getValue().luminosity()==10&&FluidCatalog.isGeneratedGasLiquidDefinition(e.getKey(),e.getValue()))owned.put(e.getKey(),e.getValue());
   require(!owned.isEmpty(),"actual generated molten-phase controls exist");
   FluidCatalog.finalizeMaterialFluidProperties();
   for(var e:owned.entrySet())require(FluidCatalog.get(e.getKey())!=e.getValue()&&FluidCatalog.isGeneratedGasLiquidDefinition(e.getKey(),FluidCatalog.get(e.getKey())),"owned immutable finalization transfers identity: "+e.getKey());
  }else{
   require(entry==external,"public preemption retains original external object "+mode);
   require(!FluidCatalog.isGeneratedGasLiquidDefinition("GenLiquid_Mercury",entry),"public preemption never becomes factory provenance "+mode);
   require(!ReviewedOreGeneratedFluids.mercury(entry),"source adapter UNKNOWN on public preemption "+mode);
   FluidCatalog.registerGeneratedGasLiquidFluids(GTMaterialRegistry.allMaterials());
   require(!ReviewedOreGeneratedFluids.mercury(entry),"repeated factory does not claim preemption "+mode);
  }
  FluidCatalog.closeDefinitions();boolean rejected=false;try{FluidCatalog.registerDefinition("AfterClosed",new FluidCatalog.FluidEntry("afterclosed"));}catch(IllegalStateException expected){rejected=true;}
  require(rejected,"existing closed registration lifecycle retained");
 }
}
