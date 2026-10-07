package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.content.logistics.LogisticsStorageHost;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/** GT6 multi-tile 32072: an unsealed barrel that participates in the logistics network directly. */
public final class LogisticsTankBlockEntity extends TankBlockEntity implements LogisticsStorageHost, com.gregtech.gregtech.content.logistics.LogisticsCoverHost {
    private final com.gregtech.gregtech.content.logistics.LogisticsCovers logisticsCovers = new com.gregtech.gregtech.content.logistics.LogisticsCovers(this,this);
    @Override public com.gregtech.gregtech.content.logistics.LogisticsCovers logisticsCovers(){return logisticsCovers;}
    public LogisticsTankBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.LOGISTICS_TANK.get(), pos, state);
        // GT6 FluidTankGT.setPreventDraining(true) retains the *fluid type* after the final drain;
        // the port's older preventDraining flag forbids all extraction and must not be used here.
        getFluidTank().setKeepFilterOnEmpty(true);
    }

    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag){super.saveAdditional(tag);logisticsCovers.save(tag);}
    @Override public void load(net.minecraft.nbt.CompoundTag tag){super.load(tag);logisticsCovers.load(tag);}
    @Override public void dropComponentCovers(){super.dropComponentCovers();logisticsCovers.dropAll();}

    @Override public boolean canLogistics(@Nullable Direction side) { return true; }
    @Override public int itemStoragePriority() { return 0; }
    @Override public int fluidStoragePriority() {
        return getFluidTank().getFluidLong().isEmpty() ? 1 : 2;
    }
    @Override public @Nullable ItemStack itemStorageFilter() { return null; }
    @Override public @Nullable Fluid fluidStorageFilter() {
        var retained = getFluidTank().getFluidLong();
        return retained.isEmpty() ? null : retained.getFluid();
    }

    /** GT6 MultiTileEntityBarrelLogistics.canBeSealed() is false. */
    @Override public void toggleSoftHammerState() { }
}
