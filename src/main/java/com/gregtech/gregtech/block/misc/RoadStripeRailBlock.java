package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** GT6's {@code BlockRailRoad}: a 0.5-speed road-stripe rail, not a full road block. */
public final class RoadStripeRailBlock extends BaseRailBlock {
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
    public static final BooleanProperty REFLECTOR = BooleanProperty.create("reflector");

    public RoadStripeRailBlock(Properties properties) {
        super(true, properties.noCollission());
        registerDefaultState(stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH)
                .setValue(REFLECTOR, false).setValue(WATERLOGGED, false));
    }

    @Override public Property<RailShape> getShapeProperty() { return SHAPE; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, REFLECTOR, WATERLOGGED);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        BlockPos pos = context.getClickedPos();
        double cross = state.getValue(SHAPE) == RailShape.EAST_WEST
                ? context.getClickLocation().z - pos.getZ()
                : context.getClickLocation().x - pos.getX();
        return state.setValue(REFLECTOR, cross > 0.5);
    }

    // BlockRailRoad: onBlockAdded and its rail shape update are NO-OP. Road stripes keep
    // the placement axis; adjacent rails must not bend them into curves or slopes.
    @Override protected BlockState updateDir(Level level, BlockPos pos, BlockState state, boolean first) {
        return state;
    }

    @Override protected void updateState(BlockState state, Level level, BlockPos pos, Block neighbor) {}

    @Override public float getRailMaxSpeed(BlockState state, Level level, BlockPos pos,
                                           @Nullable AbstractMinecart cart) {
        return TrackBlock.railSpeed(0.5F, state, level, pos);
    }

    @Override public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        TrackBlock.applySpeedToCart(0.5F, state, level, pos, cart);
        TrackBlock.moveCart(cart,state,level,pos,true);
    }

    private static boolean solid(Level level, BlockPos pos) {
        return level.getBlockState(pos).isRedstoneConductor(level, pos);
    }

    private static boolean isToggleTool(ItemStack tool) {
        return GTToolHelper.matchesTool(tool, GTToolType.CROWBAR)
                || GTToolHelper.matchesTool(tool, GTToolType.CHISEL)
                || GTToolHelper.matchesTool(tool, GTToolType.SCISSORS)
                || GTToolHelper.matchesTool(tool, GTToolType.KNIFE)
                || tool.is(Items.SHEARS);
    }

    /** GT6 tool-click toggles stripe/reflector metadata bit 8 in place. */
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        ItemStack tool = player.getItemInHand(hand);
        if (!isToggleTool(tool)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!level.setBlock(pos, state.cycle(REFLECTOR), Block.UPDATE_CLIENTS)) return InteractionResult.FAIL;
        GTToolHelper.damageForToolClickReturn(tool, 1000, player,
                hand == InteractionHand.OFF_HAND
                        ? net.minecraft.world.entity.EquipmentSlot.OFFHAND
                        : net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        return InteractionResult.CONSUME;
    }
}
