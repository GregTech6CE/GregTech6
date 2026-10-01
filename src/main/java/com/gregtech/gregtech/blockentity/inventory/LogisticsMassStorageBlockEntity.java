package com.gregtech.gregtech.blockentity.inventory;

import com.gregtech.gregtech.content.logistics.LogisticsStorageHost;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/** GT6 6200+ metalset: a Mass Storage that joins the logistics network directly. */
public final class LogisticsMassStorageBlockEntity extends MassStorageBlockEntity implements LogisticsStorageHost {
    public LogisticsMassStorageBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.LOGISTICS_MASS_STORAGE.get(), pos, state);
    }

    @Override public boolean canLogistics(@Nullable Direction side) { return true; }
    @Override public int itemStoragePriority() { return template().isEmpty() ? 1 : 2; }
    @Override public int fluidStoragePriority() { return 0; }
    @Override public @Nullable ItemStack itemStorageFilter() { return null; }
    @Override public @Nullable Fluid fluidStorageFilter() { return null; }
}
