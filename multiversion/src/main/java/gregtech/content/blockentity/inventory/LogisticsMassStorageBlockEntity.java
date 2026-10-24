package gregtech.content.blockentity.inventory;

import gregtech.content.logistics.LogisticsStorageHost;
//? if >=1.21.1 {
/*import gregtech.platform.neoforge.logistics.StorageRegistries;
*///?} else {
import gregtech.content.registry.GTBlockEntities;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/** GT6 6200+ metalset: a Mass Storage that joins the logistics network directly. */
public final class LogisticsMassStorageBlockEntity extends MassStorageBlockEntity implements LogisticsStorageHost,
        gregtech.content.logistics.LogisticsCoverHost {
    private final gregtech.content.logistics.LogisticsCovers logisticsCovers =
            new gregtech.content.logistics.LogisticsCovers(this, this);

    @Override
    public gregtech.content.logistics.LogisticsCovers logisticsCovers() {
        return logisticsCovers;
    }

    public LogisticsMassStorageBlockEntity(BlockPos pos, BlockState state) {
//? if >=1.21.1 {
        /*super(StorageRegistries.LOGISTICS_MASS_STORAGE.get(), pos, state);
*///?} else {
        super(GTBlockEntities.LOGISTICS_MASS_STORAGE.get(), pos, state);
//?}
    }

//? if >=1.21.1 {
    /*@Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag,
            net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        logisticsCovers.save(tag, registries);
    }

    @Override
    public void loadAdditional(net.minecraft.nbt.CompoundTag tag,
            net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        logisticsCovers.load(tag, registries);
    }
*///?} else {
    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        logisticsCovers.save(tag);
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        logisticsCovers.load(tag);
    }
//?}

    @Override
    public boolean canLogistics(@Nullable Direction side) {
        return true;
    }

    @Override
    public int itemStoragePriority() {
        return template().isEmpty() ? 1 : 2;
    }

    @Override
    public int fluidStoragePriority() {
        return 0;
    }

    @Override
    public @Nullable ItemStack itemStorageFilter() {
        return null;
    }

    @Override
    public @Nullable Fluid fluidStorageFilter() {
        return null;
    }
}
