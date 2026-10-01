package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.item.GTDungeonKeyItem;
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

/**
 * The dungeon portal of GT6's two portal rooms
 * ({@code gregapi/worldgen/dungeon/DungeonChunkRoomPortalNether.java} and
 * {@code DungeonChunkRoomPortalEnd.java}): the block that fills the portal frame those rooms build.
 *
 * <p>GT6 has no such block - its rooms leave the obsidian frame of the Nether room empty (the player
 * lights it with flint and steel, i.e. a vanilla nether portal) and place the vanilla
 * {@code Blocks.end_portal} in the End room - and its own portals are the craftable miniature portal
 * multi-tiles ({@code MultiTileEntityMiniPortalNether}, id 32766, and {@code MultiTileEntityMiniPortalEnd},
 * id 32000) that a player builds somewhere else. This block is the port's answer to the same job: it
 * carries GT6's portal state ({@link DungeonPortalBlockEntity}) and is what the two rooms place.</p>
 *
 * <h2>Activation rule</h2>
 * <p>Following GT6's key handling ({@code gregtech/items/behaviors/Behavior_Key.java:44-64} and
 * {@code MultiTileEntitySafeKeyLocked.useKey:79-91}):</p>
 * <ul>
 *   <li>every portal of a dungeon room is built with one of the dungeon's key ids
 *       ({@link com.gregtech.gregtech.worldgen.dungeon.GTDungeonChunkRoomPortal}),</li>
 *   <li>right-clicking it with a dungeon key whose {@code gt.key} id matches opens it,</li>
 *   <li>a key with a different id does nothing, and a portal without an id (a hand-placed block, or a
 *       cell built without key ids) adopts the id of the first key used on it - GT6's
 *       {@code if (mID == 0) mID = tID;} of the safe,</li>
 *   <li>an open portal is closed again with flint and steel, GT6's {@code TOOL_igniter} toggle
 *       ({@code MultiTileEntityMiniPortalNether:116-128}); a key does not close it.</li>
 * </ul>
 *
 * <h2>Port differences (rendering and behaviour)</h2>
 * <ul>
 *   <li>GT6's portal is a multi-tile with thirteen render passes
 *       ({@code MultiTileEntityMiniPortal:272-323}): the inner portal cube of
 *       {@code sBlockBounds[0]} plus the twelve two-pixel frame bars of {@code sBlockBounds[1..12]}.
 *       The port's block model has the same shapes (see
 *       {@code assets/gregtech/models/block/dungeon/portal_*}), but they are static quads and the
 *       frame bars only show while the portal is closed, because 1.20.1 models cannot switch a part of
 *       a model on a block state.</li>
 *   <li>GT6's portal texture is the vanilla portal texture (untinted for the Nether, tinted black for
 *       the End, {@code MultiTileEntityMiniPortalEnd:128}); the port's Nether portal uses
 *       {@code minecraft:block/nether_portal} and its End portal a black copy of GT6's own portal
 *       texture, {@code gregtech:block/dungeon/portal_end}.</li>
 *   <li>The particle effect of GT6's {@code randomDisplayTick} ({@code MultiTileEntityMiniPortalNether:62-64})
 *       and its activation sound ({@code MultiTileEntityMiniPortal:254}) are not ported.</li>
 *   <li>GT6's portal hardness is the one of obsidian (Nether) and end stone (End)
 *       ({@code MultiTileEntityMiniPortalNether:130-131}, {@code ...End:125-126}), which the port's two
 *       blocks copy.</li>
 * </ul>
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

    @Override public com.mojang.serialization.MapCodec<? extends Block> codec(){return com.mojang.serialization.MapCodec.unit(this);}
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME,CONSUME_PARTIAL->net.minecraft.world.ItemInteractionResult.CONSUME;case FAIL->net.minecraft.world.ItemInteractionResult.FAIL;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return interact(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
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

    /**
     * GT6's mini portals teleport nothing (they relay); the port's dungeon portal moves an entity that
     * is inside it, which is checked here the way vanilla's portals check it - the game calls this for
     * every block an entity's bounding box touches. See
     * {@link DungeonPortalBlockEntity#teleport(ServerLevel, BlockPos, Entity)}, which is also what the
     * tests drive directly.
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !state.getValue(ACTIVE)) return;
        DungeonPortalBlockEntity.teleport((ServerLevel) level, pos, entity);
    }

    public InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        boolean igniter = held.is(Items.FLINT_AND_STEEL);
        if (!igniter && !(held.getItem() instanceof GTDungeonKeyItem)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof DungeonPortalBlockEntity portal)) return InteractionResult.PASS;
        if (igniter) {
            // GT6's TOOL_igniter: an active portal is extinguished, an inactive one lit.
            if (portal.isActive()) portal.deactivate(); else portal.activate();
            return InteractionResult.CONSUME;
        }
        return portal.useKey(GTDungeonKeyItem.keyId(held)) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    /** GT6's portal tooltips ({@code MultiTileEntityMiniPortalNether:49-59}, {@code ...End:49-60}). */
    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,List<Component> tooltip,
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
        // GT6 asks for flint and steel or an Ender Eye here; the port's dungeon portal needs a key.
        tooltip.add(Component.translatable("tooltip.gregtech.portal.key").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.gregtech.portal.ignite").withStyle(ChatFormatting.DARK_GRAY));
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
