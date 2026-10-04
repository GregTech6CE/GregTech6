package com.gregtech.gregtech.block.misc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
/** Transparent six-directional glass half blocks. Only a completely covered touching face is culled. */
public final class ColoredGlassSlabBlock extends CFoamSlabBlock {
    public ColoredGlassSlabBlock(Properties properties) {super(properties);}
    @Override public boolean skipRendering(BlockState state,BlockState adjacent,Direction side) {
        Direction own=state.getValue(FACING);
        if(side==own.getOpposite())return false;
        if(adjacent.getBlock() instanceof ColoredGlassBlock full && full.glow() && state.getValue(COLOR)==adjacent.getValue(ColoredGlassBlock.COLOR))return true;
        if(adjacent.getBlock() instanceof ColoredGlassSlabBlock && state.getValue(COLOR)==adjacent.getValue(COLOR)) {
            Direction other=adjacent.getValue(FACING);
            return other==side.getOpposite() || other!=side && other==own;
        }
        return super.skipRendering(state,adjacent,side);
    }
    @Override public int getLightBlock(BlockState state,BlockGetter level,BlockPos pos) {return 0;}
    @Override public float getShadeBrightness(BlockState state,BlockGetter level,BlockPos pos) {return 1;}
    @Override public boolean propagatesSkylightDown(BlockState state,BlockGetter level,BlockPos pos) {return true;}
}
