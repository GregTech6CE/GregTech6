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

/** Saved legacy panel block; original panel items attach to hosts and refuse new block placement. */
public class PanelBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for(var face:Direction.values()){var v=com.gregtech.gregtech.content.transport.PanelCatalog.bounds(face.ordinal());SHAPES.put(face,box(v[0],v[1],v[2],v[3],v[4],v[5]));}
    }

    private final String materialName;
    private final int tintRgb;

    public PanelBlock(String materialName, int tintRgb, Properties properties) {
        super(properties);
        this.materialName = materialName;
        this.tintRgb = tintRgb;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public com.gregtech.gregtech.content.transport.PanelCatalog.Spec spec() { return com.gregtech.gregtech.content.transport.PanelCatalog.get(materialName); }
    public String materialName() { return materialName; }
    public int tintRgb() { return tintRgb; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) { return true; }
}
