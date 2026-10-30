package gregtech.content.recipe;

import static gregtech.api.material.GTValues.U9;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.block.OreBlock;
import gregtech.content.block.OreHostStone;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.util.OM;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import java.util.*;
import java.util.function.Predicate;
import static gregtech.content.recipe.SourceOreSiftingRules.*;
import static gregtech.content.recipe.SourceOreWashingRules.Availability;
import static gregtech.content.recipe.SharedOreWashingRows.NativeForm;

/** Source fixed purified Sifter calls precede the generic producer, using ordinary collision checks.
 * Generic grades remain startup-captured; only eligibility and fixed native holders rebind on reload.
 */
public final class SharedOreSiftingRows {
 private SharedOreSiftingRows() {}
 private static final Set<Recipe> OWNED=Collections.newSetFromMap(new IdentityHashMap<>());
 private static final List<Deferred> DEFERRED=new ArrayList<>();
 private static List<GTMaterial> materials=List.of();
 private static Map<GTMaterial,GenericOutputs> startupOutputs=Map.of();
 private static boolean initialized;
 public record Deferred(String material,String producer,String reason) {}
 public static Set<Recipe> owned(){
  Set<Recipe> out=Collections.newSetFromMap(new IdentityHashMap<>());out.addAll(OWNED);
  return Collections.unmodifiableSet(out);
 }
 public static List<Deferred> deferred(){return List.copyOf(DEFERRED);}
 public static void initialize(){
  if(initialized)throw new IllegalStateException("Sifting rows initialized twice");initialized=true;
  Set<GTMaterial> seen=Collections.newSetFromMap(new IdentityHashMap<>());var list=new ArrayList<GTMaterial>();
  for(var m:GTMaterialRegistry.allMaterials())if(seen.add(m.resolve()))list.add(m.resolve());
  materials=List.copyOf(list);
  // Same immutable startup output policy as the old direct/dormant OPL definitions. Late crafting
  // aliases may resume an input chain but cannot silently replace its reviewed graded outputs.
  startupOutputs=captureGeneric(materials,new Snapshot());rebuild();
 }
 private static void clearOwned(){OWNED.forEach(MachineRecipeMaps.Sifting::unlink);OWNED.clear();}
 /** Keep startup definitions for the next server in this JVM, without stale accepted identities. */
 public static void clear(){clearOwned();DEFERRED.clear();}
 public static void rebuild(){
  if(!initialized)return;
  var snapshot=new Snapshot();var planned=definitions(materials,snapshot);
  clearOwned();DEFERRED.clear();DEFERRED.addAll(snapshot.siftingDeferred);
  try{for(var definition:planned){var row=definition.admit();if(row!=null)OWNED.add(row);}}
  catch(RuntimeException|Error failure){clear();throw failure;}
 }
 record Definition(boolean optimize,ItemStack input,ItemStack[] outputs,long[] chances,long duration,long power,boolean special,
                   Predicate<ItemStack> inputPredicate){
  Definition(boolean optimize,ItemStack input,ItemStack[] outputs,long[] chances,long duration,long power,boolean special){
   this(optimize,input,outputs,chances,duration,power,special,null);
  }
  Recipe admit(){
   // Source descriptors retain their literal chance arrays. The native map normalizes them to the
   // effective output slots; unused long quartz tails are not a second runtime metadata contract.
   Recipe row=RecipeMap.make(optimize,new ItemStack[]{input},outputs,null,chances,
       new FluidStack[0],new FluidStack[0],duration,power,0);
   if(row==null)return null;
   // The host-tagged representative distinguishes the row; the predicate lets a stack with the
   // same host but different stored data (or an older tag format) match at runtime.
   if(inputPredicate!=null)row.withSameItemInputPredicate(0,inputPredicate);
   return MachineRecipeMaps.Sifting.addRecipe(row);
  }
 }
 record GenericOutputs(Availability availability,ItemStack[] outputs){
  GenericOutputs{outputs=copies(outputs);}
  @Override public ItemStack[] outputs(){return copies(outputs);}
 }
 static Map<GTMaterial,GenericOutputs> captureGeneric(Collection<GTMaterial> selected,Snapshot snapshot){
  var captured=new IdentityHashMap<GTMaterial,GenericOutputs>();
  for(var material:selected){
   GTMaterial m=material.resolve();var gem=snapshot.direct(MaterialPrefix.gem,m,1);var dust=snapshot.direct(MaterialPrefix.dust,m,1);
   Availability availability=SourceOreWashingRules.requireAll(gem.availability(),dust.availability());
   if(availability!=Availability.PRESENT){captured.put(m,new GenericOutputs(availability,new ItemStack[0]));continue;}
   var outputs=new ItemStack[genericOutputs().size()];
   for(int index=0;index<outputs.length;index++){
    var rule=genericOutputs().get(index);
    if(rule.fallbackGemCount()>0)outputs[index]=snapshot.grade(rule.prefix(),m,rule.count(),gem.stack(),rule.fallbackGemCount());
    else outputs[index]=(rule.prefix()==MaterialPrefix.gem?gem.stack():dust.stack()).copy();
   }
   captured.put(m,new GenericOutputs(Availability.PRESENT,outputs));
  }
  return Collections.unmodifiableMap(captured);
 }
 static List<Definition> definitions(Collection<GTMaterial> selected,Snapshot snapshot){
  return definitions(selected,snapshot,startupOutputs);
 }
 static List<Definition> definitions(Collection<GTMaterial> selected,Snapshot snapshot,Map<GTMaterial,GenericOutputs> captured){
  var out=new ArrayList<Definition>();
  Set<GTMaterial> candidates=Collections.newSetFromMap(new IdentityHashMap<>());
  for(var material:selected)candidates.add(material.resolve());
  var unresolved=new IdentityHashMap<GTMaterial,EnumSet<Size>>();
  // Keep the source order across all 22 fixed calls, not just per material. A known absent fixed
  // row is not a permanent blacklist. An UNKNOWN fixed binding must not become a generic guess.
  for(var row:specialRows())for(var m:candidates)if(row.input().matches(m.getId(),m.getName())){
   Availability state=special(m,row,snapshot,out);
   if(state==Availability.UNKNOWN)unresolved.computeIfAbsent(m,k->EnumSet.noneOf(Size.class)).add(row.size());
  }
  for(var material:selected)generic(material.resolve(),snapshot,captured,unresolved,out);
  sediment(selected,out);
  return List.copyOf(out);
 }

 /** GT6 Loader_OreProcessing:351 — every DUST_ORE host ore stack gets a Sifter row. The host is a
  * block state in the port, so each sediment host gets one row over the same ore block. */
 private static final List<OreHostStone> DUST_ORE_HOSTS=List.of(
     OreHostStone.SAND,OreHostStone.RED_SAND,OreHostStone.GRAVEL,OreHostStone.MUD);
 private static final long[] DUST_ORE_CHANCES={10000,10000,1500,1000,500};
 private static final int DUST_ORE_TICKS=256;

 private static void sediment(Collection<GTMaterial> selected,List<Definition> out){
  for(var candidate:selected){
   GTMaterial material=candidate.resolve();
   // Oilsands has its own Centrifuge event instead of the standard ore rows. Stone has no ore
   // stack in GT6 (only the block), so it gets no DUST_ORE rows even though the port registers one.
   if(!material.isValid()||"OilSand".equals(material.getName())||"Stone".equals(material.getName())
       ||!MaterialPrefix.crushed.isValidFor(material))continue;
   GTMaterial target=material.getTargetCrushingMaterial().resolve();
   if(target==null||!target.isValid())continue;
   ItemStack oreStack=GTBlocks.getStack(BlockMaterialPrefix.ore,material);
   if(oreStack.isEmpty()||!(oreStack.getItem() instanceof BlockItem item)||!(item.getBlock() instanceof OreBlock ore))continue;
   int multiplier=Math.max(1,Math.min(64,target.getOreMultiplier()*material.getOreProcessingMultiplier()));
   ItemStack purified=GTItems.getStack(MaterialPrefix.crushedPurified,target,multiplier);
   var byproducts=target.getByProducts();
   ItemStack primary=sedimentTiny(byproducts,0,OM.dustOrIngot(target,U9));
   ItemStack secondary=sedimentTiny(byproducts,1,primary);
   ItemStack tertiary=sedimentTiny(byproducts,2,secondary);
   ItemStack[] outputs={purified,purified.isEmpty()?ItemStack.EMPTY:purified.copy(),
       scaled(primary,multiplier),scaled(secondary,multiplier),scaled(tertiary,multiplier)};
   for(OreHostStone host:DUST_ORE_HOSTS){
    ItemStack input=OreBlock.withStone(oreStack.copy(),ore.stateFor(host));
    out.add(new Definition(OPTIMIZE,input,outputs,DUST_ORE_CHANCES,DUST_ORE_TICKS,POWER,false,stack->{
     var data=SourceOreCrushingAdapter.hostData(stack);
     return !data.broken()&&data.host().equals(host.id());
    }));
   }
  }
 }
 /** The source's first three byproducts, falling back to earlier slots (own dust first). */
 private static ItemStack sedimentTiny(List<GTMaterial> byproducts,int index,ItemStack fallback){
  if(index<byproducts.size()){
   GTMaterial material=byproducts.get(index);
   if(material!=null&&material.isValid()){
    ItemStack stack=OM.dustOrIngot(material.resolve(),U9);
    if(!stack.isEmpty())return stack;
   }
  }
  return fallback;
 }
 /** Source ST.amount(multiplier, stack): keeps the form, multiplies its count. */
 private static ItemStack scaled(ItemStack stack,int multiplier){
  if(stack==null||stack.isEmpty())return ItemStack.EMPTY;
  ItemStack copy=stack.copy();copy.setCount(Math.max(1,Math.min(64,copy.getCount()*multiplier)));return copy;
 }
 private static Availability special(GTMaterial m,SpecialRow row,Snapshot s,List<Definition> out){
  if(!sourceIdentity(m,row.input())){s.note(m,"fixed/"+row.sourceLine(),"unverified-source-material");return Availability.UNKNOWN;}
  var input=s.direct(row.size().input(),m,row.inputCount());
  if(input.availability()!=Availability.PRESENT){
   if(input.availability()==Availability.UNKNOWN)s.note(m,"fixed/"+row.sourceLine(),"unknown-input-form");
   return input.availability();
  }
  var outputs=new ItemStack[row.outputs().size()];
  for(int index=0;index<outputs.length;index++){
   var form=s.specialOutput(row.outputs().get(index));
   if(form.availability()==Availability.UNKNOWN){s.note(m,"fixed/"+row.sourceLine(),"unknown-output-slot-"+index);return Availability.UNKNOWN;}
   outputs[index]=form.stack(); // known ABSENT keeps its indexed EMPTY slot, without OM/grade fallback
  }
  out.add(new Definition(row.optimize(),input.stack(),outputs,row.chances().stream().mapToLong(Long::longValue).toArray(),
      row.ticks(),row.power(),true));
  return Availability.PRESENT;
 }
 private static void generic(GTMaterial m,Snapshot s,Map<GTMaterial,GenericOutputs> captured,
                             Map<GTMaterial,EnumSet<Size>> unresolved,List<Definition> out){
  var dust=s.direct(MaterialPrefix.dust,m,1);var gem=s.direct(MaterialPrefix.gem,m,1);
  // Preserve reviewed dormant input policy, including supported continuation evidence after tags.
  var whole=s.input(MaterialPrefix.crushedPurified,m,1);var tiny=s.input(MaterialPrefix.crushedPurifiedTiny,m,1);
  Availability gate=eligibility(m.isValid(),m.has(MaterialProperty.ANTIMATTER),m==GTMaterialRegistry.get("Bedrock"),
      dust.availability(),whole.availability(),tiny.availability(),gem.availability());
  if(gate!=Availability.PRESENT){if(gate==Availability.UNKNOWN)s.note(m,"generic","unknown-input-form");return;}
  var outputs=captured.get(m);
  if(outputs==null||outputs.availability()!=Availability.PRESENT){
   if(outputs==null||outputs.availability()==Availability.UNKNOWN)s.note(m,"generic","unresolved-startup-output");return;
  }
  for(Size size:Size.values()){
   if(unresolved.getOrDefault(m,EnumSet.noneOf(Size.class)).contains(size))continue;
   out.add(new Definition(OPTIMIZE,size==Size.WHOLE?whole.stack():tiny.stack(),outputs.outputs(),
       size.genericChances().stream().mapToLong(Long::longValue).toArray(),size.ticks(),POWER,false));
  }
 }
 private static boolean sourceIdentity(GTMaterial material,SourceOreSiftingRules.Material source){
  if(material==null||!material.isValid())return false;var m=material.resolve();
  return source.matches(m.getId(),m.getName())&&GTMaterialRegistry.isCoreMaterial(m)&&!AddonMaterials.owns(m);
 }
 private static ItemStack[] copies(ItemStack[] values){return Arrays.stream(values).map(ItemStack::copy).toArray(ItemStack[]::new);}
 static class Snapshot extends SharedOreWashingRows.Snapshot {
  final List<Deferred> siftingDeferred=new ArrayList<>();
  void note(GTMaterial m,String producer,String reason){siftingDeferred.add(new Deferred(m.getName(),producer,reason));}
  ItemStack grade(MaterialPrefix prefix,GTMaterial m,int count,ItemStack ordinaryGem,int fallbackCount){
   return SourceGemGradeResolver.gradeOrGem(prefix,m,count,ordinaryGem,fallbackCount);
  }
  /** No trusted modern UB fossil binding exists in this port. Explicit per-index bindings may be
   * supplied by a future reviewed adapter; a mod-presence check or guessed registry name is not one. */
  NativeForm fossil(int index){return NativeForm.absent();}
  NativeForm specialOutput(Output output){
   if(output instanceof FormOutput form){
    GTMaterial material=GTMaterialRegistry.get(form.material().name());
    if(material==null||!material.isValid())return NativeForm.absent();
    if(!sourceIdentity(material,form.material()))return NativeForm.unknown();
    return direct(form.prefix(),material.resolve(),form.count());
   }
   if(output instanceof VanillaOutput vanilla){
    var item=switch(vanilla.item()){case SKELETON_SKULL->Items.SKELETON_SKULL;case BONE->Items.BONE;};
    return NativeForm.present(new ItemStack(item,vanilla.count()));
   }
   if(output instanceof FossilOutput fossil){
    var provider=fossil(fossil.index());
    return switch(fossilChoice(provider.availability())){
     case PROVIDER -> provider;
     case FALLBACK -> specialOutput(fossil.fallback());
     case DEFER -> NativeForm.unknown();
    };
   }
   throw new IllegalArgumentException("Unsupported source Sifter output: "+output);
  }
 }
}
