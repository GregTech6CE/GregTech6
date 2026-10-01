package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractionTarget;
import com.gregtech.gregtech.api.tool.ToolInteractions;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.blockentity.machine.LogisticsWireBlockEntity;
import com.gregtech.gregtech.content.logistics.LogisticsHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** GT6 24901: six-way cuttable logistics connector, 6/16 block diameter. */
public final class LogisticsWireBlock extends Block implements EntityBlock, ToolInteractionTarget {
    public LogisticsWireBlock(Properties properties) {
        super(properties.noOcclusion());
        var initial = defaultBlockState();
        for (var property : ElectricWireBlock.CONNECTIONS) initial = initial.setValue(property, false);
        registerDefaultState(initial);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ElectricWireBlock.CONNECTIONS);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ElectricWireBlock.propFor(context.getClickedFace().getOpposite()), true);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogisticsWireBlockEntity(pos, state);
    }

    @Override public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isWireCutter(tool)
                ? ToolInteractionSpec.connections(ToolInteractionSpec.ConnectionKind.LOGISTICS) : null;
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof LogisticsWireBlockEntity wire) {
            var coverResult = com.gregtech.gregtech.content.logistics.LogisticsCoverInteraction.use(
                    wire, level, player, hand, hit.getDirection());
            if (coverResult != InteractionResult.PASS) return coverResult;
        }
        return ToolInteractions.use(state, level, pos, player, hand, hit)
                ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof LogisticsWireBlockEntity wire)
            wire.logisticsCovers().dropAll();
        super.onRemove(state, level, pos, next, moving);
    }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (level.isClientSide || old.is(this)) return;
        BlockState next = state;
        for (Direction side : Direction.values()) {
            BlockPos adjacent = pos.relative(side);
            if (!level.hasChunkAt(adjacent)) continue;
            BlockState neighbor = level.getBlockState(adjacent);
            boolean ours = next.getValue(ElectricWireBlock.propFor(side));
            if (neighbor.getBlock() instanceof LogisticsWireBlock) {
                boolean theirs = neighbor.getValue(ElectricWireBlock.propFor(side.getOpposite()));
                if (ours && !theirs)
                    level.setBlockAndUpdate(adjacent,
                            neighbor.setValue(ElectricWireBlock.propFor(side.getOpposite()), true));
                if (theirs && !ours) next = next.setValue(ElectricWireBlock.propFor(side), true);
            } else if (ours) {
                // GT6 permits a free end in air or fluid, but refuses a solid that
                // does not offer a logistics face (TileEntityBase09Connector.connect).
                boolean permitted = level.getBlockEntity(adjacent) instanceof LogisticsHost host
                        ? host.canLogistics(side.getOpposite())
                        : neighbor.isAir() || !neighbor.getFluidState().isEmpty();
                if (!permitted) next = next.setValue(ElectricWireBlock.propFor(side), false);
            }
        }
        if (next != state) level.setBlockAndUpdate(pos, next);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext e && e.getEntity() instanceof Player player
                && GTToolHelper.isWireCutter(player.getMainHandItem())) return Shapes.block();
        var shape = Block.box(5, 5, 5, 11, 11, 11);
        for (Direction side : Direction.values()) if (state.getValue(ElectricWireBlock.propFor(side)))
            shape = Shapes.or(shape, Block.box(side == Direction.WEST ? 0 : 5,
                    side == Direction.DOWN ? 0 : 5, side == Direction.NORTH ? 0 : 5,
                    side == Direction.EAST ? 16 : 11, side == Direction.UP ? 16 : 11,
                    side == Direction.SOUTH ? 16 : 11));
        return shape;
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                                   CollisionContext context) {
        return getShape(state, level, pos, CollisionContext.empty());
    }

    @Override public List<ItemStack> getDrops(BlockState state,
                                              net.minecraft.world.level.storage.loot.LootParams.Builder context) {
        return List.of(new ItemStack(this));
    }

    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return com.gregtech.gregtech.content.logistics.LogisticsCoverSignals.at(level, pos, side);
    }
    @Override public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return getSignal(state, level, pos, side);
    }
}
