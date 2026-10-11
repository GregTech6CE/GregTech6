package com.gregtech.gregtech.block.machine;
import net.minecraft.world.item.*;import net.minecraft.network.chat.Component;
/** Native item boundary for original pipe material name, stack size and full original tooltips. */
public final class ItemPipeBlockItem extends BlockItem {
 public ItemPipeBlockItem(ItemPipeBlock block,Properties properties){super(block,properties);}
 @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,java.util.List<Component> lines,TooltipFlag flag){super.appendHoverText(stack,context,lines,flag);com.gregtech.gregtech.client.TankTooltips.appendItemPipe(((ItemPipeBlock)getBlock()).spec(),lines);}
}
