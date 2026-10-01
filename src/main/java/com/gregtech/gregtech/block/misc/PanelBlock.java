package com.gregtech.gregtech.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** GT6-style decorative thin panel block (1px thick, placed on any face). */
public class PanelBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.put(Direction.DOWN, box(0, 0, 0, 16, 1, 16));
        SHAPES.put(Direction.UP, box(0, 15, 0, 16, 16, 16));
        SHAPES.put(Direction.NORTH, box(0, 0, 0, 16, 16, 1));
        SHAPES.put(Direction.SOUTH, box(0, 0, 15, 16, 16, 16));
        SHAPES.put(Direction.WEST, box(0, 0, 0, 1, 16, 16));
        SHAPES.put(Direction.EAST, box(15, 0, 0, 16, 16, 16));
    }

    private final String materialName;
    private final int tintRgb;

    public PanelBlock(String materialName, int tintRgb, Properties properties) {
        super(properties);
        this.materialName = materialName;
        this.tintRgb = tintRgb;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public String materialName() { return materialName; }
    public int tintRgb() { return tintRgb; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) { return true; }
}
