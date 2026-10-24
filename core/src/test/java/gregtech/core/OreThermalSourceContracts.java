package gregtech.core;
import gregtech.api.material.*;
import gregtech.internal.material.*;
import gregtech.internal.material.*;
import gregtech.internal.material.*;
import gregtech.content.recipe.ReviewedOreGeneratedFluids;
import gregtech.content.data.FluidCatalog;
public final class OreThermalSourceContracts {
 static void require(boolean condition,String evidence){if(!condition)throw new AssertionError(evidence);System.out.println(evidence);}
 static FluidCatalog.FluidEntry exact(){return new FluidCatalog.FluidEntry("mercury",FluidCatalog.FluidTextureMode.DEDICATED,"Mercury",0,true,false,false,0,0,0,300,FluidCatalog.FluidFlags.LIQUID);}
 public static void main(String[] args){String mode=args[0];if(!java.util.Set.of("positive","same-field","alias-field").contains(mode))throw new IllegalArgumentException(mode);gregtech.internal.mod.ModData.bindPresence(id->id.equals("minecraft")||id.equals("gt6ce"));GTMaterialRegistry.init();
  FluidCatalog.FluidEntry external=null;
  if(!mode.equals("positive"))external=FluidCatalog.registerDefinition(mode.equals("same-field")?"GenLiquid_Mercury":"ExternalMercury",exact());
  FluidCatalog.FluidEntry externalTi=null,externalFe=null;
  if(!mode.equals("positive")){
   externalTi=FluidCatalog.registerDefinition(mode.equals("same-field")?"GenMolten_Titanium":"ExternalTitanium",
    new FluidCatalog.FluidEntry("molten.titanium",FluidCatalog.FluidTextureMode.GENERIC_MOLTEN,"Titanium",0,true,false,false,0,0,10,1941,FluidCatalog.FluidFlags.LIQUID));
   externalFe=FluidCatalog.registerDefinition(mode.equals("same-field")?"GenMolten_Iron":"ExternalIron",
    new FluidCatalog.FluidEntry(mode.equals("same-field")?"external.iron":"molten_iron",FluidCatalog.FluidTextureMode.GENERIC_MOLTEN,"Iron",0,true,false,false,0,0,10,1811,FluidCatalog.FluidFlags.LIQUID));
  }
  gregtech.content.fluid.FluidDefinitions.prepare();
  for(String name:new String[]{"Titanium","Iron"}){
   String field="GenMolten_"+name;var material=GTMaterialRegistry.get(name).resolve();var molten=FluidCatalog.get(field);
   if(mode.equals("positive")){
    require(ReviewedOreGeneratedFluids.wholeDustMetal(material,molten),"reviewed generated molten "+name);
    require(!FluidCatalog.isGeneratedMoltenDefinition(field,molten.withTemperature(molten.temperature())),"equal molten record not factory identity "+name);
    require(!ReviewedOreGeneratedFluids.wholeDustMetal(null,molten)&&!ReviewedOreGeneratedFluids.wholeDustMetal(GTMaterialRegistry.get("Aluminium"),molten),"null/foreign material cannot acquire molten permission "+name);
    var oldTarget=material.getTargetSmeltingMaterial();long oldAmount=material.getTargetSmeltingAmount();
    try{material.setSmelting(GTMaterialRegistry.get("Aluminium"),oldAmount);require(!ReviewedOreGeneratedFluids.wholeDustMetal(material,molten),"explicit target override defers "+name);}
    finally{material.setSmelting(oldTarget,oldAmount);}
    try{material.setSmelting(oldTarget,oldAmount/2);require(!ReviewedOreGeneratedFluids.wholeDustMetal(material,molten),"explicit target ratio override defers "+name);}
    finally{material.setSmelting(oldTarget,oldAmount);}
    var before=molten;FluidCatalog.finalizeMaterialFluidProperties();molten=FluidCatalog.get(field);
    require(molten!=before&&ReviewedOreGeneratedFluids.wholeDustMetal(material,molten),"owned molten immutable finalization transfers identity "+name);
    FluidCatalog.registerGeneratedMoltenFluids(GTMaterialRegistry.allMaterials());
    require(FluidCatalog.get(field)==molten&&ReviewedOreGeneratedFluids.wholeDustMetal(material,molten),"repeated molten generation preserves exact identity "+name);
    var math=gregtech.content.recipe.SourceRecyclingMath.singleLiquid(648648000L,648648000L,648648000L,144,name.equals("Titanium")?4.54:7.874,name.equals("Titanium")?1941:1811,name.equals("Titanium")?1941:1811,293);
    require(math.millibuckets()==144&&math.ticks()==(name.equals("Titanium")?519:830),"source whole dust work "+name);
   }else{
    if(mode.equals("same-field"))require(molten==(name.equals("Titanium")?externalTi:externalFe),"same-key molten preemption retains exact public object "+name);
    else require(molten==null,"alternate-key same-path molten preemption creates no factory alias "+name);
    require(!ReviewedOreGeneratedFluids.wholeDustMetal(material,molten),"external molten definition stays UNKNOWN "+name);
    FluidCatalog.registerGeneratedMoltenFluids(GTMaterialRegistry.allMaterials());
    require(FluidCatalog.get(field)==molten&&!ReviewedOreGeneratedFluids.wholeDustMetal(material,molten),"repeated molten factory cannot claim external preemption "+name);
   }
  }

  var entry=FluidCatalog.get("GenLiquid_Mercury");
  require(entry!=null&&entry.temperature()==300,"actual native Mercury300");
  if(mode.equals("positive")){
   long u=648648000L;long[] inputs={u,u/4,u/9,10*(u/8),11*(u/8),10*(u/72),11*(u/72),u/72,9*u};int[] outputs={48,12,5,60,66,6,7,0,432};
   for(int i=0;i<inputs.length;i++){
    var result=gregtech.content.recipe.SourceRecyclingMath.singleLiquid(inputs[i],u,u/3,144,7.8003,234,300,293);
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
