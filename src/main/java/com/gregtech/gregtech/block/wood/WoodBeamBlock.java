package com.gregtech.gregtech.block.wood;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** GT6 wooden beam (thin post-like block). */
public class WoodBeamBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    private final WoodSpecies species;

    private static final VoxelShape SHAPE_X = box(0, 4, 4, 16, 12, 12);
    private static final VoxelShape SHAPE_Y = box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape SHAPE_Z = box(4, 4, 0, 12, 12, 16);

    public WoodBeamBlock(WoodSpecies species, Properties properties) {
        super(properties);
        this.species = species;
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
    }

    public WoodSpecies species() { return species; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(AXIS); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(AXIS, ctx.getClickedFace().getAxis());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(AXIS)) {
            case X -> SHAPE_X;
            case Y -> SHAPE_Y;
            case Z -> SHAPE_Z;
        };
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) { return true; }
}
