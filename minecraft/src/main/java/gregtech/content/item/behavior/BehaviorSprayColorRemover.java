package gregtech.content.item.behavior;

import gregtech.content.item.behavior.ItemBehaviors.Consumable;
import gregtech.content.item.behavior.ItemBehaviors.Outcome;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;

/**
 * GT6 color-removal can, adapted to the port's existing consumable ledger.
 * PaintTargets first handles registered construction colors, Paintable entities and GT grass.
 * The remaining local fallbacks restore vanilla terracotta, stained glass panes and stained glass.
 * Original delegating-tile and external IBlockDecolorable routes are separate compatibility contracts.
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
     * where plain hardened clay was a separate block and was not decolorised.
     */
    public static final java.util.Set<net.minecraft.world.level.block.Block> STAINED_CLAY = java.util.Set.of(
            Blocks.WHITE_TERRACOTTA,
            Blocks.ORANGE_TERRACOTTA,
            Blocks.MAGENTA_TERRACOTTA,
            Blocks.LIGHT_BLUE_TERRACOTTA,
            Blocks.YELLOW_TERRACOTTA,
            Blocks.LIME_TERRACOTTA,
            Blocks.PINK_TERRACOTTA,
            Blocks.GRAY_TERRACOTTA,
            Blocks.LIGHT_GRAY_TERRACOTTA,
            Blocks.CYAN_TERRACOTTA,
            Blocks.PURPLE_TERRACOTTA,
            Blocks.BLUE_TERRACOTTA,
            Blocks.BROWN_TERRACOTTA,
            Blocks.GREEN_TERRACOTTA,
            Blocks.RED_TERRACOTTA,
            Blocks.BLACK_TERRACOTTA);

    private BehaviorSprayColorRemover() {}

    /** The port's can triple: the full id is registered first, the {@code _2} one is the used can. */
    public static Consumable paintRemovalSpray() {
        return new Consumable(
                ItemBehaviors.stack("gt6ce:empty_spray_can"),
                ItemBehaviors.stack("gt6ce:paint_removal_spray_2"),
                ItemBehaviors.stack("gt6ce:paint_removal_spray"),
                USES * USES_MULTIPLIER);
    }

    /** GT6 {@code Behavior_Spray_Color_Remover:58-94}: one click. */
    public static Outcome useOn(
            Level level,
            BlockPos pos,
            Direction side,
            @Nullable Player player,
            ItemStack stack,
            float hitX,
            float hitY,
            float hitZ) {
        return ItemBehaviors.useSpray(
                level,
                pos,
                side,
                player,
                stack,
                paintRemovalSpray(),
                BehaviorSprayColorRemover::decolorize,
                hitX,
                hitY,
                hitZ);
    }

    /**
     * GT6 {@code Behavior_Spray_Color_Remover:96-106} for the port's content.
     *
     * @return {@link #REMOVE_COST} when the block was decolorised, {@code 0} when it is not a
     *         colourable block
     */
    public static long decolorize(
            Level level,
            BlockPos pos,
            Direction side,
            long uses,
            @Nullable Player player,
            ItemStack can,
            float hitX,
            float hitY,
            float hitZ) {
        if (uses < 1L) return 0L;
        if (gregtech.content.tool.PaintTargets.unpaint(level, pos)) return REMOVE_COST;
        var state = level.getBlockState(pos);
        var block = state.getBlock();
        if (STAINED_CLAY.contains(block)) {
            // :101
            return level.setBlock(pos, Blocks.TERRACOTTA.defaultBlockState(), 3) ? REMOVE_COST : 0L;
        }
        if (block instanceof StainedGlassPaneBlock) {
            // :102
            return level.setBlock(pos, Blocks.GLASS_PANE.defaultBlockState(), 3) ? REMOVE_COST : 0L;
        }
        if (block instanceof StainedGlassBlock) {
            // :103
            return level.setBlock(pos, Blocks.GLASS.defaultBlockState(), 3) ? REMOVE_COST : 0L;
        }
        return 0L;
        // GT paint, construction colors and grass are handled by PaintTargets above.
    }
}
