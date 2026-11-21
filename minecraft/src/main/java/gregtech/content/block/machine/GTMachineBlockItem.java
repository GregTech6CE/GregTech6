package gregtech.content.block.machine;

import gregtech.client.TankTooltips;
import gregtech.content.item.GTBlockItem;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Tank, fluid pipe and item pipe item with the original stack limit and tooltips. */
public class GTMachineBlockItem extends GTBlockItem {
    public GTMachineBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        var tag = gregtech.engine.stack.BlockEntityData.read(stack);
        if (getBlock() instanceof TankBlock
                && tag != null
                && tag.getCompound("gt.tank").getLong("Amount") > 0) return 1;
        return super.getMaxStackSize(stack);
    }

    @Override
    protected void appendTooltip(ItemStack stack, @Nullable Level level,
            @Nullable HolderLookup.Provider registries, List<Component> tooltip, TooltipFlag flag) {
        if (getBlock() instanceof TankBlock tank)
//? if >=1.21.1 {
            /*TankTooltips.appendTank(tank.spec(), stack, registries, tooltip);
*///?} else {
            TankTooltips.appendTank(tank.spec(), stack, tooltip);
//?}
        if (getBlock() instanceof FluidPipeBlock pipe) TankTooltips.appendPipe(pipe.spec(), tooltip);
        if (getBlock() instanceof ItemPipeBlock pipe) TankTooltips.appendItemPipe(pipe.spec(), tooltip);
    }
}
