package gregtech.content.recipe;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialFormItem;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import static gregtech.api.material.GTValues.U;
/** Bounded source thermal adapter for whole dust metal and reviewed Ti/Fe dust fractions. */
public final class ReviewedCinnabarThermalRecipes {
 private ReviewedCinnabarThermalRecipes(){}
 private record MetalFraction(MaterialPrefix prefix,long amount,int millibuckets,long titaniumTicks,long ironTicks){}
 private static final MetalFraction[] METAL_FRACTIONS={
  new MetalFraction(MaterialPrefix.dustSmall,U/4,36,129,207),
  new MetalFraction(MaterialPrefix.dustTiny,U/9,16,57,92),
  new MetalFraction(MaterialPrefix.dustDiv72,U/72,2,16,16)};
 public static boolean emitIfSupported(GTMaterial material){
  return emitWholeDustMetal(material);
 }
 /** Source Ti/Fe targets lack FURNACE; preserve Smelter, do not invent a generic Melter row.
  * Only this producer's old ordinary dust row is replaced. Independent/mapwide rows are untouched.
  */
 private static boolean emitWholeDustMetal(GTMaterial material){
  if(material==null||!material.isValid())return false;
  material=material.resolve();String field="GenMolten_"+material.getName();
  if(!ReviewedOreGeneratedFluids.wholeDustMetal(material,RegisteredFluids.get(field)))return false;
  var input=GTItems.getStack(MaterialPrefix.dust,material,1);
  // Both native registrars install an explicit self/dust/U composition for every material item.
  // Exact equivalent compositions are harmless; changed/mixed/unrecoverable bindings still defer.
  if(input.isEmpty()||!unchangedInput(input,MaterialPrefix.dust,material,U))return false;
  var fluid=gregtech.content.registry.GTFluids.stack(field,144);
  String expected="gt6ce:molten_"+material.getName().toLowerCase(java.util.Locale.ROOT);
  if(fluid==null||fluid.isEmpty()||fluid.getAmount()!=144
    ||!BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString().equals(expected))return false;
  // Independent paired original literals: floor(weight*(melting-293)/1600), density4.54/7.874.
  long ticks=material.getId()==220?519:830;
  admit(MachineRecipeMaps.Smelter,input,fluid,ticks,RecoveryInputProof.capture(input,material,MaterialPrefix.dust,U));
  // Finite extension of this reviewed Ti/Fe single-material source branch, not generic recycling.
  // Missing forms stay missing; explicit changed compositions defer independently, never deleted.
  for(var form:METAL_FRACTIONS){
   var fraction=GTItems.getStack(form.prefix,material,1);
   if(fraction.isEmpty()||!unchangedInput(fraction,form.prefix,material,form.amount))continue;
   long fractionTicks=material.getId()==220?form.titaniumTicks:form.ironTicks;
   var fractionFluid=fluid.copy();fractionFluid.setAmount(form.millibuckets);
  admit(MachineRecipeMaps.Smelter,fraction,fractionFluid,fractionTicks,RecoveryInputProof.capture(fraction,material,form.prefix,form.amount));
  }
  return true;
 }
 private static boolean unchangedInput(ItemStack stack,MaterialPrefix prefix,GTMaterial material,long amount){
  if(stack.isEmpty())return true; // Missing forms were not registered by this producer either.
  if(!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE))return false;
  if(!(stack.getItem() instanceof MaterialFormItem form)||form.getPrefix()!=prefix
    ||form.getMaterial().resolve()!=material||prefix.getMaterialWeight()!=amount)return false;
  var composition=ItemMaterialRegistry.base(stack.getItem()).orElse(null);
  if(composition==null)return false; // Missing ordinary item proof must not create a guessed row.
  return composition.prefix()==prefix&&composition.components().size()==1
    &&composition.material().resolve()==material&&composition.amount()==amount
    &&RecoveryInputProof.capture(stack,material,prefix,amount)!=null;
 }
 private static Recipe admit(RecipeMap map,ItemStack input,FluidStack output,long ticks,RecoveryInputProof proof){
  if(proof==null||input.isEmpty()||output.isEmpty())return null;
  var recipe=RecipeMap.make(true,new ItemStack[]{input.copy()},new ItemStack[0],null,null,
      new FluidStack[0],new FluidStack[]{output.copy()},ticks,16,0);
  return recipe==null?null:map.addRecipe(recipe.withItemInputGuard(0,proof::matches));
 }
}
