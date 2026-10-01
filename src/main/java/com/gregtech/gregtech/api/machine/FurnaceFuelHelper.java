package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.api.material.MaterialStackItemHelper;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.GregTechConstants;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** GT6 {@code FM.Furnace} fuel power without the full recipe map. */
public final class FurnaceFuelHelper {
    private FurnaceFuelHelper() {}

    public record FuelBurnResult(long heatUnits, @Nullable ItemStack container, @Nullable ItemStack byproduct) {}

    /**
     * Computes HU from a furnace fuel stack (GT6: {@code tFuelValue * EU_PER_FURNACE_TICK}, scaled by efficiency).
     */
    public static Optional<FuelBurnResult> burnOne(ItemStack fuel, int efficiency) {
        if (fuel.isEmpty()) {
            return Optional.empty();
        }
        long burnValue = FurnaceFuelValue.getBurnValue(fuel);
        if (burnValue <= 0) {
            return Optional.empty();
        }
        long heat = burnValue * GregTechConstants.EU_PER_FURNACE_TICK * efficiency / 10000L;
        if (heat <= 0) {
            return Optional.empty();
        }

        ItemStack container = fuel.getCraftingRemainingItem();
        if (container.isEmpty()) {
            container = null;
        } else {
            container = container.copyWithCount(1);
        }
        return Optional.of(new FuelBurnResult(heat, container, ashByproduct(fuel)));
    }

    @Nullable
    private static ItemStack ashByproduct(ItemStack fuel) {
        return ItemMaterialRegistry.get(fuel).flatMap(data -> {
            GTMaterial material = data.material().resolve();
            GTMaterial ash = material.getTargetBurningMaterial();
            long amount = material.getTargetBurningAmount();
            if (ash == null || !ash.isValid() || amount <= 0) {
                return Optional.empty();
            }
            long itemAmount = data.amount() > 0 ? data.amount() : MaterialPrefix.dust.getMaterialWeight();
            long scaled = FurnaceFuelValue.scaleUnits(amount, GregTechConstants.U, itemAmount);
            if (scaled <= 0) {
                return Optional.empty();
            }
            int count = (int) Math.min(64L, Math.max(1L, scaled / GregTechConstants.U9));
            ItemStack stack = MaterialStackItemHelper.createStack(MaterialPrefix.dustTiny, ash, count);
            return stack.isEmpty() ? Optional.empty() : Optional.of(stack);
        }).orElse(null);
    }
}
