package com.gregtech.gregtech.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Simple decorative block that renders its texture from {@code gregtech:block/iconsets/<iconName>}. */
public class IconSetBlock extends Block {
    private final String iconName;

    public IconSetBlock(BlockBehaviour.Properties properties, String iconName) {
        super(properties);
        this.iconName = iconName;
    }

    public String iconName() { return iconName; }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        // GT6's ordinary treated planks are BlockBasePlanksFlammable (20/5).
        return iconName.equals("planks_treated") ? 20 : super.getFlammability(state, level, pos, face);
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return iconName.equals("planks_treated") ? 5 : super.getFireSpreadSpeed(state, level, pos, face);
    }
}
