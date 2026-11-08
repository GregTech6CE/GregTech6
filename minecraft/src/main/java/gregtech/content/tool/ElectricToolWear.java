package gregtech.content.tool;

import gregtech.engine.stack.StackData;
import gregtech.engine.tool.ElectricToolWearRules;

import gregtech.content.item.ElectricToolItem;
import net.minecraft.world.item.ItemStack;
import java.util.function.IntUnaryOperator;

/** GT6 MultiItemTool material durability and electric wear lottery, separate from stored EU. */
public final class ElectricToolWear {
    private ElectricToolWear() {}
    public static long maximum(ElectricToolItem tool, ItemStack stack) {
        return ElectricToolWearRules.maximum(tool.headMaterial(stack).getToolDurability()) * tool.definition().durabilityMultiplier();
    }
    public static long damage(ItemStack stack) {
        var tag = StackData.read(stack);
        var stats = tag == null ? null : tag.getCompound("GT.ToolStats");
        return stats == null ? 0 : Math.max(0, stats.getLong("k"));
    }
    public static boolean broken(ElectricToolItem tool, ItemStack stack) {
        return damage(stack) >= maximum(tool, stack);
    }
    public static int chanceBound(ElectricToolItem tool, ItemStack stack) {
        return ElectricToolWearRules.chanceBound(tool.headMaterial(stack).getToolQuality());
    }
    /** GT6 tool-container crafting charge; shaped and shapeless rows must charge the same amount. */
    public static long craftEnergy(ElectricToolItem tool) {
//? if >=1.21.1 {
        /*String name = tool.toolName();
*///?} else {
        String name = tool.definition().name();
//?}
        if (name.equals("Chainsaw")) return 200;
        if (name.equals("Wrench")) return 800L * (1L << (2 * (tool.energyTier() - 1)));
        return name.equals("Screwdriver") ? 200 : 100;
    }
    /** Caller provides the world's RNG, or a controlled RNG for boundary tests. */
    public static void apply(ElectricToolItem tool, ItemStack stack, long amount, IntUnaryOperator random) {
        if (amount <= 0 || stack.getCount() != 1 || random.applyAsInt(chanceBound(tool, stack)) != 0) return;
        long old = damage(stack);
        StackData.update(stack,tag->{var stats=tag.getCompound("GT.ToolStats");stats.putLong("k",ElectricToolWearRules.addDamage(old,amount));tag.put("GT.ToolStats",stats);});
    }
    /** ToolStats: 1 + nextInt(1 + 4 * materialAmount / U). */
    public static int scrapRandomBound(ElectricToolItem tool) {
        return ElectricToolWearRules.scrapRandomBound(tool.toolName());
    }
}
