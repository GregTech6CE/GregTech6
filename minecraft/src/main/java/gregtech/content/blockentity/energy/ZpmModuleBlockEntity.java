package gregtech.content.blockentity.energy;

import gregtech.content.block.energy.ZpmModuleBlock;
import gregtech.content.blockentity.GTBlockEntity;
import gregtech.content.energy.ZpmEnergy;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public final class ZpmModuleBlockEntity extends GTBlockEntity {
    private long energy;

    public ZpmModuleBlockEntity(BlockPos p, BlockState s) {
        super(gregtech.content.registry.GTBlockEntities.ZPM_MODULE.get(), p, s);
    }

    public long energy() {
        return energy;
    }

    public void initializeDungeonEnergy(boolean active) {
        energy = gregtech.engine.energy.ZpmWorkRules.dungeonCharge(active);
        setChanged();
    }

    public void updateLight() {
        int light = gregtech.engine.energy.ZpmWorkRules.light(energy);
        if (level != null && getBlockState().getValue(ZpmModuleBlock.CHARGE) != light)
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(ZpmModuleBlock.CHARGE, light));
    }

    @Override
    protected void saveData(CompoundTag t, @Nullable HolderLookup.Provider lookup) {
        super.saveData(t, lookup);
        t.putLong(ZpmEnergy.KEY, energy);
    }

    @Override
    protected void loadData(CompoundTag t, @Nullable HolderLookup.Provider lookup) {
        super.loadData(t, lookup);
        energy = gregtech.engine.energy.ZpmWorkRules.charge(t.getLong(ZpmEnergy.KEY));
    }
}
