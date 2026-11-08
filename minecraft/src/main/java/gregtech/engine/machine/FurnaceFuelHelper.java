package gregtech.engine.machine;

import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.MaterialStackItemHelper;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** GT6 {@code FM.Furnace} fuel power without the full recipe map. */
public final class FurnaceFuelHelper {
    private FurnaceFuelHelper() {}

    public record FuelBurnResult(
            long heatUnits,
            @Nullable ItemStack container,
            @Nullable ItemStack byproduct) {}

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
        long heat = MaterialFuelRules.heatUnits(burnValue, efficiency);
        if (heat <= 0) {
            return Optional.empty();
        }

        ItemStack container = fuel.getCraftingRemainingItem();
        if (container.isEmpty()) {
            container = null;
        } else {
            container = container.copyWithCount(1);
        }
        var ashes = ashByproducts(fuel);
        if (ashes.isEmpty()) return Optional.empty();
        ItemStack byproduct = ashes.get().isEmpty() ? null : ashes.get().get(0);
        return Optional.of(new FuelBurnResult(heat, container, byproduct));
    }

    private static Optional<List<ItemStack>> ashByproducts(ItemStack fuel) {
        var data = ItemMaterialRegistry.get(fuel).orElse(null);
        if (data == null) return Optional.of(List.of());
        var piles = MaterialFuelRules.ashResidues(data.components());
        if (piles.isEmpty()) return Optional.empty();
        var stacks = piles.get().stream()
                .map(pile ->
                        MaterialStackItemHelper.createStack(MaterialPrefix.dustTiny, pile.material(), pile.tinyDust()))
                .filter(stack -> !stack.isEmpty())
                .toList();
        return Optional.of(stacks);
    }
}
