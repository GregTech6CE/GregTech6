package com.gregtech.gregtech.platform.neoforge.machine;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import java.util.List;
/** Native item boundary for the complete original basic-machine tooltip. */
public final class BasicMachineItem extends BlockItem {
 public BasicMachineItem(BasicMachineBlock block,Properties props){super(block,props);}
 @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
  super.appendHoverText(stack,context,lines,flag);var spec=((BasicMachineBlock)getBlock()).basicSpec();
  com.gregtech.gregtech.client.MachineTooltips.appendBasicMachine(spec,lines);
 }
}
