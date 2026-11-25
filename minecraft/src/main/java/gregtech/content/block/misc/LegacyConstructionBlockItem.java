package gregtech.content.block.misc;

import gregtech.engine.stack.StackData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Retained legacy IDs place and decode into the corresponding registered color identity. */
public abstract class LegacyConstructionBlockItem extends BlockItem {
    protected LegacyConstructionBlockItem(Block block, Properties properties) { super(block, properties); }

    private static final java.util.Map<Block, java.util.List<ItemStack>> MATCHING_ALTERNATIVES =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * A bare legacy ingredient used to accept every color of the same palette block.
     * The recipe lookup index expands these variants as requirement keys; change both together.
     */
    public static java.util.List<ItemStack> ingredientAlternatives(ItemStack stack) {
        if (!plainPalette(stack)) return matchingAlternatives(stack);
        var shared = matchingAlternatives(stack);
        var result = new java.util.ArrayList<ItemStack>(shared.size());
        for (var color : shared) result.add(color.copyWithCount(stack.getCount()));
        return result;
    }

    /**
     * Shared palette alternatives for matching: one count-1 stack per color, cached per legacy block.
     * Callers must only read the returned stacks; use {@link #ingredientAlternatives} for stacks that
     * may be modified or need the offered count.
     */
    public static java.util.List<ItemStack> matchingAlternatives(ItemStack stack) {
        if (!legacyPalette(stack)) return java.util.List.of();
        if (!plainPalette(stack)) return java.util.List.of(canonical(stack));
        var block = ((LegacyConstructionBlockItem) stack.getItem()).getBlock();
        return MATCHING_ALTERNATIVES.computeIfAbsent(block, key -> {
            var result = new java.util.ArrayList<ItemStack>(net.minecraft.world.item.DyeColor.values().length);
            for (var color : net.minecraft.world.item.DyeColor.values())
                result.add(new ItemStack(gregtech.content.registry.GTConstructionVariants.coloredBlock(key, color)));
            return java.util.List.copyOf(result);
        });
    }

    private static boolean legacyPalette(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof LegacyConstructionBlockItem legacy
                && gregtech.content.registry.GTConstructionVariants.legacy(legacy.getBlock());
    }

    private static boolean plainPalette(ItemStack stack) {
        if (!legacyPalette(stack)) return false;
        return !StackData.hasAnyData(stack);
    }

    /** Normalized offered identity; {@code RecipeInputs.offerKeys} mirrors this normalization. */
    public static ItemStack canonical(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof LegacyConstructionBlockItem legacy)
                || !gregtech.content.registry.GTConstructionVariants.legacy(legacy.getBlock())) return stack;
        var color = legacy.getBlock() instanceof ColoredConstructionBlock
                ? ColoredConstructionBlock.itemColor(stack) : legacy.getBlock() instanceof ColoredGlassBlock
                ? ColoredGlassBlock.itemColor(stack) : ConcreteBlock.itemColor(stack);
        Block block = gregtech.content.registry.GTConstructionVariants.coloredBlock(legacy.getBlock(), color);
        if (block == null) return stack;
        ItemStack result = new ItemStack(block, stack.getCount());
        StackData.copyAll(stack, result);
        if (gregtech.engine.stack.BlockStateData.has(result)) {
            var properties = new java.util.HashMap<>(gregtech.engine.stack.BlockStateData.readProperties(result));
            properties.remove("color");
            gregtech.engine.stack.BlockStateData.writeProperties(result, properties);
        }
        return result;
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) return null;
        ItemStack canonical = canonical(context.getItemInHand());
        return canonical.getItem() instanceof BlockItem item && item.getBlock() instanceof FixedConstructionColor fixed
                ? FixedConstructionColor.recolor(state, fixed.color()) : state;
    }
}
