package com.gregtech.gregtech.block.wood;

import com.gregtech.gregtech.registry.GTWoods;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 tinted leaves block.
 *
 * <p>GT6's leaves do <em>not</em> disappear on their own: {@code BlockBaseLeaves.updateTick2} first
 * scans a per-species box around the leaf for the matching log and only drops the leaf when that log
 * is gone ({@code beginLeavesDecay} even refuses to schedule the check for meta &lt; 8, GT6's
 * "will not decay" flag). Vanilla {@code LeavesBlock} instead decays a leaf as soon as its
 * {@code distance} property is 7, and since the port's logs are not vanilla logs the distance of
 * every worldgen leaf stayed at 7 — so naturally grown trees lost their leaves over time.
 *
 * <p>The port now implements GT6's rule: {@code persistent} stands for GT6's meta &lt; 8 (never
 * decays), everything else is checked against the species' log within GT6's scan ranges
 * ({@code BlockTreeLeavesAB/BlockTreeLeavesCD.getLeavesRangeSide/YNeg}, {@code YPos} is always 0
 * because GT6 never places leaves below the trunk). The drop itself stays GT6's
 * ({@link #getDrops}: saplings and the occasional stick).
 */
public class WoodLeavesBlock extends LeavesBlock {
    private final WoodSpecies species;

    public WoodLeavesBlock(WoodSpecies species, Properties properties) {
        super(properties);
        this.species = species;
    }

    /** GT6 {@code BlockTreeLeavesAB.getLeavesRangeSide} / {@code BlockTreeLeavesCD}'s blue spruce. */
    public static int rangeSide(WoodSpecies species) {
        return switch (species) {
            case RUBBER -> 2;                                    // AB meta 0
            case WILLOW, COCONUT -> 4;                           // AB metas 2 and 6
            case BLUE_SPRUCE, PINE -> 6;                         // CD meta 0 (the conifer's wide skirt)
            case MAPLE, BLUE_MAHOE, HAZEL, CINNAMON, RAINBOWOOD -> 3;
            // The wood-dictionary species borrow their shape's ranges.
            case EBONY -> 3;
            case WHITE_MAHOE -> 3;
        };
    }

    /** GT6 {@code BlockTreeLeavesAB.getLeavesRangeYNeg} / {@code BlockTreeLeavesCD} (always 2). */
    public static int rangeYNeg(WoodSpecies species) {
        return switch (species) {
            case BLUE_MAHOE -> 4;                                // AB meta 3
            case CINNAMON, RAINBOWOOD -> 3;                      // AB metas 5 and 7
            case COCONUT -> 1;                                   // AB meta 6
            case WHITE_MAHOE -> 4;                               // borrows the blue mahoe shape
            default -> 2;                                        // rubber, maple, willow, hazel, spruce, pine, ebony
        };
    }

    /**
     * GT6's decay check ({@code BlockBaseLeaves.updateTick2}): is the species' log still inside the
     * scan box {@code ±side} horizontally and {@code -yNeg..0} vertically?
     */
    public static boolean hasLogNearby(net.minecraft.world.level.BlockGetter level, BlockPos pos, WoodSpecies species) {
        Block log = GTWoods.log(species);
        int side = rangeSide(species);
        int down = rangeYNeg(species);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -side; dx <= side; dx++) {
            for (int dy = -down; dy <= 0; dy++) {
                for (int dz = -side; dz <= side; dz++) {
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (level.getBlockState(cursor).is(log)) return true;
                }
            }
        }
        return false;
    }

    /**
     * GT6's decay instead of vanilla's distance based one. {@code persistent} leaves (GT6's meta
     * &lt; 8, e.g. what a player places) are never touched, and a leaf of a standing tree is kept as
     * long as the tree's own log is in range — exactly like GT6.
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LeavesBlock.PERSISTENT)) return;
        if (hasLogNearby(level, pos, species)) return;
        dropResources(state, level, pos);
        level.removeBlock(pos, false);
    }

    @Override
    public java.util.List<net.minecraft.world.item.ItemStack> getDrops(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        var tool=builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL);
        if(tool==null)tool=net.minecraft.world.item.ItemStack.EMPTY;
        if(tool.canPerformAction(net.minecraftforge.common.ToolActions.SHEARS_HARVEST)
                || com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(tool,com.gregtech.gregtech.api.tool.GTToolType.SCISSORS)
                || net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,tool)>0)
            return java.util.List.of(new net.minecraft.world.item.ItemStack(this));
        int fortune=net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE,tool);
        int chance=fortune<=0?50:fortune>=4?5:Math.max(5,50-(5<<fortune));
        var random=builder.getLevel().random;
        if(random.nextInt(chance)==0) {
            var sapling=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("gregtech","sapling_"+species.id()));
            return java.util.List.of(new net.minecraft.world.item.ItemStack(sapling));
        }
        if((species==WoodSpecies.WILLOW || species==WoodSpecies.BLUE_MAHOE) && random.nextInt(chance)<2)
            return java.util.List.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
        return java.util.List.of();
    }

    public WoodSpecies species() { return species; }
}
