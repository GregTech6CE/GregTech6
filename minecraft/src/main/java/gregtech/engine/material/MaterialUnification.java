package gregtech.engine.material;

import gregtech.content.block.MaterialBlockLike;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Explicit equal-quantity vanilla targets. Existing registered GT stacks remain usable. */
public final class MaterialUnification {
    private MaterialUnification() {}

    private static Item requestedTarget(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof MaterialBlockLike block) {
            if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("gt6ce")) return null;
            String name = block.material().resolve().getName();
            return switch (block.prefix().getName() + "/" + name) {
                case "blockIngot/Iron", "blockSolid/Iron" -> Items.IRON_BLOCK;
                case "blockIngot/Gold", "blockSolid/Gold" -> Items.GOLD_BLOCK;
                case "blockIngot/Copper", "blockSolid/Copper" -> Items.COPPER_BLOCK;
                case "blockIngot/Netherite", "blockSolid/Netherite" -> Items.NETHERITE_BLOCK;
                case "blockGem/Diamond" -> Items.DIAMOND_BLOCK;
                case "blockGem/Coal" -> Items.COAL_BLOCK;
                case "blockGem/Lapis" -> Items.LAPIS_BLOCK;
                case "blockDust/Redstone", "blockGem/Redstone" -> Items.REDSTONE_BLOCK;
                default -> null;
            };
        }
        var form = MaterialEquivalence.form(stack);
        if (form == null) return null;
        return switch (form.prefix().getName() + "/" + form.material().getName()) {
            case "ingot/Iron" -> Items.IRON_INGOT;
            case "ingot/Gold" -> Items.GOLD_INGOT;
            case "ingot/Copper" -> Items.COPPER_INGOT;
            case "ingot/Netherite" -> Items.NETHERITE_INGOT;
            case "nugget/Iron" -> Items.IRON_NUGGET;
            case "nugget/Gold" -> Items.GOLD_NUGGET;
            case "gem/Diamond" -> Items.DIAMOND;
            case "gem/Emerald" -> Items.EMERALD;
            case "gem/Amethyst" -> Items.AMETHYST_SHARD;
            case "gem/Coal" -> Items.COAL;
            case "gem/Charcoal" -> Items.CHARCOAL;
            case "gem/Lapis" -> Items.LAPIS_LAZULI;
            case "dust/Redstone" -> Items.REDSTONE;
            case "dust/Sugar" -> Items.SUGAR;
            case "dust/Gunpowder" -> Items.GUNPOWDER;
            case "dust/Glowstone" -> Items.GLOWSTONE_DUST;
            default -> null;
        };
    }

    /** The explicitly replaced GT entries, shared by creative tabs, JEI and EMI. */
    public static boolean hiddenDuplicate(ItemStack stack) {
        if (stack == null || stack.isEmpty()
                || !BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("gt6ce")) return false;
        Item target = requestedTarget(stack);
        return target != null && target != stack.getItem();
    }

    public static ItemStack canonical(ItemStack stack) {
//? if >=1.21.1 {
        /*if (stack == null || stack.isEmpty() || !stack.getComponentsPatch().isEmpty() || stack.isDamaged()) return stack;
*///?} else {
        if (stack == null || stack.isEmpty() || stack.hasTag() || stack.isDamaged()) return stack;
//?}
        Item target = requestedTarget(stack);
        if (target != null) return target == stack.getItem() ? stack : new ItemStack(target, stack.getCount());
        if (target == null) {
            var form = MaterialEquivalence.form(stack);
            if (form != null) target = switch (form.prefix().getName() + "/" + form.material().getName()) {
                case "gem/Emerald" -> Items.EMERALD;
                case "gem/NetherQuartz" -> Items.QUARTZ;
                case "stick/Wood" -> Items.STICK;
                default -> null;
            };
        }
        if (target == null || target == stack.getItem()) return stack;
        // Block forms have a separately verified 9U mapping; item forms must agree with vanilla data.
        if (stack.getItem() instanceof BlockItem) return new ItemStack(target, stack.getCount());
        var expected = MaterialEquivalence.form(stack);
        var result = new ItemStack(target, stack.getCount());
        var actual = MaterialEquivalence.form(result);
        return expected != null && actual != null && actual.prefix() == expected.prefix()
                && actual.material() == expected.material() ? result : stack;
    }
}
