package com.gregtech.gregtech.block.misc;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** GT6 craftable miniature portal: cross-dimension items, fluids, GT/FE energy and signals.
 * Existing registry ids are retained. Dungeon rooms use vanilla portals separately.
 */
public class DungeonPortalBlock extends Block implements EntityBlock {

    /** GT6's {@code mActive} as a block state, so the model can follow it. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    /** The two dimensions GT6's own mini portals serve (see the subclasses of {@code MultiTileEntityMiniPortal}). */
    public enum Target {
        /** {@code MultiTileEntityMiniPortalNether}: overworld to Nether, distance factor 8, margin 128. */
        NETHER(Level.NETHER, 8, 128),
        /** {@code MultiTileEntityMiniPortalEnd}: overworld to End, distance factor 128, margin 512. */
        END(Level.END, 128, 512);

        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final int distanceFactor, margin;

        Target(net.minecraft.resources.ResourceKey<Level> dimension, int distanceFactor, int margin) {
            this.dimension = dimension;
            this.distanceFactor = distanceFactor;
            this.margin = margin;
        }
    }

    private final Target target;

    public DungeonPortalBlock(Target target, Properties properties) {
        super(properties);
        this.target = target;
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    public Target target() {
        return target;
    }

    /** GT6's distance factor of this portal's dimension pair. */
    public int distanceFactor() {
        return target.distanceFactor;
    }

    /** GT6's margin of error of this portal's dimension pair, in overworld metres. */
    public int margin() {
        return target.margin;
    }

    /**
     * The level this portal is used from leads to: the portal's own dimension from the overworld, the
     * overworld from its own dimension, and nowhere from any other dimension - GT6's
     * {@code findTargetPortal} and {@code addThisPortalToLists} only know the overworld and the other
     * side ({@code MultiTileEntityMiniPortalNether:67-113}).
     */
    @Nullable
    public ServerLevel targetLevel(Level level) {
        if (level.getServer() == null) return null;
        if (level.dimension() == Level.OVERWORLD) return level.getServer().getLevel(target.dimension);
        if (level.dimension() == target.dimension) return level.getServer().getLevel(Level.OVERWORLD);
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonPortalBlockEntity(pos, state);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) { }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        boolean igniter = target == Target.NETHER && held.is(Items.FLINT_AND_STEEL);
        boolean eye = target == Target.END && held.is(Items.ENDER_EYE);
        if (!igniter && !eye) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof DungeonPortalBlockEntity portal)) return InteractionResult.PASS;
        if (igniter) {
            portal.onIgnite(level, pos, hit.getDirection(), player, held, player.isShiftKeyDown(), 0.5F, 0.5F, 0.5F);
            if (!player.isCreative()) held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        } else {
            portal.activate();
            if (!player.isCreative()) held.shrink(1);
        }
        var remote = portal.linkedPortal();
        if (eye && remote != null) player.displayClientMessage(Component.literal("X: " + remote.getBlockPos().getX()
                + "   Y: " + remote.getBlockPos().getY() + "   Z: " + remote.getBlockPos().getZ()), false);
        return InteractionResult.CONSUME;
    }

    /** GT6's portal tooltips ({@code MultiTileEntityMiniPortalNether:49-59}, {@code ...End:49-60}). */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
                                TooltipFlag flag) {
        if (target == Target.NETHER) {
            tooltip.add(Component.translatable("tooltip.gregtech.portal.nether.range")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.gregtech.portal.nether.margin")
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.gregtech.portal.end.range")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.gregtech.portal.end.margin")
                    .withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.gregtech.portal.relay").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(target == Target.NETHER ? "tooltip.gregtech.portal.ignite" : "tooltip.gregtech.portal.eye")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide || type != com.gregtech.gregtech.registry.GTBlockEntities.DUNGEON_PORTAL.get()
                ? null : (world, pos, blockState, entity) -> ((DungeonPortalBlockEntity)entity).tickRelay();
    }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction side) {
        return level.getBlockEntity(pos) instanceof DungeonPortalBlockEntity portal ? portal.signal(side) : 0;
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof DungeonPortalBlockEntity portal ? portal.comparator(null) : 0;
    }

    /** The portal is not pushed around, so an active one cannot lose its place in the portal list. */
    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }
}
