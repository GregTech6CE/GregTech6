package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.data.FluidCatalog;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTFluids;
import gregtech.content.registry.GTItems;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import java.util.*;
import java.util.function.BiFunction;
import static gregtech.api.material.GTValues.U;
import static gregtech.content.recipe.SourceOreWashingRules.*;

/** Original Ores Bath/water Sluice, Handlers Petrotheum and explicit Cryotheum rows.
 * Definitions are resolved before unlinking; ownership is only an accepted addRecipe identity.
 * No signatures, generation flags, broad material exclusions or legacy collision fallbacks.
 */
public final class SharedOreWashingRows {
 private SharedOreWashingRows() {}
 private static final Map<RecipeMap,Set<Recipe>> OWNED=new IdentityHashMap<>();
 private static final List<Deferred> DEFERRED=new ArrayList<>();
 private static List<GTMaterial> materials=List.of();
 private static boolean initialized;
 public record Deferred(String material,String producer,String reason) {}
 public static Set<Recipe> owned(){
  Set<Recipe> out=Collections.newSetFromMap(new IdentityHashMap<>());
  OWNED.values().forEach(out::addAll);return Collections.unmodifiableSet(out);
 }
 public static List<Deferred> deferred(){return List.copyOf(DEFERRED);}
 public static void initialize(){
  if(initialized)throw new IllegalStateException("Washing initialized twice");
  initialized=true;Set<GTMaterial> seen=Collections.newSetFromMap(new IdentityHashMap<>());
  var list=new ArrayList<GTMaterial>();
  for(var m:GTMaterialRegistry.allMaterials())if(seen.add(m.resolve()))list.add(m.resolve());
  materials=List.copyOf(list);rebuild();
 }
 private static void clearOwned(){OWNED.forEach((map,rows)->rows.forEach(map::unlink));OWNED.clear();}
 /** Keep the initialization snapshot for another server in this JVM, but no stale row diagnostics. */
 public static void clear(){clearOwned();DEFERRED.clear();}
 public static void rebuild(){
  if(!initialized)return;
  var snapshot=new Snapshot();
  List<Definition> definitions=definitions(materials,snapshot);
  clearOwned();DEFERRED.clear();DEFERRED.addAll(snapshot.deferred);
  try{
   for(var definition:definitions){
    Recipe row=definition.admit();
    if(row!=null)OWNED.computeIfAbsent(definition.map(),k->Collections.newSetFromMap(new IdentityHashMap<>())).add(row);
   }
  }catch(RuntimeException|Error failure){clear();throw failure;}
 }
 static List<Definition> definitions(Collection<GTMaterial> selected,Snapshot snapshot){
  var out=new ArrayList<Definition>();
   for(var material:selected){
    GTMaterial m=material.resolve();
    petrotheum(m,snapshot,out); // Handlers forms are independent of the Ores whole+tiny gate.
    ores(m,snapshot,out);
    cryotheum(m,snapshot,out); // Three direct shapes, independent of ordinary washing eligibility.
   }
  return List.copyOf(out);
 }
 record Definition(RecipeMap map,boolean optimize,ItemStack[] inputs,ItemStack[] outputs,long[] chances,
                   FluidStack[] fluidInputs,FluidStack[] fluidOutputs,long duration,long power,boolean waterTag){
  Definition(RecipeMap map,boolean optimize,ItemStack[] inputs,ItemStack[] outputs,long[] chances,
             FluidStack[] fluidInputs,FluidStack[] fluidOutputs,long duration,long power){
   this(map,optimize,inputs,outputs,chances,fluidInputs,fluidOutputs,duration,power,false);
  }
  Definition withWater(){return new Definition(map,optimize,inputs,outputs,chances,fluidInputs,fluidOutputs,duration,power,true);}
  Recipe tagWater(Recipe row){
   return waterTag&&row!=null?row.withFluidTagInput(0,net.minecraft.resources.ResourceLocation.parse("gt6ce:water")):row;
  }
  Recipe admit(){
   // RecipeMap discards EMPTY fluids. Reject required unresolved bindings before it can create a free row.
   for(var fluid:fluidInputs)if(fluid==null||fluid.isEmpty())return null;
   for(var fluid:fluidOutputs)if(fluid==null||fluid.isEmpty())return null;
   if(optimize&&map==MachineRecipeMaps.Bath&&hasOverlap(inputs,outputs)){
    // Original GT6 retained the identity of an input cancelled to zero. Adapt just the exact
    // two-input reagent/main-output cancellation; never bypass the native optimizer globally.
    if(inputs.length!=2||outputs.length==0||inputs[1].getCount()!=1
        ||!ItemStack.matches(inputs[1],outputs[0]))return null;
    for(int i=0;i<inputs.length;i++)for(int j=0;j<outputs.length;j++)
     if(sameItem(inputs[i],outputs[j])&&(i!=1||j!=0))return null;
    var normalized=RecipeMap.make(true,inputs,outputs,null,chances,fluidInputs,fluidOutputs,duration,power,0);
    if(normalized==null||normalized.mInputs.length!=1||!ItemStack.matches(normalized.mInputs[0],inputs[0])
        ||normalized.mOutputs.length!=outputs.length||!normalized.mOutputs[0].isEmpty())return null;
    for(int j=1;j<outputs.length;j++)if(!ItemStack.matches(normalized.mOutputs[j],outputs[j]))return null;
    if(normalized.mDuration!=duration||normalized.mEUt!=power||!sameFluids(normalized.mFluidInputs,fluidInputs)
        ||!sameFluids(normalized.mFluidOutputs,fluidOutputs))return null;
    ItemStack reagent=inputs[1].copy();reagent.setCount(1);
    var retained=new Recipe(new ItemStack[]{normalized.mInputs[0].copy(),reagent},normalized.mOutputs,
        normalized.mSpecialItems,normalized.mChances,normalized.mFluidInputs,normalized.mFluidOutputs,
        normalized.mDuration,normalized.mEUt,normalized.mSpecialValue).withCatalystInputs(1);
    return map.addRecipe(tagWater(retained));
   }
   var row=RecipeMap.make(optimize,inputs,outputs,null,chances,fluidInputs,fluidOutputs,duration,power,0);
   return row==null?null:map.addRecipe(tagWater(row));
  }
 }
 private static boolean hasOverlap(ItemStack[] inputs,ItemStack[] outputs){
  for(var input:inputs)for(var output:outputs)if(sameItem(input,output))return true;return false;
 }
 private static boolean sameItem(ItemStack a,ItemStack b){
  if(a==null||b==null||a.isEmpty()||b.isEmpty())return false;
//? if >=1.21.1 {
  /*return ItemStack.isSameItemSameComponents(a,b);
*///?} else {
  return ItemStack.isSameItemSameTags(a,b);
//?}
 }
 private static boolean sameFluids(FluidStack[] a,FluidStack[] b){
  if(a.length!=b.length)return false;
  for(int i=0;i<a.length;i++)if(a[i].getFluid()!=b[i].getFluid()||a[i].getAmount()!=b[i].getAmount())return false;
  return true;
 }
 private static final String[] WATERS={"Water","MnWtr","DistW","SpDew"};
 private static void petrotheum(GTMaterial m,Snapshot s,List<Definition> out){
  GTMaterial sand=GTMaterialRegistry.get("SluiceSand");
  for(Size size:Size.values()){
   String route="petrotheum/"+size;
   NativeForm input=s.direct(size.input(),m,1);
   Availability gate=petroEligibility(m.isValid(),m.has(MaterialProperty.ANTIMATTER),input.availability());
   if(!s.ready(m,route,gate))continue;
   NativeForm main=s.direct(size.purified(),m,2),fine=s.direct(MaterialPrefix.crushedPurifiedTiny,m,size.byproductCount());
   // RecipeMapHandlerPrefix validates each output-prefix form; its extra OM sand is optional.
   if(!s.ready(m,route,requireAll(main.availability(),fine.availability())))continue;
   NativeForm tail=s.sand(size,sand);
   if(tail.availability()==Availability.UNKNOWN){s.defer(m,route,"unknown-sand-form");continue;}
   ItemStack[] outputs=tail.availability()==Availability.PRESENT
       ?new ItemStack[]{main.stack(),fine.stack(),tail.stack()}:new ItemStack[]{main.stack(),fine.stack()};
   FluidStack fluid=s.fluid("Petrotheum",size.petrotheumMb());if(!s.fluidReady(m,route,fluid))continue;
   out.add(new Definition(MachineRecipeMaps.Sluice,PETROTHEUM_OPTIMIZE,new ItemStack[]{input.stack()},
       outputs,PETROTHEUM_CHANCES.stream().mapToLong(Long::longValue).toArray(),
       new FluidStack[]{fluid},new FluidStack[0],size.ticks(),SLUICE_POWER));
  }
 }
 private static void ores(GTMaterial m,Snapshot s,List<Definition> out){
  NativeForm dust=s.direct(MaterialPrefix.dust,m,1),whole=s.input(MaterialPrefix.crushed,m,1),tiny=s.input(MaterialPrefix.crushedTiny,m,1);
  Availability gate=oresEligibility(m.isValid(),m.has(MaterialProperty.ANTIMATTER),m==GTMaterialRegistry.get("Bedrock"),
      dust.availability(),whole.availability(),tiny.availability());
  if(!s.ready(m,"ores",gate))return;
  List<GTMaterial> by=m.getByProducts().stream().map(GTMaterial::resolve).toList();
  for(Size size:Size.values()){
   String route="water/"+size;
   NativeForm main=s.purified(size,m,1);var products=waterByproducts(m,by);
   var outputs=new ItemStack[9];outputs[0]=main.stack();boolean known=main.availability()!=Availability.UNKNOWN;
   for(int i=0;i<8;i++){var fine=s.purified(Size.TINY,products.get(i),size.byproductCount());outputs[i+1]=fine.stack();known&=fine.availability()!=Availability.UNKNOWN;}
   if(!known){s.defer(m,route,"unknown-output-form");continue;}
   FluidStack sluice=s.fluid("Sluice",size.waterMb());if(!s.fluidReady(m,route,sluice))continue;
   var waters=waters(m,s,route,size.waterMb());
   for(FluidStack fluid:waters){
    var row=new Definition(MachineRecipeMaps.Sluice,WATER_OPTIMIZE,new ItemStack[]{size==Size.WHOLE?whole.stack():tiny.stack()},
        copies(outputs),SLUICE_CHANCES.stream().mapToLong(Long::longValue).toArray(),new FluidStack[]{fluid},
        new FluidStack[]{sluice.copy()},size.ticks(),SLUICE_POWER);
    out.add(waters.size()==WATERS.length?row.withWater():row);
    if(waters.size()==WATERS.length)break;
   }
  }
  for(var tag:SourceOreWashingData.Tag.values()){
   var selection=selectByproduct(m,by,candidate->s.tag(candidate,tag),candidate->s.purified(Size.TINY,candidate,1).availability());
   if(!s.ready(m,"bath/"+tag,selection.availability()))continue;
   GTMaterial product=selection.material();
   if(tag==SourceOreWashingData.Tag.MERCURY){
    for(Size size:Size.values()){
     var outputs=bathOutputs(m,product,size,s);if(outputs==null)continue;
     var mercury=s.fluid("GenLiquid_Mercury",size.mercuryMb());if(!s.fluidReady(m,"mercury/"+size,mercury))continue;
     out.add(new Definition(MachineRecipeMaps.Bath,BATH_OPTIMIZE,new ItemStack[]{size==Size.WHOLE?whole.stack():tiny.stack()},outputs,
         new long[0],new FluidStack[]{mercury},new FluidStack[0],size.ticks(),BATH_POWER));
    }
   }else{
    String[] reagents=tag==SourceOreWashingData.Tag.PERSULFATE?new String[]{"SodiumPersulfate","PotassiumPersulfate"}
        :new String[]{"Firestone","Pyrotheum","Octine"};
    for(String reagentName:reagents)for(BathShape shape:bathShapes()){
     String route="bath/"+tag+"/"+reagentName+"/"+shape;
     NativeForm reagent=s.direct(shape.reagent(),GTMaterialRegistry.get(reagentName),shape.reagentCount());
     if(!s.ready(m,route,reagent.availability()))continue;
     var outputs=bathOutputs(m,product,shape.outputSize(),s);if(outputs==null)continue;
     FluidStack[] steam=new FluidStack[0];
     if(tag==SourceOreWashingData.Tag.FIRESTONE){
      var fluid=s.fluid("Steam",shape.outputSize().steamMb());if(!s.fluidReady(m,route,fluid))continue;steam=new FluidStack[]{fluid};
     }
     var input=s.input(shape.input(),m,shape.inputCount());if(!s.ready(m,route,input.availability()))continue;
     var waters=waters(m,s,route,shape.outputSize().waterMb());
     for(FluidStack fluid:waters){
      var row=new Definition(MachineRecipeMaps.Bath,REAGENT_BATH_OPTIMIZE,new ItemStack[]{input.stack(),reagent.stack()},copies(outputs),new long[0],
          new FluidStack[]{fluid},copies(steam),shape.outputSize().ticks(),BATH_POWER);
      out.add(waters.size()==WATERS.length?row.withWater():row);
      if(waters.size()==WATERS.length)break;
     }
    }
   }
  }
 }
 private static List<FluidStack> waters(GTMaterial material,Snapshot snapshot,String route,int amount){
  var result=new ArrayList<FluidStack>();
  for(String water:WATERS){
   var fluid=snapshot.fluid(water,amount);
   if(snapshot.fluidReady(material,route+"/"+water,fluid))result.add(fluid);
  }
  return result;
 }
 private static ItemStack[] bathOutputs(GTMaterial m,GTMaterial product,Size size,Snapshot s){
  var counts=size==Size.WHOLE?WHOLE_BATH_BYPRODUCT_COUNTS:TINY_BATH_BYPRODUCT_COUNTS;
  var outputs=new ItemStack[counts.size()+1];var main=s.purified(size,m,1);outputs[0]=main.stack();
  if(main.availability()==Availability.UNKNOWN){s.defer(m,"bath/"+size,"unknown-primary-form");return null;}
  for(int i=0;i<counts.size();i++){
   var fine=s.purified(Size.TINY,product,counts.get(i));outputs[i+1]=fine.stack();
   if(fine.availability()==Availability.UNKNOWN){s.defer(m,"bath/"+size,"unknown-byproduct-form");return null;}
  }
  return outputs;
 }
 private static void cryotheum(GTMaterial m,Snapshot s,List<Definition> out){
  if(SourceOreCryotheumRules.binding(m.getId(),m.getName())==null||!m.isValid()
      ||!GTMaterialRegistry.isCoreMaterial(m)||AddonMaterials.owns(m))return;
  for(var shape:SourceOreCryotheumRules.shapes()){
   String route="cryotheum/"+shape;
   NativeForm input=switch(shape.input()){
    case BLOCK_DUST -> s.blockDust(m,shape.inputCount());
    case CRUSHED -> s.direct(MaterialPrefix.crushed,m,shape.inputCount());
    case CRUSHED_TINY -> s.direct(MaterialPrefix.crushedTiny,m,shape.inputCount());
   };
   NativeForm output=s.direct(shape.output(),m,shape.outputCount());
   if(!s.ready(m,route,requireAll(input.availability(),output.availability())))continue;
   var fluid=s.fluid("Cryotheum",shape.cryotheumMb());if(!s.fluidReady(m,route,fluid))continue;
   out.add(new Definition(MachineRecipeMaps.Bath,SourceOreCryotheumRules.OPTIMIZE,new ItemStack[]{input.stack()},
       new ItemStack[]{output.stack()},new long[0],new FluidStack[]{fluid},new FluidStack[0],
       SourceOreCryotheumRules.TICKS,SourceOreCryotheumRules.POWER));
  }
 }
 private static ItemStack[] copies(ItemStack[] values){return Arrays.stream(values).map(ItemStack::copy).toArray(ItemStack[]::new);}
 private static FluidStack[] copies(FluidStack[] values){return Arrays.stream(values).map(FluidStack::copy).toArray(FluidStack[]::new);}
 record NativeForm(Availability availability,ItemStack stack){
  static NativeForm absent(){return new NativeForm(Availability.ABSENT,ItemStack.EMPTY);}
  static NativeForm unknown(){return new NativeForm(Availability.UNKNOWN,ItemStack.EMPTY);}
  static NativeForm present(ItemStack stack){return new NativeForm(Availability.PRESENT,stack.copy());}
 }
 static class Snapshot {
  final List<Deferred> deferred=new ArrayList<>();
  final Map<GTMaterial,Map<MaterialPrefix,List<ItemStack>>> explicit=new IdentityHashMap<>();
  final Map<String,FluidCatalog.FluidEntry> definitions;
  final BiFunction<String,Integer,FluidStack> fluidLookup;
  final NonOreContinuationEvidence.Snapshot continuation;
  Snapshot(){this(FluidCatalog.all(),GTFluids::stack);}
  Snapshot(Map<String,FluidCatalog.FluidEntry> definitions,BiFunction<String,Integer,FluidStack> fluidLookup){
   this.definitions=Map.copyOf(definitions);this.fluidLookup=fluidLookup;
   continuation=NonOreContinuationEvidence.snapshot(materials);
   for(var item:ItemMaterialRegistry.entries().keySet()){
    var stack=new ItemStack(item);var form=MaterialEquivalence.form(stack);
    if(form!=null)explicit.computeIfAbsent(form.material().resolve(),k->new IdentityHashMap<>())
        .computeIfAbsent(form.prefix(),k->new ArrayList<>()).add(stack);
   }
  }
  void defer(GTMaterial m,String route,String reason){deferred.add(new Deferred(m.getName(),route,reason));}
  boolean ready(GTMaterial m,String route,Availability value){
   if(value==Availability.UNKNOWN)defer(m,route,"unknown-binding-or-evidence");return value==Availability.PRESENT;
  }
  boolean fluidReady(GTMaterial m,String route,FluidStack fluid){
   if(fluid.isEmpty()){defer(m,route,"required-fluid-unavailable");return false;}return true;
  }
  NativeForm direct(MaterialPrefix prefix,GTMaterial m,int count){
   if(m==null||!m.isValid()||count<1)return NativeForm.absent();m=m.resolve();
   ItemStack selected=GTItems.getStack(prefix,m,count);
   if(selected.isEmpty()){
    var candidates=explicit.getOrDefault(m,Map.of()).getOrDefault(prefix,List.of());
    if(candidates.size()>1)return NativeForm.unknown();
    if(candidates.size()==1){selected=candidates.get(0).copy();selected.setCount(count);}
   }
   if(selected.isEmpty())return NativeForm.absent();
   var form=MaterialEquivalence.form(selected);
   if(form==null||form.prefix()!=prefix||form.material().resolve()!=m||selected.getCount()!=count)
    return NativeForm.unknown();
   var metadata=ItemMaterialRegistry.explicit(selected.getItem()).orElse(null);
   if(metadata!=null&&(metadata.prefix()!=prefix||metadata.components().size()!=1
       ||metadata.material().resolve()!=m||metadata.amount()!=prefix.getMaterialWeight()))return NativeForm.unknown();
   return NativeForm.present(selected);
  }
  NativeForm input(MaterialPrefix prefix,GTMaterial m,int count){
   NativeForm direct=direct(prefix,m,count);
   if(direct.availability()!=Availability.PRESENT)return direct;
   if(NonOreMacerationRows.reviewed(m)&&!continuation.active(m)
       &&!SourceOreInputForms.allowGeneratedInput(m,prefix,direct.stack(),SourceOreInputForms.Evidence.NO))return NativeForm.absent();
   return direct;
  }
  NativeForm output(MaterialPrefix prefix,GTMaterial m,int count){
   NativeForm direct=direct(prefix,m,count);
   if(direct.availability()==Availability.UNKNOWN)return direct;
   ItemStack canonical=GTItems.getStack(prefix,m,count);
   ItemStack reviewed=SourceOreOutputRows.resolvingOutputs(()->SourceOreInputForms.reviewedOreOutput(prefix,m,count));
   if(ReviewedCrushedFormGeneration.excludedBuiltin(m,prefix,canonical)){
    // That helper returns the canonical legacy stack only for ambiguous/unsupported aliases.
    if(!canonical.isEmpty()&&ItemStack.matches(canonical,reviewed))return NativeForm.unknown();
    return reviewed.isEmpty()?NativeForm.absent():NativeForm.present(reviewed);
   }
   return reviewed.isEmpty()?(canonical.isEmpty()?direct:NativeForm.absent()):NativeForm.present(reviewed);
  }
  NativeForm purified(Size size,GTMaterial m,int count){
   var selection=purifiedPrefix(size,prefix->output(prefix,m,count).availability());
   return selection.availability()==Availability.PRESENT?output(selection.prefix(),m,count)
       :selection.availability()==Availability.UNKNOWN?NativeForm.unknown():NativeForm.absent();
  }
  NativeForm sand(Size size,GTMaterial m){
   for(var candidate:sandForms(size)){
    NativeForm form=direct(candidate.prefix(),m,candidate.count());
    if(form.availability()!=Availability.ABSENT)return form;
   }
   return NativeForm.absent();
  }
  NativeForm blockDust(GTMaterial m,int count){
   var stack=GTBlocks.getStack(BlockMaterialPrefix.blockDust,m);
   if(stack.isEmpty())return NativeForm.absent();
   if(!(stack.getItem() instanceof BlockItem item)||!(item.getBlock() instanceof MaterialBlockLike form)
       ||form.prefix()!=BlockMaterialPrefix.blockDust||form.prefix().getMaterialWeight()!=9*U
       ||form.material().resolve()!=m.resolve())return NativeForm.unknown();
   stack.setCount(count);return NativeForm.present(stack);
  }
  SourceOreWashingData.Decision tag(GTMaterial m,SourceOreWashingData.Tag tag){
   if(m==null||!m.isValid()||!GTMaterialRegistry.isCoreMaterial(m.resolve())||AddonMaterials.owns(m.resolve()))
    return SourceOreWashingData.Decision.UNKNOWN;
   return SourceOreWashingData.decision(m.getId(),m.getName(),tag);
  }
  FluidStack fluid(String field,int amount){return checkedFluid(field,amount,definitions,fluidLookup);}
 }
 static FluidStack checkedFluid(String field,int amount){return checkedFluid(field,amount,FluidCatalog.all(),GTFluids::stack);}
 static FluidStack checkedFluid(String field,int amount,Map<String,FluidCatalog.FluidEntry> definitions,
                                BiFunction<String,Integer,FluidStack> lookup){
  if(amount<=0||!FluidCatalog.canonicalField(field).equals(field))return FluidStack.EMPTY;
  var entry=definitions.get(field);if(entry==null)return FluidStack.EMPTY;
  var builtin=switch(field){case "Water"->FluidCatalog.Water;case "MnWtr"->FluidCatalog.MnWtr;
   case "DistW"->FluidCatalog.DistW;case "SpDew"->FluidCatalog.SpDew;case "Sluice"->FluidCatalog.Sluice;
   case "Steam"->FluidCatalog.Steam;case "Petrotheum"->FluidCatalog.Petrotheum;case "Cryotheum"->FluidCatalog.Cryotheum;default->null;};
  if(field.equals("GenLiquid_Mercury")){
   if(!ReviewedOreGeneratedFluids.mercury(entry))return FluidStack.EMPTY;
  }else if(entry!=builtin||!FluidCatalog.isBuiltInDefinition(entry))return FluidStack.EMPTY;
  // Reviewed original material identities determine unit conversion; names alone do not establish it.
  var binding=switch(field){case "GenLiquid_Mercury"->ReviewedOreFluidBindings.liquid(800,"Mercury");
   case "Petrotheum"->ReviewedOreFluidBindings.liquid(8245,"Petrotheum");
   case "Cryotheum"->ReviewedOreFluidBindings.liquid(8213,"Cryotheum");default->null;};
  boolean materialLiquid=field.equals("GenLiquid_Mercury")||field.equals("Petrotheum")||field.equals("Cryotheum");
  if(materialLiquid&&(binding==null||!binding.field().equals(field)
      ||!binding.registryPath().equals(entry.registryName())))return FluidStack.EMPTY;
  var stack=lookup.apply(field,amount);if(stack==null||stack.isEmpty()||stack.getAmount()!=amount)return FluidStack.EMPTY;
  String expected=field.equals("Water")?"minecraft:water":"gt6ce:"+FluidCatalog.sanitizePath(entry.registryName());
  if(!BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString().equals(expected))return FluidStack.EMPTY;
  if(!field.equals("Water")&&GTFluids.entryForFluid(stack.getFluid())!=entry)return FluidStack.EMPTY;
  return stack.copy();
 }
}
