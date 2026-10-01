package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.api.tool.ToolInteractions;
import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractionTarget;
import com.gregtech.gregtech.api.energy.WireSpec;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.blockentity.energy.ElectricWireBlockEntity;
import com.gregtech.gregtech.util.GTEntityHelper;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import com.gregtech.gregtech.client.WireTooltips;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import com.gregtech.gregtech.api.GTWaterloggable;
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
import java.util.Map;

/** GT6 electric wire block with pipe-style 6-way BooleanProperty connections and dynamic thickness. */
public class ElectricWireBlock extends Block implements EntityBlock, SimpleWaterloggedBlock, ToolInteractionTarget, com.gregtech.gregtech.api.energy.WireMaterialLike {
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

    private final WireSpec spec;
    private @Nullable Direction placementFace;

    public ElectricWireBlock(WireSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        BlockState def = defaultBlockState();
        for (BooleanProperty prop : CONNECTIONS) def = def.setValue(prop, false);
        registerDefaultState(def.setValue(GTWaterloggable.WATERLOGGED, false));
    }

    public WireSpec spec() { return spec; }
    public static BooleanProperty propFor(Direction dir) { return DIR_TO_PROP.get(dir); }

    @Nullable Direction consumePlacementFace() {
        Direction f = placementFace;
        placementFace = null;
        return f;
    }

    @Override
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isWireCutter(tool)
                ? ToolInteractionSpec.connections(
                    ToolInteractionSpec.ConnectionKind.ELECTRIC) : null;
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

    /** Dynamic shape based on wire thickness, matching cross-boundary arm rendering. */
    /** Lighting must not load neighbouring chunks through the dynamic connection shape. */
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return spec.halfThickness() < 8.0 && state.getFluidState().isEmpty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (ctx instanceof EntityCollisionContext entityCtx
                && entityCtx.getEntity() instanceof Player player
                && (GTToolHelper.isWireCutter(player.getMainHandItem())
                    || GTToolHelper.isWireCutter(player.getOffhandItem()))) {
            return Shapes.block();
        }
        return shapeFor(state, level, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state, level, pos);
    }

    /** Core + GT6 connection arms (thin side extends into a strictly thicker neighbor),
     *  matching the dynamic model geometry exactly so the selection box fits the visual. */
    private VoxelShape shapeFor(BlockState state, BlockGetter level, BlockPos pos) {
        double half = spec.halfThickness();
        double min = (8.0 - half) / 16.0;
        double max = (8.0 + half) / 16.0;
        VoxelShape shape = Shapes.box(min, min, min, max, max, max);
        for (Direction dir : Direction.values()) {
            if (!state.getValue(propFor(dir))) continue;
            double n = com.gregtech.gregtech.api.machine.PipeGeometry.wireHalfOf(
                    level.getBlockState(pos.relative(dir)));
            double[] box = com.gregtech.gregtech.api.machine.PipeGeometry.armBox(dir, half, n);
            if (box != null) shape = Shapes.or(shape, com.gregtech.gregtech.api.machine.PipeGeometry.boxShape(box));
        }
        return shape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricWireBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide || type != com.gregtech.gregtech.registry.GTBlockEntities.ELECTRIC_WIRE.get()) return null;
        return (world, pos, blockState, entity) -> ((ElectricWireBlockEntity) entity).serverTick();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!spec().contactDamage()) return;
        if (level.isClientSide || !(entity instanceof LivingEntity living)) return;
        if (living.invulnerableTime > 0) return;
        if (!(level.getBlockEntity(pos) instanceof ElectricWireBlockEntity wire) || !wire.isConducting()) return;
        // GT6 MultiTileEntityWireElectric:203 — a bare wire that carried current hurts whatever touches
        // it with UT.Entities.applyElectricityDamage(entity, mWattageLast), i.e. tierMax(wattage) * 4
        // with the lightning hazard suit, creative mode and the mob immunities exempt. The port used to
        // invent a 2/4/8/20 damage ladder off the wire's rated voltage and a vanilla generic source.
        GTEntityHelper.applyElectricityDamage(living, wire.lastWattage());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (level.isClientSide || oldState.is(state.getBlock())) return;
        Direction face = consumePlacementFace();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ElectricWireBlockEntity wire) wire.autoConnectOnPlace(face);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ElectricWireBlockEntity wire) wire.onRemoved();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        WireTooltips.append(spec, tooltip);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        if (ToolInteractions.use(state, level, pos, player, hand, hit)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
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
