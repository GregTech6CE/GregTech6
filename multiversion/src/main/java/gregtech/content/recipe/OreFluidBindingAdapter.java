package gregtech.content.recipe;
import gregtech.internal.material.GTMaterial;
import gregtech.internal.addon.AddonMaterials;
import gregtech.registry.GTFluids;
import net.minecraft.core.registries.BuiltInRegistries;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}
/** Opt-in original-unit adapter. Unsupported/mismatched bindings never silently fall back. */
public final class OreFluidBindingAdapter {
 private OreFluidBindingAdapter() {}
 public static FluidStack liquid(GTMaterial material,long sourceAmount,boolean roundUp) {
  if(material==null||!material.isValid()||AddonMaterials.owns(material))return FluidStack.EMPTY;
  material=material.resolve();
  return sourceLiquid(material.getId(),material.getName(),sourceAmount,roundUp);
 }
 /** A source material need not have a port item/material object when its exact builtin fluid exists.
  * Only the reviewed ID/name binding table can resolve here; never infer an arbitrary fluid alias.
  */
 static FluidStack sourceLiquid(int sourceId,String sourceName,long sourceAmount,boolean roundUp) {
  var b=ReviewedOreFluidBindings.liquid(sourceId,sourceName);
  int mb=ReviewedOreFluidBindings.amount(b,sourceAmount,roundUp);
  if(b==null||mb<=0)return FluidStack.EMPTY;
  var stack=GTFluids.stack(b.field(),mb);
  if(stack==null||stack.isEmpty())return FluidStack.EMPTY;
  var key=BuiltInRegistries.FLUID.getKey(stack.getFluid());
  String nativePath=switch(b.field()) {
   case "GenMolten_Wax" -> "molten_wax";
   case "GenMolten_WaxBee" -> "molten_waxbee";
   case "GenMolten_WaxRefractory" -> "molten_waxrefractory";
   case "GenMolten_WaxParaffin" -> "molten_waxparaffin";
   case "GenMolten_WaxPlant" -> "molten_waxplant";
   case "GenMolten_WaxMagic" -> "molten_waxmagic";
   case "GenMolten_WaxAmnesic" -> "molten_waxamnesic";
   case "GenMolten_WaxSoulful" -> "molten_waxsoulful";
   case "GenLiquid_Mercury" -> "mercury";
   case "Cryotheum" -> "cryotheum";
   case "Petrotheum" -> "petrotheum";
   default -> throw new IllegalStateException("No reviewed native identity: "+b.field());
  };
  var entry=gregtech.data.RegisteredFluids.get(b.field());
  if(entry==null||!(gregtech.data.RegisteredFluids.isBuiltInDefinition(entry)
       || (b.field().equals("GenLiquid_Mercury") && ReviewedOreGeneratedFluids.mercury(entry)))||!entry.registryName().equals(b.registryPath()))
   throw new IllegalStateException("Reviewed source catalog identity mismatch: "+b);
  if(!key.getNamespace().equals("gt6ce")||!key.getPath().equals(nativePath))
   throw new IllegalStateException("Reviewed source fluid binding identity mismatch: "+b+" -> "+key);
  return stack;
 }
}
