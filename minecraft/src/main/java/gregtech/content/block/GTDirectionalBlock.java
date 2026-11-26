package gregtech.content.block;

import gregtech.engine.tool.ToolInteractions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Thin {@link DirectionalBlock} adapter that centralizes the 1.21.1 {@code MapCodec} stub as well
 * as 1.20.1 / 1.21.1 interaction and tooltip signatures without holding gameplay fields or state.
 */
public abstract class GTDirectionalBlock extends DirectionalBlock {

    protected GTDirectionalBlock(Properties properties) {
        super(properties);
    }

//? if >=1.21.1 {
    /*// Satisfies 1.21.1 abstract codec() redeclaration; runtime registration uses block instances.
    @Override
    protected com.mojang.serialization.MapCodec<? extends DirectionalBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }
*///?} else {
//?}

//? if >=1.21.1 {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = useBlock(state, level, pos, player, hand, hit);
        return result == null ? super.useItemOn(stack, state, level, pos, player, hand, hit)
                : ToolInteractions.toItemResult(result);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        InteractionResult result = useBlock(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
        return result == null ? super.useWithoutItem(state, level, pos, player, hit) : result;
    }
*///?} else {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = useBlock(state, level, pos, player, hand, hit);
        return result == null ? super.use(state, level, pos, player, hand, hit) : result;
    }
//?}

    /**
     * Handles block right-click interaction across 1.20.1 / 1.21.1.
     *
     * <p>Returning {@code null} delegates back to vanilla {@code BlockBehaviour} defaults so
     * non-interactive subclasses preserve {@code PASS_TO_DEFAULT_BLOCK_INTERACTION} on 1.21.1.</p>
     */
    @Nullable
    protected InteractionResult useBlock(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return null;
    }

//? if >=1.21.1 {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        appendTooltip(stack, context.level(), context.registries(), tooltip, flag);
    }
*///?} else {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
            List<Component> tooltip, TooltipFlag flag) {
        appendTooltip(stack, level, null, tooltip, flag);
    }
//?}

    /** Appends block tooltip lines when registry lookup access may also be required on 1.21.1. */
    protected void appendTooltip(ItemStack stack, @Nullable BlockGetter level,
            @Nullable HolderLookup.Provider registries, List<Component> tooltip, TooltipFlag flag) {
        appendTooltip(stack, level, tooltip, flag);
    }

    /** Appends block tooltip lines across 1.20.1 / 1.21.1. */
    protected void appendTooltip(ItemStack stack, @Nullable BlockGetter level,
            List<Component> tooltip, TooltipFlag flag) {
    }

//? if >=1.21.1 {
    /*@Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        onPlayerWillDestroy(level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
*///?} else {
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        onPlayerWillDestroy(level, pos, state, player);
        super.playerWillDestroy(level, pos, state, player);
    }
//?}

    /** Runs before vanilla {@code playerWillDestroy} handling across 1.20.1 / 1.21.1. */
    protected void onPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
    }

//? if >=1.21.1 {
    /*@Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level,
            BlockPos pos, Player player) {
        ItemStack stack = onGetCloneItemStack(state, target, level, pos, player);
        return stack != null ? stack : super.getCloneItemStack(state, target, level, pos, player);
    }
*///?} else {
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = onGetCloneItemStack(state, null, level, pos, null);
        return stack != null ? stack : super.getCloneItemStack(level, pos, state);
    }
//?}

    /**
     * Handles block pick-block cloning across 1.20.1 / 1.21.1.
     *
     * <p>Returning {@code null} delegates back to vanilla {@code getCloneItemStack} defaults.
     * On 1.20.1, {@code target} and {@code player} are {@code null}.</p>
     */
    @Nullable
    protected ItemStack onGetCloneItemStack(BlockState state, @Nullable HitResult target,
            BlockGetter level, BlockPos pos, @Nullable Player player) {
        return null;
    }
}
