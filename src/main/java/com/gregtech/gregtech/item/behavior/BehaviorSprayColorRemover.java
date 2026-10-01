package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.item.behavior.ItemBehaviors.Consumable;
import com.gregtech.gregtech.item.behavior.ItemBehaviors.Outcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;

import javax.annotation.Nullable;

/**
 * GT6 {@code Behavior_Spray_Color_Remover} ({@code Behavior_Spray_Color_Remover.java:46-123}) — the
 * Paint Removal Spray.
 *
 * <h2>The original</h2>
 *
 * <p>{@code decolorize(...)} ({@code :96-106}) has six routes, in this order:
 * {@code ITileEntityDecolorable} ({@code :98}), {@code IBlockDecolorable} ({@code :100}), then the
 * four vanilla-ish identities — stained hardened clay becomes hardened clay, stained glass pane
 * becomes glass pane, stained glass becomes glass ({@code :101-103}) and GT's own grass becomes
 * vanilla grass ({@code :104}). Exactly one is used per click and the operation costs
 * {@code 10} uses ({@code :77}).</p>
 *
 * <h2>The port's three routes</h2>
 *
 * <p>1.20.1 splits what the original could express with block identities:</p>
 *
 * <ul>
 *   <li>stained hardened clay is the sixteen {@code minecraft:*_terracotta} blocks. The original
 *       tested one block identity ({@code aBlock == Blocks.stained_hardened_clay}) because 1.7.10
 *       carried the colour in metadata; 1.20.1 has sixteen separate blocks, so {@link #STAINED_CLAY}
 *       is that same identity test spelled out. The replacement is the uncoloured
 *       {@code Blocks.TERRACOTTA} ({@code Blocks:593});</li>
 *   <li>stained glass is {@link StainedGlassBlock} ({@code Blocks:342-357}), so one class test
 *       covers all sixteen, and the replacement is {@code Blocks.GLASS} ({@code Blocks:141});</li>
 *   <li>stained glass panes are {@link StainedGlassPaneBlock} ({@code Blocks:538-553}), replaced by
 *       {@code Blocks.GLASS_PANE} ({@code Blocks:384}).</li>
 * </ul>
 *
 * <p>The original replaced the whole block with metadata {@code 0} ({@code DelegatorTileEntity
 * .setBlock(Block)} without a meta), which is what {@code defaultBlockState()} is here — the colour
 * is dropped, not carried over.</p>
 *
 * <h2>What is not ported</h2>
 *
 * <ul>
 *   <li>{@code ITileEntityDecolorable} and {@code IBlockDecolorable} ({@code :98,100}) have no port
 *       counterpart: the GT6 blocks that implemented them are the machine faces GT's own colour
 *       sprays painted.</li>
 *   <li>The GT grass branch ({@code :104}, {@code BlocksGT.Grass}). The port's coloured grasses are
 *       separate blocks ({@code gregtech:grass}, {@code gregtech:grass_<variant>},
 *       {@code gregtech:grassblock_<colour>} — {@code GTIconSetBlocks:118-124}) rather than one
 *       block with six metadata values, and which of them is "the spray-painted one" is not
 *       decidable from the port's tables, so no mapping is invented here.</li>
 *   <li>The painting spray itself, {@code Behavior_Spray_Color} ({@code :45-183}), is not portable
 *       for the same reason plus a missing item: the port has no dye spray cans at all
 *       ({@code GTMultiItemsGen.ENTRIES:63-71} carries only the empty can, this remover, the
 *       hardening spray, the C-Foam remover and the extinguisher).</li>
 * </ul>
 */
public final class BehaviorSprayColorRemover {

    /** GT6 {@code MultiItemRandomTools:271}: {@code (Spray_Empty, Spray_Color_Remover_Used, Spray_Color_Remover, 256)}. */
    public static final long USES = 256L;

    /** GT6 {@code Behavior_Spray_Color_Remover:54}: {@code mUses = aUses * 10}. */
    public static final int USES_MULTIPLIER = 10;

    /** GT6 {@code :77}: {@code tUses -= 10} for one decolorised block. */
    public static final int REMOVE_COST = 10;

    /**
     * The sixteen coloured terracottas — GT6's {@code aBlock == Blocks.stained_hardened_clay}
     * ({@code :101}) with the metadata split into separate 1.20.1 blocks ({@code Blocks:522-537}).
     * Plain {@code Blocks.TERRACOTTA} is deliberately not in the set, exactly like the original,
     * where plain hardened clay was a different metadata value and was not decolorised.
     */
    public static final java.util.Set<net.minecraft.world.level.block.Block> STAINED_CLAY = java.util.Set.of(
            Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA,
            Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA,
            Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA,
            Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA,
            Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA);

    private BehaviorSprayColorRemover() {}

    /** The port's can triple: the full id is registered first, the {@code _2} one is the used can. */
    public static Consumable paintRemovalSpray() {
        return new Consumable(ItemBehaviors.stack("gregtech:empty_spray_can"),
                ItemBehaviors.stack("gregtech:paint_removal_spray_2"),
                ItemBehaviors.stack("gregtech:paint_removal_spray"),
                USES * USES_MULTIPLIER);
    }

    /** GT6 {@code Behavior_Spray_Color_Remover:58-94}: one click. */
    public static Outcome useOn(Level level, BlockPos pos, Direction side, @Nullable Player player, ItemStack stack,
                                float hitX, float hitY, float hitZ) {
        return ItemBehaviors.useSpray(level, pos, side, player, stack, paintRemovalSpray(),
                BehaviorSprayColorRemover::decolorize, hitX, hitY, hitZ);
    }

    /**
     * GT6 {@code Behavior_Spray_Color_Remover:96-106} for the port's content.
     *
     * @return {@link #REMOVE_COST} when the block was decolorised, {@code 0} when it is not a
     *         colourable block
     */
    public static long decolorize(Level level, BlockPos pos, Direction side, long uses, @Nullable Player player,
                                  ItemStack can, float hitX, float hitY, float hitZ) {
        if (uses < 1L) return 0L;
        if(com.gregtech.gregtech.content.tool.PaintTargets.unpaint(level,pos))return REMOVE_COST;
        var state = level.getBlockState(pos);
        var block = state.getBlock();
        if (STAINED_CLAY.contains(block)) {                                       // :101
            return level.setBlock(pos, Blocks.TERRACOTTA.defaultBlockState(), 3) ? REMOVE_COST : 0L;
        }
        if (block instanceof StainedGlassPaneBlock) {                             // :102
            return level.setBlock(pos, Blocks.GLASS_PANE.defaultBlockState(), 3) ? REMOVE_COST : 0L;
        }
        if (block instanceof StainedGlassBlock) {                                 // :103
            return level.setBlock(pos, Blocks.GLASS.defaultBlockState(), 3) ? REMOVE_COST : 0L;
        }
        return 0L;                                                                // :104 is documented above
    }
}
