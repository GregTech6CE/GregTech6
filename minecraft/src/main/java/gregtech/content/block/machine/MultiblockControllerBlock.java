package gregtech.content.block.machine;

import gregtech.content.multiblock.RecipeControllerFamily;
import gregtech.engine.machine.BasicMachineSpec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;

import javax.annotation.Nullable;
import java.util.List;

/** Distillation Tower controller block — basic machine wiring + structure gate. */
public class MultiblockControllerBlock extends BasicMachineBlock {

    public MultiblockControllerBlock(
            BasicMachineSpec spec, Properties properties, RecipeControllerFamily controllerFamily) {
        super(spec, properties, controllerFamily);
    }

    @Override
//? if >=1.21.1 {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
*///?} else {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
//?}
        gregtech.client.OriginalControllerTooltips.structure(
                gregtech.content.multiblock.OriginalControllerTooltipData.Family.DISTILLATION_TOWER,tooltip);
        gregtech.client.OriginalControllerTooltips.basic(basicSpec(),tooltip);
    }
}
