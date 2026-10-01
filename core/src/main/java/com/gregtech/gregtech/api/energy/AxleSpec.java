package com.gregtech.gregtech.api.energy;

import com.gregtech.gregtech.api.material.GTMaterial;

/** Defines one rotational axle type (GT6 {@code MultiTileEntityAxle}). RU conductor. */
public record AxleSpec(String id, GTMaterial material, int size, long maxSpeed, long maxPower, long lossPerBlock) {

    /** Axle radius in 1/16 px for collision and model rendering. Original sizes have diameters 6, 9, 12 and 16 px. */
    public double thickness() {
        return switch(size) { case 1 -> 6; case 2 -> 9; case 3 -> 12; case 4 -> 16; default -> throw new IllegalArgumentException("Axle size "+size); };
    }

    public double halfThickness() { return thickness() / 2.0; }
}
