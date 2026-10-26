package gregtech.content.block.machine;

//? if >=1.21.1 {
//?} else {
import gregtech.engine.tool.ToolInteractions;
import gregtech.engine.tool.ToolInteractionSpec;
import gregtech.engine.tool.ToolInteractionTarget;
//?}
import gregtech.engine.GTWaterloggable;
import gregtech.engine.machine.PipeSpec;
import gregtech.api.material.MaterialTextureSet;
//? if >=1.21.1 {
//?} else {
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
//?}
import gregtech.content.blockentity.machine.FluidPipeBlockEntity;
import gregtech.content.cover.CoverItems;
import gregtech.content.cover.PanelCoverInteraction;
//? if >=1.21.1 {
/*import gregtech.content.transport.fluid.FluidTransportRegistries;
import gregtech.platform.neoforge.NeoToolBindings;
import gregtech.platform.neoforge.transport.FluidPipeToolInteractions;
*///?} else {
import gregtech.content.registry.GTBlockEntities;
//?}
import gregtech.util.GTEntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
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
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/** GT6 fluid pipe block with wrench-toggleable connections and thin-cross collision shape. */
//? if >=1.21.1 {
/*public class FluidPipeBlock extends Block implements EntityBlock, SimpleWaterloggedBlock, gregtech.engine.tool.ToolInteractionTarget, gregtech.engine.inventory.PipeFormLike {
    public static final BooleanProperty UP    = gregtech.engine.transport.PipeConnections.UP;
    public static final BooleanProperty DOWN  = gregtech.engine.transport.PipeConnections.DOWN;
    public static final BooleanProperty NORTH = gregtech.engine.transport.PipeConnections.NORTH;
    public static final BooleanProperty SOUTH = gregtech.engine.transport.PipeConnections.SOUTH;
    public static final BooleanProperty WEST  = gregtech.engine.transport.PipeConnections.WEST;
    public static final BooleanProperty EAST  = gregtech.engine.transport.PipeConnections.EAST;
*///?} else {
public class FluidPipeBlock extends Block implements EntityBlock, SimpleWaterloggedBlock, ToolInteractionTarget, gregtech.engine.inventory.PipeFormLike {
    public static final BooleanProperty UP    = BooleanProperty.create("up");
    public static final BooleanProperty DOWN  = BooleanProperty.create("down");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST  = BooleanProperty.create("west");
    public static final BooleanProperty EAST  = BooleanProperty.create("east");
//?}

    public static final BooleanProperty[] CONNECTIONS = {DOWN, UP, NORTH, SOUTH, WEST, EAST};

    private final PipeSpec spec;
    private @Nullable Direction placementFace;

    public FluidPipeBlock(PipeSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        BlockState defaultState = stateDefinition.any();
        for (BooleanProperty prop : CONNECTIONS) {
            defaultState = defaultState.setValue(prop, false);
        }
        registerDefaultState(defaultState.setValue(GTWaterloggable.WATERLOGGED, false));
    }

    public PipeSpec spec() { return spec; }
//? if >=1.21.1 {
//?} else {
    @Override public String pipeSizeName() { return spec.size().name(); }
//?}

    @Override
//? if >=1.21.1 {
    /*public gregtech.engine.tool.ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return gregtech.engine.tool.GTToolHelper.isMachineWrench(tool)
            ? gregtech.engine.tool.ToolInteractionSpec.connections(
                gregtech.engine.tool.ToolInteractionSpec.ConnectionKind.FLUID) : null;
*///?} else {
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isMachineWrench(tool)
                ? ToolInteractionSpec.connections(
                    ToolInteractionSpec.ConnectionKind.FLUID) : null;
//?}
    }
//? if >=1.21.1 {

    /*@Override
    public String pipeSizeName(){return spec.size().name();}
*///?} else {
//?}

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        placementFace = ctx.getClickedFace();
        return GTWaterloggable.getStateForPlacement(defaultBlockState(), ctx);
    }

    @Nullable Direction consumePlacementFace() {
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

    public static BooleanProperty propFor(Direction dir) { return DIR_TO_PROP.get(dir); }

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
//? if >=1.21.1 {
                /*&& (NeoToolBindings.isMachineWrench(player.getMainHandItem())
                    || NeoToolBindings.isMachineWrench(player.getOffhandItem()))) {
*///?} else {
                && (GTToolHelper.isMachineWrench(player.getMainHandItem())
                    || GTToolHelper.isMachineWrench(player.getOffhandItem()))) {
//?}
            return Shapes.block();
        }
        return shapeFor(state, level, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state, level, pos);
    }

    /**
     * GT6 {@code MultiTileEntityPipeFluid:460}: {@code onEntityCollidedWithBlock} ->
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
            double n = gregtech.engine.machine.PipeGeometry.fluidNeighborHalf(
                    level.getBlockState(pos.relative(dir)), dir);
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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
//? if >=1.21.1 {
        /*if (level.isClientSide || type != FluidTransportRegistries.FLUID_PIPE.get()) return null;
*///?} else {
        if (level.isClientSide || type != GTBlockEntities.FLUID_PIPE.get()) return null;
//?}
        return (BlockEntityTicker<T>) (BlockEntityTicker<FluidPipeBlockEntity>)
                gregtech.content.transport.fluid.FluidPipeTickPhases::enqueue;
    }

//? if >=1.21.1 {
    /*/^*
     * GT6's right-click order on a tile that can carry covers, mirrored from
     * {@code BasicMachineBlock.use:94-159} so a pipe and a machine answer a click the same way.
     *
     * <p>{@code FluidPipeToolInteractions.use} stays first, exactly where {@code super.use} (the wrench
     * rotation) is on the machine, and everything the pipe did before this batch stays where it was:
     * the magnifying glass, the fluid-container path through
     * {@link FluidPipeBlockEntity#handleUse} and the shift-empty-hand debug print. Between the two
     * sits the cover block, in {@code BasicMachineBlock}'s own order —
     * {@code PanelCoverInteraction.use} ({@code :106-109}), then a held cover item attached to the
     * clicked face ({@code :111-117}), then the crowbar removing the face's cover
     * ({@code :118-126}), then the filter covers' screwdriver / soft-hammer configuration
     * ({@code :146-154}) and the filter-setting click ({@code :155-157}).</p>
     *
     * <p>A pipe with no cover and no cover item in hand is unaffected: {@code PanelCoverInteraction.use}
     * returns {@code PASS} for every item that is not a panel, a cover item or a tool, and for a tool
     * it returns {@code PASS} as soon as {@code PanelCoverRuntime.configure} answers false, which it
     * does whenever the clicked face has no cover ({@code PanelCoverRuntime:127}).</p>
     ^/
    private InteractionResult useOriginal(BlockState state, Level level, BlockPos pos, Player player,
*///?} else {
    /**
     * GT6's right-click order on a tile that can carry covers, mirrored from
     * {@code BasicMachineBlock.use:94-159} so a pipe and a machine answer a click the same way.
     *
     * <p>{@code ToolInteractions.use} stays first, exactly where {@code super.use} (the wrench
     * rotation) is on the machine, and everything the pipe did before this batch stays where it was:
     * the magnifying glass, the fluid-container path through
     * {@link FluidPipeBlockEntity#handleUse} and the shift-empty-hand debug print. Between the two
     * sits the cover block, in {@code BasicMachineBlock}'s own order —
     * {@code PanelCoverInteraction.use} ({@code :106-109}), then a held cover item attached to the
     * clicked face ({@code :111-117}), then the crowbar removing the face's cover
     * ({@code :118-126}), then the filter covers' screwdriver / soft-hammer configuration
     * ({@code :146-154}) and the filter-setting click ({@code :155-157}).</p>
     *
     * <p>A pipe with no cover and no cover item in hand is unaffected: {@code PanelCoverInteraction.use}
     * returns {@code PASS} for every item that is not a panel, a cover item or a tool, and for a tool
     * it returns {@code PASS} as soon as {@code PanelCoverRuntime.configure} answers false, which it
     * does whenever the clicked face has no cover ({@code PanelCoverRuntime:127}).</p>
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
//?}
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

//? if >=1.21.1 {
        /*if (FluidPipeToolInteractions.use(state, level, pos, player, hand, hit)) {
*///?} else {
        if (ToolInteractions.use(state, level, pos, player, hand, hit)) {
//?}
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Covers on the clicked face — the machine's order, see the method javadoc.
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FluidPipeBlockEntity coverHost) {
            InteractionResult panelResult =
                    PanelCoverInteraction.use(coverHost, player, hand, hit, false);
            if (panelResult != InteractionResult.PASS) return panelResult;

            if (CoverItems.isCover(held)) {
                if (!level.isClientSide && coverHost.attachCover(hit.getDirection(), held)) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
//? if >=1.21.1 {
            /*if (NeoToolBindings.matches(held,"crowbar")) {
*///?} else {
            if (GTToolHelper.matchesTool(held, GTToolType.CROWBAR)) {
//?}
                if (!level.isClientSide) {
                    ItemStack removed = coverHost.removeCover(hit.getDirection());
                    if (!removed.isEmpty() && !player.addItem(removed)) player.drop(removed, false);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!held.isEmpty()) {
//? if >=1.21.1 {
                /*if (NeoToolBindings.matches(held,"screwdriver")
*///?} else {
                if (GTToolHelper.matchesTool(held, GTToolType.SCREWDRIVER)
//?}
                        && coverHost.configureFilterCover(hit.getDirection(), true, false)) {
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
//? if >=1.21.1 {
                /*if (NeoToolBindings.matches(held,"soft_hammer")
*///?} else {
                if (GTToolHelper.matchesTool(held, GTToolType.SOFT_HAMMER)
//?}
                        && coverHost.configureFilterCover(hit.getDirection(), false, true)) {
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                if (!CoverItems.isCover(held)
                        && coverHost.clickFilterCover(hit.getDirection(), held)) {
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }

        // Magnifying glass: show pipe network info in chat
//? if >=1.21.1 {
        /*if (!player.isShiftKeyDown() && NeoToolBindings.matches(held,"magnifying_glass")) {
*///?} else {
        if (!player.isShiftKeyDown() && GTToolHelper.matchesTool(held, GTToolType.MAGNIFYING_GLASS)) {
//?}
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            pipe.autoConnectOnPlace(consumePlacementFace());
        }
    }

    /** Repair a connection toward a neighbour that changed after this pipe was placed. */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, isMoving);
        if (level.isClientSide) return;
        Direction side = Direction.fromDelta(neighborPos.getX() - pos.getX(),
                neighborPos.getY() - pos.getY(), neighborPos.getZ() - pos.getZ());
        if (side != null && level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            pipe.tryConnectTo(side);
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
     * {@code BasicMachineBlock:254-262} calling {@code dropContents}. The block entity is still in
     * the world at this point, which is what makes the drop work.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FluidPipeBlockEntity pipe) {
                pipe.dumpFluidsToAdjacent();
                pipe.dropCovers();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
//? if >=1.21.1 {
//?} else {
        if (player.getMainHandItem().getItem() instanceof gregtech.content.item.ElectricToolItem)
            return super.getDestroyProgress(state, player, level, pos);
//?}
        ItemStack tool = player.getMainHandItem();
        MaterialTextureSet texSet = spec.material().getTextureSet();
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) return 0;

        if (texSet == MaterialTextureSet.RUBBER) {
//? if >=1.21.1 {
            /*if (tool.getItem() instanceof ShearsItem || NeoToolBindings.matches(tool,"scissors")) {
                float speed = NeoToolBindings.matches(tool,"scissors") && !tool.isEmpty()
                        ? tool.getDestroySpeed(state)
*///?} else {
            if (tool.getItem() instanceof ShearsItem || GTToolHelper.matchesTool(tool, GTToolType.SCISSORS)) {
                float speed = GTToolHelper.matchesTool(tool, GTToolType.SCISSORS) && GTToolHelper.isUsable(tool)
                        ? GTToolHelper.getMiningSpeed(tool, state)
//?}
                        : tool.getItem() instanceof ShearsItem ? tool.getDestroySpeed(state) : 1.0F;
                return speed / hardness / 30.0F;
            }
        } else if (texSet == MaterialTextureSet.WOOD) {
//? if >=1.21.1 {
            /*if (tool.getItem() instanceof AxeItem || NeoToolBindings.matches(tool,"axe")) {
                float speed = NeoToolBindings.matches(tool,"axe") && !tool.isEmpty()
                        ? tool.getDestroySpeed(state)
*///?} else {
            if (tool.getItem() instanceof AxeItem || GTToolHelper.matchesTool(tool, GTToolType.AXE)) {
                float speed = GTToolHelper.matchesTool(tool, GTToolType.AXE) && GTToolHelper.isUsable(tool)
                        ? GTToolHelper.getMiningSpeed(tool, state)
//?}
                        : tool.getItem() instanceof AxeItem ? tool.getDestroySpeed(state) : 1.0F;
                return speed / hardness / 30.0F;
            }
        } else {
//? if >=1.21.1 {
            /*if (NeoToolBindings.isMachineWrench(tool)) {
*///?} else {
            if (GTToolHelper.isMachineWrench(tool)) {
//?}
                return 1.0F / hardness / 10.0F;
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
//? if >=1.21.1 {
//?} else {
        if (player.getMainHandItem().getItem() instanceof gregtech.content.item.ElectricToolItem electric)
            return gregtech.content.tool.ElectricWrenchHarvest.canHarvest(electric, player.getMainHandItem(), state);
//?}
        ItemStack tool = player.getMainHandItem();
        MaterialTextureSet texSet = spec.material().getTextureSet();
        if (texSet == MaterialTextureSet.RUBBER) {
//? if >=1.21.1 {
            /*if (tool.getItem() instanceof ShearsItem || NeoToolBindings.matches(tool,"scissors")) return true;
*///?} else {
            if (tool.getItem() instanceof ShearsItem || GTToolHelper.matchesTool(tool, GTToolType.SCISSORS)) return true;
//?}
        } else if (texSet == MaterialTextureSet.WOOD) {
//? if >=1.21.1 {
            /*if (tool.getItem() instanceof AxeItem || NeoToolBindings.matches(tool,"axe")) return true;
*///?} else {
            if (tool.getItem() instanceof AxeItem || GTToolHelper.matchesTool(tool, GTToolType.AXE)) return true;
//?}
        } else {
//? if >=1.21.1 {
            /*if (NeoToolBindings.isMachineWrench(tool)) return true;
*///?} else {
            if (GTToolHelper.isMachineWrench(tool)) return true;
//?}
        }
        return super.canHarvestBlock(state, level, pos, player);
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
        return Properties.of()
                .strength(1.5F, 6.0F)
                .noOcclusion()
                .dynamicShape();
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

//? if >=1.21.1 {

    /*@Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        var result=useOriginal(state,level,pos,player,hand,hit);
        return result==InteractionResult.PASS?net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION:
                result==InteractionResult.FAIL?net.minecraft.world.ItemInteractionResult.FAIL:net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return useOriginal(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
*///?} else {
//?}
}
