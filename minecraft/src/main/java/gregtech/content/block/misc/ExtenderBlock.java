package gregtech.content.block.misc;

import gregtech.content.block.GTDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** GT6 Extender block — sided inventory/fluid interface extension. */
public class ExtenderBlock extends GTDirectionalBlock
        implements net.minecraft.world.level.block.EntityBlock, gregtech.engine.tool.ToolInteractionTarget {
    public static final DirectionProperty SECONDARY = DirectionProperty.create("secondary");

    public ExtenderBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SECONDARY, Direction.SOUTH));
    }

    @Override
    protected InteractionResult useBlock(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return gregtech.engine.tool.ToolInteractions.use(state, level, pos, player, hand, hit)
                ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, SECONDARY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction front = ctx.getNearestLookingDirection();
        return defaultBlockState().setValue(FACING, front).setValue(SECONDARY, front.getOpposite());
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExtenderBlockEntity(pos, state);
    }

    /** GT6 {@code MultiTileEntityExtender extends TileEntityBase10FacingDouble}. */
    @Override
    public gregtech.engine.tool.ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        if (gregtech.engine.tool.GTToolHelper.isMonkeyWrench(tool)) {
            return gregtech.engine.tool.ToolInteractionSpec.facing(
                    SECONDARY, gregtech.engine.tool.MachineRotationType.ALL);
        }
        return gregtech.engine.tool.GTToolHelper.isMachineWrench(tool)
                ? gregtech.engine.tool.ToolInteractionSpec.facing(FACING, gregtech.engine.tool.MachineRotationType.ALL)
                : null;
    }
}
