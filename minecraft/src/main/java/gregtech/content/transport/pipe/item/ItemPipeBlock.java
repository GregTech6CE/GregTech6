package gregtech.content.transport.pipe.item;

import gregtech.content.transport.pipe.MaterialPipeBlock;
import gregtech.engine.GTWaterloggable;
import gregtech.engine.machine.ItemPipeSpec;
import gregtech.engine.machine.PipeGeometry;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.ToolInteractionSpec;
import gregtech.engine.tool.ToolInteractionTarget;
import gregtech.engine.tool.ToolInteractions;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
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
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** GT6 item pipe block with wrench-toggleable I/O and thin-cross collision shape. */
public class ItemPipeBlock extends MaterialPipeBlock
        implements EntityBlock,
                SimpleWaterloggedBlock,
                ToolInteractionTarget,
                PipeGeometry.PipeConnectorGeometry,
                gregtech.engine.inventory.PipeFormLike {
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");

    public static final BooleanProperty[] CONNECTIONS = {DOWN, UP, NORTH, SOUTH, WEST, EAST};

    private final ItemPipeSpec spec;
    private @Nullable Direction placementFace;

    @Override
    protected InteractionResult useBlock(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        if (ToolInteractions.use(state, level, pos, player, hand, hit)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ItemPipeBlockEntity pipe) {
            InteractionResult result = pipe.usePipeCover(player, hand, hit);
            if (result != InteractionResult.PASS) return result;
        }

        // Shift-empty-hand: debug info
        if (player.isShiftKeyDown() && held.isEmpty()) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (be instanceof ItemPipeBlockEntity pipeEntity) {
                printDebug(player, pipeEntity);
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    public ItemPipeBlock(ItemPipeSpec spec, Properties properties) {
        super(spec.material(), properties);
        this.spec = spec;
        BlockState defaultState = stateDefinition.any();
        for (BooleanProperty prop : CONNECTIONS) {
            defaultState = defaultState.setValue(prop, false);
        }
        registerDefaultState(defaultState.setValue(GTWaterloggable.WATERLOGGED, false));
    }

    public ItemPipeSpec spec() {
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

    @Override
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isMachineWrench(tool)
                ? ToolInteractionSpec.connections(ToolInteractionSpec.ConnectionKind.ITEM)
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
        // GT6 item pipes block entity movement (mBlocking = true)
        return shapeFor(state, level, pos);
    }

    /** Core plus this cell's share of each connection, including incoming thinner pipes. */
    private VoxelShape shapeFor(BlockState state, BlockGetter level, BlockPos pos) {
        double half = Math.max(1.0, spec.diameter() * 8.0);
        double min = (8.0 - half) / 16.0;
        double max = (8.0 + half) / 16.0;
        VoxelShape shape = Shapes.box(min, min, min, max, max, max);
        for (Direction dir : Direction.values()) {
            if (!state.getValue(propFor(dir))) continue;
            double n = gregtech.engine.machine.PipeGeometry.pipeHalfOf(level.getBlockState(pos.relative(dir)));
            double[] box = gregtech.engine.machine.PipeGeometry.localArmBox(dir, half, n);
            if (box != null) shape = Shapes.or(shape, gregtech.engine.machine.PipeGeometry.boxShape(box));
        }
        return shape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }

    // No ticker: ItemPipeNetworks schedules covered pipes and pipes holding items.

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
        if (level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
            pipe.autoConnectOnPlace(consumePlacementFace());
        }
    }

    private void printDebug(Player player, ItemPipeBlockEntity pipeEntity) {
        int itemCount = 0;
        for (int i = 0; i < pipeEntity.getSlots(); i++) {
            itemCount += pipeEntity.getStackInSlot(i).getCount();
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "Item Pipe " + spec.size() + " (" + spec.materialName() + "): " + itemCount
                        + " items, step=" + spec.stepSize() + ", inv="
                        + spec.invSize() + ", transferred=" + pipeEntity.getTransferredThisSecond()));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (level.isClientSide || oldState.is(state.getBlock())) return;
        // onPlace runs before the block entity is registered, so it cannot read this pipe's own
        // state here. The placed-item path finishes in setPlacedBy; other paths reconnect on the
        // first server tick.
        if (level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
            pipe.autoConnectOnPlace(null);
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
        if (side != null && level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
            pipe.tryConnectTo(side);
        }
    }

    /** A wrench toggled one face: remember a manual disconnect so auto-connect leaves it alone. */
    @Override
    public void connectionToggled(Level level, BlockPos pos, Direction side, boolean connected) {
        if (level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
            pipe.setFaceManuallyDisconnected(side, !connected);
        }
    }

    /**
     * The pipe's removal path, and therefore where the covers are dropped: the same
     * {@code !state.is(newState.getBlock())} guard the item drop already uses, with
     * {@link ItemPipeBlockEntity#dropCovers} next to it — the pipe's copy of
     * {@code BasicMachineBlock} calling {@code dropContents}, and of
     * {@link FluidPipeBlock#onRemove}. The block entity is still in the world at this point, which is
     * what makes the drop work.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ItemPipeBlockEntity pipe) {
                pipe.dropStoredItems();
                pipe.dropCovers();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    public static Properties defaultProperties(ItemPipeSpec spec) {
        Properties p = Properties.of()
                .strength(1.5F, spec.blastResistance())
                .noOcclusion()
                .dynamicShape();
        if (spec.blocking()) {
            p = p.strength(2.0F, spec.blastResistance()); // slightly stronger for entity-blocking pipes
        }
        return p;
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
