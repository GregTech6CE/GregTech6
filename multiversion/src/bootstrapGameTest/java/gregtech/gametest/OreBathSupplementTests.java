package gregtech.gametest;
import gregtech.data.MachineRecipeMaps;
import gregtech.registry.GTFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}
/** Independent supplement da962ce38dff3624784e73a39b1f30fec8033cd8bc22f439cff08ffbab4e260e.
 * Actual accepted maps+findRecipe, not inserted expected rows. Source Ores184-187 / w188-191.
 */
@GameTestHolder("gregtech_ore_source_audit")
@PrefixGameTestTemplate(false)
public final class OreBathSupplementTests {
 private static void check(GameTestHelper h,String input,int inputCount,String output,int mb) {
  var cryo=GTFluids.stack("Cryotheum",mb);
  h.assertTrue(cryo!=null&&!cryo.isEmpty(),"Cryotheum source fixture");
  var key=BuiltInRegistries.FLUID.getKey(cryo.getFluid());
  h.assertTrue(key.getNamespace().equals("gt6ce")&&key.getPath().equals("cryotheum"),"Original fluid identity");
  OreSourceDifferentialTests.row(h,"Cinnabar explicit Cryotheum Bath",MachineRecipeMaps.Bath,
   OreSourceDifferentialTests.s(input,"Cinnabar",inputCount),new FluidStack[]{cryo},
   new ItemStack[]{OreSourceDifferentialTests.s(output,"Cinnabar",1)},new long[]{10000},new FluidStack[0],256,0);
  h.succeed();
 }
 @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
 public static void cinnabarBlockDustCryotheum(GameTestHelper h){check(h,"blockDust",1,"gemExquisite",25);}
 @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
 public static void cinnabarCrushedCryotheum(GameTestHelper h){check(h,"crushed",1,"gemFlawless",13);}
 @GameTest(template="test_empty",batch="ore_source_differential",timeoutTicks=200)
 public static void cinnabarTinyCrushedCryotheum(GameTestHelper h){check(h,"crushedTiny",9,"gemFlawless",13);}
}
