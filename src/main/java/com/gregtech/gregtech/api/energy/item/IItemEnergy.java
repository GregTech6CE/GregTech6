package com.gregtech.gregtech.api.energy.item;

import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

/** Item charge API (GT6 {@code IItemEnergy}). */
public interface IItemEnergy {

    boolean isEnergyType(ItemStack stack, GregTechTags.Tag energyType);

    /** Theoretical capability, independent of current charge or free capacity. */
    default boolean canEnergyInjection(ItemStack stack, GregTechTags.Tag type, long size) {
        return stack.getCount() == 1 && size > 0 && isEnergyType(stack, type);
    }
    default boolean canEnergyExtraction(ItemStack stack, GregTechTags.Tag type, long size) {
        return canEnergyInjection(stack, type, size);
    }

    long getEnergyCapacity(ItemStack stack, GregTechTags.Tag energyType);

    long getEnergyStored(ItemStack stack, GregTechTags.Tag energyType);

    long doEnergyInjection(GregTechTags.Tag energyType, ItemStack stack, long size, long amount,
                           Level level, BlockPos pos, boolean doInject);

    long doEnergyExtraction(GregTechTags.Tag energyType, ItemStack stack, long size, long amount,
                            Level level, BlockPos pos, boolean doExtract);
}
