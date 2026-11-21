package gregtech.content.block.machine;

import gregtech.content.item.GTBlockItem;
import gregtech.engine.machine.MachineSpec;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Original burning-box item ratings and front/air/ash/tool guidance. */
public final class BurningBoxBlockItem extends GTBlockItem {
    private final MachineSpec spec;

    public BurningBoxBlockItem(net.minecraft.world.level.block.Block block, Properties properties, MachineSpec spec) {
        super(block, properties);
        this.spec = spec;
    }

    @Override
    protected void appendTooltip(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendTooltip(stack, level, tooltip, flag);
        appendTooltips(tooltip);
    }

    private void appendTooltips(List<Component> tooltip) {
        if (getBlock() instanceof SolidBurningBoxBlock)
            gregtech.client.MachineTooltips.appendSolidBurningBox(spec, tooltip);
        else {
            gregtech.client.MachineTooltips.appendEfficiency(spec, tooltip);
            gregtech.client.MachineTooltips.appendEnergyOutHu(spec, tooltip);
            gregtech.client.MachineTooltips.appendRequiresAirFront(tooltip);
            gregtech.client.MachineTooltips.appendRequiresIgnitionFront(tooltip);
        }
    }
}
