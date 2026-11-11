package gregtech.content.block.inventory;

import gregtech.api.material.*;

import gregtech.engine.GTWaterloggable;
import gregtech.content.blockentity.inventory.LockerBlockEntity;
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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/** Native locker interaction: a main-hand click swaps the stored and worn armor sets. */
public class LockerBlock extends HorizontalDirectionalBlock
        implements EntityBlock, SimpleWaterloggedBlock, gregtech.engine.tool.ToolInteractionTarget {

//? if >=1.21.1 {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        // A second off-hand callback would immediately swap the armor back.
        if (hand == net.minecraft.world.InteractionHand.OFF_HAND) return InteractionResult.PASS;
        if (gregtech.engine.tool.ToolInteractions.use(state, level, pos, player, hand, hit))
            return InteractionResult.sidedSuccess(level.isClientSide);
        if (hit.getDirection() != state.getValue(FACING)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof LockerBlockEntity locker)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            locker.swapArmor(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

//? if >=1.21.1 {
    /*@Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }
*///?} else {
//?}
    private final gregtech.api.material.GTMaterial material;

    public gregtech.api.material.GTMaterial material() {
        return material;
    }

    public LockerBlock(Properties properties) {
        this(gregtech.content.material.Materials.StainlessSteel, properties);
    }

    public LockerBlock(gregtech.api.material.GTMaterial material, Properties properties) {
        super(properties);
        this.material = material;
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(GTWaterloggable.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(GTWaterloggable.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState base = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        return GTWaterloggable.getStateForPlacement(base, ctx);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        GTWaterloggable.updateShape(state, level, pos);
        return state;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LockerBlockEntity(pos, state);
    }

    /** GT6 {@code MultiTileEntityLocker extends TileEntityBase09FacingSingle}. */
    @Override
    public gregtech.engine.tool.ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return gregtech.engine.tool.GTToolHelper.isMachineWrench(tool)
                ? gregtech.engine.tool.ToolInteractionSpec.facing(FACING,
                        gregtech.content.block.machine.MachineRotationType.HORIZONTAL)
                : null;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof LockerBlockEntity locker) {
            locker.dropContents();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
//? if >=1.21.1 {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
*///?} else {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
//?}
        tooltip.add(Component.translatable("gt.tooltip.locker.1"));
    }
}
