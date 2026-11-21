package gregtech.content.block.machine;

import gregtech.content.item.GTBlockItem;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Item for the original basic machines; adds the shared machine tooltip. */
public final class BasicMachineItem extends GTBlockItem {
    public BasicMachineItem(BasicMachineBlock block, Properties props) {
        super(block, props);
    }

    @Override
    protected void appendTooltip(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        super.appendTooltip(stack, level, lines, flag);
        gregtech.client.MachineTooltips.appendBasicMachine(((BasicMachineBlock) getBlock()).basicSpec(), lines);
    }
}
