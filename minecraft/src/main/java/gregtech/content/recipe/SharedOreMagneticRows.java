package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.recipe.Recipe;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import java.util.*;
import static gregtech.content.recipe.SourceOreMagneticRules.*;
import static gregtech.content.recipe.SourceOreWashingRules.Availability;
import static gregtech.content.recipe.SharedOreWashingRows.NativeForm;

/** Original Ores magnetic rows resolved from current holders after tag binding.
 * Own only ordinary addRecipe return identities; independent startup/datapack rows are never adopted.
 */
public final class SharedOreMagneticRows {
 private SharedOreMagneticRows() {}
 private static final Set<Recipe> OWNED=Collections.newSetFromMap(new IdentityHashMap<>());
 private static final List<Deferred> DEFERRED=new ArrayList<>();
 private static List<GTMaterial> materials=List.of();
 private static boolean initialized;
 public record Deferred(String material,String producer,String reason) {}
 public static Set<Recipe> owned(){
  Set<Recipe> out=Collections.newSetFromMap(new IdentityHashMap<>());out.addAll(OWNED);
  return Collections.unmodifiableSet(out);
 }
 public static List<Deferred> deferred(){return List.copyOf(DEFERRED);}
 public static void initialize(){
  if(initialized)throw new IllegalStateException("Magnetic rows initialized twice");initialized=true;
  Set<GTMaterial> seen=Collections.newSetFromMap(new IdentityHashMap<>());var list=new ArrayList<GTMaterial>();
  for(var m:GTMaterialRegistry.allMaterials())if(seen.add(m.resolve()))list.add(m.resolve());
  materials=List.copyOf(list);rebuild();
 }
 private static void clearOwned(){OWNED.forEach(MachineRecipeMaps.MagneticSeparator::unlink);OWNED.clear();}
 /** Initialization survives a server restart in the same JVM; accepted rows and diagnostics do not. */
 public static void clear(){clearOwned();DEFERRED.clear();}
 public static void rebuild(){
  if(!initialized)return;
  var snapshot=new Snapshot();var planned=definitions(materials,snapshot);
  clearOwned();DEFERRED.clear();DEFERRED.addAll(snapshot.magneticDeferred);
  try{for(var definition:planned){var row=definition.admit();if(row!=null)OWNED.add(row);}}
  catch(RuntimeException|Error failure){clear();throw failure;}
 }
 record Definition(boolean optimize,ItemStack input,ItemStack[] outputs,long[] chances,long duration,long power){
  Recipe admit(){return MachineRecipeMaps.MagneticSeparator.addRecipe(optimize,new ItemStack[]{input},outputs,null,
      chances,new FluidStack[0],new FluidStack[0],duration,power,0);}
 }
 static List<Definition> definitions(Collection<GTMaterial> selected,Snapshot snapshot){
  var out=new ArrayList<Definition>();
  for(var material:selected){
   GTMaterial m=material.resolve();
   generic(m,snapshot,out);
   for(var row:blockRows())if(row.input().id()==m.getId()&&row.input().name().equals(m.getName()))
    physical(m,row,snapshot,out);
  }
  return List.copyOf(out);
 }
 private static void generic(GTMaterial m,Snapshot s,List<Definition> out){
  NativeForm dust=s.direct(MaterialPrefix.dust,m,1),whole=s.direct(MaterialPrefix.crushedPurified,m,1),
      tiny=s.direct(MaterialPrefix.crushedPurifiedTiny,m,1);
  Availability gate=eligibility(m.isValid(),m.has(MaterialProperty.ANTIMATTER),m==GTMaterialRegistry.get("Bedrock"),
      dust.availability(),whole.availability(),tiny.availability());
  if(!s.available(m,"purified",gate))return;
  // Keep the already-reviewed 84 magnetic facts and the existing unknown/addon flag fallback.
  List<GTMaterial> selected=byproducts(m,OreSourcePolicy.magneticCandidates(m));
  if(selected.isEmpty())return;
  for(Size size:Size.values()){
   String route="purified/"+size;
   var main=s.refined(size,m,1);var outputs=new ItemStack[6];outputs[0]=main.stack();
   boolean known=main.availability()!=Availability.UNKNOWN;
   for(int index=0;index<5;index++){
    var fine=s.refined(Size.TINY,selected.get(index).resolve(),size.byproductCount());
    outputs[index+1]=fine.stack();known&=fine.availability()!=Availability.UNKNOWN;
   }
   if(!known){s.note(m,route,"unknown-output-form");continue;}
   // An absent OM output remains an EMPTY raw slot; native normalization validates the remainder.
   out.add(new Definition(OPTIMIZE,size==Size.WHOLE?whole.stack():tiny.stack(),outputs,
       CHANCES.stream().mapToLong(Long::longValue).toArray(),size.ticks(),POWER));
  }
 }
 private static void physical(GTMaterial m,BlockRow row,Snapshot s,List<Definition> out){
  if(!sourceIdentity(m,row.input())){s.note(m,"blockDust","unverified-source-material");return;}
  var input=s.blockDust(m,row.inputCount());if(!s.available(m,"blockDust",input.availability()))return;
  var outputs=new ItemStack[row.outputs().size()];
  for(int index=0;index<outputs.length;index++){
   var definition=row.outputs().get(index);GTMaterial material=GTMaterialRegistry.get(definition.material().name());
   if(!sourceIdentity(material,definition.material())){s.note(m,"blockDust","unverified-output-material");return;}
   var form=s.direct(definition.prefix(),material.resolve(),definition.count());
   if(form.availability()==Availability.UNKNOWN){s.note(m,"blockDust","unknown-output-form");return;}
   outputs[index]=form.stack();
  }
  out.add(new Definition(row.optimize(),input.stack(),outputs,row.chances().stream().mapToLong(Long::longValue).toArray(),
      row.ticks(),row.power()));
 }
 private static boolean sourceIdentity(GTMaterial material,SourceOreMagneticRules.Material source){
  if(material==null||!material.isValid())return false;var m=material.resolve();
  return m.getId()==source.id()&&m.getName().equals(source.name())&&GTMaterialRegistry.isCoreMaterial(m)&&!AddonMaterials.owns(m);
 }
 /** Reuse the exact native form and physical-block binder, not the washing eligibility or recipe rules. */
 static class Snapshot extends SharedOreWashingRows.Snapshot {
  final List<Deferred> magneticDeferred=new ArrayList<>();
  void note(GTMaterial m,String producer,String reason){magneticDeferred.add(new Deferred(m.getName(),producer,reason));}
  boolean available(GTMaterial m,String producer,Availability availability){
   if(availability==Availability.UNKNOWN)note(m,producer,"unknown-input-form");return availability==Availability.PRESENT;
  }
  NativeForm refined(Size size,GTMaterial material,int count){
   var selected=refinedPrefix(size,prefix->output(prefix,material,count).availability());
   return selected.availability()==Availability.PRESENT?output(selected.prefix(),material,count)
       :selected.availability()==Availability.UNKNOWN?NativeForm.unknown():NativeForm.absent();
  }
 }
}
