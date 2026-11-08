package gregtech.engine.material;

import gregtech.api.material.MaterialProperty;
import java.util.EnumSet;
import java.util.Set;

/** Host-owned boolean capability closure; declaration and link-time flags use the same rules. */
final class MaterialPropertyRules {
    private MaterialPropertyRules() {}

    static Set<MaterialProperty> normalize(Set<MaterialProperty> input) {
        var result = EnumSet.noneOf(MaterialProperty.class);
        result.addAll(input);
        if (result.contains(MaterialProperty.GAS)) result.add(MaterialProperty.LIQUID);
        if (result.contains(MaterialProperty.CENTRIFUGE) || result.contains(MaterialProperty.ELECTROLYSER))
            result.add(MaterialProperty.DECOMPOSABLE);
        if (result.contains(MaterialProperty.FLAMMABLE) && result.contains(MaterialProperty.UNBURNABLE))
            throw new IllegalArgumentException("A material cannot be both FLAMMABLE and UNBURNABLE");
        return Set.copyOf(result);
    }
}
