package gregtech.content.block.machine;

import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import gregtech.content.cover.CoverConnections;
import gregtech.content.logistics.LogisticsCoverHost;
import gregtech.engine.GTWaterloggable;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.ToolInteractions;
import gregtech.engine.tool.ToolInteractionSpec;
import gregtech.engine.tool.ToolInteractionTarget;
//? if >=1.21.1 {
/*import gregtech.engine.transport.PipeConnections;
*///?} else {
import gregtech.content.block.energy.ElectricWireBlock;
//?}
import gregtech.content.blockentity.machine.LogisticsWireBlockEntity;
import gregtech.content.logistics.LogisticsHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** GT6 24901: six-way cuttable logistics connector, 6/16 block diameter. */
public final class LogisticsWireBlock extends Block implements EntityBlock, ToolInteractionTarget, SimpleWaterloggedBlock {
//? if >=1.21.1 {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return ToolInteractions.toItemResult(use(state, level, pos, player, hand, hit));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return use(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
*///?} else {
    @Override
//?}
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof LogisticsWireBlockEntity wire) {
            var coverResult = gregtech.content.logistics.LogisticsCoverInteraction.use(
                    wire, level, player, hand, hit.getDirection());
            if (coverResult != InteractionResult.PASS) return coverResult;
        }
        ItemStack tool = player.getItemInHand(hand);
        if (!GTToolHelper.isWireCutter(tool)) return InteractionResult.PASS;
        Direction side = ToolInteractions.selectedFace(hit);
        BooleanProperty property = propFor(side);
        boolean open = !state.getValue(property);
        BlockPos neighborPos = pos.relative(side);
        if (!player.mayBuild() || !level.mayInteract(player, pos)
                || !level.hasChunkAt(neighborPos) || !level.mayInteract(player, neighborPos))
            return InteractionResult.FAIL;
        if (open && hasBlockingCover(level, pos, neighborPos, side)) return InteractionResult.FAIL;
        if (open && !canOpenToward(level, neighborPos, side.getOpposite())) return InteractionResult.FAIL;
        level.setBlockAndUpdate(pos, state.setValue(property, open));
        BlockState neighborState = level.getBlockState(neighborPos);
        boolean updatedNeighbor = false;
        if (neighborState.getBlock() instanceof LogisticsWireBlock) {
            BooleanProperty opposite = propFor(side.getOpposite());
            if (neighborState.getValue(opposite) != open) {
                level.setBlockAndUpdate(neighborPos, neighborState.setValue(opposite, open));
                updatedNeighbor = true;
            }
        }
        if (!level.isClientSide) {
            GTToolHelper.damageForUse(tool, 1, player);
            // ServerGamePacketListenerImpl.handleUseItemOn only resends pos and clicked-face;
            // push neighborPos immediately so both wire ends update in the same tick.
            if (updatedNeighbor && side != hit.getDirection() && player instanceof ServerPlayer serverPlayer)
                serverPlayer.connection.send(new ClientboundBlockUpdatePacket(level, neighborPos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static boolean hasBlockingCover(Level level, BlockPos pos, BlockPos neighborPos, Direction side) {
        if (level.getBlockEntity(pos) instanceof LogisticsCoverHost host
                && !host.logisticsCovers().get(side).isEmpty()) return true;
        if (!CoverConnections.canConnect(level, pos, side)) return true;
        return level.hasChunkAt(neighborPos)
                && level.getBlockEntity(neighborPos) instanceof LogisticsCoverHost otherHost
                && !otherHost.logisticsCovers().get(side.getOpposite()).isEmpty();
    }

    public static boolean canOpenToward(Level level, BlockPos neighborPos, Direction opposite) {
        var neighbor = level.getBlockState(neighborPos);
        // Adjacent logistics wires use mutual connector pairing; LogisticsHost.canLogistics tracks already-open faces.
        if (neighbor.getBlock() instanceof LogisticsWireBlock) return true;
        return level.getBlockEntity(neighborPos) instanceof LogisticsHost host
                ? host.canLogistics(opposite)
                : neighbor.isAir() || !neighbor.getFluidState().isEmpty();
    }

    private static BooleanProperty propFor(Direction side) {
//? if >=1.21.1 {
        /*return PipeConnections.propFor(side);
*///?} else {
        return ElectricWireBlock.propFor(side);
//?}
    }

    @Override
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isWireCutter(tool)
                ? ToolInteractionSpec.connections(ToolInteractionSpec.ConnectionKind.LOGISTICS) : null;
    }

    public LogisticsWireBlock(Properties properties) {
        super(properties.noOcclusion());
        var initial = defaultBlockState();
//? if >=1.21.1 {
        /*for (var property : PipeConnections.CONNECTIONS) initial = initial.setValue(property, false);
*///?} else {
        for (var property : ElectricWireBlock.CONNECTIONS) initial = initial.setValue(property, false);
//?}
        registerDefaultState(initial.setValue(GTWaterloggable.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GTWaterloggable.WATERLOGGED);
//? if >=1.21.1 {
        /*builder.add(PipeConnections.CONNECTIONS);
*///?} else {
        builder.add(ElectricWireBlock.CONNECTIONS);
//?}
    }

    private @javax.annotation.Nullable Direction placementFace;

    private @javax.annotation.Nullable Direction consumePlacementFace() {
        Direction face = placementFace;
        placementFace = null;
        return face;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!context.getLevel().isClientSide) placementFace = context.getClickedFace();
        return GTWaterloggable.getStateForPlacement(defaultBlockState(), context);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogisticsWireBlockEntity(pos, state);
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
        if (level.isClientSide || old.is(this) || placementFace != null) return;
        connectOnPlace(level, pos, state, null, null);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @javax.annotation.Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        Direction clickedFace = consumePlacementFace();
        Player player = placer instanceof Player p ? p : null;
        connectOnPlace(level, pos, level.getBlockState(pos),
                clickedFace == null ? null : clickedFace.getOpposite(), player);
    }

    private static void connectOnPlace(Level level, BlockPos pos, BlockState state,
            @javax.annotation.Nullable Direction placedSide, @javax.annotation.Nullable Player placer) {
        if (!(state.getBlock() instanceof LogisticsWireBlock)) return;
        BlockState updatedState = state;
        for (Direction side : Direction.values()) {
            BlockPos neighborPos = pos.relative(side);
            if (!level.hasChunkAt(neighborPos)) continue;
            BooleanProperty property = propFor(side);
            boolean wantsConnect = side == placedSide;
            if (wantsConnect && placer != null && !level.mayInteract(placer, neighborPos)) wantsConnect = false;
            BlockState neighbor = level.getBlockState(neighborPos);
            if (neighbor.getBlock() instanceof LogisticsWireBlock) {
                if (hasBlockingCover(level, pos, neighborPos, side)) continue;
                BooleanProperty opposite = propFor(side.getOpposite());
                boolean connectedThere = neighbor.getValue(opposite);
                if (wantsConnect && !connectedThere) {
                    level.setBlockAndUpdate(neighborPos, neighbor.setValue(opposite, true));
                    if (placer instanceof ServerPlayer sp && (placedSide == null || side != placedSide))
                        sp.connection.send(new ClientboundBlockUpdatePacket(level, neighborPos));
                }
                if (wantsConnect || connectedThere) updatedState = updatedState.setValue(property, true);
            } else if (wantsConnect && !hasBlockingCover(level, pos, neighborPos, side)
                    && canOpenToward(level, neighborPos, side.getOpposite())) {
                // GT6 permits a free end in air or fluid, but refuses a solid that
                // does not offer a logistics face (TileEntityBase09Connector.connect).
                updatedState = updatedState.setValue(property, true);
            }
        }
        if (updatedState != state) level.setBlockAndUpdate(pos, updatedState);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player
                && GTToolHelper.isWireCutter(player.getMainHandItem())) return Shapes.block();
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

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        GTWaterloggable.updateShape(state, level, pos);
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }
}
