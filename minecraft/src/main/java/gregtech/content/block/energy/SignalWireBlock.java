package gregtech.content.block.energy;

//? if >=1.21.1 {
/*import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.ToolInteractions;
import gregtech.engine.tool.ToolInteractionSpec;
import gregtech.engine.tool.ToolInteractionTarget;
*///?} else {
//?}

import gregtech.api.material.GTMaterial;

//? if >=1.21.1 {
/*import gregtech.platform.neoforge.NeoToolBindings;
*///?} else {
import gregtech.engine.tool.*;
//?}

import gregtech.content.blockentity.energy.SignalWireBlockEntity;

//? if >=1.21.1 {
/*import gregtech.content.registry.GTSignalWires;
*///?} else {
import gregtech.content.registry.GTBlockEntities;
//?}

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import java.util.List;

/** Six independent ports; the common cutter spec also drives the client face overlay. */
//? if >=1.21.1 {
/*public final class SignalWireBlock extends Block implements EntityBlock, ToolInteractionTarget,
        gregtech.engine.tool.ScrewdriverUseTarget {
*///?} else {
public final class SignalWireBlock extends Block implements EntityBlock, ToolInteractionTarget {
//?}
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);
    private final GTMaterial material;
    private final int range;
    private final boolean insulated;
//? if >=1.21.1 {
    /*

    @Override
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isWireCutter(tool) ? ToolInteractionSpec.connections(ToolInteractionSpec.ConnectionKind.REDSTONE) : null;
    }
*///?} else {
//?}

    public SignalWireBlock(GTMaterial material, int range, boolean insulated, boolean luminous, Properties properties) {
        super(properties.dynamicShape().lightLevel(state -> luminous ? state.getValue(POWER) : 0));
        this.material = material;
        this.range = range;
        this.insulated = insulated;
        var state = defaultBlockState().setValue(POWER, 0);
        for (var property : ElectricWireBlock.CONNECTIONS) state = state.setValue(property, false);
        registerDefaultState(state);
    }

    public GTMaterial material() {
        return material;
    }

    public int range() {
        return range;
    }

    public boolean insulated() {
        return insulated;
    }

    public int halfThickness() {
        return insulated ? 2 : 1;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(ElectricWireBlock.propFor(context.getClickedFace().getOpposite()), true);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ElectricWireBlock.CONNECTIONS).add(POWER);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SignalWireBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {

//? if >=1.21.1 {
        /*return !level.isClientSide && type == GTSignalWires.SIGNAL_WIRE.get() ? (tickLevel, tickPos, tickState,
                wireEntity) -> ((SignalWireBlockEntity) wireEntity).tickNetwork() : null;
*///?} else {
        return !level.isClientSide && type == GTBlockEntities.SIGNAL_WIRE.get() ? (tickLevel, tickPos, tickState,
                wireEntity) -> ((SignalWireBlockEntity) wireEntity).tickNetwork() : null;
//?}

    }

//? if >=1.21.1 {
    /*private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (gregtech.platform.neoforge.energy.WireToolInteractions.use(state, level, pos, player, hand,
                hit)) return InteractionResult.sidedSuccess(level.isClientSide);
        if (NeoToolBindings.isScrewdriver(player.getItemInHand(hand))) {
*///?} else {
    @Override
    public ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return GTToolHelper.isWireCutter(tool) ? ToolInteractionSpec.connections(ToolInteractionSpec.ConnectionKind.REDSTONE) : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (ToolInteractions.use(state, level, pos, player, hand, hit)) return InteractionResult.sidedSuccess(level.isClientSide);
        if (GTToolHelper.isScrewdriver(player.getItemInHand(hand))) {
//?}

            if (!level.isClientSide && player.mayBuild() && level.mayInteract(player,
                    pos) && level.getBlockEntity(pos) instanceof SignalWireBlockEntity wire) {
                wire.setMode((wire.mode() + (player.isShiftKeyDown() ? 15 : 1)) & 15);
                player.displayClientMessage(Component.translatable("message.gt6ce.signal_wire.mode", wire.mode()), true);

//? if >=1.21.1 {
                /*NeoToolBindings.damageForUse(player.getItemInHand(hand), 1, player);
*///?} else {
                GTToolHelper.damageForUse(player.getItemInHand(hand), 1, player);
//?}

            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

//? if >=1.21.1 {
    /*@Override
    public InteractionResult useScrewdriver(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(state, level, pos, player, hand, hit);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        return interact(state, level, pos, player, hand,
                hit).consumesAction() ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
*///?} else {
//?}

    /** Original placement: the clicked support port, then compatible neighbors already open toward us.
     * The initial placement state carries the support bit; validate it after the block entity and
     * any item-restored covers exist. Ordinary neighbor updates never rewrite explicit ports.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof SignalWireBlockEntity)) return;
        var requested = level.getBlockState(pos);
        if (!requested.is(this)) return;
        for (var side : Direction.values()) if (requested.getValue(ElectricWireBlock.propFor(side))) {
            if (!connectPlacedPort(level, pos, side, placer)) {
                var current = level.getBlockState(pos);
                if (current.is(this) && current.getValue(ElectricWireBlock.propFor(side)))
                    level.setBlockAndUpdate(pos, current.setValue(ElectricWireBlock.propFor(side), false));
            }
        }
        for (var side : Direction.values()) {
            var neighbor = pos.relative(side);
            if (!level.hasChunkAt(neighbor)) continue;
            var other = level.getBlockState(neighbor);
            if (other.getBlock() instanceof SignalWireBlock
                    && other.getValue(ElectricWireBlock.propFor(side.getOpposite())))
                connectPlacedPort(level, pos, side, placer);
        }
    }

    private boolean connectPlacedPort(Level level, BlockPos pos, Direction side,
                                      net.minecraft.world.entity.LivingEntity placer) {
        var neighbor = pos.relative(side);
        if (!level.hasChunkAt(neighbor) || !level.getBlockState(pos).is(this)) return false;
        if (placer instanceof Player player && (!player.mayBuild() || !level.mayInteract(player, pos)
                || !level.mayInteract(player, neighbor))) return false;
        if (!gregtech.content.cover.CoverConnections.canConnect(level, pos, side)) return false;
        // Original redstone connectors permit ordinary blocks and nonmatching connector types
        // on the explicitly selected side; only matching signal wires receive reciprocal ports.
        var current = level.getBlockState(pos);
        if (!current.getValue(ElectricWireBlock.propFor(side)))
            level.setBlockAndUpdate(pos, current.setValue(ElectricWireBlock.propFor(side), true));
        var other = level.getBlockState(neighbor);
        if (other.getBlock() instanceof SignalWireBlock
                && !other.getValue(ElectricWireBlock.propFor(side.getOpposite())))
            level.setBlockAndUpdate(neighbor, other.setValue(ElectricWireBlock.propFor(side.getOpposite()), true));
        return true;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        if (side == null) return false;
        var face = side.getOpposite();
        // Attachment placement deliberately closes the wire port. That blocks
        // wire-network intake, not vanilla dust connecting to the cover output.
        if (level.getBlockEntity(pos) instanceof gregtech.content.cover.PanelCoverHost host) {
            var cover = gregtech.content.cover.CoverItems.behavior(host.getCover(face));
            if (gregtech.content.cover.CoverItems.REDSTONE_TORCH.equals(cover)
                    || gregtech.content.cover.CoverItems.REDSTONE_REPEATER.equals(cover)) return true;
        }
        return state.getValue(ElectricWireBlock.propFor(face));
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction query) {
        return level.getBlockEntity(pos) instanceof SignalWireBlockEntity wire ? wire.output(query.getOpposite()) : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction query) {
        return getSignal(state, level, pos, query);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SignalWireBlockEntity wire ? wire.comparator() : 0;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, level, pos, next, moving);
        if (!state.is(next.getBlock())) level.updateNeighborsAt(pos, this);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return List.of(new ItemStack(this));
    }

//? if >=1.21.1 {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> text, TooltipFlag flag) {
*///?} else {
    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> text, TooltipFlag flag) {
//?}

        text.add(Component.translatable("tooltip.gt6ce.signal_wire.range", range));
        text.add(Component.translatable("tooltip.gt6ce.signal_wire.controls"));
    }

    /** Lighting does not need the neighbor-dependent connection shape. */
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

//? if >=1.21.1 {
        /*if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player playerHoldingTool
                && NeoToolBindings.isWireCutter(playerHoldingTool.getMainHandItem())) return Shapes.block();
*///?} else {
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player playerHoldingTool
                && GTToolHelper.isWireCutter(playerHoldingTool.getMainHandItem())) return Shapes.block();
//?}
        return connectionShape(state, level, pos);
    }

    private VoxelShape connectionShape(BlockState state, BlockGetter level, BlockPos pos) {
        int half = halfThickness();
        int minimum = 8 - half, maximum = 8 + half;
        var shape = Block.box(minimum, minimum, minimum, maximum, maximum, maximum);
        for (var side : Direction.values()) {
            if (!state.getValue(ElectricWireBlock.propFor(side))) continue;
            var neighbor = level.getBlockState(pos.relative(side));
            double neighborHalf = neighbor.getBlock() instanceof SignalWireBlock wire ? wire.halfThickness()
                    : gregtech.engine.machine.PipeGeometry.NONE;
            double[] box = gregtech.engine.machine.PipeGeometry.localArmBox(side, half, neighborHalf);
            if (box != null) shape = Shapes.or(shape, gregtech.engine.machine.PipeGeometry.boxShape(box));
        }
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, CollisionContext.empty());
    }
}
