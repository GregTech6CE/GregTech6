package gregtech.content.recipe;
import gregtech.internal.material.GTMaterial;
import gregtech.internal.recipe.Recipe;
import gregtech.internal.recipe.RecipeMap;
import gregtech.content.data.MaterialPrefix;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import java.util.*;
/** Exact OPL builtin raw Crusher/Hammer, maceration/washing and purified Sifting and water Sluice producers only; no other map rows are inferred. */
public final class NonOreMacerationRows {
 private NonOreMacerationRows(){}
 private static final DormantOreRows ROWS=new DormantOreRows();
 private static final Set<GTMaterial> MATERIALS=Collections.newSetFromMap(new IdentityHashMap<>());
 private static NonOreContinuationEvidence.Snapshot startup;
 public static void begin(){
  if(startup!=null)throw new IllegalStateException("Nonore definitions initialized twice");
  startup=NonOreContinuationEvidence.startupSnapshot(gregtech.internal.material.GTMaterialRegistry.allMaterials());
 }
 public static boolean reviewed(GTMaterial material){
  if(startup==null)throw new IllegalStateException("Nonore definitions not initialized");
  return ReviewedOreInputProfile.decision(material)==ReviewedOreInputProfile.Evidence.NO&&!startup.active(material);
 }
 public static void register(GTMaterial material,MaterialPrefix prefix,String producer,RecipeMap map,
       boolean optimize,ItemStack input,ItemStack[] outputs,long[] chances,FluidStack[] fluids,long duration,long eu){
  register(material,prefix,producer,map,optimize,input,outputs,chances,fluids,null,duration,eu);
 }
 public static void register(GTMaterial material,MaterialPrefix prefix,String producer,RecipeMap map,
       boolean optimize,ItemStack input,ItemStack[] outputs,long[] chances,FluidStack[] fluids,
       FluidStack[] fluidOutputs,long duration,long eu){
  if(input==null||input.isEmpty())return; // same missing-form skip as original callsite
  if(reviewed(material)&&!SourceOreInputForms.allowGeneratedInput(material,prefix,input,SourceOreInputForms.Evidence.NO)){
   MATERIALS.add(material);ROWS.define(material,producer,map,optimize,input,outputs,chances,fluids,fluidOutputs,duration,eu);
  }else map.addRecipe(optimize,new ItemStack[]{input},outputs,null,chances,fluids,fluidOutputs,duration,eu,0);
 }
 public static void seal(){ROWS.seal();}
 public static Set<Recipe> owned(){return ROWS.owned();}
 public static void rebuild(){var snapshot=NonOreContinuationEvidence.snapshot(MATERIALS);ROWS.reconcile(snapshot::active);}
 public static void clear(){ROWS.clear();}
}
