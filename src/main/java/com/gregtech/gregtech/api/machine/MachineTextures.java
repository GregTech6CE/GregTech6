package com.gregtech.gregtech.api.machine;

import com.gregtech.gregtech.GregTech;

/** Texture paths under {@code textures/block/machines/generators/}. */
public final class MachineTextures {
    public static final String BURNING_SOLID = "burning_solid";
    public static final String BURNING_LIQUID = "burning_liquid";
    public static final String BURNING_GAS = "burning_gas";
    public static final String BURNING_FLUIDBED = "burning_fluidbed";

    private MachineTextures() {}

    public static String colored(String set, String face) {
        return "block/machines/generators/" + set + "/colored/" + face;
    }

    public static String overlay(String set, boolean active, String face) {
        String layer = active ? "overlay_active" : "overlay";
        return "block/machines/generators/" + set + "/" + layer + "/" + face;
    }

    public static String modelId(String blockId) {
        return "block/machine/" + blockId;
    }

    public static String blockModelLocation(String blockId) {
        return GregTech.id(modelId(blockId)).toString();
    }
}
