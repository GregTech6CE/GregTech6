package com.gregtech.gregtech.block.machine;
import com.gregtech.gregtech.api.machine.MachineSpec;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import java.util.List;
/** Original burning-box item ratings and front/air/ash/tool guidance. */
public final class BurningBoxBlockItem extends BlockItem {
 private final MachineSpec spec;
 public BurningBoxBlockItem(net.minecraft.world.level.block.Block block,Properties properties,MachineSpec spec){super(block,properties);this.spec=spec;}
 @Override public Component getName(ItemStack stack){return getBlock() instanceof SolidBurningBoxBlock?Component.literal(spec.materialName()+" Burning Box"):super.getName(stack);}
 @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){super.appendHoverText(stack,context,tooltip,flag);if(getBlock() instanceof SolidBurningBoxBlock)com.gregtech.gregtech.client.MachineTooltips.appendSolidBurningBox(spec,tooltip);else{com.gregtech.gregtech.client.MachineTooltips.appendEfficiency(spec,tooltip);com.gregtech.gregtech.client.MachineTooltips.appendEnergyOutHu(spec,tooltip);com.gregtech.gregtech.client.MachineTooltips.appendRequiresAirFront(tooltip);com.gregtech.gregtech.client.MachineTooltips.appendRequiresIgnitionFront(tooltip);}}
}
