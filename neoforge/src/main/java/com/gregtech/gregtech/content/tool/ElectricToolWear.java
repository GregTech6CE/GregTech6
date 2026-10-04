package com.gregtech.gregtech.content.tool;

import com.gregtech.gregtech.item.ElectricToolItem;
import net.minecraft.world.item.ItemStack;
import java.util.function.IntUnaryOperator;

/** GT6 MultiItemTool material durability and electric wear lottery, separate from stored EU. */
public final class ElectricToolWear {
    private ElectricToolWear() {}
    public static long maximum(ElectricToolItem tool, ItemStack stack) {
        return ElectricToolWearRules.maximum(tool.headMaterial(stack).getToolDurability()) * tool.definition().durabilityMultiplier();
    }
    public static long damage(ItemStack stack) {
        var stats = com.gregtech.gregtech.platform.neoforge.StackCustomData.read(stack).getCompound("GT.ToolStats");
        return stats == null ? 0 : Math.max(0, stats.getLong("k"));
    }
    public static boolean broken(ElectricToolItem tool, ItemStack stack) {
        return damage(stack) >= maximum(tool, stack);
    }
    public static int chanceBound(ElectricToolItem tool, ItemStack stack) {
        return ElectricToolWearRules.chanceBound(tool.headMaterial(stack).getToolQuality());
    }
    /** Caller provides the world's RNG, or a controlled RNG for boundary tests. */
    public static void apply(ElectricToolItem tool, ItemStack stack, long amount, IntUnaryOperator random) {
        if (amount <= 0 || stack.getCount() != 1 || random.applyAsInt(chanceBound(tool, stack)) != 0) return;
        long old = damage(stack);
        com.gregtech.gregtech.platform.neoforge.StackCustomData.update(stack,tag->{var stats=tag.getCompound("GT.ToolStats");stats.putLong("k",ElectricToolWearRules.addDamage(old,amount));tag.put("GT.ToolStats",stats);});
    }
    /** ToolStats: 1 + nextInt(1 + 4 * materialAmount / U). */
    public static int scrapRandomBound(ElectricToolItem tool) {
        return ElectricToolWearRules.scrapRandomBound(tool.toolName());
    }
}
