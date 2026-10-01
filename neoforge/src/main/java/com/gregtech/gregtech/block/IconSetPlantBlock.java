package com.gregtech.gregtech.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Cross-rendered iconset plant (flowers, glowtus, saplings) with vanilla bush placement rules. */
public class IconSetPlantBlock extends BushBlock {
    private static final VoxelShape SHAPE = box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    @Override public com.mojang.serialization.MapCodec<IconSetPlantBlock> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    private final String iconName;

    public IconSetPlantBlock(Properties properties, String iconName) {
        super(properties);
        this.iconName = iconName;
    }

    public String iconName() { return iconName; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){return java.util.List.of(new net.minecraft.world.item.ItemStack(this));}
}
