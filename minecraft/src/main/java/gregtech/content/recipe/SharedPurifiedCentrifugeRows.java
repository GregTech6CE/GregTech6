package gregtech.content.recipe;

import gregtech.api.material.*;
import gregtech.engine.material.*;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.content.data.*;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTFluids;
import gregtech.util.OM;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import java.util.*;
import static gregtech.content.recipe.SourcePurifiedCentrifugePlan.*;
import static gregtech.api.material.GTValues.U9;

/** Shared purified Centrifuge producer. No named-material recipe eligibility roster.
 * Source Ores385-387,448-470. Unknown phase retains only this producer's exact legacy definition.
 * All accepted rows are identity-owned and excluded from baseline capture before tag reload.
 */
public final class SharedPurifiedCentrifugeRows {
 private SharedPurifiedCentrifugeRows() {}
 private static final Set<Recipe> OWNED=Collections.newSetFromMap(new IdentityHashMap<>());
 private static final Map<GTMaterial,String> STATUS=new IdentityHashMap<>();
 private static List<GTMaterial> materials=List.of();
 private static boolean initialized;
 private record Definition(boolean optimize,ItemStack input,ItemStack[] outputs,long[] chances,
                           FluidStack[] fluids,long duration,long power) {
  Recipe admit(RecipeMap map) {return map.addRecipe(optimize,new ItemStack[]{input.copy()},outputs,null,chances,
      new FluidStack[0],fluids,duration,power,0);}
 }
 public static Set<Recipe> owned(){var out=Collections.newSetFromMap(new IdentityHashMap<Recipe,Boolean>());out.addAll(OWNED);return Collections.unmodifiableSet(out);}
 public static Map<GTMaterial,String> statuses(){return Collections.unmodifiableMap(new IdentityHashMap<>(STATUS));}
 public static void initialize(){
  if(initialized)throw new IllegalStateException("Centrifuge initialized twice");initialized=true;
  var seen=Collections.newSetFromMap(new IdentityHashMap<GTMaterial,Boolean>());var list=new ArrayList<GTMaterial>();
  for(var m:GTMaterialRegistry.allMaterials())if(seen.add(m.resolve()))list.add(m.resolve());materials=List.copyOf(list);rebuild();
 }
 public static void clear(){for(var row:OWNED)MachineRecipeMaps.Centrifuge.unlink(row);OWNED.clear();STATUS.clear();}
 public static void rebuild(){
  if(!initialized)return;
  // Resolve every definition before changing any map. Every reload uses fresh selected forms/bindings.
  var definitions=new ArrayList<Definition>();var status=new IdentityHashMap<GTMaterial,String>();
  var snapshot=new Snapshot();
  for(var m:materials){
   var outcome=plan(new Request<>(m,m.isValid(),m.has(MaterialProperty.ANTIMATTER),
       m==GTMaterialRegistry.get("Bedrock"),m.getToolQuality(),m.getByProducts().stream().map(GTMaterial::resolve).toList()),snapshot);
   status.put(m,outcome.status()+":"+outcome.reason());
   if(outcome.status()==Status.PLANNED){
    for(var v:outcome.variants()){
     ItemStack[] outputs=new ItemStack[v.rawItemSlots().size()];
     for(int j=0;j<outputs.length;j++){var slot=v.rawItemSlots().get(j);outputs[j]=slot.present()?snapshot.item(slot.bindingKey(),slot.count()):ItemStack.EMPTY;}
     FluidStack[] fluids=new FluidStack[v.fluidOutputs().size()];
     for(int j=0;j<fluids.length;j++){var f=v.fluidOutputs().get(j);fluids[j]=snapshot.fluids.get(f.bindingKey()).copy();fluids[j].setAmount(f.millibuckets());}
     definitions.add(new Definition(false,snapshot.item(v.inputBinding(),1),outputs,v.rawChances().stream().mapToLong(Long::longValue).toArray(),fluids,v.duration(),v.nativeRecipePower()));
    }
   }else if(outcome.status()==Status.UNRESOLVED){
    // One pre-admission compatibility branch, NEVER a collision fallback. Same known OPL shape,
    // not discovered by scanning rows. No NonOreMacerationRows definition can resurrect this producer.
    Definition legacy=legacy(m);if(legacy!=null){definitions.add(legacy);status.put(m,status.get(m)+":LEGACY_COMPATIBILITY_NOT_ALIGNED");}
   }
  }
  clear();STATUS.clear();STATUS.putAll(status);
  try{for(var definition:definitions){var row=definition.admit(MachineRecipeMaps.Centrifuge);if(row!=null)OWNED.add(row);}}
  catch(RuntimeException|Error failure){clear();throw failure;}
 }
 /** Existing selected getter first, then exactly one explicitly registered equal-form fallback.
  * No namespace/addon blanket ban and no generation permission check. Ambiguous foreign choices defer.
  */
 static final class Snapshot implements Resolver<GTMaterial>{
  final Map<String,ItemStack> items=new HashMap<>();final Map<String,FluidStack> fluids=new HashMap<>();
  final Map<GTMaterial,EnumMap<Form,FormResult>> formMemo=new IdentityHashMap<>();
  final Map<GTMaterial,Environment> environmentMemo=new IdentityHashMap<>();
  final Map<String,FluidCatalog.FluidEntry> fluidDefinitions;
  final Map<GTMaterial,Map<MaterialPrefix,List<ItemStack>>> explicit=new IdentityHashMap<>();
  Snapshot(){this(FluidCatalog.all());}
  Snapshot(Map<String,FluidCatalog.FluidEntry> definitions){
   fluidDefinitions=Map.copyOf(definitions);
   // One explicit registry pass per rebuild, never per output slot or material.
   for(var item:ItemMaterialRegistry.entries().keySet()){
    var stack=new ItemStack(item);var form=MaterialEquivalence.form(stack);
    if(form!=null)explicit.computeIfAbsent(form.material().resolve(),k->new IdentityHashMap<>())
        .computeIfAbsent(form.prefix(),k->new ArrayList<>()).add(stack);
   }
  }
  @Override public FormResult form(GTMaterial m,Form role){
   var memo=formMemo.computeIfAbsent(m,k->new EnumMap<>(Form.class));
   if(memo.containsKey(role))return memo.get(role);
   FormResult result=resolveForm(m,role);memo.put(role,result);return result;
  }
  private FormResult resolveForm(GTMaterial m,Form role){
   MaterialPrefix prefix=switch(role){case DUST->MaterialPrefix.dust;case DUST_TINY->MaterialPrefix.dustTiny;case PURIFIED->MaterialPrefix.crushedPurified;case PURIFIED_TINY->MaterialPrefix.crushedPurifiedTiny;case REFINED_TINY->MaterialPrefix.crushedCentrifugedTiny;};
   ItemStack selected=GTItems.getStack(prefix,m,1);
   if(selected.isEmpty()){
    var candidates=explicit.getOrDefault(m,Map.of()).getOrDefault(prefix,List.of());
    if(candidates.size()>1)return FormResult.unknown();
    if(candidates.size()==1)selected=candidates.get(0);
   }
   if(selected.isEmpty())return FormResult.absent();
   String key=prefix.getName()+"/"+m.getId()+"/"+m.getName();items.put(key,selected.copy());return FormResult.present(key);
  }
  ItemStack item(String key,int count){ItemStack out=items.get(key).copy();out.setCount(count);return out;}
  @Override public Environment environment(GTMaterial m,int kelvin){
   if(kelvin!=AMBIENT_K)return Environment.unknown("unsupported-environment-query");
   return environmentMemo.computeIfAbsent(m,key->resolveEnvironment(key,kelvin));
  }
  private Environment resolveEnvironment(GTMaterial m,int kelvin){
   NoPhaseBinding noPhase=defaultNone(m);
   if(noPhase!=null&&!defaultNoneCompatible(m,noPhase))
       return Environment.unknown("source-default-NONE-provenance-or-observed-override");
   PhaseBinding declared=binding(m,null);
   // Restrict provenance checks to these source-positive phase DATA, never all addon materials.
   if(declared!=null&&(!GTMaterialRegistry.isCoreMaterial(m)||AddonMaterials.owns(m)
       ||m.getMeltingPoint()!=declared.melting||m.getBoilingPoint()!=declared.boiling))
       return Environment.unknown("source-phase-material-provenance-or-thermal-override");
   if(m.getBoilingPoint()<m.getMeltingPoint()||m.getMeltingPoint()<0)return Environment.unknown("non-source-ordered-thermal-metadata");
   // Source heat/default/composition invariants guarantee plasma>=boiling; exact plasma accessor
   // is unnecessary for this solid branch, not an excuse to guess a low-temperature phase.
   if(kelvin<m.getMeltingPoint()&&kelvin<m.getBoilingPoint())return Environment.item("source-thermal-order-solid");
   String phase=kelvin>=m.getBoilingPoint()?"gas":"liquid";
   if(noPhase!=null&&noPhase.phase.equals(phase))
       return Environment.item("source-explicit-default-NONE-not-empty-native-query");
   PhaseBinding binding=binding(m,phase);if(binding==null)return Environment.unknown("source-phase-binding-undeclared");
   if(!sourceBindingOwned(binding))return Environment.unknown("source-fluid-binding-override-or-provenance");
   FluidStack stack=GTFluids.stack(binding.field,1);
   if(stack==null||stack.isEmpty()||!BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString().equals(binding.nativeId))return Environment.unknown("source-exact-fluid-holder-unavailable");
   fluids.put(binding.field,stack.copy());return Environment.fluid(binding.field,648648000L,binding.mb);
  }
  private boolean defaultNoneCompatible(GTMaterial m,NoPhaseBinding source){
   if(!sourceNoneMaterialCompatible(source,sourceNoneFacts(m)))return false;
   String lower=source.name.toLowerCase(Locale.ROOT);
   Set<String> relatedFields=Set.of(source.name,"GenGas_"+source.name,"GenLiquid_"+source.name,
       "GenMolten_"+source.name,"GenPlasma_"+source.name);
   Set<String> relatedNames=Set.of(lower,"molten."+lower,"vapor."+lower,"plasma."+lower);
   for(var definition:fluidDefinitions.entrySet()){
    String field=definition.getKey();var entry=definition.getValue();
    if(!source.name.equals(entry.materialKey())&&!relatedFields.contains(field)
        &&!relatedFields.contains(FluidCatalog.canonicalField(field))&&!relatedNames.contains(entry.registryName()))continue;
    // A port factory artifact is not a source phase binding. This is material-independent:
    // it is allowed only when this material already has an explicit proven source NONE declaration.
    int expectedTemperature=source.melting<=0?1000:source.melting<300?Math.min(300,source.boiling-1):source.melting;
    boolean knownPortArtifact=field.equals("GenMolten_"+source.name)
        &&FluidCatalog.isGeneratedMoltenDefinition(field,entry)
        &&entry.registryName().equals("molten."+lower)&&source.name.equals(entry.materialKey())
        &&entry.textureMode()==FluidCatalog.FluidTextureMode.GENERIC_MOLTEN&&!entry.gas()
        &&entry.flags()==FluidCatalog.FluidFlags.LIQUID&&entry.temperature()==expectedTemperature
        &&entry.luminosity()==10;
    if(!knownPortArtifact)return false; // Explicit/aliased/foreign candidate is an override, not NONE.
   }
   return true;
  }

 }
 // Source DATA only, not recipe/material admission: explicit fluids and default gas registrations
 // cover the six ambient positive bindings in current539 input observations. Other phases defer.
 private record PhaseBinding(int id,String name,String phase,String field,String nativeId,int mb,int melting,int boiling){}
 private static final List<PhaseBinding> PHASES=List.of(
  new PhaseBinding(21,"Helium3","gas","GenGas_Helium3","gt6ce:helium3",1000,1,4),
  new PhaseBinding(90,"Fluorine","gas","GenGas_Fluorine","gt6ce:fluorine",1000,53,85),
  new PhaseBinding(800,"Mercury","liquid","GenLiquid_Mercury","gt6ce:mercury",144,234,629),
  new PhaseBinding(9791,"Honey","liquid","Honey","gt6ce:for_honey",1000,273,373),
  new PhaseBinding(9793,"Honeydew","liquid","Honeydew","gt6ce:honeydew",1000,273,373),
  new PhaseBinding(9802,"Ice","liquid","Ice","gt6ce:ice",1000,273,373));
 private static PhaseBinding binding(GTMaterial m,String phase){for(var b:PHASES)if(b.id==m.getId()&&b.name.equals(m.getName())&&(phase==null||b.phase.equals(phase)))return b;return null;}
 private static boolean sourceBindingOwned(PhaseBinding binding){
  var entry=FluidCatalog.get(binding.field);
  if(entry==null||!FluidCatalog.canonicalField(binding.field).equals(binding.field))return false;
  if(binding.id==800)return ReviewedOreGeneratedFluids.mercury(entry);
  if(binding.id==21||binding.id==90)return FluidCatalog.isGeneratedGasLiquidDefinition(binding.field,entry)
      &&binding.name.equals(entry.materialKey())&&entry.gas()
      &&entry.registryName().equals(binding.nativeId.substring("gt6ce:".length()));
  var builtin=switch(binding.id){case 9791->FluidCatalog.Honey;case 9793->FluidCatalog.Honeydew;case 9802->FluidCatalog.Ice;default->null;};
  return entry==builtin&&FluidCatalog.isBuiltInDefinition(entry);
 }
 // Explicit source material-phase data, not a whitelist or exception in the recipe algorithm.
 // Source proof: full official1229/W1306 Java binding scan plus MT generator-tag/factory review.
 // Native empty field queries are NEVER used to prove these NONE declarations.
 record NoPhaseBinding(int id,String name,String phase,int melting,int boiling){}
 // The runtime calls this exact pure guard; isolated contract variants never mutate core materials.
 record SourceNoneFacts(int id,String name,boolean core,boolean addon,boolean valid,int melting,int boiling,
                        Set<MaterialProperty> properties){
  SourceNoneFacts{properties=Set.copyOf(properties);}
 }
 static SourceNoneFacts sourceNoneFacts(GTMaterial m){return new SourceNoneFacts(m.getId(),m.getName(),
     GTMaterialRegistry.isCoreMaterial(m),AddonMaterials.owns(m),m.isValid(),m.getMeltingPoint(),
     m.getBoilingPoint(),m.getProperties());}
 static boolean sourceNoneMaterialCompatible(NoPhaseBinding source,SourceNoneFacts facts){
  if(source.id!=facts.id||!source.name.equals(facts.name)||!facts.core||facts.addon||!facts.valid
      ||facts.melting!=source.melting||facts.boiling!=source.boiling)return false;
  return source.phase.equals("gas")?!facts.properties.contains(MaterialProperty.GAS)
      :!facts.properties.contains(MaterialProperty.LIQUID)&&!facts.properties.contains(MaterialProperty.MOLTEN);
 }
 private static final List<NoPhaseBinding> DEFAULT_NONE=List.of(
  new NoPhaseBinding(1120,"Copernicium","liquid",150,357),
  new NoPhaseBinding(1180,"Oganesson","gas",258,263),
  new NoPhaseBinding(1190,"Ununennium","liquid",290,903),
  new NoPhaseBinding(9110,"Celestine","liquid",275,455),
  new NoPhaseBinding(9199,"DuraniumHexafluoride","liquid",216,428),
  new NoPhaseBinding(9801,"Snow","liquid",273,373),
  new NoPhaseBinding(9851,"OilSand","liquid",100,400),
  new NoPhaseBinding(9852,"CrudeOil","liquid",100,400));
 static NoPhaseBinding defaultNone(GTMaterial m){for(var d:DEFAULT_NONE)if(d.id==m.getId()&&d.name.equals(m.getName()))return d;return null;}
 private static Definition legacy(GTMaterial m){
  if(!m.isValid()||!MaterialPrefix.crushed.isValidFor(m)||!MaterialPrefix.crushedCentrifuged.isValidFor(m))return null;
  // Preserve old producer suppression decisions only in its unresolved compatibility branch.
  if(NonOreMacerationRows.reviewed(m))return null;
  var input=GTItems.getStack(MaterialPrefix.crushedPurified,m,1);var output=GTItems.getStack(MaterialPrefix.crushedCentrifuged,m,1);
  if(input.isEmpty()||output.isEmpty())return null;
  ItemStack side=OM.dustOrIngot(m,U9);var by=m.getByProducts();
  for(int index=0;index<2&&index<by.size();index++){var b=by.get(index);if(b!=null&&b.isValid()){var candidate=OM.dustOrIngot(b,U9);if(!candidate.isEmpty())side=candidate;}}
  return new Definition(true,input,new ItemStack[]{output,side},new long[]{10000,5000},new FluidStack[0],120,16);
 }
}
