package com.gregtech.gregtech.api.energy;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.GregTechConstants;

/** Defines one electric wire type (GT6 {@code MultiTileEntityWireElectric}). */
public record WireSpec(String id, GTMaterial material, int size, long voltage, long amperage, long lossPerBlock,
                       boolean insulated, boolean contactDamage) {

    private static final int[] WIRE_DIAMETERS = {2,3,4,6,7,7,8,8,9,10,11,12,13,14,15,16};

    public WireSpec(String id, GTMaterial material, int size, long voltage, long amperage, long lossPerBlock, boolean insulated) {
        this(id, material, size, voltage, amperage, lossPerBlock, insulated, !insulated);
    }

    public WireSpec(String id, GTMaterial material, int size, long voltage, long amperage, long lossPerBlock) {
        this(id, material, size, voltage, amperage, lossPerBlock, false);
    }

    public boolean isSuperconductor() { return lossPerBlock <= 0; }

    /** Texture set folder name matching {@code MaterialTextureSet}. */
    public String textureSet() { return material.getTextureSet().name().toLowerCase(java.util.Locale.ROOT); }

    /** GT6: amount = size 脳 U/2 (1x wire = 0.5 units). */
    public long materialAmount() { return (long) size * GregTechConstants.U2; }

    /** GT6 explicit connector diameters, shared by rendered geometry and physical bounds. */
    public double thickness() {
        if (insulated) return switch (size) {
            case 1 -> 4; case 2 -> 6; case 4 -> 8; case 8 -> 12; case 12 -> 16;
            default -> throw new IllegalArgumentException("GT6 cable size: " + size);
        };
        if (size < 1 || size > 16) throw new IllegalArgumentException("GT6 wire size: " + size);
        return WIRE_DIAMETERS[size-1];
    }

    /** Half-thickness for model from/to coordinates. */
    public double halfThickness() { return thickness() / 2.0; }
}
