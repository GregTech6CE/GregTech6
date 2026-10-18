package com.gregtech.gregtech.platform.neoforge;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
public final class NeoToolBindings {
 private NeoToolBindings(){}
 public static boolean matches(ItemStack stack,String kind){try{return GTToolHelper.matchesTool(stack,GTToolType.byId(kind));}catch(IllegalArgumentException ex){return false;}}
 public static boolean isWireCutter(ItemStack stack){return GTToolHelper.isWireCutter(stack);}
 public static boolean isScrewdriver(ItemStack stack){return GTToolHelper.isScrewdriver(stack);}
 public static boolean isMonkeyWrench(ItemStack stack){return GTToolHelper.isMonkeyWrench(stack);}
 public static boolean isSoftHammer(ItemStack stack){return GTToolHelper.isSoftHammer(stack);}
 public static boolean isMagnifyingGlass(ItemStack stack){return GTToolHelper.isMagnifyingGlass(stack);}
 public static boolean isMachineWrench(ItemStack stack){return GTToolHelper.isMachineWrench(stack);}
 public static boolean isTool(ItemStack stack){return GTToolHelper.isInteractionTool(stack);}
 public static void damageForToolClickReturn(ItemStack stack,long returned,LivingEntity user){GTToolHelper.damageForToolClickReturn(stack,returned,user);}
 public static void damageForUse(ItemStack stack,int amount,LivingEntity user){GTToolHelper.damageForUse(stack,amount,user);}
 public static void damageForUse(ItemStack stack,int amount,LivingEntity user,EquipmentSlot slot){GTToolHelper.damageForUse(stack,amount,user,slot);}
 public static String craftKind(ItemStack stack){return GTToolHelper.isTool(stack)?((com.gregtech.gregtech.item.GTToolItem)stack.getItem()).toolType().id():"";}
 public static int craftDamage(ItemStack stack){return com.gregtech.gregtech.api.tool.ToolCraftingRules.damage(craftKind(stack));}
}
