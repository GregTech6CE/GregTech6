package com.gregtech.gregtech.block.wood;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** A GT6 wood slab: two half slabs merged into a double slab return two items when mined. */
public final class WoodSlabBlock extends SlabBlock {
    public WoodSlabBlock(Properties properties) { super(properties); }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(this, state.getValue(TYPE) == SlabType.DOUBLE ? 2 : 1));
    }
}
