package gregtech.gametest;
import gregtech.content.food.BottleContents;
import gregtech.content.food.GTDrinks;
import gregtech.item.BottleItem;
import gregtech.item.PortableFluidContainerItem;
import gregtech.block.inventory.BottleCrateBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
//?}
/** Fixed-phase extra coverage, not a baseline case: exercises new shared payload boundary. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class BottleContentsNativeTests {
 @GameTest(template="test_empty")
 public static void vanillaBottlesAndNativeCapabilityPayloadsKeepTheirIdentity(GameTestHelper h) {
  var nativeItems=new net.minecraft.world.item.Item[]{Items.HONEY_BOTTLE,Items.DRAGON_BREATH,Items.EXPERIENCE_BOTTLE};
  var keys=new String[]{"Honey","Dragon_Breath","Potion_Jump_1"};
  for(int i=0;i<keys.length;i++) {
   var stack=new ItemStack(nativeItems[i]);
   h.assertTrue(BottleCrateBlock.isBottle(stack),"native remainder bottle admitted "+keys[i]);
   var fluid=BottleContents.resolve(stack);
   h.assertTrue(!fluid.isEmpty() && fluid.getFluid()==GTDrinks.fluidForField(keys[i]) && fluid.getAmount()==250,"native bottle actual identity "+keys[i]);
  }
  for(var item:new net.minecraft.world.item.Item[]{Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION}) {
   var stack=new ItemStack(item);
//? if >=1.21.1 {
   /*stack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
       new net.minecraft.world.item.alchemy.PotionContents(java.util.Optional.of(net.minecraft.world.item.alchemy.Potions.HEALING),java.util.Optional.of(0x13579B),java.util.List.of()));
*///?} else {
   net.minecraft.world.item.alchemy.PotionUtils.setPotion(stack,net.minecraft.world.item.alchemy.Potions.HEALING);
   stack.getOrCreateTag().putInt("CustomPotionColor",0x13579B);
//?}
   var before=stack.copy();var fluid=BottleContents.resolve(stack);
   h.assertTrue(fluid.getFluid()==Fluids.WATER,"potion intentionally has water sprite base");
//? if >=1.21.1 {
   /*h.assertTrue(ItemStack.isSameItemSameComponents(before,stack) && stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS).getColor()==0x13579B,"custom potion payload preserved");
*///?} else {
   h.assertTrue(ItemStack.isSameItemSameTags(before,stack) && net.minecraft.world.item.alchemy.PotionUtils.getColor(stack)==0x13579B,"custom potion payload preserved");
//?}
  }
  for(var stack:new ItemStack[]{ItemStack.EMPTY,new ItemStack(Items.GLASS_BOTTLE),BottleItem.emptyBottle(),new ItemStack(Items.STICK)})
   h.assertTrue(BottleContents.resolve(stack).isEmpty(),"empty/unknown never fictional water");
  ItemStack carrier=ItemStack.EMPTY;
  for(var item:BuiltInRegistries.ITEM)if(item instanceof PortableFluidContainerItem vessel && vessel.accepts(new FluidStack(Fluids.WATER,250))){carrier=new ItemStack(item);break;}
  h.assertTrue(!carrier.isEmpty(),"real registered native fluid handler carrier");
  var handler=FluidUtil.getFluidHandler(carrier).orElseThrow(() -> new AssertionError("missing native fluid capability"));
  var payload=new FluidStack(Fluids.WATER,250);
//? if >=1.21.1 {
  /*payload.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("capability-payload"));
*///?} else {
  var tag=new net.minecraft.nbt.CompoundTag();tag.putString("testPayload","capability-payload");payload.setTag(tag);
//?}
  h.assertTrue(handler.fill(payload,FluidAction.EXECUTE)==250,"native capability stores payload");
  var actual=BottleContents.resolve(handler.getContainer());
  h.assertTrue(actual.getFluid()==Fluids.WATER && actual.getAmount()==250,"external namespace minecraft fluid and amount preserved");
//? if >=1.21.1 {
  /*h.assertTrue(actual.getComponentsPatch().equals(payload.getComponentsPatch()),"native fluid components preserved");
*///?} else {
  h.assertTrue(java.util.Objects.equals(actual.getTag(),payload.getTag()),"native fluid NBT preserved");
//?}
  h.assertTrue(handler.getFluidInTank(0).getAmount()==250,"render lookup does not drain capability");
  h.succeed();
 }
}
