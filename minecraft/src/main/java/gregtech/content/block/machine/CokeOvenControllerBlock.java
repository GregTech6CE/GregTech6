package gregtech.content.block.machine;

import gregtech.content.multiblock.ControllerDeclarations;
import gregtech.engine.machine.BasicMachineSpec;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** GT6 fire-brick coke oven using the native basic-machine GUI, tools and processing. */
public class CokeOvenControllerBlock extends BasicMachineBlock {
    public CokeOvenControllerBlock() {
        this(Properties.of());
    }

    public CokeOvenControllerBlock(Properties properties) {
        super(
                makeSpec(),
                properties
                        .strength(5, 5)
                        .sound(net.minecraft.world.level.block.SoundType.STONE)
                        .requiresCorrectToolForDrops(),
                ControllerDeclarations.COKE_OVEN);
        setBeTypeSupplier(() -> gregtech.content.registry.GTBlockEntities.COKE_OVEN.get());
    }

    public static BasicMachineSpec makeSpec() {
        var p = gregtech.content.multiblock.OriginalMultiblockMachineParameters.cokeOven();
        return BasicMachineSpec.builtIn(p, gregtech.content.data.MachineRecipeMaps.CokeOven);
    }

    @Override
    protected void appendTooltip(
            ItemStack stack, net.minecraft.world.level.BlockGetter context, List<Component> tooltip, TooltipFlag flag) {
        gregtech.client.OriginalControllerTooltips.structure(
                gregtech.content.multiblock.OriginalControllerTooltipData.Family.COKE_OVEN, tooltip);
        gregtech.client.OriginalControllerTooltips.basic(basicSpec(), tooltip);
    }
}
