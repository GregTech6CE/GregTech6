package gregtech.internal.machine;

import gregtech.content.material.Materials;
import gregtech.internal.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.internal.material.ItemMaterialRegistry;
import gregtech.internal.material.MaterialProperty;
import gregtech.data.MaterialPrefix;
import gregtech.data.GregTechConstants;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

//? if >=1.21.1 {
//?} else {
import net.minecraftforge.common.ForgeHooks;
//?}
import org.jetbrains.annotations.Nullable;

/**
 * GT6 {@code GameRegistry.getFuelValue} / {@code GT_API_Proxy} furnace fuel lookup.
 * Burn value * {@link CS#EU_PER_FURNACE_TICK} = heat units (HU).
 */
public final class FurnaceFuelValue {
    private FurnaceFuelValue() {}

    public static long getBurnValue(ItemStack stack) {
        if (stack.isEmpty() || ItemMaterialRegistry.hasStoredContents(stack)) {
            return 0;
        }
        var explicit = originalBurnTime(stack);
        if (explicit != null) return explicit;
        if (stack.is(ItemTags.PLANKS)) {
            return 3L * GregTechConstants.TICKS_PER_SMELT / 2;
        }
        if (stack.is(ItemTags.LOGS)) {
            return 6L * GregTechConstants.TICKS_PER_SMELT;
        }

        return ItemMaterialRegistry.get(stack)
                .map(data -> {
                    long sum = 0;
                    for (var c : data.components()) {
                        long value = getBurnValue(c.material(), data.components().size() == 1 ? data.prefix() : null, c.amount());
                        if (value <= 0) return 0L; // A wooden handle does not make an iron tool furnace fuel.
                        sum += value;
                    }
                    return clampFuelValue(sum);
                })
                .filter(value -> value > 0)
//? if >=1.21.1 {
                /*.orElseGet(() -> (long) Math.max(0, stack.getBurnTime(RecipeType.SMELTING)));
*///?} else {
                .orElseGet(() -> (long) Math.max(0, ForgeHooks.getBurnTime(stack, RecipeType.SMELTING)));
//?}
    }

    public static long getHeatUnits(ItemStack stack) {
        return getBurnValue(stack) * GregTechConstants.EU_PER_FURNACE_TICK;
    }

    /** Original explicit item values; never calls the loader hook, avoiding fuel-event recursion. */
    public static Integer originalBurnTime(ItemStack stack) {
        if (stack.isEmpty() || ItemMaterialRegistry.hasStoredContents(stack)) return null;
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        Integer time = gregtech.content.recipe.OriginalSingleItemCatalog.burnTime(id.getNamespace(), id.getPath());
        if (time != null) return time;
        if (stack.getItem() instanceof gregtech.internal.material.MaterialFormItem form) {
            long total = 0;
            for (var part : gregtech.internal.material.MaterialChemistry.prefixMaterialWeights(form.getMaterial(), form.getPrefix()))
                total += getBurnValue(part.material(), form.getPrefix(), part.amount());
            return (int) clampFuelValue(total);
        }
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                && item.getBlock() instanceof gregtech.block.MaterialBlockLike block)
            return (int) MaterialFuelRules.blockBurnValue(block.material(), block.prefix());
        var data = ItemMaterialRegistry.get(stack).orElse(null);
        // MT.java:1243 declares 100 ticks per U of bark; dry bark is its half-U item.
        if (data != null && data.components().size() == 1 && data.material().resolve().getName().equals("Bark"))
            return (int) Math.min(32000L, 100L * data.amount() / GTValues.U);
        return null;
    }

    public static long getBurnValue(GTMaterial material, @Nullable MaterialPrefix prefix, long amount) {
        return MaterialFuelRules.getBurnValue(material, prefix, amount);
    }

    public static long scaleUnits(long value, long unit, long amount) {
        return MaterialFuelRules.scaleUnits(value, unit, amount);
    }

    public static long clampFuelValue(long value) {
        return MaterialFuelRules.clampFuelValue(value);
    }
}
