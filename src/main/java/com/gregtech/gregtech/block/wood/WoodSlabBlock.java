package com.gregtech.gregtech.block.wood;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** A GT6 wood slab: two half slabs merged into a double slab return two items when mined. */
public final class WoodSlabBlock extends SlabBlock implements com.gregtech.gregtech.api.block.FaceGridSlab {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;
    private static final net.minecraft.world.phys.shapes.VoxelShape[] SHAPES = {
        box(0,0,0,16,8,16), box(0,8,0,16,16,16), box(0,0,0,16,16,8),
        box(0,0,8,16,16,16), box(0,0,0,8,16,16), box(8,0,0,16,16,16)
    };
    public WoodSlabBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, net.minecraft.core.Direction.DOWN));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(FACING);
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        var old = context.getLevel().getBlockState(context.getClickedPos());
        if (old.is(this)) return old.setValue(TYPE,SlabType.DOUBLE).setValue(WATERLOGGED,false);
        var face = com.gregtech.gregtech.util.GTPlacementCode.resolveSlabFace(context);
        return defaultBlockState().setValue(FACING,face).setValue(TYPE,face == net.minecraft.core.Direction.UP ? SlabType.TOP : SlabType.BOTTOM)
                .setValue(WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).getType() == net.minecraft.world.level.material.Fluids.WATER);
    }
    @Override public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
        if (state.getValue(TYPE)==SlabType.DOUBLE || !context.getItemInHand().is(asItem())) return false;
        return !context.replacingClickedOnBlock() || context.getClickedFace()==slabFace(state).getOpposite();
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return state.getValue(TYPE)==SlabType.DOUBLE ? net.minecraft.world.phys.shapes.Shapes.block() : SHAPES[slabFace(state).get3DDataValue()];
    }
    // Before vertical slabs, old top-half states did not carry FACING.
    private static net.minecraft.core.Direction slabFace(BlockState state) {
        return state.getValue(TYPE) == SlabType.TOP ? net.minecraft.core.Direction.UP : state.getValue(FACING);
    }
    @Override public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) { return state.setValue(FACING,rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) { return rotate(state,mirror.getRotation(state.getValue(FACING))); }


    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(this, state.getValue(TYPE) == SlabType.DOUBLE ? 2 : 1));
    }
}
