package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
/** Actual accepted rows only. No source-negative inference or guessed cohort size. */
final class RemainingOreCensus {
 static void audit(GameTestHelper h){
  int materials=0,roles=0,accepted=0,selected=0;var seen=Collections.newSetFromMap(new IdentityHashMap<GTMaterial,Boolean>());
  for(var alias:GTMaterialRegistry.sortedMaterials()){
   var m=alias.resolve();if(!m.isValid()||!GTMaterialRegistry.isCoreMaterial(m)||!seen.add(m))continue;materials++;
   for(var p:new MaterialPrefix[]{MaterialPrefix.dust,MaterialPrefix.crushedPurified,MaterialPrefix.crushedPurifiedTiny}){
    var in=GTItems.getStack(p,m,1);if(in.isEmpty())continue;
    var maps=p==MaterialPrefix.dust?new RecipeMap[]{MachineRecipeMaps.Melter,MachineRecipeMaps.Smelter}:new RecipeMap[]{MachineRecipeMaps.Centrifuge};
    for(var map:maps){
     var rows=map.mRecipeList.stream().filter(row->Arrays.stream(row.mInputs).anyMatch(i->!i.isEmpty()&&i.is(in.getItem()))).toList();
     var cold=map.findRecipe(List.of(in),List.of(),false,map.mInputItemsCount,map.mOutputItemsCount);
     var warm=map.findRecipe(List.of(in),List.of(),false,map.mInputItemsCount,map.mOutputItemsCount);
     h.assertTrue(cold==warm,"observational warm lookup stable "+m.getName()+"/"+p.getName()+"/"+map.mNameInternal);
     roles++;accepted+=rows.size();if(cold!=null)selected++;
     System.out.println("[ore-completion-census] "+m.getId()+"/"+m.getName()+"/"+p.getName()+"/"+map.mNameInternal+
      " accepted="+rows.size()+" selected="+(cold!=null)+" dynamic="+(cold!=null&&!rows.contains(cold))+" addon="+AddonMaterials.owns(m)+
      " quality="+m.getToolQuality()+" target="+m.getTargetSmeltingMaterial().resolve().getName()+" amount="+m.getTargetSmeltingAmount());
     for(var row:rows)System.out.println("[ore-completion-detail] "+m.getId()+"/"+p.getName()+"/"+map.mNameInternal+" "+fingerprint(row));
    }
   }
  }
  h.assertTrue(materials>0&&roles>0&&accepted>0&&selected>0,"observational census must reach real native rows");
  System.out.println("[ore-completion-census-total] materials="+materials+" roles="+roles+" accepted="+accepted+" selected="+selected+" sourceParityClaim=false");
 }
 private static String items(ItemStack[] stacks){var out=new ArrayList<String>();for(var s:stacks)out.add(s.isEmpty()?"empty":BuiltInRegistries.ITEM.getKey(s.getItem())+"x"+s.getCount());return out.toString();}
 private static String fluids(FluidStack[] stacks){var out=new ArrayList<String>();for(var s:stacks)out.add(s==null||s.isEmpty()?"empty":BuiltInRegistries.FLUID.getKey(s.getFluid())+"x"+s.getAmount());return out.toString();}
 private static String fingerprint(Recipe r){return items(r.mInputs)+"->"+items(r.mOutputs)+";chance="+Arrays.toString(r.mChances)+";work="+r.mDuration+"/"+r.mEUt+";fluidInputs="+fluids(r.mFluidInputs)+";fluidOutputs="+fluids(r.mFluidOutputs);}
}
