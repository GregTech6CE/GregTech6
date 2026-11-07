/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

/** Process keys owned by the GT6CE host. Addons define additional namespaced keys. */
public final class MaterialProcessKeys {
    private MaterialProcessKeys() {}

    public static final MaterialProcessKey<MaterialTarget> SMELTING = target("smelting");
    public static final MaterialProcessKey<MaterialTarget> CRUSHING = target("crushing");
    public static final MaterialProcessKey<MaterialTarget> PULVERIZING = target("pulverizing");
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
    public static MaterialProcessKey<MaterialTarget> legacyTarget(String kind) {
        return switch (kind) {
            case "Solidifying" -> SOLIDIFYING;
            case "Bending" -> BENDING;
            case "Compressing" -> COMPRESSING;
            case "Cutting" -> CUTTING;
            case "Forging" -> FORGING;
            case "Smashing" -> SMASHING;
            case "Working" -> WORKING;
            default -> throw new IllegalArgumentException("Unknown processing target: " + kind);
        };
    }

    public static String legacyTargetName(MaterialProcessKey<?> key) {
        if (key.equals(SOLIDIFYING)) return "Solidifying";
        if (key.equals(BENDING)) return "Bending";
        if (key.equals(COMPRESSING)) return "Compressing";
        if (key.equals(CUTTING)) return "Cutting";
        if (key.equals(FORGING)) return "Forging";
        if (key.equals(SMASHING)) return "Smashing";
        if (key.equals(WORKING)) return "Working";
        return "";
    }

    private static MaterialProcessKey<MaterialTarget> target(String path) {
        return key(path, MaterialTarget.class);
    }

    private static <T> MaterialProcessKey<T> key(String path, Class<T> type) {
        return new MaterialProcessKey<>(new DataKey("gt6ce", path), type);
    }
}
