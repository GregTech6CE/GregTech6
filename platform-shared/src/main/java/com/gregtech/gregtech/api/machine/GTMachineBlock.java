package com.gregtech.gregtech.api.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Blocks dismantled with a GT wrench (GT6 {@code canCollectDropsDirectly} machines). */
public interface GTMachineBlock {
    default ItemStack createMachineDrop(BlockState state) {
        return new ItemStack(state.getBlock());
    }
}
