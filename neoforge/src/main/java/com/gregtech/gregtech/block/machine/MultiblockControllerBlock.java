package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.DistillationTowerControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/** Distillation Tower controller block — basic machine wiring + structure gate. */
public class MultiblockControllerBlock extends BasicMachineBlock {

    public MultiblockControllerBlock(BasicMachineSpec spec, Properties properties) {
        super(spec, properties);
    }

    @Override
    public BasicMachineBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new DistillationTowerControllerBlockEntity(type, pos, state);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        com.gregtech.gregtech.client.OriginalControllerTooltips.structure(
                com.gregtech.gregtech.content.multiblock.OriginalControllerTooltipData.Family.DISTILLATION_TOWER,tooltip);
        com.gregtech.gregtech.client.OriginalControllerTooltips.basic(basicSpec(),tooltip);
    }
}
