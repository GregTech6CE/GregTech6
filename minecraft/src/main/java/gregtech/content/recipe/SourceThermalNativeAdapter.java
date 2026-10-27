package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.FluidCatalog;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTFluids;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialFormItem;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import java.util.*;
import java.util.function.BiFunction;
import static gregtech.api.material.GTValues.U;
import static gregtech.content.recipe.SourceRecyclingFacts.*;

/** Shared source thermal proof for the existing OM-selected1U input and thirteen exact form inputs.
 * No registrations, material mutations, map sweeps or new global unit conventions.
 * Runtime physical stats are retained; imported-stat precision is not claimed source-exact.
 */
final class SourceThermalNativeAdapter {
 private SourceThermalNativeAdapter() {}
 private static final List<Diagnostic> DIAGNOSTICS=new ArrayList<>();
 record Diagnostic(int materialId,String material,Action action,Reason reason) {}
 static List<Diagnostic> diagnostics(){return List.copyOf(DIAGNOSTICS);}
 static void begin(){DIAGNOSTICS.clear();}
 static void note(GTMaterial material,Outcome outcome){
  if(outcome.action()==Action.DEFERRED_NO_ROW||outcome.action()==Action.LEGACY_UNSUPPORTED)
   DIAGNOSTICS.add(new Diagnostic(material.getId(),material.getName(),outcome.action(),outcome.reason()));
 }
 enum Action { ROWS, SOURCE_NO_ROW, DEFERRED_NO_ROW, LEGACY_UNSUPPORTED }
 enum Reason { SOURCE_NOT_MELTING, SOURCE_BLACKLISTED, SOURCE_ZERO_TARGET, SOURCE_NO_DEFAULT_PHASE,
  SOURCE_ZERO_VOLUME, INPUT_ABSENT, REQUIRED_NATIVE_FLUID_ABSENT, UNKNOWN_SOURCE_IDENTITY,
  UNKNOWN_SOURCE_FACT, ITEM_COMPOSITION_OVERRIDE, MATERIAL_TARGET_OVERRIDE,
  MATERIAL_ELIGIBILITY_OVERRIDE, FLUID_BINDING_OVERRIDE, INVALID_RUNTIME_STATS, VERIFIED,
  SOURCE_PREFIX_EXCLUDED, UNSUPPORTED_INPUT_PREFIX }
 record Outcome(Action action,Reason reason,List<NativeRow> rows) {
  Outcome { rows=List.copyOf(rows); }
 }
 record NativeRow(RecipeMap map,ItemStack input,FluidStack output,long ticks,long power) {
  Recipe admit(){
   if(input.isEmpty()||output.isEmpty())return null;
   return map.addRecipe1(true,power,ticks,input,(FluidStack)null,output,new ItemStack[0]);
  }
 }
 private static Outcome result(Action action,Reason reason){return new Outcome(action,reason,List.of());}
 private static Outcome unsupported(Reason reason){return result(Action.LEGACY_UNSUPPORTED,reason);}
 private static Outcome no(Reason reason){return result(Action.SOURCE_NO_ROW,reason);}
 private static Outcome deferred(Reason reason){return result(Action.DEFERRED_NO_ROW,reason);}

 static Outcome inspect(GTMaterial raw,ItemStack selected,Snapshot snapshot){
  return inspect(raw,selected,snapshot,null);
 }
 /** Thirteen independently verified one-item forms share the existing source/phase/override proof.
  * Unsupported contexts have no legacy producer here; callers must not manufacture a fallback.
  */
 static Outcome inspectForm(MaterialPrefix prefix,GTMaterial raw,ItemStack selected,Snapshot snapshot){
  var permission=SourceThermalInputRules.prefixDecision(prefix);
  if(permission==SourceThermalRecyclingRules.Truth.NO)return no(Reason.SOURCE_PREFIX_EXCLUDED);
  if(permission!=SourceThermalRecyclingRules.Truth.YES)return unsupported(Reason.UNSUPPORTED_INPUT_PREFIX);
  return inspect(raw,selected,snapshot,prefix);
 }
 private static Outcome inspect(GTMaterial raw,ItemStack selected,Snapshot snapshot,MaterialPrefix prefix){
  if(raw==null)return unsupported(Reason.UNKNOWN_SOURCE_IDENTITY);
  if(selected==null||selected.isEmpty())return deferred(Reason.INPUT_ABSENT);
  GTMaterial material=raw.resolve();Facts source=find(material.getId(),material.getName());
  if(!identity(material,source==null?null:source.identity()))return unsupported(Reason.UNKNOWN_SOURCE_IDENTITY);
  if(source.portDefaults()==null)return unsupported(Reason.UNKNOWN_SOURCE_FACT); // Output-only Water/Lava.
  Reason changed=snapshot.materialOverride(material,source);
  if(changed!=null)return unsupported(changed);
  long inputUnits=U;
  if(prefix==null){
   if(!snapshot.input(material,selected))return unsupported(Reason.ITEM_COMPOSITION_OVERRIDE);
  }else{
   inputUnits=snapshot.inputUnits(material,prefix,selected);
   if(inputUnits!=SourceThermalInputRules.form(prefix).sourceUnits())return unsupported(Reason.ITEM_COMPOSITION_OVERRIDE);
  }
  if(source.melting()==Decision.NO)return no(Reason.SOURCE_NOT_MELTING);
  if(source.blacklistedSmelter()==Decision.YES)return no(Reason.SOURCE_BLACKLISTED);
  if(source.targetUnitsPerU()==0)return no(Reason.SOURCE_ZERO_TARGET);
  if(source.melting()!=Decision.YES||source.blacklistedSmelter()!=Decision.NO||source.targetUnitsPerU()<0)
   return unsupported(Reason.UNKNOWN_SOURCE_FACT);
  GTMaterial target=GTMaterialRegistry.get(source.target().name());
  Facts targetSource=find(source.target().id(),source.target().name());
  if(!identity(target,source.target())||targetSource==null)return unsupported(Reason.UNKNOWN_SOURCE_IDENTITY);
  changed=snapshot.materialOverride(target,targetSource);
  if(changed!=null)return unsupported(changed);
  Phase phase=targetSource.phase();
  if(phase.kind()==PhaseKind.UNKNOWN||phase.kind()==PhaseKind.OPTIONAL_EXTERNAL)
   return unsupported(Reason.UNKNOWN_SOURCE_FACT);
  if(phase.kind()==PhaseKind.NO_DEFAULT_PHASE){
   return snapshot.defaultNoPhase(target,targetSource)?no(Reason.SOURCE_NO_DEFAULT_PHASE)
       :unsupported(Reason.FLUID_BINDING_OVERRIDE);
  }
  Binding sourceBinding=phase.binding();
  if(sourceBinding==null)return unsupported(Reason.UNKNOWN_SOURCE_FACT);
  NativeBinding binding=nativeBinding(target,sourceBinding);
  if(binding==null)return deferred(Reason.REQUIRED_NATIVE_FLUID_ABSENT);
  var entry=snapshot.definition(binding.field());
  if(entry==null)return deferred(Reason.REQUIRED_NATIVE_FLUID_ABSENT);
  if(!snapshot.owned(target,binding,entry)||!snapshot.noExternalPhase(target,targetSource,binding.field()))
   return unsupported(Reason.FLUID_BINDING_OVERRIDE);
  FluidStack fluid=snapshot.fluid(binding.field(),1);
  if(fluid==null||fluid.isEmpty())return deferred(Reason.REQUIRED_NATIVE_FLUID_ABSENT);
  if(!snapshot.holder(binding,entry,fluid))return unsupported(Reason.FLUID_BINDING_OVERRIDE);
  boolean calcite=targetSource.target().id()==9107&&targetSource.target().name().equals("Calcite");
  if(!calcite&&targetSource.furnace()==Decision.UNKNOWN)return unsupported(Reason.UNKNOWN_SOURCE_FACT);
  var facts=new SourceThermalRecyclingRules.Facts(truth(source.melting()),truth(source.blacklistedSmelter()),
      source.targetUnitsPerU(),truth(targetSource.furnace()),calcite?SourceThermalRecyclingRules.Truth.YES:SourceThermalRecyclingRules.Truth.NO);
  var stats=snapshot.stats(material,target);
  var proof=new SourceThermalRecyclingRules.Phase(SourceThermalRecyclingRules.PhaseProof.VERIFIED_SINGLE_BINDING,
      sourceBinding.referenceMaterialUnits(),sourceBinding.millibuckets(),snapshot.temperature(fluid),fluid.getFluid()==Fluids.LAVA);
  var plan=prefix==null
      ?SourceThermalRecyclingRules.plan(SourceThermalRecyclingRules.InputProof.VERIFIED_SELF_ONE_UNIT,facts,stats,proof)
      :SourceThermalInputRules.plan(prefix,SourceThermalInputRules.InputProof.VERIFIED_SINGLE_COMPONENT,inputUnits,facts,stats,proof);
  if(plan.decision()==SourceThermalRecyclingRules.Decision.UNSUPPORTED)return deferred(Reason.INVALID_RUNTIME_STATS);
  if(plan.decision()==SourceThermalRecyclingRules.Decision.SUPPORTED_NO_ROW)return no(Reason.SOURCE_ZERO_VOLUME);
  var output=fluid.copy();output.setAmount(plan.row().millibuckets());
  var rows=new ArrayList<NativeRow>();
  if(plan.row().melter())rows.add(new NativeRow(MachineRecipeMaps.Melter,selected.copy(),output.copy(),plan.row().ticks(),plan.row().power()));
  rows.add(new NativeRow(MachineRecipeMaps.Smelter,selected.copy(),output.copy(),plan.row().ticks(),plan.row().power()));
  return new Outcome(Action.ROWS,Reason.VERIFIED,rows);
 }
 private static SourceThermalRecyclingRules.Truth truth(Decision decision){
  return switch(decision){case YES->SourceThermalRecyclingRules.Truth.YES;case NO->SourceThermalRecyclingRules.Truth.NO;case UNKNOWN->SourceThermalRecyclingRules.Truth.UNKNOWN;};
 }
 private static boolean identity(GTMaterial m,Identity source){
  return m!=null&&source!=null&&m.isValid()&&m.resolve()==m&&m.getId()==source.id()&&m.getName().equals(source.name())
      &&GTMaterialRegistry.isCoreMaterial(m)&&!AddonMaterials.owns(m)&&!m.has(MaterialProperty.ANTIMATTER);
 }
 record NativeBinding(String field,String registry,FluidCatalog.FluidEntry declaration,boolean finalized) {}
 private static NativeBinding declared(String field,String registry,FluidCatalog.FluidEntry declaration){
  return new NativeBinding(field,registry,declaration,false);
 }
 private static NativeBinding finalized(String name,FluidCatalog.FluidEntry declaration){
  return new NativeBinding("GenMolten_"+name,"molten."+name.toLowerCase(Locale.ROOT),declaration,true);
 }
 /** Explicit native spellings are bindings, not newly registered aliases or material-name guesses. */
 static NativeBinding nativeBinding(GTMaterial target,Binding source){
  NativeBinding special=switch(target.getId()){
   case 300->declared("Zinc","molten.zinc",FluidCatalog.Zinc);
   case 8001->declared("Glass","glass",FluidCatalog.Glass);
   case 8211->declared("Blaze","blaze",FluidCatalog.Blaze);
   case 8213->declared("Cryotheum","cryotheum",FluidCatalog.Cryotheum);
   case 8218->declared("Plastic","molten.plastic",FluidCatalog.Plastic);
   case 8246->declared("Aerotheum","aerotheum",FluidCatalog.Aerotheum);
   case 8318->declared("Ender","molten.enderpearl",FluidCatalog.Ender);
   case 8333->declared("Redstone","molten.redstone",FluidCatalog.Redstone);
   case 8620->declared("Brass","molten.brass",FluidCatalog.Brass);
   case 8637->declared("HSLA_Molten","molten.hsla",FluidCatalog.HSLA_Molten);
   case 9107->declared("Calcite","molten.calcite",FluidCatalog.Calcite);
   case 9800->declared("Water","water",FluidCatalog.Water);
   case 9810->declared("Lava","lava",FluidCatalog.Lava);
   case 9883->declared("CFoam","ic2constructionfoam",FluidCatalog.CFoam);
   case 8217->finalized("Rubber",FluidCatalog.Molten_Rubber);
   case 8235->finalized("Wax",FluidCatalog.Molten_Wax);
   case 8236->finalized("WaxBee",FluidCatalog.Molten_WaxBee);
   case 8237->finalized("WaxRefractory",FluidCatalog.Molten_WaxRefractory);
   case 8238->finalized("WaxParaffin",FluidCatalog.Molten_WaxParaffin);
   case 8239->finalized("WaxPlant",FluidCatalog.Molten_WaxPlant);
   case 8240->finalized("WaxMagic",FluidCatalog.Molten_WaxMagic);
   case 8280->finalized("WaxAmnesic",FluidCatalog.Molten_WaxAmnesic);
   case 8281->finalized("WaxSoulful",FluidCatalog.Molten_WaxSoulful);
   case 9703->finalized("Sugar",FluidCatalog.Molten_Sugar);
   case 9780->finalized("Cheese",FluidCatalog.Molten_Cheese);
   case 9783->finalized("Chocolate",FluidCatalog.Molten_Chocolate);
   // These original molten salts retain their established port liquid IDs, with source144/U.
   case 8009,8010,8029,8033->new NativeBinding("GenLiquid_"+target.getName(),target.getName().toLowerCase(Locale.ROOT),null,false);
   default->null;
  };
  if(special!=null)return special;
  String lower=target.getName().toLowerCase(Locale.ROOT),path=source.sourceRegistryPath();
  if(path.equals("molten."+lower))return new NativeBinding("GenMolten_"+target.getName(),path,null,false);
  if(path.equals(lower))return new NativeBinding(FluidCatalog.canonicalField("GenLiquid_"+target.getName()),path,null,false);
  return null;
 }

 static class Snapshot {
  final Map<String,FluidCatalog.FluidEntry> definitions;
  private final BiFunction<String,Integer,FluidStack> fluids;
  Snapshot(){this(FluidCatalog.all(),GTFluids::stack);}
  Snapshot(Map<String,FluidCatalog.FluidEntry> definitions,BiFunction<String,Integer,FluidStack> fluids){
   this.definitions=Map.copyOf(definitions);this.fluids=fluids;
  }
  FluidCatalog.FluidEntry definition(String field){return definitions.get(field);}
  FluidStack fluid(String field,int amount){return fluids.apply(field,amount);}
  int temperature(FluidStack stack){return stack.getFluid().getFluidType().getTemperature(stack);}
  SourceThermalRecyclingRules.Stats stats(GTMaterial input,GTMaterial target){
   return new SourceThermalRecyclingRules.Stats(input.getDensity(),target.getMeltingPoint());
  }
  Reason materialOverride(GTMaterial material,Facts source){
   PortDefaults defaults=contextDefaults(source.identity().id(),source.identity().name());
   if(defaults==null)return Reason.UNKNOWN_SOURCE_FACT;
   if(material.has(MaterialProperty.MELTING)!=defaults.melting())return Reason.MATERIAL_ELIGIBILITY_OVERRIDE;
   var target=material.getTargetSmeltingMaterial();
   if(target==null||!target.isValid())return Reason.MATERIAL_TARGET_OVERRIDE;target=target.resolve();
   return defaults.matches(material.has(MaterialProperty.MELTING),target.getId(),target.getName(),material.getTargetSmeltingAmount())
       ?null:Reason.MATERIAL_TARGET_OVERRIDE;
  }
  boolean input(GTMaterial material,ItemStack stack){
   return validatedUnits(material,stack,Set.of(MaterialPrefix.dust,MaterialPrefix.dustSmall,MaterialPrefix.dustTiny,
       MaterialPrefix.dustDiv72,MaterialPrefix.ingot,MaterialPrefix.chunkGt,MaterialPrefix.nugget))==U;
  }
  /** Do not rescale the stack to a synthetic1U. Prefix and explicit original composition must
   * describe this actual single selected item, including clean-ore fractions greater than1U. */
  long inputUnits(GTMaterial material,MaterialPrefix prefix,ItemStack stack){
   var source=SourceThermalInputRules.form(prefix);
   if(source==null||stack==null||stack.isEmpty()||stack.getCount()!=1)return -1;
   long amount=validatedUnits(material,stack,Set.of(prefix));
   return amount==source.sourceUnits()?amount:-1;
  }
  private long validatedUnits(GTMaterial material,ItemStack stack,Set<MaterialPrefix> allowed){
   if(stack.isEmpty()||stack.isDamaged()||ItemMaterialRegistry.hasStoredContents(stack)
       ||!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("gt6ce"))return -1;
//? if >=1.21.1 {
   /*if(!stack.getComponentsPatch().isEmpty())return -1;
*///?} else {
   if(stack.hasTag())return -1;
//?}
   if(!(stack.getItem() instanceof MaterialFormItem form)||form.getMaterial().resolve()!=material)return -1;
   if(!allowed.contains(form.getPrefix())
       ||GTItems.getStack(form.getPrefix(),material,stack.getCount()).getItem()!=stack.getItem())return -1;
   var data=ItemMaterialRegistry.explicit(stack.getItem()).orElse(null);
   if(data==null||!data.recoverable()||data.prefix()!=form.getPrefix()||data.components().size()!=1
       ||data.material().resolve()!=material||data.amount()!=form.getPrefix().getMaterialWeight())return -1;
   try{return Math.multiplyExact((long)stack.getCount(),data.amount());}
   catch(ArithmeticException invalid){return -1;}
  }
  boolean owned(GTMaterial material,NativeBinding binding,FluidCatalog.FluidEntry entry){
   String field=binding.field();
   if(entry!=FluidCatalog.get(field)||!FluidCatalog.canonicalField(field).equals(field)
       ||!entry.registryName().equals(binding.registry()))return false;
   if(binding.declaration()!=null){
    if(!FluidCatalog.isBuiltInDefinition(entry))return false;
    if(!binding.finalized())return entry==binding.declaration();
    int melting=material.getMeltingPoint();
    int temperature=melting<=0?1000:melting<300?Math.min(300,material.getBoilingPoint()-1):melting;
    // Static createMolten declarations are immutable; preparation copies only these two fields.
    return entry.equals(binding.declaration().withTemperature(temperature).withLuminosity(10));
   }
   return material.getName().equals(entry.materialKey())
       &&(FluidCatalog.isGeneratedMoltenDefinition(field,entry)||FluidCatalog.isGeneratedGasLiquidDefinition(field,entry));
  }
  boolean holder(NativeBinding binding,FluidCatalog.FluidEntry entry,FluidStack stack){
   if(stack.getAmount()!=1)return false;
   if(entry.isVanillaWater())return stack.getFluid()==Fluids.WATER;
   if(entry.isVanillaLava())return stack.getFluid()==Fluids.LAVA;
   return BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString().equals("gt6ce:"+FluidCatalog.sanitizePath(binding.registry()))
       &&GTFluids.entryForFluid(stack.getFluid())==entry;
  }
  boolean defaultNoPhase(GTMaterial material,Facts source){return noExternalPhase(material,source,null);}
  boolean noExternalPhase(GTMaterial material,Facts source,String selectedField){
   String name=material.getName(),lower=name.toLowerCase(Locale.ROOT);
   for(var definition:definitions.entrySet()){
    String field=definition.getKey();var entry=definition.getValue();
    String bound=entry.materialKey()!=null?entry.materialKey():FluidCatalog.boundMaterial(entry.registryName());
    boolean related=bound!=null&&GTMaterialRegistry.get(bound).resolve()==material
        ||field.equals("GenMolten_"+name)||field.equals("GenLiquid_"+name)||field.equals("GenGas_"+name)
        ||entry.registryName().equals(lower)||entry.registryName().equals("molten."+lower);
    if(!related||field.equals(selectedField))continue;
    // A proven source NO may coexist with the port's known over-broad molten factory artifact.
    if(defaultMolten(material,source,field,entry))continue;
    // Loader_Fluids145-153: redstone's optional liquid is superseded by molten.redstone;
    // glowstone's optional entry is gas-only. These unchanged directory aliases are not overrides.
    if(material.getId()==8333&&field.equals("Redstone_TE")&&entry==FluidCatalog.Redstone_TE
        ||material.getId()==8341&&field.equals("Glowstone_TE")&&entry==FluidCatalog.Glowstone_TE)continue;
    // The immutable short-name Alumina compatibility declaration is not its source phase choice.
    if(material.getId()==8008&&field.equals("GenMolten_Al2O3")&&entry==FluidCatalog.get(field)
        &&entry.equals(FluidCatalog.Molten_Al2O3.withTemperature(material.getMeltingPoint()).withLuminosity(10)))continue;
    // ConstructionFoam's old port gas is a known default artifact; thermal uses the source CFoam.
    if(material.getId()==9883&&field.equals("GenGas_ConstructionFoam")
        &&FluidCatalog.isGeneratedGasLiquidDefinition(field,entry)&&"ConstructionFoam".equals(entry.materialKey()))continue;
    return false;
   }
   return true;
  }
  private boolean defaultMolten(GTMaterial material,Facts source,String field,FluidCatalog.FluidEntry entry){
   return source.portDefaults()!=null&&source.portDefaults().generatedMolten()
       &&field.equals("GenMolten_"+material.getName())&&material.getName().equals(entry.materialKey())
       &&entry.registryName().equals("molten."+material.getName().toLowerCase(Locale.ROOT))
       &&FluidCatalog.isGeneratedMoltenDefinition(field,entry);
  }
 }
}
