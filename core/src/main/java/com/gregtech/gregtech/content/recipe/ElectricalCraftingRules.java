package com.gregtech.gregtech.content.recipe;

/** Original CS.OD_CIRCUITS and MT.DATA wire/cable arrays, without loader types. */
public final class ElectricalCraftingRules {
    private ElectricalCraftingRules() {}

    public static String circuitTag(int tier) {
        if (tier < 0 || tier > 9) throw new IllegalArgumentException("Unknown GT6 circuit tier: " + tier);
        return "gregtech:circuits_tier_" + tier + "_plus";
    }

    public static String wireMaterial(int tier) {
        if (tier < 0 || tier > 15) throw new IllegalArgumentException("Unknown GT6 wire tier: " + tier);
        return switch (tier) {
            case 0 -> "Lead";
            case 1 -> "Tin";
            case 2 -> "Copper"; // ANY.Cu is resolved as a family tag at the platform boundary.
            case 3 -> "Gold";
            case 4 -> "Aluminium";
            case 5 -> "Platinum";
            default -> tier <= 10 ? "Graphene" : "Superconductor";
        };
    }

    public static boolean insulatedCable(int tier) {
        wireMaterial(tier);
        return tier < 6;
    }
}
