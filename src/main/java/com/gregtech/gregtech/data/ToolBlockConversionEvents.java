package com.gregtech.gregtech.data;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * GT6 tool-driven block conversion.
 * <p>
 * In GregTech 6 a hard hammer (or jack hammer) hitting a block converts its drops through
 * {@code RM.Hammer}, and a chisel does the same through {@code RM.Chisel}
 * ({@code GT_Tool_HardHammer#convertBlockDrops}, {@code ToolCompat}). This port registered
 * hundreds of recipes into those maps — including the 974 vanilla block processing routes —
 * but nothing ever read them, so hammering a block in the world did nothing.
 * </p>
 * <p>
 * 1.20.1 has no drop-modifying event, so the break is intercepted, the block is removed, and
 * the converted drops are spawned in its place.
 * </p>
 */
@Mod.EventBusSubscriber(modid = "gregtech")
public final class ToolBlockConversionEvents {
    /** GT6 {@code getToolDamagePerBlockBreak} per tool. */
    private static final int HAMMER_BLOCK_BREAK = 25;
    private static final int CHISEL_BLOCK_BREAK = 50;
    private static final int SPADE_BLOCK_BREAK = 100;
    private static final int CROWBAR_BLOCK_BREAK = 50;
    /** GT6 {@code getToolDamagePerDropConversion} per tool. */
    private static final int HAMMER_PER_CONVERSION = 50;
    private static final int CHISEL_PER_CONVERSION = 100;
    private static final int SPADE_PER_CONVERSION = 100;
    private static final int CROWBAR_PER_CONVERSION = 100;

    private ToolBlockConversionEvents() {}

    private record Conversion(RecipeMap map, int blockBreakDamage, int perConversionDamage) {}

    /** Converted block drops and how many conversions were charged. */
    public record Result(List<ItemStack> drops, int conversions) {}

    private static Conversion conversionFor(ItemStack stack) {
        if (GTToolHelper.matchesTool(stack, GTToolType.HARD_HAMMER)) {
            return new Conversion(MachineRecipeMaps.Hammer, HAMMER_BLOCK_BREAK, HAMMER_PER_CONVERSION);
        }
        if (GTToolHelper.matchesTool(stack, GTToolType.CHISEL)) {
            return new Conversion(MachineRecipeMaps.Chisel, CHISEL_BLOCK_BREAK, CHISEL_PER_CONVERSION);
        }
        // GT6 GT_Tool_UniversalSpade / GT_Tool_Crowbar unpack the block's drops through RM.Unboxinator.
        if (GTToolHelper.matchesTool(stack, GTToolType.UNIVERSAL_SPADE)) {
            return new Conversion(MachineRecipeMaps.Unboxinator, SPADE_BLOCK_BREAK, SPADE_PER_CONVERSION);
        }
        if (GTToolHelper.matchesTool(stack, GTToolType.CROWBAR)) {
            return new Conversion(MachineRecipeMaps.Unboxinator, CROWBAR_BLOCK_BREAK, CROWBAR_PER_CONVERSION);
        }
        return null;
    }

    /**
     * GT6's {@code convertBlockDrops} core: the block itself converts when the map has a recipe
     * for it (blocks with a block entity never do), otherwise each drop converts individually.
     */
    public static Result convert(ServerLevel level, BlockState state, List<ItemStack> drops, ItemStack tool) {
        Conversion conversion = conversionFor(tool);
        if (conversion == null) return new Result(List.copyOf(drops), 0);

        List<ItemStack> converted = new ArrayList<>();
        int conversions = 0;
        Recipe blockRecipe = state.hasBlockEntity() ? null
                : conversion.map().findRecipe(List.of(new ItemStack(state.getBlock())), List.of(), false, 1, 1);
        if (blockRecipe != null) {
            converted.addAll(roll(level, blockRecipe));
            conversions = 1;
        } else {
            for (ItemStack drop : drops) {
                if (drop.isEmpty()) continue;
                Recipe perDrop = conversion.map().findRecipe(List.of(drop), List.of(), false, 1, 1);
                if (perDrop == null) {
                    converted.add(drop);
                    continue;
                }
                for (int i = 0; i < drop.getCount(); i++) converted.addAll(roll(level, perDrop));
                conversions += drop.getCount();
            }
        }
        return new Result(List.copyOf(converted), conversions);
    }

    @SubscribeEvent
    public static void convertBlockDrops(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var player = event.getPlayer();
        ItemStack tool = player.getMainHandItem();
        Conversion conversion = conversionFor(tool);
        if (conversion == null) return;

        var pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;

        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool);
        Result result = convert(level, state, drops, tool);
        if (result.conversions() == 0) return;

        int damage = conversion.blockBreakDamage() + result.conversions() * conversion.perConversionDamage();
        // GT6 only converts while the tool can pay for it; otherwise the break happens normally.
        int remaining = GTToolHelper.getMaxDurability(tool) - tool.getDamageValue();
        if (remaining < damage) return;

        event.setCanceled(true);
        level.removeBlock(pos, false);
        for (ItemStack stack : result.drops()) if (!stack.isEmpty()) Block.popResource(level, pos, stack);
        GTToolHelper.damageForUse(tool, damage, player);
    }

    /** One rolled output set of a conversion recipe, in GT6's {@code getOutputs(random)} sense. */
    private static List<ItemStack> roll(ServerLevel level, Recipe recipe) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < recipe.mOutputs.length; i++) {
            if (recipe.mOutputs[i] == null || recipe.mOutputs[i].isEmpty()) continue;
            long count = recipe.rollOutputCount(i, 1, level.random::nextInt);
            if (count > 0) result.add(recipe.mOutputs[i].copyWithCount((int) Math.min(count, 64)));
        }
        return result;
    }
}
