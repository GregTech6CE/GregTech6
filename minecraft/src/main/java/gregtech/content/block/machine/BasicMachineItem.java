package gregtech.content.block.machine;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=1.21.1 {
//?} else {
import net.minecraft.world.level.Level;
//?}

/** Item for the original basic machines; adds the shared machine tooltip. */
public final class BasicMachineItem extends BlockItem {
    public BasicMachineItem(BasicMachineBlock block, Properties props) {
        super(block, props);
    }

//? if >=1.21.1 {
    /*@Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        gregtech.client.MachineTooltips.appendBasicMachine(((BasicMachineBlock) getBlock()).basicSpec(), lines);
    }
*///?} else {
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        gregtech.client.MachineTooltips.appendBasicMachine(((BasicMachineBlock) getBlock()).basicSpec(), lines);
    }
//?}
}
