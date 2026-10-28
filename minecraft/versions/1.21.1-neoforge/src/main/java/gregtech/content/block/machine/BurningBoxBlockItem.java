package gregtech.content.block.machine;

import gregtech.engine.machine.MachineSpec;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Original burning-box item ratings and front/air/ash/tool guidance. */
public final class BurningBoxBlockItem extends BlockItem {
    private final MachineSpec spec;

    public BurningBoxBlockItem(net.minecraft.world.level.block.Block block, Properties properties, MachineSpec spec) {
        super(block, properties);
        this.spec = spec;
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
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
