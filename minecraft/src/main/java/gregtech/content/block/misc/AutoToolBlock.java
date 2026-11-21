package gregtech.content.block.misc;

import gregtech.content.block.GTDirectionalBlock;
import gregtech.content.block.machine.MachineRotationType;
import gregtech.engine.tool.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

/** Six-sided automatic tool housing, sharing wrench selection and preview with other machines. */
public abstract class AutoToolBlock extends GTDirectionalBlock implements EntityBlock, ToolInteractionTarget {
    private final long input;
    private final int quality;

    protected AutoToolBlock(Properties properties, long input, int quality) {
        super(properties);
        this.input = input;
        this.quality = quality;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public long input() {
        return input;
    }

    public int quality() {
        return quality;
    }

    @Override
    protected InteractionResult useBlock(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (ToolInteractions.use(state, level, pos, player, hand, hit))
            return InteractionResult.sidedSuccess(level.isClientSide);
        if (player.isShiftKeyDown()
                && player.getItemInHand(hand).isEmpty()
                && level.getBlockEntity(pos) instanceof AutoToolBlockEntity tool) {
            if (!level.isClientSide) tool.setStopped(!tool.stopped());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void appendTooltip(
            ItemStack stack,
            net.minecraft.world.level.BlockGetter level,
            java.util.List<net.minecraft.network.chat.Component> lines,
            net.minecraft.world.item.TooltipFlag flag) {
        gregtech.client.AutomaticToolTooltips.append(this, lines);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.matchesTool(tool, GTToolType.WRENCH)
                ? ToolInteractionSpec.facing(FACING, MachineRotationType.ALL)
                : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (world, pos, current, entity) -> {
            if (entity instanceof AutoToolBlockEntity tool) tool.tick();
        };
    }
}
