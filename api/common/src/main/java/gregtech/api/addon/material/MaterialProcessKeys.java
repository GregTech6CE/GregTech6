/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

/** Process keys owned by the GT6CE host. Addons define additional namespaced keys. */
public final class MaterialProcessKeys {
    private MaterialProcessKeys() {}

    public static final MaterialProcessKey<MaterialTarget> SMELTING = target("smelting");
    public static final MaterialProcessKey<MaterialTarget> CRUSHING = target("crushing");
    public static final MaterialProcessKey<MaterialTarget> PULVERIZING = target("pulverizing");
    public static final MaterialProcessKey<MaterialDecompositionRule> DECOMPOSITION =
            key("decomposition", MaterialDecompositionRule.class);
    public static final MaterialProcessKey<FuelRule> FUEL = key("fuel", FuelRule.class);
    public static final MaterialProcessKey<OreProcessingRule> ORE_PROCESSING =
            key("ore_processing", OreProcessingRule.class);
    public static final MaterialProcessKey<MaterialTarget> SOLIDIFYING = target("solidifying");
    public static final MaterialProcessKey<MaterialTarget> BENDING = target("bending");
    public static final MaterialProcessKey<MaterialTarget> COMPRESSING = target("compressing");
    public static final MaterialProcessKey<MaterialTarget> CUTTING = target("cutting");
    public static final MaterialProcessKey<MaterialTarget> FORGING = target("forging");
    public static final MaterialProcessKey<MaterialTarget> SMASHING = target("smashing");
    public static final MaterialProcessKey<MaterialTarget> WORKING = target("working");

    /** Resolves one of GT6's seven legacy processing target names. */
    @ApiStatus.Internal
    public static Optional<MaterialProcessKey<MaterialTarget>> legacyTarget(String kind) {
        if (kind == null) return Optional.empty();
        return Optional.ofNullable(switch (kind) {
            case "Solidifying" -> SOLIDIFYING;
            case "Bending" -> BENDING;
            case "Compressing" -> COMPRESSING;
            case "Cutting" -> CUTTING;
            case "Forging" -> FORGING;
            case "Smashing" -> SMASHING;
            case "Working" -> WORKING;
            default -> null;
        });
    }

    @ApiStatus.Internal
    public static Optional<String> legacyTargetName(MaterialProcessKey<?> key) {
        if (key == null) return Optional.empty();
        if (key.equals(SOLIDIFYING)) return Optional.of("Solidifying");
        if (key.equals(BENDING)) return Optional.of("Bending");
        if (key.equals(COMPRESSING)) return Optional.of("Compressing");
        if (key.equals(CUTTING)) return Optional.of("Cutting");
        if (key.equals(FORGING)) return Optional.of("Forging");
        if (key.equals(SMASHING)) return Optional.of("Smashing");
        if (key.equals(WORKING)) return Optional.of("Working");
        return Optional.empty();
    }

    private static MaterialProcessKey<MaterialTarget> target(String path) {
        return key(path, MaterialTarget.class);
    }

    private static <T> MaterialProcessKey<T> key(String path, Class<T> type) {
        return MaterialProcessKey.single(new DataKey("gt6ce", path), type);
    }
}
