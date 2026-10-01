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
import net.minecraftforge.common.ForgeHooks;
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
                .orElseGet(() -> (long) Math.max(0, ForgeHooks.getBurnTime(stack, RecipeType.SMELTING)));
    }

    public static long getHeatUnits(ItemStack stack) {
        return getBurnValue(stack) * GregTechConstants.EU_PER_FURNACE_TICK;
    }

    public static long getBurnValue(GTMaterial material, @Nullable MaterialPrefix prefix, long amount) {
        GTMaterial mat = material.resolve();
        if (!mat.isValid()) {
            return 0;
        }
        long burnTime = mat.getFurnaceBurnTime();
        if (burnTime <= 0 && prefix == null) {
            return 0;
        }

        long scaled;
        if (prefix == null) {
            scaled = scaleUnits(burnTime, GTValues.U, amount);
        } else if (prefix == MaterialPrefix.oreRaw) {
            scaled = burnTime;
        } else if (isBurnablePrefix(prefix)) {
            scaled = scaleUnits(burnTime, GTValues.U, amount);
            scaled = applyWoodPrefixRules(prefix, mat, scaled);
        } else {
            scaled = scaleUnits(burnTime, GTValues.U, amount);
        }
        return clampFuelValue(scaled);
    }

    private static long applyWoodPrefixRules(MaterialPrefix prefix, GTMaterial material, long burnTime) {
        if (!isWoodFamily(material)) {
            return burnTime;
        }
        if (prefix == MaterialPrefix.stick) {
            return Math.max(GregTechConstants.TICKS_PER_SMELT / 2L, burnTime);
        }
        if (prefix == MaterialPrefix.stickLong) {
            return Math.max(GregTechConstants.TICKS_PER_SMELT, burnTime);
        }
        if (prefix == MaterialPrefix.plate) {
            return Math.max((GregTechConstants.TICKS_PER_SMELT * 27L) / 2, burnTime);
        }
        return burnTime;
    }

    private static boolean isWoodFamily(GTMaterial material) {
        return material.has(MaterialProperty.WOOD) || material.resolve() == com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood.resolve();
    }

    private static boolean isBurnablePrefix(MaterialPrefix prefix) {
        return prefix == MaterialPrefix.stick
                || prefix == MaterialPrefix.stickLong
                || prefix == MaterialPrefix.plate
                || prefix == MaterialPrefix.plateDouble
                || prefix == MaterialPrefix.plateTriple
                || prefix == MaterialPrefix.plateQuadruple
                || prefix == MaterialPrefix.plateQuintuple
                || prefix == MaterialPrefix.plateDense
                || prefix == MaterialPrefix.dust
                || prefix == MaterialPrefix.dustSmall
                || prefix == MaterialPrefix.dustTiny
                || prefix == MaterialPrefix.dustDiv72
                || prefix == MaterialPrefix.gem;
    }

    /** GT6 {@code UT.Code.units}. */
    public static long scaleUnits(long value, long unit, long amount) {
        if (value <= 0 || amount <= 0) {
            return 0;
        }
        return value * amount / unit;
    }

    public static long clampFuelValue(long value) {
        return Math.min(32000L, Math.max(0L, value));
    }
}
