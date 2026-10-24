package gregtech.gametest;
import gregtech.api.material.*;
import gregtech.internal.material.*;
import gregtech.internal.material.*;
import gregtech.api.recipe.*;
import gregtech.internal.recipe.*;
import gregtech.internal.recipe.*;
import gregtech.content.data.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
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
/** Read-only finite source remainder batch. No recipe injection, reload or world writes. */
@GameTestHolder("gregtech_ore_completion_batch")
@PrefixGameTestTemplate(false)
public final class OreCompletionBatchTests {
 @GameTest(template="test_empty",batch="ore_completion_batch",timeoutTicks=200)
 public static void slurryRecyclingPreservesSourceFractionsAndWaters(GameTestHelper h){
  var failures=new ArrayList<String>();SlurryRecyclingAssertions.audit(h,failures);
  h.assertTrue(failures.isEmpty(),"source slurry recycling mismatches: "+failures);h.succeed();
 }
 @GameTest(template="test_empty",batch="ore_completion_batch",timeoutTicks=200)
 public static void anvilRockAndRawUseSourceTargetAndWork(GameTestHelper h){
  var failures=new ArrayList<String>();AnvilSourceAssertions.audit(h,failures);
  h.assertTrue(failures.isEmpty(),"source Anvil target/work mismatches: "+failures);h.succeed();
 }
 @GameTest(template="test_empty",batch="ore_completion_batch",timeoutTicks=200)
 public static void fractionalTitaniumIronUseSourceThermalRows(GameTestHelper h){
  var failures=new ArrayList<String>();FractionalThermalAssertions.audit(h,failures);
  h.assertTrue(failures.isEmpty(),"source fractional Ti/Fe thermal mismatches: "+failures);h.succeed();
 }
 @GameTest(template="test_empty",batch="ore_completion_batch",timeoutTicks=200)
 public static void existingWholeDustAndCinnabarThermalRemainLiteral(GameTestHelper h){
  var snapshot=ThermalPreservationControls.capture(h);var failures=new ArrayList<String>();
  WholeDustThermalAssertions.audit(h,failures);h.assertTrue(failures.isEmpty(),"preserved source whole Ti/Fe rows: "+failures);
  snapshot.assertUnchanged(h);System.out.println("[ore-completion-preserved] thermal16+whole4 verified=true");h.succeed();
 }
 @GameTest(template="test_empty",batch="ore_completion_batch",timeoutTicks=200)
 public static void existingRawBonusAndCopperCentrifugeRemainLiteral(GameTestHelper h){
  var raw=OreSourceDifferentialTests.s("oreRaw","Cinnabar",1);var crushed=OreSourceDifferentialTests.s("crushed","Cinnabar",1);
  OreSourceDifferentialTests.row(h,"source Cinnabar raw bonus",MachineRecipeMaps.Crusher,raw,new FluidStack[0],
   new ItemStack[]{crushed,crushed.copy(),OreSourceDifferentialTests.s("gem","Cinnabar",1)},new long[]{10000,10000,3000},new FluidStack[0],128,16);
  for(boolean tiny:new boolean[]{false,true}){
   int count=tiny?1:9;
   OreSourceDifferentialTests.row(h,"source Copper purified Centrifuge",MachineRecipeMaps.Centrifuge,
    OreSourceDifferentialTests.s(tiny?"crushedPurifiedTiny":"crushedPurified","Copper",1),new FluidStack[0],
    new ItemStack[]{OreSourceDifferentialTests.s("crushedCentrifugedTiny","Copper",count),
     OreSourceDifferentialTests.s("crushedCentrifugedTiny","Cobaltite",count),
     OreSourceDifferentialTests.s("crushedCentrifugedTiny","Gold",count),
     OreSourceDifferentialTests.s("crushedCentrifugedTiny","Nickel",count)},
    new long[]{10000,1000,1000,1000},new FluidStack[0],tiny?16:144,16);
  }
  System.out.println("[ore-completion-preserved] raw1+centrifuge2 verified=true");h.succeed();
 }
 @GameTest(template="test_empty",batch="ore_completion_batch",timeoutTicks=400)
 public static void remainingThermalAndCentrifugeCensusIsObservational(GameTestHelper h){
  RemainingOreCensus.audit(h);h.succeed();
 }
}
