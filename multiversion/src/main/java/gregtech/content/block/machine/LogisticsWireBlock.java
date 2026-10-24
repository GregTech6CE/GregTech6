package gregtech.content.block.machine;

import gregtech.internal.tool.GTToolHelper;
//? if >=1.21.1 {
/*import gregtech.internal.tool.ToolInteractions;
*///?} else {
//?}
import gregtech.internal.tool.ToolInteractionSpec;
import gregtech.internal.tool.ToolInteractionTarget;
//? if >=1.21.1 {
/*import gregtech.internal.transport.PipeConnections;
import gregtech.platform.neoforge.NeoToolBindings;
*///?} else {
import gregtech.internal.tool.ToolInteractions;
import gregtech.content.block.energy.ElectricWireBlock;
//?}
import gregtech.content.blockentity.machine.LogisticsWireBlockEntity;
import gregtech.content.logistics.LogisticsHost;
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
//? if >=1.21.1 {
    /*@Override
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isWireCutter(tool)
        ? ToolInteractionSpec.connections(ToolInteractionSpec.ConnectionKind.LOGISTICS) : null;
    }

*///?} else {
//?}
    public LogisticsWireBlock(Properties properties) {
        super(properties.noOcclusion());
        var initial = defaultBlockState();
//? if >=1.21.1 {
        /*for (var property : PipeConnections.CONNECTIONS) initial = initial.setValue(property, false);
*///?} else {
        for (var property : ElectricWireBlock.CONNECTIONS) initial = initial.setValue(property, false);
//?}
        registerDefaultState(initial);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
//? if >=1.21.1 {
        /*builder.add(PipeConnections.CONNECTIONS);
*///?} else {
        builder.add(ElectricWireBlock.CONNECTIONS);
//?}
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
//? if >=1.21.1 {
        /*return defaultBlockState().setValue(PipeConnections.propFor(context.getClickedFace().getOpposite()), true);
*///?} else {
        return defaultBlockState().setValue(ElectricWireBlock.propFor(context.getClickedFace().getOpposite()), true);
//?}
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogisticsWireBlockEntity(pos, state);
    }

//? if >=1.21.1 {
    /*protected InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
*///?} else {
    @Override
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isWireCutter(tool)
        ? ToolInteractionSpec.connections(ToolInteractionSpec.ConnectionKind.LOGISTICS) : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
//?}
                                           InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof LogisticsWireBlockEntity wire) {
            var coverResult = gregtech.content.logistics.LogisticsCoverInteraction.use(
                    wire, level, player, hand, hit.getDirection());
            if (coverResult != InteractionResult.PASS) return coverResult;
        }
//? if >=1.21.1 {
        /*if (!NeoToolBindings.isWireCutter(player.getItemInHand(hand))) return InteractionResult.PASS;
        if (!level.isClientSide) {
            Direction side = ToolInteractions.selectedFace(hit);
            var property = PipeConnections.propFor(side);
            boolean open = !state.getValue(property);
            BlockPos neighborPos = pos.relative(side);
            if (open && (level.getBlockEntity(pos) instanceof gregtech.content.logistics.LogisticsCoverHost host
                    && !host.logisticsCovers().get(side).isEmpty()
                    || level.getBlockEntity(neighborPos) instanceof gregtech.content.logistics.LogisticsCoverHost otherHost
                            && !otherHost.logisticsCovers().get(side.getOpposite()).isEmpty())) return InteractionResult.FAIL;
            if (open && level.hasChunkAt(neighborPos)) {
                var neighbor = level.getBlockState(neighborPos);
                boolean allowed = level.getBlockEntity(neighborPos) instanceof LogisticsHost host
                        ? host.canLogistics(side.getOpposite())
                        : neighbor.isAir() || !neighbor.getFluidState().isEmpty();
                if (!allowed) return InteractionResult.FAIL;
            }
            level.setBlockAndUpdate(pos, state.setValue(property, open));
            if (level.hasChunkAt(neighborPos) && level.getBlockState(neighborPos).getBlock() instanceof LogisticsWireBlock)
                level.setBlockAndUpdate(neighborPos,
                        level.getBlockState(neighborPos).setValue(PipeConnections.propFor(side.getOpposite()), open));
            NeoToolBindings.damageForUse(player.getItemInHand(hand), 1, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        var result = interact(state, level, pos, player, hand, hit);
        return result == InteractionResult.PASS ? net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : result == InteractionResult.FAIL ? net.minecraft.world.ItemInteractionResult.FAIL
                : net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
*///?} else {
        return ToolInteractions.use(state, level, pos, player, hand, hit)
                ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
//?}
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof LogisticsWireBlockEntity wire)
        wire.logisticsCovers().dropAll();
        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (level.isClientSide || old.is(this)) return;
        BlockState updatedState = state;
        for (Direction side : Direction.values()) {
            BlockPos neighborPos = pos.relative(side);
            if (!level.hasChunkAt(neighborPos)) continue;
            BlockState neighbor = level.getBlockState(neighborPos);
//? if >=1.21.1 {
            /*boolean connectedHere = updatedState.getValue(PipeConnections.propFor(side));
*///?} else {
            boolean connectedHere = updatedState.getValue(ElectricWireBlock.propFor(side));
//?}
            if (neighbor.getBlock() instanceof LogisticsWireBlock) {
                boolean coverBlocksConnection = level.getBlockEntity(neighborPos) instanceof gregtech.content.logistics.LogisticsCoverHost otherHost
                        && !otherHost.logisticsCovers().get(side.getOpposite()).isEmpty()
                            || level.getBlockEntity(pos) instanceof gregtech.content.logistics.LogisticsCoverHost host
                        && !host.logisticsCovers().get(side).isEmpty();
//? if >=1.21.1 {
                /*if (coverBlocksConnection) {
                    updatedState = updatedState.setValue(PipeConnections.propFor(side), false);
                    continue;
                }
                boolean connectedThere = neighbor.getValue(PipeConnections.propFor(side.getOpposite()));
*///?} else {
                if (coverBlocksConnection) {
                    updatedState = updatedState.setValue(ElectricWireBlock.propFor(side), false);
                    continue;
                }
                boolean connectedThere = neighbor.getValue(ElectricWireBlock.propFor(side.getOpposite()));
//?}
                if (connectedHere && !connectedThere)
                    level.setBlockAndUpdate(neighborPos,
//? if >=1.21.1 {
                        /*neighbor.setValue(PipeConnections.propFor(side.getOpposite()), true));
                if (connectedThere && !connectedHere) updatedState = updatedState.setValue(PipeConnections.propFor(side), true);
*///?} else {
                        neighbor.setValue(ElectricWireBlock.propFor(side.getOpposite()), true));
                if (connectedThere && !connectedHere) updatedState = updatedState.setValue(ElectricWireBlock.propFor(side), true);
//?}
            } else if (connectedHere) {
                // GT6 permits a free end in air or fluid, but refuses a solid that
                // does not offer a logistics face (TileEntityBase09Connector.connect).
                boolean permitted = level.getBlockEntity(neighborPos) instanceof LogisticsHost host
                        ? host.canLogistics(side.getOpposite())
                        : neighbor.isAir() || !neighbor.getFluidState().isEmpty();
//? if >=1.21.1 {
                /*if (!permitted) updatedState = updatedState.setValue(PipeConnections.propFor(side), false);
*///?} else {
                if (!permitted) updatedState = updatedState.setValue(ElectricWireBlock.propFor(side), false);
//?}
            }
        }
        if (updatedState != state) level.setBlockAndUpdate(pos, updatedState);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player
//? if >=1.21.1 {
                /*&& NeoToolBindings.isWireCutter(player.getMainHandItem())) return Shapes.block();
*///?} else {
                && GTToolHelper.isWireCutter(player.getMainHandItem())) return Shapes.block();
//?}
        var shape = Block.box(5, 5, 5, 11, 11, 11);
//? if >=1.21.1 {
        /*for (Direction side : Direction.values()) if (state.getValue(PipeConnections.propFor(side)))
*///?} else {
        for (Direction side : Direction.values()) if (state.getValue(ElectricWireBlock.propFor(side)))
//?}
            shape = Shapes.or(shape, Block.box(side == Direction.WEST ? 0 : 5,
                    side == Direction.DOWN ? 0 : 5, side == Direction.NORTH ? 0 : 5,
                    side == Direction.EAST ? 16 : 11, side == Direction.UP ? 16 : 11,
                    side == Direction.SOUTH ? 16 : 11));
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return getShape(state, level, pos, CollisionContext.empty());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder context) {
        return List.of(new ItemStack(this));
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return gregtech.content.logistics.LogisticsCoverSignals.at(level, pos, side.getOpposite());
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return getSignal(state, level, pos, side);
    }
}
