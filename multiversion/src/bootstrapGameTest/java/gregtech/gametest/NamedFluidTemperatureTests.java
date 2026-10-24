package gregtech.gametest;
import gregtech.content.registry.GTFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}
/** Actual registered source/flowing types. No world mutation or movement-system changes. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class NamedFluidTemperatureTests {
 @GameTest(template="test_empty")
 public static void matterChargedKeepsAuthoredTemperature(GameTestHelper h) {
  check(h,"MatterCharged","chargedmatter",1,15,-5000,1000,false,41L);
  h.succeed();
 }
 @GameTest(template="test_empty")
 public static void matterNeutralKeepsAuthoredTemperature(GameTestHelper h) {
  check(h,"MatterNeutral","neutralmatter",1,15,-5000,1000,false,41L);
  h.succeed();
 }
 @GameTest(template="test_empty")
 public static void uUAmplifierKeepsAuthoredTemperature(GameTestHelper h) {
  check(h,"UUAmplifier","uuamplifier",100,0,1000,1000,false,9L);
  h.succeed();
 }
 @GameTest(template="test_empty")
 public static void uUMKeepsAuthoredTemperature(GameTestHelper h) {
  check(h,"UUM","ic2uumatter",100,0,1000,1000,false,41L);
  h.succeed();
 }
 @GameTest(template="test_empty")
 public static void coolant_IC2_HotKeepsAuthoredTemperature(GameTestHelper h) {
  check(h,"Coolant_IC2_Hot","ic2hotcoolant",1200,0,1000,1000,false,4105L);
  h.succeed();
 }
 @GameTest(template="test_empty")
 public static void thorium_SaltKeepsAuthoredTemperature(GameTestHelper h) {
  check(h,"Thorium_Salt","thoriumsalt",600,0,1000,1000,false,9L);
  h.succeed();
 }
 @GameTest(template="test_empty")
 public static void oil_FryingKeepsAuthoredTemperature(GameTestHelper h) {
  check(h,"Oil_Frying","hotfryingoil",400,0,1000,1000,false,35717129L);
  h.succeed();
 }
 @GameTest(template="test_empty")
 public static void glassKeepsAuthoredTemperature(GameTestHelper h) {
  check(h,"Glass","glass",1200,0,1000,1000,false,9L);
  h.succeed();
 }
 @GameTest(template="test_empty")
 public static void phaseAndAuthoredMoltenControlsStayUnchanged(GameTestHelper h) {
  check(h,"Hot_Helium","hothelium",1150,0,-100,200,true,4106L);
  check(h,"Hot_Carbon_Dioxide","hotcarbondioxide",950,0,-100,200,true,4106L);
  var cryolite=GTFluids.stack("GenLiquid_Cryolite",1000);
  h.assertTrue(!cryolite.isEmpty() && cryolite.getFluid().getFluidType().getTemperature(cryolite)==1285
   && cryolite.getFluid().getFluidType().getLightLevel(cryolite)==10,"existing cryolite fix retained");
  var ender=GTFluids.stack("Ender",1000);
  h.assertTrue(!ender.isEmpty() && ender.getFluid().getFluidType().getTemperature(ender)==2723
   && ender.getFluid().getFluidType().getLightLevel(ender)==5,"authored Ender retained");
  var redstone=GTFluids.stack("Redstone",1000);
  h.assertTrue(!redstone.isEmpty() && redstone.getFluid().getFluidType().getTemperature(redstone)==500
   && redstone.getFluid().getFluidType().getLightLevel(redstone)==5,"authored Redstone retained");
  h.succeed();
 }
 private static void check(GameTestHelper h,String field,String path,int temp,int light,int density,
                           int viscosity,boolean gas,long flags) {
  var stack=GTFluids.stack(field,1000);
  h.assertTrue(!stack.isEmpty(),"registered source exists: "+field);
  var source=stack.getFluid();
  var flowingId=ResourceLocation.fromNamespaceAndPath("gt6ce",path+"_flowing");
  h.assertTrue(BuiltInRegistries.FLUID.containsKey(flowingId),"registered flowing exists: "+field);
  var flowing=BuiltInRegistries.FLUID.get(flowingId);
  h.assertTrue(BuiltInRegistries.FLUID.getKey(source).equals(ResourceLocation.fromNamespaceAndPath("gt6ce",path)),"stable source ID: "+field);
  h.assertTrue(flowing!=source && flowing.getFluidType()==source.getFluidType(),"source/flowing share actual type: "+field);
  var type=source.getFluidType();
  h.assertTrue(type.getTemperature(stack)==temp,field+" native temperature expected="+temp+" actual="+type.getTemperature(stack));
  h.assertTrue(type.getLightLevel(stack)==light && type.getDensity(stack)==density
   && type.getViscosity(stack)==viscosity,"unchanged light/density/viscosity: "+field);
  h.assertTrue(GTFluids.isGas(stack)==gas && GTFluids.isNonLiquidType(type)==gas,"unchanged phase selector: "+field);
  var entry=GTFluids.entryForFluid(source);
  var flowingEntry=GTFluids.entryForFluid(flowing);
  h.assertTrue(entry!=null && entry.temperature()==temp && entry.flags()==flags && entry.gas()==gas
   && entry.registryName().equals(path),"canonical registered metadata: "+field);
  h.assertTrue(entry.equals(flowingEntry),"flowing lookup retains canonical metadata: "+field);
 }
}
