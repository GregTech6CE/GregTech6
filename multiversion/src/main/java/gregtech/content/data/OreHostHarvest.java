package gregtech.content.data;
import gregtech.internal.tool.GTToolHelper;
import gregtech.content.item.ElectricToolItem;
import gregtech.content.item.GTToolItem;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
/** Only known native tool contracts. Unknown addon tiers/tool components retain their previous path. */
public final class OreHostHarvest {
 private OreHostHarvest() {}
 private static boolean category(String tool,Item item){
  return tool.equals("PICKAXE")?item instanceof PickaxeItem:tool.equals("SHOVEL")&&item instanceof ShovelItem;
 }
 private static boolean vanilla(ItemStack stack){
  if(!(stack.getItem() instanceof TieredItem tool) || !(tool.getTier() instanceof Tiers)
      || !BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft"))return false;
//? if >=1.21.1 {
/*  if(!java.util.Objects.equals(stack.get(net.minecraft.core.component.DataComponents.TOOL),
       stack.getItem().components().get(net.minecraft.core.component.DataComponents.TOOL)))return false;
*///?}
  return true;
 }
 private static int tier(Tiers tier){return switch(tier){case WOOD,GOLD->0;case STONE->1;case IRON->2;case DIAMOND->3;case NETHERITE->4;};}
 public static Optional<Boolean> decision(ItemStack stack,BlockState state){
  var params=OreHostPhysics.params(state);if(params.isEmpty())return Optional.empty();
  // Source Forge canHarvestBlock checks the block material before any held-item class/tier/power.
  // This is drop permission, NOT effective mining speed; custom tool speed remains untouched.
  if(!params.get().requiresToolForDrops())return Optional.of(true);
  return effectiveDecision(stack,state);
 }
 public static Optional<Boolean> effectiveDecision(ItemStack stack,BlockState state){
  var params=OreHostPhysics.params(state);if(params.isEmpty())return Optional.empty();
  var p=params.get();
  if(stack.isEmpty())return Optional.of(false);
  if(stack.getItem() instanceof ElectricToolItem electric){
   if(stack.getItem().getClass()!=ElectricToolItem.class || !BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("gt6ce"))return Optional.empty();
   boolean tool=electric.toolName().equals("Mining Drill")&&(p.tool().equals("PICKAXE")||p.tool().equals("SHOVEL"));
   return Optional.of(tool&&electric.isPoweredUsable(stack)
       &&electric.headMaterial(stack).getToolQuality()+electric.definition().quality()>=p.harvest());
  }
  if(stack.getItem() instanceof GTToolItem){
   if(stack.getItem().getClass()!=GTToolItem.class && stack.getItem().getClass()!=gregtech.content.item.PocketToolItem.class)return Optional.empty();
   if(!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("gt6ce"))return Optional.empty();
   var tag=GTToolHelper.getType(stack).harvestTag();
   boolean tool=p.tool().equals("PICKAXE")?BlockTags.MINEABLE_WITH_PICKAXE.equals(tag):BlockTags.MINEABLE_WITH_SHOVEL.equals(tag);
   return Optional.of(tool&&GTToolHelper.isUsable(stack)&&GTToolHelper.getHarvestLevel(stack)>=p.harvest());
  }
  if(vanilla(stack))return Optional.of(category(p.tool(),stack.getItem())
      &&tier((Tiers)((TieredItem)stack.getItem()).getTier())>=p.harvest());
  return Optional.empty();
 }
 public static Optional<Float> vanillaSpeed(ItemStack stack,BlockState state){
  var p=OreHostPhysics.params(state);if(p.isEmpty()||!vanilla(stack))return Optional.empty();
  return Optional.of(category(p.get().tool(),stack.getItem())?((TieredItem)stack.getItem()).getTier().getSpeed():1F);
 }
}
