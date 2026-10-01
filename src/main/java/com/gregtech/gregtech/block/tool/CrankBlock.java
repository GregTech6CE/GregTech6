package com.gregtech.gregtech.block.tool;

import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.data.GregTechTags;
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
 * GT6's Hand Crank ({@code MultiTileEntityCrank}, multi-tile {@code 32111},
 * {@code Loader_MultiTileEntities:2105}): a small attachment a player cranks by hand. It is a
 * <b>redstone lever that also turns</b> - GT6 never asks for a machine, so a crank bolted to a plain
 * wall just turns and pulses redstone (this is what the user report was about: the port used to answer
 * {@code message.gregtech.crank.no_target} instead).
 *
 * <h2>GT6's behaviour, ported here</h2>
 * <ul>
 *   <li><b>Sides.</b> GT6 stores the mounting side in {@code NBT_FACING} ({@code mFacing}): the crank
 *       drives the block on that side ({@code MultiTileEntityCrank:193} {@code isEnergyEmittingTo} only
 *       answers for {@code aSide == mFacing}, {@code :78} emits into it) and its handle - the lit
 *       "front" texture - points the other way ({@code :144}: the front texture is drawn for
 *       {@code OPOS[mFacing]}, the axle for {@code mFacing}). {@link #FACING} is the <em>handle</em>
 *       side, i.e. GT6's {@code OPOS[mFacing]}: the block is placed with {@code ctx.getClickedFace()},
 *       hugs the block on {@code FACING.getOpposite()} with its shape ({@link #SHAPES}) and drives the
 *       block entity there - exactly vanilla's lever convention. So GT6's {@code mFacing} is this
 *       block's {@code FACING.getOpposite()}.</li>
 *   <li><b>Every click turns the crank.</b> {@code onBlockActivated3} ({@code :103-112}) sets
 *       {@code mActive}, updates the client and causes a block update, unconditionally and without
 *       looking for a machine; {@code onTick2} ({@code :72-96}) repeats that while somebody still
 *       targets the crank. {@link #use} does the same: it always turns, and it only tries to push the
 *       packet into a machine if there happens to be one.</li>
 *   <li><b>Redstone.</b> {@code isProvidingWeakPower2} and {@code isProvidingStrongPower2}
 *       ({@code :115-122}) return {@code 15} for {@code OPOS[mFacing]} - the handle side - while
 *       {@code mActive}, and {@code 0} everywhere else. That is {@link #getSignal} and
 *       {@link #getDirectSignal} here: dust and lamps placed in front of the handle are powered while
 *       the crank turns, no matter what the crank is mounted on. The direction of that query is
 *       {@code FACING.getOpposite()} in 1.20.1 terms, because the engine asks an emitter about the
 *       direction pointing back at the asking block (see {@link #isProvidingPower}).</li>
 *   <li><b>The packet.</b> One turn offers GT6's
 *       {@code -divup(8L * pot2Strength, pot1Weakness)} RU at {@code pot1Haste} packets
 *       ({@code :78}, strength {@code 2 + level}, weakness {@code 1 + level}), which is what
 *       {@link #use} inserts into the machine on {@code FACING.getOpposite()}; the player is only
 *       exhausted when the packet was accepted ({@code :78}), as in GT6.</li>
 * </ul>
 *
 * <h2>Port differences</h2>
 * <ul>
 *   <li><b>No tile entity.</b> GT6 keeps {@code mActive} in its tile entity and clears it on the next
 *       tick once no player targets the crank any more ({@code :74-96}), so the crank is active exactly
 *       as long as it is cranked. GT6 mirrors that flag into the block's own metadata for the client
 *       ({@code getDirectionData}, {@code :201}: {@code (mFacing &amp; 7) | (mActive ? 8 : 0)}), which is
 *       the block state {@link #ACTIVE} here, and a click arms a pulse of {@link #ACTIVE_TICKS} ticks:
 *       the same "the crank turns for a moment, then stops" behaviour without a block entity.</li>
 *   <li><b>One packet per click.</b> GT6 emits one packet per tick for every player that still targets
 *       the crank ({@code :76-80}, plus one for a villager standing in it, {@code :82-91}); the port
 *       keeps its one packet per right click and ignores villagers.</li>
 *   <li><b>No chat messages.</b> GT6 shows neither a "no machine here" nor a "machine is full" message;
 *       the port used to show {@code message.gregtech.crank.no_target} and
 *       {@code message.gregtech.crank.full} instead. Both are gone, so a wrong click says nothing and
 *       the only hint left is the tooltip.</li>
 *   <li><b>Sound.</b> GT6 plays {@code SFX.MC_MINECART} on the click and again at random while active
 *       ({@code :98}, {@code :108}); the port keeps its wooden crank knock.</li>
 *   <li><b>No spin model.</b> GT6 swaps in a {@code frontspin} texture and syncs {@code mActive} to the
 *       clients ({@code :94}, {@code :126}); the port's crank has a single static model, so
 *       {@code active = true} only carries the redstone state.</li>
 * </ul>
 */
public class CrankBlock extends DirectionalBlock {

    /** GT6 {@code mActive} ({@code MultiTileEntityCrank:52}): the crank is being turned right now. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    /**
     * Port model of GT6's {@code mActive}: how long one turn keeps the crank active. GT6 keeps it set
     * for as long as a player keeps targeting the crank ({@code :76-96}); half a second is the port's
     * stand-in for "the player let go".
     */
    public static final int ACTIVE_TICKS = 10;

    /** GT6's redstone value while active ({@code MultiTileEntityCrank:116}). */
    public static final int ACTIVE_SIGNAL = 15;

    private static final VoxelShape[] SHAPES = new VoxelShape[6];
    static {
        SHAPES[Direction.DOWN.ordinal()] = Block.box(2, 13, 2, 14, 16, 14);
        SHAPES[Direction.UP.ordinal()] = Block.box(2, 0, 2, 14, 3, 14);
        SHAPES[Direction.NORTH.ordinal()] = Block.box(2, 2, 13, 14, 14, 16);
        SHAPES[Direction.SOUTH.ordinal()] = Block.box(2, 2, 0, 14, 14, 3);
        SHAPES[Direction.WEST.ordinal()] = Block.box(13, 2, 2, 16, 14, 14);
        SHAPES[Direction.EAST.ordinal()] = Block.box(0, 2, 2, 3, 14, 14);
    }

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
     * GT6's {@code aSide == OPOS[mFacing] && mActive} test ({@code MultiTileEntityCrank:116}, {@code :121}):
     * the signal leaves the crank towards the side the handle points to - the side away from the block
     * the crank is mounted on - which is this block's {@link #FACING}.
     *
     * <p>Side convention: 1.20.1 hands {@code getSignal} the direction <em>from the asking block towards
     * this one</em> - {@code SignalGetter.getBestNeighborSignal} calls
     * {@code getSignal(pos.relative(direction), direction)}, and {@code hasNeighborSignal} and
     * {@code PistonBaseBlock.getNeighborSignal} do the same. A block in front of the handle, i.e. in the
     * world direction {@link #FACING} from the crank, therefore asks with {@code FACING.getOpposite()},
     * which is the value answered here; {@code level.getSignal(crank, FACING)} is the query of a block on
     * the mounting side and stays 0, exactly like GT6's {@code 0} for every side but its one.</p>
     */
    private static boolean isProvidingPower(BlockState state, Direction side) {
        return state.getValue(ACTIVE) && side == state.getValue(FACING).getOpposite();
    }

    /** GT6's {@code causeBlockUpdate} after the state changed, plus the end of the pulse. */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE)) {
            level.setBlock(pos, state.setValue(ACTIVE, false), Block.UPDATE_ALL);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // both hands fire use(); handling the off hand double-triggers
        // (insert+extract in one click, double withdrawals, armor swap-backs)
        if (hand == net.minecraft.world.InteractionHand.OFF_HAND) return InteractionResult.PASS;
        if (!level.isClientSide) {
            Direction handle = state.getValue(FACING);
            Direction mounting = handle.getOpposite();
            long strength = 2L + effectLevel(player, net.minecraft.world.effect.MobEffects.DAMAGE_BOOST);
            long weakness = 1 + effectLevel(player, net.minecraft.world.effect.MobEffects.WEAKNESS);
            long speed = -((8 * strength + weakness - 1) / weakness);

            // GT6 MultiTileEntityCrank:78/193: the packet goes into the block the crank is mounted on,
            // i.e. the machine on the side opposite the handle. There is no machine on most cranks, and
            // GT6 does not care - the crank turns either way.
            long accepted = 0;
            var target = level.getBlockEntity(pos.relative(mounting));
            if (target instanceof IEnergyBlock) {
                accepted = EnergyTransfer.insertEnergyInto(GregTechTags.Energy.RU, handle, speed,
                        1 + effectLevel(player, net.minecraft.world.effect.MobEffects.DIG_SPEED), null, target);
            }
            if (accepted > 0) player.causeFoodExhaustion(0.025F);

            // GT6 MultiTileEntityCrank:103-112 and :93-96: every right click turns the crank, updates
            // the clients and causes a block update - with or without a machine behind it. The neighbour
            // update is vanilla's LeverBlock.updateNeighbours (LeverBlock:107): redstone dust and lamps
            // only recompute their state when an update reaches them, and a second click on an already
            // turning crank has no state change of its own to notify them.
            if (!state.getValue(ACTIVE)) {
                level.setBlock(pos, state.setValue(ACTIVE, true), Block.UPDATE_ALL);
            }
            level.updateNeighborsAt(pos, this);
            level.scheduleTick(pos, this, ACTIVE_TICKS);
            level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 0.7F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static int effectLevel(Player player, net.minecraft.world.effect.MobEffect effect) {
        var active = player.getEffect(effect);
        return active == null ? 0 : active.getAmplifier() + 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.tooltip.crank.1"));
        tooltip.add(Component.translatable("gt.tooltip.crank.2"));
    }
}
