package com.gregtech.gregtech.blockentity.inventory;

import com.gregtech.gregtech.content.logistics.LogisticsStorageHost;
import com.gregtech.gregtech.platform.neoforge.logistics.StorageRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/** GT6 6200+ metalset: a Mass Storage that joins the logistics network directly. */
public final class LogisticsMassStorageBlockEntity extends MassStorageBlockEntity implements LogisticsStorageHost, com.gregtech.gregtech.content.logistics.LogisticsCoverHost {
    private final com.gregtech.gregtech.content.logistics.LogisticsCovers logisticsCovers = new com.gregtech.gregtech.content.logistics.LogisticsCovers(this,this);
    @Override public com.gregtech.gregtech.content.logistics.LogisticsCovers logisticsCovers(){return logisticsCovers;}
    public LogisticsMassStorageBlockEntity(BlockPos pos, BlockState state) {
        super(StorageRegistries.LOGISTICS_MASS_STORAGE.get(), pos, state);
    }

    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries){super.saveAdditional(tag,registries);logisticsCovers.save(tag,registries);}
    @Override public void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries){super.loadAdditional(tag,registries);logisticsCovers.load(tag,registries);}

    @Override public boolean canLogistics(@Nullable Direction side) { return true; }
    @Override public int itemStoragePriority() { return template().isEmpty() ? 1 : 2; }
    @Override public int fluidStoragePriority() { return 0; }
    @Override public @Nullable ItemStack itemStorageFilter() { return null; }
    @Override public @Nullable Fluid fluidStorageFilter() { return null; }
}
