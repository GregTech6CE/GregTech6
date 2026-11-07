package gregtech.content.blockentity.machine;

import gregtech.engine.multiblock.MultiblockLayout.Role;

import gregtech.content.multiblock.LargeMachineLayouts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import gregtech.engine.multiblock.StructureOutputs;
import gregtech.content.multiblock.OriginalMultiblockOutputRules;

/** GT6 17101: heat-only base, bottom item/fluid IO, upper fluid outputs. */
public class DistillationTowerControllerBlockEntity extends LargeRecipeMachineBlockEntity {
    public DistillationTowerControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    @Override public void setSpec(gregtech.engine.machine.BasicMachineSpec spec) {
        super.setSpec(spec);
        for(var tank:getTanksOutput())tank.setCapacity(Long.MAX_VALUE);
    }
    protected boolean cryogenic() {return false;}
    @Override protected void beforeMachineTick() {
        // Source onTick2 outputs fluids before doWork, even without energy or while stopped.
        if(faceConfig().fluidAutoOutput()<0
                || !automaticTransferEnabled(gregtech.engine.machine.MachineToolSettings.Transfer.FLUID_OUTPUT))return;
        var tanks = getTanksOutput();
        for(int index = 0; index < tanks.length; index++) {
            var tank = tanks[index];
            var offer=tank.getFluid();if(offer.isEmpty())continue;
            var entry=gregtech.content.registry.GTFluids.entryForFluid(offer.getFluid());
            String name=entry==null?net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(offer.getFluid()).getPath():entry.registryName();
            StructureOutputs.fluid(this, OriginalMultiblockOutputRules.towerFluid(cryogenic(), name),
                    getTanksInput().length + index, tank, Integer.MAX_VALUE);
        }
    }
    @Override protected void autoOutputFluids(net.minecraft.world.level.Level level,BlockPos pos,int relativeSide) {
        // Already moved each species to its rear outlet before work, once this tick.
    }
    @Override protected void autoOutputItems(net.minecraft.world.level.Level level,BlockPos pos,int relativeSide) {
        StructureOutputs.items(this, OriginalMultiblockOutputRules.TOWER_ITEMS, inventory(), inputSlots());
    }
//? if >=1.21.1 {
    /*@Override public void loadAdditional(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
*///?} else {
    @Override public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
//?}
        for(var tank:getTanksOutput())tank.setCapacity(Long.MAX_VALUE);
    }
}
