package gregtech.content.block.tool;

import gregtech.engine.energy.EnergyBlockViews;
import gregtech.engine.energy.EnergyTransfer;
import gregtech.content.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Hand crank (GT6 multi-tile 32111). Activation is latched to a player's last interaction,
 * not a fixed pulse or gaze timer: MultiTileEntityCrank:72-112, UT.Entities.
 * Every server tick each eligible operator supplies one potion-adjusted RU offer. A rejected
 * offer does not stop motion/redstone or cause hunger. FACING is the handle side; its opposite
 * is the mount and energy recipient. No stored energy or rotational decay exists here.
 *
 * <p>The original villager-operated idle crank is not implemented by this player-duration fix.</p>
 */
public class CrankBlock extends DirectionalBlock {

    /** Loader_MultiTileEntities 32111 uses ANY.Iron, whose appearance is MT.Fe. */
    public static int tintRgb() {
        return gregtech.content.material.Materials.Iron.getColor();
    }

    /** GT6 {@code mActive} ({@code MultiTileEntityCrank:52}): the crank is being turned right now. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    /** Poll operator eligibility every server tick, rather than end a ten-tick pulse. */
    public static final int ACTIVE_TICKS = 1;

    /** GT6's redstone value while active ({@code MultiTileEntityCrank:116}). */
    public static final int ACTIVE_SIGNAL = gregtech.content.tool.UtilityToolRules.CRANK_SIGNAL;

    private static final VoxelShape[] SHAPES = new VoxelShape[6];
    static {
        SHAPES[Direction.DOWN.ordinal()] = Block.box(2, 13, 2, 14, 16, 14);
        SHAPES[Direction.UP.ordinal()] = Block.box(2, 0, 2, 14, 3, 14);
        SHAPES[Direction.NORTH.ordinal()] = Block.box(2, 2, 13, 14, 14, 16);
        SHAPES[Direction.SOUTH.ordinal()] = Block.box(2, 2, 0, 14, 14, 3);
        SHAPES[Direction.WEST.ordinal()] = Block.box(13, 2, 2, 16, 14, 14);
        SHAPES[Direction.EAST.ordinal()] = Block.box(0, 2, 2, 3, 14, 14);
    }

//? if >=1.21.1 {
    /*@Override
    public com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }

    @Override
    public net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        return switch (interact(state, level, pos, player, hand, hit)) {
            case SUCCESS -> net.minecraft.world.ItemInteractionResult.SUCCESS;
            case CONSUME, CONSUME_PARTIAL -> net.minecraft.world.ItemInteractionResult.CONSUME;
            case FAIL -> net.minecraft.world.ItemInteractionResult.FAIL;
            default -> net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
*///?} else {
//?}
    public CrankBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[state.getValue(FACING).ordinal()];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    /** The crank is an attachment: the block it points away from ({@code FACING.getOpposite()}) has to exist. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return !level.getBlockState(pos.relative(facing.getOpposite())).isAir();
    }

    // ---- GT6 MultiTileEntityCrank:115-122: the crank is a redstone source while it is turned ----

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return isProvidingPower(state, side) ? ACTIVE_SIGNAL : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return isProvidingPower(state, side) ? ACTIVE_SIGNAL : 0;
    }

    /**
     * GT6 receives the opposite/query side already (TileEntityBase06Covers:405-416).
     * OPOS[mFacing] therefore powers the mounting block, rather than the handle's neighbour.
     * This port stores the handle in FACING, the same query value as OPOS[mFacing].
     */

    private static boolean isProvidingPower(BlockState state, Direction side) {
        return state.getValue(ACTIVE) && side == state.getValue(FACING);
    }

    /** GT6's {@code causeBlockUpdate} after the state changed, plus the end of the pulse. */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) return;
        boolean active = false;
        for (Player player : CrankInteractionEvents.operators(level, pos)) {
            active = true;
            offerEnergy(state, level, pos, player);
            player.swing(InteractionHand.MAIN_HAND);
        }
        if (active) {
            level.scheduleTick(pos, this, ACTIVE_TICKS);
        } else {
            level.setBlock(pos, state.setValue(ACTIVE, false), Block.UPDATE_ALL);
            updateMountNeighbors(state, level, pos);
        }
    }

    private static void offerEnergy(BlockState state, ServerLevel level, BlockPos pos, Player player) {
        Direction handle = state.getValue(FACING);
        long size = gregtech.content.tool.UtilityToolRules.crankSize(
                effectLevel(player, net.minecraft.world.effect.MobEffects.DAMAGE_BOOST),
                effectLevel(player, net.minecraft.world.effect.MobEffects.WEAKNESS));
        BlockPos mount = pos.relative(handle.getOpposite());
        if (!level.hasChunkAt(mount)) return;
        var target = level.getBlockEntity(mount);
        long accepted = EnergyBlockViews.view(target) == null ? 0 : EnergyTransfer.insertEnergyInto(
                GregTechTags.Energy.RU, handle, size,
                1 + effectLevel(player, net.minecraft.world.effect.MobEffects.DIG_SPEED), null, target);
        if (accepted > 0) player.causeFoodExhaustion(0.025F *
                (1 + effectLevel(player, net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN)));
    }

    /** Like a vanilla lever, notify both the attachment and the block conducting its strong signal. */
    private void updateMountNeighbors(BlockState state, Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.relative(state.getValue(FACING).getOpposite()), this);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && state.getValue(ACTIVE) && !level.isClientSide) {
            updateMountNeighbors(state, level, pos);
        }
        super.onRemove(state, level, pos, next, moving);
    }
//? if >=1.21.1 {
    /*public InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
*///?} else {

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
//?}
            InteractionHand hand, BlockHitResult hit) {
        // Only the main hand drives the crank; processing the off hand would send a second packet.
        if (hand == net.minecraft.world.InteractionHand.OFF_HAND) return InteractionResult.PASS;
        if (!level.isClientSide) {
            // The event also records other interactions so clicking elsewhere releases the crank.
            // Record here too for direct native block interactions (no loader event dispatch).
            CrankInteractionEvents.target(player, pos);
            // GT6 activation only latches mActive; RU starts on the next server tick.
            if (!state.getValue(ACTIVE)) {
                level.setBlock(pos, state.setValue(ACTIVE, true), Block.UPDATE_ALL);
            }
            updateMountNeighbors(state, level, pos);
            level.scheduleTick(pos, this, ACTIVE_TICKS);
            level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 0.7F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

//? if >=1.21.1 {
    /*private static int effectLevel(Player player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
*///?} else {
    private static int effectLevel(Player player, net.minecraft.world.effect.MobEffect effect) {
//?}
        var active = player.getEffect(effect);
        return active == null ? 0 : active.getAmplifier() + 1;
    }

//? if >=1.21.1 {
    /*@Override
    public java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return java.util.List.of(new ItemStack(this));
    }
*///?} else {
//?}
    @Override
//? if >=1.21.1 {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
*///?} else {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
//?}
        tooltip.add(Component.translatable("gt.tooltip.crank.1"));
        tooltip.add(Component.translatable("gt.tooltip.crank.2"));
    }
}
