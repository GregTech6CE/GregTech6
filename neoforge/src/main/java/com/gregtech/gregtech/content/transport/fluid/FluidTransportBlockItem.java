package com.gregtech.gregtech.content.transport.fluid;
import com.gregtech.gregtech.block.machine.TankBlock;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.client.TankTooltips;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;
import java.util.List;
/** Original tank/pipe tooltip and stack limits; BlockItem restores BLOCK_ENTITY_DATA on placement. */
public final class FluidTransportBlockItem extends BlockItem {
    public FluidTransportBlockItem(Block block,Properties properties){super(block,properties);}
    @Override public int getMaxStackSize(ItemStack stack) {
        var stored = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        var tag = stored == null ? null : stored.copyTag();
        if (getBlock() instanceof TankBlock && tag != null && tag.getCompound("gt.tank").getLong("Amount") > 0) return 1;
        return super.getMaxStackSize(stack);
    }

    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,context,lines,flag);
        if(getBlock() instanceof TankBlock tank)TankTooltips.appendTank(tank.spec(),stack,context.registries(),lines);
        if(getBlock() instanceof FluidPipeBlock pipe)TankTooltips.appendPipe(pipe.spec(),lines);
    }
}
