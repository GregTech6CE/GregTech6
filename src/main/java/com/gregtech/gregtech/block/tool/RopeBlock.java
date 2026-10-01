package com.gregtech.gregtech.block.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/**
 * GT6 Rope (MultiTileEntityRope): climbable wall strip; right-clicking with
 * more rope extends it downwards.
 */
public class RopeBlock extends HorizontalDirectionalBlock {

    /**
     * GT6 draws every rope with the same greyscale texture tinted by the rope's material
     * ({@code Loader_MultiTileEntities:2086-2091} hands each rope an {@code aMat}); the six ropes
     * therefore differ only by this colour.
     */
    private final int tintRgb;

    private static final VoxelShape[] SHAPES = new VoxelShape[4];
    static {
        // FACING = away from the supporting wall (vanilla ladder convention)
        SHAPES[Direction.NORTH.get2DDataValue()] = Block.box(6, 0, 12, 10, 16, 16);
        SHAPES[Direction.SOUTH.get2DDataValue()] = Block.box(6, 0, 0, 10, 16, 4);
        SHAPES[Direction.WEST.get2DDataValue()] = Block.box(12, 0, 6, 16, 16, 10);
        SHAPES[Direction.EAST.get2DDataValue()] = Block.box(0, 0, 6, 4, 16, 10);
    }

    public RopeBlock(Properties properties, int tintRgb) {
        super(properties);
        this.tintRgb = tintRgb;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    /** The rope material's colour (GT6 tints the shared greyscale texture with it). */
    public int tintRgb() { return tintRgb; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (ctx.getClickedFace().getAxis().isHorizontal()) {
            BlockState state = defaultBlockState().setValue(FACING, ctx.getClickedFace());
            if (state.canSurvive(ctx.getLevel(), ctx.getClickedPos())) return state;
        }
        // fall back to any survivable orientation (e.g. hanging from a rope above)
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState state = defaultBlockState().setValue(FACING, dir);
            if (state.canSurvive(ctx.getLevel(), ctx.getClickedPos())) return state;
        }
        return null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos wallPos = pos.relative(facing.getOpposite());
        if (level.getBlockState(wallPos).isFaceSturdy(level, wallPos, facing)) return true;
        BlockState above = level.getBlockState(pos.above());
        return above.is(this) && above.getValue(FACING) == facing;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor,
                                  net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // both hands fire use(); handling the off hand double-triggers
        if (hand == InteractionHand.OFF_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(asItem())) return InteractionResult.PASS;
        // extend the rope at the lowest air block below
        BlockPos cursor = pos.below();
        while (cursor.getY() > level.getMinBuildHeight() && level.getBlockState(cursor).is(this)) {
            cursor = cursor.below();
        }
        if (!level.getBlockState(cursor).canBeReplaced()) return InteractionResult.PASS;
        BlockState placed = state.setValue(FACING, state.getValue(FACING));
        if (!placed.canSurvive(level, cursor)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            level.setBlockAndUpdate(cursor, placed);
            level.playSound(null, cursor, soundType.getPlaceSound(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) held.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.tooltip.rope.1").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
