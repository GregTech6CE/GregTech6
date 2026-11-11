package gregtech.content.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/** GT6 {@code MultiTileEntityGregOLantern}: a horizontal-faced lit pumpkin with a Greg face. */
public final class GregLanternBlock extends HorizontalDirectionalBlock
        implements gregtech.engine.tool.ToolInteractionTarget {
    public GregLanternBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
//? if >=1.21.1 {
    /*@Override
    public com.mojang.serialization.MapCodec<HorizontalDirectionalBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return gregtech.engine.tool.ToolInteractions.toItemResult(use(state, level, pos, player, hand, hit));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return use(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
*///?} else {
    @Override
//?}
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        return gregtech.engine.tool.ToolInteractions.use(state, level, pos, player, hand, hit)
                ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    /** GT6 {@code MultiTileEntityGregOLantern} uses {@code SIDES_HORIZONTAL}. */
    @Override
    public gregtech.engine.tool.ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return gregtech.engine.tool.GTToolHelper.isMachineWrench(tool)
                ? gregtech.engine.tool.ToolInteractionSpec.facing(FACING,
                        gregtech.content.block.machine.MachineRotationType.HORIZONTAL)
                : null;
    }

    /** GT6 emits light 15 but reports no light opacity despite its full solid selection. */
    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }
}