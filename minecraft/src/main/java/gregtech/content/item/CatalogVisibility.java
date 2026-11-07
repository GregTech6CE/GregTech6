package gregtech.content.item;

import gregtech.GregTechConfig.Rule;
import gregtech.api.material.MaterialProperty;
import gregtech.engine.material.MaterialFormItem;
import gregtech.content.block.MaterialBlockLike;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Presentation only: never removes a registered item, recipe, or obtainable stack. */
public final class CatalogVisibility {
    private CatalogVisibility() {}

    public static boolean visible(ItemStack stack) {
        if (stack.isEmpty()) return true;
        var item = stack.getItem();
        var id = BuiltInRegistries.ITEM.getKey(item);
        if (gregtech.engine.material.MaterialUnification.hiddenDuplicate(stack)) return false;
        if (id.getNamespace().equals("gt6ce")
                && (id.getPath().startsWith("sapling_small_") || id.getPath().startsWith("sapling_large_"))) return false;
        // Original MicroBlocks controls third-party microblocks/facades, not GT slabs.
        if (!Rule.MICROBLOCKS.enabled() && java.util.Set.of(
                "forgemultipart:microblock", "extrautils:microblocks", "extrautils2:microblocks",
                "appliedenergistics2:facade", "ae2:facade").contains(id.toString())) return false;
        if (item instanceof gregtech.content.block.misc.LegacyConstructionBlockItem) return false;
        if (item instanceof MaterialFormItem form) {
            if (!Rule.HIDDEN_MATERIALS.enabled() && form.getMaterial().has(MaterialProperty.HIDDEN)) return false;
            if (!Rule.HIDDEN_PREFIXES.enabled() && form.getPrefix().isHiddenFromCreative()) return false;
        }
        if (item instanceof BlockItem block && block.getBlock() instanceof MaterialBlockLike form) {
            if (!Rule.HIDDEN_MATERIALS.enabled() && form.material().has(MaterialProperty.HIDDEN)) return false;
            // Keep normal stone ores, as original PrefixBlockItem does.
            if (!Rule.ORE_BLOCKS.enabled() && form.prefix().getName().startsWith("ore")
                    && !form.prefix().getName().equals("ore")) return false;
        }
        if (!Rule.ORE_BLOCKS.enabled() && item instanceof BlockItem block
                && block.getBlock() instanceof gregtech.content.block.OreBlock
                && (gregtech.content.block.OreBlock.isBrokenStack(stack)
                || gregtech.content.block.OreBlock.stoneOfStack(stack) != gregtech.content.block.OreHostStone.STONE)) return false;
        if (!id.getNamespace().equals("gt6ce")) return true;
        if (!Rule.BUMBLEBEES.enabled() && gregtech.content.bumble.BumbleBeeType.speciesOf(stack) != null) return false;
//? if >=1.21.1 {
        /*if (item instanceof gregtech.platform.neoforge.fluid.FluidDisplayItem fluid)
*///?} else {
        if (item instanceof FluidItem fluid)
//?}
            return !Rule.FLUID_DISPLAY.enabled() && (Rule.HIDDEN_ITEMS.enabled() || !fluid.fluidEntry().isHidden());
        if (!Rule.HIDDEN_ITEMS.enabled()) {
            if (item instanceof CannedFoodItem can && can.spec().hidden()) return false;
            if (item instanceof ColoredBookItem book && book.variant().hidden()) return false;
        }
        return true;
    }
}
