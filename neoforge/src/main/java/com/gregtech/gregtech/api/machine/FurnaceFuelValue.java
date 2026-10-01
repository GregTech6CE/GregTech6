package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.GregTechConstants;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

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
                .orElseGet(() -> (long) Math.max(0, stack.getBurnTime(RecipeType.SMELTING)));
    }

    public static long getHeatUnits(ItemStack stack) {
        return getBurnValue(stack) * GregTechConstants.EU_PER_FURNACE_TICK;
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
