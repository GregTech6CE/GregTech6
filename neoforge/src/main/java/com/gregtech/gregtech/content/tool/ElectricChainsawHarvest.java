package com.gregtech.gregtech.content.tool;

import com.gregtech.gregtech.item.ElectricToolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.IShearable;

/** GT_Tool_Chainsaw_LV target/quality rules and leaf/ice drop conversion. */
public final class ElectricChainsawHarvest {
    private static final net.minecraft.tags.TagKey<Block> SAW = BlockTags.create(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","mineable/saw"));
    private ElectricChainsawHarvest() {}
    // Forge's vanilla leaves inherit an empty default onSheared; their shearing is a loot-table rule.
    private static final ClassValue<Boolean> DEFAULT_SHEARING = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("onSheared",Player.class,ItemStack.class,
                        net.minecraft.world.level.Level.class,BlockPos.class).getDeclaringClass()==IShearable.class;
            } catch (NoSuchMethodException e) { throw new IllegalStateException(e); }
        }
    };
    public static boolean target(BlockState state) {
        var b=state.getBlock();
        return state.is(BlockTags.MINEABLE_WITH_AXE) || state.is(SAW) || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.CORALS) || state.is(BlockTags.CORAL_BLOCKS)
                || b instanceof BushBlock || b instanceof CactusBlock || b instanceof VineBlock
                || b instanceof GrowingPlantBlock || b instanceof LeavesBlock
                || b instanceof IceBlock || b==Blocks.PACKED_ICE || b==Blocks.MELON || b instanceof PumpkinBlock;
    }
    public static boolean canHarvest(ElectricToolItem tool, ItemStack stack, BlockState state) {
        return tool.toolName().equals("Chainsaw") && tool.isPoweredUsable(stack) && target(state)
                && (long)tool.headMaterial(stack).getToolQuality()+1 >= ElectricWrenchHarvest.requiredQuality(state);
    }
    public static float speed(ElectricToolItem tool, ItemStack stack) {
        return Math.max(Float.MIN_NORMAL,2*tool.headMaterial(stack).getToolSpeed());
    }
    /** Called by ServerPlayerGameMode after Forge's cancellable break event. */
    public static boolean convertDrops(ElectricToolItem tool, ItemStack stack, BlockPos pos, Player player) {
        if (!(player instanceof ServerPlayer server) || player.getAbilities().instabuild || !player.mayBuild()
                || !player.level().mayInteract(player,pos)
                || player.blockActionRestricted(player.level(),pos,server.gameMode.getGameModeForPlayer())) return false;
        var level=server.serverLevel();var state=level.getBlockState(pos);var block=state.getBlock();
        if (!canHarvest(tool,stack,state) || state.getDestroySpeed(level,pos)<0 || state.hasBlockEntity()) return false;
        java.util.List<ItemStack> drops;
        if ((state.is(BlockTags.LEAVES)||block instanceof LeavesBlock) && block instanceof IShearable shearable
                && shearable.isShearable(player,stack,level,pos)) {
            if (DEFAULT_SHEARING.get(block.getClass())) {
                var shears=new ItemStack(net.minecraft.world.item.Items.SHEARS);
                shears.applyComponents(stack.getComponentsPatch());
                drops=Block.getDrops(state,level,pos,null,player,shears);
            } else {
                drops=shearable.onSheared(player,stack,level,pos);
            }
        } else if (block instanceof IceBlock || block==Blocks.PACKED_ICE) {
            drops=Block.getDrops(state,level,pos,null,player,stack);
            // Preserve existing silk/loot results. Only replace the original empty-ice result.
            if (!drops.isEmpty()) return false;
            var ice=new ItemStack(block);if (ice.isEmpty()) return false;
            drops=java.util.List.of(ice);
        } else return false;
        if (!state.onDestroyedByPlayer(level,pos,player,true,state.getFluidState())) return true;
        block.destroy(level,pos,state);
        var before=stack.copy();
        tool.mineBlock(stack,level,state,pos,player);
        if (stack.isEmpty()) net.neoforged.neoforge.event.EventHooks.onPlayerDestroyItem(player,before,net.minecraft.world.InteractionHand.MAIN_HAND);
        for (var drop:drops) if (!drop.isEmpty()) Block.popResource(level,pos,drop);
        state.spawnAfterBreak(level,pos,before,true);
        player.awardStat(Stats.BLOCK_MINED.get(block));player.causeFoodExhaustion(.005F);
        level.levelEvent(player,2001,pos,Block.getId(state));
        return true;
    }
}
