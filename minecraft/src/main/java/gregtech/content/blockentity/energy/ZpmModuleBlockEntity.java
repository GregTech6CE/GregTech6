package gregtech.content.blockentity.energy;
import gregtech.content.energy.ZpmEnergy;
import gregtech.content.block.energy.ZpmModuleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class ZpmModuleBlockEntity extends BlockEntity {
    private long energy;
    public ZpmModuleBlockEntity(BlockPos p,BlockState s){super(gregtech.content.registry.GTBlockEntities.ZPM_MODULE.get(),p,s);}
    public long energy(){return energy;}
    public void initializeDungeonEnergy(boolean active){energy=gregtech.engine.energy.ZpmWorkRules.dungeonCharge(active);setChanged();}
    public void updateLight(){int light=gregtech.engine.energy.ZpmWorkRules.light(energy);if(level!=null&&getBlockState().getValue(ZpmModuleBlock.CHARGE)!=light)level.setBlockAndUpdate(worldPosition,getBlockState().setValue(ZpmModuleBlock.CHARGE,light));}
//? if >=1.21.1 {
    /*@Override protected void saveAdditional(CompoundTag t,net.minecraft.core.HolderLookup.Provider lookup){super.saveAdditional(t,lookup);t.putLong(ZpmEnergy.KEY,energy);}
    @Override protected void loadAdditional(CompoundTag t,net.minecraft.core.HolderLookup.Provider lookup){super.loadAdditional(t,lookup);energy=gregtech.engine.energy.ZpmWorkRules.charge(t.getLong(ZpmEnergy.KEY));}
*///?} else {
    @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.putLong(ZpmEnergy.KEY,energy);}
    @Override public void load(CompoundTag t){super.load(t);energy=gregtech.engine.energy.ZpmWorkRules.charge(t.getLong(ZpmEnergy.KEY));}
//?}
}
