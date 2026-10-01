package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.blockentity.machine.FusionReactorControllerBlockEntity;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

/** GT6 17198: timed reactions, LU startup and EU generation. */
public class FusionReactorControllerBlock extends BasicMachineBlock {
    public FusionReactorControllerBlock(Properties properties) {
        super(BasicMachineSpec.builder("fusion_reactor_main",GTMaterialRegistry.get("SteelGalvanized"))
                .machineType("fusionreactor").recipes(MachineRecipeMaps.Fusion)
                .energy(GregTechTags.Energy.TU,1).strength(12.5f,12.5f).build(),properties);
        setBeTypeSupplier(()->GTBlockEntities.FUSION_REACTOR.get());
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return new FusionReactorControllerBlockEntity(pos,state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        if(level.isClientSide||type!=GTBlockEntities.FUSION_REACTOR.get())return null;
        return (l,p,s,be)->FusionReactorControllerBlockEntity.serverTick(l,p,s,(FusionReactorControllerBlockEntity)be);
    }
}
