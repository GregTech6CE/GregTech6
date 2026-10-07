package com.gregtech.gregtech.block.inventory;

import com.gregtech.gregtech.api.GTWaterloggable;
import com.gregtech.gregtech.blockentity.inventory.SafeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/** GT6 15-slot mechanical or key-controlled safe, with front-only GUI access. */
public class SafeBlock extends DirectionalBlock implements EntityBlock, SimpleWaterloggedBlock, com.gregtech.gregtech.api.tool.ToolInteractionTarget {
    @Override protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec() { return com.mojang.serialization.MapCodec.unit(this); }
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty OPEN = net.minecraft.world.level.block.state.properties.BooleanProperty.create("open");
    private final boolean keyLocked;
    private final com.gregtech.gregtech.api.material.GTMaterial material;

    public SafeBlock(Properties properties) {
        this(com.gregtech.gregtech.content.material.Materials.Steel, false, properties);
    }
    public SafeBlock(com.gregtech.gregtech.api.material.GTMaterial material, boolean keyLocked, Properties properties) {
        super(properties);
        this.material = material;
        this.keyLocked = keyLocked;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(GTWaterloggable.WATERLOGGED, false).setValue(OPEN, false));
    }
    public boolean keyLocked() { return keyLocked; }
    public com.gregtech.gregtech.api.material.GTMaterial material() { return material; }
    @Override public com.gregtech.gregtech.api.tool.ToolInteractionSpec toolInteraction(BlockState state, ItemStack tool) {
        return com.gregtech.gregtech.api.tool.GTToolHelper.isMachineWrench(tool)
                ? com.gregtech.gregtech.api.tool.ToolInteractionSpec.facing(FACING, com.gregtech.gregtech.block.machine.MachineRotationType.ALL) : null;
    }
    @Override public boolean canUseTool(Level level, BlockPos pos, Player player, ItemStack tool) {
        return level.getBlockEntity(pos) instanceof SafeBlockEntity safe && safe.canOpen(player);
    }
    @Override public boolean beginToolUse(Level level, BlockPos pos, Player player, ItemStack tool) {
        return level.getBlockEntity(pos) instanceof SafeBlockEntity safe && safe.claimAndOpen(player);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(GTWaterloggable.WATERLOGGED);
        builder.add(OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        float pitch = ctx.getPlayer() == null ? 0 : ctx.getPlayer().getXRot();
        Direction facing = pitch >= 65 ? Direction.UP : pitch <= -65 ? Direction.DOWN : ctx.getHorizontalDirection().getOpposite();
        BlockState base = defaultBlockState().setValue(FACING, facing);
        return GTWaterloggable.getStateForPlacement(base, ctx);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return GTWaterloggable.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        GTWaterloggable.updateShape(state, level, pos);
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        if (!keyLocked && placer instanceof Player player && player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof SafeBlockEntity safe) {
            safe.claimAndOpen(player);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SafeBlockEntity(pos, state);
    }

    public InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // both hands fire use(); handling the off hand double-triggers
        // (insert+extract in one click, double withdrawals, armor swap-backs)
        if (hand == net.minecraft.world.InteractionHand.OFF_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof SafeBlockEntity safe)) return InteractionResult.PASS;
        if (com.gregtech.gregtech.api.tool.ToolInteractions.use(state, level, pos, player, hand, hit)) return InteractionResult.sidedSuccess(level.isClientSide);
        if (player.getItemInHand(hand).getItem() instanceof com.gregtech.gregtech.item.GTDungeonKeyItem && keyLocked) {
            if (!level.isClientSide) safe.useKey(player.getItemInHand(hand));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (hit.getDirection() != state.getValue(FACING)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (!safe.claimAndOpen(player)) {
                player.displayClientMessage(Component.translatable("message.gregtech.safe.locked"), true);
            } else {
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> safe.createMenu(id, inv),
                        Component.translatable(getDescriptionId())));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return switch(interact(state,level,pos,player,hand,hit)){case SUCCESS->net.minecraft.world.ItemInteractionResult.SUCCESS;case CONSUME,CONSUME_PARTIAL->net.minecraft.world.ItemInteractionResult.CONSUME;case FAIL->net.minecraft.world.ItemInteractionResult.FAIL;default->net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;};}
    @Override public InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return interact(state,level,pos,player,InteractionHand.MAIN_HAND,hit);}
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof SafeBlockEntity safe) {
            safe.dropContents();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SafeBlockEntity safe && !safe.claimAndOpen(player)) return 0;
        return Math.max(super.getDestroyProgress(state, player, level, pos), .0001F);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        com.gregtech.gregtech.client.StorageBlockTooltips.safe(stack, keyLocked, tooltip);
    }
}
