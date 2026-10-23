package com.gregtech.gregtech.content.energy;

import com.gregtech.gregtech.api.material.GTMaterial;

/** Existing unpainted signal-wire tints; paint overrides and native alpha stay in their renderers. */
public final class SignalWireColors {
    private SignalWireColors() {}

    public static int tint(GTMaterial material, int layer) {
        return layer == 0 ? material.getColor() : layer == 1 ? 0x604040 : 0xFFFFFF;
    }
}
