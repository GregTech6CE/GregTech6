package gregtech.content.recipe;
import gregtech.api.material.GTMaterial;
import gregtech.api.recipe.Recipe;
import gregtech.api.recipe.RecipeMap;
import gregtech.data.MaterialPrefix;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import java.util.*;
/** only the OPL maceration/washing producer is eligible for this ledger. */
public final class NonOreMacerationRows {
 private NonOreMacerationRows(){}
 private static final DormantOreRows ROWS=new DormantOreRows();
 private static final Set<GTMaterial> MATERIALS=Collections.newSetFromMap(new IdentityHashMap<>());
 private static NonOreContinuationEvidence.Snapshot startup;
 public static void begin(){
  if(startup!=null)throw new IllegalStateException("Nonore definitions initialized twice");
  startup=NonOreContinuationEvidence.startupSnapshot(gregtech.api.material.GTMaterialRegistry.allMaterials());
 }
 public static boolean reviewed(GTMaterial material){
  if(startup==null)throw new IllegalStateException("Nonore definitions not initialized");
  return ReviewedOreInputProfile.decision(material)==ReviewedOreInputProfile.Evidence.NO&&!startup.active(material);
 }
 public static void register(GTMaterial material,MaterialPrefix prefix,String producer,RecipeMap map,
       boolean optimize,ItemStack input,ItemStack[] outputs,long[] chances,FluidStack[] fluids,long duration,long eu){
  if(input==null||input.isEmpty())return; // same missing-form skip as original callsite
  if(reviewed(material)&&!SourceOreInputForms.allowGeneratedInput(material,prefix,input,SourceOreInputForms.Evidence.NO)){
   MATERIALS.add(material);ROWS.define(material,producer,map,optimize,input,outputs,chances,fluids,duration,eu);
  }else map.addRecipe(optimize,new ItemStack[]{input},outputs,null,chances,fluids,null,duration,eu,0);
 }
 public static void seal(){ROWS.seal();}
 public static Set<Recipe> owned(){return ROWS.owned();}
 public static void rebuild(){var snapshot=NonOreContinuationEvidence.snapshot(MATERIALS);ROWS.reconcile(snapshot::active);}
 public static void clear(){ROWS.clear();}
}
