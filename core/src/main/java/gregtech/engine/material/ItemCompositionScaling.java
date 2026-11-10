package gregtech.engine.material;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import gregtech.api.material.MaterialSentinels;
import java.math.BigInteger;
import java.util.List;

/** Exact floor scaling for positive component amounts on a damaged item stack. */
public final class ItemCompositionScaling {
    private ItemCompositionScaling() {}

    public static long scale(long amount, int remaining, int maximum) {
        if (amount < 0) throw new IllegalArgumentException("amount must not be negative");
        if (remaining < 0) throw new IllegalArgumentException("remaining must not be negative");
        if (maximum <= 0) throw new IllegalArgumentException("maximum must be positive");
        if (remaining > maximum) throw new IllegalArgumentException("remaining must not exceed maximum");
        return BigInteger.valueOf(amount)
                .multiply(BigInteger.valueOf(remaining))
                .divide(BigInteger.valueOf(maximum))
                .longValueExact();
    }

    /** Scales every component while retaining primary identity only while that component remains. */
    public static ItemComposition scale(ItemComposition composition, int remaining, int maximum) {
        List<MaterialComponent> components = composition.components().stream()
                .map(component ->
                        MaterialComponent.of(component.material(), scale(component.amount(), remaining, maximum)))
                .filter(component -> component.amount() > 0)
                .toList();
        GTMaterial originalPrimary = composition.primaryMaterial();
        boolean primaryRemains =
                components.stream().anyMatch(component -> component.material().resolve() == originalPrimary);
        GTMaterial primary = primaryRemains ? originalPrimary : MaterialSentinels.INVALID;
        return new ItemComposition(
                composition.prefix(), components, primary, composition.sources(), composition.bulkRecoveryAllowed());
    }
}
