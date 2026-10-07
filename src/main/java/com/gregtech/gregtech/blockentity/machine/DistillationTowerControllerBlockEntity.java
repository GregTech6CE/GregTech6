package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role;
import com.gregtech.gregtech.content.multiblock.ControllerStructureLayouts;
import com.gregtech.gregtech.content.multiblock.LargeMachineLayouts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** GT6 17101: heat-only base, bottom item/fluid IO, upper fluid outputs. */
public class DistillationTowerControllerBlockEntity extends LargeRecipeMachineBlockEntity {
    public DistillationTowerControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    @Override public void setSpec(com.gregtech.gregtech.api.machine.BasicMachineSpec spec) {
        super.setSpec(spec);
        for(var tank:getTanksOutput())tank.setCapacity(Long.MAX_VALUE);
    }
    @Override protected long inputMaximum() { return 1024; }
    @Override protected List<LargeMachineLayouts.Cell> structureCells() {
        return Layout.CELLS;
    }

    protected boolean cryogenic() {return false;}
    @Override protected void beforeMachineTick() {
        // Source onTick2 outputs fluids before doWork, even without energy or while stopped.
        if(faceConfig().fluidAutoOutput()<0)return;
        var front=getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        for(var tank:getTanksOutput()) {
            var offer=tank.getFluid();if(offer.isEmpty())continue;
            var entry=com.gregtech.gregtech.registry.GTFluids.entryForFluid(offer.getFluid());
            String name=entry==null?net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(offer.getFluid()).getPath():entry.registryName();
            int height=com.gregtech.gregtech.content.multiblock.OriginalDistillationOutputRules.fluidHeight(cryogenic(),name);
            var targetPos=worldPosition.relative(front.getOpposite(),3).above(height);
            if(!level.hasChunkAt(targetPos))continue;
            var target=level.getBlockEntity(targetPos)==null?null:level.getBlockEntity(targetPos).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,front).resolve().orElse(null);
            if(target==null)continue;
            int simulated=target.fill(offer.copy(),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
            if(simulated<=0)continue;
            offer.setAmount(Math.min(simulated,offer.getAmount()));
            int accepted=target.fill(offer.copy(),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            if(accepted<0 || accepted>offer.getAmount())throw new IllegalStateException("Invalid tower fluid receiver result: "+accepted);
            if(accepted>0)tank.remove(accepted);
        }
    }
    @Override protected void autoOutputFluids(net.minecraft.world.level.Level level,BlockPos pos,int relativeSide) {
        // Already moved each species to its rear outlet before work, once this tick.
    }
    @Override protected void autoOutputItems(net.minecraft.world.level.Level level,BlockPos pos,int relativeSide) {
        var front=getBlockState().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        var targetPos=pos.relative(front.getOpposite(),3);
        if(!level.hasChunkAt(targetPos))return;
        var target=level.getBlockEntity(targetPos)==null?null:level.getBlockEntity(targetPos).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,front).resolve().orElse(null);
        if(target==null)return;
        for(int slot=inputSlots();slot<inventory().getSlots();slot++) {
            var offer=inventory().getStackInSlot(slot);if(offer.isEmpty())continue;
            var result=com.gregtech.gregtech.content.transport.ItemPipeTransferAdapter.transfer(offer.copy(),()->target);
            if(result.accepted()>0)inventory().extractItem(slot,result.accepted(),false);
        }
    }
    @Override public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        for(var tank:getTanksOutput())tank.setCapacity(Long.MAX_VALUE);
    }
    private static final class Layout {
        static final List<LargeMachineLayouts.Cell> CELLS=LargeMachineLayouts.fromShared(com.gregtech.gregtech.content.multiblock.SharedDistillationTowerStructure.CELLS);
    }
}
