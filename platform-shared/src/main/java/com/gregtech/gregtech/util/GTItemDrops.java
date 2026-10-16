package com.gregtech.gregtech.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** GT container contents retain their original stacks; only actual stack-size limits split them. */
public final class GTItemDrops {
    private GTItemDrops() {}
    public static void dropItemStack(Level level,double x,double y,double z,ItemStack stack) {
        if (level == null || level.isClientSide || stack == null || stack.isEmpty()) return;
        int maximum = Math.max(1, stack.getMaxStackSize());
        while (!stack.isEmpty()) {
            int count = Math.min(maximum, stack.getCount());
            ItemEntity entity = new ItemEntity(level, x, y, z, stack.split(count));
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
    }
    public static void dropContents(Level level,BlockPos pos,Container inventory) {
        if (level == null || level.isClientSide) return;
        for (int slot=0;slot<inventory.getContainerSize();slot++) {
            ItemStack stack = inventory.removeItemNoUpdate(slot);
            dropItemStack(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,stack);
        }
        inventory.setChanged();
    }
}
