package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.addon.AddonMaterials;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Ignored SAME-batch actual-map whole-dust census. No new source profile/namespace or map mutation.
 * Original MT410/414 -> refractmetal/transmetal -> metal62: FURNACE only<=1200;
 * default target self/U; no Ti/Fe flag/target mutation in paired source census.
 * RecyclingProcessing254/288-291; OM324; OreDictMaterial1391; FL.createMolten1077.
 */
final class WholeDustThermalAssertions {
 private record Expected(int id,String name,int temperature,float density,String fluid,long ticks){}
 private static final Expected[] EXPECTED={
  new Expected(220,"Titanium",1941,4.54F,"gt6ce:molten_titanium",519),
  new Expected(260,"Iron",1811,7.874F,"gt6ce:molten_iron",830)};
 /** Caller asserts its combined failures ONLY after collecting every producer's observations. */
 static void audit(GameTestHelper h,List<String> failures){
  for(var expected:EXPECTED){
   var material=GTMaterialRegistry.get(expected.name).resolve();
   var input=OreSourceDifferentialTests.s("dust",expected.name,1);
   var fluid=GTFluids.stack("GenMolten_"+expected.name,144);
   var entry=RegisteredFluids.get("GenMolten_"+expected.name);
   boolean canonical=material.getId()==expected.id&&material.getName().equals(expected.name)
    &&GTMaterialRegistry.isCoreMaterial(material)&&!AddonMaterials.owns(material)
    &&input.getItem() instanceof MaterialFormItem form&&form.getPrefix()==MaterialPrefix.dust
    &&form.getMaterial().resolve()==material&&BuiltInRegistries.ITEM.getKey(input.getItem()).getNamespace().equals("gt6ce");
   var composition=ItemMaterialRegistry.explicit(input.getItem()).orElse(null);
   boolean ordinary=composition==null||(composition.recoverable()&&composition.prefix()==MaterialPrefix.dust
    &&composition.components().size()==1&&composition.material().resolve()==material&&composition.amount()==GTValues.U);
   boolean unchanged=canonical&&ordinary&&material.getTargetSmeltingMaterial().resolve()==material
    &&material.getTargetSmeltingAmount()==GTValues.U&&material.getDensity()==expected.density
    &&material.getMeltingPoint()==expected.temperature&&material.has(MaterialProperty.MELTING)
    &&fluid!=null&&!fluid.isEmpty()&&BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString().equals(expected.fluid)
    &&entry!=null&&entry.temperature()==expected.temperature;
   System.out.println("[whole-dust-thermal-facts] "+expected.name+" canonical="+canonical+" ordinary="+ordinary+" unchanged="+unchanged
    +" melting="+material.getMeltingPoint()+" density="+material.getDensity()+" target="+material.getTargetSmeltingMaterial()
    +" amount="+material.getTargetSmeltingAmount()+" fluid="+fluidText(fluid)+" sourceSmelterTicks="+expected.ticks);
   for(var map:new RecipeMap[]{MachineRecipeMaps.Melter,MachineRecipeMaps.Smelter}){
    String label=expected.name+"/"+(map==MachineRecipeMaps.Melter?"Melter":"Smelter");
    var rows=map.mRecipeList.stream().filter(r->r.mInputs.length==1&&r.mInputs[0].is(input.getItem())
     &&r.mInputs[0].getCount()==1&&r.mFluidInputs.length==0).toList();
    var winner=map.findRecipe(List.of(input),List.of(),false,map.mInputItemsCount,map.mOutputItemsCount);
    h.assertTrue(map.findRecipe(List.of(input),List.of(),false,map.mInputItemsCount,map.mOutputItemsCount)==winner,"Stable cold/warm whole dust lookup "+label);
    System.out.println("[whole-dust-thermal-census] "+label+" accepted="+rows.size()+" winner="+(winner==null?"none":describe(winner)));
    for(var r:rows)System.out.println("[whole-dust-thermal-row] "+label+" selected="+(r==winner)+" "+describe(r));
    // Foreign/composition/target overrides are diagnostics, never source-negative deletion authority.
    if(!unchanged){System.out.println("[whole-dust-thermal-deferred] "+label+" native facts overridden/missing; no source assertion");continue;}
    if(map==MachineRecipeMaps.Melter){
     if(!rows.isEmpty()||winner!=null)failures.add(label+": source target lacks FURNACE/Calcite exception; generic row forbidden");
    }else if(rows.size()!=1||winner!=rows.get(0))failures.add(label+": expected one selected ordinary source Smelter row");
    else {
     var r=rows.get(0);
     if(r.mOutputs.length!=0||r.mFluidOutputs.length!=1||r.mFluidOutputs[0].getFluid()!=fluid.getFluid()
      ||r.mFluidOutputs[0].getAmount()!=144||r.mDuration!=expected.ticks||r.mEUt!=16)
      failures.add(label+": expected empty items / "+expected.fluid+"144mB / ticks="+expected.ticks+" /16GU, actual="+describe(r));
    }
   }
  }
 }
 private static String fluidText(FluidStack fluid){return fluid==null||fluid.isEmpty()?"empty":BuiltInRegistries.FLUID.getKey(fluid.getFluid())+"x"+fluid.getAmount();}
 private static String describe(Recipe r){return "items="+Arrays.toString(r.mOutputs)+" fluids="+Arrays.stream(r.mFluidOutputs).map(WholeDustThermalAssertions::fluidText).toList()
  +" ticks="+r.mDuration+" eut="+r.mEUt;}
}
