package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.api.machine.PipeSpec;
import com.gregtech.gregtech.api.material.MaterialTextureSet;
import com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity;
import com.gregtech.gregtech.content.cover.CoverItems;
import com.gregtech.gregtech.content.cover.PanelCoverInteraction;
import com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries;
import com.gregtech.gregtech.platform.neoforge.NeoToolBindings;
import com.gregtech.gregtech.platform.neoforge.transport.FluidPipeToolInteractions;
import com.gregtech.gregtech.util.GTEntityHelper;
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
public class FluidPipeBlock extends Block implements EntityBlock, SimpleWaterloggedBlock, com.gregtech.gregtech.api.inventory.PipeFormLike {
    public static final BooleanProperty UP    = com.gregtech.gregtech.api.transport.PipeConnections.UP;
    public static final BooleanProperty DOWN  = com.gregtech.gregtech.api.transport.PipeConnections.DOWN;
    public static final BooleanProperty NORTH = com.gregtech.gregtech.api.transport.PipeConnections.NORTH;
    public static final BooleanProperty SOUTH = com.gregtech.gregtech.api.transport.PipeConnections.SOUTH;
    public static final BooleanProperty WEST  = com.gregtech.gregtech.api.transport.PipeConnections.WEST;
    public static final BooleanProperty EAST  = com.gregtech.gregtech.api.transport.PipeConnections.EAST;

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

    @Override
    public String pipeSizeName(){return spec.size().name();}

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
                && (NeoToolBindings.isMachineWrench(player.getMainHandItem())
                    || NeoToolBindings.isMachineWrench(player.getOffhandItem()))) {
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

    /** Core + GT6 connection arms (thin side extends into a strictly thicker neighbor). */
    private VoxelShape shapeFor(BlockState state, BlockGetter level, BlockPos pos) {
        double half = Math.max(1.0, spec.diameter() * 8.0);
        double min = (8.0 - half) / 16.0;
        double max = (8.0 + half) / 16.0;
        VoxelShape shape = Shapes.box(min, min, min, max, max, max);
        for (Direction dir : Direction.values()) {
            if (!state.getValue(propFor(dir))) continue;
            double n = com.gregtech.gregtech.api.machine.PipeGeometry.fluidNeighborHalf(
                    level.getBlockState(pos.relative(dir)), dir);
            double[] box = com.gregtech.gregtech.api.machine.PipeGeometry.armBox(dir, half, n);
            if (box == null) box = com.gregtech.gregtech.api.machine.PipeGeometry.stubBox(dir, half, n);
            if (box != null) shape = Shapes.or(shape, com.gregtech.gregtech.api.machine.PipeGeometry.boxShape(box));
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
        if (level.isClientSide || type != FluidTransportRegistries.FLUID_PIPE.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<FluidPipeBlockEntity>) FluidPipeBlockEntity::serverTick;
    }

    /**
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
     */
    private InteractionResult useOriginal(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        if (FluidPipeToolInteractions.use(state, level, pos, player, hand, hit)) {
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
            if (NeoToolBindings.matches(held,"crowbar")) {
                if (!level.isClientSide) {
                    ItemStack removed = coverHost.removeCover(hit.getDirection());
                    if (!removed.isEmpty() && !player.addItem(removed)) player.drop(removed, false);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!held.isEmpty()) {
                if (NeoToolBindings.matches(held,"screwdriver")
                        && coverHost.configureFilterCover(hit.getDirection(), true, false)) {
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                if (NeoToolBindings.matches(held,"soft_hammer")
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
        if (!player.isShiftKeyDown() && NeoToolBindings.matches(held,"magnifying_glass")) {
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
                InteractionResult result = pipe.handleUse(player, hand);
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
        Direction face = consumePlacementFace();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FluidPipeBlockEntity pipe) {
            pipe.autoConnectOnPlace(face);
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
        ItemStack tool = player.getMainHandItem();
        MaterialTextureSet texSet = spec.material().getTextureSet();
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) return 0;

        if (texSet == MaterialTextureSet.RUBBER) {
            if (tool.getItem() instanceof ShearsItem || NeoToolBindings.matches(tool,"scissors")) {
                float speed = NeoToolBindings.matches(tool,"scissors") && !tool.isEmpty()
                        ? tool.getDestroySpeed(state)
                        : tool.getItem() instanceof ShearsItem ? tool.getDestroySpeed(state) : 1.0F;
                return speed / hardness / 30.0F;
            }
        } else if (texSet == MaterialTextureSet.WOOD) {
            if (tool.getItem() instanceof AxeItem || NeoToolBindings.matches(tool,"axe")) {
                float speed = NeoToolBindings.matches(tool,"axe") && !tool.isEmpty()
                        ? tool.getDestroySpeed(state)
                        : tool.getItem() instanceof AxeItem ? tool.getDestroySpeed(state) : 1.0F;
                return speed / hardness / 30.0F;
            }
        } else {
            if (NeoToolBindings.isMachineWrench(tool)) {
                return 1.0F / hardness / 10.0F;
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        ItemStack tool = player.getMainHandItem();
        MaterialTextureSet texSet = spec.material().getTextureSet();
        if (texSet == MaterialTextureSet.RUBBER) {
            if (tool.getItem() instanceof ShearsItem || NeoToolBindings.matches(tool,"scissors")) return true;
        } else if (texSet == MaterialTextureSet.WOOD) {
            if (tool.getItem() instanceof AxeItem || NeoToolBindings.matches(tool,"axe")) return true;
        } else {
            if (NeoToolBindings.isMachineWrench(tool)) return true;
        }
        return super.canHarvestBlock(state, level, pos, player);
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


    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        var result=useOriginal(state,level,pos,player,hand,hit);
        return result==InteractionResult.PASS?net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION:
                result==InteractionResult.FAIL?net.minecraft.world.ItemInteractionResult.FAIL:net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return useOriginal(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
}
