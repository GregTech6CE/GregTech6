package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.ToolInteractions;
import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractionTarget;



import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.api.energy.AxleSpec;

import com.gregtech.gregtech.blockentity.energy.AxleBlockEntity;
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
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** GT6 rotational axle block — RU conductor with pipe-style 6-way connections. */
public class AxleBlock extends Block implements EntityBlock, SimpleWaterloggedBlock, com.gregtech.gregtech.api.machine.PipeGeometry.AxleConnectorGeometry, ToolInteractionTarget {
    public static final BooleanProperty UP    = BooleanProperty.create("up");
    public static final BooleanProperty DOWN  = BooleanProperty.create("down");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST  = BooleanProperty.create("west");
    public static final BooleanProperty EAST  = BooleanProperty.create("east");
    public static final BooleanProperty[] CONNECTIONS = {DOWN, UP, NORTH, SOUTH, WEST, EAST};

    private static final Map<Direction, BooleanProperty> DIR_TO_PROP = new EnumMap<>(Direction.class);
    static {
        DIR_TO_PROP.put(Direction.DOWN, DOWN);
        DIR_TO_PROP.put(Direction.UP, UP);
        DIR_TO_PROP.put(Direction.NORTH, NORTH);
        DIR_TO_PROP.put(Direction.SOUTH, SOUTH);
        DIR_TO_PROP.put(Direction.WEST, WEST);
        DIR_TO_PROP.put(Direction.EAST, EAST);
    }

    private final AxleSpec spec;
    private @Nullable Direction placementFace;

    @Override public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isMachineWrench(tool)
                ? ToolInteractionSpec.connections(ToolInteractionSpec.ConnectionKind.AXLE) : null;
    }

    public AxleBlock(AxleSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        BlockState def = defaultBlockState();
        for (BooleanProperty prop : CONNECTIONS) def = def.setValue(prop, false);
        registerDefaultState(def.setValue(GTWaterloggable.WATERLOGGED, false));
    }

    public AxleSpec spec() { return spec; }
    public static BooleanProperty propFor(Direction dir) { return DIR_TO_PROP.get(dir); }

    @Nullable Direction consumePlacementFace() {
        Direction f = placementFace;
        placementFace = null;
        return f;
    }

    /** Axles may only connect in a straight line: max 2 connections on the same axis. */
    public static boolean isValidConnectionState(BlockState state) {
        int mask=0;for(var direction:Direction.values())if(state.getValue(propFor(direction)))mask|=1<<direction.ordinal();
        return com.gregtech.gregtech.content.energy.AxleConnectionRules.valid(mask);
    }

    private static Direction propToDir(BooleanProperty prop) {
        if (prop == DOWN) return Direction.DOWN;
        if (prop == UP) return Direction.UP;
        if (prop == NORTH) return Direction.NORTH;
        if (prop == SOUTH) return Direction.SOUTH;
        if (prop == WEST) return Direction.WEST;
        return Direction.EAST;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTIONS);
        builder.add(GTWaterloggable.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        placementFace = context.getClickedFace();
        return GTWaterloggable.getStateForPlacement(defaultBlockState(), context);
    }

    /** Lighting must not load neighbouring chunks through the dynamic connection shape. */
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return spec.halfThickness() < 8.0 && state.getFluidState().isEmpty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (ctx instanceof EntityCollisionContext entityCtx
                && entityCtx.getEntity() instanceof Player player
                && (com.gregtech.gregtech.platform.neoforge.NeoToolBindings.isMachineWrench(player.getMainHandItem())
                    || com.gregtech.gregtech.platform.neoforge.NeoToolBindings.isMachineWrench(player.getOffhandItem()))) {
            return Shapes.block();
        }
        return shapeFor(state, level, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state, level, pos);
    }

    private VoxelShape shapeFor(BlockState state, BlockGetter level, BlockPos pos) {
        double half = spec.halfThickness();
        double min = (8.0 - half) / 16.0;
        double max = (8.0 + half) / 16.0;
        VoxelShape shape = Shapes.box(min, min, min, max, max, max);
        for (Direction dir : Direction.values()) {
            if (!state.getValue(propFor(dir))) continue;
            double n = com.gregtech.gregtech.api.machine.PipeGeometry.axleHalfOf(
                    level.getBlockState(pos.relative(dir)));
            double[] box = com.gregtech.gregtech.api.machine.PipeGeometry.armBox(dir, half, n);
            if (box != null) shape = Shapes.or(shape, com.gregtech.gregtech.api.machine.PipeGeometry.boxShape(box));
        }
        return shape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AxleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if(level.isClientSide || type!=com.gregtech.gregtech.registry.GTAxles.AXLE.get())return null;
        return (world,pos,blockState,entity)->((AxleBlockEntity)entity).serverTick();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (level.isClientSide || oldState.is(state.getBlock())) return;
        Direction face = consumePlacementFace();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof AxleBlockEntity axle) axle.autoConnectOnPlace(face);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AxleBlockEntity axle) axle.onRemoved();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        com.gregtech.gregtech.client.MechanicalBlockTooltips.appendAxle(spec, getExplosionResistance(), tooltip);
    }

    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        if (com.gregtech.gregtech.platform.neoforge.energy.AxleToolInteractions.use(state, level, pos, player, hand, hit))
            return InteractionResult.sidedSuccess(level.isClientSide);

        return InteractionResult.PASS;
    }

    @Override public double axleHalfThickness(){return spec.halfThickness();}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return interact(state,level,pos,player,hand,hit).consumesAction()?net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide):net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
    @Override public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction face) {
        return spec.material()==com.gregtech.gregtech.content.material.generated.WoodMaterials.WoodTreated?150:0;
    }
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction face) {
        return getFlammability(state,level,pos,face);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        GTWaterloggable.updateShape(state, level, pos);
        if (level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) be.requestModelDataUpdate();
        }
        return state;
    }
}
