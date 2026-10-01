package com.gregtech.gregtech.api.material;
import net.minecraft.world.item.*;
import java.util.List;
/** Explicit vanilla output targets. Never merge tags of different forms or discard stack NBT. */
public final class MaterialUnification {
 private MaterialUnification() {}
 public static ItemStack canonical(ItemStack stack) {
  if(stack==null||stack.isEmpty()||stack.hasTag()||stack.isDamaged())return stack;
  var form=MaterialEquivalence.form(stack);if(form==null)return stack;
  Item target=switch(form.prefix().getName()+"/"+form.material().getName()) {
   case "ingot/Iron" -> Items.IRON_INGOT; case "ingot/Gold" -> Items.GOLD_INGOT;
   case "ingot/Copper" -> Items.COPPER_INGOT; case "nugget/Iron" -> Items.IRON_NUGGET;
   case "nugget/Gold" -> Items.GOLD_NUGGET; case "gem/Diamond" -> Items.DIAMOND;
   case "gem/Emerald" -> Items.EMERALD; case "gem/NetherQuartz" -> Items.QUARTZ;
   case "gem/Coal" -> Items.COAL; case "gem/Charcoal" -> Items.CHARCOAL;
   case "dust/Redstone" -> Items.REDSTONE; case "dust/Sugar" -> Items.SUGAR;
   case "dust/Gunpowder" -> Items.GUNPOWDER; case "stick/Wood" -> Items.STICK;
   default -> null;
  };
  if(target==null||target==stack.getItem())return stack;
  var result=new ItemStack(target,stack.getCount());
  var actual=MaterialEquivalence.form(result);
  return actual!=null&&actual.prefix()==form.prefix()&&actual.material()==form.material()?result:stack;
 }
}
