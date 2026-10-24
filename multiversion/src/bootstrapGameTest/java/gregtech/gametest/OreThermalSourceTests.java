package gregtech.gametest;
import gregtech.api.material.MaterialFormItem;
import gregtech.api.recipe.RecipeMap;
import gregtech.block.machine.BasicMachineBlock;
import gregtech.blockentity.machine.BasicMachineBlockEntity;
import gregtech.data.MachineRecipeMaps;
import gregtech.recipe.MachineRecipeOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}
/** Tests-only next baseline; source RecyclingProcessing, not an assertion that all Cinnabar thermal routes are forbidden. */
@GameTestHolder("gregtech_ore_thermal_audit")
@PrefixGameTestTemplate(false)
public final class OreThermalSourceTests {
 private static final FluidStack[] NONE=new FluidStack[0];
 private static FluidStack mercury(int amount){
  var stack=gregtech.registry.GTFluids.stack("GenLiquid_Mercury",amount);
  if(stack==null||stack.isEmpty()||!BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString().equals("gt6ce:mercury"))
   throw new AssertionError("Required reviewed native Mercury fluid identity");
  var catalog=gregtech.data.RegisteredFluids.get("GenLiquid_Mercury");
  if(catalog==null||!catalog.registryName().equals("mercury"))throw new AssertionError("Required source mercury registry binding");
  return stack;
 }
 private static void row(GameTestHelper h,String prefix,int mb,long ticks){
  var input=OreSourceDifferentialTests.s(prefix,"Cinnabar",1);
  for(var map:new RecipeMap[]{MachineRecipeMaps.Melter,MachineRecipeMaps.Smelter})
   OreSourceDifferentialTests.row(h,"RecyclingProcessing: Cinnabar "+prefix+" -> Hg U/3; floor144mB/U",map,input,
    NONE,new ItemStack[0],new long[0],new FluidStack[]{mercury(mb)},ticks,16);
 }
 private static void snapshot(){
  var inputs=new ArrayList<ItemStack>();
  for(String prefix:new String[]{"oreRaw","crushed","crushedTiny","dust","dustSmall","dustTiny","dustDiv72",
    "crushedPurified","crushedPurifiedTiny","crushedCentrifuged","crushedCentrifugedTiny","blockDust"})
   inputs.add(OreSourceDifferentialTests.s(prefix,"Cinnabar",1));
  for(var map:new RecipeMap[]{MachineRecipeMaps.Melter,MachineRecipeMaps.Smelter})
   for(var r:map.mRecipeList)if(Arrays.stream(r.mInputs).anyMatch(in->inputs.stream().anyMatch(s->s.is(in.getItem()))))
    com.mojang.logging.LogUtils.getLogger().info("SOURCE_THERMAL_BASELINE {} inputs={} outputs={} fluidInputs={} fluidOutputs={} ticks={} EUt={}",
     map==MachineRecipeMaps.Melter?"Melter":"Smelter",Arrays.toString(r.mInputs),Arrays.toString(r.mOutputs),
     Arrays.toString(r.mFluidInputs),Arrays.toString(r.mFluidOutputs),r.mDuration,r.mEUt);
 }
 private static void absent(GameTestHelper h,String prefix){
  var input=OreSourceDifferentialTests.s(prefix,"Cinnabar",1);
  for(var map:new RecipeMap[]{MachineRecipeMaps.Melter,MachineRecipeMaps.Smelter}){
   h.assertTrue(map.mRecipeList.stream().noneMatch(r->Arrays.stream(r.mInputs).anyMatch(s->s.is(input.getItem()))),
    "Source recycling excludes "+prefix+" (dirty/ore prefix or zero rounded fluid)");
   h.assertTrue(map.findRecipe(List.of(input),List.of(),false,map.mInputItemsCount,map.mOutputItemsCount)==null,
    "Actual thermal lookup must not select forbidden input "+prefix);
  }
 }
 @GameTest(template="test_empty",batch="ore_thermal_source")
 public static void rawAndDirtyFormsHaveNoGenericThermalRows(GameTestHelper h){
  snapshot();
  for(String prefix:new String[]{"oreRaw","crushed","crushedTiny"})absent(h,prefix);h.succeed();
 }
 @GameTest(template="test_empty",batch="ore_thermal_source")
 public static void dustRecyclesToTargetMercuryNotMoltenCinnabar(GameTestHelper h){row(h,"dust",48,16);h.succeed();}
 @GameTest(template="test_empty",batch="ore_thermal_source")
 public static void dustFractionsPreserveSourceFloorRounding(GameTestHelper h){row(h,"dustSmall",12,16);row(h,"dustTiny",5,16);h.succeed();}
 @GameTest(template="test_empty",batch="ore_thermal_source")
 public static void cleanAndRefinedFormsRemainRecyclable(GameTestHelper h){row(h,"crushedPurified",60,16);row(h,"crushedCentrifuged",66,16);h.succeed();}
 @GameTest(template="test_empty",batch="ore_thermal_source")
 public static void cleanAndRefinedTinyFormsUseSourceUnits(GameTestHelper h){row(h,"crushedPurifiedTiny",6,16);row(h,"crushedCentrifugedTiny",7,16);h.succeed();}
 @GameTest(template="test_empty",batch="ore_thermal_source")
 public static void subMillibucketDustDoesNotCreateAFluidRow(GameTestHelper h){absent(h,"dustDiv72");h.succeed();}
 @GameTest(template="test_empty",batch="ore_thermal_source")
 public static void blockDustPreservesSourceWeightAndTruncatedWork(GameTestHelper h){row(h,"blockDust",432,34);h.succeed();}
}
