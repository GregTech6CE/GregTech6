package com.gregtech.gregtech.block.misc;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** A hive whose color is part of its registered identity, including its plain item stack. */
public final class FixedBumbleHiveBlock extends BumbleHiveBlock {
    private final DyeColor color;

    public FixedBumbleHiveBlock(Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {}

    public DyeColor color() { return color; }
}
