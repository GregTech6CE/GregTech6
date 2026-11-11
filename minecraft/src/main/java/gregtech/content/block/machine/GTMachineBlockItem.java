package gregtech.content.block.machine;

import gregtech.client.TankTooltips;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Tank, fluid pipe and item pipe item with the original stack limit and tooltips. */
public class GTMachineBlockItem extends BlockItem {
    public GTMachineBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
//? if >=1.21.1 {
        /*var stored = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        var tag = stored == null ? null : stored.copyTag();
*///?} else {
        var tag = stack.getTagElement("BlockEntityTag");
//?}
        if (getBlock() instanceof TankBlock
                && tag != null
                && tag.getCompound("gt.tank").getLong("Amount") > 0) return 1;
        return super.getMaxStackSize(stack);
    }

//? if >=1.21.1 {
    /*@Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        if (getBlock() instanceof TankBlock tank)
            TankTooltips.appendTank(tank.spec(), stack, context.registries(), lines);
        if (getBlock() instanceof FluidPipeBlock pipe) TankTooltips.appendPipe(pipe.spec(), lines);
        if (getBlock() instanceof ItemPipeBlock pipe) TankTooltips.appendItemPipe(pipe.spec(), lines);
    }
*///?} else {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (getBlock() instanceof TankBlock tank) TankTooltips.appendTank(tank.spec(), stack, tooltip);
        if (getBlock() instanceof FluidPipeBlock pipe) TankTooltips.appendPipe(pipe.spec(), tooltip);
        if (getBlock() instanceof ItemPipeBlock pipe) TankTooltips.appendItemPipe(pipe.spec(), tooltip);
    }
//?}
}
