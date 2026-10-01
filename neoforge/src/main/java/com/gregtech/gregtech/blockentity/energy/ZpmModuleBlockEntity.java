package com.gregtech.gregtech.blockentity.energy;
import com.gregtech.gregtech.content.energy.ZpmEnergy;
import com.gregtech.gregtech.block.energy.ZpmModuleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class ZpmModuleBlockEntity extends BlockEntity {
    private long energy;
    public ZpmModuleBlockEntity(BlockPos p,BlockState s){super(com.gregtech.gregtech.registry.GTBlockEntities.ZPM_MODULE.get(),p,s);}
    public long energy(){return energy;}
    public void initializeDungeonEnergy(boolean active){energy=com.gregtech.gregtech.content.energy.ZpmWorkRules.dungeonCharge(active);setChanged();}
    public void updateLight(){int light=com.gregtech.gregtech.content.energy.ZpmWorkRules.light(energy);if(level!=null&&getBlockState().getValue(ZpmModuleBlock.CHARGE)!=light)level.setBlockAndUpdate(worldPosition,getBlockState().setValue(ZpmModuleBlock.CHARGE,light));}
    @Override protected void saveAdditional(CompoundTag t,net.minecraft.core.HolderLookup.Provider lookup){super.saveAdditional(t,lookup);t.putLong(ZpmEnergy.KEY,energy);}
    @Override protected void loadAdditional(CompoundTag t,net.minecraft.core.HolderLookup.Provider lookup){super.loadAdditional(t,lookup);energy=com.gregtech.gregtech.content.energy.ZpmWorkRules.charge(t.getLong(ZpmEnergy.KEY));}
}
