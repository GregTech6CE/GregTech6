package gregtech.content.tool;

import gregtech.GregTechConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Exact GT6_Main SmallerVanillaToolDurability roster, without mutating item or stack data. */
public final class VanillaToolDurability {
    private VanillaToolDurability() {}

    public static int maximum(ItemStack stack, int original) {
        return maximum(stack.getItem(), original, GregTechConfig.Rule.SMALLER_VANILLA_TOOL_DURABILITY.enabled());
    }

    public static int maximum(Item item, int original, boolean enabled) {
        if (!enabled) return original;
        // Apply only to the unmodified vanilla maximum. Per-stack components and other mods'
        // custom durability remain authoritative, as do unrelated tools and Netherite.
        if (original == 59
                && (item == Items.WOODEN_SWORD
                        || item == Items.WOODEN_PICKAXE
                        || item == Items.WOODEN_SHOVEL
                        || item == Items.WOODEN_AXE
                        || item == Items.WOODEN_HOE)) return 8;
        if (original == 131
                && (item == Items.STONE_SWORD
                        || item == Items.STONE_PICKAXE
                        || item == Items.STONE_SHOVEL
                        || item == Items.STONE_AXE
                        || item == Items.STONE_HOE)) return 16;
        if (original == 32
                && (item == Items.GOLDEN_SWORD
                        || item == Items.GOLDEN_PICKAXE
                        || item == Items.GOLDEN_SHOVEL
                        || item == Items.GOLDEN_AXE
                        || item == Items.GOLDEN_HOE)) return 32;
        if (original == 250
                && (item == Items.IRON_SWORD
                        || item == Items.IRON_PICKAXE
                        || item == Items.IRON_SHOVEL
                        || item == Items.IRON_AXE
                        || item == Items.IRON_HOE)) return 128;
        if (original == 1561
                && (item == Items.DIAMOND_SWORD
                        || item == Items.DIAMOND_PICKAXE
                        || item == Items.DIAMOND_SHOVEL
                        || item == Items.DIAMOND_AXE
                        || item == Items.DIAMOND_HOE)) return 512;
        return original;
    }
}
