/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Original 17111 CU controller, using the native basic-machine tools, menu and recipe engine. */
package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
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
        setBeTypeSupplier(()->com.gregtech.gregtech.registry.GTBlockEntities.CRYO_DISTILLATION.get());
    }
    public static BasicMachineSpec makeSpec() {
        var p=com.gregtech.gregtech.content.multiblock.OriginalMultiblockMachineParameters.cryoDistillationTower();
        return new BasicMachineSpec(p.id(),p.material(),p.machineName(),p.energyType(),p.tier(),p.energyIn(),p.energyOut(),
                p.hardness(),p.blastResistance(),com.gregtech.gregtech.api.energy.FaceConfig.from(p.faceConfig()),p.constructionMaterials(),
                com.gregtech.gregtech.data.MachineRecipeMaps.CryoDistillationTower,p.parallelLimit(),p.energyInMin(),p.energyInMax());
    }
    @Override public BasicMachineBlockEntity createBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state) {
        return new com.gregtech.gregtech.blockentity.machine.CryoDistillationControllerBlockEntity(pos,state);
    }
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        com.gregtech.gregtech.client.OriginalControllerTooltips.structure(
                com.gregtech.gregtech.content.multiblock.OriginalControllerTooltipData.Family.CRYO_DISTILLATION_TOWER,tooltip);
        com.gregtech.gregtech.client.OriginalControllerTooltips.basic(basicSpec(),tooltip);
    }
}
