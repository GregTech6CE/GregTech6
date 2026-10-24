/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original 17111 CU controller, using the native basic-machine tools, menu and recipe engine. */
package gregtech.content.block.machine;

import gregtech.internal.machine.BasicMachineSpec;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

public class CryoDistillationControllerBlock extends BasicMachineBlock {
    public CryoDistillationControllerBlock(Properties properties) {
        super(makeSpec(),properties.strength(6,6).requiresCorrectToolForDrops());
        setBeTypeSupplier(()->gregtech.content.registry.GTBlockEntities.CRYO_DISTILLATION.get());
    }
    public static BasicMachineSpec makeSpec() {
        var p=gregtech.content.multiblock.OriginalMultiblockMachineParameters.cryoDistillationTower();
        return new BasicMachineSpec(p.id(),p.material(),p.machineName(),p.energyType(),p.tier(),p.energyIn(),p.energyOut(),
                p.hardness(),p.blastResistance(),gregtech.internal.energy.FaceConfig.from(p.faceConfig()),p.constructionMaterials(),
                gregtech.content.data.MachineRecipeMaps.CryoDistillationTower,p.parallelLimit(),p.energyInMin(),p.energyInMax());
    }
    @Override public BasicMachineBlockEntity createBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state) {
        return new gregtech.content.blockentity.machine.CryoDistillationControllerBlockEntity(pos,state);
    }
//? if >=1.21.1 {
    /*@Override public void appendHoverText(ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
*///?} else {
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.BlockGetter context,List<Component> tooltip,TooltipFlag flag) {
//?}
        gregtech.client.OriginalControllerTooltips.structure(
                gregtech.content.multiblock.OriginalControllerTooltipData.Family.CRYO_DISTILLATION_TOWER,tooltip);
        gregtech.client.OriginalControllerTooltips.basic(basicSpec(),tooltip);
    }
}
