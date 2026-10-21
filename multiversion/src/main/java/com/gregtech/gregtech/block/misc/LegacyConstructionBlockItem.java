package com.gregtech.gregtech.block.misc;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Retained legacy IDs place and decode into the corresponding registered color identity. */
public abstract class LegacyConstructionBlockItem extends BlockItem {
    protected LegacyConstructionBlockItem(Block block, Properties properties) { super(block, properties); }

    /** A bare legacy ingredient used to accept every color of the same palette block. */
    public static java.util.List<ItemStack> ingredientAlternatives(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof LegacyConstructionBlockItem legacy)
                || !com.gregtech.gregtech.registry.GTConstructionVariants.legacy(legacy.getBlock())) return java.util.List.of();
//? if >=1.21.1 {
        /*if (!stack.getComponentsPatch().isEmpty()) return java.util.List.of(canonical(stack));
*///?} else {
        if (stack.hasTag()) return java.util.List.of(canonical(stack));
//?}
        var result = new java.util.ArrayList<ItemStack>();
        for (var color : net.minecraft.world.item.DyeColor.values())
            result.add(new ItemStack(com.gregtech.gregtech.registry.GTConstructionVariants.coloredBlock(legacy.getBlock(), color), stack.getCount()));
        return result;
    }

    public static ItemStack canonical(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof LegacyConstructionBlockItem legacy)
                || !com.gregtech.gregtech.registry.GTConstructionVariants.legacy(legacy.getBlock())) return stack;
        var color = legacy.getBlock() instanceof ColoredConstructionBlock
                ? ColoredConstructionBlock.itemColor(stack) : legacy.getBlock() instanceof ColoredGlassBlock
                ? ColoredGlassBlock.itemColor(stack) : ConcreteBlock.itemColor(stack);
        Block block = com.gregtech.gregtech.registry.GTConstructionVariants.coloredBlock(legacy.getBlock(), color);
        if (block == null) return stack;
        ItemStack result = new ItemStack(block, stack.getCount());
//? if >=1.21.1 {
        /*result.applyComponents(stack.getComponentsPatch());
        var state = result.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
        if (state != null) {
            var properties = new java.util.HashMap<>(state.properties());
            properties.remove("color");
            if (properties.isEmpty()) result.remove(net.minecraft.core.component.DataComponents.BLOCK_STATE);
            else result.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                    new net.minecraft.world.item.component.BlockItemStateProperties(properties));
        }
*///?} else {
        if (stack.hasTag()) result.setTag(stack.getTag().copy());
        var state = result.getTagElement("BlockStateTag");
        if (state != null) {
            state.remove("color");
            if (state.isEmpty()) result.removeTagKey("BlockStateTag");
        }
        if (result.hasTag() && result.getTag().isEmpty()) result.setTag(null);
//?}
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
