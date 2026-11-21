/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original 17111 CU controller, using the native basic-machine tools, menu and recipe engine. */
package gregtech.content.block.machine;

import gregtech.content.multiblock.ControllerDeclarations;
import gregtech.engine.machine.BasicMachineSpec;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class CryoDistillationControllerBlock extends BasicMachineBlock {
    public CryoDistillationControllerBlock(Properties properties) {
        super(
                makeSpec(),
                properties.strength(6, 6).requiresCorrectToolForDrops(),
                ControllerDeclarations.CRYO_DISTILLATION);
        setBeTypeSupplier(() -> gregtech.content.registry.GTBlockEntities.CRYO_DISTILLATION.get());
    }

    public static BasicMachineSpec makeSpec() {
        var p = gregtech.content.multiblock.OriginalMultiblockMachineParameters.cryoDistillationTower();
        return BasicMachineSpec.builtIn(p, gregtech.content.data.MachineRecipeMaps.CryoDistillationTower);
    }

    @Override
    protected void appendTooltip(
            ItemStack stack, net.minecraft.world.level.BlockGetter context, List<Component> tooltip, TooltipFlag flag) {
        gregtech.client.OriginalControllerTooltips.structure(
                gregtech.content.multiblock.OriginalControllerTooltipData.Family.CRYO_DISTILLATION_TOWER, tooltip);
        gregtech.client.OriginalControllerTooltips.basic(basicSpec(), tooltip);
    }
}
