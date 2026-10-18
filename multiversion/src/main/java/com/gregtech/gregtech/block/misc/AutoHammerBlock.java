package com.gregtech.gregtech.block.misc;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public class AutoHammerBlock extends AutoToolBlock {
    public AutoHammerBlock(Properties properties) { this(properties,32,2); }
    public AutoHammerBlock(Properties properties,long input,int quality) { super(properties,input,quality); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new AutoHammerBlockEntity(pos,state); }
}
