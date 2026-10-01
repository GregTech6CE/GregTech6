package com.gregtech.gregtech.content.loot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * GT6's loot bags — the items that open GT's own loot tables.
 *
 * <p>In GT6 these are MultiItem entries whose behavior is
 * {@code Behavior_Drop_Loot("gt.<table>", …)} ({@code MultiItemRandomTools:583-586},
 * {@code MultiItemBooks:67}): right-click a block, the bag is consumed and one stack per listed
 * table is dropped in front of the player, drawn with {@code ChestGenHooks.getOneItem} — a weighted
 * pick with the row's stack-size range ("Loot: Gems, one of which Flawless" is
 * {@code ("gt.flawless", "gt.gems", "gt.gems")}, i.e. three stacks). That is how a player gets GT's
 * gem, seed, sapling, misc and book loot: those tables are never rolled by world chests (world
 * chests roll the <em>vanilla</em> category named by the chest's {@code gt.dungeonloot}, see
 * {@link LootTableInjection} and {@link GTLootTables}).</p>
 *
 * <p>The port registers the four bags in {@code GTMultiItems} (their ids come from the GT6 display
 * names: Bagged Sapling, Seed Pouch, Gem Pouch, Loot Pouch) and the Dusty Guide Book here.</p>
 */
public class LootBagItem extends Item {

    /** GT6's {@code Behavior_Drop_Loot} tables, rolled in this order. */
    private final String[] tables;

    public LootBagItem(Item.Properties properties, String... tables) {
        super(properties);
        this.tables = tables;
    }

    /** The GT6 tables this bag rolls (tests read it). */
    public String[] tables() {
        return tables.clone();
    }

    /**
     * Rolls every table of this bag and drops the stacks where the player clicked, consuming one bag
     * (GT6's {@code Behavior_Drop_Loot.onItemUse}, including its {@code SFX.MC_DIG_CLOTH} sound).
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack bag = context.getItemInHand();
        BlockPos spawn = context.getClickedPos().relative(context.getClickedFace());
        for (String table : tables) {
            ItemStack loot = GTLootTables.roll(table, level.random);
            if (!loot.isEmpty()) net.minecraft.world.level.block.Block.popResource(level, spawn, loot);
        }
        if (player == null || !player.getAbilities().instabuild) bag.shrink(1);
        level.playSound(null, spawn, SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 0.6F, 1.0F);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // GT6: Behavior_Drop_Loot.getAdditionalToolTips -> "Rightclick this on a Block to loot"
        tooltip.add(Component.translatable("gregtech.tooltip.loot_bag").withStyle(ChatFormatting.GRAY));
    }
}
