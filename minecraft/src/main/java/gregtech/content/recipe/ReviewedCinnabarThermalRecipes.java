package gregtech.content.recipe;

import gregtech.engine.recipe.SourceRecyclingMath;
import gregtech.engine.addon.AddonMaterials;
import gregtech.api.material.*;
import gregtech.engine.material.*;
import gregtech.engine.material.*;
import gregtech.engine.material.*;
import gregtech.content.data.*;
import gregtech.content.registry.GTItems;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import static gregtech.api.material.GTValues.U;
/** Bounded source thermal adapter: Cinnabar forms -> Mercury plus Ti/Fe dust fractions; not a universal recycling listener. */
public final class ReviewedCinnabarThermalRecipes {
 private ReviewedCinnabarThermalRecipes(){}
 private record Form(MaterialPrefix prefix,long amount){}
 private record Input(ItemStack stack,long amount,ThermalInputProof proof){}
 private record MetalFraction(MaterialPrefix prefix,long amount,int millibuckets,long titaniumTicks,long ironTicks){}
 private static final MetalFraction[] METAL_FRACTIONS={
  new MetalFraction(MaterialPrefix.dustSmall,U/4,36,129,207),
  new MetalFraction(MaterialPrefix.dustTiny,U/9,16,57,92),
  new MetalFraction(MaterialPrefix.dustDiv72,U/72,2,16,16)};
 private static final Form[] FORMS={new Form(MaterialPrefix.dust,U),new Form(MaterialPrefix.dustSmall,U/4),
  new Form(MaterialPrefix.dustTiny,U/9),new Form(MaterialPrefix.dustDiv72,U/72),
  new Form(MaterialPrefix.crushedPurified,10*(U/8)),new Form(MaterialPrefix.crushedPurifiedTiny,10*(U/72)),
  new Form(MaterialPrefix.crushedCentrifuged,11*(U/8)),new Form(MaterialPrefix.crushedCentrifugedTiny,11*(U/72))};
 public static boolean emitIfSupported(GTMaterial material){
  if(emitWholeDustMetal(material))return true;
  if(!ReviewedCinnabarFacts.supports(material))return false;
  material=material.resolve();var target=material.getTargetSmeltingMaterial();
  if(target==null||!target.isValid())return false;target=target.resolve();
  if(AddonMaterials.owns(target)||!GTMaterialRegistry.isCoreMaterial(target)||target.getId()!=800||!target.getName().equals("Mercury")
    ||material.getTargetSmeltingAmount()!=U/3||!material.has(MaterialProperty.MELTING)
    ||material.getDensity()!=7.8003F||target.getMeltingPoint()!=234)return false;
  var binding=ReviewedOreFluidBindings.liquid(800,"Mercury");
  var nativeFluid=RegisteredFluids.get("GenLiquid_Mercury");
  // Source and native Mercury are300K. Factory identity is distinct from static-directory ownership.
  if(nativeFluid==null||!ReviewedOreGeneratedFluids.mercury(nativeFluid)||nativeFluid.temperature()!=300
    ||binding==null||binding.mbPerU()!=144||OreFluidBindingAdapter.liquid(target,U/3,false).isEmpty())return false;
  var inputs=new java.util.ArrayList<Input>();
  // Preflight the ENTIRE bounded row set before emitting anything. Overridden native item compositions defer.
  for(var form:FORMS){
   var stack=GTItems.getStack(form.prefix,material,1);
   if(!unchangedInput(stack,form.prefix,material,form.amount))return false;
   inputs.add(new Input(stack,form.amount,ThermalInputProof.capture(stack,material,form.prefix,form.amount)));
  }
  var block=ReviewedCinnabarFacts.INSTANCE.blockDust(material,1);
  if(!unchangedInput(block,null,material,9*U))return false;
  inputs.add(new Input(block,9*U,ThermalInputProof.captureCinnabarBlock(block,material)));
  for(var input:inputs)emit(input.stack,input.amount,target,input.proof);
  return true; // Only replaces this producer's old dust row; no mapwide removals.
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
    ||!net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString().equals(expected))return false;
  // Independent paired original literals: floor(weight*(melting-293)/1600), density4.54/7.874.
  long ticks=material.getId()==220?519:830;
  admit(MachineRecipeMaps.Smelter,input,fluid,ticks,ThermalInputProof.capture(input,material,MaterialPrefix.dust,U));
  // Finite extension of this reviewed Ti/Fe single-material source branch, not generic recycling.
  // Missing forms stay missing; explicit changed compositions defer independently, never deleted.
  for(var form:METAL_FRACTIONS){
   var fraction=GTItems.getStack(form.prefix,material,1);
   if(fraction.isEmpty()||!unchangedInput(fraction,form.prefix,material,form.amount))continue;
   long fractionTicks=material.getId()==220?form.titaniumTicks:form.ironTicks;
   var fractionFluid=fluid.copy();fractionFluid.setAmount(form.millibuckets);
   admit(MachineRecipeMaps.Smelter,fraction,fractionFluid,fractionTicks,ThermalInputProof.capture(fraction,material,form.prefix,form.amount));
  }
  return true;
 }
 private static boolean unchangedInput(ItemStack stack,MaterialPrefix prefix,GTMaterial material,long amount){
  if(stack.isEmpty())return true; // Missing forms were not registered by this producer either.
  if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("gt6ce"))return false;
  if(prefix!=null){
   if(!(stack.getItem() instanceof MaterialFormItem form)||form.getPrefix()!=prefix
     ||form.getMaterial().resolve()!=material||prefix.getMaterialWeight()!=amount)return false;
  }else if(!(stack.getItem() instanceof gregtech.content.block.MaterialBlockItem block)
     ||block.material()==null||block.material().resolve()!=material)return false;
  if(prefix==null)return amount==9*U&&ThermalInputProof.captureCinnabarBlock(stack,material)!=null;
  var composition=ItemMaterialRegistry.explicit(stack.getItem()).orElse(null);
  if(composition==null)return false; // Missing ordinary item proof must not create a guessed row.
  return composition.recoverable()&&composition.prefix()==prefix&&composition.components().size()==1
    &&composition.material().resolve()==material&&composition.amount()==amount
    &&ThermalInputProof.capture(stack,material,prefix,amount)!=null;
 }
 private static void emit(ItemStack input,long amount,GTMaterial target,ThermalInputProof proof){
  if(input.isEmpty())return;
  var result=SourceRecyclingMath.singleLiquid(amount,U,U/3,144,7.8003,234,300,293);
  if(result.millibuckets()<=0)return;
  var fluid=OreFluidBindingAdapter.liquid(target,result.targetUnits(),false);
  if(fluid.isEmpty()||fluid.getAmount()!=result.millibuckets())throw new IllegalStateException("Reviewed thermal unit mismatch");
  // Source target Mercury has FURNACE through elemental metal factory (234K <=1200K).
  admit(MachineRecipeMaps.Melter,input,fluid,result.ticks(),proof);
  admit(MachineRecipeMaps.Smelter,input,fluid,result.ticks(),proof);
 }
 private static Recipe admit(RecipeMap map,ItemStack input,FluidStack output,long ticks,ThermalInputProof proof){
  if(proof==null||input.isEmpty()||output.isEmpty())return null;
  var recipe=RecipeMap.make(true,new ItemStack[]{input.copy()},new ItemStack[0],null,null,
      new FluidStack[0],new FluidStack[]{output.copy()},ticks,16,0);
  return recipe==null?null:map.addRecipe(recipe.withItemInputGuard(0,proof::matches));
 }
}
