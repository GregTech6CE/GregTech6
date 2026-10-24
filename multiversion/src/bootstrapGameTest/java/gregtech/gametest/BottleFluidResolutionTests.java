package gregtech.gametest;

import gregtech.content.food.GTDrinks;

import gregtech.data.RegisteredFluids;
import gregtech.data.generated.GTBottlesGen;
import gregtech.item.BottleItem;
import gregtech.registry.GTFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class BottleFluidResolutionTests {
 @GameTest(template="test_empty")
 public static void allGeneratedBottlesResolveTheirActualRegisteredFluid(GameTestHelper h) {
  String[] rows=GTBottlesGen.ENTRIES;
  for(int i=0;i<rows.length;i+=3){
   var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse("gt6ce:"+rows[i]));
   h.assertTrue(item instanceof BottleItem,"actual registered BottleItem "+rows[i]);
   var resolved=((BottleItem)item).fluid();
   h.assertTrue(resolved!=null,"bottle fluid identity "+rows[i]);
   var fluid=GTDrinks.fluidForField(rows[i+2]);
   h.assertTrue(fluid!=null && fluid==resolved,"actual bottle payload identity "+rows[i]);
   var entry=GTFluids.entryForFluid(fluid);
   h.assertTrue(entry!=null,"actual registry identity "+rows[i]);
  }
  h.assertTrue(rows.length/3==169,"full generated census");
  h.succeed();
 }
 @GameTest(template="test_empty")
 public static void sixPreviouslyWaterBottlesUseSourceNamedSpriteAndWhiteTint(GameTestHelper h) {
  String[] keys={"Hg","potion.goldenapplejuice","potion.goldencider","potion.idunsapplejuice","potion.notchesbrew","goldencarrotjuice"};
  String[] names={"mercury","potion.goldenapplejuice","potion.goldencider","potion.idunsapplejuice","potion.notchesbrew","goldencarrotjuice"};
  for(int i=0;i<keys.length;i++) {
   var fluid=GTDrinks.fluidForField(keys[i]);
   h.assertTrue(fluid!=null && fluid!=Fluids.WATER,"not missing/water "+keys[i]);
   var entry=GTFluids.entryForFluid(fluid);
   h.assertTrue(entry!=null && entry.registryName().equals(names[i]),"source fluid identity "+keys[i]);
   var expected=RegisteredFluids.fluidTexture(names[i]);
   var visual=gregtech.internal.fluid.FluidVisualPolicy.select(entry,expected::equals);
   h.assertTrue(visual.texture().equals(expected) && visual.tint()==0xFFFFFFFF,"named original sprite and no double tint "+keys[i]);
   if(i>0)h.assertTrue(entry.temperature()==300 && entry.luminosity()==15 && entry.viscosity()==1000 && entry.glint(),"source golden drink properties");
  }
  h.succeed();
 }
}
