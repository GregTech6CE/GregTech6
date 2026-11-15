package gregtech.content.block;

import gregtech.content.loot.MaterialCrateLoot;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import gregtech.engine.tool.ToolInteractions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
//? if >=1.21.1 {
/*import net.minecraft.world.item.Item;
*///?} else {
//?}
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * GT6's twelve material crate prefix blocks (dust, gem, ingot, plate and raw crates, each in a
 * sixteen- and a sixty-four-piece size) as one block: a
 * crowbar pries it open and hands out sixteen or sixty-four units of one material plus the empty crate.
 *
 * <p>GT6 opens a material crate by <em>mining</em> it with a crowbar, whose
 * {@code GT_Tool_Crowbar.convertBlockDrops} replaces the block drop with the Unboxinator output. The
 * port opens on right-click instead, matching the loot crate; the yield is the same. The block carries
 * no block entity, so its content is resolved when it opens, not when the dungeon is generated.</p>
 */
public class MaterialCrateBlock extends Block {
    /** GT6's crowbar-only opening, minus its loot: the crowbar decides whether anything happens. */
//? if >=1.21.1 {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return ToolInteractions.toItemResult(use(state, level, pos, player, hand, hit));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return use(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
*///?} else {
    @Override
//?}
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!GTToolHelper.matchesTool(held, GTToolType.CROWBAR)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack content = MaterialCrateLoot.roll(level.random);
        // Remove first: a failed removal (protected position, concurrent open) must not duplicate items.
        if (!level.removeBlock(pos, false)) return InteractionResult.PASS;
        if (!content.isEmpty()) popResource(level, pos, content);
        ItemStack crate = LootCrateBlock.crateStack();
        if (!crate.isEmpty()) popResource(level, pos, crate);
        level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        GTToolHelper.damageForToolClickReturn(held, 500, player);
        return InteractionResult.CONSUME;
    }

    public MaterialCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
//? if >=1.21.1 {
    /*public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
*///?} else {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
//?}
        tooltip.add(Component.translatable("block.gt6ce.material_crate.tooltip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
