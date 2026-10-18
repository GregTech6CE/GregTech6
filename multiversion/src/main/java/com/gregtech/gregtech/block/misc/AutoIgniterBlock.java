package com.gregtech.gregtech.block.misc;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public class AutoIgniterBlock extends AutoToolBlock {
    public AutoIgniterBlock(Properties properties) { this(properties,32,1); }
    public AutoIgniterBlock(Properties properties,long input,int quality) { super(properties,input,quality); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new AutoIgniterBlockEntity(pos,state); }
}
