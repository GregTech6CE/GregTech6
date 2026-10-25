package gregtech.gametest;
import gregtech.internal.addon.AddonMaterials;
import gregtech.api.material.*;
import gregtech.internal.material.*;
import gregtech.internal.material.*;
import gregtech.internal.recipe.*;
import gregtech.internal.recipe.*;
import gregtech.content.data.*;
import gregtech.content.registry.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import java.util.*;
/** Source literals, not production row/table derived. No map mutation/test completion. */
final class FractionalThermalAssertions {
 static void audit(GameTestHelper h,List<String> failures){
  for(String name:new String[]{"Titanium","Iron"}){
   boolean ti=name.equals("Titanium");var material=GTMaterialRegistry.get(name).resolve();
   var entry=RegisteredFluids.get("GenMolten_"+name);String fluidId="gt6ce:molten_"+name.toLowerCase(java.util.Locale.ROOT);
   for(boolean tiny:new boolean[]{false,true}){
    var prefix=tiny?MaterialPrefix.dustTiny:MaterialPrefix.dustSmall;int mb=tiny?16:36;long ticks=ti?(tiny?57:129):(tiny?92:207);
    var input=GTItems.getStack(prefix,material,1);var fluid=GTFluids.stack("GenMolten_"+name,mb);
    var data=input.isEmpty()?null:ItemMaterialRegistry.explicit(input.getItem()).orElse(null);
    boolean canonical=material.getId()==(ti?220:260)&&material.getName().equals(name)&&GTMaterialRegistry.isCoreMaterial(material)&&!AddonMaterials.owns(material);
    boolean ordinary=!input.isEmpty()&&input.getItem() instanceof MaterialFormItem form&&form.getMaterial().resolve()==material&&form.getPrefix()==prefix
     &&BuiltInRegistries.ITEM.getKey(input.getItem()).getNamespace().equals("gt6ce")&&(data==null||(data.recoverable()&&data.prefix()==prefix&&data.components().size()==1&&data.material().resolve()==material&&data.amount()==prefix.getMaterialWeight()));
    boolean unchanged=material.getTargetSmeltingMaterial().resolve()==material&&material.getTargetSmeltingAmount()==GTValues.U&&material.getDensity()==(ti?4.54F:7.874F)
     &&material.getMeltingPoint()==(ti?1941:1811)&&material.has(MaterialProperty.MELTING)&&!material.has(MaterialProperty.ANTIMATTER)
     &&entry!=null&&entry.temperature()==(ti?1941:1811)&&FluidCatalog.isGeneratedMoltenDefinition("GenMolten_"+name,entry)
     &&fluid!=null&&!fluid.isEmpty()&&BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString().equals(fluidId);
    String key=name+"/"+prefix.getName();System.out.println("[fractional-thermal-facts] "+key+" canonical="+canonical+" ordinary="+ordinary+" unchanged="+unchanged+" sourceMb="+mb+" sourceTicks="+ticks);
    for(var map:new RecipeMap[]{MachineRecipeMaps.Melter,MachineRecipeMaps.Smelter}){
     String label=key+"/"+(map==MachineRecipeMaps.Melter?"Melter":"Smelter");
     var rows=input.isEmpty()?List.<Recipe>of():map.mRecipeList.stream().filter(r->r.mInputs.length==1&&r.mInputs[0].is(input.getItem())&&r.mInputs[0].getCount()==1&&r.mFluidInputs.length==0).toList();
     var cold=input.isEmpty()?null:map.findRecipe(List.of(input),List.of(),false,map.mInputItemsCount,map.mOutputItemsCount);
     var warm=input.isEmpty()?null:map.findRecipe(List.of(input),List.of(),false,map.mInputItemsCount,map.mOutputItemsCount);
     System.out.println("[fractional-thermal-census] "+label+" accepted="+rows.size()+" cold="+describe(cold)+" warmSame="+(warm==cold));
     for(var r:rows)System.out.println("[fractional-thermal-row] "+label+" selected="+(cold==r)+" "+describe(r));
     if(!canonical||!ordinary||!unchanged){failures.add(label+": controlled native source fixture altered/missing; no silent deferred pass");continue;}
     if(warm!=cold)failures.add(label+": unstable warm winner");
     if(map==MachineRecipeMaps.Melter){if(!rows.isEmpty()||cold!=null)failures.add(label+": source target lacks FURNACE/Calcite exception");}
     else if(rows.size()!=1||cold!=rows.get(0))failures.add(label+": one exact selected source Smelter row missing");
     else if(cold.mOutputs.length!=0||cold.mFluidOutputs.length!=1||cold.mFluidOutputs[0].getFluid()!=fluid.getFluid()||cold.mFluidOutputs[0].getAmount()!=mb||cold.mDuration!=ticks||cold.mEUt!=16)
      failures.add(label+": expected source "+fluidId+"/"+mb+"mB "+ticks+"ticks/16GU");
    }
   }
  }
 }
 private static String describe(Recipe r){if(r==null)return "NONE";var fluids=new ArrayList<String>();for(var f:r.mFluidOutputs)fluids.add(BuiltInRegistries.FLUID.getKey(f.getFluid())+"x"+f.getAmount());return "out="+Arrays.toString(r.mOutputs)+" fluids="+fluids+" ticks="+r.mDuration+" eut="+r.mEUt;}
}
