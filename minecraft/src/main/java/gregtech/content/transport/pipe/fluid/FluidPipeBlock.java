package gregtech.content.transport.pipe.fluid;

import gregtech.content.registry.GTBlockEntities;
import gregtech.content.transport.pipe.MaterialPipeBlock;
import gregtech.engine.GTWaterloggable;
import gregtech.engine.machine.PipeGeometry;
import gregtech.engine.machine.PipeSpec;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import gregtech.engine.tool.ToolInteractionSpec;
import gregtech.engine.tool.ToolInteractionTarget;
import gregtech.engine.tool.ToolInteractions;
import gregtech.util.GTEntityHelper;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** GT6 fluid pipe block with wrench-toggleable connections and thin-cross collision shape. */
public class FluidPipeBlock extends MaterialPipeBlock
        implements EntityBlock,
                SimpleWaterloggedBlock,
                ToolInteractionTarget,
                PipeGeometry.PipeConnectorGeometry,
                gregtech.engine.inventory.PipeFormLike {
    public static final BooleanProperty UP = gregtech.engine.transport.PipeConnections.UP;
    public static final BooleanProperty DOWN = gregtech.engine.transport.PipeConnections.DOWN;
    public static final BooleanProperty NORTH = gregtech.engine.transport.PipeConnections.NORTH;
    public static final BooleanProperty SOUTH = gregtech.engine.transport.PipeConnections.SOUTH;
    public static final BooleanProperty WEST = gregtech.engine.transport.PipeConnections.WEST;
    public static final BooleanProperty EAST = gregtech.engine.transport.PipeConnections.EAST;

    private static final BooleanProperty[] CONNECTIONS = {DOWN, UP, NORTH, SOUTH, WEST, EAST};

    private final PipeSpec spec;
    private @Nullable Direction placementFace;

    @Override
    protected InteractionResult useBlock(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        if (ToolInteractions.use(state, level, pos, player, hand, hit)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FluidPipeBlockEntity pipe) {
            InteractionResult result = pipe.usePipeCover(player, hand, hit);
            if (result != InteractionResult.PASS) return result;
        }

        // Magnifying glass: show pipe network info in chat
        if (!player.isShiftKeyDown() && GTToolHelper.matchesTool(held, GTToolType.MAGNIFYING_GLASS)) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            BlockEntity glassTarget = level.getBlockEntity(pos);
            if (glassTarget instanceof FluidPipeBlockEntity pipe) {
                pipe.printNetworkInfo(player);
                return InteractionResult.CONSUME;
            }
        }

        // Fluid container interaction
        if (!player.isShiftKeyDown()) {
            BlockEntity fluidTarget = level.getBlockEntity(pos);
            if (fluidTarget instanceof FluidPipeBlockEntity pipe) {
                InteractionResult result = pipe.handleUse(player, hand, hit.getDirection());
                if (result != InteractionResult.PASS) return result;
            }
        }

        // Shift-empty-hand: debug info
        if (player.isShiftKeyDown() && held.isEmpty()) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            BlockEntity debugTarget = level.getBlockEntity(pos);
            if (debugTarget instanceof FluidPipeBlockEntity pipe) {
                pipe.printDebug(player);
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    public FluidPipeBlock(PipeSpec spec, Properties properties) {
        super(spec.material(), properties);
        this.spec = spec;
        BlockState defaultState = stateDefinition.any();
        for (BooleanProperty prop : CONNECTIONS) {
            defaultState = defaultState.setValue(prop, false);
        }
        registerDefaultState(defaultState.setValue(GTWaterloggable.WATERLOGGED, false));
    }

    public PipeSpec spec() {
        return spec;
    }

    @Override
    public double pipeHalfThickness() {
        return Math.max(1.0, spec().diameter() * 8.0);
    }

    @Override
    public String pipeSizeName() {
        return spec.size().name();
    }

    @Override
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isMachineWrench(tool)
                ? ToolInteractionSpec.connections(ToolInteractionSpec.ConnectionKind.FLUID)
                : null;
    }

    /**
     * The clicked face is the one the pipe links on placement. Only the server consumes it
     * ({@link #setPlacedBy}), so only the server writes it: in single player the client's
     * placement prediction shares this block instance, and a client write would otherwise decide
     * the face the server connects.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (!ctx.getLevel().isClientSide) placementFace = ctx.getClickedFace();
        return GTWaterloggable.getStateForPlacement(defaultBlockState(), ctx);
    }

    @Nullable
    Direction consumePlacementFace() {
        Direction f = placementFace;
        placementFace = null;
        return f;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTIONS);
        builder.add(GTWaterloggable.WATERLOGGED);
    }

    private static final Map<Direction, BooleanProperty> DIR_TO_PROP = new EnumMap<>(Direction.class);

    static {
        DIR_TO_PROP.put(Direction.UP, UP);
        DIR_TO_PROP.put(Direction.DOWN, DOWN);
        DIR_TO_PROP.put(Direction.NORTH, NORTH);
        DIR_TO_PROP.put(Direction.SOUTH, SOUTH);
        DIR_TO_PROP.put(Direction.WEST, WEST);
        DIR_TO_PROP.put(Direction.EAST, EAST);
    }

    public static BooleanProperty propFor(Direction dir) {
        return DIR_TO_PROP.get(dir);
    }

    /** Lighting may run off-thread: never ask the dynamic arm shape to load neighbours.
     * Only a full-diameter core fills the cube; smaller arms cannot fill its corners. */
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return spec.diameter() < 1.0 && state.getFluidState().isEmpty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext ctx
                && ctx.getEntity() instanceof Player player
                && (GTToolHelper.isMachineWrench(player.getMainHandItem())
                        || GTToolHelper.isMachineWrench(player.getOffhandItem()))) {
            return Shapes.block();
        }
        return shapeFor(state, level, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state, level, pos);
    }

    /**
     * GT6 {@code MultiTileEntityPipeFluid}: {@code onEntityCollidedWithBlock} ->
     * {@code UT.Entities.applyTemperatureDamage(entity, mTemperature, 1, 5.0F)}. The pipe's own
     * temperature tracks the fluid inside it (and drifts back to the environment), so touching a pipe
     * full of molten metal burns and one full of liquid nitrogen freezes - the port already had the
     * temperature, it just never hurt anyone with it.
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || entity.isSpectator()) return;
        if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            GTEntityHelper.applyContactTemperatureDamage(entity, pipe.getTemperature());
        }
    }

    /** Core plus this cell's share of each connection, including incoming thinner pipes. */
    private VoxelShape shapeFor(BlockState state, BlockGetter level, BlockPos pos) {
        double half = Math.max(1.0, spec.diameter() * 8.0);
        double min = (8.0 - half) / 16.0;
        double max = (8.0 + half) / 16.0;
        VoxelShape shape = Shapes.box(min, min, min, max, max, max);
        for (Direction dir : Direction.values()) {
            if (!state.getValue(propFor(dir))) continue;
            double n =
                    gregtech.engine.machine.PipeGeometry.fluidNeighborHalf(level.getBlockState(pos.relative(dir)), dir);
            double[] box = gregtech.engine.machine.PipeGeometry.localArmBox(dir, half, n);
            if (box != null) shape = Shapes.or(shape, gregtech.engine.machine.PipeGeometry.boxShape(box));
        }
        return shape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != GTBlockEntities.FLUID_PIPE.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<FluidPipeBlockEntity>) FluidPipeTickPhases::enqueue;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (level.isClientSide || oldState.is(state.getBlock())) return;
        // onPlace runs before the block entity is registered, so it cannot read this pipe's own
        // state here. The placed-item path finishes in setPlacedBy; other paths reconnect on the
        // first server tick.
        if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            pipe.autoConnectOnPlace(null);
        }
    }

    /** The block entity exists by now: connect the clicked face and everything already adjacent. */
    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            net.minecraft.world.entity.LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            pipe.autoConnectOnPlace(consumePlacementFace());
        }
    }

    /** Repair a connection toward a neighbour that changed after this pipe was placed. */
    @Override
    public void neighborChanged(
            BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, isMoving);
        if (level.isClientSide) return;
        Direction side = Direction.fromDelta(
                neighborPos.getX() - pos.getX(), neighborPos.getY() - pos.getY(), neighborPos.getZ() - pos.getZ());
        if (side != null && level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            pipe.tryConnectTo(side);
            FluidPipeTickPhases.wakeAround(pipe);
            FluidPipeNetworks.invalidate(pipe);
        }
    }

    /** A wrench toggled one face: remember a manual disconnect so auto-connect leaves it alone. */
    @Override
    public void connectionToggled(Level level, BlockPos pos, Direction side, boolean connected) {
        if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            pipe.setFaceManuallyDisconnected(side, !connected);
        }
    }

    /**
     * The pipe's removal path, and therefore where the covers are dropped: the same
     * {@code !state.is(newState.getBlock())} guard the fluid dump already uses, with
     * {@link FluidPipeBlockEntity#dropCovers} next to it — the pipe's copy of
     * {@code BasicMachineBlock} calling {@code dropContents}. The block entity is still in
     * the world at this point, which is what makes the drop work.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FluidPipeBlockEntity pipe) {
                pipe.dumpFluidsToAdjacent();
                pipe.dropCovers();
                FluidPipeTickPhases.wakeAround(pipe);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return spec.flammability();
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return spec.flammability();
    }

    public static Properties defaultProperties(PipeSpec spec) {
        return Properties.of().strength(1.5F, 6.0F).noOcclusion().dynamicShape();
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }

    /** Client: refresh dynamic-model neighbor sizes when an adjacent block changes. */
    @Override
    public net.minecraft.world.level.block.state.BlockState updateShape(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.core.Direction dir,
            net.minecraft.world.level.block.state.BlockState neighborState,
            net.minecraft.world.level.LevelAccessor level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.core.BlockPos neighborPos) {
        GTWaterloggable.updateShape(state, level, pos);
        if (level.isClientSide()) {
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            if (be != null) be.requestModelDataUpdate();
        }
        return state;
    }
}
