package com.gregtech.gregtech.platform.neoforge.smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Neo block callbacks, including 1.21's separate item and empty-hand dispatch. */
public final class SmelteryBlock extends Block implements EntityBlock {
    public enum Kind { BURNING_BOX, CRUCIBLE, MOLD }
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape CRUCIBLE = Shapes.join(Shapes.block(),
            Shapes.box(0.125, 0.125, 0.125, 0.875, 1, 0.875), BooleanOp.ONLY_FIRST);
    private static final VoxelShape MOLD_SHAPE = Shapes.box(0, 0, 0, 1, 0.375, 1);
    private final Kind kind;
    public SmelteryBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, LIT); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind == Kind.CRUCIBLE ? CRUCIBLE : kind == Kind.MOLD ? MOLD_SHAPE : Shapes.block();
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case BURNING_BOX -> new SolidBurningBoxEntity(pos, state);
            case CRUCIBLE -> new SmeltingCrucibleEntity(pos, state);
            case MOLD -> new MoldEntity(pos, state);
        };
    }
    @Override @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (kind == Kind.BURNING_BOX && type == SmelteryRegistries.SOLID_BURNING_BOX.get())
            return (l, p, s, be) -> ((SolidBurningBoxEntity) be).serverTick();
        if (kind == Kind.CRUCIBLE && type == SmelteryRegistries.SMELTING_CRUCIBLE.get())
            return (l, p, s, be) -> ((SmeltingCrucibleEntity) be).serverTick();
        if (kind == Kind.MOLD && type == SmelteryRegistries.MOLD.get())
            return (l, p, s, be) -> ((MoldEntity) be).serverTick();
        return null;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                         Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(level, pos, player, hand, hit) ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(level, pos, player, InteractionHand.MAIN_HAND, hit) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }
    private boolean interact(Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof SolidBurningBoxEntity box) return box.use(player, hand, hit);
        if (level.getBlockEntity(pos) instanceof MoldEntity mold) return mold.use(player, hand, hit);
        if (level.getBlockEntity(pos) instanceof SmeltingCrucibleEntity crucible) return crucible.use(player, hand, hit);
        return false;
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) {
            if (level.getBlockEntity(pos) instanceof SolidBurningBoxEntity box) box.dropContents();
            if (level.getBlockEntity(pos) instanceof SmeltingCrucibleEntity crucible) crucible.dropContents();
            if (level.getBlockEntity(pos) instanceof MoldEntity mold) mold.dropContents();
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
