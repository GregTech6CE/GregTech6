package com.gregtech.gregtech.content.tool;

import com.gregtech.gregtech.item.ElectricToolItem;
import net.minecraft.world.item.ItemStack;
import java.util.function.IntUnaryOperator;

/** GT6 MultiItemTool material durability and electric wear lottery, separate from stored EU. */
public final class ElectricToolWear {
    private ElectricToolWear() {}
    public static long maximum(ElectricToolItem tool, ItemStack stack) {
        long base = Math.max(0, tool.headMaterial(stack).getToolDurability());
        return base > Long.MAX_VALUE / 100 ? Long.MAX_VALUE : Math.max(1, base * 100);
    }
    public static long damage(ItemStack stack) {
        var stats = stack.getTagElement("GT.ToolStats");
        return stats == null ? 0 : Math.max(0, stats.getLong("k"));
    }
    public static boolean broken(ElectricToolItem tool, ItemStack stack) {
        return damage(stack) >= maximum(tool, stack);
    }
    public static int chanceBound(ElectricToolItem tool, ItemStack stack) {
        return (int)Math.min(Integer.MAX_VALUE, Math.max(10L, tool.headMaterial(stack).getToolQuality() * 20L));
    }
    /** Caller provides the world's RNG, or a controlled RNG for boundary tests. */
    public static void apply(ElectricToolItem tool, ItemStack stack, long amount, IntUnaryOperator random) {
        if (amount <= 0 || stack.getCount() != 1 || random.applyAsInt(chanceBound(tool, stack)) != 0) return;
        long old = damage(stack);
        stack.getOrCreateTagElement("GT.ToolStats").putLong("k", old > Long.MAX_VALUE - amount ? Long.MAX_VALUE : old + amount);
    }
    /** ToolStats: 1 + nextInt(1 + 4 * materialAmount / U). */
    public static int scrapRandomBound(ElectricToolItem tool) {
        return switch (tool.toolName()) {
            case "Drill" -> 3; // half an ingot rod
            case "Chainsaw" -> 9; // two ingots
            case "Wrench" -> 17; // four ingots
            case "Screwdriver" -> 5; // one ingot
            default -> throw new IllegalArgumentException("Unknown electric tool " + tool.toolName());
        };
    }
}
