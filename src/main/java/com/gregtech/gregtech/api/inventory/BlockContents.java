package com.gregtech.gregtech.api.inventory;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Local contents only: never traverse sided capabilities that may proxy another block. */
public interface BlockContents {
    void dropContents();

    static void drop(BlockEntity owner, IItemHandlerModifiable inventory) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide || inventory == null) return;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot).copy();
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
            drop(owner, stack);
        }
    }
    static void drop(BlockEntity owner, ItemStack stack) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide || stack == null || stack.isEmpty()) return;
        var pos = owner.getBlockPos();
        net.minecraft.world.Containers.dropItemStack(owner.getLevel(), pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5, stack.copy());
    }
}
