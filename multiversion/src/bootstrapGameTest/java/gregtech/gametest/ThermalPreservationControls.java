package gregtech.gametest;

import gregtech.internal.recipe.Recipe;
import gregtech.internal.recipe.RecipeMap;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.registry.GTFluids;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
/** Ignored mergeable control for the ONE consolidated PVC lifecycle, no test namespace/reload/new profile.
 * Source literals match already-shipped OreThermalSourceTests. Does not mutate recipes or call succeed().
 */
final class ThermalPreservationControls {
 private static final String[] FORMS={"dust","dustSmall","dustTiny","crushedPurified","crushedPurifiedTiny",
  "crushedCentrifuged","crushedCentrifugedTiny","blockDust"};
 private static final int[] MB={48,12,5,60,6,66,7,432};
 private static final long[] TICKS={16,16,16,16,16,16,16,34};
 private record Selected(RecipeMap map,ItemStack input,Recipe recipe){}
 static final class Snapshot {
  private final List<Selected> rows;
  private Snapshot(List<Selected> rows){this.rows=List.copyOf(rows);}
  void assertUnchanged(GameTestHelper h){
   verify(h);
   for(var row:rows)h.assertTrue(lookup(row.map,row.input)==row.recipe,
    "Unrelated Cinnabar thermal recipe identity retained through PVC lifecycle");
  }
 }
 static Snapshot capture(GameTestHelper h){
  verify(h);var rows=new ArrayList<Selected>();
  for(var map:new RecipeMap[]{MachineRecipeMaps.Melter,MachineRecipeMaps.Smelter})
   for(String form:FORMS){var input=OreSourceDifferentialTests.s(form,"Cinnabar",1);rows.add(new Selected(map,input,lookup(map,input)));}
  return new Snapshot(rows);
 }
 private static Recipe lookup(RecipeMap map,ItemStack input){
  return map.findRecipe(List.of(input),List.of(),false,map.mInputItemsCount,map.mOutputItemsCount);
 }
 static void verify(GameTestHelper h){
  for(var map:new RecipeMap[]{MachineRecipeMaps.Melter,MachineRecipeMaps.Smelter}){
   for(int i=0;i<FORMS.length;i++){
    var mercury=GTFluids.stack("GenLiquid_Mercury",MB[i]);
    h.assertTrue(mercury!=null&&!mercury.isEmpty()&&net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(mercury.getFluid()).toString().equals("gt6ce:mercury"),"Source Mercury native identity");
    OreSourceDifferentialTests.row(h,"Original RecyclingProcessing/Cinnabar preserved through nonore batch",map,
     OreSourceDifferentialTests.s(FORMS[i],"Cinnabar",1),new FluidStack[0],new ItemStack[0],new long[0],
     new FluidStack[]{mercury},TICKS[i],16);
   }
   for(String form:new String[]{"oreRaw","crushed","crushedTiny","dustDiv72"}){
    var input=OreSourceDifferentialTests.s(form,"Cinnabar",1);
    h.assertTrue(lookup(map,input)==null,"Original dirty/ORE or zero-fluid thermal exclusion: "+form);
    h.assertTrue(map.mRecipeList.stream().noneMatch(r->Arrays.stream(r.mInputs).anyMatch(s->s.is(input.getItem()))),"No competing dirty/zero-fluid thermal entry: "+form);
   }
  }
 }
}
