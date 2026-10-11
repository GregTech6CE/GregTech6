package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.blockentity.machine.FusionReactorControllerBlockEntity;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;
import java.util.List;

/** GT6 17198: timed reactions, LU startup and EU generation. */
public class FusionReactorControllerBlock extends BasicMachineBlock {
    public FusionReactorControllerBlock(Properties properties) {
        super(com.gregtech.gregtech.content.machine.BasicMachineDefinitions.from(com.gregtech.gregtech.content.multiblock.OriginalMultiblockMachineParameters.fusionReactor()),properties);
        setBeTypeSupplier(()->GTBlockEntities.FUSION_REACTOR.get());
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return new FusionReactorControllerBlockEntity(pos,state);
    }
    // This controller uses a plain BlockItem, so the block owns its source tooltip rows.
    @Override public void appendHoverText(ItemStack stack,@Nullable BlockGetter level,List<Component> tooltip,TooltipFlag flag) {
        super.appendHoverText(stack,level,tooltip,flag);
        com.gregtech.gregtech.client.AdvancedControllerTooltips.append(basicSpec(),tooltip);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        if(level.isClientSide||type!=GTBlockEntities.FUSION_REACTOR.get())return null;
        return (l,p,s,be)->FusionReactorControllerBlockEntity.serverTick(l,p,s,(FusionReactorControllerBlockEntity)be);
    }
}
